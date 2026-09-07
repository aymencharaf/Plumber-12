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
            Log.e(
                TAG,
                "FirebaseFirestore instance error: ${e.message}",
                e
            )
            null
        }
    }

    /**
     * الحصول على UID للمستخدم الحالي.
     *
     * Firebase Authentication هو المصدر الأساسي للهوية.
     */
    fun getCurrentUserUid(): String {
        return try {
            FirebaseAuth.getInstance()
                .currentUser
                ?.uid
                ?: ""
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Unable to get current user UID",
                e
            )
            ""
        }
    }

    /**
     * التحقق من وجود جلسة Firebase.
     */
    fun isFirebaseAuthenticated(): Boolean {
        return try {
            FirebaseAuth.getInstance()
                .currentUser != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * تسجيل خروج Firebase.
     *
     * تستخدم من ViewModel عند الحاجة.
     */
    fun signOutFirebase() {
        try {
            FirebaseAuth.getInstance().signOut()
            stopRealtimeListener()

            Log.d(
                TAG,
                "Firebase user signed out."
            )
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error signing out Firebase user",
                e
            )
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

    /**
     * تحويل:
     *
     * "uid1,uid2"
     *
     * إلى:
     *
     * ["uid1", "uid2"]
     */
    private fun parseAssignedWorkers(
        assignedWorkers: String
    ): List<String> {
        return assignedWorkers
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    /**
     * قراءة assignedWorkers من Firestore.
     *
     * يدعم البيانات القديمة والجديدة.
     */
    private fun readAssignedWorkers(
        doc: DocumentSnapshot
    ): String {

        return when (
            val value = doc.get("assignedWorkers")
        ) {

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

        val plumberDb =
            PlumberDatabase.getDatabase(context)

        val scope =
            CoroutineScope(Dispatchers.IO)

        stopRealtimeListener()

        val normalizedRole =
            normalizeRole(userRole)

        val isAdmin =
            normalizedRole == "ADMIN"

        val isManager =
            normalizedRole == "MANAGER"

        val isManagement =
            isAdmin || isManager

        val targetUid =
            workerUid
                .ifBlank {
                    getCurrentUserUid()
                }
                .trim()

        val cleanWorkshopId =
            workshopId.trim()

        if (!isFirebaseAuthenticated()) {

            Log.w(
                TAG,
                "Firebase user is not authenticated. Realtime sync not started."
            )

            return
        }

        if (targetUid.isBlank()) {

            Log.w(
                TAG,
                "Target UID is empty. Realtime sync not started."
            )

            return
        }

        if (cleanWorkshopId.isBlank()) {

            Log.w(
                TAG,
                "workshopId is blank. Realtime listener setup skipped."
            )

            return
        }

        Log.d(
            TAG,
            "Starting realtime sync. " +
                    "Role=$normalizedRole " +
                    "UID=$targetUid " +
                    "WorkshopId=$cleanWorkshopId " +
                    "Management=$isManagement"
        )

        // ========================================================
        // PROJECTS QUERY
        // ========================================================

        val projectsQuery =
            if (isManagement) {

                db.collection("projects")
                    .whereEqualTo(
                        "workshopId",
                        cleanWorkshopId
                    )

            } else if (
                normalizedRole == "WORKER"
            ) {

                /*
                 * العامل يرى فقط المشاريع التي تحتوي
                 * على Firebase UID الخاص به.
                 */
                db.collection("projects")
                    .whereEqualTo(
                        "workshopId",
                        cleanWorkshopId
                    )
                    .whereArrayContains(
                        "assignedWorkers",
                        targetUid
                    )

            } else {

                Log.w(
                    TAG,
                    "Invalid role '$normalizedRole'."
                )

                return
            }

        projectsListenerRegistration =
            projectsQuery.addSnapshotListener { snapshot, error ->

                if (error != null) {

                    if (
                        error.code ==
                        FirebaseFirestoreException.Code.PERMISSION_DENIED
                    ) {

                        Log.w(
                            TAG,
                            "Projects listener permission denied: ${error.message}"
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
                                readAssignedWorkers(doc)

                            val projectWorkshopId =
                                doc.getString("workshopId")
                                    ?: cleanWorkshopId

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
                                    workshopId = projectWorkshopId,
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

        // ========================================================
        // PROJECT ITEMS QUERY
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

                    if (
                        error.code ==
                        FirebaseFirestoreException.Code.PERMISSION_DENIED
                    ) {

                        Log.w(
                            TAG,
                            "Project items listener permission denied: ${error.message}"
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

                            val workshopValue =
                                doc.getString("workshopId")
                                    ?: cleanWorkshopId

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
                                    workshopId = workshopValue,
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

    // ============================================================
    // PROJECT SYNC
    // ============================================================

    fun syncProjectToFirestore(
        project: Project,
        workshopId: String = ""
    ) {

        val db = getDb() ?: return

        val currentUid =
            getCurrentUserUid()

        if (currentUid.isBlank()) {

            Log.w(
                TAG,
                "Cannot sync project: no authenticated Firebase user."
            )

            return
        }

        try {

            val currentWorkshop =
                resolveWorkshopId(
                    project.workshopId,
                    workshopId
                )

            if (currentWorkshop.isBlank()) {

                Log.w(
                    TAG,
                    "Cannot sync project ${project.id}: workshopId is blank."
                )

                return
            }

            /*
             * يجب أن تحتوي assignedWorkers على UIDs.
             */
            val assignedWorkersArray =
                parseAssignedWorkers(
                    project.assignedWorkers
                )

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

                    if (
                        e is FirebaseFirestoreException &&
                        e.code ==
                        FirebaseFirestoreException.Code.PERMISSION_DENIED
                    ) {

                        Log.w(
                            TAG,
                            "Sync project ${project.id} denied: ${e.message}"
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

        val currentUid =
            getCurrentUserUid()

        if (currentUid.isBlank()) {

            Log.w(
                TAG,
                "Cannot sync project item: unauthenticated."
            )

            return
        }

        try {

            val currentWorkshop =
                if (item.workshopId.isNotBlank()) {
                    item.workshopId.trim()
                } else {
                    workshopId.trim()
                }

            if (currentWorkshop.isBlank()) {

                Log.w(
                    TAG,
                    "Cannot sync project item ${item.id}: workshopId is blank."
                )

                return
            }

            /*
             * عند إنشاء عنصر جديد بواسطة العامل:
             *
             * workerId = Firebase UID للعامل.
             *
             * عند قيام المدير بتعديل عنصر موجود،
             * يجب أن يرسل ViewModel workerId الأصلي للعنصر.
             */
            val ownerWorkerId =
                workerId
                    .trim()
                    .ifBlank {
                        currentUid
                    }

            val itemMap =
                mutableMapOf<String, Any?>(
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

                    /*
                     * السعر موجود.
                     *
                     * Rules تمنع العامل من تعديله
                     * بعد إنشاء العنصر.
                     */
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

            val cleanItemMap =
                itemMap.filterValues {
                    it != null
                }

            val docId =
                if (item.id > 0L) {
                    item.id.toString()
                } else {
                    "${item.projectId}_${item.materialKey}_${item.size}"
                }

            db.collection("project_items")
                .document(docId)
                .set(
                    cleanItemMap,
                    SetOptions.merge()
                )
                .addOnSuccessListener {

                    Log.d(
                        TAG,
                        "Project item synced: $docId"
                    )
                }
                .addOnFailureListener { e ->

                    if (
                        e is FirebaseFirestoreException &&
                        e.code ==
                        FirebaseFirestoreException.Code.PERMISSION_DENIED
                    ) {

                        Log.w(
                            TAG,
                            "Sync project item $docId denied: ${e.message}"
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

                if (
                    e is FirebaseFirestoreException &&
                    e.code ==
                    FirebaseFirestoreException.Code.PERMISSION_DENIED
                ) {

                    Log.w(
                        TAG,
                        "Delete project denied: ${e.message}"
                    )

                } else {

                    Log.e(
                        TAG,
                        "Error deleting project $projectId",
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
                "Cannot delete project item: unauthenticated."
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

                if (
                    e is FirebaseFirestoreException &&
                    e.code ==
                    FirebaseFirestoreException.Code.PERMISSION_DENIED
                ) {

                    Log.w(
                        TAG,
                        "Delete project item denied: ${e.message}"
                    )

                } else {

                    Log.e(
                        TAG,
                        "Error deleting project item $itemId",
                        e
                    )
                }
            }
    }

    // ============================================================
    // USER SYNC - CURRENT USER
    // ============================================================

    /**
     * مزامنة المستخدم الحالي فقط.
     *
     * لا يمكن لهذه الدالة كتابة users/{UID} لمستخدم آخر.
     *
     * password لا يتم تخزينه.
     */
    fun syncUserToFirestore(
        user: TeamUser
    ) {

        val db = getDb() ?: return

        val currentUid =
            getCurrentUserUid()

        if (user.uid.isBlank()) {

            Log.w(
                TAG,
                "Cannot sync user: UID is empty."
            )

            return
        }

        if (
            currentUid.isBlank() ||
            currentUid != user.uid
        ) {

            Log.w(
                TAG,
                "Security check failed. " +
                        "Authenticated UID=$currentUid " +
                        "Target UID=${user.uid}"
            )

            return
        }

        val normalizedRole =
            normalizeRole(user.role)

        val userMap =
            mapOf(
                "uid" to user.uid,
                "name" to user.name,
                "phone" to user.phone,
                "email" to user.email,
                "role" to normalizedRole,
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
                    "Current user synced: ${user.uid}"
                )
            }
            .addOnFailureListener { e ->

                Log.e(
                    TAG,
                    "Error syncing current user ${user.uid}: ${e.message}",
                    e
                )
            }
    }

    // ============================================================
    // USER PROFILE SYNC - MANAGEMENT
    // ============================================================

    /**
     * إنشاء/تحديث ملف مستخدم بواسطة المدير/الإدارة.
     *
     * مهم جداً:
     *
     * هذه الدالة لا تستخدم Firebase Authentication لإنشاء الحساب.
     *
     * إنشاء Firebase Auth يتم في ViewModel باستخدام
     * FirebaseApp ثانوي عند إنشاء العامل بواسطة المدير.
     *
     * هذه الدالة تقوم فقط بإنشاء:
     *
     * users/{workerUid}
     *
     * وقواعد Firestore هي التي تمنع العامل العادي
     * من استدعائها فعلياً.
     *
     * password غير موجود إطلاقاً.
     */
    fun syncUserProfileAsManagement(
        user: TeamUser
    ) {

        val db = getDb() ?: return

        val currentUid =
            getCurrentUserUid()

        if (currentUid.isBlank()) {

            Log.w(
                TAG,
                "Cannot create user profile: manager is not authenticated."
            )

            return
        }

        if (user.uid.isBlank()) {

            Log.w(
                TAG,
                "Cannot create user profile: target UID is empty."
            )

            return
        }

        /*
         * لا يسمح باستخدام هذه الدالة لإنشاء ADMIN
         * أو MANAGER من واجهة إنشاء العامل.
         */
        val normalizedRole =
            normalizeRole(user.role)

        if (
            normalizedRole != "WORKER"
        ) {

            Log.w(
                TAG,
                "Management profile creation rejected for role=$normalizedRole"
            )

            return
        }

        val userMap =
            mapOf(
                "uid" to user.uid,
                "name" to user.name,
                "phone" to user.phone,
                "email" to user.email,
                "role" to "WORKER",
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
                    "Worker profile created by management: ${user.uid}"
                )
            }
            .addOnFailureListener { e ->

                if (
                    e is FirebaseFirestoreException &&
                    e.code ==
                    FirebaseFirestoreException.Code.PERMISSION_DENIED
                ) {

                    Log.w(
                        TAG,
                        "Management cannot create worker profile: ${e.message}"
                    )

                } else {

                    Log.e(
                        TAG,
                        "Error creating worker profile: ${e.message}",
                        e
                    )
                }
            }
    }

    // ============================================================
    // FETCH CURRENT USER BY UID
    // ============================================================

    /**
     * الطريقة المفضلة بعد تسجيل الدخول.
     *
     * بدلاً من البحث بواسطة email/phone،
     * نقرأ:
     *
     * users/{FirebaseAuth.currentUser.uid}
     *
     * وهذا يتوافق مع Firestore Rules الآمنة.
     */
    fun fetchCurrentUserFromFirestore(
        onResult: (TeamUser?) -> Unit
    ) {

        val db = getDb()

        if (db == null) {

            onResult(null)
            return
        }

        val currentUid =
            getCurrentUserUid()

        if (currentUid.isBlank()) {

            onResult(null)
            return
        }

        db.collection("users")
            .document(currentUid)
            .get()
            .addOnSuccessListener { doc ->

                if (!doc.exists()) {

                    Log.w(
                        TAG,
                        "No Firestore user profile for UID=$currentUid"
                    )

                    onResult(null)
                    return@addOnSuccessListener
                }

                val storedUid =
                    doc.getString("uid")
                        ?: doc.id

                if (storedUid != currentUid) {

                    Log.w(
                        TAG,
                        "Firestore UID mismatch. " +
                                "Auth=$currentUid Firestore=$storedUid"
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
                    "Error fetching current user from Firestore",
                    e
                )

                onResult(null)
            }
    }

    // ============================================================
    // FETCH USER BY PHONE OR EMAIL
    // ============================================================

    /**
     * البحث القديم بواسطة الهاتف أو البريد.
     *
     * هذه الدالة مفيدة للإدارة/البحث الداخلي.
     *
     * تسجيل الدخول نفسه يجب أن يستخدم
     * fetchCurrentUserFromFirestore().
     */
    fun fetchUserFromFirestoreByPhoneOrEmail(
        query: String,
        onResult: (TeamUser?) -> Unit
    ) {

        val db = getDb()

        if (db == null) {

            onResult(null)
            return
        }

        val cleanQuery =
            query.trim()

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

                if (
                    snapshots.isNotEmpty()
                ) {

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
            .addOnFailureListener { e ->

                Log.e(
                    TAG,
                    "Error searching user by phone: ${e.message}",
                    e
                )

                searchUserByEmail(
                    db,
                    cleanQuery,
                    onResult
                )
            }
    }

    private fun searchUserByEmail(
        db: FirebaseFirestore,
        cleanQuery: String,
        onResult: (TeamUser?) -> Unit
    ) {

        db.collection("users")
            .whereEqualTo(
                "email",
                cleanQuery
            )
            .get()
            .addOnSuccessListener { snapshots ->

                if (
                    snapshots.isNotEmpty()
                ) {

                    onResult(
                        createTeamUserFromDocument(
                            snapshots.documents.first(),
                            cleanQuery
                        )
                    )

                } else {

                    onResult(null)
                }
            }
            .addOnFailureListener { e ->

                Log.e(
                    TAG,
                    "Error searching user by email: ${e.message}",
                    e
                )

                onResult(null)
            }
    }

    // ============================================================
    // CREATE TEAM USER
    // ============================================================

    private fun createTeamUserFromDocument(
        doc: DocumentSnapshot,
        fallbackQuery: String
    ): TeamUser {

        val role =
            normalizeRole(
                doc.getString("role") ?: "WORKER"
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

            /*
             * مهم جداً:
             *
             * كلمة المرور لا تأتي من Firestore.
             */
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

        val currentUid =
            getCurrentUserUid()

        if (currentUid.isBlank()) {

            Log.w(
                TAG,
                "Cannot sync store settings: unauthenticated."
            )

            return
        }

        val cleanWorkshopId =
            workshopId.trim()

        if (cleanWorkshopId.isBlank()) {

            Log.w(
                TAG,
                "Cannot sync store settings: workshopId is blank."
            )

            return
        }

        val settingsMap =
            mapOf(
                "storeName" to storeName,
                "storePhone" to storePhone,
                "storeWhatsapp" to storeWhatsapp,
                "managerName" to managerName,
                "workshopId" to cleanWorkshopId,
                "updatedAt" to System.currentTimeMillis(),
                "updatedBy" to currentUid
            )

        db.collection("store_settings")
            .document(cleanWorkshopId)
            .set(
                settingsMap,
                SetOptions.merge()
            )
            .addOnSuccessListener {

                Log.d(
                    TAG,
                    "Store settings synced: $cleanWorkshopId"
                )
            }
            .addOnFailureListener { e ->

                Log.e(
                    TAG,
                    "Error syncing store settings: ${e.message}",
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

        if (currentUid.isBlank()) {

            Log.w(
                TAG,
                "Cannot sync audit log: unauthenticated."
            )

            return
        }

        /*
         * workerId يجب أن يكون المستخدم الحالي
         * إذا تم تمريره.
         */
        if (
            log.workerId.isNotBlank() &&
            log.workerId != currentUid
        ) {

            Log.w(
                TAG,
                "Audit log workerId mismatch."
            )

            return
        }

        val cleanWorkshopId =
            workshopId.trim()

        if (cleanWorkshopId.isBlank()) {

            Log.w(
                TAG,
                "Audit log workshopId is blank."
            )

            return
        }

        val logMap =
            mapOf(
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
            .add(logMap)
            .addOnSuccessListener {

                Log.d(
                    TAG,
                    "Audit log synced."
                )
            }
            .addOnFailureListener { e ->

                Log.e(
                    TAG,
                    "Error syncing audit log: ${e.message}",
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

        if (user.uid != currentUid) {

            onResult(
                false,
                "خطأ أمني: حساب المستخدم لا يطابق حساب Firebase."
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

                } else {

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
                                    "رمز المزامنة غير صحيح، الورشة غير موجودة."
                                )
                            }
                        }
                        .addOnFailureListener {

                            onResult(
                                false,
                                "رمز المزامنة غير صحيح، الورشة غير موجودة."
                            )
                        }
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
        targetWorkshopId: String,
        cleanCode: String,
        onResult: (Boolean, String) -> Unit
    ) {

        val currentUid =
            getCurrentUserUid()

        if (
            currentUid.isBlank() ||
            currentUid != user.uid
        ) {

            onResult(
                false,
                "حساب Firebase غير صالح."
            )

            return
        }

        val requestId =
            "${user.uid}_$targetWorkshopId"

        val requestRef =
            db.collection("joinRequests")
                .document(requestId)

        requestRef
            .get()
            .addOnSuccessListener { doc ->

                if (doc.exists()) {

                    when (
                        doc.getString("status")
                            ?: ""
                    ) {

                        "pending" -> {

                            onResult(
                                false,
                                "تم تقديم طلب الانضمام سابقاً، بانتظار موافقة مدير الورشة ⏳"
                            )

                            return@addOnSuccessListener
                        }

                        "approved" -> {

                            onResult(
                                true,
                                "أنت عضو في هذه الورشة بالفعل 🎉"
                            )

                            return@addOnSuccessListener
                        }
                    }
                }

                val now =
                    System.currentTimeMillis()

                val requestData =
                    hashMapOf(
                        "requestId" to requestId,
                        "workerUid" to user.uid,
                        "workerName" to user.name,
                        "workerEmail" to user.email.ifBlank {
                            user.phone
                        },
                        "workshopId" to targetWorkshopId,
                        "syncCode" to cleanCode,
                        "status" to "pending",
                        "createdAt" to now,
                        "updatedAt" to now
                    )

                requestRef
                    .set(requestData)
                    .addOnSuccessListener {

                        onResult(
                            true,
                            "تم إرسال طلب الانضمام بنجاح 🎉 بانتظار موافقة مدير الورشة."
                        )
                    }
                    .addOnFailureListener { e ->

                        onResult(
                            false,
                            "فشل إرسال طلب الانضمام: ${e.localizedMessage}"
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

        val db =
            getDb()
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
                            "Join requests listener error: ${error.message}"
                        )
                    }

                    return@addSnapshotListener
                }

                val list =
                    snapshots.documents.mapNotNull { doc ->

                        doc.data
                            ?.toMutableMap()
                            ?.apply {

                                if (!containsKey("requestId")) {

                                    this["requestId"] =
                                        doc.id
                                }
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
                "Cloud Firestore غير متصل"
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
                "بيانات طلب الانضمام غير مكتملة."
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
                    requestDoc.getString("workshopId")
                        ?: ""

                val requestWorkerUid =
                    requestDoc.getString("workerUid")
                        ?: ""

                val requestStatus =
                    requestDoc.getString("status")
                        ?: ""

                if (
                    requestWorkshopId != cleanWorkshopId ||
                    requestWorkerUid != cleanWorkerUid
                ) {

                    onResult(
                        false,
                        "بيانات طلب الانضمام غير متطابقة."
                    )

                    return@addOnSuccessListener
                }

                if (
                    requestStatus != "pending"
                ) {

                    onResult(
                        false,
                        "طلب الانضمام تمت معالجته مسبقاً."
                    )

                    return@addOnSuccessListener
                }

                val now =
                    System.currentTimeMillis()

                val batch =
                    db.batch()

                // ------------------------------------------------
                // JOIN REQUEST
                // ------------------------------------------------

                batch.update(
                    requestRef,
                    mapOf(
                        "status" to "approved",
                        "updatedAt" to now,
                        "approvedBy" to currentUid
                    )
                )

                // ------------------------------------------------
                // WORKSHOP MEMBER
                // ------------------------------------------------

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

                // ------------------------------------------------
                // USER PROFILE
                // ------------------------------------------------

                val userRef =
                    db.collection("users")
                        .document(cleanWorkerUid)

                batch.update(
                    userRef,
                    mapOf(
                        "workshopId" to cleanWorkshopId,
                        "active" to true,
                        "updatedAt" to now
                    )
                )

                batch.commit()
                    .addOnSuccessListener {

                        onResult(
                            true,
                            "تمت الموافقة على طلب العامل بنجاح 🎉"
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
                    "فشل قراءة طلب الانضمام: ${e.localizedMessage}"
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
                "Cloud Firestore غير متصل"
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
                "رقم طلب الانضمام فارغ."
            )

            return
        }

        db.collection("joinRequests")
            .document(cleanRequestId)
            .update(
                mapOf(
                    "status" to "rejected",
                    "updatedAt" to System.currentTimeMillis(),
                    "rejectedBy" to currentUid
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
