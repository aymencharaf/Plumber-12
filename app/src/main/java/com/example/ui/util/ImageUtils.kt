package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {

    fun saveBitmapToFile(context: Context, bitmap: Bitmap): String {
        return try {
            val file = File(context.filesDir, "material_photo_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun copyUriToFile(context: Context, uri: Uri): String {
        return try {
            val file = File(context.filesDir, "material_gallery_${System.currentTimeMillis()}.jpg")
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
