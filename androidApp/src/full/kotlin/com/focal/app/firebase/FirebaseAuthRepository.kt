package com.focal.app.firebase

import com.focal.app.data.AuthRepository
import com.focal.app.data.FocalLocalStorage
import com.focal.app.data.FocalRepository
import com.focal.app.model.User
import com.focal.app.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Result<User> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            return Result.failure(Exception("Enter a valid email"))
        }
        if (password.isEmpty()) {
            return Result.failure(Exception("Email or password is incorrect"))
        }

        return try {
            val authResult = auth.signInWithEmailAndPassword(trimmedEmail, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Sign in failed"))
            val uid = firebaseUser.uid

            // Fetch user doc from Firestore /users/{uid}
            val userDoc = firestore.collection("users").document(uid).get().await()
            val name = userDoc.getString("name") ?: firebaseUser.displayName ?: trimmedEmail.substringBefore("@")
            val roleStr = userDoc.getString("role") ?: "CONTROLLER"
            val familyId = userDoc.getString("familyId") ?: uid
            val role = if (roleStr == "ADMIN") UserRole.ADMIN else UserRole.CONTROLLER

            val onboardingDone = userDoc.getBoolean("onboardingDone") ?: false
            val focusMethod = userDoc.getString("focusMethod") ?: ""
            if (onboardingDone || focusMethod.isNotBlank()) {
                FocalLocalStorage.saveBoolean("KEY_FOCUS_METHOD_CHOSEN", true)
                FocalLocalStorage.saveBoolean("KEY_ONBOARDING_DONE", true)
                if (focusMethod.isNotBlank()) {
                    FocalLocalStorage.saveString("KEY_PREFERRED_FOCUS_METHOD", focusMethod)
                    FocalLocalStorage.saveString("KEY_FOCUS_METHOD", focusMethod)
                }
            }

            val user = User(
                id = uid,
                name = name,
                email = trimmedEmail,
                role = role,
                familyId = familyId
            )

            val repo = FocalRepository.instance
            repo.currentUser = user
            repo.controllerUser = user
            repo.switchUserRole(role)
            FocalLocalStorage.saveString("KEY_SELECTED_ROLE", if (role == UserRole.ADMIN) "ADMIN" else "PARENT")
            FocalLocalStorage.saveBoolean("KEY_AUTH_DONE", true)

            // Attach parent family listeners
            (repo as? FirestoreFocalRepository)?.attachFamily(familyId)
            (com.focal.app.data.FocusQrRepository.instance as? FirestoreFocusQrRepository)?.attachFamily(familyId)
            (com.focal.app.data.ActivityRepository.instance as? FirestoreActivityRepository)?.attachFamily(familyId)

            Result.success(user)
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("password", ignoreCase = true) == true ||
                e.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true -> "Email or password is incorrect"
                e.message?.contains("no user record", ignoreCase = true) == true -> "No account found with this email"
                else -> e.localizedMessage ?: "Sign in failed. Check your connection."
            }
            Result.failure(Exception(msg))
        }
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<User> {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()

        if (trimmedName.isEmpty()) {
            return Result.failure(Exception("Name cannot be empty"))
        }
        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            return Result.failure(Exception("Enter a valid email"))
        }
        if (password.length < 8) {
            return Result.failure(Exception("Password must be at least 8 characters"))
        }

        return try {
            val authResult = auth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Registration failed"))
            val uid = firebaseUser.uid
            val familyId = uid
            val now = System.currentTimeMillis()

            // 1. Create user document in users/{uid}
            val userData = hashMapOf(
                "uid" to uid,
                "name" to trimmedName,
                "email" to trimmedEmail,
                "role" to "CONTROLLER",
                "familyId" to uid,
                "createdAt" to now
            )
            firestore.collection("users").document(uid).set(userData).await()

            // 2. Create family document in families/{uid}
            val familyData = hashMapOf(
                "id" to uid,
                "name" to "$trimmedName's Family",
                "ownerUid" to uid,
                "createdAt" to now
            )
            firestore.collection("families").document(uid).set(familyData).await()

            val user = User(
                id = uid,
                name = trimmedName,
                email = trimmedEmail,
                role = UserRole.CONTROLLER,
                familyId = familyId
            )

            val repo = FocalRepository.instance
            repo.currentUser = user
            repo.controllerUser = user
            repo.switchUserRole(UserRole.CONTROLLER)
            FocalLocalStorage.saveString("KEY_SELECTED_ROLE", "PARENT")
            FocalLocalStorage.saveBoolean("KEY_AUTH_DONE", true)

            // Attach parent family listeners immediately
            (repo as? FirestoreFocalRepository)?.attachFamily(familyId)
            (com.focal.app.data.FocusQrRepository.instance as? FirestoreFocusQrRepository)?.attachFamily(familyId)
            (com.focal.app.data.ActivityRepository.instance as? FirestoreActivityRepository)?.attachFamily(familyId)

            Result.success(user)
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("already in use", ignoreCase = true) == true ||
                e.message?.contains("EMAIL_EXISTS", ignoreCase = true) == true ->
                    "An account with this email already exists — Log in instead"
                else -> e.localizedMessage ?: "Registration failed. Check your connection."
            }
            Result.failure(Exception(msg))
        }
    }

    override suspend fun signInWithGoogle(): Result<User> {
        return Result.failure(Exception("Google Sign-In is not configured"))
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        val trimmed = email.trim()
        if (trimmed.isEmpty() || !trimmed.contains("@") || !trimmed.contains(".")) {
            return Result.failure(Exception("Enter a valid email"))
        }
        return try {
            auth.sendPasswordResetEmail(trimmed).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Failed to send reset email"))
        }
    }

    override suspend fun signOut() {
        try {
            auth.signOut()
        } catch (ignored: Exception) {}
        FocalLocalStorage.saveBoolean("KEY_AUTH_DONE", false)
    }

    override fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return FocalRepository.instance.currentUser
    }

    override fun isUserAdmin(): Boolean {
        val user = getCurrentUser() ?: return false
        return user.role == UserRole.ADMIN
    }

    suspend fun ensureAnonymousAuth(): String {
        val current = auth.currentUser
        if (current != null) {
            return current.uid
        }
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: throw IllegalStateException("Anonymous auth returned null user")
    }

    override fun saveFocusMethod(method: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid).set(
                mapOf(
                    "focusMethod" to method,
                    "onboardingDone" to true
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
        } catch (_: Exception) {}
    }
}
