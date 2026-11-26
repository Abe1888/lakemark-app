package com.example.data.auth

import com.example.data.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

data class StaffSignInResult(
    val uid: String,
    val username: String,
    val name: String,
    val role: UserRole,
)

data class CreatedStaffAccount(
    val uid: String,
    val username: String,
    val name: String,
    val role: UserRole,
)

/**
 * Pure Firebase Authentication + Firestore staff sign-in client.
 * Uses Firebase Auth Email/Password with internal `@staff.lakemark.local` addresses
 * so staff login works directly on the Firebase Spark plan without external servers.
 */
class StaffAuthClient(
    private val authProvider: () -> FirebaseAuth = { Firebase.auth },
    private val dbProvider: () -> FirebaseFirestore = { Firebase.firestore },
) {
    suspend fun signInStaff(username: String, password: String): Result<StaffSignInResult> {
        val cleanUser = normalizeUsername(username)
        if (!USERNAME_REGEX.matches(cleanUser)) {
            return Result.failure(
                IllegalArgumentException("Enter a valid staff username (3-32 letters, numbers, ., _, -)."),
            )
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        val auth = authProvider()
        val db = dbProvider()
        return try {
            val email = staffEmailFor(cleanUser)
            val authRes = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authRes.user?.uid
                ?: return Result.failure(IllegalStateException("Staff sign-in did not return a Firebase user."))

            val snap = db.collection("users").document(uid).get().await()
            if (!snap.exists()) {
                auth.signOut()
                return Result.failure(
                    IllegalStateException(
                        "Staff profile not found. Ask your Manager to sign in once to sync staff accounts.",
                    ),
                )
            }

            val active = snap.getBoolean("active") ?: true
            if (!active) {
                auth.signOut()
                return Result.failure(
                    IllegalStateException("This staff account is deactivated. Ask your Manager to activate it."),
                )
            }

            val role = UserRole.fromKey(snap.getString("role"))
            if (role == UserRole.CUSTOMER) {
                auth.signOut()
                return Result.failure(
                    IllegalStateException("This account is not configured as an active staff member."),
                )
            }

            Result.success(
                StaffSignInResult(
                    uid = uid,
                    username = snap.getString("username")?.ifBlank { cleanUser } ?: cleanUser,
                    name = snap.getString("name").orEmpty(),
                    role = role,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException(mapFirebaseAuthException(e), e))
        }
    }

    companion object {
        const val STAFF_EMAIL_DOMAIN = "staff.lakemark.local"
        private val USERNAME_REGEX = Regex("^[a-z0-9._-]{3,32}$")

        fun normalizeUsername(username: String): String =
            username.trim().trimStart('@').trim().lowercase()

        fun staffEmailFor(username: String): String {
            val clean = normalizeUsername(username)
            return "$clean@$STAFF_EMAIL_DOMAIN"
        }

        fun mapFirebaseAuthException(err: Throwable?): String {
            val authEx = generateSequence(err) { it.cause }
                .filterIsInstance<FirebaseAuthException>()
                .firstOrNull()
            val code = authEx?.errorCode.orEmpty()
            val msg = err?.localizedMessage.orEmpty()
            return when {
                code == "ERROR_OPERATION_NOT_ALLOWED" || msg.contains("PASSWORD_LOGIN_DISABLED", ignoreCase = true) ->
                    "Email/Password sign-in is disabled in Firebase Console. Enable Email/Password in Authentication → Sign-in method, or use Manager Login."
                code in setOf("ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL") ->
                    "Invalid staff username or password."
                code == "ERROR_USER_DISABLED" ->
                    "This staff account is disabled."
                code == "ERROR_TOO_MANY_REQUESTS" ->
                    "Too many failed sign-in attempts. Please wait a moment and try again."
                msg.isNotBlank() -> msg
                else -> "Staff sign-in failed."
            }
        }
    }
}
