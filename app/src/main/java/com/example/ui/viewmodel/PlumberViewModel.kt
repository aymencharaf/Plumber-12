package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.example.data.db.MaterialDatabase
import com.example.data.db.PlumberDatabase
import com.example.data.model.Appointment
import com.example.data.model.AuditLog
import com.example.data.model.CalculatedMaterialCache
import com.example.data.model.CustomMaterial
import com.example.data.model.MaterialEntity
import com.example.data.model.PlumbingMaterial
import com.example.data.model.Project
import com.example.data.model.ProjectItem
import com.example.data.model.TeamUser
import com.example.data.model.WorkAlert
import com.example.data.preset.PlumbingLibraryData
import com.example.data.preset.TeamStorePreferences
import com.example.data.repository.MaterialRepository
import com.example.data.repository.PlumberRepository
import com.example.data.sync.FirestoreSync

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PlumberViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PlumberRepository
    private val materialRepository: MaterialRepository

    private val teamStorePrefs =
        TeamStorePreferences(application)

    private val authPrefs =
        application.getSharedPreferences(
            "auth_session",
            android.content.Context.MODE_PRIVATE
        )

    private val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    companion object {

        private const val SECONDARY_FIREBASE_APP_NAME =
            "PlumberAdminSecondaryAuth"
    }

    // ============================================================
    // LIBRARY
    // ============================================================

    val libraryMaterials: StateFlow<List<MaterialEntity>>

    // ============================================================
    // TEAM USER STATE & RBAC
    // ============================================================

    private val _currentUser =
        MutableStateFlow<TeamUser?>(null)

    val currentUser: StateFlow<TeamUser?> =
        _currentUser.asStateFlow()

    private val _currentRole =
        MutableStateFlow("WORKER")

    val currentRole: StateFlow<String> =
        _currentRole.asStateFlow()

    private val _isUserLoggedIn =
        MutableStateFlow(false)

    val isUserLoggedIn: StateFlow<Boolean> =
        _isUserLoggedIn.asStateFlow()

    private val _pendingJoinRequests =
        MutableStateFlow<List<Map<String, Any>>>(emptyList())

    val pendingJoinRequests: StateFlow<List<Map<String, Any>>> =
        _pendingJoinRequests.asStateFlow()

    private var pendingRequestsListener:
            com.google.firebase.firestore.ListenerRegistration? = null

    val allTeamUsers: StateFlow<List<TeamUser>>

    val allWorkers: StateFlow<List<TeamUser>>

    val allAuditLogs: StateFlow<List<AuditLog>>

    val calculatedMaterialCaches:
            StateFlow<List<CalculatedMaterialCache>>

    // ============================================================
    // INITIALIZATION
    // ============================================================

    init {

        val db =
            PlumberDatabase.getDatabase(application)

        val matDb =
            MaterialDatabase.getDatabase(application)

        repository =
            PlumberRepository(
                db.projectDao(),
                db.projectItemDao(),
                db.customMaterialDao(),
                db.appointmentDao(),
                db.workAlertDao(),
                db.teamUserDao(),
                db.auditLogDao(),
                db.calculatedMaterialCacheDao(),
                db.appSettingCacheDao()
            )

        materialRepository =
            MaterialRepository(
                matDb.materialDao()
            )

        allTeamUsers =
            repository.allTeamUsers
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    emptyList()
                )

        allWorkers =
            repository.allWorkers
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    emptyList()
                )

        allAuditLogs =
            repository.allAuditLogs
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    emptyList()
                )

        calculatedMaterialCaches =
            repository.calculatedMaterialCaches
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    emptyList()
                )

        libraryMaterials =
            materialRepository.allMaterials
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    emptyList()
                )

        viewModelScope.launch {

            materialRepository
                .initializeLibraryIfEmpty(application)

            /*
             * لا نقوم بإنشاء مستخدمين افتراضيين هنا.
             *
             * جميع حسابات المستخدمين يجب أن تكون مرتبطة
             * بـ Firebase Authentication.
             */
            restoreLoginSession()
        }
    }

    // ============================================================
    // CALCULATION CACHE
    // ============================================================

    fun saveCalculationCache(
        title: String,
        calculationType: String,
        pipeCategory: String,
        size: String,
        inputValuesSummary: String,
        estimatedPipesCount: Double,
        estimatedFittingsCount: Int,
        estimatedGlueOrSolder: String,
        generatedMaterialsJson: String
    ) {

        viewModelScope.launch {

            val cache =
                CalculatedMaterialCache(
                    title = title,
                    calculationType = calculationType,
                    pipeCategory = pipeCategory,
                    size = size,
                    inputValuesSummary = inputValuesSummary,
                    estimatedPipesCount = estimatedPipesCount,
                    estimatedFittingsCount = estimatedFittingsCount,
                    estimatedGlueOrSolder = estimatedGlueOrSolder,
                    generatedMaterialsJson = generatedMaterialsJson
                )

            repository.saveCalculatedMaterialCache(
                cache
            )
        }
    }

    fun deleteCalculationCache(
        id: Long
    ) {

        viewModelScope.launch {
            repository.deleteCalculatedMaterialCache(id)
        }
    }

    // ============================================================
    // FIREBASE EMAIL HELPER
    // ============================================================

    private fun getFirebaseEmail(
        input: String,
        localUser: TeamUser? = null
    ): String {

        val cleanInput =
            input.trim()

        if (
            android.util.Patterns.EMAIL_ADDRESS
                .matcher(cleanInput)
                .matches()
        ) {
            return cleanInput
        }

        val savedEmail =
            localUser?.email
                ?.trim()
                .orEmpty()

        if (
            savedEmail.isNotBlank() &&
            android.util.Patterns.EMAIL_ADDRESS
                .matcher(savedEmail)
                .matches()
        ) {
            return savedEmail
        }

        return "${cleanInput.replace(" ", "")}@plumber.com"
    }

    // ============================================================
    // FIREBASE ERROR MESSAGE
    // ============================================================

    private fun getFirebaseErrorMessage(
        exception: Exception
    ): String {

        val errorCode =
            (exception as? FirebaseAuthException)
                ?.errorCode
                ?.uppercase()
                ?: ""

        return when {

            errorCode.contains("USER_NOT_FOUND") ->
                "لا يوجد حساب بهذا البريد الإلكتروني أو رقم الهاتف."

            errorCode.contains("WRONG_PASSWORD") ->
                "كلمة المرور غير صحيحة."

            errorCode.contains("INVALID_CREDENTIAL") ->
                "البريد الإلكتروني أو كلمة المرور غير صحيحة."

            errorCode.contains("INVALID_EMAIL") ->
                "البريد الإلكتروني غير صالح."

            errorCode.contains("EMAIL_ALREADY_IN_USE") ->
                "هذا البريد الإلكتروني مستخدم بالفعل."

            errorCode.contains("WEAK_PASSWORD") ->
                "كلمة المرور ضعيفة. يجب أن تحتوي على 6 أحرف أو أرقام على الأقل."

            errorCode.contains("NETWORK") ->
                "تعذر الاتصال بـ Firebase. تحقق من اتصال الإنترنت."

            errorCode.contains("TOO_MANY_REQUESTS") ->
                "تمت محاولات كثيرة. حاول مرة أخرى لاحقاً."

            exception is FirebaseAuthInvalidCredentialsException ->
                "بيانات تسجيل الدخول غير صحيحة."

            exception is FirebaseAuthWeakPasswordException ->
                "كلمة المرور ضعيفة. يجب أن تحتوي على 6 أحرف أو أرقام على الأقل."

            exception is FirebaseAuthUserCollisionException ->
                "هذا الحساب موجود بالفعل في Firebase Authentication."

            else ->
                exception.localizedMessage
                    ?: "حدث خطأ غير معروف في Firebase Authentication."
        }
    }

    // ============================================================
    // RESTORE FIREBASE SESSION
    // ============================================================

    private suspend fun restoreLoginSession() {

        val firebaseUser =
            firebaseAuth.currentUser

        if (firebaseUser == null) {

            authPrefs.edit()
                .remove("logged_uid")
                .apply()

            return
        }

        val firebaseUid =
            firebaseUser.uid

        Log.d(
            "PlumberViewModel",
            "Firebase session found. UID=$firebaseUid"
        )

        /*
         * نبحث أولاً في Room عن المستخدم المرتبط
         * بنفس UID الحقيقي.
         */
        val localUser =
            repository.getUserByUid(firebaseUid)

        if (localUser != null) {

            if (!localUser.active) {

                firebaseAuth.signOut()

                authPrefs.edit()
                    .remove("logged_uid")
                    .apply()

                return
            }

            val restoredUser =
                localUser.copy(
                    uid = firebaseUid,
                    password = ""
                )

            restoreUserIntoSession(
                restoredUser
            )

            return
        }

        /*
         * إذا لم يكن المستخدم موجوداً في Room،
         * نبحث عنه في Firestore بواسطة البريد.
         */
        val firebaseEmail =
            firebaseUser.email.orEmpty()

        if (firebaseEmail.isBlank()) {

            firebaseAuth.signOut()

            authPrefs.edit()
                .remove("logged_uid")
                .apply()

            Log.e(
                "PlumberViewModel",
                "Firebase user has no email. UID=$firebaseUid"
            )

            return
        }

        FirestoreSync
            .fetchUserFromFirestoreByPhoneOrEmail(
                firebaseEmail
            ) { fetchedUser ->

                viewModelScope.launch {

                    if (fetchedUser == null) {

                        /*
                         * لا ننشئ Worker افتراضياً.
                         *
                         * وجود Firebase Auth بدون TeamUser
                         * يعني أن الحساب غير مربوط بالنظام.
                         */
                        Log.e(
                            "PlumberViewModel",
                            "Firebase account exists but no TeamUser metadata. UID=$firebaseUid"
                        )

                        firebaseAuth.signOut()

                        authPrefs.edit()
                            .remove("logged_uid")
                            .apply()

                        return@launch
                    }

                    /*
                     * حماية مهمة:
                     * UID في Firestore يجب أن يطابق UID في Firebase.
                     */
                    if (
                        fetchedUser.uid.isNotBlank() &&
                        fetchedUser.uid != firebaseUid
                    ) {

                        Log.e(
                            "PlumberViewModel",
                            "UID mismatch during session restore. Firebase=$firebaseUid Firestore=${fetchedUser.uid}"
                        )

                        firebaseAuth.signOut()

                        authPrefs.edit()
                            .remove("logged_uid")
                            .apply()

                        return@launch
                    }

                    val correctedUser =
                        fetchedUser.copy(
                            uid = firebaseUid,
                            password = ""
                        )

                    if (!correctedUser.active) {

                        firebaseAuth.signOut()

                        authPrefs.edit()
                            .remove("logged_uid")
                            .apply()

                        return@launch
                    }

                    repository.insertOrUpdateTeamUser(
                        correctedUser
                    )

                    restoreUserIntoSession(
                        correctedUser
                    )
                }
            }
    }

    // ============================================================
    // RESTORE USER INTO SESSION
    // ============================================================

    private fun restoreUserIntoSession(
        user: TeamUser
    ) {

        val normalizedRole =
            user.role
                .trim()
                .uppercase()

        _currentUser.value =
            user.copy(
                password = ""
            )

        _currentRole.value =
            normalizedRole

        _isUserLoggedIn.value =
            true

        _isManagerLoggedIn.value =
            normalizedRole == "ADMIN"

        authPrefs.edit()
            .putString(
                "logged_uid",
                user.uid
            )
            .apply()

        if (user.workshopId.isNotBlank()) {

            FirestoreSync.startRealtimeListener(
                getApplication(),
                normalizedRole,
                user.uid,
                user.workshopId
            )
        }
    }

    // ============================================================
    // LOGIN WITH FIREBASE AUTHENTICATION
    // ============================================================

    fun loginTeamUser(
        phoneOrEmail: String,
        pass: String,
        onResult: (Boolean, String) -> Unit
    ) {

        val cleanInput =
            phoneOrEmail.trim()

        val cleanPass =
            pass.trim()

        if (
            cleanInput.isBlank() ||
            cleanPass.isBlank()
        ) {

            onResult(
                false,
                "يرجى إدخال رقم الهاتف/البريد الإلكتروني وكلمة المرور."
            )

            return
        }

        if (cleanPass.length < 6) {

            onResult(
                false,
                "كلمة المرور يجب أن تحتوي على 6 أحرف أو أرقام على الأقل."
            )

            return
        }

        viewModelScope.launch {

            try {

                /*
                 * Room يستخدم هنا فقط للمساعدة في معرفة
                 * البريد المرتبط برقم الهاتف.
                 *
                 * كلمة المرور لا تتم قراءتها من Room.
                 */
                val localUser =
                    repository.getUserByPhoneOrEmail(
                        cleanInput
                    )

                val firebaseEmail =
                    getFirebaseEmail(
                        cleanInput,
                        localUser
                    )

                Log.d(
                    "PlumberViewModel",
                    "Firebase login requested: $firebaseEmail"
                )

                firebaseAuth
                    .signInWithEmailAndPassword(
                        firebaseEmail,
                        cleanPass
                    )
                    .addOnSuccessListener { authResult ->

                        viewModelScope.launch {

                            try {

                                val firebaseUser =
                                    authResult.user

                                if (firebaseUser == null) {

                                    onResult(
                                        false,
                                        "تعذر الحصول على بيانات حساب Firebase."
                                    )

                                    return@launch
                                }

                                val firebaseUid =
                                    firebaseUser.uid

                                Log.d(
                                    "PlumberViewModel",
                                    "Firebase login successful. UID=$firebaseUid"
                                )

                                /*
                                 * يجب أن يكون UID هو نفس UID الموجود
                                 * في TeamUser.
                                 */
                                val roomUser =
                                    repository.getUserByUid(
                                        firebaseUid
                                    )

                                if (roomUser != null) {

                                    val correctedUser =
                                        roomUser.copy(
                                            uid = firebaseUid,
                                            password = ""
                                        )

                                    processFirebaseUserLogin(
                                        correctedUser,
                                        onResult
                                    )

                                    return@launch
                                }

                                /*
                                 * المستخدم موجود في Firebase Auth
                                 * ولكنه غير موجود في Room.
                                 *
                                 * نبحث في Firestore.
                                 */
                                FirestoreSync
                                    .fetchUserFromFirestoreByPhoneOrEmail(
                                        cleanInput
                                    ) { fetchedUser ->

                                        viewModelScope.launch {

                                            if (fetchedUser == null) {

                                                /*
                                                 * نحاول بواسطة البريد.
                                                 */
                                                FirestoreSync
                                                    .fetchUserFromFirestoreByPhoneOrEmail(
                                                        firebaseEmail
                                                    ) { emailUser ->

                                                        viewModelScope.launch {

                                                            if (emailUser == null) {

                                                                /*
                                                                 * مهم جداً:
                                                                 *
                                                                 * لا ننشئ Worker تلقائياً.
                                                                 */
                                                                firebaseAuth.signOut()

                                                                onResult(
                                                                    false,
                                                                    "حساب Firebase موجود، لكن بيانات المستخدم غير موجودة في نظام الورشة. يجب ربط الحساب بالمستخدم أولاً."
                                                                )

                                                                return@launch
                                                            }

                                                            if (
                                                                emailUser.uid.isNotBlank() &&
                                                                emailUser.uid != firebaseUid
                                                            ) {

                                                                firebaseAuth.signOut()

                                                                onResult(
                                                                    false,
                                                                    "خطأ أمني: UID في Firestore لا يطابق UID في Firebase Authentication."
                                                                )

                                                                return@launch
                                                            }

                                                            val correctedUser =
                                                                emailUser.copy(
                                                                    uid = firebaseUid,
                                                                    password = ""
                                                                )

                                                            if (!correctedUser.active) {

                                                                firebaseAuth.signOut()

                                                                onResult(
                                                                    false,
                                                                    "هذا الحساب معطل حالياً من قبل الإدارة."
                                                                )

                                                                return@launch
                                                            }

                                                            repository
                                                                .insertOrUpdateTeamUser(
                                                                    correctedUser
                                                                )

                                                            processFirebaseUserLogin(
                                                                correctedUser,
                                                                onResult
                                                            )
                                                        }
                                                    }

                                                return@launch
                                            }

                                            if (
                                                fetchedUser.uid.isNotBlank() &&
                                                fetchedUser.uid != firebaseUid
                                            ) {

                                                firebaseAuth.signOut()

                                                onResult(
                                                    false,
                                                    "خطأ أمني: UID في Firestore لا يطابق UID في Firebase Authentication."
                                                )

                                                return@launch
                                            }

                                            val correctedUser =
                                                fetchedUser.copy(
                                                    uid = firebaseUid,
                                                    password = ""
                                                )

                                            if (!correctedUser.active) {

                                                firebaseAuth.signOut()

                                                onResult(
                                                    false,
                                                    "هذا الحساب معطل حالياً من قبل الإدارة."
                                                )

                                                return@launch
                                            }

                                            repository
                                                .insertOrUpdateTeamUser(
                                                    correctedUser
                                                )

                                            processFirebaseUserLogin(
                                                correctedUser,
                                                onResult
                                            )
                                        }
                                    }

                            } catch (e: Exception) {

                                Log.e(
                                    "PlumberViewModel",
                                    "Error after Firebase login",
                                    e
                                )

                                firebaseAuth.signOut()

                                onResult(
                                    false,
                                    "حدث خطأ بعد تسجيل الدخول: ${
                                        e.localizedMessage
                                            ?: "خطأ غير معروف"
                                    }"
                                )
                            }
                        }
                    }
                    .addOnFailureListener { e ->

                        Log.e(
                            "PlumberViewModel",
                            "Firebase login failed",
                            e
                        )

                        onResult(
                            false,
                            getFirebaseErrorMessage(e)
                        )
                    }

            } catch (e: Exception) {

                Log.e(
                    "PlumberViewModel",
                    "loginTeamUser error",
                    e
                )

                onResult(
                    false,
                    "حدث خطأ أثناء تسجيل الدخول: ${
                        e.localizedMessage
                            ?: "خطأ غير معروف"
                    }"
                )
            }
        }
    }

    // ============================================================
    // PROCESS FIREBASE LOGIN
    // ============================================================

    private suspend fun processFirebaseUserLogin(
        user: TeamUser,
        onResult: (Boolean, String) -> Unit
    ) {

        val currentFirebaseUser =
            firebaseAuth.currentUser

        if (currentFirebaseUser == null) {

            onResult(
                false,
                "لا توجد جلسة Firebase صالحة."
            )

            return
        }

        if (currentFirebaseUser.uid != user.uid) {

            Log.e(
                "PlumberViewModel",
                "UID mismatch. Firebase=${currentFirebaseUser.uid}, User=${user.uid}"
            )

            firebaseAuth.signOut()

            onResult(
                false,
                "حساب Firebase لا يتطابق مع بيانات المستخدم."
            )

            return
        }

        if (!user.active) {

            firebaseAuth.signOut()

            onResult(
                false,
                "هذا الحساب معطل حالياً من قبل الإدارة."
            )

            return
        }

        val normalizedRole =
            user.role
                .trim()
                .uppercase()

        /*
         * لا توجد كلمة مرور في TeamUser.
         */
        val updatedUser =
            user.copy(
                password = "",
                role = normalizedRole,
                lastLoginAt =
                    System.currentTimeMillis()
            )

        repository.insertOrUpdateTeamUser(
            updatedUser
        )

        /*
         * المزامنة لا ترسل كلمة المرور.
         */
        FirestoreSync.syncUserToFirestore(
            updatedUser
        )

        _currentUser.value =
            updatedUser

        _currentRole.value =
            normalizedRole

        _isUserLoggedIn.value =
            true

        _isManagerLoggedIn.value =
            normalizedRole == "ADMIN"

        authPrefs.edit()
            .putString(
                "logged_uid",
                updatedUser.uid
            )
            .apply()

        if (updatedUser.workshopId.isNotBlank()) {

            FirestoreSync.startRealtimeListener(
                getApplication(),
                normalizedRole,
                updatedUser.uid,
                updatedUser.workshopId
            )
        }

        logAuditAction(
            "تسجيل دخول إلى النظام: ${updatedUser.name}",
            0,
            ""
        )

        onResult(
            true,
            "مرحباً بك ${updatedUser.name}! 🟢"
        )
    }

    // ============================================================
    // WORKER SELF REGISTRATION
    // ============================================================

    fun registerWorkerAccount(
        name: String,
        phone: String,
        email: String,
        pass: String,
        role: String = "WORKER",
        autoLogin: Boolean = true,
        onResult: (Boolean, String) -> Unit
    ) {

        val cleanName =
            name.trim()

        val cleanPhone =
            phone.trim()
                .replace(" ", "")

        val cleanEmailInput =
            email.trim()

        val cleanPassword =
            pass.trim()

        val cleanRole =
            role.trim()
                .uppercase()

        if (
            cleanName.isBlank() ||
            cleanPhone.isBlank() ||
            cleanPassword.isBlank()
        ) {

            onResult(
                false,
                "يرجى ملء الاسم الكامل ورقم الهاتف وكلمة المرور."
            )

            return
        }

        if (cleanPassword.length < 6) {

            onResult(
                false,
                "كلمة المرور يجب أن تحتوي على 6 أحرف أو أرقام على الأقل."
            )

            return
        }

        if (
            cleanRole != "WORKER" &&
            cleanRole != "ADMIN"
        ) {

            onResult(
                false,
                "نوع الحساب غير صالح."
            )

            return
        }

        viewModelScope.launch {

            try {

                val existingByPhone =
                    repository.getUserByPhoneOrEmail(
                        cleanPhone
                    )

                if (existingByPhone != null) {

                    onResult(
                        false,
                        "رقم الهاتف ($cleanPhone) مسجل بالفعل."
                    )

                    return@launch
                }

                val firebaseEmail =
                    if (cleanEmailInput.isBlank()) {
                        "$cleanPhone@plumber.com"
                    } else {
                        cleanEmailInput
                    }

                if (
                    !android.util.Patterns.EMAIL_ADDRESS
                        .matcher(firebaseEmail)
                        .matches()
                ) {

                    onResult(
                        false,
                        "البريد الإلكتروني غير صالح."
                    )

                    return@launch
                }

                val existingByEmail =
                    repository.getUserByPhoneOrEmail(
                        firebaseEmail
                    )

                if (existingByEmail != null) {

                    onResult(
                        false,
                        "البريد الإلكتروني مستخدم بالفعل."
                    )

                    return@launch
                }

                Log.d(
                    "PlumberViewModel",
                    "Creating Firebase account: $firebaseEmail"
                )

                firebaseAuth
                    .createUserWithEmailAndPassword(
                        firebaseEmail,
                        cleanPassword
                    )
                    .addOnSuccessListener { authResult ->

                        viewModelScope.launch {

                            try {

                                val firebaseUser =
                                    authResult.user

                                if (firebaseUser == null) {

                                    onResult(
                                        false,
                                        "تم إنشاء الحساب ولكن تعذر الحصول على UID."
                                    )

                                    return@launch
                                }

                                val firebaseUid =
                                    firebaseUser.uid

                                val now =
                                    System.currentTimeMillis()

                                val newUser =
                                    TeamUser(
                                        uid = firebaseUid,
                                        name = cleanName,
                                        phone = cleanPhone,
                                        email = firebaseEmail,
                                        password = "",
                                        role = cleanRole,
                                        active = true,
                                        workshopId = "",
                                        createdAt = now,
                                        lastLoginAt = now
                                    )

                                repository
                                    .insertOrUpdateTeamUser(
                                        newUser
                                    )

                                FirestoreSync
                                    .syncUserToFirestore(
                                        newUser
                                    )

                                val updatedList =
                                    _workersList.value
                                        .toMutableList()

                                val entry =
                                    "${newUser.name} (${newUser.phone})"

                                if (
                                    !updatedList.contains(entry)
                                ) {

                                    updatedList.add(entry)

                                    updateWorkersList(
                                        updatedList
                                    )
                                }

                                /*
                                 * Firebase يسجل الحساب الجديد
                                 * تلقائياً.
                                 */
                                if (autoLogin) {

                                    restoreUserIntoSession(
                                        newUser
                                    )
                                }

                                logAuditAction(
                                    "تسجيل حساب عامل جديد في Firebase: ${newUser.name}",
                                    0,
                                    ""
                                )

                                onResult(
                                    true,
                                    "تم إنشاء حساب العامل ${newUser.name} بنجاح! 🟢"
                                )

                            } catch (e: Exception) {

                                Log.e(
                                    "PlumberViewModel",
                                    "Error saving new worker",
                                    e
                                )

                                onResult(
                                    false,
                                    "تم إنشاء حساب Firebase ولكن حدث خطأ في حفظ بيانات العامل: ${
                                        e.localizedMessage
                                            ?: "خطأ غير معروف"
                                    }"
                                )
                            }
                        }
                    }
                    .addOnFailureListener { e ->

                        Log.e(
                            "PlumberViewModel",
                            "Worker registration failed",
                            e
                        )

                        onResult(
                            false,
                            getFirebaseErrorMessage(e)
                        )
                    }

            } catch (e: Exception) {

                Log.e(
                    "PlumberViewModel",
                    "registerWorkerAccount error",
                    e
                )

                onResult(
                    false,
                    "حدث خطأ أثناء إنشاء الحساب: ${
                        e.localizedMessage
                            ?: "خطأ غير معروف"
                    }"
                )
            }
        }
    }

    // ============================================================
    // SECONDARY FIREBASE AUTH
    // ============================================================

    private fun getSecondaryFirebaseAuth(): FirebaseAuth? {

        return try {

            val context =
                getApplication<Application>()

            val secondaryApp =
                try {

                    FirebaseApp.getInstance(
                        SECONDARY_FIREBASE_APP_NAME
                    )

                } catch (_: IllegalStateException) {

                    FirebaseApp.initializeApp(
                        context,
                        FirebaseApp.getInstance().options,
                        SECONDARY_FIREBASE_APP_NAME
                    )
                }

            if (secondaryApp == null) {
                null
            } else {
                FirebaseAuth.getInstance(
                    secondaryApp
                )
            }

        } catch (e: Exception) {

            Log.e(
                "PlumberViewModel",
                "Unable to create secondary Firebase Auth",
                e
            )

            null
        }
    }

    // ============================================================
    // LOGOUT
    // ============================================================

    fun logoutTeamUser() {

        if (_currentUser.value != null) {

            logAuditAction(
                "تسجيل خروج من النظام",
                0,
                ""
            )
        }

        FirestoreSync.stopRealtimeListener()

        pendingRequestsListener?.remove()
        pendingRequestsListener = null

        firebaseAuth.signOut()

        _currentUser.value =
            null

        _currentRole.value =
            "WORKER"

        _isUserLoggedIn.value =
            false

        _isManagerLoggedIn.value =
            false

        authPrefs.edit()
            .remove("logged_uid")
            .apply()
    }

    // ============================================================
    // WORKSHOP JOIN REQUESTS
    // ============================================================

    fun startPendingRequestsListener(
        workshopId: String
    ) {

        pendingRequestsListener?.remove()

        if (workshopId.isBlank()) {
            return
        }

        pendingRequestsListener =
            FirestoreSync.listenToPendingJoinRequests(
                workshopId
            ) { list ->

                _pendingJoinRequests.value =
                    list
            }
    }

    fun connectWorkerToWorkshopWithSyncCode(
        syncCode: String,
        onResult: (Boolean, String) -> Unit
    ) {

        val user =
            _currentUser.value

        if (user == null) {

            onResult(
                false,
                "يجب تسجيل الدخول أولاً."
            )

            return
        }

        if (user.role.uppercase() != "WORKER") {

            onResult(
                false,
                "هذه العملية مخصصة لحسابات العمال."
            )

            return
        }

        viewModelScope.launch {

            FirestoreSync.sendWorkerJoinRequest(
                user,
                syncCode.trim()
            ) { success, msg ->

                onResult(
                    success,
                    msg
                )
            }
        }
    }

    fun approveJoinRequest(
        requestId: String,
        workerUid: String,
        workshopId: String,
        onResult: (Boolean, String) -> Unit
    ) {

        if (_currentRole.value != "ADMIN") {

            onResult(
                false,
                "هذه العملية متاحة للإدارة فقط."
            )

            return
        }

        viewModelScope.launch {

            FirestoreSync.approveJoinRequest(
                requestId,
                workerUid,
                workshopId
            ) { success, msg ->

                if (success) {

                    _pendingJoinRequests.value =
                        _pendingJoinRequests.value
                            .filterNot {
                                it["requestId"] == requestId
                            }
                }

                onResult(
                    success,
                    msg
                )
            }
        }
    }

    fun rejectJoinRequest(
        requestId: String,
        onResult: (Boolean, String) -> Unit
    ) {

        if (_currentRole.value != "ADMIN") {

            onResult(
                false,
                "هذه العملية متاحة للإدارة فقط."
            )

            return
        }

        viewModelScope.launch {

            FirestoreSync.rejectJoinRequest(
                requestId
            ) { success, msg ->

                if (success) {

                    _pendingJoinRequests.value =
                        _pendingJoinRequests.value
                            .filterNot {
                                it["requestId"] == requestId
                            }
                }

                onResult(
                    success,
                    msg
                )
            }
        }
    }

    // ============================================================
    // ADMIN CREATE WORKER ACCOUNT
    // ============================================================

    fun createWorkerAccount(
        name: String,
        phone: String,
        email: String,
        pass: String,
        role: String = "WORKER",
        onResult: (Boolean, String) -> Unit
    ) {

        if (
            !_isUserLoggedIn.value ||
            _currentRole.value != "ADMIN"
        ) {

            onResult(
                false,
                "عذراً! الإدارة فقط تملك صلاحية إنشاء حسابات العمال."
            )

            return
        }

        val cleanName =
            name.trim()

        val cleanPhone =
            phone.trim()
                .replace(" ", "")

        val cleanEmailInput =
            email.trim()

        val cleanPassword =
            pass.trim()

        val cleanRole =
            role.trim()
                .uppercase()

        if (
            cleanName.isBlank() ||
            cleanPhone.isBlank() ||
            cleanPassword.isBlank()
        ) {

            onResult(
                false,
                "يرجى ملء الاسم الكامل ورقم الهاتف وكلمة المرور."
            )

            return
        }

        if (cleanPassword.length < 6) {

            onResult(
                false,
                "كلمة المرور يجب أن تحتوي على 6 أحرف أو أرقام على الأقل."
            )

            return
        }

        if (cleanRole != "WORKER") {

            onResult(
                false,
                "يمكن للمدير إنشاء حساب عامل من هذا القسم فقط."
            )

            return
        }

        viewModelScope.launch {

            try {

                val existingByPhone =
                    repository.getUserByPhoneOrEmail(
                        cleanPhone
                    )

                if (existingByPhone != null) {

                    onResult(
                        false,
                        "رقم الهاتف مستخدم بالفعل."
                    )

                    return@launch
                }

                val firebaseEmail =
                    if (cleanEmailInput.isBlank()) {
                        "$cleanPhone@plumber.com"
                    } else {
                        cleanEmailInput
                    }

                if (
                    !android.util.Patterns.EMAIL_ADDRESS
                        .matcher(firebaseEmail)
                        .matches()
                ) {

                    onResult(
                        false,
                        "البريد الإلكتروني غير صالح."
                    )

                    return@launch
                }

                val existingByEmail =
                    repository.getUserByPhoneOrEmail(
                        firebaseEmail
                    )

                if (existingByEmail != null) {

                    onResult(
                        false,
                        "البريد الإلكتروني مستخدم بالفعل."
                    )

                    return@launch
                }

                val secondaryAuth =
                    getSecondaryFirebaseAuth()

                if (secondaryAuth == null) {

                    onResult(
                        false,
                        "تعذر تهيئة Firebase Authentication الثانوي."
                    )

                    return@launch
                }

                secondaryAuth
                    .createUserWithEmailAndPassword(
                        firebaseEmail,
                        cleanPassword
                    )
                    .addOnSuccessListener { result ->

                        viewModelScope.launch {

                            try {

                                val firebaseUser =
                                    result.user

                                if (firebaseUser == null) {

                                    secondaryAuth.signOut()

                                    onResult(
                                        false,
                                        "تعذر الحصول على UID للعامل."
                                    )

                                    return@launch
                                }

                                val workerUid =
                                    firebaseUser.uid

                                val newUser =
                                    TeamUser(
                                        uid = workerUid,
                                        name = cleanName,
                                        phone = cleanPhone,
                                        email = firebaseEmail,
                                        password = "",
                                        role = "WORKER",
                                        active = true,
                                        workshopId = "",
                                        createdAt =
                                            System.currentTimeMillis(),
                                        lastLoginAt = 0L
                                    )

                                repository
                                    .insertOrUpdateTeamUser(
                                        newUser
                                    )

                                FirestoreSync
                                    .syncUserToFirestore(
                                        newUser
                                    )

                                val updatedList =
                                    _workersList.value
                                        .toMutableList()

                                val entry =
                                    "${newUser.name} (${newUser.phone})"

                                if (
                                    !updatedList.contains(entry)
                                ) {

                                    updatedList.add(entry)

                                    updateWorkersList(
                                        updatedList
                                    )
                                }

                                /*
                                 * مهم:
                                 *
                                 * تسجيل العامل عبر الحساب الثانوي
                                 * لا يغير جلسة المدير الأساسية.
                                 */
                                secondaryAuth.signOut()

                                logAuditAction(
                                    "إنشاء حساب Firebase للعامل: ${newUser.name}",
                                    0,
                                    ""
                                )

                                onResult(
                                    true,
                                    "تم إنشاء حساب العامل ${newUser.name} في Firebase Authentication بنجاح! 🟢"
                                )

                            } catch (e: Exception) {

                                secondaryAuth.signOut()

                                Log.e(
                                    "PlumberViewModel",
                                    "Error saving admin-created worker",
                                    e
                                )

                                onResult(
                                    false,
                                    "تم إنشاء الحساب في Firebase لكن حدث خطأ في حفظ بيانات العامل: ${
                                        e.localizedMessage
                                            ?: "خطأ غير معروف"
                                    }"
                                )
                            }
                        }
                    }
                    .addOnFailureListener { e ->

                        secondaryAuth.signOut()

                        Log.e(
                            "PlumberViewModel",
                            "Admin worker creation failed",
                            e
                        )

                        onResult(
                            false,
                            getFirebaseErrorMessage(e)
                        )
                    }

            } catch (e: Exception) {

                Log.e(
                    "PlumberViewModel",
                    "createWorkerAccount error",
                    e
                )

                onResult(
                    false,
                    "حدث خطأ أثناء إنشاء الحساب: ${
                        e.localizedMessage
                            ?: "خطأ غير معروف"
                    }"
                )
            }
        }
    }

    // ============================================================
    // WORKER ACTIVE / DISABLE
    // ============================================================

    fun toggleWorkerActive(
        uid: String,
        active: Boolean
    ) {

        if (
            !_isUserLoggedIn.value ||
            _currentRole.value != "ADMIN"
        ) {
            return
        }

        viewModelScope.launch {

            val user =
                repository.getUserByUid(uid)

            if (user == null) {
                return@launch
            }

            repository.setUserActive(
                uid,
                active
            )

            val updated =
                user.copy(
                    active = active,
                    password = ""
                )

            FirestoreSync.syncUserToFirestore(
                updated
            )

            logAuditAction(
                "تغيير حالة حساب العامل ${user.name} إلى: ${
                    if (active) "مفعّل" else "معطّل"
                }",
                0,
                ""
            )
        }
    }

    // ============================================================
    // RESET WORKER PASSWORD
    // ============================================================

    fun resetWorkerPassword(
        uid: String,
        newPass: String,
        onDone: (Boolean) -> Unit
    ) {

        if (
            !_isUserLoggedIn.value ||
            _currentRole.value != "ADMIN"
        ) {

            onDone(false)
            return
        }

        if (newPass.trim().length < 6) {

            onDone(false)
            return
        }

        viewModelScope.launch {

            val user =
                repository.getUserByUid(uid)

            if (user == null) {

                onDone(false)
                return@launch
            }

            /*
             * لا نحفظ كلمة المرور.
             *
             * تغيير كلمة مرور مستخدم آخر يحتاج
             * Firebase Admin SDK / Cloud Function.
             */
            Log.w(
                "PlumberViewModel",
                "Password reset requires Firebase Admin SDK. UID=$uid"
            )

            onDone(false)
        }
    }

    // ============================================================
    // DELETE WORKER
    // ============================================================

    fun deleteWorkerAccount(
        uid: String
    ) {

        if (
            !_isUserLoggedIn.value ||
            _currentRole.value != "ADMIN"
        ) {
            return
        }

        viewModelScope.launch {

            val user =
                repository.getUserByUid(uid)

            if (user != null) {

                repository.deleteTeamUser(
                    uid
                )

                logAuditAction(
                    "حذف بيانات العامل من الجهاز: ${user.name}",
                    0,
                    ""
                )

                /*
                 * ملاحظة:
                 *
                 * هذا يحذف TeamUser من Room فقط.
                 * حذف Firebase Authentication نفسه يحتاج
                 * Firebase Admin SDK / Cloud Function.
                 */
                Log.w(
                    "PlumberViewModel",
                    "Firebase Auth account was NOT deleted. Admin SDK required. UID=$uid"
                )
            }
        }
    }

    // ============================================================
    // AUDIT LOG
    // ============================================================

    fun logAuditAction(
        action: String,
        projectId: Long = 0,
        projectName: String = ""
    ) {

        viewModelScope.launch {

            val worker =
                _currentUser.value

            val log =
                AuditLog(
                    workerId =
                        worker?.uid
                            ?: "GUEST",

                    workerName =
                        worker?.name
                            ?: _activeWorker.value,

                    action =
                        action,

                    projectId =
                        projectId,

                    projectName =
                        projectName,

                    timestamp =
                        System.currentTimeMillis()
                )

            repository.insertAuditLog(
                log
            )

            FirestoreSync.syncAuditLogToFirestore(
                log
            )
        }
    }

    // ============================================================
    // MATERIAL LIBRARY
    // ============================================================

    fun addMaterialToLibrary(
        material: MaterialEntity
    ) {

        viewModelScope.launch {

            materialRepository.insertMaterial(
                material
            )
        }
    }

    fun updateMaterialInLibrary(
        material: MaterialEntity
    ) {

        viewModelScope.launch {

            materialRepository.updateMaterial(
                material
            )
        }
    }

    fun deleteMaterialFromLibrary(
        id: String
    ) {

        viewModelScope.launch {

            materialRepository.deleteMaterial(
                id
            )
        }
    }

    fun addLibraryMaterialToProject(
        material: MaterialEntity,
        size: String,
        quantity: Double,
        unit: String,
        notes: String = ""
    ) {

        val projId =
            _selectedProjectId.value
                ?: return

        viewModelScope.launch {

            val item =
                ProjectItem(
                    projectId =
                        projId,

                    materialKey =
                        material.id,

                    materialNameAr =
                        material.nameAr,

                    materialNameFr =
                        material.nameFr,

                    category =
                        material.category,

                    size =
                        size,

                    quantity =
                        quantity,

                    unit =
                        unit,

                    unitPrice =
                        material.price,

                    notes =
                        notes,

                    iconType =
                        material.iconType,

                    imageUri =
                        material.image.ifBlank {
                            null
                        }
                )

            repository.addOrUpdateProjectItem(
                item
            )
        }
    }

    // ============================================================
    // STORE SETTINGS
    // ============================================================

    private val _storeName =
        MutableStateFlow(
            teamStorePrefs.storeName
        )

    val storeName: StateFlow<String> =
        _storeName.asStateFlow()

    private val _storePhone =
        MutableStateFlow(
            teamStorePrefs.storePhone
        )

    val storePhone: StateFlow<String> =
        _storePhone.asStateFlow()

    private val _storeWhatsapp =
        MutableStateFlow(
            teamStorePrefs.storeWhatsapp
        )

    val storeWhatsapp: StateFlow<String> =
        _storeWhatsapp.asStateFlow()

    private val _activeWorker =
        MutableStateFlow(
            teamStorePrefs.activeWorker
        )

    val activeWorker: StateFlow<String> =
        _activeWorker.asStateFlow()

    private val _managerName =
        MutableStateFlow(
            teamStorePrefs.managerName
        )

    val managerName: StateFlow<String> =
        _managerName.asStateFlow()

    private val _isManagerLoggedIn =
        MutableStateFlow(false)

    val isManagerLoggedIn: StateFlow<Boolean> =
        _isManagerLoggedIn.asStateFlow()

    private val _managerPin =
        MutableStateFlow(
            teamStorePrefs.managerPin
        )

    val managerPin: StateFlow<String> =
        _managerPin.asStateFlow()

    private val _workersList =
        MutableStateFlow(
            teamStorePrefs.getWorkersList()
        )

    val workersList: StateFlow<List<String>> =
        _workersList.asStateFlow()

    /*
     * هذه الدالة لا تمنح صلاحية ADMIN.
     *
     * صلاحية الإدارة يجب أن تأتي من Firebase + role.
     */
    fun setManagerLoggedIn(
        loggedIn: Boolean
    ) {

        if (!loggedIn) {

            _isManagerLoggedIn.value =
                false

            return
        }

        val firebaseUser =
            firebaseAuth.currentUser

        val currentRole =
            _currentRole.value.uppercase()

        _isManagerLoggedIn.value =
            firebaseUser != null &&
                    currentRole == "ADMIN"
    }

    /*
     * PIN المدير أصبح اختصاراً محلياً للواجهة فقط.
     *
     * لا يسمح بإنشاء جلسة ADMIN من دون
     * Firebase Authentication.
     */
    fun loginManager(
        pin: String
    ): Boolean {

        val firebaseUser =
            firebaseAuth.currentUser

        if (firebaseUser == null) {
            return false
        }

        if (_currentRole.value.uppercase() != "ADMIN") {
            return false
        }

        if (
            pin.trim() ==
            teamStorePrefs.managerPin
        ) {

            _isManagerLoggedIn.value =
                true

            return true
        }

        return false
    }

    fun logoutManager() {

        _isManagerLoggedIn.value =
            false
    }

    fun updateManagerPin(
        oldPin: String,
        newPin: String
    ): Boolean {

        if (
            _currentRole.value.uppercase() != "ADMIN"
        ) {
            return false
        }

        if (
            oldPin.trim() ==
            teamStorePrefs.managerPin &&
            newPin.isNotBlank()
        ) {

            teamStorePrefs.managerPin =
                newPin.trim()

            _managerPin.value =
                newPin.trim()

            return true
        }

        return false
    }

    fun updateTeamStoreSettings(
        storeNameVal: String,
        storePhoneVal: String,
        activeWorkerVal: String,
        managerNameVal: String =
            _managerName.value,
        storeWhatsappVal: String =
            _storeWhatsapp.value
    ) {

        teamStorePrefs.storeName =
            storeNameVal

        teamStorePrefs.storePhone =
            storePhoneVal

        teamStorePrefs.storeWhatsapp =
            storeWhatsappVal

        teamStorePrefs.activeWorker =
            activeWorkerVal

        teamStorePrefs.managerName =
            managerNameVal

        _storeName.value =
            storeNameVal

        _storePhone.value =
            storePhoneVal

        _storeWhatsapp.value =
            storeWhatsappVal

        _activeWorker.value =
            activeWorkerVal

        _managerName.value =
            managerNameVal

        FirestoreSync.syncStoreSettingsToFirestore(
            storeName = storeNameVal,
            storePhone = storePhoneVal,
            storeWhatsapp = storeWhatsappVal,
            managerName = managerNameVal
        )
    }

    fun updateWorkersList(
        newList: List<String>
    ) {

        teamStorePrefs.saveWorkersList(
            newList
        )

        _workersList.value =
            newList
    }

    // ============================================================
    // PROJECT STATUS
    // ============================================================

    fun updateProjectOrderStatus(
        projectId: Long,
        newStatus: String
    ) {

        viewModelScope.launch {

            val proj =
                repository.getProject(projectId)
                    .firstOrNull()

            if (proj != null) {

                repository.updateProject(
                    proj.copy(
                        orderStatus = newStatus,
                        updatedAt =
                            System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // ============================================================
    // PROJECTS
    // ============================================================

    val allProjects:
            StateFlow<List<Project>> =
        repository.allProjects
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    val visibleProjects:
            StateFlow<List<Project>> =
        combine(
            allProjects,
            currentUser,
            currentRole
        ) { projects, user, role ->

            if (
                role == "ADMIN"
            ) {

                projects

            } else {

                if (user == null) {
                    emptyList()
                } else {

                    val userUid =
                        user.uid

                    val userPhone =
                        user.phone

                    val userName =
                        user.name

                    projects.filter { proj ->

                        proj.isAssignedToWorker(
                            userUid
                        ) ||
                                proj.isAssignedToWorker(
                                    userPhone
                                ) ||
                                proj.isAssignedToWorker(
                                    userName
                                ) ||
                                proj.workerName.isBlank()
                    }
                }
            }

        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // ============================================================
    // OTHER DATA
    // ============================================================

    val customMaterials:
            StateFlow<List<CustomMaterial>> =
        repository.customMaterials
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    val allAppointments:
            StateFlow<List<Appointment>> =
        repository.allAppointments
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    val allWorkAlerts:
            StateFlow<List<WorkAlert>> =
        repository.allWorkAlerts
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    // ============================================================
    // WORK ALERTS
    // ============================================================

    fun addWorkAlert(
        alertType: String,
        senderName: String,
        recipientRole: String,
        title: String,
        details: String,
        projectName: String = ""
    ) {

        viewModelScope.launch {

            val alert =
                WorkAlert(
                    alertType = alertType,
                    senderName = senderName,
                    recipientRole = recipientRole,
                    title = title,
                    details = details,
                    projectName = projectName
                )

            repository.insertWorkAlert(
                alert
            )
        }
    }

    fun updateWorkAlertStatus(
        alert: WorkAlert,
        newStatus: String
    ) {

        viewModelScope.launch {

            repository.updateWorkAlert(
                alert.copy(
                    status = newStatus
                )
            )
        }
    }

    fun deleteWorkAlert(
        id: Long
    ) {

        viewModelScope.launch {

            repository.deleteWorkAlert(
                id
            )
        }
    }

    // ============================================================
    // APPOINTMENTS
    // ============================================================

    fun addAppointment(
        serviceType: String,
        customerName: String,
        phoneNumber: String,
        address: String,
        scheduledTime: String,
        notes: String = ""
    ) {

        viewModelScope.launch {

            val appt =
                Appointment(
                    serviceType = serviceType,
                    customerName = customerName,
                    phoneNumber = phoneNumber,
                    address = address,
                    scheduledTime = scheduledTime,
                    notes = notes
                )

            repository.insertAppointment(
                appt
            )
        }
    }

    fun updateAppointmentStatus(
        appointment: Appointment,
        newStatus: String
    ) {

        viewModelScope.launch {

            repository.updateAppointment(
                appointment.copy(
                    status = newStatus
                )
            )
        }
    }

    fun deleteAppointment(
        id: Long
    ) {

        viewModelScope.launch {

            repository.deleteAppointment(
                id
            )
        }
    }

    // ============================================================
    // CURRENT PROJECT
    // ============================================================

    private val _selectedProjectId =
        MutableStateFlow<Long?>(null)

    val selectedProjectId:
            StateFlow<Long?> =
        _selectedProjectId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentProject:
            StateFlow<Project?> =
        _selectedProjectId
            .flatMapLatest { id ->

                if (id != null) {
                    repository.getProject(id)
                } else {
                    flowOf(null)
                }

            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentProjectItems:
            StateFlow<List<ProjectItem>> =
        _selectedProjectId
            .flatMapLatest { id ->

                if (id != null) {
                    repository.getProjectItems(id)
                } else {
                    flowOf(emptyList())
                }

            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    fun selectProject(
        projectId: Long?
    ) {

        _selectedProjectId.value =
            projectId
    }

    // ============================================================
    // CREATE PROJECT
    // ============================================================

    fun createProject(
        title: String,
        clientName: String = "",
        location: String = "",
        notes: String = "",
        workTypeKey: String = "CUSTOM",
        workerName: String = _activeWorker.value,
        managerName: String = _managerName.value,
        storePhone: String = _storePhone.value,
        laborCost: Double = 0.0,
        paidAmount: Double = 0.0,
        onProjectCreated: (Long) -> Unit
    ) {

        viewModelScope.launch {

            val workTypeObj =
                PlumbingLibraryData.WORK_TYPES
                    .find {
                        it.key == workTypeKey
                    }

            val workTypeName =
                workTypeObj?.titleAr
                    ?: "عمل مخصص"

            val currentUser =
                _currentUser.value

            val createdBy =
                currentUser?.uid
                    ?: return@launch

            val finalWorkerName =
                workerName.ifBlank {
                    _activeWorker.value
                }

            val finalManagerName =
                managerName.ifBlank {
                    _managerName.value
                }

            val finalStorePhone =
                storePhone.ifBlank {
                    _storePhone.value
                }

            val newProj =
                Project(
                    title =
                        title.ifBlank {
                            "مشروع جديد"
                        },

                    clientName =
                        clientName,

                    location =
                        location,

                    notes =
                        notes,

                    workTypeKey =
                        workTypeKey,

                    workTypeNameAr =
                        workTypeName,

                    workerName =
                        finalWorkerName,

                    managerName =
                        finalManagerName,

                    storePhone =
                        finalStorePhone,

                    assignedWorkers =
                        finalWorkerName,

                    createdBy =
                        createdBy,

                    laborCost =
                        laborCost,

                    paidAmount =
                        paidAmount
                )

            val newId =
                repository.createProject(
                    newProj
                )

            val createdObj =
                newProj.copy(
                    id = newId
                )

            FirestoreSync.syncProjectToFirestore(
                createdObj
            )

            logAuditAction(
                "إنشاء مشروع جديد: ${createdObj.title}",
                newId,
                createdObj.title
            )

            _selectedProjectId.value =
                newId

            onProjectCreated(
                newId
            )
        }
    }

    // ============================================================
    // PAYMENTS
    // ============================================================

    fun addWorkerPayment(
        projectId: Long,
        paymentAmount: Double
    ) {

        if (paymentAmount <= 0) {
            return
        }

        viewModelScope.launch {

            val proj =
                repository.getProject(projectId)
                    .firstOrNull()

            if (proj != null) {

                /*
                 * حماية الواجهة فقط.
                 * قواعد Firestore يجب أن تمنع العامل
                 * من تغيير paidAmount على الخادم.
                 */
                if (_currentRole.value != "ADMIN") {
                    return@launch
                }

                val newPaid =
                    proj.paidAmount +
                            paymentAmount

                val updated =
                    proj.copy(
                        paidAmount =
                            newPaid,

                        updatedAt =
                            System.currentTimeMillis()
                    )

                repository.updateProject(
                    updated
                )

                FirestoreSync.syncProjectToFirestore(
                    updated
                )

                logAuditAction(
                    "إضافة دفعة للمشروع: $paymentAmount د.ج",
                    projectId,
                    proj.title
                )
            }
        }
    }

    fun updateProjectAssignmentAndFinancials(
        projectId: Long,
        workerName: String,
        managerName: String,
        laborCost: Double,
        paidAmount: Double
    ) {

        if (_currentRole.value != "ADMIN") {
            return
        }

        viewModelScope.launch {

            val proj =
                repository.getProject(projectId)
                    .firstOrNull()

            if (proj != null) {

                val updated =
                    proj.copy(
                        workerName =
                            workerName,

                        managerName =
                            managerName,

                        laborCost =
                            laborCost,

                        paidAmount =
                            paidAmount,

                        updatedAt =
                            System.currentTimeMillis()
                    )

                repository.updateProject(
                    updated
                )

                FirestoreSync.syncProjectToFirestore(
                    updated
                )

                logAuditAction(
                    "تعديل تعيين العامل والتكاليف للمشروع: ${proj.title}",
                    projectId,
                    proj.title
                )
            }
        }
    }

    fun updateProjectInfo(
        project: Project
    ) {

        viewModelScope.launch {

            repository.updateProject(
                project
            )

            FirestoreSync.syncProjectToFirestore(
                project
            )

            logAuditAction(
                "تعديل بيانات المشروع: ${project.title}",
                project.id,
                project.title
            )
        }
    }

    fun deleteProject(
        projectId: Long
    ) {

        if (_currentRole.value != "ADMIN") {
            return
        }

        viewModelScope.launch {

            val proj =
                repository.getProject(projectId)
                    .firstOrNull()

            val projTitle =
                proj?.title ?: ""

            repository.deleteProject(
                projectId
            )

            FirestoreSync.deleteProjectFromFirestore(
                projectId
            )

            logAuditAction(
                "حذف مشروع: $projTitle",
                projectId,
                projTitle
            )

            if (
                _selectedProjectId.value ==
                projectId
            ) {

                _selectedProjectId.value =
                    null
            }
        }
    }

    fun duplicateProject(
        projectId: Long,
        newTitle: String
    ) {

        viewModelScope.launch {

            val newId =
                repository.duplicateProject(
                    projectId,
                    newTitle
                )

            if (newId > 0) {

                _selectedProjectId.value =
                    newId

                repository.getProject(newId)
                    .firstOrNull()
                    ?.let { duplicated ->

                        FirestoreSync
                            .syncProjectToFirestore(
                                duplicated
                            )
                    }
            }
        }
    }

    // ============================================================
    // PROJECT MATERIALS
    // ============================================================

    fun addMaterialToProject(
        material: PlumbingMaterial,
        size: String,
        quantity: Double,
        unit: String,
        notes: String = ""
    ) {

        val projId =
            _selectedProjectId.value
                ?: return

        viewModelScope.launch {

            val item =
                ProjectItem(
                    projectId =
                        projId,

                    materialKey =
                        material.key,

                    materialNameAr =
                        material.nameAr,

                    materialNameFr =
                        material.nameFr,

                    category =
                        material.category,

                    size =
                        size,

                    quantity =
                        quantity,

                    unit =
                        unit,

                    unitPrice =
                        material.defaultUnitPrice,

                    notes =
                        notes,

                    iconType =
                        material.iconType
                )

            repository.addOrUpdateProjectItem(
                item
            )

            logAuditAction(
                "إضافة مادة للمشروع: ${material.nameAr} (الكمية: $quantity $unit)",
                projId,
                currentProject.value?.title ?: ""
            )
        }
    }

    fun addCustomItemToProject(
        nameAr: String,
        nameFr: String = "",
        size: String,
        quantity: Double,
        unit: String,
        category: String,
        price: Double = 0.0,
        imageUri: String? = null,
        notes: String = ""
    ) {

        viewModelScope.launch {

            val customMat =
                CustomMaterial(
                    nameAr =
                        nameAr,

                    nameFr =
                        nameFr,

                    category =
                        category,

                    defaultSize =
                        size,

                    defaultUnit =
                        unit,

                    defaultPrice =
                        price,

                    imageUri =
                        imageUri,

                    notes =
                        notes
                )

            val customId =
                repository.addCustomMaterial(
                    customMat
                )

            val projId =
                _selectedProjectId.value

            if (
                projId != null &&
                quantity > 0
            ) {

                val item =
                    ProjectItem(
                        projectId =
                            projId,

                        materialKey =
                            "custom_$customId",

                        materialNameAr =
                            nameAr,

                        materialNameFr =
                            nameFr,

                        category =
                            category,

                        size =
                            size,

                        quantity =
                            quantity,

                        unit =
                            unit,

                        unitPrice =
                            price,

                        notes =
                            notes,

                        iconType =
                            "custom",

                        imageUri =
                            imageUri
                    )

                repository.addOrUpdateProjectItem(
                    item
                )

                logAuditAction(
                    "إضافة مادة مخصصة: $nameAr (الكمية: $quantity $unit)",
                    projId,
                    currentProject.value?.title ?: ""
                )
            }
        }
    }

    fun updateItemUnitPrice(
        item: ProjectItem,
        newUnitPrice: Double
    ) {

        if (_currentRole.value != "ADMIN") {
            return
        }

        if (newUnitPrice < 0) {
            return
        }

        viewModelScope.launch {

            val updated =
                item.copy(
                    unitPrice =
                        newUnitPrice
                )

            repository.addOrUpdateProjectItem(
                updated
            )

            logAuditAction(
                "تعديل سعر المادة ${item.materialNameAr} إلى $newUnitPrice د.ج",
                item.projectId,
                currentProject.value?.title ?: ""
            )
        }
    }

    fun deleteCustomMaterial(
        id: Long
    ) {

        viewModelScope.launch {

            repository.deleteCustomMaterial(
                id
            )
        }
    }

    fun updateItemQuantity(
        item: ProjectItem,
        newQty: Double
    ) {

        if (newQty <= 0) {

            deleteItem(
                item
            )

            return
        }

        viewModelScope.launch {

            repository.updateProjectItem(
                item.copy(
                    quantity =
                        newQty
                )
            )

            logAuditAction(
                "تحديث كمية المادة ${item.materialNameAr} إلى $newQty ${item.unit}",
                item.projectId,
                currentProject.value?.title ?: ""
            )
        }
    }

    fun incrementItemQuantity(
        item: ProjectItem,
        delta: Double = 1.0
    ) {

        if (delta <= 0) {
            return
        }

        updateItemQuantity(
            item,
            item.quantity + delta
        )
    }

    fun decrementItemQuantity(
        item: ProjectItem,
        delta: Double = 1.0
    ) {

        if (delta <= 0) {
            return
        }

        updateItemQuantity(
            item,
            item.quantity - delta
        )
    }

    fun deleteItem(
        item: ProjectItem
    ) {

        viewModelScope.launch {

            repository.deleteProjectItem(
                item.id,
                item.projectId
            )

            logAuditAction(
                "حذف مادة من المشروع: ${item.materialNameAr}",
                item.projectId,
                currentProject.value?.title ?: ""
            )
        }
    }

    fun toggleItemPurchased(
        item: ProjectItem
    ) {

        viewModelScope.launch {

            repository.setItemPurchased(
                item.id,
                !item.isPurchased
            )
        }
    }

    // ============================================================
    // AUTOMATIC MATERIAL ESTIMATION
    // ============================================================

    fun estimateMaterialsByWaterPoints(
        projectId: Long,
        bathroomsCount: Int,
        kitchensCount: Int,
        sinksCount: Int,
        showersCount: Int,
        toiletsCount: Int
    ) {

        viewModelScope.launch {

            val totalPoints =
                (bathroomsCount * 4) +
                        (kitchensCount * 3) +
                        sinksCount +
                        showersCount +
                        toiletsCount

            if (totalPoints <= 0) {
                return@launch
            }

            val coudesPprQty =
                totalPoints * 2.5

            val tesPprQty =
                totalPoints * 1.2

            val tubePprMeters =
                totalPoints * 4.5

            val coudesWallQty =
                totalPoints * 1.0

            val vanneQty =
                (
                    bathroomsCount +
                            kitchensCount +
                            1
                    ).toDouble()

            val tubePvc110Meters =
                (
                    bathroomsCount +
                            toiletsCount
                    ) * 4.0

            val tubePvc50Meters =
                (
                    sinksCount +
                            showersCount +
                            kitchensCount
                    ) * 3.0

            val teflonRolls =
                kotlin.math.max(
                    2.0,
                    totalPoints * 0.4
                )

            val estimates =
                listOf(
                    Triple(
                        "ppr_coude_90",
                        "25mm",
                        coudesPprQty
                    ),
                    Triple(
                        "ppr_te",
                        "25mm",
                        tesPprQty
                    ),
                    Triple(
                        "ppr_tube",
                        "25mm",
                        tubePprMeters
                    ),
                    Triple(
                        "ppr_coude_filete",
                        "25x1/2\"",
                        coudesWallQty
                    ),
                    Triple(
                        "ppr_vanne",
                        "25mm",
                        vanneQty
                    ),
                    Triple(
                        "pvc_tube",
                        "110mm",
                        tubePvc110Meters
                    ),
                    Triple(
                        "pvc_tube",
                        "50mm",
                        tubePvc50Meters
                    ),
                    Triple(
                        "teflon",
                        "قياسي (12mm x 12m)",
                        teflonRolls
                    )
                )

            estimates.forEach {
                    (matKey, size, qty) ->

                val matObj =
                    PlumbingLibraryData.ALL_MATERIALS
                        .find {
                            it.key == matKey
                        }

                if (
                    matObj != null &&
                    qty > 0
                ) {

                    val item =
                        ProjectItem(
                            projectId =
                                projectId,

                            materialKey =
                                matObj.key,

                            materialNameAr =
                                matObj.nameAr,

                            materialNameFr =
                                matObj.nameFr,

                            category =
                                matObj.category,

                            size =
                                size,

                            quantity =
                                kotlin.math.round(
                                    qty * 10.0
                                ) / 10.0,

                            unit =
                                matObj.defaultUnit,

                            notes =
                                "تقدير تلقائي حسب نقاط الماء ($totalPoints نقطة)",

                            iconType =
                                matObj.iconType
                        )

                    repository.addOrUpdateProjectItem(
                        item
                    )
                }
            }
        }
    }

    // ============================================================
    // PIPE CALCULATOR
    // ============================================================

    fun savePipeCalculationResult(
        projectId: Long,
        pipeCategory: String,
        pipeSize: String,
        totalPipesCount: Int,
        totalMetersWithWaste: Double,
        couplingsCount: Int,
        clampsCount: Int,
        calculationSummaryNotes: String
    ) {

        if (totalPipesCount <= 0) {
            return
        }

        viewModelScope.launch {

            val pipeMaterialKey =
                if (
                    pipeCategory.contains(
                        "PPR",
                        ignoreCase = true
                    )
                ) {
                    "ppr_tube"
                } else {
                    "pvc_tube"
                }

            val matObj =
                PlumbingLibraryData.ALL_MATERIALS
                    .find {
                        it.key ==
                                pipeMaterialKey
                    }

            val pipeNameAr =
                "أنبوب $pipeCategory ($totalPipesCount عود / أنبوب)"

            val pipeItem =
                ProjectItem(
                    projectId =
                        projectId,

                    materialKey =
                        matObj?.key
                            ?: "pipe_${System.currentTimeMillis()}",

                    materialNameAr =
                        pipeNameAr,

                    materialNameFr =
                        "Tube $pipeCategory",

                    category =
                        pipeCategory,

                    size =
                        pipeSize,

                    quantity =
                        totalPipesCount.toDouble(),

                    unit =
                        "قطعة",

                    notes =
                        "$calculationSummaryNotes - إجمالي الأمتار: ${
                            String.format(
                                "%.2f",
                                totalMetersWithWaste
                            )
                        } م",

                    iconType =
                        "pipe"
                )

            repository.addOrUpdateProjectItem(
                pipeItem
            )

            if (couplingsCount > 0) {

                val couplingItem =
                    ProjectItem(
                        projectId =
                            projectId,

                        materialKey =
                            "coupling_${System.currentTimeMillis()}",

                        materialNameAr =
                            "وصلة مستقيمة (مانشون $pipeCategory)",

                        materialNameFr =
                            "Manchon $pipeCategory",

                        category =
                            pipeCategory,

                        size =
                            pipeSize,

                        quantity =
                            couplingsCount.toDouble(),

                        unit =
                            "قطعة",

                        notes =
                            "مصلحة ربط الوصلات حسب طول الأنابيب",

                        iconType =
                            "fitting"
                    )

                repository.addOrUpdateProjectItem(
                    couplingItem
                )
            }

            if (clampsCount > 0) {

                val clampItem =
                    ProjectItem(
                        projectId =
                            projectId,

                        materialKey =
                            "clamp_${System.currentTimeMillis()}",

                        materialNameAr =
                            "قفيز / مشابك تثبيت جداري",

                        materialNameFr =
                            "Colliers de fixation",

                        category =
                            "Accessoires",

                        size =
                            pipeSize,

                        quantity =
                            clampsCount.toDouble(),

                        unit =
                            "قطعة",

                        notes =
                            "محسوبة للتثبيت على مسافات متساوية",

                        iconType =
                            "fitting"
                    )

                repository.addOrUpdateProjectItem(
                    clampItem
                )
            }
        }
    }
}
