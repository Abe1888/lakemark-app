package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.auth.CreatedStaffAccount
import com.example.data.auth.DefaultStaffRoster
import com.example.data.auth.StaffAuthClient
import com.example.data.auth.StaffPasswordHasher
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AlertTargets
import com.example.data.model.AppNotification
import com.example.data.model.AppReleaseConfig
import com.example.data.model.BrandConfig
import com.example.data.model.CartItem
import com.example.data.model.DefaultMenuCatalog
import com.example.data.model.MenuItem
import com.example.data.model.Order
import com.example.data.model.OrderLineItem
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.SavedMenuItem
import com.example.data.model.StaffUser
import com.example.data.model.UserRole
import com.example.data.model.WaiterArea
import com.example.data.model.WaiterCoverage
import com.example.localization.RemoteTranslation
import com.example.localization.TranslationCategory
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.roundToLong

class LakemarkRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val appContext: Context? = null,
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
        appContext = context.applicationContext,
    )

    private fun getOrInitProvisionerAuth(): FirebaseAuth {
        val ctx = appContext
            ?: throw IllegalStateException("Application context is required to provision staff logins.")
        val app = try {
            FirebaseApp.getInstance("staffProvisioner")
        } catch (_: IllegalStateException) {
            val defaultApp = FirebaseApp.getInstance()
            FirebaseApp.initializeApp(ctx, defaultApp.options, "staffProvisioner")
        }
        return FirebaseAuth.getInstance(app)
    }

    private suspend fun provisionStaffAuthAccount(
        username: String,
        password: String,
        previousPassword: String? = null,
    ): Result<String> {
        val email = StaffAuthClient.staffEmailFor(username)
        return try {
            val provisionerAuth = getOrInitProvisionerAuth()
            try {
                val uid = provisionerAuth.createUserWithEmailAndPassword(email, password).await().user?.uid
                    ?: return Result.failure(IllegalStateException("Firebase Auth did not return a user ID for $email."))
                Result.success(uid)
            } catch (collision: FirebaseAuthUserCollisionException) {
                val signedIn = runCatching {
                    provisionerAuth.signInWithEmailAndPassword(email, password).await().user
                }.getOrNull()
                if (signedIn != null) {
                    Result.success(signedIn.uid)
                } else if (!previousPassword.isNullOrBlank() && previousPassword != password) {
                    val oldUser = runCatching {
                        provisionerAuth.signInWithEmailAndPassword(email, previousPassword).await().user
                    }.getOrNull()
                    if (oldUser != null) {
                        oldUser.updatePassword(password).await()
                        Result.success(oldUser.uid)
                    } else {
                        Result.failure(
                            IllegalStateException(
                                "Staff login for $username already exists with a different password.",
                                collision,
                            ),
                        )
                    }
                } else {
                    Result.failure(
                        IllegalStateException(
                            "Staff login for $username already exists with a different password.",
                            collision,
                        ),
                    )
                }
            } finally {
                runCatching { getOrInitProvisionerAuth().signOut() }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("LakemarkAuth", "Failed to provision staff auth for $email", e)
            Result.failure(IllegalStateException(StaffAuthClient.mapFirebaseAuthException(e), e))
        }
    }

    private suspend fun writeAndDispatchNotification(payload: Map<String, Any>) {
        db.collection("notifications").document().set(payload).await()
    }

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")

    private fun logStreamErrorIfActive(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    suspend fun ensureUserProfile(user: FirebaseUser): Result<StaffUser> {
        val uid = user.uid
        val path = "users/$uid"
        return try {
            val verifiedAdminEmails = setOf(
                "abebaw4040@gmail.com",
            )
            val isVerifiedAdmin = user.email?.lowercase() in verifiedAdminEmails && user.isEmailVerified
            val isAdminDoc = runCatching {
                db.collection("admins").document(uid).get().await().exists()
            }.getOrDefault(false)
            val isAuthorizedManager = isVerifiedAdmin || isAdminDoc

            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(StaffUser::class.java, ServerTimestampBehavior.ESTIMATE)
                if (existing != null) {
                    if (isAuthorizedManager && (existing.userRole != UserRole.MANAGER || !existing.active)) {
                        val upgradePayload = mapOf(
                            "role" to UserRole.MANAGER.key,
                            "active" to true,
                            "permanentlyActivated" to true,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                        docRef.update(upgradePayload).await()
                        return Result.success(
                            existing.copy(
                                role = UserRole.MANAGER.key,
                                active = true,
                                permanentlyActivated = true,
                            ),
                        )
                    }
                    return Result.success(existing)
                }
            }

            if (!isAuthorizedManager) {
                return Result.failure(
                    SecurityException(
                        "Only an authorized Manager / Admin account can sign in with Google. Staff must use Staff Sign In.",
                    ),
                )
            }

            val displayName = (user.displayName ?: user.email?.substringBefore("@") ?: "Manager")
                .trim()
                .ifBlank { "Manager" }
                .take(60)
            val email = (user.email ?: "manager@lakemark.local").trim().take(120)
            val photoUrl = user.photoUrl?.toString()?.trim().orEmpty().take(1000)

            val payload = mapOf(
                "userId" to uid,
                "email" to email,
                "name" to displayName,
                "photoUrl" to photoUrl,
                "role" to UserRole.MANAGER.key,
                "active" to true,
                "permanentlyActivated" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            val created = docRef.get().await().toObject(StaffUser::class.java, ServerTimestampBehavior.ESTIMATE)
                ?: StaffUser(
                    id = uid,
                    userId = uid,
                    email = email,
                    name = displayName,
                    role = UserRole.MANAGER.key,
                    active = true,
                    permanentlyActivated = true,
                )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeCurrentUser(uid: String): Flow<StaffUser?> = flow {
        if (uid.isBlank()) return@flow
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { it.toObject(StaffUser::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeAllUsers(uid: String): Flow<List<StaffUser>> = flow {
        if (uid.isBlank()) return@flow
        val path = "users"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    val raw = snapshot.toObjects(StaffUser::class.java, ServerTimestampBehavior.ESTIMATE)
                    val migratedUsernames = raw
                        .filter { it.username.isNotBlank() && !it.userId.startsWith("staff_") }
                        .map { it.username.trim().lowercase() }
                        .toSet()
                    raw.filterNot {
                        it.userId.startsWith("staff_") &&
                            it.username.trim().lowercase() in migratedUsernames
                    }.sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun updateOwnProfile(
        uid: String,
        name: String? = null,
        photoUrl: String? = null,
    ): Result<Unit> {
        val path = "users/$uid"
        return try {
            val signedInUid = requireUserId()
            require(signedInUid == uid) { "Cannot update another user's profile." }
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (name != null) {
                val cleanName = name.trim().take(60)
                require(cleanName.isNotEmpty()) { "Name must not be empty." }
                updates["name"] = cleanName
            }
            if (photoUrl != null) {
                updates["photoUrl"] = photoUrl.trim().take(1000)
            }
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeMenuItems(uid: String, isStaffOrAdmin: Boolean): Flow<List<MenuItem>> = flow {
        if (uid.isBlank()) return@flow
        val path = "menu_items"
        val query = if (isStaffOrAdmin) {
            db.collection(path)
        } else {
            db.collection(path).whereEqualTo("available", true)
        }
        emitAll(
            query
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MenuItem::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun seedDefaultMenuIfEmpty(): Result<Unit> {
        val path = "menu_items"
        return try {
            requireUserId()
            val existing = db.collection(path).limit(1).get().await()
            if (!existing.isEmpty) return Result.success(Unit)
            val batch = db.batch()
            DefaultMenuCatalog.items.forEach { item ->
                val ref = db.collection(path).document(item.id)
                batch.set(
                    ref,
                    mapOf(
                        "name" to item.name,
                        "category" to item.category,
                        "description" to item.description,
                        "price" to item.price,
                        "available" to item.available,
                        "sortOrder" to item.sortOrder,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeOrders(uid: String, isStaffOrAdmin: Boolean): Flow<List<Order>> = flow {
        if (uid.isBlank()) return@flow
        val path = "orders"
        val query = if (isStaffOrAdmin) {
            db.collection(path)
        } else {
            db.collection(path).whereEqualTo("userId", uid)
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Order::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createOrder(
        actorRole: UserRole,
        senderBrandName: String,
        orderType: OrderType,
        location: String,
        customerName: String,
        specialNote: String,
        cartItems: List<CartItem>,
        assignedWaiterId: String = "",
        assignedMethod: String = "",
        notificationMessageOverride: String? = null,
    ): Result<Order> {
        var path = "orders"
        return try {
            val uid = requireUserId()
            require(cartItems.isNotEmpty() && cartItems.size <= 30) { "Order must contain 1 to 30 items." }
            val cleanCustomer = customerName.trim().take(80)
            val cleanLocation = location.trim().take(80)
            val cleanNote = specialNote.trim().take(200)
            val cleanWaiterId = assignedWaiterId.trim().take(128)
            val cleanMethod = if (cleanWaiterId.isNotBlank()) assignedMethod.ifBlank { "auto" } else ""
            require(cleanCustomer.isNotEmpty()) { "Enter your name." }
            require(cleanLocation.isNotEmpty()) { "Enter ${orderType.locationPrompt.lowercase()}." }

            val subtotal = cartItems.sumOf { it.menuItem.price * it.quantity.toLong() }
            val total = (subtotal * 1.15).roundToLong()
            val lineItems = cartItems.map {
                OrderLineItem(
                    menuItemId = it.menuItem.id,
                    name = it.menuItem.name,
                    quantity = it.quantity.toLong(),
                    price = it.menuItem.price,
                    note = cleanNote,
                )
            }

            val counterRef = db.collection("counters").document("orders")
            val orderRef = db.collection("orders").document()
            path = orderRef.path

            val orderNumber = db.runTransaction { tx ->
                val counterSnap = tx.get(counterRef)
                val nextNum = if (counterSnap.exists()) {
                    val current = (counterSnap.getLong("nextOrderNumber") ?: 1007L)
                    tx.update(
                        counterRef,
                        mapOf(
                            "nextOrderNumber" to (current + 1L),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                    current
                } else {
                    tx.set(
                        counterRef,
                        mapOf(
                            "nextOrderNumber" to 1008L,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                    1007L
                }

                val orderPayload = mapOf(
                    "orderNumber" to nextNum,
                    "userId" to uid,
                    "createdByRole" to actorRole.key,
                    "lastUpdatedByRole" to actorRole.key,
                    "lastUpdatedByUid" to uid,
                    "customerName" to cleanCustomer,
                    "orderType" to orderType.key,
                    "location" to cleanLocation,
                    "status" to OrderStatus.NEW.key,
                    "items" to lineItems.map { it.toMap() },
                    "specialNote" to cleanNote,
                    "cancelReason" to "",
                    "assignedWaiterId" to cleanWaiterId,
                    "assignedMethod" to cleanMethod,
                    "subtotal" to subtotal,
                    "total" to total,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                tx.set(orderRef, orderPayload)
                nextNum
            }.await()

            writeAndDispatchNotification(
                mapOf(
                    "senderUid" to uid,
                    "senderRole" to actorRole.key,
                    "senderBrandName" to senderBrandName.ifBlank { actorRole.displayName }.take(30),
                    "message" to (notificationMessageOverride?.trim()?.take(300)?.ifBlank { null }
                        ?: AlertTargets.messageForStatus(orderNumber, cleanLocation, OrderStatus.NEW)),
                    "targetRoles" to AlertTargets.recipientsForStatus(OrderStatus.NEW),
                    "targetUserId" to uid,
                    "orderId" to orderRef.id,
                    "kind" to "new",
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )

            val created = Order(
                id = orderRef.id,
                orderNumber = orderNumber,
                userId = uid,
                createdByRole = actorRole.key,
                lastUpdatedByRole = actorRole.key,
                lastUpdatedByUid = uid,
                customerName = cleanCustomer,
                orderType = orderType.key,
                location = cleanLocation,
                status = OrderStatus.NEW.key,
                items = lineItems,
                specialNote = cleanNote,
                cancelReason = "",
                assignedWaiterId = cleanWaiterId,
                assignedMethod = cleanMethod,
                subtotal = subtotal,
                total = total,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun advanceOrderStatus(
        order: Order,
        targetStatus: OrderStatus,
        actorRole: UserRole,
        senderBrandName: String,
        cancelReason: String = "",
        notificationMessageOverride: String? = null,
    ): Result<Unit> {
        val path = "orders/${order.id}"
        return try {
            val uid = requireUserId()
            require(order.id.isNotBlank()) { "Order ID must not be blank" }
            val current = order.statusEnum
            val validTarget = when (targetStatus) {
                OrderStatus.CONFIRMED -> current == OrderStatus.NEW
                OrderStatus.PREPARING -> current == OrderStatus.NEW || current == OrderStatus.CONFIRMED
                OrderStatus.READY -> current == OrderStatus.PREPARING
                OrderStatus.DONE -> current == OrderStatus.READY
                OrderStatus.CANCELLED -> (current == OrderStatus.NEW || current == OrderStatus.CONFIRMED) && cancelReason.trim().isNotEmpty()
                OrderStatus.NEW -> false
            }
            require(validTarget) { "Invalid order transition from ${current.key} to ${targetStatus.key}" }

            val updates = mutableMapOf<String, Any>(
                "status" to targetStatus.key,
                "updatedAt" to FieldValue.serverTimestamp(),
                "lastUpdatedByRole" to actorRole.key,
                "lastUpdatedByUid" to uid,
            )
            when (targetStatus) {
                OrderStatus.CONFIRMED -> updates["confirmedAt"] = FieldValue.serverTimestamp()
                OrderStatus.PREPARING -> {
                    if (order.confirmedAt == null) updates["confirmedAt"] = FieldValue.serverTimestamp()
                    updates["preparingAt"] = FieldValue.serverTimestamp()
                }
                OrderStatus.READY -> updates["readyAt"] = FieldValue.serverTimestamp()
                OrderStatus.DONE -> updates["doneAt"] = FieldValue.serverTimestamp()
                OrderStatus.CANCELLED -> {
                    updates["cancelReason"] = cancelReason.trim().take(200)
                    updates["cancelledAt"] = FieldValue.serverTimestamp()
                }
                OrderStatus.NEW -> Unit
            }

            db.collection("orders").document(order.id).update(updates).await()

            val notifKind = when (targetStatus) {
                OrderStatus.READY -> "ready"
                else -> "ping"
            }
            writeAndDispatchNotification(
                mapOf(
                    "senderUid" to uid,
                    "senderRole" to actorRole.key,
                    "senderBrandName" to senderBrandName.ifBlank { actorRole.displayName }.take(30),
                    "message" to (notificationMessageOverride?.trim()?.take(300)?.ifBlank { null }
                        ?: AlertTargets.messageForStatus(
                            order.orderNumber,
                            order.location,
                            targetStatus,
                            cancelReason.trim(),
                        )),
                    "targetRoles" to AlertTargets.recipientsForStatus(targetStatus),
                    "targetUserId" to order.userId,
                    "orderId" to order.id,
                    "kind" to notifKind,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeBrands(uid: String): Flow<Map<UserRole, BrandConfig>> = flow {
        if (uid.isBlank()) return@flow
        val path = "brands"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    val defaults = UserRole.entries.associateWith { it.defaultBrandConfig() }.toMutableMap()
                    snapshot.toObjects(BrandConfig::class.java, ServerTimestampBehavior.ESTIMATE).forEach { cfg ->
                        val role = UserRole.fromKey(cfg.roleKey)
                        defaults[role] = cfg
                    }
                    defaults.toMap()
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun saveBrandConfig(config: BrandConfig): Result<Unit> {
        val path = "brands/${config.roleKey}"
        return try {
            val uid = requireUserId()
            require(config.brandName.trim().isNotEmpty()) { "Brand name cannot be empty." }
            db.collection("brands").document(config.roleKey).set(config.toWriteMap(uid)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun resetBrandConfig(role: UserRole): Result<Unit> {
        val path = "brands/${role.key}"
        return try {
            requireUserId()
            db.collection("brands").document(role.key).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeNotifications(uid: String, isStaffOrAdmin: Boolean): Flow<List<AppNotification>> = flow {
        if (uid.isBlank()) return@flow
        val path = "notifications"
        if (isStaffOrAdmin) {
            emitAll(
                db.collection(path)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(AppNotification::class.java, ServerTimestampBehavior.ESTIMATE)
                            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                            .take(40)
                    }
                    .catch { error ->
                        logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                        throw error
                    }
            )
        } else {
            val directFlow = db.collection(path)
                .whereEqualTo("targetUserId", uid)
                .snapshots()
                .map { it.toObjects(AppNotification::class.java, ServerTimestampBehavior.ESTIMATE) }
            val broadcastFlow = db.collection(path)
                .whereArrayContainsAny("targetRoles", listOf("all", UserRole.CUSTOMER.key))
                .snapshots()
                .map { it.toObjects(AppNotification::class.java, ServerTimestampBehavior.ESTIMATE) }
            emitAll(
                combine(directFlow, broadcastFlow) { direct, broadcasts ->
                    (direct + broadcasts)
                        .distinctBy { it.id }
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        .take(40)
                }.catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
            )
        }
    }

    suspend fun sendBroadcast(
        senderRole: UserRole,
        senderBrandName: String,
        message: String,
        targetRoleKey: String,
    ): Result<Unit> {
        val path = "notifications"
        return try {
            val uid = requireUserId()
            val cleanMsg = message.trim().take(300)
            require(cleanMsg.isNotEmpty()) { "Type a message first" }
            val targets = if (targetRoleKey == "all") {
                listOf("all")
            } else {
                listOf(targetRoleKey, "manager").distinct()
            }
            db.collection(path).document().set(
                mapOf(
                    "senderUid" to uid,
                    "senderRole" to senderRole.key,
                    "senderBrandName" to senderBrandName.ifBlank { senderRole.displayName }.take(30),
                    "message" to cleanMsg,
                    "targetRoles" to targets,
                    "targetUserId" to "",
                    "orderId" to "",
                    "kind" to "broadcast",
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun seedDefaultStaffIfEmpty(): Result<Int> {
        val path = "users"
        return try {
            requireUserId()
            val usersSnap = db.collection(path).get().await()
            val staffRoles = setOf(
                UserRole.RECEPTION.key,
                UserRole.KITCHEN.key,
                UserRole.WAITER.key,
                UserRole.CASHIER.key,
                UserRole.DELIVERY.key,
            )
            val alreadyHasStaff = usersSnap.documents.any { doc ->
                doc.getString("role")?.lowercase() in staffRoles
            }
            if (alreadyHasStaff) return Result.success(0)

            val existingIds = usersSnap.documents.map { it.id }.toSet()
            val toSeed = DefaultStaffRoster.accounts.filter { it.uid !in existingIds }
            if (toSeed.isEmpty()) return Result.success(0)

            val batch = db.batch()
            toSeed.forEach { staff ->
                val targetUid = provisionStaffAuthAccount(
                    username = staff.username,
                    password = staff.initialPassword,
                ).getOrThrow()
                val hashed = StaffPasswordHasher.hashPassword(staff.initialPassword)
                val now = FieldValue.serverTimestamp()
                batch.set(
                    db.collection("users").document(targetUid),
                    mapOf(
                        "userId" to targetUid,
                        "email" to StaffAuthClient.staffEmailFor(staff.username),
                        "username" to staff.username,
                        "staffPassword" to staff.initialPassword,
                        "name" to staff.name,
                        "role" to staff.role.key,
                        "active" to true,
                        "permanentlyActivated" to true,
                        "createdAt" to now,
                        "updatedAt" to now,
                    ),
                )
                batch.set(
                    db.collection("staff_credentials").document(targetUid),
                    mapOf(
                        "userId" to targetUid,
                        "username" to staff.username,
                        "passwordHash" to hashed.hashHex,
                        "passwordSalt" to hashed.saltHex,
                        "hashAlgo" to hashed.hashAlgo,
                        "role" to staff.role.key,
                        "createdAt" to now,
                        "updatedAt" to now,
                    ),
                )
                if (staff.role == UserRole.WAITER) {
                    batch.set(
                        db.collection("waiter_coverage").document(targetUid),
                        mapOf(
                            "userId" to targetUid,
                            "waiterName" to staff.name,
                            "onDuty" to true,
                            "areas" to staff.waiterAreas.ifEmpty { listOf(WaiterArea.F1.key) },
                            "updatedAt" to now,
                        ),
                    )
                }
            }
            batch.commit().await()
            Result.success(toSeed.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Ensures every active staff account in Firestore has a corresponding Firebase Auth
     * Email/Password account and user profile keyed by its Firebase Auth UID.
     */
    suspend fun syncExistingStaffAuthAccounts(): Result<Int> {
        val path = "users"
        return try {
            requireUserId()
            val usersSnap = db.collection(path).get().await()
            val existingIds = usersSnap.documents.map { it.id }.toSet()
            val staffRoles = setOf(
                UserRole.RECEPTION.key,
                UserRole.KITCHEN.key,
                UserRole.WAITER.key,
                UserRole.CASHIER.key,
                UserRole.DELIVERY.key,
            )
            val rosterByUsername = DefaultStaffRoster.accounts.associateBy {
                StaffAuthClient.normalizeUsername(it.username)
            }
            val staffDocs = usersSnap.documents.filter { doc ->
                val roleKey = doc.getString("role")?.lowercase().orEmpty()
                val username = StaffAuthClient.normalizeUsername(doc.getString("username").orEmpty())
                roleKey in staffRoles && username.isNotBlank()
            }
            var synced = 0
            var firstError: Throwable? = null
            for (doc in staffDocs) {
                val username = StaffAuthClient.normalizeUsername(doc.getString("username").orEmpty())
                val storedPassword = doc.getString("staffPassword").orEmpty()
                val rosterMatch = rosterByUsername[username]
                val effectivePassword = if (storedPassword.length >= 6) {
                    storedPassword
                } else {
                    rosterMatch?.initialPassword.orEmpty()
                }
                if (effectivePassword.length < 6) continue

                val provisionResult = provisionStaffAuthAccount(username, effectivePassword)
                val authUid = provisionResult.getOrNull()
                if (authUid == null) {
                    if (firstError == null) firstError = provisionResult.exceptionOrNull()
                    continue
                }
                if (authUid !in existingIds && authUid != doc.id) {
                    val now = FieldValue.serverTimestamp()
                    val roleKey = doc.getString("role").orEmpty()
                    val name = doc.getString("name").orEmpty().ifBlank { rosterMatch?.name.orEmpty() }
                    val active = doc.getBoolean("active") ?: true
                    val hashed = StaffPasswordHasher.hashPassword(effectivePassword)
                    val batch = db.batch()
                    batch.set(
                        db.collection("users").document(authUid),
                        mapOf(
                            "userId" to authUid,
                            "email" to StaffAuthClient.staffEmailFor(username),
                            "username" to username,
                            "staffPassword" to effectivePassword,
                            "name" to name,
                            "role" to roleKey,
                            "active" to active,
                            "permanentlyActivated" to true,
                            "createdAt" to now,
                            "updatedAt" to now,
                        ),
                    )
                    batch.set(
                        db.collection("staff_credentials").document(authUid),
                        mapOf(
                            "userId" to authUid,
                            "username" to username,
                            "passwordHash" to hashed.hashHex,
                            "passwordSalt" to hashed.saltHex,
                            "hashAlgo" to hashed.hashAlgo,
                            "role" to roleKey,
                            "createdAt" to now,
                            "updatedAt" to now,
                        ),
                    )
                    if (roleKey == UserRole.WAITER.key) {
                        batch.set(
                            db.collection("waiter_coverage").document(authUid),
                            mapOf(
                                "userId" to authUid,
                                "waiterName" to name,
                                "onDuty" to active,
                                "areas" to (rosterMatch?.waiterAreas ?: listOf(WaiterArea.F1.key)),
                                "updatedAt" to now,
                            ),
                        )
                    }
                    batch.commit().await()
                    synced++
                } else if (storedPassword.length < 6) {
                    db.collection("users").document(authUid).update(
                        mapOf(
                            "staffPassword" to effectivePassword,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    ).await()
                    synced++
                }
            }
            if (firstError != null && synced == 0) {
                Result.failure(firstError)
            } else {
                Result.success(synced)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createStaffAccountDirectly(
        name: String,
        username: String,
        role: UserRole,
        password: String,
    ): Result<CreatedStaffAccount> {
        var path = "users"
        return try {
            requireUserId()
            val cleanName = name.trim().take(60)
            val cleanUser = username.trim().lowercase().removePrefix("@")
            require(cleanName.isNotBlank()) { "Enter the staff member's name." }
            require(Regex("^[a-z0-9._-]{3,32}$").matches(cleanUser)) {
                "Username must be 3-32 characters (letters, numbers, ., _, -)."
            }
            require(role != UserRole.CUSTOMER && role != UserRole.MANAGER) {
                "Choose a staff role (Reception, Kitchen, Waiter, Cashier, or Delivery)."
            }
            require(password.length in 6..128) {
                "Password must be 6 to 128 characters."
            }

            val existingWithUsername = db.collection("users")
                .whereEqualTo("username", cleanUser)
                .limit(1)
                .get()
                .await()
            require(existingWithUsername.isEmpty) { "That username is already in use." }

            val staffUid = provisionStaffAuthAccount(cleanUser, password).getOrThrow()
            path = "users/$staffUid"

            val hashed = StaffPasswordHasher.hashPassword(password)
            val now = FieldValue.serverTimestamp()
            val batch = db.batch()
            batch.set(
                db.collection("users").document(staffUid),
                mapOf(
                    "userId" to staffUid,
                    "email" to "$cleanUser@staff.lakemark.local",
                    "username" to cleanUser,
                    "staffPassword" to password,
                    "name" to cleanName,
                    "role" to role.key,
                    "active" to true,
                    "permanentlyActivated" to true,
                    "createdAt" to now,
                    "updatedAt" to now,
                ),
            )
            batch.set(
                db.collection("staff_credentials").document(staffUid),
                mapOf(
                    "userId" to staffUid,
                    "username" to cleanUser,
                    "passwordHash" to hashed.hashHex,
                    "passwordSalt" to hashed.saltHex,
                    "hashAlgo" to hashed.hashAlgo,
                    "role" to role.key,
                    "createdAt" to now,
                    "updatedAt" to now,
                ),
            )
            if (role == UserRole.WAITER) {
                batch.set(
                    db.collection("waiter_coverage").document(staffUid),
                    mapOf(
                        "userId" to staffUid,
                        "waiterName" to cleanName,
                        "onDuty" to true,
                        "areas" to listOf(WaiterArea.F1.key),
                        "updatedAt" to now,
                    ),
                )
            }
            batch.commit().await()
            Result.success(
                CreatedStaffAccount(
                    uid = staffUid,
                    username = cleanUser,
                    name = cleanName,
                    role = role,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateStaffAccountDirectly(
        targetUserId: String,
        name: String,
        username: String,
        role: UserRole,
        active: Boolean,
        newPassword: String,
    ): Result<CreatedStaffAccount> {
        val path = "users/$targetUserId"
        return try {
            requireUserId()
            require(targetUserId.isNotBlank()) { "Invalid staff account." }
            val cleanName = name.trim().take(60)
            require(cleanName.isNotBlank()) { "Enter the staff member's name." }
            val cleanUser = username.trim().lowercase().removePrefix("@")
            if (cleanUser.isNotBlank()) {
                require(Regex("^[a-z0-9._-]{3,32}$").matches(cleanUser)) {
                    "Username must be 3-32 characters (letters, numbers, ., _, -)."
                }
            }
            require(role != UserRole.CUSTOMER) { "Use customer approval controls for guest accounts." }
            if (newPassword.isNotEmpty()) {
                require(newPassword.length in 6..128) { "New password must be 6 to 128 characters." }
            }

            val userRef = db.collection("users").document(targetUserId)
            val snap = userRef.get().await()
            require(snap.exists()) { "Staff account not found." }
            val existingRole = UserRole.fromKey(snap.getString("role"))
            require(existingRole != UserRole.CUSTOMER) { "Use customer approval controls for guest accounts." }

            val isExistingManager = existingRole == UserRole.MANAGER
            val existingUsername = snap.getString("username").orEmpty().trim().lowercase()
            val existingPassword = snap.getString("staffPassword").orEmpty()
            if (isExistingManager) {
                userRef.update(
                    mapOf(
                        "name" to cleanName,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
                return Result.success(
                    CreatedStaffAccount(
                        uid = targetUserId,
                        username = existingUsername,
                        name = cleanName,
                        role = UserRole.MANAGER,
                    ),
                )
            }

            val effectiveUser = cleanUser.ifBlank { existingUsername }
            val usernameChanged = existingUsername.isNotBlank() && effectiveUser != existingUsername
            if (newPassword.isNotEmpty()) {
                require(effectiveUser.isNotBlank()) { "Enter a username before setting a staff password." }
            }
            if (usernameChanged && newPassword.isEmpty()) {
                require(existingPassword.length >= 6) {
                    "Enter a new password (6+ characters) when changing a staff username."
                }
            }
            if (effectiveUser.isNotBlank()) {
                val dupSnap = db.collection("users")
                    .whereEqualTo("username", effectiveUser)
                    .limit(2)
                    .get()
                    .await()
                val takenByOther = dupSnap.documents.any { it.id != targetUserId }
                require(!takenByOther) { "That username is already in use." }
            }

            val now = FieldValue.serverTimestamp()
            val userUpdates = mutableMapOf<String, Any>(
                "name" to cleanName,
                "role" to role.key,
                "active" to active,
                "updatedAt" to now,
            )
            if (active) {
                userUpdates["permanentlyActivated"] = true
            }
            if (effectiveUser.isNotBlank()) {
                userUpdates["username"] = effectiveUser
            }
            if (newPassword.isNotEmpty()) {
                userUpdates["staffPassword"] = newPassword
                provisionStaffAuthAccount(
                    username = effectiveUser,
                    password = newPassword,
                    previousPassword = existingPassword,
                ).getOrThrow()
            } else if (usernameChanged) {
                provisionStaffAuthAccount(
                    username = effectiveUser,
                    password = existingPassword,
                ).getOrThrow()
            }
            userRef.update(userUpdates).await()

            if (effectiveUser.isNotBlank() || newPassword.isNotEmpty()) {
                val credRef = db.collection("staff_credentials").document(targetUserId)
                val credUpdates = mutableMapOf<String, Any>(
                    "username" to effectiveUser,
                    "role" to role.key,
                    "updatedAt" to now,
                )
                val hashed = if (newPassword.isNotEmpty()) {
                    StaffPasswordHasher.hashPassword(newPassword).also {
                        credUpdates["passwordHash"] = it.hashHex
                        credUpdates["passwordSalt"] = it.saltHex
                        credUpdates["hashAlgo"] = it.hashAlgo
                    }
                } else {
                    null
                }
                try {
                    credRef.update(credUpdates).await()
                } catch (e: FirebaseFirestoreException) {
                    if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                        val createHash = hashed
                            ?: throw IllegalArgumentException("Enter a password when assigning a username to this staff account.")
                        credRef.set(
                            mapOf(
                                "userId" to targetUserId,
                                "username" to effectiveUser,
                                "passwordHash" to createHash.hashHex,
                                "passwordSalt" to createHash.saltHex,
                                "hashAlgo" to createHash.hashAlgo,
                                "role" to role.key,
                                "createdAt" to now,
                                "updatedAt" to now,
                            ),
                        ).await()
                    } else {
                        throw e
                    }
                }
            }

            val covRef = db.collection("waiter_coverage").document(targetUserId)
            if (role == UserRole.WAITER) {
                val covSnap = covRef.get().await()
                if (!covSnap.exists()) {
                    covRef.set(
                        mapOf(
                            "userId" to targetUserId,
                            "waiterName" to cleanName,
                            "onDuty" to active,
                            "areas" to listOf(WaiterArea.F1.key),
                            "updatedAt" to now,
                        ),
                    ).await()
                } else {
                    covRef.update(
                        mapOf(
                            "waiterName" to cleanName,
                            "onDuty" to active,
                            "updatedAt" to now,
                        ),
                    ).await()
                }
            } else {
                covRef.delete().await()
            }

            Result.success(
                CreatedStaffAccount(
                    uid = targetUserId,
                    username = effectiveUser,
                    name = cleanName,
                    role = role,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun resetStaffPasswordDirectly(
        targetUserId: String,
        newPassword: String,
    ): Result<Unit> {
        val path = "staff_credentials/$targetUserId"
        return try {
            requireUserId()
            require(targetUserId.isNotBlank()) { "Invalid staff account." }
            require(newPassword.length in 6..128) { "New password must be 6 to 128 characters." }

            val userSnap = db.collection("users").document(targetUserId).get().await()
            require(userSnap.exists()) { "Staff account not found." }
            val roleKey = userSnap.getString("role").orEmpty().trim().lowercase()
            val username = userSnap.getString("username").orEmpty().trim().lowercase()
            val existingPassword = userSnap.getString("staffPassword").orEmpty()
            require(roleKey != UserRole.CUSTOMER.key && roleKey != UserRole.MANAGER.key && username.isNotBlank()) {
                "Password reset applies only to app-managed staff accounts."
            }

            provisionStaffAuthAccount(
                username = username,
                password = newPassword,
                previousPassword = existingPassword,
            ).getOrThrow()
            val hashed = StaffPasswordHasher.hashPassword(newPassword)
            val now = FieldValue.serverTimestamp()
            db.collection("users").document(targetUserId).update(
                mapOf(
                    "staffPassword" to newPassword,
                    "updatedAt" to now,
                ),
            ).await()
            val credRef = db.collection("staff_credentials").document(targetUserId)
            try {
                credRef.update(
                    mapOf(
                        "username" to username,
                        "passwordHash" to hashed.hashHex,
                        "passwordSalt" to hashed.saltHex,
                        "hashAlgo" to hashed.hashAlgo,
                        "role" to roleKey,
                        "updatedAt" to now,
                    ),
                ).await()
            } catch (e: FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                    credRef.set(
                        mapOf(
                            "userId" to targetUserId,
                            "username" to username,
                            "passwordHash" to hashed.hashHex,
                            "passwordSalt" to hashed.saltHex,
                            "hashAlgo" to hashed.hashAlgo,
                            "role" to roleKey,
                            "createdAt" to now,
                            "updatedAt" to now,
                        ),
                    ).await()
                } else {
                    throw e
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateStaffAccount(
        targetUserId: String,
        newRole: UserRole,
        active: Boolean,
        waiterName: String = "",
    ): Result<Unit> {
        val path = "users/$targetUserId"
        return try {
            requireUserId()
            require(targetUserId.isNotBlank()) { "User ID must not be blank" }
            val cleanName = waiterName.trim().take(60)
            val updates = mutableMapOf<String, Any>(
                "role" to newRole.key,
                "active" to active,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (cleanName.isNotEmpty()) {
                updates["name"] = cleanName
            }
            if (newRole == UserRole.CUSTOMER) {
                updates["permanentlyActivated"] = active
            } else if (active) {
                updates["permanentlyActivated"] = true
            }
            db.collection("users").document(targetUserId).update(updates).await()
            val covRef = db.collection("waiter_coverage").document(targetUserId)
            if (newRole == UserRole.WAITER) {
                val snap = covRef.get().await()
                if (!snap.exists()) {
                    covRef.set(
                        mapOf(
                            "userId" to targetUserId,
                            "waiterName" to cleanName.ifBlank { "Waiter" },
                            "onDuty" to active,
                            "areas" to listOf(WaiterArea.F1.key),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    ).await()
                } else {
                    val covUpdates = mutableMapOf<String, Any>(
                        "onDuty" to active,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                    if (cleanName.isNotEmpty()) {
                        covUpdates["waiterName"] = cleanName
                    }
                    covRef.update(covUpdates).await()
                }
            } else {
                covRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun approveCustomerPermanently(targetUserId: String): Result<Unit> {
        val path = "users/$targetUserId"
        return try {
            requireUserId()
            require(targetUserId.isNotBlank()) { "User ID must not be blank" }
            db.collection("users").document(targetUserId).update(
                mapOf(
                    "role" to UserRole.CUSTOMER.key,
                    "active" to true,
                    "permanentlyActivated" to true,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeWaiterCoverage(uid: String): Flow<Map<String, WaiterCoverage>> = flow {
        if (uid.isBlank()) return@flow
        val path = "waiter_coverage"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(WaiterCoverage::class.java, ServerTimestampBehavior.ESTIMATE)
                        .associateBy { it.userId.ifBlank { it.id } }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun ensureWaiterCoverageForUsers(
        waiterUsers: List<StaffUser>,
        existingCoverage: Map<String, WaiterCoverage>,
    ): Result<Unit> {
        val path = "waiter_coverage"
        return try {
            requireUserId()
            val missing = waiterUsers.filter { w ->
                val key = w.userId.ifBlank { w.id }
                key.isNotBlank() && !existingCoverage.containsKey(key)
            }
            if (missing.isEmpty()) return Result.success(Unit)
            val batch = db.batch()
            missing.forEach { w ->
                val key = w.userId.ifBlank { w.id }
                val ref = db.collection(path).document(key)
                batch.set(
                    ref,
                    mapOf(
                        "userId" to key,
                        "waiterName" to w.name.ifBlank { "Waiter" }.take(60),
                        "onDuty" to w.active,
                        "areas" to listOf(WaiterArea.F1.key),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun setWaiterDuty(
        waiterUserId: String,
        waiterName: String,
        onDuty: Boolean,
        areas: List<String>,
    ): Result<Boolean> {
        val path = "waiter_coverage/$waiterUserId"
        return try {
            requireUserId()
            require(waiterUserId.isNotBlank()) { "Waiter ID must not be blank" }
            val cleanAreas = areas.mapNotNull { WaiterArea.fromKey(it)?.key }.distinct().take(4)
                .ifEmpty { listOf(WaiterArea.F1.key) }
            db.collection("waiter_coverage").document(waiterUserId).set(
                mapOf(
                    "userId" to waiterUserId,
                    "waiterName" to waiterName.ifBlank { "Waiter" }.take(60),
                    "onDuty" to onDuty,
                    "areas" to cleanAreas,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(onDuty)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun setWaiterAreas(
        waiterUserId: String,
        waiterName: String,
        onDuty: Boolean,
        areas: Set<WaiterArea>,
    ): Result<Unit> {
        val path = "waiter_coverage/$waiterUserId"
        return try {
            requireUserId()
            require(waiterUserId.isNotBlank()) { "Waiter ID must not be blank" }
            val cleanAreas = areas.sortedBy { it.index }.map { it.key }.take(4)
            db.collection("waiter_coverage").document(waiterUserId).set(
                mapOf(
                    "userId" to waiterUserId,
                    "waiterName" to waiterName.ifBlank { "Waiter" }.take(60),
                    "onDuty" to onDuty,
                    "areas" to cleanAreas,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun assignOrderWaiter(
        order: Order,
        chosenWaiterId: String,
        chosenWaiterName: String,
        method: String,
        actorRole: UserRole,
        senderBrandName: String,
    ): Result<String> {
        val path = "orders/${order.id}"
        return try {
            val uid = requireUserId()
            require(order.id.isNotBlank()) { "Order ID must not be blank" }
            require(chosenWaiterId.isNotBlank()) { "Waiter ID must not be blank" }
            val cleanMethod = if (method == "manual") "manual" else "auto"

            db.collection("orders").document(order.id).update(
                mapOf(
                    "assignedWaiterId" to chosenWaiterId,
                    "assignedMethod" to cleanMethod,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastUpdatedByRole" to actorRole.key,
                    "lastUpdatedByUid" to uid,
                )
            ).await()

            val displayName = chosenWaiterName.ifBlank { "Waiter" }
            writeAndDispatchNotification(
                mapOf(
                    "senderUid" to uid,
                    "senderRole" to actorRole.key,
                    "senderBrandName" to senderBrandName.ifBlank { actorRole.displayName }.take(30),
                    "message" to "Order #${order.orderNumber} assigned to $displayName · ${order.location}",
                    "targetRoles" to listOf("waiter", "manager"),
                    "targetUserId" to chosenWaiterId,
                    "orderId" to order.id,
                    "kind" to "ping",
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )

            Result.success(displayName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertFcmToken(
        deviceId: String,
        token: String,
        role: UserRole,
        appVersion: String,
        notificationsEnabled: Boolean,
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated with Firebase"))
        val cleanDeviceId = deviceId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        val cleanToken = token.trim().take(512)
        if (cleanDeviceId.isBlank() || cleanToken.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid FCM token or device ID"))
        }
        val path = "users/$uid/fcm_tokens/$cleanDeviceId"
        return try {
            val docRef = db.collection("users").document(uid)
                .collection("fcm_tokens").document(cleanDeviceId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "token" to cleanToken,
                        "role" to role.key,
                        "appVersion" to appVersion.ifBlank { "1.0" }.take(32),
                        "notificationsEnabled" to notificationsEnabled,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "token" to cleanToken,
                        "userId" to uid,
                        "deviceId" to cleanDeviceId,
                        "role" to role.key,
                        "platform" to "android",
                        "appVersion" to appVersion.ifBlank { "1.0" }.take(32),
                        "notificationsEnabled" to notificationsEnabled,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun removeFcmToken(deviceId: String, expectedUid: String? = null): Result<Unit> {
        val uid = expectedUid?.ifBlank { null } ?: auth.currentUser?.uid
            ?: return Result.success(Unit)
        val cleanDeviceId = deviceId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        if (cleanDeviceId.isBlank()) return Result.success(Unit)
        val path = "users/$uid/fcm_tokens/$cleanDeviceId"
        return try {
            db.collection("users").document(uid)
                .collection("fcm_tokens").document(cleanDeviceId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMenuItem(
        itemId: String,
        price: Long? = null,
        available: Boolean? = null,
        imageUrl: String? = null,
    ): Result<Unit> {
        val path = "menu_items/$itemId"
        return try {
            requireUserId()
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (price != null) updates["price"] = price.coerceIn(1L, 100000L)
            if (available != null) updates["available"] = available
            if (imageUrl != null) updates["imageUrl"] = imageUrl.trim().take(1000)
            db.collection("menu_items").document(itemId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeSavedItems(uid: String): Flow<Map<String, SavedMenuItem>> = flow {
        if (uid.isBlank()) return@flow
        val path = "users/$uid/saved_items"
        emitAll(
            db.collection("users").document(uid).collection("saved_items")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SavedMenuItem::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.savedAt ?: Timestamp(0, 0) }
                        .associateBy { it.menuItemId.ifBlank { it.id } }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun saveMenuItemBookmark(uid: String, item: MenuItem): Result<Unit> {
        val path = "users/$uid/saved_items/${item.id}"
        return try {
            val signedInUid = requireUserId()
            require(signedInUid == uid) { "Cannot modify another user's saved items." }
            require(item.id.isNotBlank()) { "Menu item ID must not be blank" }
            db.collection("users").document(uid)
                .collection("saved_items").document(item.id)
                .set(
                    mapOf(
                        "menuItemId" to item.id,
                        "name" to item.name.trim().take(60),
                        "category" to item.category,
                        "price" to item.price.coerceIn(1L, 100000L),
                        "imageUrl" to item.imageUrl.trim().take(1000),
                        "savedAt" to FieldValue.serverTimestamp(),
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun removeMenuItemBookmark(uid: String, menuItemId: String): Result<Unit> {
        val path = "users/$uid/saved_items/$menuItemId"
        return try {
            val signedInUid = requireUserId()
            require(signedInUid == uid) { "Cannot modify another user's saved items." }
            db.collection("users").document(uid)
                .collection("saved_items").document(menuItemId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeTranslations(uid: String): Flow<Map<String, RemoteTranslation>> = flow {
        if (uid.isBlank()) return@flow
        val path = "translations"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        val key = doc.getString("key")?.takeIf { it.isNotBlank() } ?: doc.id
                        val en = doc.getString("en").orEmpty()
                        val am = doc.getString("am").orEmpty()
                        val category = doc.getString("category").orEmpty()
                        val enVersion = (doc.getLong("enVersion") ?: 1L).toInt().coerceAtLeast(1)
                        val updatedBy = doc.getString("updatedBy").orEmpty()
                        val updatedAt = doc.getTimestamp("updatedAt")
                        RemoteTranslation(
                            key = key,
                            category = category,
                            en = en,
                            am = am,
                            enVersion = enVersion,
                            updatedBy = updatedBy,
                            updatedAt = updatedAt,
                        )
                    }.associateBy { it.key }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun saveTranslation(
        key: String,
        category: TranslationCategory,
        en: String,
        am: String,
        enVersion: Int,
    ): Result<Unit> {
        val cleanKey = key.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        val path = "translations/$cleanKey"
        return try {
            val uid = requireUserId()
            require(cleanKey.isNotBlank()) { "Translation key must not be blank." }
            val cleanEn = en.trim().take(400)
            require(cleanEn.isNotEmpty()) { "English text cannot be empty." }
            val cleanAm = am.trim().take(500)
            db.collection("translations").document(cleanKey).set(
                mapOf(
                    "key" to cleanKey,
                    "category" to category.key.take(40),
                    "en" to cleanEn,
                    "am" to cleanAm,
                    "enVersion" to enVersion.coerceAtLeast(1),
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTranslationOverride(key: String): Result<Unit> {
        val cleanKey = key.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        val path = "translations/$cleanKey"
        return try {
            requireUserId()
            db.collection("translations").document(cleanKey).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeReleaseConfig(uid: String): Flow<AppReleaseConfig?> = flow {
        if (uid.isBlank()) return@flow
        val path = "app_config/release"
        emitAll(
            db.collection("app_config").document("release")
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else snap.toObject(AppReleaseConfig::class.java, ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    suspend fun publishReleaseUpdate(
        senderRole: UserRole,
        senderBrandName: String,
        versionCode: Long,
        versionName: String,
        apkDownloadUrl: String,
        releaseNotes: String,
        mandatory: Boolean,
        apkSha256: String = "",
    ): Result<Unit> {
        val path = "app_config/release"
        return try {
            val uid = requireUserId()
            val cleanVerName = versionName.trim().take(32)
            val cleanUrl = apkDownloadUrl.trim().take(1000)
            val cleanSha = apkSha256.trim().lowercase().replace(":", "").replace(" ", "").take(128)
            val cleanNotes = releaseNotes.trim().take(400)
            require(versionCode >= 1L) { "Version code must be >= 1." }
            require(cleanVerName.isNotEmpty()) { "Version name is required." }
            require(cleanUrl.isEmpty() || cleanUrl.startsWith("https://", ignoreCase = true)) {
                "APK download URL must use HTTPS."
            }
            require(cleanSha.isEmpty() || Regex("^[a-f0-9]{64}$").matches(cleanSha)) {
                "APK SHA-256 must be 64 hex characters (or leave blank)."
            }

            db.collection("app_config").document("release").set(
                mapOf(
                    "latestVersionCode" to versionCode.coerceIn(1L, 9999999L),
                    "latestVersionName" to cleanVerName,
                    "apkDownloadUrl" to cleanUrl,
                    "apkSha256" to cleanSha,
                    "releaseNotes" to cleanNotes,
                    "mandatory" to mandatory,
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()

            val msg = buildString {
                append("Lakemark v").append(cleanVerName).append(" update available")
                if (cleanNotes.isNotEmpty()) {
                    append(" — ").append(cleanNotes)
                }
            }.take(300)

            writeAndDispatchNotification(
                mapOf(
                    "senderUid" to uid,
                    "senderRole" to senderRole.key,
                    "senderBrandName" to senderBrandName.ifBlank { senderRole.displayName }.take(30),
                    "message" to msg,
                    "targetRoles" to listOf("all"),
                    "targetUserId" to "",
                    "orderId" to "",
                    "kind" to "app_update",
                    "apkUrl" to cleanUrl,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}

