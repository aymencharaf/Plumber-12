package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.preset.TeamStorePreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.UUID

/**
 * مدير رمز المزامنة (SyncCode Manager)
 * يختص بإدارة وربط رمز المزامنة الخاص بالورشة بين تطبيق المدير وتطبيق العمال مع Cloud Firestore.
 */
object SyncCodeManager {

    private const val TAG = "SyncCodeManager"
    private const val PREF_SYNC_CODE_KEY = "current_sync_code"
    private const val DEFAULT_SYNC_CODE = ""

    /**
     * قراءة رمز المزامنة الحالي المخزن على الجهاز
     */
    fun getSyncCode(context: Context): String {
        val prefs = context.getSharedPreferences("sync_code_prefs", Context.MODE_PRIVATE)
        return prefs.getString(PREF_SYNC_CODE_KEY, DEFAULT_SYNC_CODE) ?: DEFAULT_SYNC_CODE
    }

    /**
     * حفظ رمز مزامنة جديد على الجهاز
     */
    fun setSyncCode(context: Context, code: String) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isNotBlank()) {
            val prefs = context.getSharedPreferences("sync_code_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString(PREF_SYNC_CODE_KEY, cleanCode).apply()
            Log.d(TAG, "SyncCode saved locally: $cleanCode")
        }
    }

    /**
     * توليد رمز مزامنة فريد للورشة الجديدة (مثال: WORKSHOP-8A3F)
     */
    fun generateNewSyncCode(): String {
        val randomSuffix = UUID.randomUUID().toString().take(4).uppercase()
        return "WORKSHOP-$randomSuffix"
    }

    /**
     * ربط وحفظ رمز المزامنة للورشة في Cloud Firestore وتحفظ معها بيانات المتجر
     */
    fun registerSyncCodeToFirestore(
        context: Context,
        syncCode: String,
        workshopName: String,
        managerPhone: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val cleanCode = syncCode.trim().uppercase()
        if (cleanCode.isBlank()) {
            onComplete(false, "رمز المزامنة فارغ")
            return
        }

        try {
            val db = FirebaseFirestore.getInstance()
            val data = mapOf(
                "syncCode" to cleanCode,
                "workshopName" to workshopName,
                "managerPhone" to managerPhone,
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection("workshops").document(cleanCode)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    setSyncCode(context, cleanCode)
                    Log.d(TAG, "SyncCode $cleanCode registered in Firestore successfully")
                    onComplete(true, "تم تسجيل وربط رمز المزامنة بنجاح $cleanCode")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to register sync code in Firestore", e)
                    onComplete(false, "تعذر الربط بـ Firestore: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore error", e)
            onComplete(false, "خطأ في الاتصال بـ Firebase: ${e.localizedMessage}")
        }
    }

    /**
     * التحقق من وجود رمز المزامنة في Cloud Firestore (ربط تطبيق العامل بورشة المدير)
     */
    fun verifyAndConnectWorkerSyncCode(
        context: Context,
        inputSyncCode: String,
        workerPhone: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanCode = inputSyncCode.trim().uppercase()
        if (cleanCode.isBlank()) {
            onResult(false, "الرجاء إدخال رمز المزامنة")
            return
        }

        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("workshops").document(cleanCode).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        setSyncCode(context, cleanCode)
                        
                        // إضافة بيانات العامل ضمن ورشة المدير في Firestore
                        val workerData = mapOf(
                            "phone" to workerPhone,
                            "joinedAt" to System.currentTimeMillis(),
                            "status" to "CONNECTED"
                        )
                        db.collection("workshops").document(cleanCode)
                            .collection("connected_workers").document(workerPhone.ifBlank { "worker_${System.currentTimeMillis()}" })
                            .set(workerData, SetOptions.merge())

                        onResult(true, "تم الربط بورشة المدير بنجاح 🎉 (الرمز: $cleanCode)")
                    } else {
                        // إذا لم تكن الورشة مجدولة سابقاً، يتم إنشاؤها لربط الأجهزة
                        setSyncCode(context, cleanCode)
                        registerSyncCodeToFirestore(context, cleanCode, "ورشة جديدة", workerPhone) { success, msg ->
                            onResult(true, "تمت إضافة وتفعيل رمز المزامنة الجديد ($cleanCode)")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // في حالة انقطاع الشبكة، يتم حفظ الرمز محلياً
                    setSyncCode(context, cleanCode)
                    onResult(true, "تم حفظ رمز المزامنة محلياً ($cleanCode)")
                }
        } catch (e: Exception) {
            setSyncCode(context, cleanCode)
            onResult(true, "تم حفظ الرمز محلياً: $cleanCode")
        }
    }
}
