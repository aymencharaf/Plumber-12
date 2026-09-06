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

    /**
     * الحصول على UID للمستخدم الحالي في Firebase Authentication.
     *
     * هذا هو المصدر الأساسي لهوية المستخدم.
     * لا نعتمد على UID مخزن محلياً إذا كان Firebase غير مسجل الدخول.
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
     * التحقق من وجود جلسة Firebase صالحة.
     */
    private fun isFirebaseAuthenticated(): Boolean {
        return try {
            FirebaseAuth.getInstance()
                .currentUser != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * الحصول على workshopId من المشروع إن وجد،
     * وإلا استخدام القيمة المرسلة للدالة.
     */
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
     * تحويل assignedWorkers إلى List<String>.
     *
     * يدعم:
     * - String قديم: "uid1,uid2"
     * - List<String> حديثة
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
            }.trim()

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
        // PROJECTS
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
                 * العامل يرى المشاريع التي تحتوي على UID الخاص به
                 * داخل assignedWorkers.
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
                    "Invalid role '$normalizedRole'. Sync not started."
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

                            /*
                             * assignedWorkers يجب أن تكون UIDs.
                             *
                             * ندعم أيضاً البيانات القديمة التي قد تكون String.
                             */
                            val assignedWorkers =
                                when (
                                    val value =
                                        doc.get("assignedWorkers")
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
        // PROJECT ITEMS
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

                            /*
                             * مهم:
                             * workerId هو صاحب/مالك العنصر في سياق الورشة.
                             * لا نستبدله بالمدير الذي قام بالتعديل.
                             */
                            val workerId =
                                doc.getString("workerId")
                                    ?: ""

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

                            /*
                             * workerId لا يوجد في ProjectItem المحلي
                             * حسب النموذج الحالي، لذلك نستخدمه فقط
                             * في فلترة Firestore ولا نضيفه إلى Room.
                             */
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
             * assignedWorkers يجب أن تحتوي على Firebase UIDs.
             *
             * إذا كانت البيانات القديمة تحتوي على أسماء،
             * لن نقوم بتحويل الاسم إلى UID بشكل تخميني.
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
                "Cannot sync project item: user is unauthenticated."
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
             * إذا تم تمرير workerId صراحة نستخدمه.
             * وإلا نستخدم المستخدم الحالي.
             *
             * الهدف هو عدم تحويل workerId إلى UID المدير
             * عندما يقوم المدير بتعديل مادة العامل.
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
                     * unitPrice يبقى موجوداً في البيانات،
                     * لكن Firestore Rules يجب أن تمنع العامل
                     * من تغييره.
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

                    /*
                     * المستخدم الذي قام فعلياً بالحفظ.
                     */
                    "updatedBy" to currentUid
                )

            /*
             * لا نكتب null fields.
             */
            val cleanItemMap =
                itemMap.filterValues { it != null }

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
                "Cannot delete project: user is unauthenticated."
            )

            return
        }

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

                    if (
                        e is FirebaseFirestoreException &&
                        e.code ==
                        FirebaseFirestoreException.Code.PERMISSION_DENIED
                    ) {

                        Log.w(
                            TAG,
                            "Delete project $projectId denied: ${e.message}"
                        )

                    } else {

                        Log.e(
                            TAG,
                            "Error deleting project $projectId: ${e.message}",
                            e
                        )
                    }
                }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error deleteProjectFromFirestore: ${e.message}",
                e
            )
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
                "Cannot delete project item: user is unauthenticated."
            )

            return
        }

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

                    if (
                        e is FirebaseFirestoreException &&
                        e.code ==
                        FirebaseFirestoreException.Code.PERMISSION_DENIED
                    ) {

                        Log.w(
                            TAG,
                            "Delete project item $itemId denied: ${e.message}"
                        )

                    } else {

                        Log.e(
                            TAG,
                            "Error deleting project item $itemId: ${e.message}",
                            e
                        )
                    }
                }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error deleteProjectItemFromFirestore: ${e.message}",
                e
            )
        }
    }

    // ============================================================
    // USER SYNC
    // ============================================================

    /**
     * مزامنة بيانات المستخدم مع:
     *
     * users/{uid}
     *
     * ملاحظة أمنية مهمة:
     * لا يتم تخزين password إطلاقاً.
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
                "Cannot sync user: user UID is empty."
            )

            return
        }

        /*
         * لا تسمح للتطبيق بكتابة بيانات مستخدم مختلف
         * عن مستخدم Firebase الحالي.
         *
         * الاستثناء الوحيد هو عدم وجود جلسة Firebase،
         * وفي هذه الحالة نرفض العملية أيضاً.
         */
        if (
            currentUid.isBlank() ||
            currentUid != user.uid
        ) {

            Log.w(
                TAG,
                "Cannot sync user ${user.uid}: UID does not match authenticated Firebase user."
            )

            return
        }

        try {

            val normalizedRole =
                when (user.role.trim().uppercase()) {

                    "ADMIN" -> "ADMIN"

                    "MANAGER" -> "MANAGER"

                    else -> "WORKER"
                }

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

            /*
             * password غير موجود هنا عمداً.
             */
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

                    if (
                        e is FirebaseFirestoreException &&
                        e.code ==
                        FirebaseFirestoreException.Code.PERMISSION_DENIED
                    ) {

                        Log.w(
                            TAG,
                            "Sync user ${user.uid} denied: ${e.message}"
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

    // ============================================================
    // FETCH USER
    // ============================================================

    /**
     * البحث عن مستخدم بواسطة:
     *
     * 1. رقم الهاتف
     * 2. البريد الإلكتروني
     *
     * لا يتم قراءة password من Firestore.
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

        /*
         * البحث بواسطة الهاتف أولاً.
         */
        db.collection("users")
            .whereEqualTo(
                "phone",
                cleanQuery
            )
            .get()
            .addOnSuccessListener { snapshots ->

                if (
                    snapshots != null &&
                    !snapshots.isEmpty
                ) {

                    val doc =
                        snapshots.documents.first()

                    val user =
                        createTeamUserFromDocument(
                            doc,
                            cleanQuery
                        )

                    onResult(user)

                } else {

                    /*
                     * إذا لم نجد الهاتف، نبحث بالبريد.
                     */
                    db.collection("users")
                        .whereEqualTo(
                            "email",
                            cleanQuery
                        )
                        .get()
                        .addOnSuccessListener { emailSnapshots ->

                            if (
                                emailSnapshots != null &&
                                !emailSnapshots.isEmpty
                            ) {

                                val doc =
                                    emailSnapshots.documents.first()

                                val user =
                                    createTeamUserFromDocument(
                                        doc,
                                        cleanQuery
                                    )

                                onResult(user)

                            } else {

                                onResult(null)
                            }
                        }
                        .addOnFailureListener { e ->

                            Log.e(
                                TAG,
                                "Error searching user by email",
                                e
                            )

                            onResult(null)
                        }
                }
            }
            .addOnFailureListener { e ->

                Log.e(
                    TAG,
                    "Error searching user by phone",
                    e
                )

                /*
                 * محاولة البحث بالبريد حتى لو فشل استعلام الهاتف.
                 */
                db.collection("users")
                    .whereEqualTo(
                        "email",
                        cleanQuery
                    )
                    .get()
                    .addOnSuccessListener { emailSnapshots ->

                        if (
                            emailSnapshots != null &&
                            !emailSnapshots.isEmpty
                        ) {

                            val doc =
                                emailSnapshots.documents.first()

                            onResult(
                                createTeamUserFromDocument(
                                    doc,
                                    cleanQuery
                                )
                            )

                        } else {

                            onResult(null)
                        }
                    }
                    .addOnFailureListener {

                        onResult(null)
                    }
            }
    }

    /**
     * تحويل Firestore Document إلى TeamUser.
     *
     * password دائماً فارغ.
     */
    private fun createTeamUserFromDocument(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        fallbackQuery: String
    ): TeamUser {

        val role =
            when (
                doc.getString("role")
                    ?.trim()
                    ?.uppercase()
            ) {

                "ADMIN" -> "ADMIN"

                "MANAGER" -> "MANAGER"

                else -> "WORKER"
            }

        return TeamUser(
            uid =
                doc.getString("uid")
                    ?: doc.id,

            name =
                doc.getString("name")
                    ?: "",

            phone =
                doc.getString("phone")
                    ?: if (!fallbackQuery.contains("@")) {
                        fallbackQuery
                    } else {
                        ""
                    },

            email =
                doc.getString("email")
                    ?: if (fallbackQuery.contains("@")) {
                        fallbackQuery
                    } else {
                        ""
                    },

            /*
             * لا يوجد Password في Firestore.
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

        try {

            val cleanWorkshopId =
                workshopId.trim()

            val settingsMap =
                mutableMapOf<String, Any>(
                    "storeName" to storeName,
                    "storePhone" to storePhone,
                    "storeWhatsapp" to storeWhatsapp,
                    "managerName" to managerName,
                    "updatedAt" to System.currentTimeMillis(),
                    "updatedBy" to currentUid
                )

            if (cleanWorkshopId.isNotBlank()) {

                settingsMap["workshopId"] =
                    cleanWorkshopId
            }

            val docId =
                if (cleanWorkshopId.isNotBlank()) {
                    cleanWorkshopId
                } else {
                    "config"
                }

            db.collection("store_settings")
                .document(docId)
                .set(
                    settingsMap,
                    SetOptions.merge()
                )
                .addOnSuccessListener {

                    Log.d(
                        TAG,
                        "Store settings successfully synced."
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
                            "Store settings sync denied: ${e.message}"
                        )

                    } else {

                        Log.e(
                            TAG,
                            "Error syncing store settings: ${e.message}",
                            e
                        )
                    }
                }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error syncStoreSettingsToFirestore: ${e.message}",
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

        try {

            /*
             * لا نسمح بتسجيل عملية باسم مستخدم آخر.
             */
            if (
                log.workerId.isNotBlank() &&
                log.workerId != currentUid
            ) {

                Log.w(
                    TAG,
                    "Audit log workerId does not match current Firebase user."
                )

                return
            }

            val cleanWorkshopId =
                workshopId.trim()

            val logMap =
                mutableMapOf<String, Any>(
                    "workerId" to currentUid,
                    "workerName" to log.workerName,
                    "action" to log.action,
                    "projectId" to log.projectId,
                    "projectName" to log.projectName,
                    "timestamp" to log.timestamp
                )

            if (cleanWorkshopId.isNotBlank()) {

                logMap["workshopId"] =
                    cleanWorkshopId
            }

            logMap["updatedBy"] =
                currentUid

            db.collection("audit_logs")
                .add(logMap)
                .addOnSuccessListener {

                    Log.d(
                        TAG,
                        "Audit log synced."
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
                            "Sync audit log denied: ${e.message}"
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

    // ============================================================
    // WORKSHOP JOIN REQUEST
    // ============================================================

    /**
     * إرسال طلب انضمام العامل إلى ورشة عبر Sync Code.
     *
     * لا يتم تغيير users/{uid}/workshopId هنا.
     *
     * التغيير يحدث فقط بعد موافقة المدير.
     */
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
                "رمز المزامنة فارغ"
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
                    snapshots
                        ?.documents
                        ?.firstOrNull()

                if (
                    workshopDoc != null &&
                    workshopDoc.exists()
                ) {

                    val targetWorkshopId =
                        workshopDoc.id

                    createJoinRequestInFirestore(
                        db,
                        user,
                        targetWorkshopId,
                        cleanCode,
                        onResult
                    )

                } else {

                    /*
                     * دعم الحالة التي يكون فيها Sync Code
                     * هو نفسه document ID.
                     */
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
                                    "رمز المزامنة غير صحيح، الورشة غير موجودة"
                                )
                            }
                        }
                        .addOnFailureListener {

                            onResult(
                                false,
                                "رمز المزامنة غير صحيح، الورشة غير موجودة"
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

                    val status =
                        doc.getString("status")
                            ?: ""

                    when (status) {

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
                        "createdAt" to System.currentTimeMillis(),
                        "updatedAt" to System.currentTimeMillis()
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

                        doc.data?.toMutableMap()?.apply {

                            /*
                             * ضمان وجود requestId حتى لو
                             * لم يكن محفوظاً في البيانات القديمة.
                             */
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

    /**
     * موافقة المدير على طلب الانضمام.
     *
     * العمليات:
     *
     * joinRequests/{requestId}
     *        status = approved
     *
     * workshops/{workshopId}/members/{workerUid}
     *        role = worker
     *
     * users/{workerUid}
     *        workshopId = workshopId
     */
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

        /*
         * نقرأ الطلب أولاً حتى نتأكد من أنه
         * يخص الورشة المطلوبة والعامل المطلوب.
         */
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

                val batch =
                    db.batch()

                batch.update(
                    requestRef,
                    mapOf(
                        "status" to "approved",
                        "updatedAt" to System.currentTimeMillis(),
                        "approvedBy" to currentUid
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
                        "joinedAt" to System.currentTimeMillis(),
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
                        "workshopId" to cleanWorkshopId,
                        "updatedAt" to System.currentTimeMillis()
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

        if (!isFirebaseAuthenticated()) {

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
                    "rejectedBy" to getCurrentUserUid()
                )
            )
            .addOnSuccessListener {

                onResult(
                    true,
                    "تم رفض طلب الانضمام"
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
