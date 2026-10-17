package com.focal.app.firebase

import android.content.Context
import com.example.myapplication.schedule.FocalScheduleManager
import com.focal.app.data.ActivityEventType
import com.focal.app.data.ActivityRepository
import com.focal.app.data.FocalLocalStorage
import com.focal.app.data.LocalFocalRepository
import com.focal.app.engine.FocusEngine
import com.focal.app.model.*
import com.focal.app.protection.ProtectionStateRepository
import com.focal.app.ui.components.PlatformPermissionsHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

class FirestoreFocalRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : LocalFocalRepository() {

    private val registrations = mutableListOf<ListenerRegistration>()
    private val activeSessionsEndTime = mutableMapOf<String, Long>()
    private val childAppsListeners = mutableMapOf<String, ListenerRegistration>()
    private val childAppsMap = mutableMapOf<String, List<InstalledApp>>()
    private var childSessionListenerReg: ListenerRegistration? = null
    private var childExitRequestListenerReg: ListenerRegistration? = null

    private var permissionDeniedCount = 0
    private var permissionDeniedTimestamp = 0L
    private var appContext: Context? = null

    init {
        // Empty initial states for full build (no mock / sample data on fresh launch)
        controllerUser = User(
            id = "",
            name = "",
            email = "",
            role = UserRole.CONTROLLER
        )
        currentUser = User(
            id = "",
            name = "",
            email = "",
            role = UserRole.CHILD
        )
        parentPin = ""

        childrenList.clear()
        emergencyRequests.clear()
        policies.clear()
        schedules.clear()
        nfcCards.clear()
        notifications.clear()
        auditLogs.clear()
        adminDevices.clear()
        parentAccounts.clear()
        childInstalledApps.clear()

        val famId = currentFamilyId()
        if (famId.isNotBlank()) {
            attachFamily(famId)
        }
    }

    private fun isDeviceOnline(): Boolean {
        return try {
            val context = PlatformPermissionsHelper.appContext ?: return true
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val network = cm?.activeNetworkInfo
            network != null && network.isConnected
        } catch (_: Exception) {
            true
        }
    }

    private fun currentFamilyId(): String {
        val authUser = FirebaseAuth.getInstance().currentUser
        val authUid = authUser?.uid
        if (authUser != null && !authUser.isAnonymous && !authUid.isNullOrBlank()) {
            return authUid
        }
        val savedFamId = FocalLocalStorage.getString("KEY_PAIRED_FAMILY_ID", "")
        if (savedFamId.isNotBlank()) {
            return savedFamId
        }
        if (controllerUser.familyId.isNotBlank()) {
            return controllerUser.familyId
        }
        return ""
    }

    fun attachFamily(familyId: String) {
        if (familyId.isBlank()) return
        registrations.forEach { it.remove() }
        registrations.clear()
        childAppsListeners.values.forEach { it.remove() }
        childAppsListeners.clear()
        childAppsMap.clear()

        val authUser = FirebaseAuth.getInstance().currentUser
        val isChildPhone = authUser == null || authUser.isAnonymous

        if (!isChildPhone) {
            // 1. Listen to active sessions to dynamically compute remaining minutes (Parent only)
            val sessReg = firestore.collection("sessions")
                .whereEqualTo("familyId", familyId)
                .whereEqualTo("isActive", true)
                .addSnapshotListener { snapshots, _ ->
                    if (snapshots != null) {
                        activeSessionsEndTime.clear()
                        for (doc in snapshots.documents) {
                            val endTime = doc.getLong("endTimeMillis") ?: 0L
                            val childId = doc.getString("childId") ?: ""
                            if (childId.isNotBlank()) {
                                activeSessionsEndTime[childId] = endTime
                            }
                        }
                        refreshChildrenRemainingMinutes()
                    }
                }
            registrations.add(sessReg)

            // 2. Listen to children collection (Parent only)
            val childReg = firestore.collection("children")
                .whereEqualTo("familyId", familyId)
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    val now = currentTimeMillis()
                    val updatedChildren = snapshots.documents.mapNotNull { doc ->
                        try {
                            val childId = doc.id
                            val name = doc.getString("name") ?: ""
                            val age = doc.getLong("age")?.toInt() ?: 14
                            val grade = doc.getString("grade") ?: "Class $age"
                            val activeSessionId = doc.getString("currentSessionId")
                            val isFocusing = !activeSessionId.isNullOrBlank()
                            val endTime = activeSessionsEndTime[childId] ?: 0L
                            val remainingMins = if (isFocusing && endTime > now) {
                                ((endTime - now) / 60000L).toInt().coerceAtLeast(1)
                            } else {
                                0
                            }

                            val childUid = doc.getString("childUid")

                            ChildProfile(
                                id = childId,
                                userId = "user_$childId",
                                controllerId = controllerUser.id,
                                name = name,
                                familyId = familyId,
                                defaultPolicyId = doc.getString("defaultPolicyId") ?: "pol_study",
                                defaultFocusMinutes = doc.getLong("defaultFocusMinutes")?.toInt() ?: 90,
                                age = age,
                                grade = grade,
                                isOnline = doc.getBoolean("isOnline") ?: false,
                                isFocusing = isFocusing,
                                todayFocusMinutes = doc.getLong("todayFocusMinutes")?.toInt() ?: 0,
                                screenTimeMinutes = doc.getLong("screenTimeMinutes")?.toInt() ?: 0,
                                focusScore = doc.getLong("focusScore")?.toInt() ?: 85,
                                timeSavedMinutes = doc.getLong("timeSavedMinutes")?.toInt() ?: 0,
                                streakDays = doc.getLong("streakDays")?.toInt() ?: 0,
                                currentSessionId = activeSessionId,
                                remainingMinutes = remainingMins,
                                focusEndsAtMillis = endTime,
                                focusTotalMinutes = doc.getLong("defaultFocusMinutes")?.toInt() ?: 90,
                                pairedCardId = doc.getString("pairedCardId"),
                                deviceName = doc.getString("deviceName") ?: "",
                                childUid = childUid
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    childrenList.clear()
                    childrenList.addAll(updatedChildren)

                    // Update child apps listeners for each linked child
                    updateChildAppListeners(updatedChildren.map { it.id to it.name })
                }
            registrations.add(childReg)

            // 3. Listen to exitRequests collection (Parent only)
            val exitReg = firestore.collection("exitRequests")
                .whereEqualTo("familyId", familyId)
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    val updatedRequests = snapshots.documents.mapNotNull { doc ->
                        try {
                            EmergencyExitRequest(
                                id = doc.id,
                                sessionId = doc.getString("sessionId") ?: "",
                                childId = doc.getString("childId") ?: "",
                                childName = doc.getString("childName") ?: "Child",
                                reason = doc.getString("reason") ?: "",
                                note = doc.getString("note") ?: "",
                                requestedAt = doc.getString("requestedAt") ?: "Just now",
                                requestedAtMillis = doc.getLong("requestedAtMillis") ?: currentTimeMillis(),
                                status = doc.getString("status") ?: "PENDING"
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    emergencyRequests.clear()
                    emergencyRequests.addAll(updatedRequests)
                }
            registrations.add(exitReg)
        } else {
            // Child phone: attach single-document listener to own child doc (permitted by security rules)
            val childId = FocalLocalStorage.getString("KEY_PAIRED_CHILD_ID", "")
            if (childId.isNotBlank()) {
                val childDocReg = firestore.collection("children").document(childId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            val isOnline = isDeviceOnline()
                            if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED && isOnline) {
                                val now = currentTimeMillis()
                                if (permissionDeniedCount == 0 || now - permissionDeniedTimestamp > 5000L) {
                                    permissionDeniedCount++
                                    permissionDeniedTimestamp = now
                                    if (permissionDeniedCount >= 2) {
                                        handleChildUnlinked()
                                    }
                                }
                            }
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            if (snapshot.metadata.isFromCache) {
                                // Never unlink on local cache reads
                                return@addSnapshotListener
                            }
                            val myUid = FirebaseAuth.getInstance().currentUser?.uid
                            if (myUid.isNullOrBlank()) {
                                // Auth is still initializing, don't unlink
                                return@addSnapshotListener
                            }
                            if (!snapshot.exists()) {
                                handleChildUnlinked()
                                return@addSnapshotListener
                            }
                            val docChildUid = snapshot.getString("childUid")
                            if (!docChildUid.isNullOrBlank() && docChildUid != myUid) {
                                handleChildUnlinked()
                                return@addSnapshotListener
                            }

                            // Successfully read from server
                            permissionDeniedCount = 0

                            val cName = snapshot.getString("name")
                            val pName = snapshot.getString("parentName")
                            if (!cName.isNullOrBlank()) {
                                currentUser = currentUser.copy(name = cName)
                                FocalLocalStorage.saveString("KEY_PAIRED_CHILD_NAME", cName)
                            }
                            if (!pName.isNullOrBlank()) {
                                controllerUser = controllerUser.copy(name = pName)
                                FocalLocalStorage.saveString("KEY_PAIRED_PARENT_NAME", pName)
                            }
                        }
                    }
                registrations.add(childDocReg)

                // Start active session listener for child phone
                startChildSessionListener(null)
            }
        }

        // 4. Listen to families/{familyId}/policies (Both Parent and Child)
        val polReg = firestore.collection("families").document(familyId)
            .collection("policies")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener
                if (snapshots.isEmpty) {
                    if (!isChildPhone) {
                        // New family: parent creates the default "Study Mode" policy once
                        val defaultPolicyDoc = hashMapOf(
                            "name" to "Study Mode",
                            "blockedApps" to emptyList<String>(),
                            "blockedWebsites" to emptyList<String>(),
                            "updatedAt" to currentTimeMillis()
                        )
                        firestore.collection("families").document(familyId)
                            .collection("policies").document("pol_study").set(defaultPolicyDoc)
                    }
                    return@addSnapshotListener
                }

                val updatedPolicies = snapshots.documents.map { doc ->
                    val pId = doc.id
                    val pName = doc.getString("name") ?: "Study Mode"
                    val blockedApps = (doc.get("blockedApps") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    val blockedWebsites = (doc.get("blockedWebsites") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    FocusPolicy(
                        id = pId,
                        childId = "",
                        name = pName,
                        mode = FocusMode.STUDY,
                        durationMinutes = 90,
                        blockedApps = blockedApps,
                        blockedWebsites = blockedWebsites
                    )
                }
                policies.clear()
                policies.addAll(updatedPolicies)

                // Child phone listens live and updates local block list + cache immediately
                val activePol = updatedPolicies.firstOrNull()
                if (activePol != null) {
                    FocalLocalStorage.saveString("KEY_CACHED_POLICY_NAME", activePol.name)
                    FocalLocalStorage.saveString("KEY_CACHED_BLOCKED_APPS", activePol.blockedApps.joinToString(","))
                    FocalLocalStorage.saveString("KEY_CACHED_BLOCKED_WEBSITES", activePol.blockedWebsites.joinToString(","))

                    val policyData = FocusPolicyData(
                        blockedApps = activePol.blockedApps,
                        blockedPackages = activePol.blockedApps.toSet(),
                        blockedWebsites = activePol.blockedWebsites
                    )
                    ProtectionStateRepository.instance.updatePolicy(policyData)
                }
            }
        registrations.add(polReg)

        // 5. Listen to families/{familyId}/schedules
        val schedReg = firestore.collection("families").document(familyId)
            .collection("schedules")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener
                val updatedSchedules = snapshots.documents.map { doc ->
                    val sId = doc.id
                    val policyId = doc.getString("policyId") ?: "Study Mode"
                    val startM = doc.getLong("startMinute")?.toInt() ?: (9 * 60)
                    val endM = doc.getLong("endMinute")?.toInt() ?: (13 * 60)
                    val daysList = (doc.get("days") as? List<*>)?.mapNotNull { it?.toString() } ?: listOf("Mon", "Tue", "Wed", "Thu", "Fri")
                    val enabled = doc.getBoolean("enabled") ?: true
                    val childIds = (doc.get("childIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                    val startH = startM / 60
                    val startMin = startM % 60
                    val endH = endM / 60
                    val endMin = endM % 60
                    val timeStr = "${startH.toString().padStart(2, '0')}:${startMin.toString().padStart(2, '0')} - ${endH.toString().padStart(2, '0')}:${endMin.toString().padStart(2, '0')}"
                    val daysStr = daysList.joinToString(", ")

                    ScheduleItem(
                        id = sId,
                        title = "Schedule",
                        timeRange = timeStr,
                        days = daysStr,
                        mode = FocusMode.STUDY,
                        isEnabled = enabled,
                        appsCount = 0,
                        websitesCount = 0,
                        policyName = policyId,
                        childId = childIds.firstOrNull() ?: ""
                    )
                }
                schedules.clear()
                schedules.addAll(updatedSchedules)

                // Cache schedules to FocalLocalStorage for offline reboot
                val serializedSchedules = updatedSchedules.joinToString(";") {
                    "${it.id}|${it.policyName}|${it.timeRange}|${it.days}|${it.isEnabled}"
                }
                FocalLocalStorage.saveString("KEY_CACHED_SCHEDULES", serializedSchedules)

                // Update schedule alarms immediately
                val context = PlatformPermissionsHelper.appContext
                if (context != null) {
                    FocalScheduleManager.updateSchedules(context, updatedSchedules)
                }
            }
        registrations.add(schedReg)
    }

    private fun handleChildUnlinked() {
        try {
            // Stop active focus if any
            val engine = FocusEngine.instance
            if (engine.currentSession != null) {
                engine.endFocus(TriggerType.EMERGENCY_OVERRIDE, "UNLINK")
            }
            ProtectionStateRepository.instance.reset()

            // Clear local FOCAL pairing data
            FocalLocalStorage.remove("KEY_PAIRED_CHILD_ID")
            FocalLocalStorage.remove("KEY_PAIRED_FAMILY_ID")
            FocalLocalStorage.remove("KEY_PAIRED_CODE")
            FocalLocalStorage.remove("KEY_PAIRED_DATE")
            FocalLocalStorage.remove("KEY_CHILD_NAME")
            FocalLocalStorage.remove("KEY_PARENT_NAME")
            FocalLocalStorage.remove("KEY_CACHED_POLICY_NAME")
            FocalLocalStorage.remove("KEY_CACHED_BLOCKED_APPS")
            FocalLocalStorage.remove("KEY_CACHED_BLOCKED_WEBSITES")
            FocalLocalStorage.remove("KEY_CACHED_SCHEDULES")
            FocalLocalStorage.remove("KEY_SELECTED_ROLE")
            FocalLocalStorage.remove("KEY_AUTH_DONE")
            FocalLocalStorage.remove("KEY_SETUP_DONE")
            FocalLocalStorage.remove("KEY_ONBOARDING_DONE")
            FocalLocalStorage.remove("KEY_PAIRED_IS_PAIRED")

            com.focal.app.pairing.PairingRepository.instance.unlink()

            // Sign out anonymous user
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser?.isAnonymous == true) {
                auth.signOut()
            }

            // Remove all Firestore snapshot listeners
            registrations.forEach { it.remove() }
            registrations.clear()
            childAppsListeners.values.forEach { it.remove() }
            childAppsListeners.clear()
            childAppsMap.clear()
            stopChildSessionListener()

            // Trigger UI navigation to Welcome on main thread
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onUnlinkedFromParent?.invoke()
            }
        } catch (_: Exception) {}
    }

    private fun updateChildAppListeners(children: List<Pair<String, String>>) {
        val currentChildIds = children.map { it.first }.toSet()
        val toRemove = childAppsListeners.keys - currentChildIds
        toRemove.forEach { id ->
            childAppsListeners.remove(id)?.remove()
            childAppsMap.remove(id)
        }

        children.forEach { (childId, childName) ->
            if (!childAppsListeners.containsKey(childId)) {
                val reg = firestore.collection("children").document(childId)
                    .collection("apps")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) return@addSnapshotListener
                        val apps = snapshot.documents.mapNotNull { doc ->
                            val pkg = doc.getString("packageName") ?: doc.id
                            val label = doc.getString("label") ?: pkg
                            val iconPng = doc.getString("iconPng") ?: ""
                            val category = doc.getString("category") ?: "Other"
                            InstalledApp(
                                packageName = pkg,
                                appName = label,
                                category = category,
                                iconPng = iconPng,
                                childId = childId,
                                childOwnerName = childName
                            )
                        }
                        childAppsMap[childId] = apps

                        // Recombine into repository childInstalledApps
                        val allApps = childAppsMap.values.flatten()
                        childInstalledApps.clear()
                        childInstalledApps.addAll(allApps)
                    }
                childAppsListeners[childId] = reg
                registrations.add(reg)
            }
        }
    }

    private fun refreshChildrenRemainingMinutes() {
        val now = currentTimeMillis()
        childrenList.forEachIndexed { idx, child ->
            val endTime = activeSessionsEndTime[child.id] ?: 0L
            val isFocusing = child.currentSessionId != null && endTime > now
            val remainingMins = if (isFocusing) ((endTime - now) / 60000L).toInt().coerceAtLeast(1) else 0
            if (child.remainingMinutes != remainingMins || child.isFocusing != isFocusing) {
                childrenList[idx] = child.copy(
                    isFocusing = isFocusing,
                    remainingMinutes = remainingMins,
                    focusEndsAtMillis = endTime
                )
            }
        }
    }

    override fun addChild(name: String, age: Int, emailOrPhone: String): Pair<ChildProfile, String> {
        val childId = "child_${currentTimeMillis()}"
        val famId = currentFamilyId()

        val newChild = ChildProfile(
            id = childId,
            userId = "user_$childId",
            controllerId = controllerUser.id,
            name = name.trim(),
            familyId = famId,
            defaultPolicyId = "pol_study",
            defaultFocusMinutes = 90,
            age = age,
            grade = "Class $age",
            isOnline = false,
            isFocusing = false,
            todayFocusMinutes = 0,
            screenTimeMinutes = 0,
            focusScore = 80,
            timeSavedMinutes = 0,
            streakDays = 0,
            currentSessionId = null,
            remainingMinutes = 0,
            pairedCardId = null,
            deviceName = "",
            childUid = null
        )
        childrenList.add(newChild)

        try {
            val childDoc = hashMapOf(
                "id" to childId,
                "familyId" to famId,
                "name" to name.trim(),
                "age" to age,
                "grade" to "Class $age",
                "currentSessionId" to null,
                "childUid" to null,
                "deviceName" to "",
                "isOnline" to false,
                "pairedCardId" to null,
                "defaultPolicyId" to "pol_study",
                "defaultFocusMinutes" to 90,
                "createdAt" to currentTimeMillis()
            )
            firestore.collection("children").document(childId).set(childDoc)
                .addOnFailureListener { e ->
                    PlatformPermissionsHelper.showToast("Failed to save child: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            PlatformPermissionsHelper.showToast("Failed to save child: ${e.localizedMessage}")
        }

        return Pair(newChild, childId)
    }

    override fun unlinkChildDevice(childId: String) {
        val child = childrenList.find { it.id == childId }
        val targetUid = child?.childUid
        val activeSessionId = child?.currentSessionId

        // 1. Update local state immediately so UI updates
        val index = childrenList.indexOfFirst { it.id == childId }
        if (index >= 0) {
            childrenList[index] = childrenList[index].copy(
                childUid = null,
                deviceName = "",
                pairedCardId = null,
                isOnline = false,
                isFocusing = false,
                remainingMinutes = 0,
                currentSessionId = null
            )
        }

        // 2. Batch update: children/{childId}.childUid = null, delete childLinks/{childUid}, end session
        try {
            val batch = firestore.batch()
            val childRef = firestore.collection("children").document(childId)
            batch.update(childRef, mapOf(
                "childUid" to null,
                "deviceName" to "",
                "isOnline" to false,
                "currentSessionId" to null
            ))

            if (!targetUid.isNullOrBlank()) {
                val linkRef = firestore.collection("childLinks").document(targetUid)
                batch.delete(linkRef)
            }

            if (!activeSessionId.isNullOrBlank()) {
                val sessRef = firestore.collection("sessions").document(activeSessionId)
                batch.update(sessRef, mapOf(
                    "status" to "ENDED_EARLY",
                    "isActive" to false,
                    "endedAt" to currentTimeMillis()
                ))
            }

            batch.commit()
                .addOnSuccessListener {
                    PlatformPermissionsHelper.showToast("Phone unlinked")
                }
                .addOnFailureListener { e ->
                    PlatformPermissionsHelper.showToast("Unlink failed: ${e.localizedMessage ?: "Permission denied"}")
                }
        } catch (e: Exception) {
            PlatformPermissionsHelper.showToast("Unlink error: ${e.localizedMessage}")
        }
    }

    override fun removeChild(childId: String) {
        val child = childrenList.find { it.id == childId }
        childrenList.removeAll { it.id == childId }
        try {
            firestore.collection("children").document(childId).delete()
                .addOnSuccessListener {
                    PlatformPermissionsHelper.showToast("Removed ${child?.name ?: "child"}")
                }
                .addOnFailureListener { e ->
                    PlatformPermissionsHelper.showToast("Delete failed: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            PlatformPermissionsHelper.showToast("Delete error: ${e.localizedMessage}")
        }
    }

    override fun updateChild(updated: ChildProfile) {
        val idx = childrenList.indexOfFirst { it.id == updated.id }
        if (idx >= 0) {
            childrenList[idx] = updated
        }

        try {
            // Child can only update allowed fields under security rules:
            // ['isOnline', 'lastSeenAt', 'deviceName', 'appVersion', 'protectionStatus', 'fcmToken']
            if (currentUser.role == UserRole.CHILD) {
                val childUpdates = mutableMapOf<String, Any>(
                    "isOnline" to updated.isOnline,
                    "lastSeenAt" to currentTimeMillis()
                )
                if (updated.deviceName.isNotBlank()) {
                    childUpdates["deviceName"] = updated.deviceName
                }
                firestore.collection("children").document(updated.id).update(childUpdates)
            } else {
                // Parent can update all profile fields
                val data = hashMapOf(
                    "name" to updated.name,
                    "age" to updated.age,
                    "grade" to updated.grade,
                    "defaultPolicyId" to updated.defaultPolicyId,
                    "defaultFocusMinutes" to updated.defaultFocusMinutes,
                    "pairedCardId" to updated.pairedCardId,
                    "deviceName" to updated.deviceName,
                    "isOnline" to updated.isOnline
                )
                firestore.collection("children").document(updated.id).set(data, SetOptions.merge())
            }
        } catch (e: Exception) {
            PlatformPermissionsHelper.showToast("Update failed: ${e.localizedMessage}")
        }
    }

    override fun reportProtectionDisabled(reason: String, childId: String) {
        super.reportProtectionDisabled(reason, childId)
        try {
            val targetDocId = childrenList.find { it.id == childId || it.userId == childId }?.id ?: childId
            firestore.collection("children").document(targetDocId).update(
                mapOf(
                    "protectionStatus" to "OFF",
                    "lastSeenAt" to currentTimeMillis()
                )
            )
        } catch (_: Exception) {}
    }

    override fun reportProtectionRestored(childId: String) {
        super.reportProtectionRestored(childId)
        try {
            val targetDocId = childrenList.find { it.id == childId || it.userId == childId }?.id ?: childId
            firestore.collection("children").document(targetDocId).update(
                mapOf(
                    "protectionStatus" to "ON",
                    "lastSeenAt" to currentTimeMillis()
                )
            )
        } catch (_: Exception) {}
    }

    override fun processChildHeartbeat(childId: String, permissionsMap: Map<String, Boolean>, battery: Int) {
        super.processChildHeartbeat(childId, permissionsMap, battery)
        try {
            val targetDocId = childrenList.find { it.id == childId || it.userId == childId }?.id ?: childId
            firestore.collection("children").document(targetDocId).update(
                mapOf(
                    "isOnline" to true,
                    "lastSeenAt" to currentTimeMillis()
                )
            )
        } catch (_: Exception) {}
    }

    override fun startRemoteSession(childId: String, policyId: String, policyName: String, durationMinutes: Int) {
        val now = currentTimeMillis()
        val durationMillis = durationMinutes * 60 * 1000L
        val endTimeMillis = now + durationMillis
        val famId = currentFamilyId()
        val sessionId = "sess_${now}_${(1000..9999).random()}"

        try {
            val sessionDoc = hashMapOf(
                "id" to sessionId,
                "familyId" to famId,
                "childId" to childId,
                "policyId" to policyId,
                "policyName" to policyName,
                "status" to "ACTIVE",
                "startedAt" to now,
                "endTimeMillis" to endTimeMillis,
                "durationMillis" to durationMillis,
                "triggerType" to "PARENT",
                "isActive" to true,
                "blockedAttempts" to 0,
                "lastHeartbeatAt" to now
            )
            val batch = firestore.batch()
            batch.set(firestore.collection("sessions").document(sessionId), sessionDoc)
            batch.update(firestore.collection("children").document(childId), mapOf(
                "currentSessionId" to sessionId,
                "isOnline" to true
            ))
            batch.commit()
                .addOnSuccessListener {
                    val cIdx = childrenList.indexOfFirst { it.id == childId }
                    if (cIdx >= 0) {
                        childrenList[cIdx] = childrenList[cIdx].copy(
                            isFocusing = true,
                            currentSessionId = sessionId,
                            remainingMinutes = durationMinutes,
                            focusEndsAtMillis = endTimeMillis
                        )
                    }
                    PlatformPermissionsHelper.showToast("Focus session started")
                }
                .addOnFailureListener { e ->
                    PlatformPermissionsHelper.showToast("Failed to start session: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            PlatformPermissionsHelper.showToast("Start session error: ${e.localizedMessage}")
        }
    }

    override fun stopRemoteSession(childId: String) {
        val now = currentTimeMillis()
        val child = childrenList.find { it.id == childId }
        val activeSessionId = child?.currentSessionId

        try {
            val batch = firestore.batch()
            if (!activeSessionId.isNullOrBlank()) {
                val sessRef = firestore.collection("sessions").document(activeSessionId)
                batch.update(sessRef, mapOf(
                    "status" to "ENDED_EARLY",
                    "endedAt" to now,
                    "endedBy" to "PARENT",
                    "isActive" to false
                ))
            }
            val childRef = firestore.collection("children").document(childId)
            batch.update(childRef, mapOf(
                "currentSessionId" to null
            ))
            batch.commit()
                .addOnSuccessListener {
                    val cIdx = childrenList.indexOfFirst { it.id == childId }
                    if (cIdx >= 0) {
                        childrenList[cIdx] = childrenList[cIdx].copy(
                            isFocusing = false,
                            currentSessionId = null,
                            remainingMinutes = 0
                        )
                    }
                    PlatformPermissionsHelper.showToast("Focus ended")
                }
                .addOnFailureListener { e ->
                    PlatformPermissionsHelper.showToast("Failed to end focus: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            PlatformPermissionsHelper.showToast("End focus error: ${e.localizedMessage}")
        }
    }

    override fun startChildSessionListener(context: Any?) {
        if (context is Context) {
            appContext = context.applicationContext
        } else if (appContext == null) {
            appContext = com.focal.app.ui.components.PlatformPermissionsHelper.appContext
        }
        val childId = FocalLocalStorage.getString("KEY_PAIRED_CHILD_ID", "")
        if (childId.isBlank()) return

        childSessionListenerReg?.remove()
        childSessionListenerReg = firestore.collection("sessions")
            .whereEqualTo("childId", childId)
            .whereEqualTo("status", "ACTIVE")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener
                if (snapshots.isEmpty) {
                    if (FocusEngine.instance.sessionState == SessionState.ACTIVE) {
                        ProtectionStateRepository.instance.updateFocusActive(active = false)
                        FocusEngine.instance.endFocus(TriggerType.SCHEDULED, "REMOTE_END")
                        appContext?.let { ctx ->
                            com.example.myapplication.service.FocusForegroundService.stop(ctx)
                        }
                    }
                } else {
                    val doc = snapshots.documents.first()
                    val sessionId = doc.id
                    val policyId = doc.getString("policyId") ?: "pol_study"
                    val policyName = doc.getString("policyName") ?: "Study Mode"
                    val endTimeMillis = doc.getLong("endTimeMillis") ?: 0L
                    val startedAt = doc.getLong("startedAt") ?: doc.getLong("startTimeMillis") ?: currentTimeMillis()
                    val now = currentTimeMillis()

                    if (now < endTimeMillis) {
                        val pol = policies.find { it.id == policyId }
                        val blockedApps = pol?.blockedApps ?: emptyList()
                        val blockedWebsites = pol?.blockedWebsites ?: emptyList()
                        val resolvedPkgs = blockedApps.map { com.focal.app.protection.ProtectionEngine.APP_NAME_TO_PACKAGE[it] ?: it }.toSet()

                        val policyData = FocusPolicyData(
                            blockedApps = blockedApps,
                            blockedPackages = resolvedPkgs,
                            blockedWebsites = blockedWebsites
                        )

                        val endsAtElapsed = android.os.SystemClock.elapsedRealtime() + (endTimeMillis - System.currentTimeMillis())
                        ProtectionStateRepository.instance.updateFocusActive(
                            active = true,
                            endsAtElapsed = endsAtElapsed,
                            policy = policyData
                        )

                        val remainingMins = ((endTimeMillis - now) / 60000L).toInt().coerceAtLeast(1)
                        val dummyPolicy = FocusPolicy(
                            id = policyId,
                            childId = childId,
                            name = policyName,
                            mode = FocusMode.STUDY,
                            durationMinutes = remainingMins,
                            blockedApps = blockedApps,
                            blockedWebsites = blockedWebsites
                        )
                        if (FocusEngine.instance.sessionState != SessionState.ACTIVE || FocusEngine.instance.currentSession?.id != sessionId) {
                            FocusEngine.instance.startFocusWithSyncedSession(sessionId, dummyPolicy, startedAt, endTimeMillis)
                            appContext?.let { ctx ->
                                com.example.myapplication.service.FocusForegroundService.start(ctx)
                            }
                        }
                    } else {
                        recordSessionCompleted(sessionId, childId, now, (now - startedAt).coerceAtLeast(0L))
                        appContext?.let { ctx ->
                            com.example.myapplication.service.FocusForegroundService.stop(ctx)
                        }
                    }
                }
            }

        childExitRequestListenerReg?.remove()
        childExitRequestListenerReg = firestore.collection("exitRequests")
            .whereEqualTo("childId", childId)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener
                val parsed = snapshots.documents.mapNotNull { doc ->
                    try {
                        val reqId = doc.id
                        val sessId = doc.getString("sessionId") ?: ""
                        val reason = doc.getString("reason") ?: ""
                        val note = doc.getString("note") ?: ""
                        val status = doc.getString("status") ?: "PENDING"
                        val reqAt = doc.getLong("requestedAtMillis") ?: currentTimeMillis()
                        val reqChildId = doc.getString("childId") ?: childId
                        val reqChildName = doc.getString("childName") ?: currentUser.name.ifBlank { "Child" }
                        val resolvedBy = doc.getString("resolvedBy") ?: "Parent"

                        EmergencyExitRequest(
                            id = reqId,
                            sessionId = sessId,
                            childId = reqChildId,
                            childName = reqChildName,
                            reason = reason,
                            note = note,
                            requestedAt = "Just now",
                            requestedAtMillis = reqAt,
                            status = status
                        )
                    } catch (_: Exception) {
                        null
                    }
                }

                for (req in parsed) {
                    val existingIdx = emergencyRequests.indexOfFirst { it.id == req.id }
                    if (existingIdx >= 0) {
                        emergencyRequests[existingIdx] = req
                    } else {
                        emergencyRequests.add(0, req)
                    }

                    when (req.status) {
                        "APPROVED" -> {
                            com.focal.app.data.EmergencyExitRepository.instance.notifyApproved(req.id)
                            if (FocusEngine.instance.sessionState == SessionState.ACTIVE) {
                                ProtectionStateRepository.instance.updateFocusActive(active = false)
                                FocusEngine.instance.endFocus(TriggerType.EMERGENCY_OVERRIDE, "Parent Approved Emergency Exit")
                                appContext?.let { ctx ->
                                    com.example.myapplication.service.FocusForegroundService.stop(ctx)
                                }
                            }
                        }
                        "DECLINED" -> {
                            val resolvedBy = snapshots.documents.find { it.id == req.id }?.getString("resolvedBy") ?: "Parent"
                            com.focal.app.data.EmergencyExitRepository.instance.notifyDeclined(req.id, resolvedBy)
                        }
                        "PENDING" -> {
                            com.focal.app.data.EmergencyExitRepository.instance.notifyPending(req)
                        }
                    }
                }
            }
    }

    override fun stopChildSessionListener() {
        childSessionListenerReg?.remove()
        childSessionListenerReg = null
        childExitRequestListenerReg?.remove()
        childExitRequestListenerReg = null
    }

    override fun scanFamilyFocusQr(payload: String, onComplete: (Result<String>) -> Unit) {
        val trimmed = payload.trim()
        val fParam = trimmed.substringAfter("f=", "").substringBefore("&").trim()
        val kParam = trimmed.substringAfter("k=", "").substringBefore("&").trim()
        val myFamId = FocalLocalStorage.getString("KEY_PAIRED_FAMILY_ID", "")
        val myChildId = FocalLocalStorage.getString("KEY_PAIRED_CHILD_ID", "")

        if (fParam.isBlank() || kParam.isBlank() || !trimmed.startsWith("focal://focus", ignoreCase = true)) {
            onComplete(Result.failure(Exception("That's not a FOCAL focus QR")))
            return
        }

        if (fParam != myFamId) {
            onComplete(Result.failure(Exception("This QR isn't for your family")))
            return
        }

        val now = currentTimeMillis()
        val engine = FocusEngine.instance

        if (engine.sessionState == SessionState.ACTIVE) {
            // Active session -> end early with FAMILY_QR and qrKey
            val sess = engine.currentSession
            val sessId = sess?.id ?: "unknown"
            val start = sess?.startTimeMillis ?: now
            val elapsed = (now - start).coerceAtLeast(0L)

            val batch = firestore.batch()
            val sessRef = firestore.collection("sessions").document(sessId)
            batch.update(sessRef, mapOf(
                "status" to "ENDED_EARLY",
                "endedAt" to now,
                "actualDurationMillis" to elapsed,
                "blockedAttempts" to engine.blockedAttemptsCount,
                "isActive" to false,
                "endedBy" to "FAMILY_QR",
                "qrKey" to kParam
            ))
            if (myChildId.isNotBlank()) {
                val childRef = firestore.collection("children").document(myChildId)
                batch.update(childRef, mapOf(
                    "currentSessionId" to null,
                    "isOnline" to true,
                    "lastSeenAt" to now
                ))
            }
            batch.commit()
                .addOnSuccessListener {
                    engine.endFocus(TriggerType.QR, payload)
                    onComplete(Result.success("Focus ended with Family QR"))
                }
                .addOnFailureListener { e ->
                    if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                        onComplete(Result.failure(Exception("This QR was reset — ask your parent for the new one")))
                    } else {
                        onComplete(Result.failure(Exception("Couldn't reach the internet")))
                    }
                }
        } else {
            // No active session -> start new session
            val sessionId = "sess_${now}_${(1000..9999).random()}"
            val pol = policies.firstOrNull()
            val polId = pol?.id ?: "pol_study"
            val polName = pol?.name ?: "Study Mode"
            val durationMinutes = 90
            val durationMillis = durationMinutes * 60 * 1000L
            val endTimeMillis = now + durationMillis

            val sessionDoc = hashMapOf(
                "id" to sessionId,
                "familyId" to myFamId,
                "childId" to myChildId,
                "policyId" to polId,
                "policyName" to polName,
                "status" to "ACTIVE",
                "startedAt" to now,
                "endTimeMillis" to endTimeMillis,
                "durationMillis" to durationMillis,
                "triggerType" to "FAMILY_QR",
                "isActive" to true,
                "blockedAttempts" to 0,
                "lastHeartbeatAt" to now
            )
            val batch = firestore.batch()
            batch.set(firestore.collection("sessions").document(sessionId), sessionDoc)
            if (myChildId.isNotBlank()) {
                batch.update(firestore.collection("children").document(myChildId), mapOf(
                    "isOnline" to true,
                    "lastSeenAt" to now
                ))
            }
            batch.commit()
                .addOnSuccessListener {
                    engine.startFocusWithQr(payload, currentUser)
                    onComplete(Result.success("Focus started with Family QR"))
                }
                .addOnFailureListener { e ->
                    if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                        onComplete(Result.failure(Exception("This QR was reset — ask your parent for the new one")))
                    } else {
                        onComplete(Result.failure(Exception("Couldn't reach the internet")))
                    }
                }
        }
    }

    override fun recordSessionStart(session: FocusSession) {
        val famId = currentFamilyId()
        val targetChildId = childrenList.find { it.userId == session.userId || it.id == session.userId }?.id ?: session.userId
        val sessionDoc = hashMapOf(
            "id" to session.id,
            "familyId" to famId,
            "childId" to targetChildId,
            "status" to "ACTIVE",
            "endTimeMillis" to session.endTimeMillis,
            "startedAt" to session.startTimeMillis,
            "durationMillis" to (session.targetDurationMinutes * 60 * 1000L),
            "policyId" to session.policyId,
            "policyName" to session.policyName,
            "triggerType" to session.triggerType.name,
            "isActive" to true,
            "blockedAttempts" to 0,
            "lastHeartbeatAt" to currentTimeMillis()
        )
        try {
            val batch = firestore.batch()
            batch.set(firestore.collection("sessions").document(session.id), sessionDoc)
            if (currentUser.role != UserRole.CHILD && com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isAnonymous != true) {
                batch.update(firestore.collection("children").document(targetChildId), mapOf(
                    "currentSessionId" to session.id,
                    "isOnline" to true
                ))
            } else {
                batch.update(firestore.collection("children").document(targetChildId), mapOf(
                    "isOnline" to true,
                    "lastSeenAt" to currentTimeMillis()
                ))
            }
            batch.commit()
        } catch (_: Exception) {}
    }

    override fun recordSessionHeartbeat(sessionId: String, childId: String, blockedAttempts: Int) {
        try {
            firestore.collection("sessions").document(sessionId).update(
                mapOf(
                    "blockedAttempts" to blockedAttempts,
                    "lastHeartbeatAt" to currentTimeMillis()
                )
            )
        } catch (_: Exception) {}
    }

    override fun recordSessionCompleted(sessionId: String, childId: String, endedAt: Long, actualDurationMillis: Long) {
        val targetChildId = childrenList.find { it.userId == childId || it.id == childId }?.id ?: childId
        try {
            val batch = firestore.batch()
            val sessRef = firestore.collection("sessions").document(sessionId)
            batch.update(sessRef, mapOf(
                "status" to "COMPLETED",
                "endedAt" to endedAt,
                "actualDurationMillis" to actualDurationMillis,
                "blockedAttempts" to FocusEngine.instance.blockedAttemptsCount,
                "isActive" to false
            ))
            val childRef = firestore.collection("children").document(targetChildId)
            batch.update(childRef, mapOf(
                "currentSessionId" to null,
                "isOnline" to true,
                "lastSeenAt" to endedAt
            ))
            batch.commit()
        } catch (_: Exception) {}
    }

    override fun recordSessionEndedEarly(sessionId: String, childId: String, endedAt: Long) {
        val targetChildId = childrenList.find { it.userId == childId || it.id == childId }?.id ?: childId
        if (currentUser.role == UserRole.CONTROLLER || currentUser.role == UserRole.ADMIN) {
            try {
                val batch = firestore.batch()
                val sessRef = firestore.collection("sessions").document(sessionId)
                batch.update(sessRef, mapOf(
                    "status" to "ENDED_EARLY",
                    "endedAt" to endedAt,
                    "endedBy" to "PARENT",
                    "isActive" to false
                ))
                val childRef = firestore.collection("children").document(targetChildId)
                batch.update(childRef, mapOf(
                    "currentSessionId" to null
                ))
                batch.commit()
            } catch (_: Exception) {}
        }
    }

    override fun submitEmergencyExitRequest(
        sessionId: String,
        reason: String,
        note: String,
        childId: String,
        childName: String
    ): EmergencyExitRequest {
        val now = currentTimeMillis()
        val famId = currentFamilyId()
        val targetChildId = childId.ifBlank { FocalLocalStorage.getString("KEY_PAIRED_CHILD_ID", "") }
        val targetChildName = childName.ifBlank { currentUser.name.ifBlank { "Child" } }
        val requestId = "req_${now}_${(1000..9999).random()}"

        val request = EmergencyExitRequest(
            id = requestId,
            sessionId = sessionId,
            childId = targetChildId,
            childName = targetChildName,
            reason = reason,
            note = note,
            requestedAt = "Just now",
            requestedAtMillis = now,
            status = "PENDING"
        )

        if (targetChildId.isBlank() || famId.isBlank()) {
            emergencyRequests.add(0, request)
            com.focal.app.data.EmergencyExitRepository.instance.notifyPending(request)
            return request
        }

        try {
            emergencyRequests.forEachIndexed { index, req ->
                if (req.status == "PENDING" && (targetChildId.isBlank() || req.childId == targetChildId)) {
                    emergencyRequests[index] = req.copy(status = "CANCELLED")
                    firestore.collection("exitRequests").document(req.id).update(
                        mapOf("status" to "CANCELLED", "resolvedAt" to now)
                    )
                }
            }

            emergencyRequests.add(0, request)
            FocalLocalStorage.saveString("KEY_ACTIVE_EXIT_REQUEST_ID", requestId)
            com.focal.app.data.EmergencyExitRepository.instance.notifyPending(request)

            val docData = hashMapOf(
                "id" to requestId,
                "familyId" to famId,
                "sessionId" to sessionId,
                "reason" to reason,
                "note" to note,
                "status" to "PENDING",
                "requestedAtMillis" to now,
                "childId" to targetChildId,
                "childName" to targetChildName
            )
            firestore.collection("exitRequests").document(requestId).set(docData)

            ActivityRepository.instance.recordEvent(
                type = ActivityEventType.EXIT_REQUESTED,
                childId = targetChildId,
                details = reason,
                metadata = mapOf("reason" to reason, "requestId" to requestId)
            )

            return request
        } catch (e: Exception) {
            PlatformPermissionsHelper.showToast("Exit request error: ${e.localizedMessage}")
            return request
        }
    }

    override fun resolveEmergencyRequest(requestId: String, approve: Boolean) {
        val now = currentTimeMillis()
        val idx = emergencyRequests.indexOfFirst { it.id == requestId }
        val req = if (idx >= 0) emergencyRequests[idx] else null

        val statusStr = if (approve) "APPROVED" else "DECLINED"
        val parentName = controllerUser.name.ifBlank { "Parent" }

        try {
            val batch = firestore.batch()
            val reqRef = firestore.collection("exitRequests").document(requestId)
            batch.update(reqRef, mapOf(
                "status" to statusStr,
                "resolvedAt" to now,
                "resolvedBy" to parentName
            ))

            if (approve && req != null) {
                if (req.sessionId.isNotBlank()) {
                    val sessRef = firestore.collection("sessions").document(req.sessionId)
                    batch.update(sessRef, mapOf(
                        "status" to "ENDED_EARLY",
                        "endedAt" to now,
                        "endedBy" to "PARENT",
                        "isActive" to false
                    ))
                }
                if (req.childId.isNotBlank()) {
                    val childRef = firestore.collection("children").document(req.childId)
                    batch.update(childRef, mapOf(
                        "currentSessionId" to null
                    ))
                }
            }

            batch.commit()
                .addOnSuccessListener {
                    if (idx >= 0 && req != null) {
                        emergencyRequests[idx] = req.copy(status = statusStr)
                    }
                    if (approve && req != null) {
                        val cIdx = childrenList.indexOfFirst { it.id == req.childId }
                        if (cIdx >= 0) {
                            childrenList[cIdx] = childrenList[cIdx].copy(
                                isFocusing = false,
                                currentSessionId = null,
                                remainingMinutes = 0
                            )
                        }
                        ActivityRepository.instance.recordEvent(
                            type = ActivityEventType.EXIT_APPROVED,
                            childId = req.childId,
                            details = "Approved emergency exit for ${req.childName}",
                            metadata = mapOf("reason" to req.reason)
                        )
                    } else if (req != null) {
                        ActivityRepository.instance.recordEvent(
                            type = ActivityEventType.EXIT_DECLINED,
                            childId = req.childId,
                            details = "Declined emergency exit for ${req.childName}",
                            metadata = mapOf("reason" to req.reason)
                        )
                    }
                }
                .addOnFailureListener { e ->
                    PlatformPermissionsHelper.showToast("Resolution failed: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            PlatformPermissionsHelper.showToast("Resolution error: ${e.localizedMessage}")
        }
    }

    override fun cancelEmergencyExitRequest(childId: String, requestId: String?) {
        val targetChildId = childId.ifBlank { FocalLocalStorage.getString("KEY_PAIRED_CHILD_ID", "") }
        val now = currentTimeMillis()
        try {
            emergencyRequests.forEachIndexed { index, req ->
                if (req.status == "PENDING" && (requestId == null || req.id == requestId) && (targetChildId.isBlank() || req.childId == targetChildId)) {
                    emergencyRequests[index] = req.copy(status = "CANCELLED")
                    firestore.collection("exitRequests").document(req.id).update(
                        mapOf(
                            "status" to "CANCELLED",
                            "resolvedAt" to now
                        )
                    )
                }
            }
        } catch (_: Exception) {}
    }

    override fun expirePendingEmergencyRequests(childId: String?) {
        val targetChildId = childId?.ifBlank { FocalLocalStorage.getString("KEY_PAIRED_CHILD_ID", "") }
        val now = currentTimeMillis()
        try {
            emergencyRequests.forEachIndexed { index, req ->
                if (req.status == "PENDING" && (targetChildId == null || targetChildId.isBlank() || req.childId == targetChildId)) {
                    emergencyRequests[index] = req.copy(status = "EXPIRED")
                    firestore.collection("exitRequests").document(req.id).update(
                        mapOf(
                            "status" to "EXPIRED",
                            "resolvedAt" to now
                        )
                    )
                }
            }
        } catch (_: Exception) {}
    }

    // Policies in Firestore: families/{familyId}/policies/{policyId}
    override fun addPolicy(policy: FocusPolicy) {
        val index = policies.indexOfFirst { it.id == policy.id }
        if (index >= 0) {
            policies[index] = policy
        } else {
            policies.add(0, policy)
        }
        val famId = currentFamilyId()
        if (famId.isNotBlank()) {
            try {
                val docData = hashMapOf(
                    "name" to policy.name,
                    "blockedApps" to policy.blockedApps,
                    "blockedWebsites" to policy.blockedWebsites,
                    "updatedAt" to currentTimeMillis()
                )
                firestore.collection("families").document(famId)
                    .collection("policies").document(policy.id).set(docData)
            } catch (e: Exception) {
                PlatformPermissionsHelper.showToast("Failed to save policy: ${e.localizedMessage}")
            }
        }
    }

    override fun updatePolicy(updated: FocusPolicy) {
        val index = policies.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            policies[index] = updated
        }
        val famId = currentFamilyId()
        if (famId.isNotBlank()) {
            try {
                val docData = hashMapOf(
                    "name" to updated.name,
                    "blockedApps" to updated.blockedApps,
                    "blockedWebsites" to updated.blockedWebsites,
                    "updatedAt" to currentTimeMillis()
                )
                firestore.collection("families").document(famId)
                    .collection("policies").document(updated.id).set(docData, SetOptions.merge())
            } catch (e: Exception) {
                PlatformPermissionsHelper.showToast("Failed to update policy: ${e.localizedMessage}")
            }
        }
    }

    override fun deletePolicy(policyId: String) {
        policies.removeAll { it.id == policyId }
        val famId = currentFamilyId()
        if (famId.isNotBlank()) {
            try {
                firestore.collection("families").document(famId)
                    .collection("policies").document(policyId).delete()
            } catch (e: Exception) {
                PlatformPermissionsHelper.showToast("Failed to delete policy: ${e.localizedMessage}")
            }
        }
    }

    override fun deletePolicyWithReplacement(policyId: String, replacementPolicyId: String) {
        super.deletePolicyWithReplacement(policyId, replacementPolicyId)
        deletePolicy(policyId)
    }

    // Schedules in Firestore: families/{familyId}/schedules/{scheduleId}
    override fun saveSchedule(schedule: ScheduleItem) {
        val index = schedules.indexOfFirst { it.id == schedule.id }
        if (index >= 0) {
            schedules[index] = schedule
        } else {
            schedules.add(0, schedule)
        }
        val famId = currentFamilyId()
        if (famId.isNotBlank()) {
            try {
                val (startM, endM) = FocalScheduleManager.parseTimeRange(schedule.timeRange)
                val daysList = schedule.days.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val sDoc = hashMapOf(
                    "policyId" to schedule.policyName,
                    "childIds" to if (schedule.childId.isNotBlank()) listOf(schedule.childId) else emptyList<String>(),
                    "days" to daysList,
                    "startMinute" to startM,
                    "endMinute" to endM,
                    "enabled" to schedule.isEnabled,
                    "updatedAt" to currentTimeMillis()
                )
                firestore.collection("families").document(famId)
                    .collection("schedules").document(schedule.id).set(sDoc)
            } catch (e: Exception) {
                PlatformPermissionsHelper.showToast("Failed to save schedule: ${e.localizedMessage}")
            }
        }
    }

    override fun toggleSchedule(scheduleId: String) {
        val index = schedules.indexOfFirst { it.id == scheduleId }
        if (index >= 0) {
            val item = schedules[index]
            val newEnabled = !item.isEnabled
            schedules[index] = item.copy(isEnabled = newEnabled)
            val famId = currentFamilyId()
            if (famId.isNotBlank()) {
                try {
                    firestore.collection("families").document(famId)
                        .collection("schedules").document(scheduleId).update(
                            mapOf("enabled" to newEnabled, "updatedAt" to currentTimeMillis())
                        )
                } catch (e: Exception) {
                    PlatformPermissionsHelper.showToast("Failed to toggle schedule: ${e.localizedMessage}")
                }
            }
        }
    }

    override fun deleteSchedule(scheduleId: String) {
        schedules.removeAll { it.id == scheduleId }
        val famId = currentFamilyId()
        if (famId.isNotBlank()) {
            try {
                firestore.collection("families").document(famId)
                    .collection("schedules").document(scheduleId).delete()
            } catch (e: Exception) {
                PlatformPermissionsHelper.showToast("Failed to delete schedule: ${e.localizedMessage}")
            }
        }
    }
}
