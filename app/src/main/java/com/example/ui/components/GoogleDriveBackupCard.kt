package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backup.GoogleDriveBackupManager
import kotlinx.coroutines.launch

@Composable
fun GoogleDriveBackupCard(
    modifier: Modifier = Modifier,
    onBackupRestored: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val driveManager = remember { GoogleDriveBackupManager(context) }

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf(driveManager.lastBackupStatus) }
    var lastBackupTime by remember { mutableStateOf(driveManager.lastBackupTime) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Launcher for exporting backup JSON to Google Drive (Save to Drive / SAF)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                try {
                    val outputStream = context.contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        val success = driveManager.writeBackupToStream(outputStream)
                        if (success) {
                            statusMessage = "تم حفظ النسخة الاحتياطية بنجاح في Google Drive! ☁️✅"
                            lastBackupTime = driveManager.lastBackupTime
                            Toast.makeText(context, "تم حفظ النسخة الاحتياطية بنجاح! ☁️✅", Toast.LENGTH_LONG).show()
                        } else {
                            statusMessage = "حدث خطأ أثناء كتابة الملف"
                            Toast.makeText(context, "فشل حفظ النسخة الاحتياطية", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    statusMessage = "خطأ: ${e.localizedMessage}"
                    Toast.makeText(context, "خطأ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    // Launcher for importing backup JSON from Google Drive (Open from Drive / SAF)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val result = driveManager.restoreBackupFromStream(inputStream)
                        result.onSuccess { count ->
                            statusMessage = "تمت استعادة البيانات بنجاح ($count مشروع ومحتوى) 📥✅"
                            lastBackupTime = driveManager.lastBackupTime
                            Toast.makeText(context, "تمت استعادة البيانات بنجاح! 📥✅", Toast.LENGTH_LONG).show()
                            onBackupRestored()
                        }.onFailure { err ->
                            statusMessage = "فشلت استعادة البيانات: ${err.localizedMessage}"
                            Toast.makeText(context, "فشلت استعادة البيانات: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    statusMessage = "خطأ: ${e.localizedMessage}"
                    Toast.makeText(context, "خطأ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF4285F4).copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Google Drive",
                                tint = Color(0xFF4285F4),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "النسخ الاحتياطي في Google Drive 📁",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "حفظ واسترجاع كافة بيانات المشاريع واللوازم والطلبيات",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = { showInfoDialog = true }) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "معلومات",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Status Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (lastBackupTime.contains("لم يتم")) Color(0xFFEAB308) else Color(0xFF16A34A),
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تاريخ آخر نسخ احتياطي:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = lastBackupTime,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (isProcessing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("جاري معالجة النسخة الاحتياطية...", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Button 1: Save Backup to Google Drive
                Button(
                    onClick = {
                        val fileName = "plumber_backup_${System.currentTimeMillis() / 1000}.json"
                        createDocumentLauncher.launch(fileName)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ نسخة احتياطية جديدة في Google Drive ☁️", fontWeight = FontWeight.Bold)
                }

                // Button 2: Restore Backup from Google Drive
                OutlinedButton(
                    onClick = {
                        openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("استعادة نسخة احتياطية من Google Drive 📥", fontWeight = FontWeight.Bold)
                }

                // Button 3: Share / Export Backup File directly
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                val uri = driveManager.createQuickLocalBackupFile()
                                isProcessing = false
                                if (uri != null) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - تطبيق السباك")
                                        putExtra(Intent.EXTRA_TEXT, "ملف النسخة الاحتياطية لشريحة السباك والمشاريع واللوازم.")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة أو رفع إلى Google Drive"))
                                } else {
                                    Toast.makeText(context, "فشل إنشاء ملف المشاركة", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة الملف 📤", fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // Info Dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF4285F4))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("كيف يعمل النسخ الاحتياطي؟", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "• حفظ في Google Drive: يتم تصدير ملف كامل يشمل كافة ورشات العمل، قائمة اللوازم والمواد، المواعيد، وإعدادات المحل بصيغة JSON آمنة ومفرزة.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• استعادة البيانات: يمكنك اختيار أي ملف نسخة احتياطية سابقة تم حفظها في حساب Google Drive أو في ذاكرة الهاتف لاسترجاع المشاريع بلمسة واحدة.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• حماية البيانات: تتشفر البيانات وتخزن مباشرة في مساحتك الخاصة على Google Drive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInfoDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("حسناً، فهمت")
                }
            }
        )
    }
}
