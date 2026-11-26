package com.example.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.model.AlertTargets
import com.example.data.model.AppNotification
import com.example.data.model.AppReleaseConfig
import com.example.data.model.AppTab
import com.example.data.model.BrandConfig
import com.example.data.model.CartItem
import com.example.data.model.FloorZone
import com.example.data.model.ManageSection
import com.example.data.model.MenuItem
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.PlaceSelection
import com.example.data.model.SavedMenuItem
import com.example.data.model.StaffUser
import com.example.data.model.TeamSubSection
import com.example.data.model.UserRole
import com.example.data.model.WaiterArea
import com.example.data.model.WaiterCoverage
import com.example.data.preferences.UserAlertPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.LakemarkRepository
import com.example.fcm.AppForegroundTracker
import com.example.fcm.FcmRegistrationInfo
import com.example.fcm.FcmTokenManager
import com.example.fcm.InAppPushCoordinator
import com.example.fcm.NotificationDeduplicator
import com.example.fcm.PushChannelCategory
import com.example.fcm.PushDeepLink
import com.example.fcm.PushHistoryStore
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.RemoteTranslation
import com.example.localization.TranslationCatalog
import com.example.localization.TranslationCategory
import com.example.ui.theme.BrandPalette
import com.example.ui.theme.deriveBrandPalette
import com.example.util.AlertAudioAndNotificationManager
import com.example.util.AlertSoundKind
import com.example.util.ImageValidationError
import com.example.util.MediaImageUploader
import com.example.util.MediaUploadException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToLong

data class AuthSession(val firebaseUser: FirebaseUser) {
    val sessionKey: String = "google_${firebaseUser.uid}"
    val uid: String = firebaseUser.uid
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class OrdersFilterTab { ACTIVE, FINISHED }
enum class OrdersZoneFilter(val key: String, val label: String) {
    ALL("all", "All"),
    ROOM("room", "Rooms"),
    DINE("dine", "Restaurant"),
    PICKUP("pickup", "Reception"),
    OUT("out", "Takeaway"),
}

data class ToastBanner(
    val id: Long,
    val message: String,
    val senderRole: UserRole,
    val brandName: String,
    val deepLink: PushDeepLink? = null,
)

data class LakemarkUiState(
    val currentUser: StaffUser? = null,
    val assignedRole: UserRole = UserRole.CUSTOMER,
    val activeRole: UserRole = UserRole.CUSTOMER,
    val isAdminUser: Boolean = false,
    val isGoogleCustomerSession: Boolean = false,
    val isVerifyingAccount: Boolean = false,
    val accountDisabled: Boolean = false,
    val rejectedGoogleSetupMessage: String? = null,
    val language: AppLanguage = AppLanguage.EN,
    val remoteTranslations: Map<String, RemoteTranslation> = emptyMap(),
    val strings: AppStrings = TranslationCatalog.defaultEnglishStrings(),
    val savedItems: Map<String, SavedMenuItem> = emptyMap(),
    val releaseConfig: AppReleaseConfig? = null,
    val showAppUpdateDialog: Boolean = false,
    val isUploadingMedia: Boolean = false,
    val uploadingMenuItemId: String? = null,
    val currentTab: AppTab = AppTab.HOME,
    val selectedZone: FloorZone = FloorZone.HOTEL,
    val selectedPlace: PlaceSelection? = null,
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val cart: List<CartItem> = emptyList(),
    val ordersFilter: OrdersFilterTab = OrdersFilterTab.ACTIVE,
    val ordersZoneFilter: OrdersZoneFilter = OrdersZoneFilter.ALL,
    val kitchenStageFilter: OrderStatus = OrderStatus.CONFIRMED,
    val manageSection: ManageSection = ManageSection.OVERVIEW,
    val teamSubSection: TeamSubSection = TeamSubSection.ACCOUNTS,
    val menuItemsState: UiState<List<MenuItem>> = UiState.Loading,
    val ordersState: UiState<List<Order>> = UiState.Loading,
    val staffUsers: List<StaffUser> = emptyList(),
    val waiterUsers: List<StaffUser> = emptyList(),
    val waiterCoverage: Map<String, WaiterCoverage> = emptyMap(),
    val brands: Map<UserRole, BrandPalette> = UserRole.entries.associateWith { deriveBrandPalette(it.defaultBrandConfig()) },
    val brandConfigs: Map<UserRole, BrandConfig> = UserRole.entries.associateWith { it.defaultBrandConfig() },
    val notifications: List<AppNotification> = emptyList(),
    val pushBrandOverrides: Map<String, BrandPalette> = emptyMap(),
    val unreadNotificationCount: Int = 0,
    val fcmRegistrationInfo: FcmRegistrationInfo = FcmRegistrationInfo(),
    val preferences: UserAlertPreferences = UserAlertPreferences(),
    val activeToast: ToastBanner? = null,
    val showCartSheet: Boolean = false,
    val showNotificationsSheet: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val editingBrandForRole: UserRole? = null,
    val cancellingOrder: Order? = null,
    val assigningWaiterOrder: Order? = null,
    val dismissedReadyOrderIds: Set<String> = emptySet(),
    val highlightedOrderId: String? = null,
    val isSubmittingOrder: Boolean = false,
) {
    val activePalette: BrandPalette
        get() = brands[activeRole] ?: deriveBrandPalette(activeRole.defaultBrandConfig())

    val cartItemCount: Int get() = cart.sumOf { it.quantity }
    val cartSubtotal: Long get() = cart.sumOf { it.menuItem.price * it.quantity.toLong() }
    val cartVat: Long get() = (cartSubtotal * 0.15).roundToLong()
    val cartTotal: Long get() = cartSubtotal + cartVat

    val orders: List<Order> get() = (ordersState as? UiState.Success)?.data ?: emptyList()
    val menuItems: List<MenuItem> get() = (menuItemsState as? UiState.Success)?.data ?: emptyList()
    val hasUpdateAvailable: Boolean
        get() = (releaseConfig?.latestVersionCode ?: 0L) > BuildConfig.VERSION_CODE.toLong()

    fun waiterName(userId: String): String {
        if (userId.isBlank()) return ""
        return waiterUsers.firstOrNull { it.userId == userId }?.name
            ?: staffUsers.firstOrNull { it.userId == userId }?.name
            ?: waiterCoverage[userId]?.waiterName?.ifBlank { null }
            ?: strings.roleName(UserRole.WAITER)
    }

    val pendingKitchenOrders: List<Order>
        get() = if (activeRole == UserRole.KITCHEN) {
            orders.filter { it.statusEnum == OrderStatus.NEW || it.statusEnum == OrderStatus.CONFIRMED }
                .sortedBy { it.createdAt?.seconds ?: 0L }
        } else {
            emptyList()
        }

    val readyPickupOrders: List<Order>
        get() = if (activeRole in listOf(UserRole.RECEPTION, UserRole.WAITER, UserRole.CASHIER, UserRole.DELIVERY)) {
            orders.filter {
                it.statusEnum == OrderStatus.READY &&
                    it.id !in dismissedReadyOrderIds &&
                    (activeRole != UserRole.DELIVERY || it.typeEnum == OrderType.OUT) &&
                    (activeRole != UserRole.WAITER || (it.typeEnum != OrderType.OUT && (it.assignedWaiterId.isBlank() || it.assignedWaiterId == currentUser?.userId)))
            }
        } else {
            emptyList()
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LakemarkViewModel(
    private val repository: LakemarkRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val alertManager: AlertAudioAndNotificationManager,
    private val pushHistoryStore: PushHistoryStore,
    private val deduplicator: NotificationDeduplicator,
    private val fcmTokenManager: FcmTokenManager,
    private val session: AuthSession,
    private val mediaUploader: MediaImageUploader? = null,
) : ViewModel() {

    private val uid: String = session.uid
    private val initialRole: UserRole = run {
        val email = session.firebaseUser.email?.lowercase()
        if (email == "abebaw4040@gmail.com" && session.firebaseUser.isEmailVerified) {
            UserRole.MANAGER
        } else {
            UserRole.CUSTOMER
        }
    }

    private val _localUi = MutableStateFlow(LocalUiState())
    private val sessionStartEpochMs: Long = System.currentTimeMillis()
    private val alertedOrderKeys = mutableSetOf<String>()
    private val chimedPopupOrderKeys = mutableSetOf<String>()
    private val seenNotificationIds = mutableSetOf<String>()
    private var lastPromptedReleaseVersionCode: Long = 0L

    private data class LocalUiState(
        val currentTab: AppTab = AppTab.HOME,
        val selectedZone: FloorZone = FloorZone.HOTEL,
        val selectedPlace: PlaceSelection? = null,
        val selectedCategory: String = "All",
        val searchQuery: String = "",
        val cart: List<CartItem> = emptyList(),
        val ordersFilter: OrdersFilterTab = OrdersFilterTab.ACTIVE,
        val ordersZoneFilter: OrdersZoneFilter = OrdersZoneFilter.ALL,
        val kitchenStageFilter: OrderStatus = OrderStatus.CONFIRMED,
        val manageSection: ManageSection = ManageSection.OVERVIEW,
        val teamSubSection: TeamSubSection = TeamSubSection.ACCOUNTS,
        val activeToast: ToastBanner? = null,
        val showCartSheet: Boolean = false,
        val showNotificationsSheet: Boolean = false,
        val showSettingsSheet: Boolean = false,
        val showAppUpdateDialog: Boolean = false,
        val isUploadingMedia: Boolean = false,
        val uploadingMenuItemId: String? = null,
        val editingBrandForRole: UserRole? = null,
        val cancellingOrder: Order? = null,
        val assigningWaiterOrder: Order? = null,
        val dismissedReadyOrderIds: Set<String> = emptySet(),
        val highlightedOrderId: String? = null,
        val isSubmittingOrder: Boolean = false,
        val rejectedGoogleSetupMessage: String? = null,
    )

    private fun unwrapFirestoreError(error: Throwable, tag: String): UiState.Error {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(tag, "Stream error", rootError)
        return UiState.Error(rootError.localizedMessage ?: "Unable to sync live data")
    }

    private val currentUserFlow: StateFlow<StaffUser?> = repository.observeCurrentUser(uid)
        .catch { error ->
            unwrapFirestoreError(error, "CurrentUser")
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val prefsFlow: StateFlow<UserAlertPreferences> = prefsRepo.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserAlertPreferences())

    private val effectiveRoleFlow: StateFlow<Pair<UserRole, Boolean>> = combine(currentUserFlow, prefsFlow) { profile, prefs ->
        val assignedRole = profile?.userRole ?: initialRole
        val isAdmin = assignedRole == UserRole.MANAGER
        val activeRole = if (isAdmin) UserRole.fromKey(prefs.preferredRoleKey) else assignedRole
        activeRole to isAdmin
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        initialRole to (initialRole == UserRole.MANAGER),
    )

    private val isActivatedOrAdminFlow: Flow<Triple<Boolean, UserRole, Boolean>> =
        combine(currentUserFlow, effectiveRoleFlow) { profile, (role, isAdmin) ->
            Triple(isAdmin || (profile?.active == true), role, isAdmin)
        }.distinctUntilChanged()

    private val menuItemsFlow: StateFlow<UiState<List<MenuItem>>> = isActivatedOrAdminFlow
        .flatMapLatest { (allowed, role, isAdmin) ->
            if (!allowed) {
                flowOf(UiState.Loading)
            } else {
                repository.observeMenuItems(uid, isStaffOrAdmin = role.isStaff || isAdmin)
                    .map<List<MenuItem>, UiState<List<MenuItem>>> { UiState.Success(it) }
                    .catch { emit(unwrapFirestoreError(it, "MenuItems")) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val ordersFlow: StateFlow<UiState<List<Order>>> = isActivatedOrAdminFlow
        .flatMapLatest { (allowed, role, isAdmin) ->
            if (!allowed) {
                flowOf(UiState.Loading)
            } else {
                val isStaffOrAdmin = role.isStaff || isAdmin
                repository.observeOrders(uid, isStaffOrAdmin = isStaffOrAdmin)
                    .map<List<Order>, UiState<List<Order>>> { list ->
                        val filtered = when (role) {
                            UserRole.CUSTOMER -> list.filter { it.userId == uid }
                            UserRole.DELIVERY -> list.filter { it.typeEnum == OrderType.OUT }
                            UserRole.WAITER -> list.filter { it.typeEnum != OrderType.OUT && (it.assignedWaiterId.isBlank() || it.assignedWaiterId == uid) }
                            else -> list
                        }
                        UiState.Success(filtered)
                    }
                    .catch { emit(unwrapFirestoreError(it, "Orders")) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val baseNotificationsFlow: Flow<List<AppNotification>> = isActivatedOrAdminFlow
        .flatMapLatest { (allowed, role, isAdmin) ->
            if (!allowed) {
                flowOf(emptyList())
            } else {
                repository.observeNotifications(uid, isStaffOrAdmin = role.isStaff || isAdmin)
                    .catch {
                        unwrapFirestoreError(it, "Notifications")
                        emit(emptyList())
                    }
            }
        }

    private val mergedPushAndBaseNotificationsFlow: StateFlow<Pair<List<AppNotification>, Map<String, BrandPalette>>> =
        combine(
            baseNotificationsFlow,
            pushHistoryStore.historyEntries,
            effectiveRoleFlow,
        ) { baseList, pushEntries, (role, _) ->
            val filteredBase = baseList.filter { n ->
                when (role) {
                    UserRole.MANAGER -> true
                    UserRole.CUSTOMER -> n.targetUserId == uid || n.targetRoles.contains("all") || n.targetRoles.contains(UserRole.CUSTOMER.key)
                    UserRole.DELIVERY -> n.targetRoles.contains("all") || n.targetRoles.contains(UserRole.DELIVERY.key) || n.senderRole == role.key
                    else -> n.targetRoles.contains("all") || n.targetRoles.contains(role.key) || n.senderRole == role.key || n.targetUserId == uid
                }
            }

            val relevantPushEntries = pushEntries.filter { entry ->
                val n = entry.notification
                when (role) {
                    UserRole.MANAGER -> true
                    UserRole.CUSTOMER -> n.targetUserId == uid || (n.targetUserId.isBlank() && (n.targetRoles.contains("all") || n.targetRoles.contains(UserRole.CUSTOMER.key)))
                    else -> n.targetRoles.contains("all") || n.targetRoles.contains(role.key) || n.targetUserId == uid
                }
            }

            val brandOverrides = relevantPushEntries.associate { entry ->
                entry.notification.id to deriveBrandPalette(entry.brandConfig)
            }

            val merged = (filteredBase + relevantPushEntries.map { it.notification })
                .distinctBy { it.id.ifBlank { "${it.orderId}:${it.kind}:${it.message}" } }
                .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                .take(40)

            merged to brandOverrides
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList<AppNotification>() to emptyMap())

    private val brandsFlow: StateFlow<Map<UserRole, BrandConfig>> = isActivatedOrAdminFlow
        .flatMapLatest { (allowed, _, _) ->
            if (!allowed) {
                flowOf(UserRole.entries.associateWith { it.defaultBrandConfig() })
            } else {
                repository.observeBrands(uid)
                    .catch {
                        unwrapFirestoreError(it, "Brands")
                        emit(UserRole.entries.associateWith { r -> r.defaultBrandConfig() })
                    }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            UserRole.entries.associateWith { it.defaultBrandConfig() },
        )

    private val staffUsersFlow: StateFlow<List<StaffUser>> = isActivatedOrAdminFlow
        .flatMapLatest { (allowed, _, isAdmin) ->
            if (allowed && isAdmin) {
                repository.observeAllUsers(uid).catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val waiterCoverageFlow: StateFlow<Map<String, WaiterCoverage>> = isActivatedOrAdminFlow
        .flatMapLatest { (allowed, role, isAdmin) ->
            if (allowed && (role.isStaff || isAdmin)) {
                repository.observeWaiterCoverage(uid).catch { emit(emptyMap()) }
            } else {
                flowOf(emptyMap())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    private val waiterUsersFlow: StateFlow<List<StaffUser>> = combine(staffUsersFlow, waiterCoverageFlow) { users, coverage ->
        val byId = linkedMapOf<String, StaffUser>()
        users.filter { it.userRole == UserRole.WAITER }.forEach { waiter ->
            val key = waiter.userId.ifBlank { waiter.id }
            if (key.isNotBlank()) byId[key] = waiter
        }
        coverage.values.forEach { cov ->
            val key = cov.userId.ifBlank { cov.id }
            if (key.isNotBlank() && key !in byId) {
                byId[key] = StaffUser(
                    id = key,
                    userId = key,
                    name = cov.waiterName.ifBlank { "Waiter" },
                    role = UserRole.WAITER.key,
                    active = true,
                )
            }
        }
        byId.values.sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val remoteTranslationsFlow: StateFlow<Map<String, RemoteTranslation>> = repository
        .observeTranslations(uid)
        .catch { emit(emptyMap()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    private val savedItemsFlow: StateFlow<Map<String, SavedMenuItem>> = isActivatedOrAdminFlow
        .flatMapLatest { (allowed, _, _) ->
            if (!allowed) flowOf(emptyMap())
            else repository.observeSavedItems(uid).catch { emit(emptyMap()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    private val releaseConfigFlow: StateFlow<AppReleaseConfig?> = isActivatedOrAdminFlow
        .flatMapLatest { (allowed, _, _) ->
            if (!allowed) flowOf(null)
            else repository.observeReleaseConfig(uid).catch { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private data class CoreStreams(
        val local: LocalUiState,
        val user: StaffUser?,
        val roleAndAdmin: Pair<UserRole, Boolean>,
        val prefs: UserAlertPreferences,
        val menu: UiState<List<MenuItem>>,
    )

    private data class StaffStreams(
        val staffUsers: List<StaffUser>,
        val waiterUsers: List<StaffUser>,
        val waiterCoverage: Map<String, WaiterCoverage>,
    )

    private data class LocalizationAndExtras(
        val remoteTranslations: Map<String, RemoteTranslation>,
        val savedItems: Map<String, SavedMenuItem>,
        val releaseConfig: AppReleaseConfig?,
    )

    private data class SecondaryStreams(
        val orders: UiState<List<Order>>,
        val mergedNotifications: Pair<List<AppNotification>, Map<String, BrandPalette>>,
        val brands: Map<UserRole, BrandConfig>,
        val staff: StaffStreams,
        val extras: LocalizationAndExtras,
    )

    private val staffStreamsFlow = combine(
        staffUsersFlow,
        waiterUsersFlow,
        waiterCoverageFlow,
        ::StaffStreams,
    )

    private val localizationAndExtrasFlow = combine(
        remoteTranslationsFlow,
        savedItemsFlow,
        releaseConfigFlow,
        ::LocalizationAndExtras,
    )

    private val secondaryStreamsFlow = combine(
        ordersFlow,
        mergedPushAndBaseNotificationsFlow,
        brandsFlow,
        staffStreamsFlow,
        localizationAndExtrasFlow,
        ::SecondaryStreams,
    )

    val uiState: StateFlow<LakemarkUiState> = combine(
        combine(_localUi, currentUserFlow, effectiveRoleFlow, prefsFlow, menuItemsFlow, ::CoreStreams),
        secondaryStreamsFlow,
        pushHistoryStore.registrationInfo,
    ) { core, sec, fcmRegistration ->
        val (activeRole, isAdmin) = core.roleAndAdmin
        val assignedRole = core.user?.userRole ?: initialRole
        val allowedTabs = activeRole.allowedTabs
        val safeTab = if (core.local.currentTab in allowedTabs) core.local.currentTab else AppTab.HOME
        val isVerifying = core.user == null && !isAdmin && core.local.rejectedGoogleSetupMessage == null
        val disabled = core.user != null && !core.user.active && !isAdmin
        val (notifList, pushOverrides) = sec.mergedNotifications
        val unread = notifList.count { n ->
            val ms = n.createdAt?.toDate()?.time ?: 0L
            ms > core.prefs.lastNotifReadAtMs && (n.targetRoles.contains("all") || n.targetRoles.contains(activeRole.key) || n.targetUserId == uid)
        }
        val palettes = sec.brands.mapValues { (_, cfg) -> deriveBrandPalette(cfg) }
        val fallbackCustomer = StaffUser(
            id = uid,
            userId = uid,
            email = session.firebaseUser.email.orEmpty(),
            name = session.firebaseUser.displayName?.ifBlank { null }
                ?: session.firebaseUser.email?.substringBefore("@")
                ?: "Guest",
            photoUrl = session.firebaseUser.photoUrl?.toString().orEmpty(),
            role = initialRole.key,
            active = isAdmin,
            permanentlyActivated = isAdmin,
        )
        val strictlyCustomerSession = !isAdmin && !activeRole.isStaff
        val language = AppLanguage.fromCode(core.prefs.languageCode)
        val cachedOverrides = TranslationCatalog.deserializeOverrides(core.prefs.cachedTranslationsJson)
        val mergedOverrides = cachedOverrides + sec.extras.remoteTranslations
        val resolvedStrings = TranslationCatalog.buildStrings(language, mergedOverrides)

        LakemarkUiState(
            currentUser = core.user ?: fallbackCustomer,
            assignedRole = assignedRole,
            activeRole = activeRole,
            isAdminUser = isAdmin,
            isGoogleCustomerSession = strictlyCustomerSession,
            isVerifyingAccount = isVerifying,
            accountDisabled = disabled,
            rejectedGoogleSetupMessage = core.local.rejectedGoogleSetupMessage,
            language = language,
            remoteTranslations = mergedOverrides,
            strings = resolvedStrings,
            savedItems = sec.extras.savedItems,
            releaseConfig = sec.extras.releaseConfig,
            showAppUpdateDialog = core.local.showAppUpdateDialog,
            isUploadingMedia = core.local.isUploadingMedia,
            uploadingMenuItemId = core.local.uploadingMenuItemId,
            currentTab = safeTab,
            selectedZone = core.local.selectedZone,
            selectedPlace = core.local.selectedPlace,
            selectedCategory = core.local.selectedCategory,
            searchQuery = core.local.searchQuery,
            cart = core.local.cart,
            ordersFilter = core.local.ordersFilter,
            ordersZoneFilter = core.local.ordersZoneFilter,
            kitchenStageFilter = core.local.kitchenStageFilter,
            manageSection = core.local.manageSection,
            teamSubSection = core.local.teamSubSection,
            menuItemsState = core.menu,
            ordersState = sec.orders,
            staffUsers = sec.staff.staffUsers,
            waiterUsers = sec.staff.waiterUsers,
            waiterCoverage = sec.staff.waiterCoverage,
            brands = palettes,
            brandConfigs = sec.brands,
            notifications = notifList,
            pushBrandOverrides = pushOverrides,
            unreadNotificationCount = unread,
            fcmRegistrationInfo = fcmRegistration,
            preferences = core.prefs,
            activeToast = core.local.activeToast,
            showCartSheet = core.local.showCartSheet,
            showNotificationsSheet = core.local.showNotificationsSheet,
            showSettingsSheet = core.local.showSettingsSheet,
            editingBrandForRole = core.local.editingBrandForRole,
            cancellingOrder = core.local.cancellingOrder,
            assigningWaiterOrder = core.local.assigningWaiterOrder,
            dismissedReadyOrderIds = core.local.dismissedReadyOrderIds,
            highlightedOrderId = core.local.highlightedOrderId,
            isSubmittingOrder = core.local.isSubmittingOrder,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LakemarkUiState())

    init {
        bootstrapSession()
        observeWaiterCoverageBootstrap()
        observeRealTimeAlerts()
        observeIncomingNotifications()
        observeFcmRegistration()
        observeForegroundPushEvents()
        observePendingDeepLinks()
        observeTranslationCacheSync()
        observeReleaseUpdatesAutoPrompt()
    }

    private fun observeReleaseUpdatesAutoPrompt() {
        viewModelScope.launch {
            releaseConfigFlow.collect { cfg ->
                val state = uiState.value
                if (!state.isAdminUser && state.currentUser?.active != true) return@collect
                val latestCode = cfg?.latestVersionCode ?: 0L
                if (latestCode > BuildConfig.VERSION_CODE.toLong() && latestCode > lastPromptedReleaseVersionCode) {
                    lastPromptedReleaseVersionCode = latestCode
                    _localUi.update { it.copy(showAppUpdateDialog = true) }
                }
            }
        }
    }

    fun onAppReopened() {
        val state = uiState.value
        if (!state.isAdminUser && state.currentUser?.active != true) return
        val cfg = releaseConfigFlow.value ?: state.releaseConfig ?: return
        if (cfg.latestVersionCode > BuildConfig.VERSION_CODE.toLong() && !_localUi.value.showAppUpdateDialog) {
            lastPromptedReleaseVersionCode = cfg.latestVersionCode
            _localUi.update { it.copy(showAppUpdateDialog = true) }
        }
    }

    private fun observeTranslationCacheSync() {
        viewModelScope.launch {
            remoteTranslationsFlow.collect { remote ->
                if (remote.isNotEmpty()) {
                    val serialized = TranslationCatalog.serializeOverrides(remote)
                    if (serialized != prefsFlow.value.cachedTranslationsJson) {
                        prefsRepo.setCachedTranslationsJson(serialized)
                    }
                }
            }
        }
    }

    private fun bootstrapSession() {
        viewModelScope.launch {
            repository.ensureUserProfile(session.firebaseUser)
                .onSuccess { user ->
                    if (user.userRole == UserRole.MANAGER || initialRole == UserRole.MANAGER) {
                        repository.seedDefaultMenuIfEmpty()
                        val seedRes = repository.seedDefaultStaffIfEmpty()
                        val syncRes = repository.syncExistingStaffAuthAccounts()
                        val authError = seedRes.exceptionOrNull() ?: syncRes.exceptionOrNull()
                        if (authError != null) {
                            showToast(
                                authError.localizedMessage ?: "Could not sync staff logins to Firebase Auth.",
                            )
                        } else if ((seedRes.getOrNull() ?: 0) > 0 || (syncRes.getOrNull() ?: 0) > 0) {
                            showToast("Staff logins synced and ready for sign-in.")
                        }
                    }
                }
                .onFailure { err ->
                    if (err is SecurityException) {
                        val lang = AppLanguage.fromCode(
                            prefsFlow.value.languageCode.ifBlank { prefsRepo.getQuickLanguageCode() },
                        )
                        val overrides = TranslationCatalog.deserializeOverrides(
                            prefsFlow.value.cachedTranslationsJson,
                        ) + remoteTranslationsFlow.value
                        val msg = TranslationCatalog.buildStrings(lang, overrides).get(
                            "auth_manager_only_error",
                            "Only an authorized Manager / Admin account can sign in with Google. Staff must use Staff Sign In.",
                        )
                        _localUi.update { it.copy(rejectedGoogleSetupMessage = msg) }
                    }
                }
        }
    }

    private fun observeWaiterCoverageBootstrap() {
        viewModelScope.launch {
            combine(staffUsersFlow, waiterCoverageFlow, effectiveRoleFlow) { users, coverage, (_, isAdmin) ->
                Triple(users, coverage, isAdmin)
            }.collect { (users, coverage, isAdmin) ->
                if (isAdmin && users.isNotEmpty()) {
                    val waiters = users.filter { it.userRole == UserRole.WAITER }
                    val missing = waiters.filter { w ->
                        val id = w.userId.ifBlank { w.id }
                        id.isNotBlank() && !coverage.containsKey(id)
                    }
                    if (missing.isNotEmpty()) {
                        repository.ensureWaiterCoverageForUsers(missing, coverage)
                    }
                }
            }
        }
    }

    private fun observeFcmRegistration() {
        viewModelScope.launch {
            combine(currentUserFlow, effectiveRoleFlow) { profile, (role, isAdmin) ->
                Triple(profile, role, isAdmin)
            }
                .distinctUntilChanged()
                .collect { (profile, role, isAdmin) ->
                    if (isAdmin || profile?.active == true) {
                        fcmTokenManager.syncSessionPushRegistration(
                            uid = uid,
                            role = role,
                            isFirebaseUser = true,
                        )
                    } else if (profile != null && !profile.active) {
                        fcmTokenManager.unregisterOnSignOut(expectedFirebaseUid = uid)
                    }
                }
        }
    }

    fun refreshFcmRegistration() {
        val state = uiState.value
        if (!state.isAdminUser && state.currentUser?.active != true) return
        val role = state.activeRole
        viewModelScope.launch {
            fcmTokenManager.syncSessionPushRegistration(
                uid = uid,
                role = role,
                isFirebaseUser = true,
            )
        }
    }

    private fun observeForegroundPushEvents() {
        viewModelScope.launch {
            InAppPushCoordinator.foregroundEvents.collect { push ->
                val state = uiState.value
                if (!state.isAdminUser && state.currentUser?.active != true) return@collect
                val prefs = state.preferences
                val soundKind = when (push.kind) {
                    "new" -> AlertSoundKind.NEW_ORDER
                    "ready" -> AlertSoundKind.ORDER_READY
                    else -> AlertSoundKind.PING
                }
                alertManager.triggerAlert(
                    kind = soundKind,
                    soundEnabled = prefs.soundEnabled,
                    vibrationEnabled = prefs.vibrationEnabled,
                    volume = prefs.volume,
                )
                showToast(
                    message = push.body,
                    senderRole = push.senderRole,
                    brandNameOverride = push.senderBrandName,
                    deepLink = push.deepLink,
                )
            }
        }
    }

    private fun observePendingDeepLinks() {
        viewModelScope.launch {
            combine(
                isActivatedOrAdminFlow,
                InAppPushCoordinator.pendingDeepLink,
            ) { (allowed, role, _), link ->
                if (allowed && link != null) role to link else null
            }
                .filterNotNull()
                .collect { (role, link) ->
                    handleDeepLink(link, activeRoleOverride = role)
                    InAppPushCoordinator.consumeDeepLink()
                }
        }
    }

    fun handleDeepLink(link: PushDeepLink, activeRoleOverride: UserRole? = null) {
        val state = uiState.value
        val activeRole = activeRoleOverride ?: state.activeRole
        if (link.kind == "app_update") {
            _localUi.update {
                it.copy(
                    showAppUpdateDialog = true,
                    showNotificationsSheet = false,
                    showSettingsSheet = false,
                    showCartSheet = false,
                )
            }
            viewModelScope.launch {
                prefsRepo.setLastNotifReadAtMs(System.currentTimeMillis())
            }
            return
        }
        if (link.openNotificationsSheet && link.orderId.isBlank()) {
            _localUi.update {
                it.copy(
                    showNotificationsSheet = true,
                    showSettingsSheet = false,
                    showCartSheet = false,
                )
            }
            viewModelScope.launch {
                prefsRepo.setLastNotifReadAtMs(System.currentTimeMillis())
            }
            return
        }

        val resolvedTab = link.resolveTargetTab(activeRole)
        val matchedOrder = if (link.orderId.isNotBlank()) {
            state.orders.firstOrNull { it.id == link.orderId }
        } else {
            null
        }
        val targetOrderStatus = matchedOrder?.statusEnum
            ?: link.orderStatus.takeIf { it.isNotBlank() }?.let { OrderStatus.fromKey(it) }

        val targetOrdersFilter = if (targetOrderStatus?.isTerminal == true) {
            OrdersFilterTab.FINISHED
        } else {
            OrdersFilterTab.ACTIVE
        }
        val targetKitchenStage = when (targetOrderStatus) {
            OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.READY -> targetOrderStatus
            else -> state.kitchenStageFilter
        }

        _localUi.update {
            it.copy(
                currentTab = resolvedTab,
                highlightedOrderId = link.orderId.ifBlank { it.highlightedOrderId },
                ordersFilter = targetOrdersFilter,
                kitchenStageFilter = targetKitchenStage,
                showNotificationsSheet = false,
                showSettingsSheet = false,
                showCartSheet = false,
            )
        }
    }

    fun openNotificationItem(notif: AppNotification) {
        if (notif.kind == "app_update") {
            _localUi.update {
                it.copy(
                    showNotificationsSheet = false,
                    showAppUpdateDialog = true,
                )
            }
            return
        }
        val order = uiState.value.orders.firstOrNull { it.id == notif.orderId }
        val targetTab = when {
            uiState.value.activeRole == UserRole.KITCHEN && notif.orderId.isNotBlank() -> AppTab.KITCHEN
            notif.orderId.isNotBlank() && AppTab.ORDERS in uiState.value.activeRole.allowedTabs -> AppTab.ORDERS
            else -> AppTab.HOME
        }
        handleDeepLink(
            PushDeepLink(
                tab = targetTab,
                orderId = notif.orderId,
                notifId = notif.id,
                openNotificationsSheet = notif.orderId.isBlank(),
                kind = notif.kind,
                orderStatus = order?.status.orEmpty(),
                apkUrl = notif.apkUrl,
            )
        )
    }

    private fun dispatchOrderAlertUi(
        soundKind: AlertSoundKind,
        toastMessage: String,
        notifTitle: String,
        notifBody: String,
        notificationId: Int,
        brandConfig: BrandConfig,
        senderRole: UserRole,
        deepLink: PushDeepLink,
        channelCategory: PushChannelCategory,
        prefs: UserAlertPreferences,
    ) {
        val isForeground = AppForegroundTracker.isAppInForeground()
        if (isForeground) {
            alertManager.triggerAlert(
                soundKind,
                prefs.soundEnabled,
                prefs.vibrationEnabled,
                prefs.volume,
            )
            showToast(
                message = toastMessage,
                senderRole = senderRole,
                brandNameOverride = brandConfig.brandName,
                deepLink = deepLink,
            )
        } else {
            val posted = alertManager.showLocalNotification(
                title = notifTitle,
                body = notifBody,
                notificationId = notificationId,
                brandConfig = brandConfig,
                senderRole = senderRole,
                deepLink = deepLink,
                channelCategory = channelCategory,
            )
            val channelHasSound = posted && alertManager.isChannelSoundEnabled(channelCategory.channelId)
            if ((prefs.soundEnabled && !channelHasSound) || (prefs.vibrationEnabled && !posted)) {
                alertManager.triggerAlert(
                    kind = soundKind,
                    soundEnabled = prefs.soundEnabled && !channelHasSound,
                    vibrationEnabled = prefs.vibrationEnabled && !posted,
                    volume = prefs.volume,
                )
            }
        }
    }

    fun notifyPopupCardVisible(order: Order, kind: AlertSoundKind, forceRepeat: Boolean = false) {
        val key = "${order.id}:${order.status}"
        if (!forceRepeat && !chimedPopupOrderKeys.add(key)) return
        val prefs = uiState.value.preferences
        alertManager.triggerAlert(
            kind = kind,
            soundEnabled = prefs.soundEnabled,
            vibrationEnabled = prefs.vibrationEnabled,
            volume = prefs.volume,
            forceForegroundStream = true,
        )
    }

    private fun observeRealTimeAlerts() {
        viewModelScope.launch {
            combine(currentUserFlow, effectiveRoleFlow) { profile, (role, isAdmin) ->
                Triple(isAdmin || (profile?.active == true), role, isAdmin)
            }
                .filter { (allowed, _, _) -> allowed }
                .distinctUntilChanged()
                .flatMapLatest { (_, role, isAdmin) ->
                    var isFirstSnapshot = true
                    repository.observeOrders(uid, isStaffOrAdmin = role.isStaff || isAdmin)
                        .catch { emit(emptyList()) }
                        .map { rawList ->
                            val filtered = when (role) {
                                UserRole.CUSTOMER -> rawList.filter { it.userId == uid }
                                UserRole.DELIVERY -> rawList.filter { it.typeEnum == OrderType.OUT }
                                UserRole.WAITER -> rawList.filter {
                                    it.typeEnum != OrderType.OUT &&
                                        (it.assignedWaiterId.isBlank() || it.assignedWaiterId == uid)
                                }
                                else -> rawList
                            }
                            val first = isFirstSnapshot
                            isFirstSnapshot = false
                            Triple(role, first, filtered)
                        }
                }
                .collect { (role, isFirst, list) ->
                    if (isFirst) {
                        list.forEach { alertedOrderKeys.add("${it.id}:${it.status}") }
                        return@collect
                    }
                    val prefs = prefsFlow.value
                    for (order in list) {
                        val key = "${order.id}:${order.status}"
                        if (!alertedOrderKeys.add(key)) continue
                        val eventMs = (order.updatedAt ?: order.createdAt)?.toDate()?.time ?: 0L
                        if (eventMs in 1L until (sessionStartEpochMs - 180_000L)) continue

                        val status = order.statusEnum
                        val shouldNotifyNewReception = (role == UserRole.RECEPTION || role == UserRole.MANAGER) &&
                            status == OrderStatus.NEW &&
                            order.lastUpdatedByUid != uid
                        val shouldNotifyKitchen = role == UserRole.KITCHEN &&
                            (status == OrderStatus.NEW || status == OrderStatus.CONFIRMED) &&
                            order.lastUpdatedByUid != uid
                        val shouldNotifyReadyStaff = role in listOf(
                            UserRole.RECEPTION,
                            UserRole.WAITER,
                            UserRole.CASHIER,
                            UserRole.DELIVERY,
                            UserRole.MANAGER,
                        ) &&
                            status == OrderStatus.READY &&
                            order.lastUpdatedByUid != uid &&
                            (role != UserRole.DELIVERY || order.typeEnum == OrderType.OUT) &&
                            (role != UserRole.WAITER || (order.typeEnum != OrderType.OUT && (order.assignedWaiterId.isBlank() || order.assignedWaiterId == uid)))
                        val shouldNotifyCustomer = role == UserRole.CUSTOMER &&
                            order.userId == uid &&
                            order.lastUpdatedByUid != uid &&
                            status in listOf(OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.CANCELLED)

                        if ((shouldNotifyNewReception || shouldNotifyKitchen || shouldNotifyReadyStaff || shouldNotifyCustomer) &&
                            deduplicator.shouldProcessOrderAlert(order.id, status)
                        ) {
                            when {
                                shouldNotifyNewReception -> {
                                    val senderRole = order.createdByRoleEnum
                                    val brandCfg = uiState.value.brandConfigs[senderRole] ?: senderRole.defaultBrandConfig()
                                    if (AppForegroundTracker.isAppInForeground()) {
                                        chimedPopupOrderKeys.add(key)
                                    }
                                    dispatchOrderAlertUi(
                                        soundKind = AlertSoundKind.NEW_ORDER,
                                        toastMessage = "New order #${order.orderNumber} · ${order.location}",
                                        notifTitle = "Reception · New order #${order.orderNumber}",
                                        notifBody = "${order.typeEnum.label} · ${order.location} · ${order.customerName}",
                                        notificationId = order.orderNumber.toInt() + 500,
                                        brandConfig = brandCfg,
                                        senderRole = senderRole,
                                        deepLink = PushDeepLink(
                                            tab = AppTab.ORDERS,
                                            orderId = order.id,
                                            kind = "new",
                                            orderStatus = order.status,
                                        ),
                                        channelCategory = PushChannelCategory.KITCHEN_ALERTS,
                                        prefs = prefs,
                                    )
                                }
                                shouldNotifyKitchen -> {
                                    val senderRole = order.createdByRoleEnum
                                    val brandCfg = uiState.value.brandConfigs[senderRole] ?: senderRole.defaultBrandConfig()
                                    if (AppForegroundTracker.isAppInForeground()) {
                                        chimedPopupOrderKeys.add(key)
                                    }
                                    dispatchOrderAlertUi(
                                        soundKind = AlertSoundKind.NEW_ORDER,
                                        toastMessage = "Kitchen ticket #${order.orderNumber} · ${order.location}",
                                        notifTitle = "Kitchen · New order #${order.orderNumber}",
                                        notifBody = "${order.typeEnum.label} · ${order.location}",
                                        notificationId = order.orderNumber.toInt() + 1000,
                                        brandConfig = brandCfg,
                                        senderRole = senderRole,
                                        deepLink = PushDeepLink(
                                            tab = AppTab.KITCHEN,
                                            orderId = order.id,
                                            kind = "new",
                                            orderStatus = order.status,
                                        ),
                                        channelCategory = PushChannelCategory.KITCHEN_ALERTS,
                                        prefs = prefs,
                                    )
                                }
                                shouldNotifyReadyStaff -> {
                                    val senderRole = order.lastUpdatedByRoleEnum
                                    val brandCfg = uiState.value.brandConfigs[senderRole] ?: senderRole.defaultBrandConfig()
                                    if (AppForegroundTracker.isAppInForeground()) {
                                        chimedPopupOrderKeys.add(key)
                                    }
                                    dispatchOrderAlertUi(
                                        soundKind = AlertSoundKind.ORDER_READY,
                                        toastMessage = "Order #${order.orderNumber} ready · ${order.location}",
                                        notifTitle = "Order #${order.orderNumber} ready",
                                        notifBody = "${order.location} · ${order.customerName}",
                                        notificationId = order.orderNumber.toInt() + 10000,
                                        brandConfig = brandCfg,
                                        senderRole = senderRole,
                                        deepLink = PushDeepLink(
                                            tab = AppTab.ORDERS,
                                            orderId = order.id,
                                            kind = "ready",
                                            orderStatus = order.status,
                                        ),
                                        channelCategory = PushChannelCategory.READY_ALERTS,
                                        prefs = prefs,
                                    )
                                }
                                shouldNotifyCustomer -> {
                                    val kind = if (status == OrderStatus.READY) AlertSoundKind.ORDER_READY else AlertSoundKind.PING
                                    val senderRole = order.lastUpdatedByRoleEnum
                                    val brandCfg = uiState.value.brandConfigs[senderRole] ?: senderRole.defaultBrandConfig()
                                    val msg = AlertTargets.messageForStatus(order.orderNumber, order.location, status, order.cancelReason)
                                    dispatchOrderAlertUi(
                                        soundKind = kind,
                                        toastMessage = msg,
                                        notifTitle = "Order #${order.orderNumber} · ${status.label}",
                                        notifBody = msg,
                                        notificationId = order.orderNumber.toInt() + 20000,
                                        brandConfig = brandCfg,
                                        senderRole = senderRole,
                                        deepLink = PushDeepLink(
                                            tab = AppTab.ORDERS,
                                            orderId = order.id,
                                            kind = if (status == OrderStatus.READY) "ready" else "ping",
                                            orderStatus = order.status,
                                        ),
                                        channelCategory = if (status == OrderStatus.READY) {
                                            PushChannelCategory.READY_ALERTS
                                        } else {
                                            PushChannelCategory.KITCHEN_ALERTS
                                        },
                                        prefs = prefs,
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun observeIncomingNotifications() {
        viewModelScope.launch {
            combine(currentUserFlow, effectiveRoleFlow) { profile, (role, isAdmin) ->
                Triple(isAdmin || (profile?.active == true), role, isAdmin)
            }
                .filter { (allowed, _, _) -> allowed }
                .distinctUntilChanged()
                .flatMapLatest { (_, role, isAdmin) ->
                    var isFirstSnapshot = true
                    repository.observeNotifications(uid, isStaffOrAdmin = role.isStaff || isAdmin)
                        .catch { emit(emptyList()) }
                        .map { list ->
                            val first = isFirstSnapshot
                            isFirstSnapshot = false
                            Triple(role, first, list)
                        }
                }
                .collect { (role, isFirst, list) ->
                    if (isFirst) {
                        list.forEach { if (it.id.isNotBlank()) seenNotificationIds.add(it.id) }
                        return@collect
                    }
                    val prefs = prefsFlow.value
                    for (notif in list) {
                        val key = notif.id.ifBlank { continue }
                        if (!seenNotificationIds.add(key)) continue
                        if (notif.senderUid == uid) continue
                        val eventMs = notif.createdAt?.toDate()?.time ?: 0L
                        if (eventMs in 1L until (sessionStartEpochMs - 180_000L)) continue

                        val isBroadcastOrDirectPing = notif.kind == "broadcast" ||
                            notif.kind == "app_update" ||
                            (notif.kind == "ping" && (notif.targetUserId == uid || notif.orderId.isBlank()))
                        if (!isBroadcastOrDirectPing) continue

                        val relevant = when (role) {
                            UserRole.MANAGER -> true
                            UserRole.CUSTOMER -> notif.targetUserId == uid ||
                                (notif.targetUserId.isBlank() && (notif.targetRoles.contains("all") || notif.targetRoles.contains(UserRole.CUSTOMER.key)))
                            else -> notif.targetRoles.contains("all") || notif.targetRoles.contains(role.key) || notif.targetUserId == uid
                        }
                        if (!relevant) continue
                        if (!deduplicator.shouldProcessNotificationId(key)) continue

                        val senderRole = UserRole.fromKey(notif.senderRole)
                        val brandCfg = uiState.value.brandConfigs[senderRole] ?: senderRole.defaultBrandConfig()
                        val brandLabel = notif.senderBrandName.ifBlank { brandCfg.brandName }
                        val deepLink = PushDeepLink(
                            tab = if (notif.orderId.isNotBlank()) AppTab.ORDERS else AppTab.HOME,
                            orderId = notif.orderId,
                            notifId = notif.id,
                            openNotificationsSheet = notif.orderId.isBlank() && notif.kind != "app_update",
                            kind = notif.kind,
                            apkUrl = notif.apkUrl,
                        )
                        val stringsNow = uiState.value.strings
                        val notifTitle = when (notif.kind) {
                            "app_update" -> "$brandLabel · ${stringsNow.get("notif_update_available", "App update available")}"
                            "broadcast" -> "$brandLabel · ${stringsNow.get("manage_tab_alerts", "Announcement")}"
                            else -> "$brandLabel · ${stringsNow.get("notif_sheet_title", "Alert")}"
                        }
                        dispatchOrderAlertUi(
                            soundKind = AlertSoundKind.PING,
                            toastMessage = notif.message,
                            notifTitle = notifTitle,
                            notifBody = notif.message,
                            notificationId = (abs(key.hashCode()) % 900000) + 30000,
                            brandConfig = brandCfg.copy(brandName = brandLabel.take(18)),
                            senderRole = senderRole,
                            deepLink = deepLink,
                            channelCategory = PushChannelCategory.BROADCAST_ALERTS,
                            prefs = prefs,
                        )
                    }
                }
        }
    }

    private fun pickBestWaiter(
        orderArea: WaiterArea?,
        orders: List<Order>,
        waiters: List<StaffUser>,
        coverage: Map<String, WaiterCoverage>,
    ): StaffUser? {
        val onDuty = waiters.filter { w ->
            w.active && (coverage[w.userId]?.onDuty ?: true)
        }
        if (onDuty.isEmpty()) return null
        val areaMatches = if (orderArea != null) {
            onDuty.filter { w ->
                val areas = coverage[w.userId]?.areaSet ?: emptySet()
                orderArea in areas
            }
        } else {
            emptyList()
        }
        val candidates = areaMatches.ifEmpty { onDuty }
        return candidates.minWithOrNull(
            compareBy<StaffUser> { w ->
                orders.count { it.assignedWaiterId == w.userId && !it.statusEnum.isTerminal }
            }.thenBy { it.name.lowercase() }
        )
    }

    fun selectTab(tab: AppTab) {
        if (tab !in uiState.value.activeRole.allowedTabs) return
        _localUi.update { it.copy(currentTab = tab) }
    }

    fun selectZone(zone: FloorZone) {
        _localUi.update { it.copy(selectedZone = zone) }
    }

    fun startFromAreaCard(zoneKey: String) {
        val state = uiState.value
        if (AppTab.FLOOR !in state.activeRole.allowedTabs) {
            showToast("Your role cannot start orders")
            return
        }
        when (zoneKey) {
            "out" -> {
                _localUi.update {
                    it.copy(
                        selectedPlace = PlaceSelection(OrderType.OUT, "", "Takeaway / delivery"),
                        currentTab = AppTab.MENU,
                    )
                }
            }
            "hotel" -> _localUi.update { it.copy(selectedZone = FloorZone.HOTEL, currentTab = AppTab.FLOOR) }
            "dine" -> _localUi.update { it.copy(selectedZone = FloorZone.DINE, currentTab = AppTab.FLOOR) }
            "rec" -> _localUi.update { it.copy(selectedZone = FloorZone.REC, currentTab = AppTab.FLOOR) }
        }
    }

    fun selectFloorTile(zone: FloorZone, location: String) {
        val existingOrder = uiState.value.orders.firstOrNull {
            it.typeEnum == zone.orderType && it.location == location && !it.statusEnum.isTerminal
        }
        val label = zone.fullLabel(location)
        _localUi.update {
            it.copy(
                selectedPlace = PlaceSelection(zone.orderType, location, label),
                currentTab = AppTab.MENU,
            )
        }
        val msg = if (existingOrder != null) {
            "$label has order #${existingOrder.orderNumber} (${existingOrder.statusEnum.label})"
        } else {
            "Ordering for $label"
        }
        showToast(msg)
    }

    fun selectCategory(category: String) {
        _localUi.update { it.copy(selectedCategory = category) }
    }

    fun updateSearchQuery(query: String) {
        _localUi.update { it.copy(searchQuery = query) }
    }

    fun selectManageSection(section: ManageSection) {
        _localUi.update { it.copy(manageSection = section) }
    }

    fun selectTeamSubSection(subSection: TeamSubSection) {
        _localUi.update { it.copy(teamSubSection = subSection) }
    }

    fun addToCart(item: MenuItem, openSheet: Boolean = false) {
        if (!item.available) {
            showToast("${item.name} is sold out")
            return
        }
        _localUi.update { state ->
            val updated = state.cart.toMutableList()
            val idx = updated.indexOfFirst { it.menuItem.id == item.id }
            if (idx >= 0) {
                updated[idx] = updated[idx].copy(quantity = updated[idx].quantity + 1)
            } else {
                updated.add(CartItem(item, 1))
            }
            state.copy(
                cart = updated,
                showCartSheet = if (openSheet) true else state.showCartSheet,
            )
        }
        if (!openSheet) {
            showToast("${item.name} added")
        }
    }

    fun addToCartAndOpenOrder(item: MenuItem) {
        addToCart(item, openSheet = true)
    }

    fun decrementCartItem(itemId: String) {
        _localUi.update { state ->
            val updated = state.cart.mapNotNull {
                if (it.menuItem.id == itemId) {
                    if (it.quantity > 1) it.copy(quantity = it.quantity - 1) else null
                } else {
                    it
                }
            }
            state.copy(
                cart = updated,
                showCartSheet = if (updated.isEmpty()) false else state.showCartSheet,
            )
        }
    }

    fun clearCart() {
        _localUi.update { it.copy(cart = emptyList(), showCartSheet = false) }
    }

    fun openCartSheet() {
        _localUi.update { it.copy(showCartSheet = true) }
    }

    fun closeCartSheet() {
        _localUi.update { it.copy(showCartSheet = false) }
    }

    fun placeOrder(
        customerName: String,
        phoneOrLocation: String,
        specialNote: String,
        onValidationError: (String) -> Unit,
    ) {
        val state = uiState.value
        val s = state.strings
        val place = state.selectedPlace ?: PlaceSelection(
            type = OrderType.DINE,
            location = "Table 1",
            label = "Table 1",
        )
        if (state.cart.isEmpty()) {
            onValidationError(s.get("orders_empty", "Your order is empty."))
            return
        }
        val cleanName = customerName.trim()
        val finalLocation = if (place.type == OrderType.OUT) phoneOrLocation.trim() else place.location
        if (cleanName.isEmpty()) {
            onValidationError(s.get("val_name_required", "Enter your name."))
            return
        }
        if (finalLocation.isEmpty()) {
            onValidationError(s.orderTypePrompt(place.type))
            return
        }
        if (place.type == OrderType.OUT && !Regex("^[0-9+ ()-]{7,16}$").matches(finalLocation)) {
            onValidationError(s.get("prompt_phone_number", "Enter a valid phone number."))
            return
        }
        val latestById = state.menuItems.associateBy { it.id }
        val soldOutItem = state.cart.firstOrNull { latestById[it.menuItem.id]?.available == false }
        if (soldOutItem != null) {
            onValidationError(s.dishName(soldOutItem.menuItem))
            return
        }

        val orderArea = when (place.type) {
            OrderType.ROOM -> if (finalLocation.trim().startsWith("1")) WaiterArea.F1 else WaiterArea.F2
            OrderType.DINE -> WaiterArea.DIN
            OrderType.PICKUP -> WaiterArea.LOB
            OrderType.OUT -> null
        }
        val autoWaiter = if (place.type != OrderType.OUT) {
            pickBestWaiter(orderArea, state.orders, state.waiterUsers, state.waiterCoverage)
        } else {
            null
        }

        _localUi.update { it.copy(isSubmittingOrder = true) }
        viewModelScope.launch {
            val result = repository.createOrder(
                actorRole = state.activeRole,
                senderBrandName = state.activePalette.brandName,
                orderType = place.type,
                location = finalLocation,
                customerName = cleanName,
                specialNote = specialNote,
                cartItems = state.cart,
                assignedWaiterId = autoWaiter?.userId.orEmpty(),
                assignedMethod = if (autoWaiter != null) "auto" else "",
            )
            result.onSuccess { created ->
                deduplicator.markOrderStatusHandled(created.id, OrderStatus.NEW)
                _localUi.update {
                    it.copy(
                        cart = emptyList(),
                        selectedPlace = null,
                        showCartSheet = false,
                        currentTab = if (AppTab.ORDERS in state.activeRole.allowedTabs) AppTab.ORDERS else state.currentTab,
                        ordersFilter = OrdersFilterTab.ACTIVE,
                        highlightedOrderId = created.id,
                        isSubmittingOrder = false,
                    )
                }
                showToast(s.orderStatusMessage(created.orderNumber, finalLocation, OrderStatus.NEW))
            }.onFailure { err ->
                _localUi.update { it.copy(isSubmittingOrder = false) }
                onValidationError(err.localizedMessage ?: "Failed to place order")
            }
        }
    }

    fun setOrdersFilter(tab: OrdersFilterTab) {
        _localUi.update { it.copy(ordersFilter = tab) }
    }

    fun setOrdersZoneFilter(zone: OrdersZoneFilter) {
        _localUi.update { it.copy(ordersZoneFilter = zone) }
    }

    fun setKitchenStageFilter(status: OrderStatus) {
        _localUi.update { it.copy(kitchenStageFilter = status) }
    }

    fun advanceOrder(order: Order, targetStatus: OrderStatus, reason: String = "") {
        val state = uiState.value
        val s = state.strings
        if (state.activeRole == UserRole.CUSTOMER || state.activeRole == UserRole.CASHIER) return
        viewModelScope.launch {
            val localizedStatusMsg = s.orderStatusMessage(order.orderNumber, order.location, targetStatus, reason)
            val result = repository.advanceOrderStatus(
                order = order,
                targetStatus = targetStatus,
                actorRole = state.activeRole,
                senderBrandName = state.activePalette.brandName,
                cancelReason = reason,
                notificationMessageOverride = localizedStatusMsg,
            )
            result.onSuccess {
                deduplicator.markOrderStatusHandled(order.id, targetStatus)
                _localUi.update {
                    it.copy(
                        highlightedOrderId = order.id,
                        cancellingOrder = null,
                    )
                }
                showToast(
                    localizedStatusMsg,
                    senderRole = state.activeRole,
                )
            }.onFailure { err ->
                showToast(err.localizedMessage ?: "Failed to update order")
            }
        }
    }

    fun openCancelDialog(order: Order) {
        _localUi.update { it.copy(cancellingOrder = order) }
    }

    fun dismissCancelDialog() {
        _localUi.update { it.copy(cancellingOrder = null) }
    }

    fun dismissReadyAlertForLater(orderId: String) {
        _localUi.update { it.copy(dismissedReadyOrderIds = it.dismissedReadyOrderIds + orderId) }
    }

    fun openNotificationsSheet() {
        _localUi.update { it.copy(showNotificationsSheet = true) }
        viewModelScope.launch {
            prefsRepo.setLastNotifReadAtMs(System.currentTimeMillis())
        }
    }

    fun closeNotificationsSheet() {
        _localUi.update { it.copy(showNotificationsSheet = false) }
    }

    fun openSettingsSheet() {
        _localUi.update { it.copy(showSettingsSheet = true) }
    }

    fun closeSettingsSheet() {
        _localUi.update { it.copy(showSettingsSheet = false) }
    }

    fun openBrandEditor(role: UserRole) {
        if (uiState.value.isGoogleCustomerSession && role != UserRole.CUSTOMER) return
        _localUi.update { it.copy(editingBrandForRole = role) }
    }

    fun closeBrandEditor() {
        _localUi.update { it.copy(editingBrandForRole = null) }
    }

    fun saveBrand(config: BrandConfig, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (uiState.value.isGoogleCustomerSession) {
                onError("Role brands are managed by hotel staff.")
                return@launch
            }
            repository.saveBrandConfig(config)
                .onSuccess {
                    _localUi.update { it.copy(editingBrandForRole = null) }
                    showToast("Brand saved")
                }
                .onFailure { err ->
                    onError(err.localizedMessage ?: "Failed to save brand")
                }
        }
    }

    fun resetBrand(role: UserRole) {
        if (uiState.value.isGoogleCustomerSession) return
        viewModelScope.launch {
            repository.resetBrandConfig(role)
                .onSuccess {
                    _localUi.update { it.copy(editingBrandForRole = null) }
                    showToast("Brand reset to default")
                }
                .onFailure { err ->
                    showToast(err.localizedMessage ?: "Failed to reset brand")
                }
        }
    }

    fun switchAdminWorkspaceRole(role: UserRole) {
        if (!uiState.value.isAdminUser) return
        viewModelScope.launch {
            prefsRepo.setPreferredRoleKey(role.key)
            val defaultTab = if (uiState.value.currentTab in role.allowedTabs) {
                uiState.value.currentTab
            } else {
                AppTab.HOME
            }
            _localUi.update { it.copy(currentTab = defaultTab) }
            showToast("Workspace switched to ${role.displayName}", senderRole = role)
        }
    }

    fun sendStaffBroadcast(targetRoleKey: String, message: String, onDone: () -> Unit) {
        val state = uiState.value
        if (state.isGoogleCustomerSession) return
        viewModelScope.launch {
            repository.sendBroadcast(
                senderRole = state.activeRole,
                senderBrandName = state.activePalette.brandName,
                message = message,
                targetRoleKey = targetRoleKey,
            ).onSuccess {
                onDone()
                showToast("Notification sent")
            }.onFailure { err ->
                showToast(err.localizedMessage ?: "Failed to send notification")
            }
        }
    }

    fun toggleStaffAccountActive(user: StaffUser) {
        if (!uiState.value.isAdminUser || user.userRole == UserRole.MANAGER) return
        if (user.userRole == UserRole.CUSTOMER && user.active && user.permanentlyActivated) {
            showToast("Customer accounts stay permanently active once approved")
            return
        }
        viewModelScope.launch {
            val nextActive = !user.active
            repository.updateStaffAccount(user.userId, user.userRole, nextActive, user.name)
                .onSuccess {
                    val label = if (user.userRole == UserRole.CUSTOMER && nextActive) {
                        "Approved & permanently activated ${user.name}"
                    } else if (nextActive) {
                        "Enabled ${user.name}"
                    } else {
                        "Disabled ${user.name}"
                    }
                    showToast(label)
                }
                .onFailure { err ->
                    showToast(err.localizedMessage ?: "Failed to update account")
                }
        }
    }

    fun approveCustomerPermanently(user: StaffUser) {
        if (!uiState.value.isAdminUser || user.userRole != UserRole.CUSTOMER) return
        viewModelScope.launch {
            repository.approveCustomerPermanently(user.userId)
                .onSuccess {
                    showToast("Permanently activated ${user.name.ifBlank { user.email }}")
                }
                .onFailure { err ->
                    showToast(err.localizedMessage ?: "Failed to activate customer account")
                }
        }
    }

    fun createStaffAccount(
        name: String,
        username: String,
        role: UserRole,
        password: String,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        if (!uiState.value.isAdminUser) {
            onError("Only a Manager can create staff accounts.")
            return
        }
        viewModelScope.launch {
            repository.createStaffAccountDirectly(name, username, role, password)
                .onSuccess { created ->
                    onDone()
                    showToast("Created ${created.role.displayName} account @${created.username}")
                }
                .onFailure { err ->
                    onError(err.localizedMessage ?: "Failed to create staff account")
                }
        }
    }

    fun resetStaffPassword(
        user: StaffUser,
        newPassword: String,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        if (!uiState.value.isAdminUser) {
            onError("Only a Manager can reset staff passwords.")
            return
        }
        viewModelScope.launch {
            repository.resetStaffPasswordDirectly(user.userId, newPassword)
                .onSuccess {
                    onDone()
                    showToast("Password updated for ${user.name.ifBlank { user.username }}")
                }
                .onFailure { err ->
                    onError(err.localizedMessage ?: "Failed to reset password")
                }
        }
    }

    fun editStaffAccount(
        user: StaffUser,
        name: String,
        username: String,
        role: UserRole,
        active: Boolean,
        newPassword: String,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        if (!uiState.value.isAdminUser) {
            onError("Only a Manager can edit staff accounts.")
            return
        }
        val cleanName = name.trim().take(60)
        if (cleanName.isBlank()) {
            onError("Enter the staff member's name.")
            return
        }
        val isSelf = user.userId == uid
        val isExistingManager = user.userRole == UserRole.MANAGER
        val effectiveRole = if (isExistingManager) UserRole.MANAGER else role
        val effectiveActive = if (isSelf || isExistingManager) true else active
        if (!isExistingManager && effectiveRole == UserRole.CUSTOMER) {
            onError("Choose a staff role (Reception, Kitchen, Waiter, Cashier, or Delivery).")
            return
        }
        val cleanUsername = if (isExistingManager) {
            user.username.trim().lowercase()
        } else {
            username.trim().lowercase().removePrefix("@")
        }
        val existingUsername = user.username.trim().lowercase()
        val usernameChanged = !isExistingManager && cleanUsername != existingUsername
        val passwordProvided = !isExistingManager && newPassword.isNotEmpty()

        viewModelScope.launch {
            if (!usernameChanged && !passwordProvided) {
                repository.updateStaffAccount(user.userId, effectiveRole, effectiveActive, cleanName)
                    .onSuccess {
                        onDone()
                        showToast("Updated $cleanName")
                    }
                    .onFailure { err ->
                        onError(err.localizedMessage ?: "Failed to update staff account")
                    }
            } else {
                repository.updateStaffAccountDirectly(
                    targetUserId = user.userId,
                    name = cleanName,
                    username = cleanUsername,
                    role = effectiveRole,
                    active = effectiveActive,
                    newPassword = newPassword,
                )
                    .onSuccess { updated ->
                        onDone()
                        showToast("Updated ${updated.name}")
                    }
                    .onFailure { err ->
                        onError(err.localizedMessage ?: "Failed to update staff account")
                    }
            }
        }
    }

    fun updateStaffRole(user: StaffUser, role: UserRole) {
        if (!uiState.value.isAdminUser) return
        if (user.userId == uid && role != UserRole.MANAGER) {
            showToast("Cannot change your own Manager role.")
            return
        }
        viewModelScope.launch {
            repository.updateStaffAccount(user.userId, role, user.active, user.name)
                .onSuccess {
                    showToast("Updated ${user.name} to ${role.displayName}")
                }
                .onFailure { err ->
                    showToast(err.localizedMessage ?: "Failed to update role")
                }
        }
    }

    fun updateMenuPrice(itemId: String, price: Long) {
        if (!uiState.value.isAdminUser) return
        viewModelScope.launch {
            repository.updateMenuItem(itemId = itemId, price = price)
                .onSuccess {
                    showToast("Menu price updated")
                }
                .onFailure { err ->
                    showToast(err.localizedMessage ?: "Failed to update price")
                }
        }
    }

    fun toggleMenuAvailable(item: MenuItem) {
        if (!uiState.value.isAdminUser) return
        val nextAvailable = !item.available
        viewModelScope.launch {
            repository.updateMenuItem(itemId = item.id, available = nextAvailable)
                .onSuccess {
                    showToast(if (nextAvailable) "${item.name} back on sale" else "${item.name} marked sold out")
                }
                .onFailure { err ->
                    showToast(err.localizedMessage ?: "Failed to update menu item")
                }
        }
    }

    fun toggleWaiterDuty(waiterUserId: String) {
        val state = uiState.value
        if (!state.isAdminUser && waiterUserId != uid) return
        val currentCov = state.waiterCoverage[waiterUserId]
        val nextOnDuty = !(currentCov?.onDuty ?: true)
        val waiterName = state.waiterName(waiterUserId)
        val currentAreas = currentCov?.areas ?: listOf(WaiterArea.F1.key)
        viewModelScope.launch {
            repository.setWaiterDuty(
                waiterUserId = waiterUserId,
                waiterName = waiterName,
                onDuty = nextOnDuty,
                areas = currentAreas,
            ).onSuccess { onDuty ->
                showToast("$waiterName ${if (onDuty) "is on duty" else "is off duty"}")
            }.onFailure { err ->
                showToast(err.localizedMessage ?: "Failed to update duty status")
            }
        }
    }

    fun toggleWaiterArea(waiterUserId: String, area: WaiterArea) {
        val state = uiState.value
        if (!state.isAdminUser) return
        val currentCov = state.waiterCoverage[waiterUserId]
        val currentSet = currentCov?.areaSet ?: setOf(WaiterArea.F1)
        val nextSet = if (area in currentSet) currentSet - area else currentSet + area
        val waiterName = state.waiterName(waiterUserId)
        viewModelScope.launch {
            repository.setWaiterAreas(
                waiterUserId = waiterUserId,
                waiterName = waiterName,
                onDuty = currentCov?.onDuty ?: true,
                areas = nextSet,
            ).onFailure { err ->
                showToast(err.localizedMessage ?: "Failed to update waiter area")
            }
        }
    }

    fun openAssignWaiterDialog(order: Order) {
        _localUi.update { it.copy(assigningWaiterOrder = order) }
    }

    fun dismissAssignWaiterDialog() {
        _localUi.update { it.copy(assigningWaiterOrder = null) }
    }

    fun assignOrderWaiter(orderId: String, waiterUserId: String?) {
        val state = uiState.value
        if (state.activeRole !in listOf(UserRole.MANAGER, UserRole.RECEPTION, UserRole.CASHIER)) return
        val order = state.orders.firstOrNull { it.id == orderId } ?: return
        if (order.typeEnum == OrderType.OUT) return

        val chosen: StaffUser?
        val method: String
        if (waiterUserId != null) {
            chosen = state.waiterUsers.firstOrNull {
                it.userId == waiterUserId && it.active && (state.waiterCoverage[it.userId]?.onDuty ?: true)
            }
            if (chosen == null) {
                showToast("Waiter is off duty")
                return
            }
            method = "manual"
        } else {
            chosen = pickBestWaiter(order.areaForOrder(), state.orders, state.waiterUsers, state.waiterCoverage)
            if (chosen == null) {
                showToast("No waiter is on duty")
                return
            }
            method = "auto"
        }

        viewModelScope.launch {
            repository.assignOrderWaiter(
                order = order,
                chosenWaiterId = chosen.userId,
                chosenWaiterName = state.waiterName(chosen.userId),
                method = method,
                actorRole = state.activeRole,
                senderBrandName = state.activePalette.brandName,
            ).onSuccess { assignedName ->
                _localUi.update { it.copy(assigningWaiterOrder = null, highlightedOrderId = orderId) }
                showToast("Assigned to $assignedName")
            }.onFailure { err ->
                showToast(err.localizedMessage ?: "Unable to assign waiter")
            }
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setSoundEnabled(enabled) }
        if (enabled) {
            val prefs = uiState.value.preferences
            alertManager.triggerAlert(
                kind = AlertSoundKind.PING,
                soundEnabled = true,
                vibrationEnabled = prefs.vibrationEnabled,
                volume = prefs.volume.coerceAtLeast(0.45f),
                forceForegroundStream = true,
            )
        }
    }

    fun playTestAlertSound() {
        val state = uiState.value
        val prefs = state.preferences
        val s = state.strings
        alertManager.triggerAlert(
            kind = AlertSoundKind.NEW_ORDER,
            soundEnabled = true,
            vibrationEnabled = prefs.vibrationEnabled,
            volume = prefs.volume.coerceAtLeast(0.55f),
            forceForegroundStream = true,
        )
        val status = alertManager.getNotificationSystemStatus()
        if (status.deviceMediaMuted) {
            showToast(
                s.get(
                    "settings_device_vol_muted",
                    "Phone media volume is muted (0%) — raise device volume to hear alerts.",
                ),
            )
        } else {
            showToast(s.get("val_test_sound_played", "Alert chime & vibration tested."))
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setVibrationEnabled(enabled) }
    }

    fun setVolume(volume: Float) {
        viewModelScope.launch { prefsRepo.setVolume(volume) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            prefsRepo.setLanguageCode(language.code)
        }
    }

    fun saveTranslationOverride(
        key: String,
        category: TranslationCategory,
        en: String,
        am: String,
        enVersion: Int,
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        val state = uiState.value
        if (!state.isAdminUser) {
            onError("Manager permission required.")
            return
        }
        viewModelScope.launch {
            repository.saveTranslation(key, category, en, am, enVersion)
                .onSuccess {
                    onDone()
                    showToast(uiState.value.strings.get("val_translation_saved", "Translation saved & synced."))
                }
                .onFailure { err ->
                    onError(err.localizedMessage ?: "Failed to save translation")
                }
        }
    }

    fun restoreDefaultTranslation(key: String, onDone: () -> Unit = {}) {
        if (!uiState.value.isAdminUser) return
        viewModelScope.launch {
            repository.deleteTranslationOverride(key)
                .onSuccess {
                    onDone()
                    showToast(uiState.value.strings.get("val_translation_saved", "Translation restored."))
                }
                .onFailure { err ->
                    showToast(err.localizedMessage ?: "Failed to restore translation")
                }
        }
    }

    fun toggleSavedItem(item: MenuItem) {
        val state = uiState.value
        val s = state.strings
        val isSaved = state.savedItems.containsKey(item.id)
        viewModelScope.launch {
            val result = if (isSaved) {
                repository.removeMenuItemBookmark(uid, item.id)
            } else {
                repository.saveMenuItemBookmark(uid, item)
            }
            result.onSuccess {
                showToast(
                    if (isSaved) s.get("val_saved_removed", "Removed from bookmarks.")
                    else s.get("val_saved_added", "Saved to bookmarks.")
                )
            }.onFailure { err ->
                showToast(err.localizedMessage ?: "Failed to update saved items")
            }
        }
    }

    fun updateProfileName(name: String, onDone: () -> Unit = {}) {
        val s = uiState.value.strings
        val clean = name.trim()
        if (clean.isEmpty()) {
            showToast(s.get("val_name_required", "Please enter a full name."))
            return
        }
        viewModelScope.launch {
            repository.updateOwnProfile(uid = uid, name = clean)
                .onSuccess {
                    onDone()
                    showToast(s.get("val_profile_updated", "Profile updated."))
                }
                .onFailure { err ->
                    showToast(err.localizedMessage ?: "Failed to update profile")
                }
        }
    }

    fun uploadProfilePhoto(uri: Uri) {
        val uploader = mediaUploader ?: return
        val state = uiState.value
        val s = state.strings
        val previousUrl = state.currentUser?.photoUrl.orEmpty()
        _localUi.update { it.copy(isUploadingMedia = true) }
        viewModelScope.launch {
            uploader.uploadProfilePhoto(uid = uid, sourceUri = uri, previousUrl = previousUrl)
                .onSuccess { downloadUrl ->
                    repository.updateOwnProfile(uid = uid, photoUrl = downloadUrl)
                        .onSuccess {
                            _localUi.update { it.copy(isUploadingMedia = false) }
                            showToast(s.get("val_profile_updated", "Profile updated."))
                        }
                        .onFailure { err ->
                            _localUi.update { it.copy(isUploadingMedia = false) }
                            showToast(err.localizedMessage ?: "Failed to save profile photo")
                        }
                }
                .onFailure { err ->
                    _localUi.update { it.copy(isUploadingMedia = false) }
                    showToast(formatUploadError(err, s))
                }
        }
    }

    fun removeProfilePhoto() {
        val state = uiState.value
        val s = state.strings
        val previousUrl = state.currentUser?.photoUrl.orEmpty()
        if (previousUrl.isBlank()) return
        _localUi.update { it.copy(isUploadingMedia = true) }
        viewModelScope.launch {
            repository.updateOwnProfile(uid = uid, photoUrl = "")
                .onSuccess {
                    mediaUploader?.deleteStorageUrlSafely(previousUrl)
                    _localUi.update { it.copy(isUploadingMedia = false) }
                    showToast(s.get("val_profile_updated", "Profile updated."))
                }
                .onFailure { err ->
                    _localUi.update { it.copy(isUploadingMedia = false) }
                    showToast(err.localizedMessage ?: "Failed to remove photo")
                }
        }
    }

    fun uploadFoodImage(item: MenuItem, uri: Uri) {
        val state = uiState.value
        val s = state.strings
        if (!state.isAdminUser) return
        val uploader = mediaUploader ?: return
        _localUi.update { it.copy(uploadingMenuItemId = item.id) }
        viewModelScope.launch {
            uploader.uploadFoodImage(itemId = item.id, sourceUri = uri, previousUrl = item.imageUrl)
                .onSuccess { downloadUrl ->
                    repository.updateMenuItem(itemId = item.id, imageUrl = downloadUrl)
                        .onSuccess {
                            _localUi.update { it.copy(uploadingMenuItemId = null) }
                            showToast(s.get("val_food_image_updated", "Dish image updated."))
                        }
                        .onFailure { err ->
                            _localUi.update { it.copy(uploadingMenuItemId = null) }
                            showToast(err.localizedMessage ?: "Failed to update dish image")
                        }
                }
                .onFailure { err ->
                    _localUi.update { it.copy(uploadingMenuItemId = null) }
                    showToast(formatUploadError(err, s))
                }
        }
    }

    fun removeFoodImage(item: MenuItem) {
        val state = uiState.value
        val s = state.strings
        if (!state.isAdminUser || item.imageUrl.isBlank()) return
        _localUi.update { it.copy(uploadingMenuItemId = item.id) }
        viewModelScope.launch {
            repository.updateMenuItem(itemId = item.id, imageUrl = "")
                .onSuccess {
                    mediaUploader?.deleteStorageUrlSafely(item.imageUrl)
                    _localUi.update { it.copy(uploadingMenuItemId = null) }
                    showToast(s.get("val_food_image_updated", "Dish image updated."))
                }
                .onFailure { err ->
                    _localUi.update { it.copy(uploadingMenuItemId = null) }
                    showToast(err.localizedMessage ?: "Failed to remove dish image")
                }
        }
    }

    fun openAppUpdateDialog() {
        _localUi.update { it.copy(showAppUpdateDialog = true) }
    }

    fun dismissAppUpdateDialog() {
        lastPromptedReleaseVersionCode = releaseConfigFlow.value?.latestVersionCode
            ?: lastPromptedReleaseVersionCode
        _localUi.update { it.copy(showAppUpdateDialog = false) }
    }

    fun publishReleaseUpdate(
        versionCode: Long,
        versionName: String,
        apkDownloadUrl: String,
        releaseNotes: String,
        mandatory: Boolean,
        apkSha256: String = "",
        onDone: () -> Unit = {},
    ) {
        val state = uiState.value
        val s = state.strings
        if (!state.isAdminUser) return
        viewModelScope.launch {
            repository.publishReleaseUpdate(
                senderRole = state.assignedRole,
                senderBrandName = state.activePalette.brandName,
                versionCode = versionCode,
                versionName = versionName,
                apkDownloadUrl = apkDownloadUrl,
                releaseNotes = releaseNotes,
                mandatory = mandatory,
                apkSha256 = apkSha256,
            ).onSuccess {
                onDone()
                showToast(s.get("val_release_published", "Update notification sent."))
            }.onFailure { err ->
                showToast(err.localizedMessage ?: "Failed to publish update")
            }
        }
    }

    private fun formatUploadError(err: Throwable, s: AppStrings): String {
        val uploadErr = err as? MediaUploadException
        return when (uploadErr?.code) {
            ImageValidationError.UNSUPPORTED_TYPE ->
                s.get("val_image_invalid_type", "Unsupported image type. Please choose JPEG, PNG, or WebP.")
            ImageValidationError.TOO_LARGE ->
                s.get("val_image_too_large", "Image is too large even after compression (max 5 MB).")
            ImageValidationError.UNREADABLE, null ->
                s.format("val_image_upload_failed", "err" to (err.localizedMessage ?: "Upload failed"))
        }
    }

    fun showToast(
        message: String,
        senderRole: UserRole = uiState.value.activeRole,
        brandNameOverride: String? = null,
        deepLink: PushDeepLink? = null,
    ) {
        val id = System.currentTimeMillis()
        val brandName = brandNameOverride?.ifBlank { null }
            ?: uiState.value.brands[senderRole]?.brandName
            ?: senderRole.displayName
        _localUi.update {
            it.copy(
                activeToast = ToastBanner(
                    id = id,
                    message = message,
                    senderRole = senderRole,
                    brandName = brandName,
                    deepLink = deepLink,
                )
            )
        }
        viewModelScope.launch {
            delay(3200L)
            _localUi.update { current ->
                if (current.activeToast?.id == id) current.copy(activeToast = null) else current
            }
        }
    }

    fun dismissToast() {
        _localUi.update { it.copy(activeToast = null) }
    }
}
