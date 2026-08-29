package com.example.data.preset

import android.content.Context
import android.content.SharedPreferences

class TeamStorePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("team_store_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_STORE_NAME = "store_name"
        private const val KEY_STORE_PHONE = "store_phone"
        private const val KEY_STORE_WHATSAPP = "store_whatsapp"
        private const val KEY_ACTIVE_WORKER = "active_worker"
        private const val KEY_MANAGER_NAME = "manager_name"
        private const val KEY_MANAGER_PIN = "manager_pin"
        private const val KEY_WORKERS_LIST = "workers_list"

        val DEFAULT_WORKERS = listOf(
            "العامل 1: أحمد (تركيبات)",
            "العامل 2: محمد (صيانة)",
            "العامل 3: ياسين (أنابيب PPR/PVC)",
            "العامل 4: مصطفى (تدفئة مركزية)",
            "العامل 5: حمزة (شبكات مياه)",
            "العامل 6: عمر (غاز وسخانات)",
            "العامل 7: خالد (مضخات وتانك)",
            "العامل 8: بلال (تركيبات صحية)",
            "العامل 9: إبراهيم (تسريبات ومغاسل)",
            "العامل 10: صالح (صيانة عامة)"
        )
    }

    var storeName: String
        get() = prefs.getString(KEY_STORE_NAME, "محل السباكة والتجهيزات العامة") ?: "محل السباكة والتجهيزات العامة"
        set(value) = prefs.edit().putString(KEY_STORE_NAME, value).apply()

    var storePhone: String
        get() = prefs.getString(KEY_STORE_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_STORE_PHONE, value).apply()

    var storeWhatsapp: String
        get() = prefs.getString(KEY_STORE_WHATSAPP, "") ?: ""
        set(value) = prefs.edit().putString(KEY_STORE_WHATSAPP, value).apply()

    var activeWorker: String
        get() = prefs.getString(KEY_ACTIVE_WORKER, DEFAULT_WORKERS[0]) ?: DEFAULT_WORKERS[0]
        set(value) = prefs.edit().putString(KEY_ACTIVE_WORKER, value).apply()

    var managerName: String
        get() = prefs.getString(KEY_MANAGER_NAME, "مدير الورشات والإنتاج") ?: "مدير الورشات والإنتاج"
        set(value) = prefs.edit().putString(KEY_MANAGER_NAME, value).apply()

    var managerPin: String
        get() = prefs.getString(KEY_MANAGER_PIN, "1234") ?: "1234"
        set(value) = prefs.edit().putString(KEY_MANAGER_PIN, value).apply()

    fun getWorkersList(): List<String> {
        val saved = prefs.getString(KEY_WORKERS_LIST, null)
        return if (saved.isNullOrEmpty()) {
            DEFAULT_WORKERS
        } else {
            saved.split(";;;").filter { it.isNotBlank() }
        }
    }

    fun saveWorkersList(workers: List<String>) {
        val str = workers.joinToString(";;;")
        prefs.edit().putString(KEY_WORKERS_LIST, str).apply()
    }
}
