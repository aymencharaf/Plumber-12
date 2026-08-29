package com.example.data.repository

import com.example.data.dao.AppSettingCacheDao
import com.example.data.dao.AppointmentDao
import com.example.data.dao.AuditLogDao
import com.example.data.dao.CalculatedMaterialCacheDao
import com.example.data.dao.CustomMaterialDao
import com.example.data.dao.ProjectDao
import com.example.data.dao.ProjectItemDao
import com.example.data.dao.TeamUserDao
import com.example.data.dao.WorkAlertDao
import com.example.data.model.AppSettingCache
import com.example.data.model.Appointment
import com.example.data.model.AuditLog
import com.example.data.model.CalculatedMaterialCache
import com.example.data.model.CustomMaterial
import com.example.data.model.Project
import com.example.data.model.ProjectItem
import com.example.data.model.TeamUser
import com.example.data.model.WorkAlert
import com.example.data.sync.FirestoreSync
import kotlinx.coroutines.flow.Flow

class PlumberRepository(
    private val projectDao: ProjectDao,
    private val projectItemDao: ProjectItemDao,
    private val customMaterialDao: CustomMaterialDao,
    private val appointmentDao: AppointmentDao? = null,
    private val workAlertDao: WorkAlertDao? = null,
    private val teamUserDao: TeamUserDao? = null,
    private val auditLogDao: AuditLogDao? = null,
    private val calculatedMaterialCacheDao: CalculatedMaterialCacheDao? = null,
    private val appSettingCacheDao: AppSettingCacheDao? = null
) {
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()
    val customMaterials: Flow<List<CustomMaterial>> = customMaterialDao.getAllCustomMaterials()
    val allAppointments: Flow<List<Appointment>> = appointmentDao?.getAllAppointments() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    val allWorkAlerts: Flow<List<WorkAlert>> = workAlertDao?.getAllAlerts() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    val allTeamUsers: Flow<List<TeamUser>> = teamUserDao?.getAllTeamUsers() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    val allWorkers: Flow<List<TeamUser>> = teamUserDao?.getAllWorkers() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    val allAuditLogs: Flow<List<AuditLog>> = auditLogDao?.getAllAuditLogs() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    val calculatedMaterialCaches: Flow<List<CalculatedMaterialCache>> = calculatedMaterialCacheDao?.getAllCalculatedCaches() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun saveCalculatedMaterialCache(cache: CalculatedMaterialCache): Long {
        return calculatedMaterialCacheDao?.insertCache(cache) ?: 0L
    }

    suspend fun deleteCalculatedMaterialCache(id: Long) {
        calculatedMaterialCacheDao?.deleteCacheById(id)
    }

    suspend fun saveSettingCache(key: String, value: String) {
        appSettingCacheDao?.insertOrUpdateSetting(AppSettingCache(key, value))
    }

    suspend fun getSettingCache(key: String): String? {
        return appSettingCacheDao?.getSettingByKey(key)?.settingValue
    }

    suspend fun insertOrUpdateTeamUser(user: TeamUser) {
        teamUserDao?.insertOrUpdateUser(user)
    }

    suspend fun getUserByUid(uid: String): TeamUser? {
        return teamUserDao?.getUserByUid(uid)
    }

    suspend fun getUserByPhoneOrEmail(query: String): TeamUser? {
        val q = query.trim()
        return teamUserDao?.getUserByPhoneOrEmail(q, q)
    }

    suspend fun setUserActive(uid: String, active: Boolean) {
        teamUserDao?.setUserActive(uid, active)
    }

    suspend fun updateUserPassword(uid: String, newPass: String) {
        teamUserDao?.updatePassword(uid, newPass)
    }

    suspend fun deleteTeamUser(uid: String) {
        teamUserDao?.deleteUserByUid(uid)
    }

    suspend fun insertAuditLog(log: AuditLog) {
        auditLogDao?.insertAuditLog(log)
    }

    suspend fun insertWorkAlert(alert: WorkAlert): Long {
        return workAlertDao?.insertAlert(alert) ?: 0L
    }

    suspend fun updateWorkAlert(alert: WorkAlert) {
        workAlertDao?.updateAlert(alert)
    }

    suspend fun deleteWorkAlert(id: Long) {
        workAlertDao?.deleteById(id)
    }

    suspend fun insertAppointment(appointment: Appointment): Long {
        return appointmentDao?.insertAppointment(appointment) ?: 0L
    }

    suspend fun updateAppointment(appointment: Appointment) {
        appointmentDao?.updateAppointment(appointment)
    }

    suspend fun deleteAppointment(id: Long) {
        appointmentDao?.deleteById(id)
    }

    fun getProject(id: Long): Flow<Project?> = projectDao.getProjectById(id)
    fun getProjectItems(projectId: Long): Flow<List<ProjectItem>> = projectItemDao.getItemsForProject(projectId)

    suspend fun createProject(project: Project): Long {
        val newId = projectDao.insertProject(project)
        val createdProject = project.copy(id = if (project.id > 0) project.id else newId)
        FirestoreSync.syncProjectToFirestore(createdProject)
        return newId
    }

    suspend fun updateProject(project: Project) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        projectDao.updateProject(updated)
        FirestoreSync.syncProjectToFirestore(updated)
    }

    suspend fun deleteProject(id: Long) {
        projectDao.deleteProjectById(id)
        FirestoreSync.deleteProjectFromFirestore(id)
    }

    suspend fun addOrUpdateProjectItem(item: ProjectItem) {
        // Check if item with same key and size already exists in this project
        val existing = projectItemDao.getItemByKeyAndSize(item.projectId, item.materialKey, item.size)
        val finalItem: ProjectItem
        if (existing != null) {
            val updated = existing.copy(
                quantity = existing.quantity + item.quantity,
                unit = item.unit,
                notes = if (item.notes.isNotBlank()) item.notes else existing.notes
            )
            projectItemDao.updateItem(updated)
            finalItem = updated
        } else {
            val newId = projectItemDao.insertItem(item)
            finalItem = item.copy(id = if (item.id > 0) item.id else newId)
        }
        FirestoreSync.syncProjectItemToFirestore(finalItem)
        // Touch project update time
        val project = projectDao.getProjectByIdDirect(item.projectId)
        if (project != null) {
            val updatedProject = project.copy(updatedAt = System.currentTimeMillis())
            projectDao.updateProject(updatedProject)
            FirestoreSync.syncProjectToFirestore(updatedProject)
        }
    }

    suspend fun updateProjectItem(item: ProjectItem) {
        projectItemDao.updateItem(item)
        FirestoreSync.syncProjectItemToFirestore(item)
        val project = projectDao.getProjectByIdDirect(item.projectId)
        if (project != null) {
            val updatedProject = project.copy(updatedAt = System.currentTimeMillis())
            projectDao.updateProject(updatedProject)
            FirestoreSync.syncProjectToFirestore(updatedProject)
        }
    }

    suspend fun deleteProjectItem(id: Long, projectId: Long) {
        projectItemDao.deleteItemById(id)
        FirestoreSync.deleteProjectItemFromFirestore(id)
        val project = projectDao.getProjectByIdDirect(projectId)
        if (project != null) {
            val updatedProject = project.copy(updatedAt = System.currentTimeMillis())
            projectDao.updateProject(updatedProject)
            FirestoreSync.syncProjectToFirestore(updatedProject)
        }
    }

    suspend fun setItemPurchased(itemId: Long, isPurchased: Boolean) {
        projectItemDao.updatePurchaseState(itemId, isPurchased)
    }

    suspend fun addCustomMaterial(material: CustomMaterial): Long {
        return customMaterialDao.insertCustomMaterial(material)
    }

    suspend fun deleteCustomMaterial(id: Long) {
        customMaterialDao.deleteCustomMaterial(id)
    }

    suspend fun duplicateProject(projectId: Long, newTitle: String): Long {
        val originalProject = projectDao.getProjectByIdDirect(projectId) ?: return 0L
        val originalItems = projectItemDao.getItemsForProjectDirect(projectId)

        val newProject = Project(
            title = newTitle,
            clientName = originalProject.clientName,
            location = originalProject.location,
            notes = originalProject.notes,
            workTypeKey = originalProject.workTypeKey,
            workTypeNameAr = originalProject.workTypeNameAr
        )
        val newProjectId = projectDao.insertProject(newProject)

        originalItems.forEach { item ->
            projectItemDao.insertItem(
                item.copy(
                    id = 0,
                    projectId = newProjectId,
                    isPurchased = false,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        return newProjectId
    }
}
