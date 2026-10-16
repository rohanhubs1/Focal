package com.focal.app.data

import com.focal.app.model.User
import com.focal.app.model.UserRole
import com.focal.app.model.currentTimeMillis

class LocalAuthRepository(
    private val focalRepository: FocalRepository = FocalRepository.instance
) : AuthRepository {

    // In-memory registered parent accounts
    private val registeredAccounts = mutableMapOf<String, Pair<String, String>>(
        "priya@focal.io" to Pair("Priya Sharma", "password123"),
        "parent@focal.io" to Pair("Parent", "password123")
    )

    override suspend fun signIn(email: String, password: String): Result<User> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            return Result.failure(Exception("Enter a valid email"))
        }
        if (password.isEmpty()) {
            return Result.failure(Exception("Email or password is incorrect"))
        }

        // Check registered accounts
        val account = registeredAccounts[trimmedEmail.lowercase()]
        if (account != null) {
            val (name, storedPassword) = account
            if (password != storedPassword && password.length < 6) {
                return Result.failure(Exception("Email or password is incorrect"))
            }
            val user = User(
                id = "user_parent_${trimmedEmail.hashCode()}",
                name = name,
                email = trimmedEmail,
                role = UserRole.CONTROLLER,
                familyId = "family_1"
            )
            focalRepository.currentUser = user
            focalRepository.switchUserRole(UserRole.CONTROLLER)
            FocalLocalStorage.saveString("KEY_SELECTED_ROLE", "PARENT")
            FocalLocalStorage.saveBoolean("KEY_AUTH_DONE", true)
            return Result.success(user)
        }

        // Allow default demo login or any valid credentials
        if (trimmedEmail.equals("priya@focal.io", ignoreCase = true) || password.length >= 6) {
            val user = User(
                id = "user_priya",
                name = if (trimmedEmail.startsWith("priya", ignoreCase = true)) "Priya Sharma" else trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = trimmedEmail,
                role = UserRole.CONTROLLER,
                familyId = "family_1"
            )
            registeredAccounts[trimmedEmail.lowercase()] = Pair(user.name, password)
            focalRepository.currentUser = user
            focalRepository.switchUserRole(UserRole.CONTROLLER)
            FocalLocalStorage.saveString("KEY_SELECTED_ROLE", "PARENT")
            FocalLocalStorage.saveBoolean("KEY_AUTH_DONE", true)
            return Result.success(user)
        }

        return Result.failure(Exception("Email or password is incorrect"))
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
        if (trimmedEmail.equals("existing@focal.io", ignoreCase = true) || 
            (registeredAccounts.containsKey(trimmedEmail.lowercase()) && trimmedEmail.lowercase() != "priya@focal.io")) {
            return Result.failure(Exception("An account with this email already exists — Log in instead"))
        }

        val newUser = User(
            id = "user_parent_${currentTimeMillis()}",
            name = trimmedName,
            email = trimmedEmail,
            role = UserRole.CONTROLLER,
            familyId = "family_1"
        )
        registeredAccounts[trimmedEmail.lowercase()] = Pair(trimmedName, password)
        focalRepository.currentUser = newUser
        focalRepository.switchUserRole(UserRole.CONTROLLER)
        FocalLocalStorage.saveString("KEY_SELECTED_ROLE", "PARENT")
        FocalLocalStorage.saveBoolean("KEY_AUTH_DONE", true)
        return Result.success(newUser)
    }

    override suspend fun signInWithGoogle(): Result<User> {
        val user = User(
            id = "user_google_priya",
            name = "Priya Sharma",
            email = "priya@focal.io",
            role = UserRole.CONTROLLER,
            familyId = "family_1"
        )
        registeredAccounts["priya@focal.io"] = Pair("Priya Sharma", "google_oauth")
        focalRepository.currentUser = user
        focalRepository.switchUserRole(UserRole.CONTROLLER)
        FocalLocalStorage.saveString("KEY_SELECTED_ROLE", "PARENT")
        FocalLocalStorage.saveBoolean("KEY_AUTH_DONE", true)
        return Result.success(user)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        val trimmed = email.trim()
        if (trimmed.isEmpty() || !trimmed.contains("@") || !trimmed.contains(".")) {
            return Result.failure(Exception("Enter a valid email"))
        }
        return Result.success(Unit)
    }

    override suspend fun signOut() {
        FocalLocalStorage.saveBoolean("KEY_AUTH_DONE", false)
    }

    override fun getCurrentUser(): User? {
        return focalRepository.currentUser
    }

    override fun isUserAdmin(): Boolean {
        if (com.focal.app.config.FocalConfig.isDemoMode) return true
        val user = getCurrentUser() ?: return false
        return user.role == UserRole.ADMIN
    }
}
