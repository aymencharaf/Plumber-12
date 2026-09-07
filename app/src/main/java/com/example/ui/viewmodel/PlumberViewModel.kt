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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlumberViewModel(
    application: Application
) : AndroidViewModel(application) {

    // ============================================================
    // REPOSITORIES
    // ============================================================

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
    // MATERIAL LIBRARY
    // ============================================================

    val libraryMaterials: StateFlow<List<MaterialEntity>>

    // ============================================================
    // CURRENT USER
    // ============================================================

    private val _currentUser =
        MutableStateFlow<TeamUser?>(null)

    val currentUser:
            StateFlow<TeamUser?> =
        _currentUser.asStateFlow()

    // ============================================================
    // CURRENT ROLE
    // ============================================================

    private val _currentRole =
        MutableStateFlow("WORKER")

    val currentRole:
            StateFlow<String> =
        _currentRole.asStateFlow()

    // ============================================================
    // LOGIN STATE
    // ============================================================

    private val _isUserLoggedIn =
        MutableStateFlow(false)

    val isUserLoggedIn:
            StateFlow<Boolean> =
        _isUserLoggedIn.asStateFlow()

    // ============================================================
    // JOIN REQUESTS
    // ============================================================

    private val _pendingJoinRequests =
        MutableStateFlow<List<Map<String, Any>>>(
            emptyList()
        )

    val pendingJoinRequests:
            StateFlow<List<Map<String, Any>>> =
        _pendingJoinRequests.asStateFlow()

    private var pendingRequestsListener:
            com.google.firebase.firestore.ListenerRegistration? =
        null

    // ============================================================
    // TEAM USERS
    // ============================================================

    val allTeamUsers:
            StateFlow<List<TeamUser>>

    val allWorkers:
            StateFlow<List<TeamUser>>

    val allAuditLogs:
            StateFlow<List<AuditLog>>

    val calculatedMaterialCaches:
            StateFlow<List<CalculatedMaterialCache>>

    // ============================================================
    // STORE SETTINGS
    // ============================================================

    private val _storeName =
        MutableStateFlow(
            teamStorePrefs.storeName
        )

    val storeName:
            StateFlow<String> =
        _storeName.asStateFlow()

    private val _storePhone =
        MutableStateFlow(
            teamStorePrefs.storePhone
        )

    val storePhone:
            StateFlow<String> =
        _storePhone.asStateFlow()

    private val _storeWhatsapp =
        MutableStateFlow(
            teamStorePrefs.storeWhatsapp
        )

    val storeWhatsapp:
            StateFlow<String> =
        _storeWhatsapp.asStateFlow()

    private val _activeWorker =
        MutableStateFlow(
            teamStorePrefs.activeWorker
        )

    val activeWorker:
            StateFlow<String> =
        _activeWorker.asStateFlow()

    private val _managerName =
        MutableStateFlow(
            teamStorePrefs.managerName
        )

    val managerName:
            StateFlow<String> =
        _managerName.asStateFlow()

    private val _isManagerLoggedIn =
        MutableStateFlow(false)

    val isManagerLoggedIn:
            StateFlow<Boolean> =
        _isManagerLoggedIn.asStateFlow()

    private val _managerPin =
        MutableStateFlow(
            teamStorePrefs.managerPin
        )

    val managerPin:
            StateFlow<String> =
        _managerPin.asStateFlow()

    private val _workersList =
        MutableStateFlow(
            teamStorePrefs.getWorkersList()
        )

    val workersList:
            StateFlow<List<String>> =
        _workersList.asStateFlow()

    // ============================================================
    // PROJECT SELECTION
    // ============================================================

    private val _selectedProjectId =
        MutableStateFlow<Long?>(null)

    val selectedProjectId:
            StateFlow<Long?> =
        _selectedProjectId.asStateFlow()

    // ============================================================
    // INIT
    // ============================================================

    init {

        // --------------------------------------------------------
        // Local databases
        // --------------------------------------------------------

        val db =
            PlumberDatabase.getDatabase(
                application
            )

        val matDb =
            MaterialDatabase.getDatabase(
                application
            )

        // --------------------------------------------------------
        // Repository
        // --------------------------------------------------------

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

        // --------------------------------------------------------
        // Team users
        // --------------------------------------------------------

        allTeamUsers =
            repository.allTeamUsers.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

        allWorkers =
            repository.allWorkers.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

        // --------------------------------------------------------
        // Audit logs
        // --------------------------------------------------------

        allAuditLogs =
            repository.allAuditLogs.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

        // --------------------------------------------------------
        // Calculation cache
        // --------------------------------------------------------

        calculatedMaterialCaches =
            repository.calculatedMaterialCaches.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

        // --------------------------------------------------------
        // Library materials
        // --------------------------------------------------------

        libraryMaterials =
            materialRepository.allMaterials.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

        // --------------------------------------------------------
        // Initialize local library
        // --------------------------------------------------------

        viewModelScope.launch {

            try {

                materialRepository
                    .initializeLibraryIfEmpty(
                        application
                    )

            } catch (e: Exception) {

                Log.e(
                    "PlumberViewModel",
                    "Failed to initialize material library",
                    e
                )
            }
        }

        // --------------------------------------------------------
        // Restore Firebase session
        // --------------------------------------------------------

        restoreLoginSession()
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
                    inputValuesSummary =
                        inputValuesSummary,
                    estimatedPipesCount =
                        estimatedPipesCount,
                    estimatedFittingsCount =
                        estimatedFittingsCount,
                    estimatedGlueOrSolder =
                        estimatedGlueOrSolder,
                    generatedMaterialsJson =
                        generatedMaterialsJson
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

            repository.deleteCalculatedMaterialCache(
                id
            )
        }
    }

    // ============================================================
    // FIREBASE HELPERS
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
            localUser
                ?.email
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

        return "${
            cleanInput.replace(
                " ",
                ""
            )
        }@plumber.com"
    }

    private fun getFirebaseErrorMessage(
        exception: Exception
    ): String {

        val errorCode =
            (
                exception as?
                    FirebaseAuthException
                )
                    ?.errorCode
                    ?.uppercase()
                    ?: ""

        return when {

            errorCode.contains(
                "USER_NOT_FOUND"
            ) ->
                "لا يوجد حساب بهذا البريد الإلكتروني أو رقم الهاتف."

            errorCode.contains(
                "WRONG_PASSWORD"
            ) ->
                "كلمة المرور غير صحيحة."

            errorCode.contains(
                "INVALID_CREDENTIAL"
            ) ->
                "البريد الإلكتروني أو كلمة المرور غير صحيحة."

            errorCode.contains(
                "INVALID_EMAIL"
            ) ->
                "البريد الإلكتروني غير صالح."

            errorCode.contains(
                "EMAIL_ALREADY_IN_USE"
            ) ->
                "هذا البريد الإلكتروني مستخدم بالفعل."

            errorCode.contains(
                "WEAK_PASSWORD"
            ) ->
                "كلمة المرور ضعيفة. يجب أن تحتوي على 6 أحرف أو أرقام على الأقل."

            errorCode.contains(
                "NETWORK"
            ) ->
                "تعذر الاتصال بـ Firebase. تحقق من اتصال الإنترنت."

            errorCode.contains(
                "TOO_MANY_REQUESTS"
            ) ->
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

    private fun restoreLoginSession() {

        viewModelScope.launch {

            try {

                // ------------------------------------------------
                // Firebase session
                // ------------------------------------------------

                val firebaseUser =
                    firebaseAuth.currentUser

                if (firebaseUser == null) {

                    Log.d(
                        "PlumberViewModel",
                        "No Firebase session found."
                    )

                    _currentUser.value =
                        null

                    _currentRole.value =
                        "WORKER"

                    _isUserLoggedIn.value =
                        false

                    _isManagerLoggedIn.value =
                        false

                    FirestoreSync
                        .stopRealtimeListener()

                    authPrefs
                        .edit()
                        .remove("logged_uid")
                        .apply()

                    return@launch
                }

                val firebaseUid =
                    firebaseUser.uid

                Log.d(
                    "PlumberViewModel",
                    "Restoring Firebase session UID=$firebaseUid"
                )

                // ------------------------------------------------
                // IMPORTANT:
                // اقرأ users/{UID} مباشرة.
                // لا تستخدم phone/email هنا.
                // ------------------------------------------------

                FirestoreSync
                    .fetchUserFromFirestoreByUid(
                        firebaseUid
                    ) { fetchedUser ->

                        viewModelScope.launch {

                            // ------------------------------------
                            // Firestore profile missing
                            // ------------------------------------

                            if (fetchedUser == null) {

                                Log.w(
                                    "PlumberViewModel",
                                    "Firebase account exists but Firestore profile is missing."
                                )

                                firebaseAuth.signOut()

                                FirestoreSync
                                    .stopRealtimeListener()

                                _currentUser.value =
                                    null

                                _currentRole.value =
                                    "WORKER"

                                _isUserLoggedIn.value =
                                    false

                                _isManagerLoggedIn.value =
                                    false

                                authPrefs
                                    .edit()
                                    .remove("logged_uid")
                                    .apply()

                                return@launch
                            }

                            // ------------------------------------
                            // UID verification
                            // ------------------------------------

                            if (
                                fetchedUser.uid !=
                                firebaseUid
                            ) {

                                Log.e(
                                    "PlumberViewModel",
                                    "UID mismatch while restoring Firebase session."
                                )

                                firebaseAuth.signOut()

                                FirestoreSync
                                    .stopRealtimeListener()

                                _currentUser.value =
                                    null

                                _currentRole.value =
                                    "WORKER"

                                _isUserLoggedIn.value =
                                    false

                                _isManagerLoggedIn.value =
                                    false

                                authPrefs
                                    .edit()
                                    .remove("logged_uid")
                                    .apply()

                                return@launch
                            }

                            // ------------------------------------
                            // Role verification
                            // ------------------------------------

                            val normalizedRole =
                                fetchedUser.role
                                    .trim()
                                    .uppercase()

                            if (
                                normalizedRole != "ADMIN" &&
                                normalizedRole != "MANAGER" &&
                                normalizedRole != "WORKER"
                            ) {

                                Log.e(
                                    "PlumberViewModel",
                                    "Invalid Firestore role: ${fetchedUser.role}"
                                )

                                firebaseAuth.signOut()

                                FirestoreSync
                                    .stopRealtimeListener()

                                _currentUser.value =
                                    null

                                _currentRole.value =
                                    "WORKER"

                                _isUserLoggedIn.value =
                                    false

                                _isManagerLoggedIn.value =
                                    false

                                return@launch
                            }

                            // ------------------------------------
                            // Active verification
                            // ------------------------------------

                            if (!fetchedUser.active) {

                                Log.w(
                                    "PlumberViewModel",
                                    "Firebase user is inactive."
                                )

                                firebaseAuth.signOut()

                                FirestoreSync
                                    .stopRealtimeListener()

                                _currentUser.value =
                                    null

                                _currentRole.value =
                                    "WORKER"

                                _isUserLoggedIn.value =
                                    false

                                _isManagerLoggedIn.value =
                                    false

                                authPrefs
                                    .edit()
                                    .remove("logged_uid")
                                    .apply()

                                return@launch
                            }

                            // ------------------------------------
                            // Correct user
                            // ------------------------------------

                            val correctedUser =
                                fetchedUser.copy(
                                    uid = firebaseUid,
                                    role = normalizedRole,
                                    password = ""
                                )

                            repository
                                .insertOrUpdateTeamUser(
                                    correctedUser
                                )

                            // ------------------------------------
                            // Restore session
                            // ------------------------------------

                            restoreUserIntoSession(
                                correctedUser
                            )
                        }
                    }

            } catch (e: Exception) {

                Log.e(
                    "PlumberViewModel",
                    "Failed to restore Firebase session.",
                    e
                )

                firebaseAuth.signOut()

                FirestoreSync
                    .stopRealtimeListener()

                _currentUser.value =
                    null

                _currentRole.value =
                    "WORKER"

                _isUserLoggedIn.value =
                    false

                _isManagerLoggedIn.value =
                    false

                authPrefs
                    .edit()
                    .remove("logged_uid")
                    .apply()
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

        val correctedUser =
            user.copy(
                uid = user.uid,
                role = normalizedRole,
                password = ""
            )

        _currentUser.value =
            correctedUser

        _currentRole.value =
            normalizedRole

        _isUserLoggedIn.value =
            true

        _isManagerLoggedIn.value =
            normalizedRole == "ADMIN"

        authPrefs
            .edit()
            .putString(
                "logged_uid",
                correctedUser.uid
            )
            .apply()

        // --------------------------------------------------------
        // Start realtime sync only when workshop exists
        // --------------------------------------------------------

        val workshopId =
            correctedUser.workshopId
                .trim()

        if (workshopId.isNotBlank()) {

            FirestoreSync.startRealtimeListener(
                context = getApplication(),
                userRole = normalizedRole,
                workerUid = correctedUser.uid,
                workshopId = workshopId
            )

            Log.d(
                "PlumberViewModel",
                "Realtime sync started. workshopId=$workshopId"
            )

        } else {

            FirestoreSync
                .stopRealtimeListener()

            Log.w(
                "PlumberViewModel",
                "User restored without workshopId."
            )
        }
    }

    // ============================================================
    // LOGIN
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

                val localUser =
                    repository.getUserByPhoneOrEmail(
                        cleanInput
                    )

                val firebaseEmail =
                    getFirebaseEmail(
                        cleanInput,
                        localUser
                    )

                firebaseAuth
                    .signInWithEmailAndPassword(
                        firebaseEmail,
                        cleanPass
                    )
                    .addOnSuccessListener { authResult ->

                        val firebaseUser =
                            authResult.user

                        if (firebaseUser == null) {

                            onResult(
                                false,
                                "تعذر الحصول على بيانات حساب Firebase."
                            )

                            return@addOnSuccessListener
                        }

                        val firebaseUid =
                            firebaseUser.uid

                        // ------------------------------------------------
                        // IMPORTANT:
                        // Firebase UID is the security identity.
                        // ------------------------------------------------

                        FirestoreSync
                            .fetchUserFromFirestoreByUid(
                                firebaseUid
                            ) { fetchedUser ->

                                viewModelScope.launch {

                                    if (fetchedUser == null) {

                                        firebaseAuth.signOut()

                                        onResult(
                                            false,
                                            "حساب Firebase موجود، لكن بيانات المستخدم غير موجودة في Firestore."
                                        )

                                        return@launch
                                    }

                                    if (
                                        fetchedUser.uid !=
                                        firebaseUid
                                    ) {

                                        firebaseAuth.signOut()

                                        onResult(
                                            false,
                                            "خطأ أمني: UID في Firestore لا يطابق UID في Firebase Authentication."
                                        )

                                        return@launch
                                    }

                                    if (
                                        !fetchedUser.active
                                    ) {

                                        firebaseAuth.signOut()

                                        onResult(
                                            false,
                                            "هذا الحساب معطل حالياً من قبل الإدارة."
                                        )

                                        return@launch
                                    }

                                    val normalizedRole =
                                        fetchedUser.role
                                            .trim()
                                            .uppercase()

                                    if (
                                        normalizedRole !=
                                        "ADMIN" &&
                                        normalizedRole !=
                                        "MANAGER" &&
                                        normalizedRole !=
                                        "WORKER"
                                    ) {

                                        firebaseAuth.signOut()

                                        onResult(
                                            false,
                                            "دور المستخدم في Firestore غير صالح."
                                        )

                                        return@launch
                                    }

                                    val correctedUser =
                                        fetchedUser.copy(
                                            uid =
                                                firebaseUid,
                                            role =
                                                normalizedRole,
                                            password = "",
                                            lastLoginAt =
                                                System.currentTimeMillis()
                                        )

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
    // PROCESS LOGIN
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

        if (
            currentFirebaseUser.uid !=
            user.uid
        ) {

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

        val updatedUser =
            user.copy(
                password = "",
                lastLoginAt =
                    System.currentTimeMillis()
            )

        repository
            .insertOrUpdateTeamUser(
                updatedUser
            )

        _currentUser.value =
            updatedUser

        _currentRole.value =
            updatedUser.role.uppercase()

        _isUserLoggedIn.value =
            true

        _isManagerLoggedIn.value =
            updatedUser.role.uppercase() ==
                    "ADMIN"

        authPrefs
            .edit()
            .putString(
                "logged_uid",
                updatedUser.uid
            )
            .apply()

        val workshopId =
            updatedUser.workshopId
                .trim()

        if (workshopId.isNotBlank()) {

            FirestoreSync.startRealtimeListener(
                context = getApplication(),
                userRole = updatedUser.role,
                workerUid = updatedUser.uid,
                workshopId = workshopId
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
    // SELF REGISTRATION
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
            phone
                .trim()
                .replace(" ", "")

        val cleanEmailInput =
            email.trim()

        val cleanPassword =
            pass.trim()

        // Self registration = WORKER only
        val cleanRole =
            "WORKER"

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

                                val newUser =
                                    TeamUser(
                                        uid =
                                            firebaseUser.uid,

                                        name =
                                            cleanName,

                                        phone =
                                            cleanPhone,

                                        email =
                                            firebaseEmail,

                                        password = "",

                                        role =
                                            cleanRole,

                                        active = true,

                                        workshopId = "",

                                        createdAt =
                                            System.currentTimeMillis(),

                                        lastLoginAt =
                                            System.currentTimeMillis()
                                    )

                                repository
                                    .insertOrUpdateTeamUser(
                                        newUser
                                    )

                                FirestoreSync
                                    .syncUserToFirestore(
                                        newUser
                                    )

                                if (!autoLogin) {

                                    firebaseAuth.signOut()

                                } else {

                                    restoreUserIntoSession(
                                        newUser
                                    )
                                }

                                val updatedList =
                                    _workersList
                                        .value
                                        .toMutableList()

                                val entry =
                                    "${newUser.name} (${newUser.phone})"

                                if (
                                    !updatedList.contains(
                                        entry
                                    )
                                ) {

                                    updatedList.add(
                                        entry
                                    )

                                    updateWorkersList(
                                        updatedList
                                    )
                                }

                                if (autoLogin) {

                                    logAuditAction(
                                        "تسجيل حساب عامل جديد في Firebase: ${newUser.name}",
                                        0,
                                        ""
                                    )
                                }

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
                                    }"
                                )
                            }
                        }
                    }
                    .addOnFailureListener { e ->

                        onResult(
                            false,
                            getFirebaseErrorMessage(e)
                        )
                    }

            } catch (e: Exception) {

                onResult(
                    false,
                    "حدث خطأ أثناء إنشاء الحساب: ${
                        e.localizedMessage
                    }"
                )
            }
        }
    }

    // ============================================================
    // SECONDARY FIREBASE AUTH
    // ============================================================

    private fun getSecondaryFirebaseAuth():
            FirebaseAuth? {

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
    // ADMIN CREATE WORKER
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
            _currentRole.value != "ADMIN" ||
            firebaseAuth.currentUser == null
        ) {

            onResult(
                false,
                "عذراً! يجب أن تكون مسجلاً بحساب مدير لإضافة عامل."
            )

            return
        }

        val currentAdmin =
            _currentUser.value

        if (currentAdmin == null) {

            onResult(
                false,
                "بيانات حساب المدير غير متوفرة."
            )

            return
        }

        val cleanName =
            name.trim()

        val cleanPhone =
            phone
                .trim()
                .replace(" ", "")

        val cleanEmailInput =
            email.trim()

        val cleanPassword =
            pass.trim()

        val normalizedRole =
            "WORKER"

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

        viewModelScope.launch {

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

                            /*
                             * مهم:
                             * العامل الذي ينشئه المدير يرتبط مباشرة
                             * بورشة المدير الحالية.
                             */
                            val workshopId =
                                currentAdmin.workshopId
                                    .trim()

                            if (workshopId.isBlank()) {

                                secondaryAuth.signOut()

                                onResult(
                                    false,
                                    "لا يمكن إنشاء العامل لأن حساب المدير غير مرتبط بورشة."
                                )

                                return@launch
                            }

                            val newUser =
                                TeamUser(
                                    uid =
                                        workerUid,

                                    name =
                                        cleanName,

                                    phone =
                                        cleanPhone,

                                    email =
                                        firebaseEmail,

                                    password = "",

                                    role =
                                        normalizedRole,

                                    active = true,

                                    workshopId =
                                        workshopId,

                                    createdAt =
                                        System.currentTimeMillis(),

                                    lastLoginAt = 0L
                                )

                            repository
                                .insertOrUpdateTeamUser(
                                    newUser
                                )

                            FirestoreSync
                                .createUserProfileByAdmin(
                                    newUser
                                ) { success, message ->

                                    if (!success) {

                                        Log.e(
                                            "PlumberViewModel",
                                            "Worker profile creation failed: $message"
                                        )

                                        secondaryAuth.signOut()

                                        onResult(
                                            false,
                                            "تم إنشاء حساب Firebase لكن تعذر إنشاء ملف العامل في Firestore: $message"
                                        )

                                        return@createUserProfileByAdmin
                                    }

                                    val updatedList =
                                        _workersList
                                            .value
                                            .toMutableList()

                                    val entry =
                                        "${newUser.name} (${newUser.phone})"

                                    if (
                                        !updatedList.contains(
                                            entry
                                        )
                                    ) {

                                        updatedList.add(
                                            entry
                                        )

                                        updateWorkersList(
                                            updatedList
                                        )
                                    }

                                    secondaryAuth.signOut()

                                    logAuditAction(
                                        "إنشاء حساب Firebase للعامل: ${newUser.name}",
                                        0,
                                        ""
                                    )

                                    onResult(
                                        true,
                                        "تم إنشاء حساب العامل ${newUser.name} وربطه بالورشة بنجاح! 🟢"
                                    )
                                }

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
                                }"
                            )
                        }
                    }
                }
                .addOnFailureListener { e ->

                    secondaryAuth.signOut()

                    onResult(
                        false,
                        getFirebaseErrorMessage(e)
                    )
                }
        }
    }

    // ============================================================
    // LOGOUT
    // ============================================================

    fun logoutTeamUser() {

        val user =
            _currentUser.value

        if (user != null) {

            logAuditAction(
                "تسجيل خروج من النظام",
                0,
                ""
            )
        }

        FirestoreSync
            .stopRealtimeListener()

        pendingRequestsListener
            ?.remove()

        pendingRequestsListener =
            null

        firebaseAuth.signOut()

        _currentUser.value =
            null

        _currentRole.value =
            "WORKER"

        _isUserLoggedIn.value =
            false

        _isManagerLoggedIn.value =
            false

        _pendingJoinRequests.value =
            emptyList()

        authPrefs
            .edit()
            .remove("logged_uid")
            .apply()
    }

    // ============================================================
    // JOIN REQUESTS
    // ============================================================

    fun startPendingRequestsListener(
        workshopId: String
    ) {

        pendingRequestsListener
            ?.remove()

        pendingRequestsListener =
            null

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

        if (
            firebaseAuth.currentUser?.uid !=
            user.uid
        ) {

            onResult(
                false,
                "جلسة Firebase غير صالحة."
            )

            return
        }

        viewModelScope.launch {

            FirestoreSync.sendWorkerJoinRequest(
                user,
                syncCode
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
                "الإدارة فقط تستطيع الموافقة على طلبات الانضمام."
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
                                it["requestId"] ==
                                    requestId
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
                "الإدارة فقط تستطيع رفض الطلبات."
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
                                it["requestId"] ==
                                    requestId
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
    // WORKER ACTIVE
    // ============================================================

    fun toggleWorkerActive(
        uid: String,
        active: Boolean
    ) {

        if (_currentRole.value != "ADMIN") {
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

            FirestoreSync
                .updateUserProfileByAdmin(
                    updated
                ) { success, message ->

                    if (!success) {

                        Log.e(
                            "PlumberViewModel",
                            "Failed to update worker active state: $message"
                        )
                    }
                }

            logAuditAction(
                "تغيير حالة حساب العامل ${user.name} إلى: ${
                    if (active) "مفعّل"
                    else "معطّل"
                }",
                0,
                ""
            )

            if (
                !active &&
                firebaseAuth.currentUser?.uid ==
                uid
            ) {

                logoutTeamUser()
            }
        }
    }

    // ============================================================
    // PASSWORD RESET
    // ============================================================

    fun resetWorkerPassword(
        uid: String,
        newPass: String,
        onDone: (Boolean) -> Unit
    ) {

        if (_currentRole.value != "ADMIN") {

            onDone(false)

            return
        }

        if (newPass.trim().length < 6) {

            onDone(false)

            return
        }

        Log.w(
            "PlumberViewModel",
            "Password reset requires Firebase Admin SDK. UID=$uid"
        )

        onDone(false)
    }

    // ============================================================
    // DELETE WORKER
    // ============================================================

    fun deleteWorkerAccount(
        uid: String
    ) {

        if (_currentRole.value != "ADMIN") {
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
                    "حذف حساب العامل: ${user.name}",
                    0,
                    ""
                )
            }
        }
    }

    // ============================================================
    // AUDIT
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
                log,
                worker?.workshopId ?: ""
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
                        },

                    workshopId =
                        _currentUser.value
                            ?.workshopId
                            ?: ""
                )

            repository
                .addOrUpdateProjectItem(
                    item
                )

            syncProjectItem(
                item
            )
        }
    }

    // ============================================================
    // STORE SETTINGS
    // ============================================================

    fun setManagerLoggedIn(
        loggedIn: Boolean
    ) {

        if (!loggedIn) {

            _isManagerLoggedIn.value =
                false

            return
        }

        if (
            firebaseAuth.currentUser != null &&
            _currentRole.value == "ADMIN"
        ) {

            _isManagerLoggedIn.value =
                true
        }
    }

    fun loginManager(
        pin: String
    ): Boolean {

        if (
            firebaseAuth.currentUser == null ||
            _currentRole.value != "ADMIN"
        ) {

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
            firebaseAuth.currentUser == null ||
            _currentRole.value != "ADMIN"
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

        val workshopId =
            _currentUser.value
                ?.workshopId
                ?: ""

        FirestoreSync.syncStoreSettingsToFirestore(
            storeName =
                storeNameVal,

            storePhone =
                storePhoneVal,

            storeWhatsapp =
                storeWhatsappVal,

            managerName =
                managerNameVal,

            workshopId =
                workshopId
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
    // PROJECTS
    // ============================================================

    val allProjects:
            StateFlow<List<Project>> =
        repository.allProjects.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // ============================================================
    // VISIBLE PROJECTS
    // ============================================================

    val visibleProjects:
            StateFlow<List<Project>> =
        combine(
            allProjects,
            currentUser,
            currentRole
        ) { projects, user, role ->

            if (user == null) {

                emptyList()

            } else {

                val normalizedRole =
                    role.trim().uppercase()

                val workshopId =
                    user.workshopId.trim()

                if (
                    normalizedRole == "ADMIN" ||
                    normalizedRole == "MANAGER"
                ) {

                    /*
                     * الإدارة ترى مشاريع ورشتها فقط.
                     *
                     * المشروع القديم الذي لا يحتوي workshopId
                     * يسمح له بالظهور حتى تتم عملية ترحيله.
                     */
                    projects.filter { project ->

                        project.workshopId.isBlank() ||
                                project.workshopId ==
                                workshopId
                    }

                } else {

                    /*
                     * العامل يرى:
                     *
                     * 1. مشاريع ورشته.
                     * 2. المشاريع المسندة إلى UID الخاص به.
                     * 3. المشاريع التي أنشأها بنفسه.
                     *
                     * لا نعتمد على phone/name.
                     */

                    projects.filter { project ->

                        if (
                            project.workshopId !=
                            workshopId
                        ) {

                            false

                        } else {

                            val assigned =
                                project.assignedWorkers
                                    .split(",")
                                    .map {
                                        it.trim()
                                    }
                                    .filter {
                                        it.isNotBlank()
                                    }

                            project.createdBy ==
                                    user.uid ||
                                    assigned.contains(
                                        user.uid
                                    )
                        }
                    }
                }

            }

        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // ============================================================
    // PROJECT MATERIALS
    // ============================================================

    val customMaterials:
            StateFlow<List<CustomMaterial>> =
        repository.customMaterials.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val allAppointments:
            StateFlow<List<Appointment>> =
        repository.allAppointments.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val allWorkAlerts:
            StateFlow<List<WorkAlert>> =
        repository.allWorkAlerts.stateIn(
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
                    alertType =
                        alertType,

                    senderName =
                        senderName,

                    recipientRole =
                        recipientRole,

                    title =
                        title,

                    details =
                        details,

                    projectName =
                        projectName
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
                    status =
                        newStatus
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
                    serviceType =
                        serviceType,

                    customerName =
                        customerName,

                    phoneNumber =
                        phoneNumber,

                    address =
                        address,

                    scheduledTime =
                        scheduledTime,

                    notes =
                        notes
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
                    status =
                        newStatus
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentProject:
            StateFlow<Project?> =
        _selectedProjectId
            .flatMapLatest { id ->

                if (id != null) {

                    repository.getProject(
                        id
                    )

                } else {

                    flowOf(null)
                }

            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )

    // ============================================================
    // CURRENT PROJECT ITEMS
    // ============================================================

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentProjectItems:
            StateFlow<List<ProjectItem>> =
        _selectedProjectId
            .flatMapLatest { id ->

                if (id != null) {

                    repository.getProjectItems(
                        id
                    )

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

            val currentUser =
                _currentUser.value
                    ?: return@launch

            val isAdmin =
                _currentRole.value
                    .trim()
                    .uppercase() == "ADMIN"

            val workTypeObj =
                PlumbingLibraryData.WORK_TYPES
                    .find {
                        it.key == workTypeKey
                    }

            val workTypeName =
                workTypeObj
                    ?.titleAr
                    ?: "عمل مخصص"

            // ----------------------------------------------------
            // Resolve worker UID
            // ----------------------------------------------------

            val assignedWorkerUid =
                if (isAdmin) {

                    if (
                        workerName.isBlank()
                    ) {

                        ""

                    } else {

                        val currentWorkshop =
                            currentUser.workshopId

                        allWorkers.value
                            .firstOrNull { worker ->

                                worker.workshopId ==
                                        currentWorkshop &&
                                        (
                                            worker.name ==
                                                    workerName ||
                                                    worker.phone ==
                                                    workerName ||
                                                    "${worker.name} (${worker.phone})" ==
                                                    workerName
                                            )
                            }
                            ?.uid
                            .orEmpty()
                    }

                } else {

                    currentUser.uid
                }

            // ----------------------------------------------------
            // Create project
            // ----------------------------------------------------

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
                        workerName.ifBlank {
                            _activeWorker.value
                        },

                    managerName =
                        managerName.ifBlank {
                            _managerName.value
                        },

                    storePhone =
                        storePhone.ifBlank {
                            _storePhone.value
                        },

                    assignedWorkers =
                        assignedWorkerUid,

                    createdBy =
                        currentUser.uid,

                    laborCost =
                        if (isAdmin) {
                            laborCost
                        } else {
                            0.0
                        },

                    paidAmount =
                        if (isAdmin) {
                            paidAmount
                        } else {
                            0.0
                        },

                    workshopId =
                        currentUser.workshopId
                )

            val newId =
                repository.createProject(
                    newProj
                )

            val createdObj =
                newProj.copy(
                    id =
                        newId,

                    updatedAt =
                        System.currentTimeMillis()
                )

            syncProject(
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
    // SYNC PROJECT
    // ============================================================

    private fun syncProject(
        project: Project
    ) {

        FirestoreSync.syncProjectToFirestore(
            project = project,
            workshopId = project.workshopId
        )
    }

    // ============================================================
    // ITEM OWNER
    // ============================================================

    private fun itemWorkerId(
        item: ProjectItem
    ): String {

        val currentUser =
            _currentUser.value
                ?: return ""

        // العامل = نفسه
        if (
            _currentRole.value
                .trim()
                .uppercase() != "ADMIN"
        ) {

            return currentUser.uid
        }

        /*
         * المدير لا يصبح مالك المادة.
         *
         * نبحث عن العامل المسند إلى المشروع.
         */
        val project =
            currentProject.value
                ?.takeIf {
                    it.id == item.projectId
                }

        val assignedWorkerUid =
            project
                ?.assignedWorkers
                ?.split(",")
                ?.map {
                    it.trim()
                }
                ?.firstOrNull {
                    it.isNotBlank()
                }
                .orEmpty()

        return assignedWorkerUid.ifBlank {
            currentUser.uid
        }
    }

    // ============================================================
    // SYNC PROJECT ITEM
    // ============================================================

    private fun syncProjectItem(
        item: ProjectItem,
        workerId: String = ""
    ) {

        val currentUser =
            _currentUser.value

        val resolvedWorkerId =
            workerId.ifBlank {

                if (
                    _currentRole.value
                        .trim()
                        .uppercase() == "ADMIN"
                ) {

                    itemWorkerId(
                        item
                    )

                } else {

                    currentUser
                        ?.uid
                        .orEmpty()
                }
            }

        val resolvedWorkerName =
            if (
                resolvedWorkerId.isNotBlank()
            ) {

                allWorkers.value
                    .firstOrNull {
                        it.uid ==
                                resolvedWorkerId
                    }
                    ?.name
                    ?: if (
                        resolvedWorkerId ==
                        currentUser?.uid
                    ) {
                        currentUser.name
                    } else {
                        ""
                    }

            } else {

                currentUser
                    ?.name
                    .orEmpty()
            }

        val workshopId =
            item.workshopId
                .ifBlank {
                    currentUser
                        ?.workshopId
                        .orEmpty()
                }

        FirestoreSync
            .syncProjectItemToFirestore(

                item =
                    item,

                workerId =
                    resolvedWorkerId,

                workerName =
                    resolvedWorkerName,

                workshopId =
                    workshopId
            )
    }

    // ============================================================
    // WORKER PAYMENT
    // ============================================================

    fun addWorkerPayment(
        projectId: Long,
        paymentAmount: Double
    ) {

        if (_currentRole.value != "ADMIN") {
            return
        }

        viewModelScope.launch {

            val proj =
                repository
                    .getProject(projectId)
                    .firstOrNull()

            if (
                proj != null &&
                paymentAmount > 0
            ) {

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

                syncProject(
                    updated
                )
            }
        }
    }

    // ============================================================
    // PROJECT ASSIGNMENT + FINANCIALS
    // ============================================================

    fun updateProjectAssignmentAndFinancials(
        projectId: Long,
        workerName: String,
        managerName: String,
        laborCost: Double,
        paidAmount: Double,
        workerUid: String = ""
    ) {

        if (_currentRole.value != "ADMIN") {
            return
        }

        viewModelScope.launch {

            val proj =
                repository
                    .getProject(projectId)
                    .firstOrNull()
                    ?: return@launch

            val currentUser =
                _currentUser.value
                    ?: return@launch

            // ----------------------------------------------------
            // Resolve worker
            // ----------------------------------------------------

            val resolvedWorkerUid =
                when {

                    workerUid.isNotBlank() ->
                        workerUid.trim()

                    workerName.isBlank() ->
                        ""

                    else -> {

                        allWorkers.value
                            .firstOrNull { worker ->

                                worker.workshopId ==
                                        currentUser.workshopId &&
                                        (
                                            worker.name ==
                                                    workerName ||
                                                    worker.phone ==
                                                    workerName ||
                                                    "${worker.name} (${worker.phone})" ==
                                                    workerName
                                            )
                            }
                            ?.uid
                            .orEmpty()
                    }
                }

            // ----------------------------------------------------
            // Preserve old assignment if name cannot be resolved
            // ----------------------------------------------------

            val finalAssignedWorkers =
                when {

                    workerName.isBlank() ->
                        ""

                    resolvedWorkerUid.isNotBlank() ->
                        resolvedWorkerUid

                    else ->
                        proj.assignedWorkers
                }

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

                    assignedWorkers =
                        finalAssignedWorkers,

                    workshopId =
                        proj.workshopId
                            .ifBlank {
                                currentUser.workshopId
                            },

                    updatedAt =
                        System.currentTimeMillis()
                )

            repository.updateProject(
                updated
            )

            syncProject(
                updated
            )
        }
    }

    // ============================================================
    // UPDATE PROJECT
    // ============================================================

    fun updateProjectInfo(
        project: Project
    ) {

        viewModelScope.launch {

            val currentUser =
                _currentUser.value
                    ?: return@launch

            val isAdmin =
                _currentRole.value
                    .trim()
                    .uppercase() == "ADMIN"

            val existing =
                repository
                    .getProject(project.id)
                    .firstOrNull()
                    ?: return@launch

            val updatedProject =
                if (isAdmin) {

                    project.copy(

                        workshopId =
                            existing.workshopId
                                .ifBlank {
                                    currentUser.workshopId
                                },

                        updatedAt =
                            System.currentTimeMillis()
                    )

                } else {

                    project.copy(

                        laborCost =
                            existing.laborCost,

                        paidAmount =
                            existing.paidAmount,

                        assignedWorkers =
                            existing.assignedWorkers,

                        workshopId =
                            existing.workshopId,

                        createdBy =
                            existing.createdBy,

                        updatedAt =
                            System.currentTimeMillis()
                    )
                }

            repository.updateProject(
                updatedProject
            )

            syncProject(
                updatedProject
            )

            logAuditAction(
                "تعديل بيانات المشروع: ${updatedProject.title}",
                updatedProject.id,
                updatedProject.title
            )
        }
    }

    // ============================================================
    // UPDATE PROJECT ORDER STATUS
    // ============================================================

    fun updateProjectOrderStatus(
        projectId: Long,
        newStatus: String
    ) {

        viewModelScope.launch {

            val proj =
                repository
                    .getProject(projectId)
                    .firstOrNull()

            if (proj != null) {

                val updated =
                    proj.copy(
                        orderStatus =
                            newStatus,

                        updatedAt =
                            System.currentTimeMillis()
                    )

                repository.updateProject(
                    updated
                )

                syncProject(
                    updated
                )
            }
        }
    }

    // ============================================================
    // DELETE PROJECT
    // ============================================================

    fun deleteProject(
        projectId: Long
    ) {

        if (_currentRole.value != "ADMIN") {
            return
        }

        viewModelScope.launch {

            val proj =
                repository
                    .getProject(projectId)
                    .firstOrNull()

            val projTitle =
                proj?.title ?: ""

            repository.deleteProject(
                projectId
            )

            FirestoreSync
                .deleteProjectFromFirestore(
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

    // ============================================================
    // DUPLICATE PROJECT
    // ============================================================

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

                val newProject =
                    repository
                        .getProject(newId)
                        .firstOrNull()

                if (newProject != null) {

                    syncProject(
                        newProject
                    )
                }

                _selectedProjectId.value =
                    newId
            }
        }
    }

    // ============================================================
    // ADD STANDARD MATERIAL
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
                        material.iconType,

                    workshopId =
                        _currentUser.value
                            ?.workshopId
                            ?: ""
                )

            repository
                .addOrUpdateProjectItem(
                    item
                )

            syncProjectItem(
                item
            )

            logAuditAction(
                "إضافة مادة للمشروع: ${material.nameAr} (الكمية: $quantity $unit)",
                projId,
                currentProject.value?.title ?: ""
            )
        }
    }

    // ============================================================
    // CUSTOM MATERIAL
    // ============================================================

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
                            imageUri,

                        workshopId =
                            _currentUser.value
                                ?.workshopId
                                ?: ""
                    )

                repository
                    .addOrUpdateProjectItem(
                        item
                    )

                syncProjectItem(
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

    // ============================================================
    // UPDATE ITEM UNIT PRICE
    // ============================================================

    fun updateItemUnitPrice(
        item: ProjectItem,
        newUnitPrice: Double
    ) {

        if (_currentRole.value != "ADMIN") {
            return
        }

        viewModelScope.launch {

            val updated =
                item.copy(
                    unitPrice =
                        newUnitPrice
                )

            repository
                .addOrUpdateProjectItem(
                    updated
                )

            syncProjectItem(
                updated,
                itemWorkerId(item)
            )

            logAuditAction(
                "تعديل سعر المادة ${item.materialNameAr} إلى $newUnitPrice د.ج",
                item.projectId,
                currentProject.value?.title ?: ""
            )
        }
    }

    // ============================================================
    // DELETE CUSTOM MATERIAL
    // ============================================================

    fun deleteCustomMaterial(
        id: Long
    ) {

        viewModelScope.launch {

            repository.deleteCustomMaterial(
                id
            )
        }
    }

    // ============================================================
    // ITEM QUANTITY
    // ============================================================

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

            val updated =
                item.copy(
                    quantity =
                        newQty
                )

            repository
                .updateProjectItem(
                    updated
                )

            syncProjectItem(
                updated
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

        updateItemQuantity(
            item,
            item.quantity + delta
        )
    }

    fun decrementItemQuantity(
        item: ProjectItem,
        delta: Double = 1.0
    ) {

        updateItemQuantity(
            item,
            item.quantity - delta
        )
    }

    // ============================================================
    // DELETE ITEM
    // ============================================================

    fun deleteItem(
        item: ProjectItem
    ) {

        viewModelScope.launch {

            repository.deleteProjectItem(
                item.id,
                item.projectId
            )

            FirestoreSync
                .deleteProjectItemFromFirestore(
                    item.id
                )

            logAuditAction(
                "حذف مادة من المشروع: ${item.materialNameAr}",
                item.projectId,
                currentProject.value?.title ?: ""
            )
        }
    }

    // ============================================================
    // TOGGLE PURCHASED
    // ============================================================

    fun toggleItemPurchased(
        item: ProjectItem
    ) {

        viewModelScope.launch {

            val updated =
                item.copy(
                    isPurchased =
                        !item.isPurchased
                )

            repository.setItemPurchased(
                item.id,
                !item.isPurchased
            )

            syncProjectItem(
                updated
            )
        }
    }

    // ============================================================
    // WATER POINT ESTIMATION
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
                    (
                        matKey,
                        size,
                        qty
                        ) ->

                val matObj =
                    PlumbingLibraryData
                        .ALL_MATERIALS
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
                                matObj.iconType,

                            workshopId =
                                _currentUser.value
                                    ?.workshopId
                                    ?: ""
                        )

                    repository
                        .addOrUpdateProjectItem(
                            item
                        )

                    syncProjectItem(
                        item
                    )
                }
            }
        }
    }

    // ============================================================
    // PIPE CALCULATION
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
                PlumbingLibraryData
                    .ALL_MATERIALS
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
                        "pipe",

                    workshopId =
                        _currentUser.value
                            ?.workshopId
                            ?: ""
                )

            repository
                .addOrUpdateProjectItem(
                    pipeItem
                )

            syncProjectItem(
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
                            "fitting",

                        workshopId =
                            _currentUser.value
                                ?.workshopId
                                ?: ""
                    )

                repository
                    .addOrUpdateProjectItem(
                        couplingItem
                    )

                syncProjectItem(
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
                            "fitting",

                        workshopId =
                            _currentUser.value
                                ?.workshopId
                                ?: ""
                    )

                repository
                    .addOrUpdateProjectItem(
                        clampItem
                    )

                syncProjectItem(
                    clampItem
                )
            }
        }
    }

    // ============================================================
    // CLEANUP
    // ============================================================

    override fun onCleared() {

        pendingRequestsListener
            ?.remove()

        pendingRequestsListener =
            null

        FirestoreSync
            .stopRealtimeListener()

        super.onCleared()
    }
}
