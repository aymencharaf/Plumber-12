package com.example.data.sync

import android.content.Context
import android.util.Log

import com.example.data.db.PlumberDatabase
import com.example.data.model.AuditLog
import com.example.data.model.Project
import com.example.data.model.ProjectItem
import com.example.data.model.TeamUser

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FirestoreSync {

    private const val TAG = "FirestoreSync"

    private var projectsListenerRegistration: ListenerRegistration? = null
    private var projectItemsListenerRegistration: ListenerRegistration? = null

    // ============================================================
    // FIREBASE
    // ============================================================

    private fun getDb(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseFirestore error: ${e.message}", e)
            null
        }
    }

    fun getCurrentUserUid(): String {
        return try {
            FirebaseAuth.getInstance()
                .currentUser
                ?.uid
                ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Unable to get Firebase UID", e)
            ""
        }
    }

    fun isFirebaseAuthenticated(): Boolean {
        return try {
            FirebaseAuth.getInstance()
                .currentUser != null
        } catch (e: Exception) {
            false
        }
    }

    fun signOutFirebase() {
        try {
            FirebaseAuth.getInstance().signOut()
            stopRealtimeListener()

            Log.d(TAG, "Firebase signed out.")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase sign out error", e)
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private fun normalizeRole(role: String): String {
        return when (role.trim().uppercase()) {
            "ADMIN" -> "ADMIN"
            "MANAGER" -> "MANAGER"
            "WORKER" -> "WORKER"
            else -> "WORKER"
        }
    }

    private fun isManagementRole(role: String): Boolean {
        return when (normalizeRole(role)) {
            "ADMIN", "MANAGER" -> true
            else -> false
        }
    }

    private fun resolveWorkshopId(
        projectWorkshopId: String,
        providedWorkshopId: String
    ): String {
        return if (projectWorkshopId.isNotBlank()) {
            projectWorkshopId.trim()
        } else {
            providedWorkshopId.trim()
        }
    }

    private fun parseAssignedWorkers(
        assignedWorkers: String
    ): List<String> {
        return assignedWorkers
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun readAssignedWorkers(
        doc: DocumentSnapshot
    ): String {
        return when (val value = doc.get("assignedWorkers")) {

            is List<*> -> {
                value
                    .filterIsInstance<String>()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(",")
            }

            is String -> {
                value
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(",")
            }

            else -> ""
        }
    }

    private fun firebasePermissionDenied(
        e: Exception
    ): Boolean {
        return e is FirebaseFirestoreException &&
                e.code ==
                FirebaseFirestoreException.Code.PERMISSION_DENIED
    }

    // ============================================================
    // REALTIME SYNC
    // ============================================================

    fun startRealtimeListener(
        context: Context,
        userRole: String,
        workerUid: String = "",
        workshopId: String = ""
    ) {

        val db = getDb() ?: return

        if (!isFirebaseAuthenticated()) {
            Log.w(TAG, "Firebase authentication required.")
            return
        }

        val cleanWorkshopId = workshopId.trim()

        if (cleanWorkshopId.isBlank()) {
            Log.w(TAG, "workshopId is empty. Realtime sync skipped.")
            return
        }

        val normalizedRole = normalizeRole(userRole)
        val isManagement = isManagementRole(normalizedRole)

        val targetUid =
            workerUid.trim().ifBlank {
                getCurrentUserUid()
            }

        if (targetUid.isBlank()) {
            Log.w(TAG, "Target UID is empty.")
            return
        }

        stopRealtimeListener()

        val plumberDb = PlumberDatabase.getDatabase(context)
        val scope = CoroutineScope(Dispatchers.IO)

        Log.d(
            TAG,
            "Starting realtime sync: role=$normalizedRole uid=$targetUid workshop=$cleanWorkshopId"
        )

        // ========================================================
        // PROJECTS LISTENER
        // ========================================================

        val projectsQuery =
            when {
                isManagement -> {
                    db.collection("projects")
                        .whereEqualTo("workshopId", cleanWorkshopId)
                }

                normalizedRole == "WORKER" -> {
                    db.collection("projects")
                        .whereEqualTo("workshopId", cleanWorkshopId)
                        .whereArrayContains(
                            "assignedWorkers",
                            targetUid
                        )
                }

                else -> {
                    Log.w(TAG, "Unknown role: $normalizedRole")
                    return
                }
            }

        projectsListenerRegistration =
            projectsQuery.addSnapshotListener { snapshot, error ->

                if (error != null) {

                    if (firebasePermissionDenied(error)) {
                        Log.w(
                            TAG,
                            "Projects permission denied: ${error.message}"
                        )
                    } else {
                        Log.e(
                            TAG,
                            "Projects listener error: ${error.message}",
                            error
                        )
                    }

                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                scope.launch {

                    snapshot.documents.forEach { doc ->

                        try {

                            val id =
                                doc.getLong("id")
                                    ?: doc.id.toLongOrNull()
                                    ?: 0L

                            if (id <= 0L) return@forEach

                            val projectWorkshopId =
                                doc.getString("workshopId")
                                    ?: cleanWorkshopId

                            if (projectWorkshopId != cleanWorkshopId) {
                                return@forEach
                            }

                            val project = Project(
                                id = id,

                                title =
                                    doc.getString("name")
                                        ?: doc.getString("title")
                                        ?: "مشروع جديد",

                                clientName =
                                    doc.getString("clientName")
                                        ?: "",

                                location =
                                    doc.getString("location")
                                        ?: doc.getString("address")
                                        ?: "",

                                notes =
                                    doc.getString("notes")
                                        ?: "",

                                workTypeKey =
                                    doc.getString("workTypeKey")
                                        ?: "CUSTOM",

                                workTypeNameAr =
                                    doc.getString("workTypeNameAr")
                                        ?: "عمل مخصص",

                                workerName =
                                    doc.getString("workerName")
                                        ?: "",

                                managerName =
                                    doc.getString("managerName")
                                        ?: "",

                                storePhone =
                                    doc.getString("storePhone")
                                        ?: "",

                                orderStatus =
                                    doc.getString("orderStatus")
                                        ?: "NEW",

                                assignedWorkers =
                                    readAssignedWorkers(doc),

                                createdBy =
                                    doc.getString("createdBy")
                                        ?: "",

                                status =
                                    doc.getString("status")
                                        ?: "NEW",

                                laborCost =
                                    doc.getDouble("laborCost")
                                        ?: 0.0,

                                paidAmount =
                                    doc.getDouble("paidAmount")
                                        ?: 0.0,

                                workshopId =
                                    projectWorkshopId,

                                createdAt =
                                    doc.getLong("createdAt")
                                        ?: System.currentTimeMillis(),

                                updatedAt =
                                    doc.getLong("updatedAt")
                                        ?: System.currentTimeMillis()
                            )

                            plumberDb
                                .projectDao()
                                .insertProject(project)

                        } catch (e: Exception) {

                            Log.e(
                                TAG,
                                "Project parsing error: ${doc.id}",
                                e
                            )
                        }
                    }
                }
            }

        // ========================================================
        // PROJECT ITEMS LISTENER
        // ========================================================

        val projectItemsQuery =
            if (isManagement) {

                db.collection("project_items")
                    .whereEqualTo(
                        "workshopId",
                        cleanWorkshopId
                    )

            } else {

                db.collection("project_items")
                    .whereEqualTo(
                        "workshopId",
                        cleanWorkshopId
                    )
                    .whereEqualTo(
                        "workerId",
                        targetUid
                    )
            }

        projectItemsListenerRegistration =
            projectItemsQuery.addSnapshotListener { snapshot, error ->

                if (error != null) {

                    if (firebasePermissionDenied(error)) {
                        Log.w(
                            TAG,
                            "Project items permission denied: ${error.message}"
                        )
                    } else {
                        Log.e(
                            TAG,
                            "Project items listener error",
                            error
                        )
                    }

                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                scope.launch {

                    snapshot.documents.forEach { doc ->

                        try {

                            val id =
                                doc.getLong("id")
                                    ?: doc.id.toLongOrNull()
                                    ?: 0L

                            val projectId =
                                doc.getLong("projectId")
                                    ?: 0L

                            if (id <= 0L || projectId <= 0L) {
                                return@forEach
                            }

                            val item = ProjectItem(

                                id = id,

                                projectId = projectId,

                                materialKey =
                                    doc.getString("materialKey")
                                        ?: doc.getString("materialId")
                                        ?: "custom",

                                materialNameAr =
                                    doc.getString("materialNameAr")
                                        ?: doc.getString("materialName")
                                        ?: "مادة",

                                materialNameFr =
                                    doc.getString("materialNameFr")
                                        ?: "",

                                category =
                                    doc.getString("category")
                                        ?: "CUSTOM",

                                size =
                                    doc.getString("size")
                                        ?: "",

                                quantity =
                                    doc.getDouble("quantity")
                                        ?: 1.0,

                                unit =
                                    doc.getString("unit")
                                        ?: "قطعة",

                                unitPrice =
                                    doc.getDouble("unitPrice")
                                        ?: 0.0,

                                standardPipeLengthMeters =
                                    doc.getDouble(
                                        "standardPipeLengthMeters"
                                    ) ?: 4.0,

                                isPurchased =
                                    doc.getBoolean("isPurchased")
                                        ?: false,

                                notes =
                                    doc.getString("notes")
                                        ?: "",

                                iconType =
                                    doc.getString("iconType")
                                        ?: "elbow",

                                imageUri =
                                    doc.getString("imageUri"),

                                workshopId =
                                    doc.getString("workshopId")
                                        ?: cleanWorkshopId,

                                createdAt =
                                    doc.getLong("createdAt")
                                        ?: System.currentTimeMillis()
                            )

                            plumberDb
                                .projectItemDao()
                                .insertItem(item)

                        } catch (e: Exception) {

                            Log.e(
                                TAG,
                                "Project item parsing error: ${doc.id}",
                                e
                            )
                        }
                    }
                }
            }
    }

    fun stopRealtimeListener() {

        projectsListenerRegistration?.remove()
        projectsListenerRegistration = null

        projectItemsListenerRegistration?.remove()
        projectItemsListenerRegistration = null

        Log.d(TAG, "Realtime listeners stopped.")
    }

    // ============================================================
    // PROJECT SYNC
    // ============================================================

    fun syncProjectToFirestore(
        project: Project,
        workshopId: String = ""
    ) {

        val db = getDb() ?: return

        val currentUid = getCurrentUserUid()

        if (currentUid.isBlank()) {
            Log.w(
                TAG,
                "Cannot sync project: unauthenticated."
            )
            return
        }

        val currentWorkshop =
            resolveWorkshopId(
                project.workshopId,
                workshopId
            )

        if (currentWorkshop.isBlank()) {
            Log.w(
                TAG,
                "Cannot sync project: workshopId empty."
            )
            return
        }

        val assignedWorkers =
            parseAssignedWorkers(
                project.assignedWorkers
            )

        val projectMap = mapOf(

            "id" to project.id,

            "name" to project.title,

            "title" to project.title,

            "clientName" to project.clientName,

            "location" to project.location,

            "address" to project.location,

            "notes" to project.notes,

            "workTypeKey" to project.workTypeKey,

            "workTypeNameAr" to project.workTypeNameAr,

            "workerName" to project.workerName,

            "managerName" to project.managerName,

            "storePhone" to project.storePhone,

            "orderStatus" to project.orderStatus,

            "assignedWorkers" to assignedWorkers,

            "createdBy" to project.createdBy,

            "status" to project.status,

            "laborCost" to project.laborCost,

            "paidAmount" to project.paidAmount,

            "workshopId" to currentWorkshop,

            "createdAt" to project.createdAt,

            "updatedAt" to project.updatedAt,

            "updatedBy" to currentUid
        )

        db.collection("projects")
            .document(project.id.toString())
            .set(
                projectMap,
                SetOptions.merge()
            )
            .addOnSuccessListener {

                Log.d(
                    TAG,
                    "Project synced: ${project.id}"
                )
            }
            .addOnFailureListener { e ->

                if (firebasePermissionDenied(e)) {

                    Log.w(
                        TAG,
                        "Project sync denied: ${project.id}"
                    )

                } else {

                    Log.e(
                        TAG,
                        "Project sync error: ${project.id}",
                        e
                    )
                }
            }
    }

    // ============================================================
    // PROJECT ITEM SYNC
    // ============================================================

    fun syncProjectItemToFirestore(
        item: ProjectItem,
        workerId: String = "",
        workerName: String = "",
        workshopId: String = ""
    ) {

        val db = getDb() ?: return

        val currentUid = getCurrentUserUid()

        if (currentUid.isBlank()) {

            Log.w(
                TAG,
                "Cannot sync item: unauthenticated."
            )

            return
        }

        val currentWorkshop =
            if (item.workshopId.isNotBlank()) {

                item.workshopId.trim()

            } else {

                workshopId.trim()
            }

        if (currentWorkshop.isBlank()) {

            Log.w(
                TAG,
                "Cannot sync item: workshopId empty."
            )

            return
        }

        val ownerWorkerId =
            workerId.trim().ifBlank {
                currentUid
            }

        val itemMap = mapOf(

            "id" to item.id,

            "projectId" to item.projectId,

            "materialId" to item.materialKey,

            "materialKey" to item.materialKey,

            "materialName" to item.materialNameAr,

            "materialNameAr" to item.materialNameAr,

            "materialNameFr" to item.materialNameFr,

            "category" to item.category,

            "size" to item.size,

            "quantity" to item.quantity,

            "unit" to item.unit,

            "unitPrice" to item.unitPrice,

            "standardPipeLengthMeters" to
                    item.standardPipeLengthMeters,

            "isPurchased" to item.isPurchased,

            "notes" to item.notes,

            "iconType" to item.iconType,

            "imageUri" to item.imageUri,

            "workerId" to ownerWorkerId,

            "workerName" to workerName,

            "workshopId" to currentWorkshop,

            "createdAt" to item.createdAt,

            "updatedAt" to System.currentTimeMillis(),

            "updatedBy" to currentUid
        )

        val docId =
            if (item.id > 0L) {

                item.id.toString()

            } else {

                "${item.projectId}_${item.materialKey}_${item.size}"
            }

        db.collection("project_items")
            .document(docId)
            .set(
                itemMap,
                SetOptions.merge()
            )
            .addOnSuccessListener {

                Log.d(
                    TAG,
                    "Project item synced: $docId"
                )
            }
            .addOnFailureListener { e ->

                if (firebasePermissionDenied(e)) {

                    Log.w(
                        TAG,
                        "Project item sync denied: $docId"
                    )

                } else {

                    Log.e(
                        TAG,
                        "Project item sync error: $docId",
                        e
                    )
                }
            }
    }

    // ============================================================
    // DELETE PROJECT
    // ============================================================

    fun deleteProjectFromFirestore(
        projectId: Long
    ) {

        val db = getDb() ?: return

        if (!isFirebaseAuthenticated()) {

            Log.w(
                TAG,
                "Cannot delete project: unauthenticated."
            )

            return
        }

        db.collection("projects")
            .document(projectId.toString())
            .delete()
            .addOnSuccessListener {

                Log.d(
                    TAG,
                    "Project deleted: $projectId"
                )
            }
            .addOnFailureListener { e ->

                if (firebasePermissionDenied(e)) {

                    Log.w(
                        TAG,
                        "Project deletion denied: $projectId"
                    )

                } else {

                    Log.e(
                        TAG,
                        "Project deletion error: $projectId",
                        e
                    )
                }
            }
    }

    // ============================================================
    // DELETE PROJECT ITEM
    // ============================================================

    fun deleteProjectItemFromFirestore(
        itemId: Long
    ) {

        val db = getDb() ?: return

        if (!isFirebaseAuthenticated()) {

            Log.w(
                TAG,
                "Cannot delete project item."
            )

            return
        }

        db.collection("project_items")
            .document(itemId.toString())
            .delete()
            .addOnSuccessListener {

                Log.d(
                    TAG,
                    "Project item deleted: $itemId"
                )
            }
            .addOnFailureListener { e ->

                if (firebasePermissionDenied(e)) {

                    Log.w(
                        TAG,
                        "Project item deletion denied: $itemId"
                    )

                } else {

                    Log.e(
                        TAG,
                        "Project item deletion error: $itemId",
                        e
                    )
                }
            }
    }

    // ============================================================
    // CURRENT USER PROFILE
    // ============================================================

    fun syncUserToFirestore(
        user: TeamUser,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {

        val db = getDb()

        if (db == null) {

            onResult?.invoke(
                false,
                "Cloud Firestore غير متصل."
            )

            return
        }

        val currentUid = getCurrentUserUid()

        if (currentUid.isBlank() ||
            currentUid != user.uid
        ) {

            Log.w(
                TAG,
                "User UID security check failed."
            )

            onResult?.invoke(
                false,
                "خطأ أمني: UID غير متطابق."
            )

            return
        }

        val userMap = mapOf(

            "uid" to user.uid,

            "name" to user.name,

            "phone" to user.phone,

            "email" to user.email,

            "role" to normalizeRole(user.role),

            "active" to user.active,

            "workshopId" to user.workshopId,

            "createdAt" to user.createdAt,

            "lastLoginAt" to user.lastLoginAt
        )

        db.collection("users")
            .document(user.uid)
            .set(
                userMap,
                SetOptions.merge()
            )
            .addOnSuccessListener {

                Log.d(
                    TAG,
                    "User profile synced: ${user.uid}"
                )

                onResult?.invoke(
                    true,
                    "تم حفظ بيانات المستخدم."
                )
            }
            .addOnFailureListener { e ->

                Log.e(
                    TAG,
                    "User profile sync failed: ${e.message}",
                    e
                )

                onResult?.invoke(
                    false,
                    e.localizedMessage
                        ?: "فشل حفظ بيانات المستخدم."
                )
            }
    }

    // ============================================================
    // MANAGEMENT CREATE USER PROFILE
    // ============================================================

    fun createUserProfileByAdmin(
        user: TeamUser,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {

        val db = getDb()

        if (db == null) {

            onResult(
                false,
                "Cloud Firestore غير متصل."
            )

            return
        }

        val managerUid = getCurrentUserUid()

        if (managerUid.isBlank()) {

            onResult(
                false,
                "لا توجد جلسة مدير صالحة."
            )

            return
        }

        if (user.uid.isBlank()) {

            onResult(
                false,
                "UID العامل فارغ."
            )

            return
        }

        if (normalizeRole(user.role) != "WORKER") {

            onResult(
                false,
                "يمكن إنشاء حساب WORKER فقط."
            )

            return
        }

        val data = mapOf(

            "uid" to user.uid,

            "name" to user.name,

            "phone" to user.phone,

            "email" to user.email,

            "role" to "WORKER",

            "active" to user.active,

            "workshopId" to user.workshopId,

            "createdAt" to user.createdAt,

            "lastLoginAt" to user.lastLoginAt,

            "createdBy" to managerUid,

            "updatedBy" to managerUid,

            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(user.uid)
            .set(
                data,
                SetOptions.merge()
            )
            .addOnSuccessListener {

                Log.d(
                    TAG,
                    "Worker profile created: ${user.uid}"
                )

                onResult(
                    true,
                    "تم إنشاء ملف العامل في Firestore."
                )
            }
            .addOnFailureListener { e ->

                Log.e(
                    TAG,
                    "Worker profile creation failed",
                    e
                )

                onResult(
                    false,
                    e.localizedMessage
                        ?: "فشل إنشاء ملف العامل."
                )
            }
    }

    fun syncUserProfileAsManagement(
        user: TeamUser
    ) {
        createUserProfileByAdmin(user)
    }

    // ============================================================
    // MANAGEMENT UPDATE USER PROFILE
    // ============================================================

    fun updateUserProfileByAdmin(
        user: TeamUser,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {

        val db = getDb()

        if (db == null) {

            onResult(
                false,
                "Cloud Firestore غير متصل."
            )

            return
        }

        val managerUid = getCurrentUserUid()

        if (managerUid.isBlank()) {

            onResult(
                false,
                "لا توجد جلسة مدير صالحة."
            )

            return
        }

        if (user.uid.isBlank()) {

            onResult(
                false,
                "UID العامل فارغ."
            )

            return
        }

        if (normalizeRole(user.role) != "WORKER") {

            onResult(
                false,
                "يمكن تحديث WORKER فقط."
            )

            return
        }

        val data = mapOf(

            "uid" to user.uid,

            "name" to user.name,

            "phone" to user.phone,

            "email" to user.email,

            "role" to "WORKER",

            "active" to user.active,

            "workshopId" to user.workshopId,

            "updatedBy" to managerUid,

            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(user.uid)
            .set(
                data,
                SetOptions.merge()
            )
            .addOnSuccessListener {

                Log.d(
                    TAG,
                    "Worker profile updated: ${user.uid}"
                )

                onResult(
                    true,
                    "تم تحديث بيانات العامل."
                )
            }
            .addOnFailureListener { e ->

                Log.e(
                    TAG,
                    "Worker profile update failed",
                    e
                )

                onResult(
                    false,
                    e.localizedMessage
                        ?: "فشل تحديث بيانات العامل."
                )
            }
    }

    // ============================================================
    // FETCH CURRENT USER BY UID
    // ============================================================

    fun fetchCurrentUserFromFirestore(
        onResult: (TeamUser?) -> Unit
    ) {

        val db = getDb()

        if (db == null) {

            onResult(null)
            return
        }

        val uid = getCurrentUserUid()

        if (uid.isBlank()) {

            onResult(null)
            return
        }

        fetchUserFromFirestoreByUid(
            uid,
            onResult
        )
    }

    // ============================================================
    // FETCH USER BY UID
    // ============================================================

    fun fetchUserFromFirestoreByUid(
        uid: String,
        onResult: (TeamUser?) -> Unit
    ) {

        val db = getDb()

        if (db == null) {

            onResult(null)
            return
        }

        val cleanUid = uid.trim()

        if (cleanUid.isBlank()) {

            onResult(null)
            return
        }

        db.collection("users")
            .document(cleanUid)
            .get()
            .addOnSuccessListener { doc ->

                if (!doc.exists()) {

                    Log.w(
                        TAG,
                        "User profile does not exist: $cleanUid"
                    )

                    onResult(null)
                    return@addOnSuccessListener
                }

                val storedUid =
                    doc.getString("uid")
                        ?: doc.id

                if (storedUid != cleanUid) {

                    Log.w(
                        TAG,
                        "UID mismatch."
                    )

                    onResult(null)
                    return@addOnSuccessListener
                }

                onResult(
                    createTeamUserFromDocument(
                        doc,
                        ""
                    )
                )
            }
            .addOnFailureListener { e ->

                Log.e(
                    TAG,
                    "Error fetching user UID=$cleanUid",
                    e
                )

                onResult(null)
            }
    }

    // ============================================================
    // FETCH USER BY PHONE OR EMAIL
    // ============================================================

    fun fetchUserFromFirestoreByPhoneOrEmail(
        query: String,
        onResult: (TeamUser?) -> Unit
    ) {

        val db = getDb()

        if (db == null) {

            onResult(null)
            return
        }

        val cleanQuery = query.trim()

        if (cleanQuery.isBlank()) {

            onResult(null)
            return
        }

        db.collection("users")
            .whereEqualTo(
                "phone",
                cleanQuery
            )
            .get()
            .addOnSuccessListener { snapshots ->

                if (snapshots.isNotEmpty()) {

                    onResult(
                        createTeamUserFromDocument(
                            snapshots.documents.first(),
                            cleanQuery
                        )
                    )

                } else {

                    searchUserByEmail(
                        db,
                        cleanQuery,
                        onResult
                    )
                }
            }
            .addOnFailureListener {

                searchUserByEmail(
                    db,
                    cleanQuery,
                    onResult
                )
            }
    }

    private fun searchUserByEmail(
        db: FirebaseFirestore,
        query: String,
        onResult: (TeamUser?) -> Unit
    ) {

        db.collection("users")
            .whereEqualTo(
                "email",
                query
            )
            .get()
            .addOnSuccessListener { snapshots ->

                if (snapshots.isNotEmpty()) {

                    onResult(
                        createTeamUserFromDocument(
                            snapshots.documents.first(),
                            query
                        )
                    )

                } else {

                    onResult(null)
                }
            }
            .addOnFailureListener { e ->

                Log.e(
                    TAG,
                    "Email search failed",
                    e
                )

                onResult(null)
            }
    }

    // ============================================================
    // TEAM USER MAPPER
    // ============================================================

    private fun createTeamUserFromDocument(
        doc: DocumentSnapshot,
        fallbackQuery: String
    ): TeamUser {

        val role =
            normalizeRole(
                doc.getString("role")
                    ?: "WORKER"
            )

        return TeamUser(

            uid =
                doc.getString("uid")
                    ?: doc.id,

            name =
                doc.getString("name")
                    ?: "",

            phone =
                doc.getString("phone")
                    ?: if (
                    fallbackQuery.isNotBlank() &&
                    !fallbackQuery.contains("@")
                ) {
                    fallbackQuery
                } else {
                    ""
                },

            email =
                doc.getString("email")
                    ?: if (
                    fallbackQuery.contains("@")
                ) {
                    fallbackQuery
                } else {
                    ""
                },

            password = "",

            role = role,

            active =
                doc.getBoolean("active")
                    ?: true,

            workshopId =
                doc.getString("workshopId")
                    ?: "",

            createdAt =
                doc.getLong("createdAt")
                    ?: System.currentTimeMillis()
        )
    }

    // ============================================================
    // STORE SETTINGS
    // ============================================================

    fun syncStoreSettingsToFirestore(
        storeName: String,
        storePhone: String,
        storeWhatsapp: String,
        managerName: String,
        workshopId: String = ""
    ) {

        val db = getDb() ?: return

        val uid = getCurrentUserUid()

        if (uid.isBlank()) return

        val cleanWorkshopId =
            workshopId.trim()

        if (cleanWorkshopId.isBlank()) return

        val data = mapOf(

            "storeName" to storeName,

            "storePhone" to storePhone,

            "storeWhatsapp" to storeWhatsapp,

            "managerName" to managerName,

            "workshopId" to cleanWorkshopId,

            "updatedAt" to System.currentTimeMillis(),

            "updatedBy" to uid
        )

        db.collection("store_settings")
            .document(cleanWorkshopId)
            .set(
                data,
                SetOptions.merge()
            )
            .addOnSuccessListener {

                Log.d(
                    TAG,
                    "Store settings synced."
                )
            }
            .addOnFailureListener { e ->

                Log.e(
                    TAG,
                    "Store settings sync failed",
                    e
                )
            }
    }

    // ============================================================
    // AUDIT LOG
    // ============================================================

    fun syncAuditLogToFirestore(
        log: AuditLog,
        workshopId: String = ""
    ) {

        val db = getDb() ?: return

        val currentUid =
            getCurrentUserUid()

        if (currentUid.isBlank()) return

        val cleanWorkshopId =
            workshopId.trim()

        if (cleanWorkshopId.isBlank()) {

            Log.w(
                TAG,
                "Audit log workshopId empty."
            )

            return
        }

        if (
            log.workerId.isNotBlank() &&
            log.workerId != currentUid
        ) {

            Log.w(
                TAG,
                "Audit workerId mismatch."
            )

            return
        }

        val data = mapOf(

            "workerId" to currentUid,

            "workerName" to log.workerName,

            "action" to log.action,

            "projectId" to log.projectId,

            "projectName" to log.projectName,

            "timestamp" to log.timestamp,

            "workshopId" to cleanWorkshopId,

            "updatedBy" to currentUid
        )

        db.collection("audit_logs")
            .add(data)
            .addOnSuccessListener {

                Log.d(
                    TAG,
                    "Audit log synced."
                )
            }
            .addOnFailureListener { e ->

                Log.e(
                    TAG,
                    "Audit log sync failed",
                    e
                )
            }
    }

    // ============================================================
    // SEND WORKER JOIN REQUEST
    // ============================================================

    fun sendWorkerJoinRequest(
        user: TeamUser,
        syncCode: String,
        onResult: (Boolean, String) -> Unit
    ) {

        val db = getDb()

        if (db == null) {

            onResult(
                false,
                "Cloud Firestore غير متصل"
            )

            return
        }

        val currentUid =
            getCurrentUserUid()

        if (currentUid.isBlank()) {

            onResult(
                false,
                "يجب تسجيل الدخول إلى Firebase أولاً."
            )

            return
        }

        if (currentUid != user.uid) {

            onResult(
                false,
                "خطأ أمني: UID غير متطابق."
            )

            return
        }

        val cleanCode =
            syncCode
                .trim()
                .uppercase()

        if (cleanCode.isBlank()) {

            onResult(
                false,
                "رمز المزامنة فارغ."
            )

            return
        }

        db.collection("workshops")
            .whereEqualTo(
                "syncCode",
                cleanCode
            )
            .get()
            .addOnSuccessListener { snapshots ->

                val workshopDoc =
                    snapshots.documents.firstOrNull()

                if (workshopDoc != null) {

                    createJoinRequestInFirestore(
                        db,
                        user,
                        workshopDoc.id,
                        cleanCode,
                        onResult
                    )

                    return@addOnSuccessListener
                }

                db.collection("workshops")
                    .document(cleanCode)
                    .get()
                    .addOnSuccessListener { directDoc ->

                        if (directDoc.exists()) {

                            createJoinRequestInFirestore(
                                db,
                                user,
                                directDoc.id,
                                cleanCode,
                                onResult
                            )

                        } else {

                            onResult(
                                false,
                                "رمز المزامنة غير صحيح."
                            )
                        }
                    }
                    .addOnFailureListener {

                        onResult(
                            false,
                            "الورشة غير موجودة."
                        )
                    }
            }
            .addOnFailureListener { e ->

                onResult(
                    false,
                    "فشل التحقق من الرمز: ${e.localizedMessage}"
                )
            }
    }

    private fun createJoinRequestInFirestore(
        db: FirebaseFirestore,
        user: TeamUser,
        workshopId: String,
        syncCode: String,
        onResult: (Boolean, String) -> Unit
    ) {

        val uid =
            getCurrentUserUid()

        if (
            uid.isBlank() ||
            uid != user.uid
        ) {

            onResult(
                false,
                "حساب Firebase غير صالح."
            )

            return
        }

        val requestId =
            "${user.uid}_$workshopId"

        val requestRef =
            db.collection("joinRequests")
                .document(requestId)

        requestRef
            .get()
            .addOnSuccessListener { existing ->

                if (existing.exists()) {

                    when (
                        existing.getString("status")
                            ?: ""
                    ) {

                        "pending" -> {

                            onResult(
                                false,
                                "طلب الانضمام موجود بالفعل."
                            )

                            return@addOnSuccessListener
                        }

                        "approved" -> {

                            onResult(
                                true,
                                "أنت عضو في هذه الورشة بالفعل."
                            )

                            return@addOnSuccessListener
                        }
                    }
                }

                val now =
                    System.currentTimeMillis()

                val data = mapOf(

                    "requestId" to requestId,

                    "workerUid" to user.uid,

                    "workerName" to user.name,

                    "workerEmail" to user.email,

                    "workshopId" to workshopId,

                    "syncCode" to syncCode,

                    "status" to "pending",

                    "createdAt" to now,

                    "updatedAt" to now
                )

                requestRef
                    .set(data)
                    .addOnSuccessListener {

                        onResult(
                            true,
                            "تم إرسال طلب الانضمام بنجاح 🎉"
                        )
                    }
                    .addOnFailureListener { e ->

                        onResult(
                            false,
                            "فشل إرسال الطلب: ${e.localizedMessage}"
                        )
                    }
            }
            .addOnFailureListener { e ->

                onResult(
                    false,
                    "فشل الاتصال بـ Firestore: ${e.localizedMessage}"
                )
            }
    }

    // ============================================================
    // PENDING JOIN REQUESTS
    // ============================================================

    fun listenToPendingJoinRequests(
        workshopId: String,
        onRequestsUpdated: (List<Map<String, Any>>) -> Unit
    ): ListenerRegistration? {

        val db = getDb()
            ?: return null

        val cleanWorkshopId =
            workshopId.trim()

        if (cleanWorkshopId.isBlank()) {
            return null
        }

        if (!isFirebaseAuthenticated()) {
            return null
        }

        return db.collection("joinRequests")
            .whereEqualTo(
                "workshopId",
                cleanWorkshopId
            )
            .whereEqualTo(
                "status",
                "pending"
            )
            .addSnapshotListener { snapshots, error ->

                if (
                    error != null ||
                    snapshots == null
                ) {

                    if (error != null) {

                        Log.w(
                            TAG,
                            "Join request listener error: ${error.message}"
                        )
                    }

                    return@addSnapshotListener
                }

                val list =
                    snapshots.documents.mapNotNull { doc ->

                        doc.data
                            ?.toMutableMap()
                            ?.apply {
                                putIfAbsent(
                                    "requestId",
                                    doc.id
                                )
                            }
                    }

                onRequestsUpdated(list)
            }
    }

    // ============================================================
    // APPROVE JOIN REQUEST
    // ============================================================

    fun approveJoinRequest(
        requestId: String,
        workerUid: String,
        workshopId: String,
        onResult: (Boolean, String) -> Unit
    ) {

        val db = getDb()

        if (db == null) {

            onResult(
                false,
                "Cloud Firestore غير متصل."
            )

            return
        }

        val currentUid =
            getCurrentUserUid()

        if (currentUid.isBlank()) {

            onResult(
                false,
                "يجب تسجيل الدخول أولاً."
            )

            return
        }

        val cleanRequestId =
            requestId.trim()

        val cleanWorkerUid =
            workerUid.trim()

        val cleanWorkshopId =
            workshopId.trim()

        if (
            cleanRequestId.isBlank() ||
            cleanWorkerUid.isBlank() ||
            cleanWorkshopId.isBlank()
        ) {

            onResult(
                false,
                "بيانات الطلب غير مكتملة."
            )

            return
        }

        val requestRef =
            db.collection("joinRequests")
                .document(cleanRequestId)

        requestRef
            .get()
            .addOnSuccessListener { requestDoc ->

                if (!requestDoc.exists()) {

                    onResult(
                        false,
                        "طلب الانضمام غير موجود."
                    )

                    return@addOnSuccessListener
                }

                val requestWorkshopId =
                    requestDoc.getString(
                        "workshopId"
                    ) ?: ""

                val requestWorkerUid =
                    requestDoc.getString(
                        "workerUid"
                    ) ?: ""

                val status =
                    requestDoc.getString(
                        "status"
                    ) ?: ""

                if (
                    requestWorkshopId !=
                    cleanWorkshopId ||
                    requestWorkerUid !=
                    cleanWorkerUid
                ) {

                    onResult(
                        false,
                        "بيانات الطلب غير متطابقة."
                    )

                    return@addOnSuccessListener
                }

                if (status != "pending") {

                    onResult(
                        false,
                        "تمت معالجة هذا الطلب مسبقاً."
                    )

                    return@addOnSuccessListener
                }

                val now =
                    System.currentTimeMillis()

                val batch =
                    db.batch()

                batch.update(
                    requestRef,
                    mapOf(

                        "status" to "approved",

                        "approvedBy" to currentUid,

                        "updatedAt" to now
                    )
                )

                val memberRef =
                    db.collection("workshops")
                        .document(cleanWorkshopId)
                        .collection("members")
                        .document(cleanWorkerUid)

                batch.set(
                    memberRef,

                    mapOf(

                        "uid" to cleanWorkerUid,

                        "role" to "worker",

                        "active" to true,

                        "joinedAt" to now,

                        "approvedBy" to currentUid
                    ),

                    SetOptions.merge()
                )

                val userRef =
                    db.collection("users")
                        .document(cleanWorkerUid)

                batch.update(
                    userRef,

                    mapOf(

                        "workshopId" to
                                cleanWorkshopId,

                        "active" to true,

                        "updatedAt" to now,

                        "updatedBy" to currentUid
                    )
                )

                batch.commit()
                    .addOnSuccessListener {

                        onResult(
                            true,
                            "تمت الموافقة على العامل بنجاح 🎉"
                        )
                    }
                    .addOnFailureListener { e ->

                        onResult(
                            false,
                            "فشل قبول الطلب: ${e.localizedMessage}"
                        )
                    }
            }
            .addOnFailureListener { e ->

                onResult(
                    false,
                    "فشل قراءة الطلب: ${e.localizedMessage}"
                )
            }
    }

    // ============================================================
    // REJECT JOIN REQUEST
    // ============================================================

    fun rejectJoinRequest(
        requestId: String,
        onResult: (Boolean, String) -> Unit
    ) {

        val db = getDb()

        if (db == null) {

            onResult(
                false,
                "Cloud Firestore غير متصل."
            )

            return
        }

        val currentUid =
            getCurrentUserUid()

        if (currentUid.isBlank()) {

            onResult(
                false,
                "يجب تسجيل الدخول أولاً."
            )

            return
        }

        val cleanRequestId =
            requestId.trim()

        if (cleanRequestId.isBlank()) {

            onResult(
                false,
                "رقم الطلب فارغ."
            )

            return
        }

        db.collection("joinRequests")
            .document(cleanRequestId)
            .update(

                mapOf(

                    "status" to "rejected",

                    "rejectedBy" to currentUid,

                    "updatedAt" to
                            System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {

                onResult(
                    true,
                    "تم رفض طلب الانضمام."
                )
            }
            .addOnFailureListener { e ->

                onResult(
                    false,
                    "فشل رفض الطلب: ${e.localizedMessage}"
                )
            }
    }
}
