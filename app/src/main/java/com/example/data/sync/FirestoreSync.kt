package com.example.data.sync

import android.content.Context
import android.util.Log

import com.example.data.db.PlumberDatabase
import com.example.data.model.AuditLog
import com.example.data.model.Project
import com.example.data.model.ProjectItem
import com.example.data.model.TeamUser

import com.google.firebase.auth.FirebaseAuth
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

    private fun getDb(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(
                TAG,
                "FirebaseFirestore instance error: ${e.message}",
                e
            )
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
            Log.e(TAG, "Unable to get current user UID", e)
            ""
        }
    }

    fun startRealtimeListener(
        context: Context,
        userRole: String,
        workerUid: String = "",
        workshopId: String = ""
    ) {

        val db = getDb() ?: return

        val plumberDb =
            PlumberDatabase.getDatabase(context)

        val scope =
            CoroutineScope(Dispatchers.IO)

        stopRealtimeListener()

        val normalizedRole =
            userRole.trim().uppercase()

        val isAdmin =
            normalizedRole == "ADMIN"

        val isManager =
            normalizedRole == "MANAGER"

        val isManagement =
            isAdmin || isManager

        val targetUid =
            workerUid.ifBlank {
                getCurrentUserUid()
            }

        if (workshopId.isBlank()) {
            Log.w(TAG, "workshopId is blank. Skipping realtime listener setup until workshopId is bound.")
            return
        }

        Log.d(
            TAG,
            "Starting realtime sync. " +
                    "Role=$normalizedRole " +
                    "UID=$targetUid " +
                    "WorkshopId=$workshopId " +
                    "Management=$isManagement"
        )

        val projectsQuery =
            if (isManagement) {
                db.collection("projects")
                    .whereEqualTo("workshopId", workshopId)
            } else if (
                normalizedRole == "WORKER" &&
                targetUid.isNotBlank()
            ) {
                db.collection("projects")
                    .whereEqualTo("workshopId", workshopId)
                    .whereArrayContains(
                        "assignedWorkers",
                        targetUid
                    )
            } else {
                Log.w(
                    TAG,
                    "Invalid role or empty UID. Sync not started."
                )
                return
            }

        projectsListenerRegistration =
            projectsQuery.addSnapshotListener { snapshot, error ->

                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w(
                            TAG,
                            "Projects snapshot listener permission denied: ${error.message}"
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

                if (snapshot == null) {
                    return@addSnapshotListener
                }

                scope.launch {
                    snapshot.documents.forEach { doc ->
                        try {
                            val id =
                                doc.getLong("id")
                                    ?: doc.id.toLongOrNull()
                                    ?: 0L

                            if (id <= 0L) {
                                return@forEach
                            }

                            val title =
                                doc.getString("name")
                                    ?: doc.getString("title")
                                    ?: "مشروع جديد"

                            val clientName =
                                doc.getString("clientName")
                                    ?: ""

                            val location =
                                doc.getString("location")
                                    ?: doc.getString("address")
                                    ?: ""

                            val notes =
                                doc.getString("notes")
                                    ?: ""

                            val workTypeKey =
                                doc.getString("workTypeKey")
                                    ?: "CUSTOM"

                            val workTypeNameAr =
                                doc.getString("workTypeNameAr")
                                    ?: "عمل مخصص"

                            val workerName =
                                doc.getString("workerName")
                                    ?: ""

                            val managerName =
                                doc.getString("managerName")
                                    ?: ""

                            val storePhone =
                                doc.getString("storePhone")
                                    ?: ""

                            val orderStatus =
                                doc.getString("orderStatus")
                                    ?: "NEW"

                            val status =
                                doc.getString("status")
                                    ?: "NEW"

                            val createdBy =
                                doc.getString("createdBy")
                                    ?: ""

                            val laborCost =
                                doc.getDouble("laborCost")
                                    ?: 0.0

                            val paidAmount =
                                doc.getDouble("paidAmount")
                                    ?: 0.0

                            val createdAt =
                                doc.getLong("createdAt")
                                    ?: System.currentTimeMillis()

                            val updatedAt =
                                doc.getLong("updatedAt")
                                    ?: System.currentTimeMillis()

                            val assignedWorkers =
                                when (
                                    val value =
                                        doc.get("assignedWorkers")
                                ) {
                                    is List<*> ->
                                        value
                                            .filterNotNull()
                                            .joinToString(",")
                                    is String ->
                                        value
                                    else ->
                                        ""
                                }

                            val project =
                                Project(
                                    id = id,
                                    title = title,
                                    clientName = clientName,
                                    location = location,
                                    notes = notes,
                                    workTypeKey = workTypeKey,
                                    workTypeNameAr = workTypeNameAr,
                                    workerName = workerName,
                                    managerName = managerName,
                                    storePhone = storePhone,
                                    orderStatus = orderStatus,
                                    assignedWorkers = assignedWorkers,
                                    createdBy = createdBy,
                                    status = status,
                                    laborCost = laborCost,
                                    paidAmount = paidAmount,
                                    workshopId = doc.getString("workshopId") ?: workshopId,
                                    createdAt = createdAt,
                                    updatedAt = updatedAt
                                )

                            plumberDb
                                .projectDao()
                                .insertProject(project)

                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Error parsing project ${doc.id}: ${e.message}",
                                e
                            )
                        }
                    }
                }
            }

        val projectItemsQuery =
            if (isManagement) {
                db.collection("project_items")
                    .whereEqualTo("workshopId", workshopId)
            } else {
                db.collection("project_items")
                    .whereEqualTo("workshopId", workshopId)
                    .whereEqualTo(
                        "workerId",
                        targetUid
                    )
            }

        projectItemsListenerRegistration =
            projectItemsQuery.addSnapshotListener { snapshot, error ->

                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w(
                            TAG,
                            "Project items snapshot listener permission denied: ${error.message}"
                        )
                    } else {
                        Log.e(
                            TAG,
                            "Project items listener error: ${error.message}",
                            error
                        )
                    }
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    return@addSnapshotListener
                }

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

                            if (
                                id <= 0L ||
                                projectId <= 0L
                            ) {
                                return@forEach
                            }

                            val materialKey =
                                doc.getString("materialKey")
                                    ?: doc.getString("materialId")
                                    ?: "custom"

                            val materialNameAr =
                                doc.getString("materialNameAr")
                                    ?: doc.getString("materialName")
                                    ?: "مادة"

                            val materialNameFr =
                                doc.getString("materialNameFr")
                                    ?: ""

                            val category =
                                doc.getString("category")
                                    ?: "CUSTOM"

                            val size =
                                doc.getString("size")
                                    ?: ""

                            val quantity =
                                doc.getDouble("quantity")
                                    ?: 1.0

                            val unit =
                                doc.getString("unit")
                                    ?: "قطعة"

                            val unitPrice =
                                doc.getDouble("unitPrice")
                                    ?: 0.0

                            val standardPipeLengthMeters =
                                doc.getDouble(
                                    "standardPipeLengthMeters"
                                ) ?: 4.0

                            val isPurchased =
                                doc.getBoolean("isPurchased")
                                    ?: false

                            val notes =
                                doc.getString("notes")
                                    ?: ""

                            val iconType =
                                doc.getString("iconType")
                                    ?: "elbow"

                            val imageUri =
                                doc.getString("imageUri")

                            val createdAt =
                                doc.getLong("createdAt")
                                    ?: System.currentTimeMillis()

                            val item =
                                ProjectItem(
                                    id = id,
                                    projectId = projectId,
                                    materialKey = materialKey,
                                    materialNameAr = materialNameAr,
                                    materialNameFr = materialNameFr,
                                    category = category,
                                    size = size,
                                    quantity = quantity,
                                    unit = unit,
                                    unitPrice = unitPrice,
                                    standardPipeLengthMeters =
                                        standardPipeLengthMeters,
                                    isPurchased = isPurchased,
                                    notes = notes,
                                    iconType = iconType,
                                    imageUri = imageUri,
                                    workshopId = doc.getString("workshopId") ?: workshopId,
                                    createdAt = createdAt
                                )

                            plumberDb
                                .projectItemDao()
                                .insertItem(item)

                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Error parsing project item ${doc.id}: ${e.message}",
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

        Log.d(
            TAG,
            "Realtime Firestore listeners stopped."
        )
    }

    fun syncProjectToFirestore(
        project: Project,
        workshopId: String = ""
    ) {
        val db = getDb() ?: return

        try {
            val assignedWorkersArray =
                project.assignedWorkers
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

            val currentWorkshop =
                if (project.workshopId.isNotBlank()) project.workshopId else workshopId

            val projectMap =
                mapOf(
                    "id" to project.id,
                    "name" to project.title,
                    "title" to project.title,
                    "clientName" to project.clientName,
                    "address" to project.location,
                    "location" to project.location,
                    "notes" to project.notes,
                    "workTypeKey" to project.workTypeKey,
                    "workTypeNameAr" to project.workTypeNameAr,
                    "workerName" to project.workerName,
                    "managerName" to project.managerName,
                    "storePhone" to project.storePhone,
                    "orderStatus" to project.orderStatus,
                    "assignedWorkers" to assignedWorkersArray,
                    "createdBy" to project.createdBy,
                    "status" to project.status,
                    "laborCost" to project.laborCost,
                    "paidAmount" to project.paidAmount,
                    "workshopId" to currentWorkshop,
                    "createdAt" to project.createdAt,
                    "updatedAt" to project.updatedAt
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
                    if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w(
                            TAG,
                            "Sync project ${project.id} skipped (Permission Denied): ${e.message}"
                        )
                    } else {
                        Log.e(
                            TAG,
                            "Error syncing project ${project.id}: ${e.message}",
                            e
                        )
                    }
                }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error syncProjectToFirestore: ${e.message}",
                e
            )
        }
    }

    fun syncProjectItemToFirestore(
        item: ProjectItem,
        workerId: String = "",
        workerName: String = "",
        workshopId: String = ""
    ) {
        val db = getDb() ?: return

        try {
            val currentUid =
                workerId.ifBlank {
                    getCurrentUserUid()
                }

            if (currentUid.isBlank()) {
                Log.w(
                    TAG,
                    "Cannot sync project item: user UID is empty / unauthenticated."
                )
                return
            }

            val currentWorkshop =
                if (item.workshopId.isNotBlank()) item.workshopId else workshopId

            val itemMap =
                mapOf(
                    "id" to item.id,
                    "projectId" to item.projectId,
                    "materialId" to item.materialKey,
                    "materialKey" to item.materialKey,
                    "materialName" to item.materialNameAr,
                    "materialNameAr" to item.materialNameAr,
                    "materialNameFr" to item.materialNameFr,
                    "category" to item.category,
                    "size" to item.size,
                    "unit" to item.unit,
                    "quantity" to item.quantity,
                    "unitPrice" to item.unitPrice,
                    "standardPipeLengthMeters" to item.standardPipeLengthMeters,
                    "isPurchased" to item.isPurchased,
                    "notes" to item.notes,
                    "iconType" to item.iconType,
                    "imageUri" to item.imageUri,
                    "workerId" to currentUid,
                    "workerName" to workerName,
                    "workshopId" to currentWorkshop,
                    "createdAt" to item.createdAt,
                    "updatedAt" to System.currentTimeMillis()
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
                    if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w(
                            TAG,
                            "Sync project item $docId skipped (Permission Denied): ${e.message}"
                        )
                    } else {
                        Log.e(
                            TAG,
                            "Error syncing project item $docId: ${e.message}",
                            e
                        )
                    }
                }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error syncProjectItemToFirestore: ${e.message}",
                e
            )
        }
    }

    fun deleteProjectFromFirestore(
        projectId: Long
    ) {
        val db = getDb() ?: return

        try {
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
                    Log.e(
                        TAG,
                        "Error deleting project $projectId: ${e.message}",
                        e
                    )
                }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error deleteProjectFromFirestore: ${e.message}",
                e
            )
        }
    }

    fun deleteProjectItemFromFirestore(
        itemId: Long
    ) {
        val db = getDb() ?: return

        try {
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
                    Log.e(
                        TAG,
                        "Error deleting project item $itemId: ${e.message}",
                        e
                    )
                }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error deleteProjectItemFromFirestore: ${e.message}",
                e
            )
        }
    }

    fun syncUserToFirestore(
        user: TeamUser
    ) {
        val db = getDb() ?: return

        try {
            if (user.uid.isBlank()) {
                Log.w(
                    TAG,
                    "Cannot sync user: user UID is empty."
                )
                return
            }

            val userMap =
                mapOf(
                    "uid" to user.uid,
                    "name" to user.name,
                    "phone" to user.phone,
                    "email" to user.email,
                    "role" to user.role,
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
                        "User synced: ${user.uid}"
                    )
                }
                .addOnFailureListener { e ->
                    if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w(
                            TAG,
                            "Sync user ${user.uid} skipped (Permission Denied): ${e.message}"
                        )
                    } else {
                        Log.e(
                            TAG,
                            "Error syncing user: ${e.message}",
                            e
                        )
                    }
                }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error syncUserToFirestore: ${e.message}",
                e
            )
        }
    }

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
        db.collection("users")
            .whereEqualTo("phone", cleanQuery)
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots != null && !snapshots.isEmpty) {
                    val doc = snapshots.documents.first()
                    val user = TeamUser(
                        uid = doc.getString("uid") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        phone = doc.getString("phone") ?: cleanQuery,
                        email = doc.getString("email") ?: "",
                        password = doc.getString("password") ?: "",
                        role = doc.getString("role") ?: "WORKER",
                        active = doc.getBoolean("active") ?: true,
                        workshopId = doc.getString("workshopId") ?: "",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                    onResult(user)
                } else {
                    db.collection("users")
                        .whereEqualTo("email", cleanQuery)
                        .get()
                        .addOnSuccessListener { emailSnapshots ->
                            if (emailSnapshots != null && !emailSnapshots.isEmpty) {
                                val doc = emailSnapshots.documents.first()
                                val user = TeamUser(
                                    uid = doc.getString("uid") ?: doc.id,
                                    name = doc.getString("name") ?: "",
                                    phone = doc.getString("phone") ?: "",
                                    email = doc.getString("email") ?: cleanQuery,
                                    password = doc.getString("password") ?: "",
                                    role = doc.getString("role") ?: "WORKER",
                                    active = doc.getBoolean("active") ?: true,
                                    workshopId = doc.getString("workshopId") ?: "",
                                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                )
                                onResult(user)
                            } else {
                                onResult(null)
                            }
                        }
                        .addOnFailureListener {
                            onResult(null)
                        }
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun syncStoreSettingsToFirestore(
        storeName: String,
        storePhone: String,
        storeWhatsapp: String,
        managerName: String
    ) {
        val db = getDb() ?: return
        try {
            val settingsMap = mapOf(
                "storeName" to storeName,
                "storePhone" to storePhone,
                "storeWhatsapp" to storeWhatsapp,
                "managerName" to managerName,
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection("store_settings")
                .document("config")
                .set(settingsMap, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Store settings successfully synced to Firestore.")
                }
                .addOnFailureListener { e ->
                    if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w(TAG, "Store settings sync skipped (Permission Denied): ${e.message}")
                    } else {
                        Log.e(TAG, "Error syncing store settings to Firestore: ${e.message}", e)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncStoreSettingsToFirestore: ${e.message}", e)
        }
    }

    fun syncAuditLogToFirestore(
        log: AuditLog
    ) {
        val db = getDb() ?: return

        try {
            val currentUid = getCurrentUserUid()

            if (
                currentUid.isNotBlank() &&
                log.workerId.isNotBlank() &&
                log.workerId != currentUid
            ) {
                Log.w(
                    TAG,
                    "Audit log workerId does not match current user."
                )
                return
            }

            val logMap =
                mapOf(
                    "workerId" to (
                        log.workerId.ifBlank {
                            currentUid
                        }
                    ),
                    "workerName" to log.workerName,
                    "action" to log.action,
                    "projectId" to log.projectId,
                    "projectName" to log.projectName,
                    "timestamp" to log.timestamp
                )

            db.collection("audit_logs")
                .add(logMap)
                .addOnSuccessListener {
                    Log.d(
                        TAG,
                        "Audit log synced."
                    )
                }
                .addOnFailureListener { e ->
                    if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w(
                            TAG,
                            "Sync audit log skipped (Permission Denied): ${e.message}"
                        )
                    } else {
                        Log.e(
                            TAG,
                            "Error syncing audit log: ${e.message}",
                            e
                        )
                    }
                }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error syncAuditLogToFirestore: ${e.message}",
                e
            )
        }
    }

    /**
     * التحقق من الـ Sync Code وإضافة العامل بورشة العمل في Firestore
     * workshops/{workshopId}/members/{workerUid} -> role: "worker"
     * users/{workerUid} -> workshopId
     */
    fun joinWorkerToWorkshop(
        workerUid: String,
        syncCode: String,
        onResult: (Boolean, String, String) -> Unit
    ) {
        val cleanCode = syncCode.trim().uppercase()
        if (cleanCode.isBlank()) {
            onResult(false, "رمز المزامنة فارغ", "")
            return
        }

        val data = hashMapOf("syncCode" to cleanCode)

        try {
            com.google.firebase.functions.FirebaseFunctions.getInstance()
                .getHttpsCallable("joinWorkshopBySyncCode")
                .call(data)
                .addOnSuccessListener { result ->
                    @Suppress("UNCHECKED_CAST")
                    val resData = result.data as? Map<String, Any>
                    val success = resData?.get("success") as? Boolean ?: true
                    val targetWorkshopId = resData?.get("workshopId") as? String ?: ""
                    val msg = resData?.get("message") as? String ?: "تم الانضمام للورشة بنجاح 🎉"
                    if (success && targetWorkshopId.isNotBlank()) {
                        onResult(true, msg, targetWorkshopId)
                    } else {
                        onResult(false, msg.ifBlank { "رمز المزامنة غير صحيح، الورشة غير موجودة" }, "")
                    }
                }
                .addOnFailureListener { e ->
                    val errMsg = e.message ?: ""
                    if (errMsg.contains("INVALID_SYNC_CODE") || errMsg.contains("NOT_FOUND", ignoreCase = true)) {
                        onResult(false, "رمز المزامنة غير صحيح، الورشة غير موجودة", "")
                    } else {
                        onResult(false, "فشل الانضمام للورشة: ${e.localizedMessage}", "")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking Cloud Function joinWorkshopBySyncCode: ${e.message}")
            onResult(false, "فشل الاتصال بالخدمة السحابية: ${e.localizedMessage}", "")
        }
    }
}
