package com.example.ui.viewmodel

import android.app.Application
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PlumberViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PlumberRepository
    private val materialRepository: MaterialRepository
    private val teamStorePrefs = TeamStorePreferences(application)
    private val authPrefs = application.getSharedPreferences("auth_session", android.content.Context.MODE_PRIVATE)

    val libraryMaterials: StateFlow<List<MaterialEntity>>

    // Team User State & RBAC
    private val _currentUser = MutableStateFlow<TeamUser?>(null)
    val currentUser: StateFlow<TeamUser?> = _currentUser.asStateFlow()

    private val _currentRole = MutableStateFlow<String>("WORKER") // ADMIN or WORKER
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    private val _isUserLoggedIn = MutableStateFlow<Boolean>(false)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    val allTeamUsers: StateFlow<List<TeamUser>>
    val allWorkers: StateFlow<List<TeamUser>>
    val allAuditLogs: StateFlow<List<AuditLog>>
    val calculatedMaterialCaches: StateFlow<List<CalculatedMaterialCache>>

    init {
        val db = PlumberDatabase.getDatabase(application)
        val matDb = MaterialDatabase.getDatabase(application)
        repository = PlumberRepository(
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
        materialRepository = MaterialRepository(matDb.materialDao())

        allTeamUsers = repository.allTeamUsers
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allWorkers = repository.allWorkers
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allAuditLogs = repository.allAuditLogs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        calculatedMaterialCaches = repository.calculatedMaterialCaches
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        viewModelScope.launch {
            materialRepository.initializeLibraryIfEmpty(application)
            seedDefaultTeamUsersIfEmpty()
            restoreLoginSession()
        }

        libraryMaterials = materialRepository.allMaterials
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

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
            val cache = CalculatedMaterialCache(
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
            repository.saveCalculatedMaterialCache(cache)
        }
    }

    fun deleteCalculationCache(id: Long) {
        viewModelScope.launch {
            repository.deleteCalculatedMaterialCache(id)
        }
    }

    private suspend fun seedDefaultTeamUsersIfEmpty() {
        val existingAdmin = repository.getUserByPhoneOrEmail("0669964145") ?: repository.getUserByPhoneOrEmail("admin@plumber.com")
        if (existingAdmin == null) {
            val adminUser = TeamUser(
                uid = "admin_001",
                name = "إدارة التطبيق والمحل",
                phone = "0669964145",
                email = "admin@plumber.com",
                password = "ay120012",
                role = "ADMIN",
                active = true
            )
            repository.insertOrUpdateTeamUser(adminUser)
            FirestoreSync.syncUserToFirestore(adminUser)
        }

        val existingWorker = repository.getUserByPhoneOrEmail("0660000000") ?: repository.getUserByPhoneOrEmail("worker@plumber.com")
        if (existingWorker == null) {
            val workerUser = TeamUser(
                uid = "worker_001",
                name = "أحمد (عامل التركيبات)",
                phone = "0660000000",
                email = "worker@plumber.com",
                password = "adb120012",
                role = "WORKER",
                active = true
            )
            repository.insertOrUpdateTeamUser(workerUser)
            FirestoreSync.syncUserToFirestore(workerUser)
        }
    }

    private suspend fun restoreLoginSession() {
        val savedUid = authPrefs.getString("logged_uid", null)
        if (!savedUid.isNullOrEmpty()) {
            val user = repository.getUserByUid(savedUid)
            if (user != null && user.active) {
                _currentUser.value = user
                _currentRole.value = user.role.uppercase()
                _isUserLoggedIn.value = true
                _isManagerLoggedIn.value = (user.role.uppercase() == "ADMIN")
                FirestoreSync.startRealtimeListener(getApplication(), user.role, user.uid)
            }
        }
    }

    fun loginTeamUser(phoneOrEmail: String, pass: String, onResult: (Boolean, String) -> Unit) {
        val cleanInput = phoneOrEmail.trim()
        val cleanPass = pass.trim()

        if (cleanInput.isBlank() || cleanPass.isBlank()) {
            onResult(false, "يرجى إدخال اسم المستخدم/الهاتف وكلمة المرور")
            return
        }

        viewModelScope.launch {
            var user = repository.getUserByPhoneOrEmail(cleanInput)
            if (user == null && (cleanInput == "0669964145" || cleanInput == "admin@plumber.com") && cleanPass == "ay120012") {
                user = TeamUser(
                    uid = "admin_001",
                    name = "إدارة التطبيق والمحل",
                    phone = "0669964145",
                    email = "admin@plumber.com",
                    password = "ay120012",
                    role = "ADMIN",
                    active = true
                )
                repository.insertOrUpdateTeamUser(user)
            } else if (user == null && (cleanInput == "0660000000" || cleanInput == "worker@plumber.com") && cleanPass == "adb120012") {
                user = TeamUser(
                    uid = "worker_001",
                    name = "أحمد (عامل التركيبات)",
                    phone = "0660000000",
                    email = "worker@plumber.com",
                    password = "adb120012",
                    role = "WORKER",
                    active = true
                )
                repository.insertOrUpdateTeamUser(user)
            }

            // If still null, try fetching from Firestore Database
            if (user == null) {
                FirestoreSync.fetchUserFromFirestoreByPhoneOrEmail(cleanInput) { fetchedUser ->
                    if (fetchedUser != null) {
                        viewModelScope.launch {
                            repository.insertOrUpdateTeamUser(fetchedUser)
                            processUserLogin(fetchedUser, cleanPass, onResult)
                        }
                    } else {
                        onResult(false, "بيانات الدخول غير مسجلة لدينا. يمكنك إنشاء حساب عامل جديد.")
                    }
                }
            } else {
                processUserLogin(user, cleanPass, onResult)
            }
        }
    }

    private suspend fun processUserLogin(user: TeamUser, cleanPass: String, onResult: (Boolean, String) -> Unit) {
        if (!user.active) {
            onResult(false, "هذا الحساب معطل حالياً من قبل الإدارة.")
            return
        }

        if (user.password.isNotBlank() && user.password != cleanPass) {
            onResult(false, "كلمة المرور غير صحيحة.")
            return
        }

        val updatedUser = user.copy(lastLoginAt = System.currentTimeMillis())
        repository.insertOrUpdateTeamUser(updatedUser)
        FirestoreSync.syncUserToFirestore(updatedUser)

        _currentUser.value = updatedUser
        _currentRole.value = updatedUser.role.uppercase()
        _isUserLoggedIn.value = true
        _isManagerLoggedIn.value = (updatedUser.role.uppercase() == "ADMIN")

        authPrefs.edit().putString("logged_uid", updatedUser.uid).apply()

        FirestoreSync.startRealtimeListener(getApplication(), updatedUser.role, updatedUser.uid)

        logAuditAction("تسجيل دخول إلى النظام: ${updatedUser.name}", 0, "")
        onResult(true, "مرحباً بك ${updatedUser.name}! 🟢")
    }

    // Direct Worker Self-Registration & Linking to Firestore
    fun registerWorkerAccount(
        name: String,
        phone: String,
        email: String,
        pass: String,
        role: String = "WORKER",
        autoLogin: Boolean = true,
        onResult: (Boolean, String) -> Unit
    ) {
        if (name.isBlank() || phone.isBlank() || pass.isBlank()) {
            onResult(false, "يرجى ملء الاسم الكامل، رقم الهاتف وكلمة المرور.")
            return
        }

        viewModelScope.launch {
            val cleanPhone = phone.trim()
            val cleanEmail = if (email.isBlank()) "$cleanPhone@plumber.com" else email.trim()

            val existing = repository.getUserByPhoneOrEmail(cleanPhone)
            if (existing != null) {
                onResult(false, "رقم الهاتف ($cleanPhone) مسجل بالفعل في النظام.")
                return@launch
            }

            val newUid = "worker_${System.currentTimeMillis()}"
            val newUser = TeamUser(
                uid = newUid,
                name = name.trim(),
                phone = cleanPhone,
                email = cleanEmail,
                password = pass.trim(),
                role = role.uppercase(),
                active = true,
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis()
            )

            // Save to local Room DB
            repository.insertOrUpdateTeamUser(newUser)

            // Sync directly to Firestore
            FirestoreSync.syncUserToFirestore(newUser)

            // Update workers list
            val updatedList = _workersList.value.toMutableList()
            val entryStr = "${newUser.name} (${newUser.phone})"
            if (!updatedList.contains(entryStr)) {
                updatedList.add(entryStr)
                updateWorkersList(updatedList)
            }

            logAuditAction("تسجيل حساب عامل جديد ومزامنته مع Firestore: ${newUser.name} (${newUser.role})", 0, "")

            if (autoLogin) {
                _currentUser.value = newUser
                _currentRole.value = newUser.role.uppercase()
                _isUserLoggedIn.value = true
                _isManagerLoggedIn.value = (newUser.role.uppercase() == "ADMIN")
                authPrefs.edit().putString("logged_uid", newUser.uid).apply()
                FirestoreSync.startRealtimeListener(getApplication(), newUser.role, newUser.uid)
            }

            onResult(true, "تم تسجيل حساب العامل ${newUser.name} وربطه بـ Firestore بنجاح! 🟢🔥")
        }
    }

    fun logoutTeamUser() {
        val user = _currentUser.value
        if (user != null) {
            logAuditAction("تسجيل خروج من النظام", 0, "")
        }
        FirestoreSync.stopRealtimeListener()
        _currentUser.value = null
        _currentRole.value = "WORKER"
        _isUserLoggedIn.value = false
        _isManagerLoggedIn.value = false
        authPrefs.edit().remove("logged_uid").apply()
    }

    // Admin Worker Account Management
    fun createWorkerAccount(
        name: String,
        phone: String,
        email: String,
        pass: String,
        role: String = "WORKER",
        onResult: (Boolean, String) -> Unit
    ) {
        if (_currentRole.value != "ADMIN") {
            onResult(false, "عذراً! الإدارة فقط تملك صلاحية إنشاء حسابات العمال.")
            return
        }

        if (name.isBlank() || phone.isBlank() || pass.isBlank()) {
            onResult(false, "يرجى ملء الاسم الكامل، رقم الهاتف وكلمة المرور.")
            return
        }

        viewModelScope.launch {
            val existing = repository.getUserByPhoneOrEmail(phone.trim())
            if (existing != null) {
                onResult(false, "رقم الهاتف مستخدم بالفعل لمستخدم آخر.")
                return@launch
            }

            val newUid = "worker_${System.currentTimeMillis()}"
            val newUser = TeamUser(
                uid = newUid,
                name = name.trim(),
                phone = phone.trim(),
                email = email.trim(),
                password = pass.trim(),
                role = role.uppercase(),
                active = true,
                createdAt = System.currentTimeMillis()
            )

            repository.insertOrUpdateTeamUser(newUser)
            FirestoreSync.syncUserToFirestore(newUser)

            // Update workers list
            val updatedList = _workersList.value.toMutableList()
            val entryStr = "${newUser.name} (${newUser.phone})"
            if (!updatedList.contains(entryStr)) {
                updatedList.add(entryStr)
                updateWorkersList(updatedList)
            }

            logAuditAction("إنشاء حساب جديد للعامل/المستخدم: ${newUser.name} (${newUser.role})", 0, "")
            onResult(true, "تم إنشاء حساب ${newUser.name} بنجاح!")
        }
    }

    fun toggleWorkerActive(uid: String, active: Boolean) {
        if (_currentRole.value != "ADMIN") return
        viewModelScope.launch {
            val user = repository.getUserByUid(uid)
            if (user != null) {
                repository.setUserActive(uid, active)
                val updated = user.copy(active = active)
                FirestoreSync.syncUserToFirestore(updated)
                logAuditAction("تغيير حالة حساب العامل ${user.name} إلى: ${if (active) "مفعّل" else "معطّل"}", 0, "")
            }
        }
    }

    fun resetWorkerPassword(uid: String, newPass: String, onDone: (Boolean) -> Unit) {
        if (_currentRole.value != "ADMIN") {
            onDone(false)
            return
        }
        viewModelScope.launch {
            val user = repository.getUserByUid(uid)
            if (user != null && newPass.isNotBlank()) {
                repository.updateUserPassword(uid, newPass.trim())
                val updated = user.copy(password = newPass.trim())
                FirestoreSync.syncUserToFirestore(updated)
                logAuditAction("إعادة تعيين كلمة المرور للعامل ${user.name}", 0, "")
                onDone(true)
            } else {
                onDone(false)
            }
        }
    }

    fun deleteWorkerAccount(uid: String) {
        if (_currentRole.value != "ADMIN") return
        viewModelScope.launch {
            val user = repository.getUserByUid(uid)
            if (user != null) {
                repository.deleteTeamUser(uid)
                logAuditAction("حذف حساب العامل: ${user.name}", 0, "")
            }
        }
    }

    // Audit Log Function
    fun logAuditAction(action: String, projectId: Long = 0, projectName: String = "") {
        viewModelScope.launch {
            val worker = _currentUser.value
            val log = AuditLog(
                workerId = worker?.uid ?: "GUEST",
                workerName = worker?.name ?: _activeWorker.value,
                action = action,
                projectId = projectId,
                projectName = projectName,
                timestamp = System.currentTimeMillis()
            )
            repository.insertAuditLog(log)
            FirestoreSync.syncAuditLogToFirestore(log)
        }
    }

    fun addMaterialToLibrary(material: MaterialEntity) {
        viewModelScope.launch {
            materialRepository.insertMaterial(material)
        }
    }

    fun updateMaterialInLibrary(material: MaterialEntity) {
        viewModelScope.launch {
            materialRepository.updateMaterial(material)
        }
    }

    fun deleteMaterialFromLibrary(id: String) {
        viewModelScope.launch {
            materialRepository.deleteMaterial(id)
        }
    }

    fun addLibraryMaterialToProject(
        material: MaterialEntity,
        size: String,
        quantity: Double,
        unit: String,
        notes: String = ""
    ) {
        val projId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            val item = ProjectItem(
                projectId = projId,
                materialKey = material.id,
                materialNameAr = material.nameAr,
                materialNameFr = material.nameFr,
                category = material.category,
                size = size,
                quantity = quantity,
                unit = unit,
                unitPrice = material.price,
                notes = notes,
                iconType = material.iconType,
                imageUri = material.image.ifBlank { null }
            )
            repository.addOrUpdateProjectItem(item)
        }
    }

    private val _storeName = MutableStateFlow(teamStorePrefs.storeName)
    val storeName: StateFlow<String> = _storeName.asStateFlow()

    private val _storePhone = MutableStateFlow(teamStorePrefs.storePhone)
    val storePhone: StateFlow<String> = _storePhone.asStateFlow()

    private val _storeWhatsapp = MutableStateFlow(teamStorePrefs.storeWhatsapp)
    val storeWhatsapp: StateFlow<String> = _storeWhatsapp.asStateFlow()

    private val _activeWorker = MutableStateFlow(teamStorePrefs.activeWorker)
    val activeWorker: StateFlow<String> = _activeWorker.asStateFlow()

    private val _managerName = MutableStateFlow(teamStorePrefs.managerName)
    val managerName: StateFlow<String> = _managerName.asStateFlow()

    private val _isManagerLoggedIn = MutableStateFlow(false)
    val isManagerLoggedIn: StateFlow<Boolean> = _isManagerLoggedIn.asStateFlow()

    private val _managerPin = MutableStateFlow(teamStorePrefs.managerPin)
    val managerPin: StateFlow<String> = _managerPin.asStateFlow()

    private val _workersList = MutableStateFlow(teamStorePrefs.getWorkersList())
    val workersList: StateFlow<List<String>> = _workersList.asStateFlow()

    fun setManagerLoggedIn(loggedIn: Boolean) {
        _isManagerLoggedIn.value = loggedIn
    }

    fun loginManager(pin: String): Boolean {
        if (pin.trim() == teamStorePrefs.managerPin) {
            _isManagerLoggedIn.value = true
            return true
        }
        return false
    }

    fun logoutManager() {
        _isManagerLoggedIn.value = false
    }

    fun updateManagerPin(oldPin: String, newPin: String): Boolean {
        if (oldPin.trim() == teamStorePrefs.managerPin && newPin.isNotBlank()) {
            teamStorePrefs.managerPin = newPin.trim()
            _managerPin.value = newPin.trim()
            return true
        }
        return false
    }

    fun updateTeamStoreSettings(
        storeNameVal: String,
        storePhoneVal: String,
        activeWorkerVal: String,
        managerNameVal: String = _managerName.value,
        storeWhatsappVal: String = _storeWhatsapp.value
    ) {
        teamStorePrefs.storeName = storeNameVal
        teamStorePrefs.storePhone = storePhoneVal
        teamStorePrefs.storeWhatsapp = storeWhatsappVal
        teamStorePrefs.activeWorker = activeWorkerVal
        teamStorePrefs.managerName = managerNameVal
        _storeName.value = storeNameVal
        _storePhone.value = storePhoneVal
        _storeWhatsapp.value = storeWhatsappVal
        _activeWorker.value = activeWorkerVal
        _managerName.value = managerNameVal

        // Sync store settings directly to Firestore Database
        FirestoreSync.syncStoreSettingsToFirestore(
            storeName = storeNameVal,
            storePhone = storePhoneVal,
            storeWhatsapp = storeWhatsappVal,
            managerName = managerNameVal
        )
    }

    fun updateWorkersList(newList: List<String>) {
        teamStorePrefs.saveWorkersList(newList)
        _workersList.value = newList
    }

    fun updateProjectOrderStatus(projectId: Long, newStatus: String) {
        viewModelScope.launch {
            val proj = repository.getProject(projectId).firstOrNull()
            if (proj != null) {
                repository.updateProject(proj.copy(orderStatus = newStatus, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    val allProjects: StateFlow<List<Project>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleProjects: StateFlow<List<Project>> = combine(allProjects, currentUser, currentRole) { projects, user, role ->
        if (role == "ADMIN" || user == null) {
            projects
        } else {
            val userUid = user.uid
            val userPhone = user.phone
            val userName = user.name
            projects.filter { proj ->
                proj.isAssignedToWorker(userUid) || 
                proj.isAssignedToWorker(userPhone) || 
                proj.isAssignedToWorker(userName) ||
                proj.workerName.isBlank()
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customMaterials: StateFlow<List<CustomMaterial>> = repository.customMaterials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAppointments: StateFlow<List<Appointment>> = repository.allAppointments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWorkAlerts: StateFlow<List<WorkAlert>> = repository.allWorkAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addWorkAlert(
        alertType: String,
        senderName: String,
        recipientRole: String,
        title: String,
        details: String,
        projectName: String = ""
    ) {
        viewModelScope.launch {
            val alert = WorkAlert(
                alertType = alertType,
                senderName = senderName,
                recipientRole = recipientRole,
                title = title,
                details = details,
                projectName = projectName
            )
            repository.insertWorkAlert(alert)
        }
    }

    fun updateWorkAlertStatus(alert: WorkAlert, newStatus: String) {
        viewModelScope.launch {
            repository.updateWorkAlert(alert.copy(status = newStatus))
        }
    }

    fun deleteWorkAlert(id: Long) {
        viewModelScope.launch {
            repository.deleteWorkAlert(id)
        }
    }

    fun addAppointment(
        serviceType: String,
        customerName: String,
        phoneNumber: String,
        address: String,
        scheduledTime: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val appt = Appointment(
                serviceType = serviceType,
                customerName = customerName,
                phoneNumber = phoneNumber,
                address = address,
                scheduledTime = scheduledTime,
                notes = notes
            )
            repository.insertAppointment(appt)
        }
    }

    fun updateAppointmentStatus(appointment: Appointment, newStatus: String) {
        viewModelScope.launch {
            repository.updateAppointment(appointment.copy(status = newStatus))
        }
    }

    fun deleteAppointment(id: Long) {
        viewModelScope.launch {
            repository.deleteAppointment(id)
        }
    }

    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentProject: StateFlow<Project?> = _selectedProjectId.flatMapLatest { id ->
        if (id != null) repository.getProject(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentProjectItems: StateFlow<List<ProjectItem>> = _selectedProjectId.flatMapLatest { id ->
        if (id != null) repository.getProjectItems(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectProject(projectId: Long?) {
        _selectedProjectId.value = projectId
    }

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
            val workTypeObj = PlumbingLibraryData.WORK_TYPES.find { it.key == workTypeKey }
            val workTypeName = workTypeObj?.titleAr ?: "عمل مخصص"

            val newProj = Project(
                title = title.ifBlank { "مشروع جديد" },
                clientName = clientName,
                location = location,
                notes = notes,
                workTypeKey = workTypeKey,
                workTypeNameAr = workTypeName,
                workerName = workerName.ifBlank { _activeWorker.value },
                managerName = managerName.ifBlank { _managerName.value },
                storePhone = storePhone.ifBlank { _storePhone.value },
                assignedWorkers = workerName.ifBlank { _activeWorker.value },
                createdBy = _currentUser.value?.uid ?: "ADMIN",
                laborCost = laborCost,
                paidAmount = paidAmount
            )
            val newId = repository.createProject(newProj)
            val createdObj = newProj.copy(id = newId)
            FirestoreSync.syncProjectToFirestore(createdObj)
            logAuditAction("إنشاء مشروع جديد: ${createdObj.title}", newId, createdObj.title)
            _selectedProjectId.value = newId
            onProjectCreated(newId)
        }
    }

    fun addWorkerPayment(projectId: Long, paymentAmount: Double) {
        viewModelScope.launch {
            val proj = repository.getProject(projectId).firstOrNull()
            if (proj != null && paymentAmount > 0) {
                val newPaid = proj.paidAmount + paymentAmount
                repository.updateProject(
                    proj.copy(
                        paidAmount = newPaid,
                        updatedAt = System.currentTimeMillis()
                    )
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
        viewModelScope.launch {
            val proj = repository.getProject(projectId).firstOrNull()
            if (proj != null) {
                repository.updateProject(
                    proj.copy(
                        workerName = workerName,
                        managerName = managerName,
                        laborCost = laborCost,
                        paidAmount = paidAmount,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun updateProjectInfo(project: Project) {
        viewModelScope.launch {
            repository.updateProject(project)
            FirestoreSync.syncProjectToFirestore(project)
            logAuditAction("تعديل بيانات المشروع: ${project.title}", project.id, project.title)
        }
    }

    fun deleteProject(projectId: Long) {
        if (_currentRole.value != "ADMIN") return // Only ADMIN can delete projects
        viewModelScope.launch {
            val proj = repository.getProject(projectId).firstOrNull()
            val projTitle = proj?.title ?: ""
            repository.deleteProject(projectId)
            FirestoreSync.deleteProjectFromFirestore(projectId)
            logAuditAction("حذف مشروع: $projTitle", projectId, projTitle)
            if (_selectedProjectId.value == projectId) {
                _selectedProjectId.value = null
            }
        }
    }

    fun duplicateProject(projectId: Long, newTitle: String) {
        viewModelScope.launch {
            val newId = repository.duplicateProject(projectId, newTitle)
            if (newId > 0) {
                _selectedProjectId.value = newId
            }
        }
    }

    fun addMaterialToProject(
        material: PlumbingMaterial,
        size: String,
        quantity: Double,
        unit: String,
        notes: String = ""
    ) {
        val projId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            val item = ProjectItem(
                projectId = projId,
                materialKey = material.key,
                materialNameAr = material.nameAr,
                materialNameFr = material.nameFr,
                category = material.category,
                size = size,
                quantity = quantity,
                unit = unit,
                unitPrice = material.defaultUnitPrice,
                notes = notes,
                iconType = material.iconType
            )
            repository.addOrUpdateProjectItem(item)
            logAuditAction("إضافة مادة للمشروع: ${material.nameAr} (الكمية: $quantity $unit)", projId, currentProject.value?.title ?: "")
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
            // 1. Save to local Room Database custom_materials table
            val customMat = CustomMaterial(
                nameAr = nameAr,
                nameFr = nameFr,
                category = category,
                defaultSize = size,
                defaultUnit = unit,
                defaultPrice = price,
                imageUri = imageUri,
                notes = notes
            )
            val customId = repository.addCustomMaterial(customMat)

            // 2. Add to current project if a project is active
            val projId = _selectedProjectId.value
            if (projId != null && quantity > 0) {
                val item = ProjectItem(
                    projectId = projId,
                    materialKey = "custom_$customId",
                    materialNameAr = nameAr,
                    materialNameFr = nameFr,
                    category = category,
                    size = size,
                    quantity = quantity,
                    unit = unit,
                    unitPrice = price,
                    notes = notes,
                    iconType = "custom",
                    imageUri = imageUri
                )
                repository.addOrUpdateProjectItem(item)
                logAuditAction("إضافة مادة مخصصة: $nameAr (الكمية: $quantity $unit)", projId, currentProject.value?.title ?: "")
            }
        }
    }

    fun updateItemUnitPrice(item: ProjectItem, newUnitPrice: Double) {
        if (_currentRole.value != "ADMIN") return // Workers CANNOT change prices!
        viewModelScope.launch {
            val updated = item.copy(unitPrice = newUnitPrice)
            repository.addOrUpdateProjectItem(updated)
            logAuditAction("تعديل سعر المادة ${item.materialNameAr} إلى $newUnitPrice د.ج", item.projectId, currentProject.value?.title ?: "")
        }
    }

    fun deleteCustomMaterial(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomMaterial(id)
        }
    }

    fun updateItemQuantity(item: ProjectItem, newQty: Double) {
        if (newQty <= 0) {
            deleteItem(item)
            return
        }
        viewModelScope.launch {
            repository.updateProjectItem(item.copy(quantity = newQty))
            logAuditAction("تحديث كمية المادة ${item.materialNameAr} إلى $newQty ${item.unit}", item.projectId, currentProject.value?.title ?: "")
        }
    }

    fun incrementItemQuantity(item: ProjectItem, delta: Double = 1.0) {
        updateItemQuantity(item, item.quantity + delta)
    }

    fun decrementItemQuantity(item: ProjectItem, delta: Double = 1.0) {
        updateItemQuantity(item, item.quantity - delta)
    }

    fun deleteItem(item: ProjectItem) {
        viewModelScope.launch {
            repository.deleteProjectItem(item.id, item.projectId)
            logAuditAction("حذف مادة من المشروع: ${item.materialNameAr}", item.projectId, currentProject.value?.title ?: "")
        }
    }

    fun toggleItemPurchased(item: ProjectItem) {
        viewModelScope.launch {
            repository.setItemPurchased(item.id, !item.isPurchased)
        }
    }

    // Automatic Estimation feature based on water points count
    fun estimateMaterialsByWaterPoints(
        projectId: Long,
        bathroomsCount: Int,
        kitchensCount: Int,
        sinksCount: Int,
        showersCount: Int,
        toiletsCount: Int
    ) {
        viewModelScope.launch {
            val totalPoints = (bathroomsCount * 4) + (kitchensCount * 3) + sinksCount + showersCount + toiletsCount
            if (totalPoints <= 0) return@launch

            // Standard estimation ratios per point
            val coudesPprQty = (totalPoints * 2.5) // ~2.5 coudes per water point
            val tesPprQty = (totalPoints * 1.2)   // ~1.2 tees per water point
            val tubePprMeters = (totalPoints * 4.5) // ~4.5m PPR pipe per point
            val coudesWallQty = (totalPoints * 1.0) // wall elbow per outlet
            val vanneQty = (bathroomsCount + kitchensCount + 1).toDouble() // main valves
            val tubePvc110Meters = ((bathroomsCount + toiletsCount) * 4.0) // PVC 110mm for toilets
            val tubePvc50Meters = ((sinksCount + showersCount + kitchensCount) * 3.0) // PVC 50mm for drainage
            val teflonRolls = kotlin.math.max(2.0, (totalPoints * 0.4))

            val estimates = listOf(
                Triple("ppr_coude_90", "25mm", coudesPprQty),
                Triple("ppr_te", "25mm", tesPprQty),
                Triple("ppr_tube", "25mm", tubePprMeters),
                Triple("ppr_coude_filete", "25x1/2\"", coudesWallQty),
                Triple("ppr_vanne", "25mm", vanneQty),
                Triple("pvc_tube", "110mm", tubePvc110Meters),
                Triple("pvc_tube", "50mm", tubePvc50Meters),
                Triple("teflon", "قياسي (12mm x 12m)", teflonRolls)
            )

            estimates.forEach { (matKey, size, qty) ->
                val matObj = PlumbingLibraryData.ALL_MATERIALS.find { it.key == matKey }
                if (matObj != null && qty > 0) {
                    val item = ProjectItem(
                        projectId = projectId,
                        materialKey = matObj.key,
                        materialNameAr = matObj.nameAr,
                        materialNameFr = matObj.nameFr,
                        category = matObj.category,
                        size = size,
                        quantity = kotlin.math.round(qty * 10.0) / 10.0,
                        unit = matObj.defaultUnit,
                        notes = "تقدير تلقائي حسب نقاط الماء ($totalPoints نقطة)",
                        iconType = matObj.iconType
                    )
                    repository.addOrUpdateProjectItem(item)
                }
            }
        }
    }

    // Save interactive pipe calculator result directly into project database
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
            val pipeMaterialKey = if (pipeCategory.contains("PPR", ignoreCase = true)) "ppr_tube" else "pvc_tube"
            val matObj = PlumbingLibraryData.ALL_MATERIALS.find { it.key == pipeMaterialKey }

            val pipeNameAr = "أنبوب $pipeCategory ($totalPipesCount عود / أنبوب)"
            val pipeItem = ProjectItem(
                projectId = projectId,
                materialKey = matObj?.key ?: "pipe_${System.currentTimeMillis()}",
                materialNameAr = pipeNameAr,
                materialNameFr = "Tube $pipeCategory",
                category = pipeCategory,
                size = pipeSize,
                quantity = totalPipesCount.toDouble(),
                unit = "قطعة",
                notes = "$calculationSummaryNotes - إجمالي الأمتار: ${String.format("%.2f", totalMetersWithWaste)} م",
                iconType = "pipe"
            )
            repository.addOrUpdateProjectItem(pipeItem)

            if (couplingsCount > 0) {
                val couplingItem = ProjectItem(
                    projectId = projectId,
                    materialKey = "coupling_${System.currentTimeMillis()}",
                    materialNameAr = "وصلة مستقيمة (مانشون $pipeCategory)",
                    materialNameFr = "Manchon $pipeCategory",
                    category = pipeCategory,
                    size = pipeSize,
                    quantity = couplingsCount.toDouble(),
                    unit = "قطعة",
                    notes = "مصلحة ربط الوصلات حسب طول الأنابيب",
                    iconType = "fitting"
                )
                repository.addOrUpdateProjectItem(couplingItem)
            }

            if (clampsCount > 0) {
                val clampItem = ProjectItem(
                    projectId = projectId,
                    materialKey = "clamp_${System.currentTimeMillis()}",
                    materialNameAr = "قفيز / مشابك تثبيت جداري",
                    materialNameFr = "Colliers de fixation",
                    category = "Accessoires",
                    size = pipeSize,
                    quantity = clampsCount.toDouble(),
                    unit = "قطعة",
                    notes = "محسوبة للتثبيت على مسافات متساوية",
                    iconType = "fitting"
                )
                repository.addOrUpdateProjectItem(clampItem)
            }
        }
    }
}
