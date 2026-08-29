package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.db.PlumberDatabase
import com.example.data.model.Appointment
import com.example.data.model.CustomMaterial
import com.example.data.model.Project
import com.example.data.model.ProjectItem
import com.example.data.model.TeamUser
import com.example.data.model.WorkAlert
import com.example.data.preset.TeamStorePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoogleDriveBackupManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("drive_backup_prefs", Context.MODE_PRIVATE)
    private val teamStorePrefs = TeamStorePreferences(context)

    companion object {
        const val BACKUP_FILE_NAME = "plumber_app_backup.json"
        const val PREF_LAST_BACKUP_TIME = "last_backup_time"
        const val PREF_LAST_BACKUP_STATUS = "last_backup_status"
        const val PREF_GOOGLE_ACCOUNT = "google_account"
    }

    var lastBackupTime: String
        get() = prefs.getString(PREF_LAST_BACKUP_TIME, "لم يتم القيام بنسخ احتياطي بعد") ?: "لم يتم القيام بنسخ احتياطي بعد"
        private set(value) = prefs.edit().putString(PREF_LAST_BACKUP_TIME, value).apply()

    var lastBackupStatus: String
        get() = prefs.getString(PREF_LAST_BACKUP_STATUS, "جاهز للنسخ الاحتياطي") ?: "جاهز للنسخ الاحتياطي"
        private set(value) = prefs.edit().putString(PREF_LAST_BACKUP_STATUS, value).apply()

    var connectedAccount: String
        get() = prefs.getString(PREF_GOOGLE_ACCOUNT, "غير متصل بحساب Google") ?: "غير متصل بحساب Google"
        set(value) = prefs.edit().putString(PREF_GOOGLE_ACCOUNT, value).apply()

    /**
     * Serializes all local data to a JSON string.
     */
    suspend fun createFullBackupJson(): String = withContext(Dispatchers.IO) {
        val db = PlumberDatabase.getDatabase(context)

        val projects = db.projectDao().getAllProjectsDirect()
        val items = db.projectItemDao().getAllProjectItemsDirect()
        val customMaterials = db.customMaterialDao().getAllCustomMaterialsDirect()
        val appointments = db.appointmentDao().getAllAppointmentsDirect()
        val workAlerts = db.workAlertDao().getAllWorkAlertsDirect()
        val teamUsers = db.teamUserDao().getAllTeamUsersDirect()

        val rootJson = JSONObject()
        rootJson.put("version", 1)
        rootJson.put("timestamp", System.currentTimeMillis())
        rootJson.put("created_at_str", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        // Settings / Prefs
        val settingsJson = JSONObject()
        settingsJson.put("storeName", teamStorePrefs.storeName)
        settingsJson.put("storePhone", teamStorePrefs.storePhone)
        settingsJson.put("storeWhatsapp", teamStorePrefs.storeWhatsapp)
        settingsJson.put("activeWorker", teamStorePrefs.activeWorker)
        settingsJson.put("workersList", JSONArray(teamStorePrefs.getWorkersList()))
        rootJson.put("settings", settingsJson)

        // Projects
        val projectsArray = JSONArray()
        projects.forEach { p ->
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("title", p.title)
            pObj.put("clientName", p.clientName)
            pObj.put("location", p.location)
            pObj.put("notes", p.notes)
            pObj.put("workTypeKey", p.workTypeKey)
            pObj.put("workTypeNameAr", p.workTypeNameAr)
            pObj.put("createdAt", p.createdAt)
            pObj.put("updatedAt", p.updatedAt)
            projectsArray.put(pObj)
        }
        rootJson.put("projects", projectsArray)

        // Project Items
        val itemsArray = JSONArray()
        items.forEach { item ->
            val iObj = JSONObject()
            iObj.put("id", item.id)
            iObj.put("projectId", item.projectId)
            iObj.put("materialKey", item.materialKey)
            iObj.put("materialNameAr", item.materialNameAr)
            iObj.put("category", item.category)
            iObj.put("size", item.size)
            iObj.put("quantity", item.quantity)
            iObj.put("unit", item.unit)
            iObj.put("isPurchased", item.isPurchased)
            iObj.put("notes", item.notes)
            iObj.put("createdAt", item.createdAt)
            itemsArray.put(iObj)
        }
        rootJson.put("projectItems", itemsArray)

        // Custom Materials
        val customMatArray = JSONArray()
        customMaterials.forEach { cm ->
            val cmObj = JSONObject()
            cmObj.put("id", cm.id)
            cmObj.put("nameAr", cm.nameAr)
            cmObj.put("nameFr", cm.nameFr)
            cmObj.put("category", cm.category)
            cmObj.put("defaultSize", cm.defaultSize)
            cmObj.put("defaultUnit", cm.defaultUnit)
            cmObj.put("defaultPrice", cm.defaultPrice)
            cmObj.put("iconType", cm.iconType)
            cmObj.put("imageUri", cm.imageUri ?: "")
            cmObj.put("notes", cm.notes)
            cmObj.put("createdAt", cm.createdAt)
            customMatArray.put(cmObj)
        }
        rootJson.put("customMaterials", customMatArray)

        // Appointments
        val apptArray = JSONArray()
        appointments.forEach { appt ->
            val aObj = JSONObject()
            aObj.put("id", appt.id)
            aObj.put("serviceType", appt.serviceType)
            aObj.put("customerName", appt.customerName)
            aObj.put("phoneNumber", appt.phoneNumber)
            aObj.put("address", appt.address)
            aObj.put("scheduledTime", appt.scheduledTime)
            aObj.put("notes", appt.notes)
            aObj.put("status", appt.status)
            aObj.put("createdAt", appt.createdAt)
            apptArray.put(aObj)
        }
        rootJson.put("appointments", apptArray)

        // Work Alerts
        val alertsArray = JSONArray()
        workAlerts.forEach { alert ->
            val waObj = JSONObject()
            waObj.put("id", alert.id)
            waObj.put("alertType", alert.alertType)
            waObj.put("senderName", alert.senderName)
            waObj.put("recipientRole", alert.recipientRole)
            waObj.put("title", alert.title)
            waObj.put("details", alert.details)
            waObj.put("projectName", alert.projectName)
            waObj.put("status", alert.status)
            waObj.put("timestamp", alert.timestamp)
            alertsArray.put(waObj)
        }
        rootJson.put("workAlerts", alertsArray)

        // Team Users
        val usersArray = JSONArray()
        teamUsers.forEach { user ->
            val uObj = JSONObject()
            uObj.put("uid", user.uid)
            uObj.put("name", user.name)
            uObj.put("phone", user.phone)
            uObj.put("email", user.email)
            uObj.put("password", user.password)
            uObj.put("role", user.role)
            uObj.put("active", user.active)
            usersArray.put(uObj)
        }
        rootJson.put("teamUsers", usersArray)

        return@withContext rootJson.toString(2)
    }

    /**
     * Writes the backup JSON string to an output stream (e.g. Google Drive SAF Uri).
     */
    suspend fun writeBackupToStream(outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = createFullBackupJson()
            outputStream.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
                stream.flush()
            }
            val formattedTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            lastBackupTime = formattedTime
            lastBackupStatus = "تم الحفظ بنجاح في Google Drive ✅"
            true
        } catch (e: Exception) {
            e.printStackTrace()
            lastBackupStatus = "فشل الحفظ: ${e.localizedMessage}"
            false
        }
    }

    /**
     * Restores database data from a JSON input stream (e.g. imported from Google Drive).
     */
    suspend fun restoreBackupFromStream(inputStream: InputStream): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val jsonString = inputStream.use { it.bufferedReader(Charsets.UTF_8).readText() }
            val rootJson = JSONObject(jsonString)

            val db = PlumberDatabase.getDatabase(context)

            // Settings
            if (rootJson.has("settings")) {
                val sObj = rootJson.getJSONObject("settings")
                if (sObj.has("storeName")) teamStorePrefs.storeName = sObj.getString("storeName")
                if (sObj.has("storePhone")) teamStorePrefs.storePhone = sObj.getString("storePhone")
                if (sObj.has("storeWhatsapp")) teamStorePrefs.storeWhatsapp = sObj.getString("storeWhatsapp")
                if (sObj.has("activeWorker")) teamStorePrefs.activeWorker = sObj.getString("activeWorker")
                if (sObj.has("workersList")) {
                    val wArray = sObj.getJSONArray("workersList")
                    val list = mutableListOf<String>()
                    for (i in 0 until wArray.length()) {
                        list.add(wArray.getString(i))
                    }
                    if (list.isNotEmpty()) teamStorePrefs.saveWorkersList(list)
                }
            }

            var itemCount = 0

            // Projects
            if (rootJson.has("projects")) {
                val pArray = rootJson.getJSONArray("projects")
                for (i in 0 until pArray.length()) {
                    val p = pArray.getJSONObject(i)
                    val project = Project(
                        id = p.optLong("id", 0),
                        title = p.optString("title", "مشروع مسترجع"),
                        clientName = p.optString("clientName", ""),
                        location = p.optString("location", ""),
                        notes = p.optString("notes", ""),
                        workTypeKey = p.optString("workTypeKey", "GENERAL"),
                        workTypeNameAr = p.optString("workTypeNameAr", "عام"),
                        createdAt = p.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = p.optLong("updatedAt", System.currentTimeMillis())
                    )
                    db.projectDao().insertProject(project)
                    itemCount++
                }
            }

            // Project Items
            if (rootJson.has("projectItems")) {
                val itemsArray = rootJson.getJSONArray("projectItems")
                for (i in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(i)
                    val item = ProjectItem(
                        id = itemObj.optLong("id", 0),
                        projectId = itemObj.optLong("projectId", 0),
                        materialKey = itemObj.optString("materialKey", ""),
                        materialNameAr = itemObj.optString("materialNameAr", ""),
                        category = itemObj.optString("category", ""),
                        size = itemObj.optString("size", ""),
                        quantity = itemObj.optDouble("quantity", 1.0),
                        unit = itemObj.optString("unit", "قطعة"),
                        isPurchased = itemObj.optBoolean("isPurchased", false),
                        notes = itemObj.optString("notes", ""),
                        createdAt = itemObj.optLong("createdAt", System.currentTimeMillis())
                    )
                    db.projectItemDao().insertItem(item)
                }
            }

            // Custom Materials
            if (rootJson.has("customMaterials")) {
                val cmArray = rootJson.getJSONArray("customMaterials")
                for (i in 0 until cmArray.length()) {
                    val cmObj = cmArray.getJSONObject(i)
                    val cm = CustomMaterial(
                        id = cmObj.optLong("id", 0),
                        nameAr = cmObj.optString("nameAr", ""),
                        nameFr = cmObj.optString("nameFr", ""),
                        category = cmObj.optString("category", "PPR"),
                        defaultSize = cmObj.optString("defaultSize", "25mm"),
                        defaultUnit = cmObj.optString("defaultUnit", "قطعة"),
                        defaultPrice = cmObj.optDouble("defaultPrice", 0.0),
                        iconType = cmObj.optString("iconType", "custom"),
                        imageUri = cmObj.optString("imageUri").ifEmpty { null },
                        notes = cmObj.optString("notes", ""),
                        createdAt = cmObj.optLong("createdAt", System.currentTimeMillis())
                    )
                    db.customMaterialDao().insertCustomMaterial(cm)
                }
            }

            // Appointments
            if (rootJson.has("appointments")) {
                val apptArray = rootJson.getJSONArray("appointments")
                for (i in 0 until apptArray.length()) {
                    val aObj = apptArray.getJSONObject(i)
                    val appt = Appointment(
                        id = aObj.optLong("id", 0),
                        serviceType = aObj.optString("serviceType", "تركيب"),
                        customerName = aObj.optString("customerName", "عميل"),
                        phoneNumber = aObj.optString("phoneNumber", ""),
                        address = aObj.optString("address", ""),
                        scheduledTime = aObj.optString("scheduledTime", ""),
                        notes = aObj.optString("notes", ""),
                        status = aObj.optString("status", "قيد الانتظار"),
                        createdAt = aObj.optLong("createdAt", System.currentTimeMillis())
                    )
                    db.appointmentDao().insertAppointment(appt)
                }
            }

            // Work Alerts
            if (rootJson.has("workAlerts")) {
                val alertsArray = rootJson.getJSONArray("workAlerts")
                for (i in 0 until alertsArray.length()) {
                    val waObj = alertsArray.getJSONObject(i)
                    val alert = WorkAlert(
                        id = waObj.optLong("id", 0),
                        alertType = waObj.optString("alertType", "طلبية"),
                        senderName = waObj.optString("senderName", "عام"),
                        recipientRole = waObj.optString("recipientRole", "المدير"),
                        title = waObj.optString("title", "تنبيه ورشة"),
                        details = waObj.optString("details", ""),
                        projectName = waObj.optString("projectName", ""),
                        status = waObj.optString("status", "جديد"),
                        timestamp = waObj.optLong("timestamp", System.currentTimeMillis())
                    )
                    db.workAlertDao().insertAlert(alert)
                }
            }

            // Team Users
            if (rootJson.has("teamUsers")) {
                val uArray = rootJson.getJSONArray("teamUsers")
                for (i in 0 until uArray.length()) {
                    val uObj = uArray.getJSONObject(i)
                    val user = TeamUser(
                        uid = uObj.optString("uid", "user_${i}"),
                        name = uObj.optString("name", ""),
                        phone = uObj.optString("phone", ""),
                        email = uObj.optString("email", ""),
                        password = uObj.optString("password", ""),
                        role = uObj.optString("role", "WORKER"),
                        active = uObj.optBoolean("active", true)
                    )
                    db.teamUserDao().insertOrUpdateUser(user)
                }
            }

            val formattedTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            lastBackupStatus = "تم استعادة البيانات بنجاح في $formattedTime ✅"
            Result.success(itemCount)
        } catch (e: Exception) {
            e.printStackTrace()
            lastBackupStatus = "فشل استعادة البيانات: ${e.localizedMessage}"
            Result.failure(e)
        }
    }

    /**
     * Writes backup directly to local app storage cache as quick backup file.
     */
    suspend fun createQuickLocalBackupFile(): Uri? = withContext(Dispatchers.IO) {
        try {
            val file = java.io.File(context.cacheDir, BACKUP_FILE_NAME)
            file.writeText(createFullBackupJson(), Charsets.UTF_8)
            val formattedTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            lastBackupTime = formattedTime
            lastBackupStatus = "نسخة احتياطية محليّة جاهزة للرفع على Drive ☁️"
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
