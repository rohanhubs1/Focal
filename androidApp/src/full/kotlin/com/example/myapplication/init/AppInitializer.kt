package com.example.myapplication.init

import android.content.Context
import com.focal.app.config.FocalConfig
import com.focal.app.data.ActivityRepository
import com.focal.app.data.AuthRepository
import com.focal.app.data.FocalLocalStorage
import com.focal.app.data.FocalRepository
import com.focal.app.data.FocusQrRepository
import com.focal.app.firebase.ChildAppScanner
import com.focal.app.firebase.FirebaseAuthRepository
import com.focal.app.firebase.FirestoreActivityRepository
import com.focal.app.firebase.FirestoreFocalRepository
import com.focal.app.firebase.FirestoreFocusQrRepository
import com.focal.app.firebase.FirestorePairingRepository
import com.focal.app.model.UserRole
import com.focal.app.pairing.PairingRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppInitializer {
    fun init(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        // Full flavor: disable demo mode and DEV switch
        FocalConfig.isDemoMode = false
        FocalConfig.isDebug = false

        val authRepo = FirebaseAuthRepository()
        val focalRepo = FirestoreFocalRepository()
        val pairingRepo = FirestorePairingRepository()
        val activityRepo = FirestoreActivityRepository()
        val focusQrRepo = FirestoreFocusQrRepository()

        AuthRepository.install(authRepo)
        FocalRepository.install(focalRepo)
        PairingRepository.install(pairingRepo)
        ActivityRepository.install(activityRepo)
        FocusQrRepository.install(focusQrRepo)

        // Initialize ChildAppScanner
        ChildAppScanner.init(context)

        // Child local state restoration
        val isPairedAsChild = FocalLocalStorage.getBoolean("KEY_PAIRED_AS_CHILD", false)
        if (isPairedAsChild) {
            val savedChildId = FocalLocalStorage.getString("KEY_PAIRED_CHILD_ID", "")
            val savedChildName = FocalLocalStorage.getString("KEY_PAIRED_CHILD_NAME", "Child")
            val savedParentName = FocalLocalStorage.getString("KEY_PAIRED_PARENT_NAME", "Parent")
            val savedFamilyId = FocalLocalStorage.getString("KEY_PAIRED_FAMILY_ID", "")

            if (savedChildId.isNotBlank()) {
                focalRepo.currentUser = focalRepo.currentUser.copy(
                    id = savedChildId,
                    name = savedChildName,
                    role = UserRole.CHILD,
                    familyId = savedFamilyId
                )
                focalRepo.controllerUser = focalRepo.controllerUser.copy(
                    name = savedParentName,
                    familyId = savedFamilyId
                )
                // Start background session listener immediately from restored local child ID
                focalRepo.startChildSessionListener(context)
            }
        }

        // Handle persistent auth and reattach listeners
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser

                if (currentUser != null) {
                    if (currentUser.isAnonymous) {
                        resolveChildLinkAndSync(context, focalRepo, currentUser.uid)
                    } else {
                        // Parent account: reattach family snapshot listeners immediately
                        focalRepo.attachFamily(currentUser.uid)
                        focalRepo.controllerUser = focalRepo.controllerUser.copy(
                            id = currentUser.uid,
                            familyId = currentUser.uid,
                            email = currentUser.email ?: ""
                        )
                        focalRepo.currentUser = focalRepo.controllerUser
                        focusQrRepo.attachFamily(currentUser.uid)
                        activityRepo.attachFamily(currentUser.uid)
                    }
                } else {
                    // Auth state might still be restoring; attach listener
                    auth.addAuthStateListener(object : FirebaseAuth.AuthStateListener {
                        var handled = false
                        override fun onAuthStateChanged(fbAuth: FirebaseAuth) {
                            val user = fbAuth.currentUser
                            if (user != null && !handled) {
                                handled = true
                                if (user.isAnonymous) {
                                    resolveChildLinkAndSync(context, focalRepo, user.uid)
                                } else {
                                    focalRepo.attachFamily(user.uid)
                                    focalRepo.controllerUser = focalRepo.controllerUser.copy(
                                        id = user.uid,
                                        familyId = user.uid,
                                        email = user.email ?: ""
                                    )
                                    focalRepo.currentUser = focalRepo.controllerUser
                                    focusQrRepo.attachFamily(user.uid)
                                    activityRepo.attachFamily(user.uid)
                                }
                            }
                        }
                    })

                    // If device is not paired and not a parent, sign in anonymously once
                    val role = FocalLocalStorage.getString("KEY_SELECTED_ROLE", "")
                    if (!isPairedAsChild && role != "PARENT") {
                        auth.signInAnonymously().addOnSuccessListener { res ->
                            val u = res.user ?: return@addOnSuccessListener
                            resolveChildLinkAndSync(context, focalRepo, u.uid)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun resolveChildLinkAndSync(context: Context, focalRepo: FirestoreFocalRepository, uid: String) {
        val savedChildId = FocalLocalStorage.getString("KEY_PAIRED_CHILD_ID", "")
        val savedFamilyId = FocalLocalStorage.getString("KEY_PAIRED_FAMILY_ID", "")

        if (savedFamilyId.isNotBlank()) {
            focalRepo.attachFamily(savedFamilyId)
        }

        FirebaseFirestore.getInstance().collection("childLinks").document(uid).get()
            .addOnSuccessListener { linkDoc ->
                if (linkDoc.exists()) {
                    val cId = linkDoc.getString("childId") ?: ""
                    val famId = linkDoc.getString("familyId") ?: ""
                    if (cId.isNotBlank()) {
                        FocalLocalStorage.saveString("KEY_PAIRED_CHILD_ID", cId)
                    }
                    if (famId.isNotBlank()) {
                        FocalLocalStorage.saveString("KEY_PAIRED_FAMILY_ID", famId)
                        focalRepo.attachFamily(famId)
                    }
                    // Sync apps on app start
                    ChildAppScanner.syncApps(context, cId.ifBlank { savedChildId })
                } else if (savedChildId.isNotBlank()) {
                    ChildAppScanner.syncApps(context, savedChildId)
                }
            }
            .addOnFailureListener {
                if (savedChildId.isNotBlank()) {
                    ChildAppScanner.syncApps(context, savedChildId)
                }
            }
    }
}
