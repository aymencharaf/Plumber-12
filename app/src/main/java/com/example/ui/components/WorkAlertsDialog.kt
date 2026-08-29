package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WorkAlert
import com.example.ui.viewmodel.PlumberViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkAlertsDialog(
    viewModel: PlumberViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val alerts by viewModel.allWorkAlerts.collectAsStateWithLifecycle()
    val projects by viewModel.allProjects.collectAsStateWithLifecycle()
    val activeWorker by viewModel.activeWorker.collectAsStateWithLifecycle()
    val managerName by viewModel.managerName.collectAsStateWithLifecycle()
    val storeWhatsapp by viewModel.storeWhatsapp.collectAsStateWithLifecycle()
    val storePhone by viewModel.storePhone.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: التنبيهات الحالية, 1: إرسال منبه جديد

    val alertTypes = listOf(
        "طلبية" to "📦 طلبية مواد",
        "تحديد موعد" to "📅 تحديد موعد",
        "إرسال لوازم" to "🚚 إرسال اللوازم",
        "تأكيد استلام" to "✅ تأكيد الاستلام"
    )

    var selectedType by remember { mutableStateOf(alertTypes[0].first) }
    var senderRole by remember { mutableStateOf("عامل") } // عامل أو مدير
    var senderNameInput by remember { mutableStateOf(if (senderRole == "عامل") activeWorker else managerName.ifBlank { "المدير أيمين" }) }
    var alertTitle by remember { mutableStateOf("") }
    var alertDetails by remember { mutableStateOf("") }
    var selectedProjectName by remember { mutableStateOf("") }

    val unreadCount = alerts.count { it.status == "جديد" }
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFDC2626).copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "منبه الرسائل (العامل ↔ المدير)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                if (unreadCount > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFDC2626)
                                    ) {
                                        Text(
                                            text = "$unreadCount جديد",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "طلبيات، مواعيد، إرسال اللوازم وتأكيد الاستلام",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs (التنبيهات الحالية | إرسال منبه جديد)
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    indicator = {},
                    divider = {},
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Text(
                                text = "🔔 التنبيهات والإشعارات (${alerts.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Text(
                                text = "➕ إرسال تنبيه جديد",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Contents
                if (activeTab == 0) {
                    // TAB 0: List of Alerts
                    if (alerts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("🔔", fontSize = 42.sp)
                                Text(
                                    text = "لا توجد تنبيهات حالية بين العامل والمدير",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { activeTab = 1 },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إرسال أول تنبيه الآن")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(alerts, key = { it.id }) { alert ->
                                WorkAlertItemCard(
                                    alert = alert,
                                    dateFormat = dateFormat,
                                    onStatusToggle = {
                                        val newStatus = if (alert.status == "تم الاستلام/الموافقة") "تمت المعاينة" else "تم الاستلام/الموافقة"
                                        viewModel.updateWorkAlertStatus(alert, newStatus)
                                        Toast.makeText(context, "تم تحديث الحالة كـ: $newStatus", Toast.LENGTH_SHORT).show()
                                    },
                                    onDelete = {
                                        viewModel.deleteWorkAlert(alert.id)
                                        Toast.makeText(context, "تم حذف التنبيه", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // TAB 1: New Alert Form
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            // 1. Role / Sender selection
                            Text("المرسِل من صفة:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf("عامل" to "👷‍♂️ العامل الميداني", "مدير" to "👔 المدير / الورشة").forEach { (roleKey, roleLabel) ->
                                    val isSel = senderRole == roleKey
                                    Surface(
                                        onClick = {
                                            senderRole = roleKey
                                            senderNameInput = if (roleKey == "عامل") activeWorker else managerName.ifBlank { "المدير أيمين" }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = roleLabel,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .padding(vertical = 10.dp)
                                                .wrapContentWidth(Alignment.CenterHorizontally)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            // Sender Name Input
                            OutlinedTextField(
                                value = senderNameInput,
                                onValueChange = { senderNameInput = it },
                                label = { Text("اسم المرسل") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        item {
                            // 2. Select Alert Category
                            Text("نوع منبه الرسالة:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                alertTypes.chunked(2).forEach { rowPair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowPair.forEach { (typeKey, typeLabel) ->
                                            val isSelected = selectedType == typeKey
                                            val (bgColor, txtColor) = when (typeKey) {
                                                "طلبية" -> Color(0xFFEFF6FF) to Color(0xFF1D4ED8)
                                                "تحديد موعد" -> Color(0xFFF5F3FF) to Color(0xFF6D28D9)
                                                "إرسال لوازم" -> Color(0xFFECFDF5) to Color(0xFF047857)
                                                else -> Color(0xFFFEF2F2) to Color(0xFFB91C1C)
                                            }

                                            Surface(
                                                onClick = { selectedType = typeKey },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) txtColor else bgColor,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, txtColor),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = typeLabel,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (isSelected) Color.White else txtColor,
                                                    modifier = Modifier
                                                        .padding(vertical = 12.dp)
                                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Project association (Optional)
                            if (projects.isNotEmpty()) {
                                Text("ربط بمشروع (اختياري):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        FilterChip(
                                            selected = selectedProjectName.isEmpty(),
                                            onClick = { selectedProjectName = "" },
                                            label = { Text("عام / بدون مشروع") },
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                    }
                                    items(projects) { proj ->
                                        FilterChip(
                                            selected = selectedProjectName == proj.title,
                                            onClick = { selectedProjectName = proj.title },
                                            label = { Text(proj.title) },
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            // Title / Subject
                            OutlinedTextField(
                                value = alertTitle,
                                onValueChange = { alertTitle = it },
                                label = { Text("عنوان التنبيه أو الطلبية") },
                                placeholder = {
                                    Text(
                                        when (selectedType) {
                                            "طلبية" -> "مثال: طلب أنبوب PPR 25 ومحابس 3/4"
                                            "تحديد موعد" -> "مثال: موعد تسليم مشروع فيلا السلام"
                                            "إرسال لوازم" -> "مثال: شحن محابس وخلاطات مع السائق"
                                            else -> "مثال: استلام كامل لوازم ورشة الشوفاج"
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        item {
                            // Details Text Field
                            OutlinedTextField(
                                value = alertDetails,
                                onValueChange = { alertDetails = it },
                                label = { Text("تفاصيل الرسالة أو كميات اللوازم") },
                                placeholder = { Text("أدخل الكميات المطلوبة، تفاصيل الموعد أو ملاحظات التسليم والاستلام...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 3,
                                maxLines = 5
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (alertTitle.isBlank()) {
                                            Toast.makeText(context, "يرجى كتابة عنوان التنبيه", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        val recipient = if (senderRole == "عامل") "المدير" else "العامل"
                                        viewModel.addWorkAlert(
                                            alertType = selectedType,
                                            senderName = senderNameInput.ifBlank { "العامل" },
                                            recipientRole = recipient,
                                            title = alertTitle,
                                            details = alertDetails,
                                            projectName = selectedProjectName
                                        )

                                        Toast.makeText(context, "تم إرسال المنبه بنجاح! 🔔", Toast.LENGTH_SHORT).show()
                                        alertTitle = ""
                                        alertDetails = ""
                                        activeTab = 0
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("حفظ وتنبيه بالتطبيق 🔔", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (alertTitle.isBlank()) {
                                            Toast.makeText(context, "يرجى كتابة عنوان التنبيه", Toast.LENGTH_SHORT).show()
                                            return@OutlinedButton
                                        }

                                        val recipient = if (senderRole == "عامل") "المدير" else "العامل"
                                        val iconEmoji = when (selectedType) {
                                            "طلبية" -> "📦"
                                            "تحديد موعد" -> "📅"
                                            "إرسال لوازم" -> "🚚"
                                            else -> "✅"
                                        }

                                        val message = """
                                            $iconEmoji *منبه رسالة عمل: $selectedType*
                                            ---------------------------
                                            👤 *المرسل:* $senderNameInput ($senderRole)
                                            🎯 *الموجه إليه:* $recipient
                                            📌 *العنوان:* $alertTitle
                                            ${if (selectedProjectName.isNotBlank()) "🏗️ *المشروع:* $selectedProjectName\n" else ""}
                                            📝 *التفاصيل:* ${alertDetails.ifBlank { "لا توجد تفاصيل إضافية" }}
                                        """.trimIndent()

                                        // Also save locally
                                        viewModel.addWorkAlert(
                                            alertType = selectedType,
                                            senderName = senderNameInput.ifBlank { "العامل" },
                                            recipientRole = recipient,
                                            title = alertTitle,
                                            details = alertDetails,
                                            projectName = selectedProjectName
                                        )

                                        val phone = storeWhatsapp.ifBlank { storePhone }
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "تعذر فتح الواتساب", Toast.LENGTH_SHORT).show()
                                        }
                                        alertTitle = ""
                                        alertDetails = ""
                                        activeTab = 0
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("إرسال واتساب 💬", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkAlertItemCard(
    alert: WorkAlert,
    dateFormat: SimpleDateFormat,
    onStatusToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val (typeBg, typeColor, iconEmoji) = when (alert.alertType) {
        "طلبية" -> Triple(Color(0xFFEFF6FF), Color(0xFF1E40AF), "📦")
        "تحديد موعد" -> Triple(Color(0xFFF5F3FF), Color(0xFF6D28D9), "📅")
        "إرسال لوازم" -> Triple(Color(0xFFECFDF5), Color(0xFF047857), "🚚")
        else -> Triple(Color(0xFFFEF2F2), Color(0xFFB91C1C), "✅")
    }

    val isConfirmed = alert.status == "تم الاستلام/الموافقة"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConfirmed) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isConfirmed) Color(0xFF86EFAC) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = typeBg
                    ) {
                        Text(
                            text = "$iconEmoji ${alert.alertType}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isConfirmed) Color(0xFF16A34A).copy(alpha = 0.15f) else Color(0xFFDC2626).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = alert.status,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isConfirmed) Color(0xFF15803D) else Color(0xFFB91C1C),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormat.format(Date(alert.timestamp)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Title
            Text(
                text = alert.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            // Sender & Recipient Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "من: ${alert.senderName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "إلى: ${alert.recipientRole}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (alert.projectName.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "🏗️ المشروع: ${alert.projectName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (alert.details.isNotBlank()) {
                Text(
                    text = alert.details,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onStatusToggle) {
                    Icon(
                        imageVector = if (isConfirmed) Icons.Default.CheckCircle else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isConfirmed) "إلغاء التأكيد" else "تأكيد الاستلام / الموافقة ✅",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = {
                    val message = """
                        📌 *متابعة منبه رسالة: ${alert.alertType}*
                        ---------------------------
                        👤 *المرسل:* ${alert.senderName}
                        📌 *العنوان:* ${alert.title}
                        📝 *التفاصيل:* ${alert.details}
                        📊 *الحالة:* ${alert.status}
                    """.trimIndent()

                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "تعذر إرسال التفاصيل", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("إعادة مشاركة 💬", fontSize = 12.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
