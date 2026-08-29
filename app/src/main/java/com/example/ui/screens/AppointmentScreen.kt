package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Appointment
import com.example.ui.viewmodel.PlumberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentScreen(
    viewModel: PlumberViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val appointments by viewModel.allAppointments.collectAsStateWithLifecycle()
    val storeWhatsapp by viewModel.storeWhatsapp.collectAsStateWithLifecycle()
    val storePhone by viewModel.storePhone.collectAsStateWithLifecycle()

    val serviceOptions = listOf("فحص", "صيانة", "تركيب", "أعمال أخرى")
    var selectedServiceType by remember { mutableStateOf(serviceOptions[0]) }

    var customerName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var scheduledTime by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var selectedFilterStatus by remember { mutableStateOf("الكل") }

    val filteredAppointments = remember(appointments, selectedFilterStatus) {
        when (selectedFilterStatus) {
            "قيد الانتظار" -> appointments.filter { it.status == "قيد الانتظار" }
            "تم الإنجاز" -> appointments.filter { it.status == "تم الإنجاز" }
            else -> appointments
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📅 ", fontSize = 20.sp)
                        Text("طلب موعد وصيانة", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // New Appointment Form Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFD97706).copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFFD97706))
                                }
                            }
                            Column {
                                Text(
                                    text = "تسجيل طلب موعد جديد",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "اختر نوع الخدمة وأدخل بيانات الزبون والوقت المحدد",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Divider()

                        // 1. Service Type Selection (فحص | صيانة | تركيب | أعمال أخرى)
                        Text(
                            text = "نوع الخدمة المطلوبة:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            serviceOptions.forEach { service ->
                                val isSelected = service == selectedServiceType
                                val (bgColor, textColor, iconEmoji) = when (service) {
                                    "فحص" -> Triple(Color(0xFFEFF6FF), Color(0xFF1E40AF), "🔍")
                                    "صيانة" -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), "🛠️")
                                    "تركيب" -> Triple(Color(0xFFECFDF5), Color(0xFF065F46), "🚰")
                                    else -> Triple(Color(0xFFF3E8FF), Color(0xFF6B21A8), "📋")
                                }

                                Surface(
                                    onClick = { selectedServiceType = service },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) textColor else bgColor,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = textColor
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = iconEmoji, fontSize = 18.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = service,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color.White else textColor
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Customer Name Field
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("اسم الزبون") },
                            placeholder = { Text("مثال: عبد القادر الجزائري") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // 3. Phone Number Field
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("رقم الهاتف") },
                            placeholder = { Text("مثال: 0661234567") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // 4. Address Field
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("العنوان / الموقع") },
                            placeholder = { Text("مثال: حي السلام - العمارة 12 رقم 4") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // 5. Scheduled Date & Time Field
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = scheduledTime,
                                onValueChange = { scheduledTime = it },
                                label = { Text("الوقت والتاريخ المحدد") },
                                placeholder = { Text("مثال: غداً الساعة 10:00 صباحاً") },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            // Quick Time Suggestions
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("اليوم 10:00", "اليوم 14:00", "غداً 09:00", "غداً 15:00", "نهاية الأسبوع")) { timeChip ->
                                    FilterChip(
                                        selected = scheduledTime == timeChip,
                                        onClick = { scheduledTime = timeChip },
                                        label = { Text(timeChip, fontSize = 12.sp) },
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                }
                            }
                        }

                        // 6. Notes Field (Optional)
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("ملاحظات / تفاصيل أخرى (اختياري)") },
                            placeholder = { Text("مثال: تسرب في أنبوب الحمام أو صيانة الشوفاج") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Submit Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (customerName.isBlank() || phoneNumber.isBlank() || scheduledTime.isBlank()) {
                                        Toast.makeText(context, "الرجاء إدخال اسم الزبون، رقم الهاتف، والوقت المحدد", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.addAppointment(
                                        serviceType = selectedServiceType,
                                        customerName = customerName,
                                        phoneNumber = phoneNumber,
                                        address = address,
                                        scheduledTime = scheduledTime,
                                        notes = notes
                                    )
                                    Toast.makeText(context, "تم تسجيل وتأكيد طلب الموعد بنجاح! 📅✅", Toast.LENGTH_SHORT).show()
                                    // Reset input fields
                                    customerName = ""
                                    phoneNumber = ""
                                    address = ""
                                    scheduledTime = ""
                                    notes = ""
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حفظ الموعد 📅", fontWeight = FontWeight.Bold)
                            }

                            // Share via WhatsApp Button
                            OutlinedButton(
                                onClick = {
                                    if (customerName.isBlank() || phoneNumber.isBlank()) {
                                        Toast.makeText(context, "أدخل اسم الزبون ورقم الهاتف لمشاركته عبر الواتساب", Toast.LENGTH_SHORT).show()
                                        return@OutlinedButton
                                    }
                                    val message = """
                                        📌 *طلب موعد جديد - ورشة السباكة*
                                        ---------------------------
                                        🛠️ *نوع الخدمة:* $selectedServiceType
                                        👤 *الزبون:* $customerName
                                        📞 *الهاتف:* $phoneNumber
                                        📍 *العنوان:* ${address.ifBlank { "غير محدد" }}
                                        ⏰ *الوقت المحدد:* $scheduledTime
                                        📝 *ملاحظات:* ${notes.ifBlank { "لا يوجد" }}
                                    """.trimIndent()

                                    val targetPhone = storeWhatsapp.ifBlank { storePhone }
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("https://api.whatsapp.com/send?phone=$targetPhone&text=${Uri.encode(message)}")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "تعذر فتح تطبيق الواتساب", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("مشاركة واتساب 💬", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Saved Appointments Header & Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "جدول المواعيد والطلبات (${filteredAppointments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("الكل", "قيد الانتظار", "تم الإنجاز").forEach { filter ->
                            FilterChip(
                                selected = selectedFilterStatus == filter,
                                onClick = { selectedFilterStatus = filter },
                                label = { Text(filter, fontSize = 11.sp) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
            }

            // Appointments List
            if (filteredAppointments.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🗓️", fontSize = 32.sp)
                            Text(
                                text = "لا توجد مواعيد مسجلة حالياً",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "أضف موعداً جديداً من النموذج في الأعلى ليظهر في الجدول.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredAppointments, key = { it.id }) { appt ->
                    AppointmentCardItem(
                        appointment = appt,
                        onStatusToggle = {
                            val nextStatus = if (appt.status == "تم الإنجاز") "قيد الانتظار" else "تم الإنجاز"
                            viewModel.updateAppointmentStatus(appt, nextStatus)
                            Toast.makeText(context, "تم تحديث حالة الموعد إلى: $nextStatus", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = {
                            viewModel.deleteAppointment(appt.id)
                            Toast.makeText(context, "تم حذف الموعد", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AppointmentCardItem(
    appointment: Appointment,
    onStatusToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val (badgeBg, badgeText, badgeIcon) = when (appointment.serviceType) {
        "فحص" -> Triple(Color(0xFFDBEAFE), Color(0xFF1E40AF), "🔍")
        "صيانة" -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), "🛠️")
        "تركيب" -> Triple(Color(0xFFD1FAE5), Color(0xFF065F46), "🚰")
        else -> Triple(Color(0xFFF3E8FF), Color(0xFF6B21A8), "📋")
    }

    val isCompleted = appointment.status == "تم الإنجاز"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCompleted) Color(0xFF86EFAC) else MaterialTheme.colorScheme.outlineVariant
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
                        color = badgeBg
                    ) {
                        Text(
                            text = "$badgeIcon ${appointment.serviceType}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = badgeText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCompleted) Color(0xFF16A34A).copy(alpha = 0.2f) else Color(0xFFD97706).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = appointment.status,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isCompleted) Color(0xFF15803D) else Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Customer Name & Phone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = appointment.customerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "📞 ${appointment.phoneNumber}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Call Action Button
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${appointment.phoneNumber}")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .background(Color(0xFF2563EB).copy(alpha = 0.1f), CircleShape)
                        .size(38.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = Color(0xFF2563EB))
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Time & Location Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⏰ ", fontSize = 14.sp)
                    Text(
                        text = "الوقت: ${appointment.scheduledTime}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (appointment.address.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📍 ", fontSize = 14.sp)
                        Text(
                            text = appointment.address,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (appointment.notes.isNotBlank()) {
                Text(
                    text = "📝 ملاحظات: ${appointment.notes}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onStatusToggle) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCompleted) "تعليم كـ قيد الانتظار" else "تحديد كـ تم الإنجاز ✅",
                        fontSize = 12.sp
                    )
                }

                TextButton(onClick = {
                    val message = """
                        📌 *تذكير بموعد - ورشة السباكة*
                        ---------------------------
                        🛠️ *نوع الخدمة:* ${appointment.serviceType}
                        👤 *الزبون:* ${appointment.customerName}
                        ⏰ *الموعد:* ${appointment.scheduledTime}
                        📍 *العنوان:* ${appointment.address}
                    """.trimIndent()

                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://api.whatsapp.com/send?phone=${appointment.phoneNumber}&text=${Uri.encode(message)}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "تعذر فتح الواتساب", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("إرسال تذكير 💬", fontSize = 12.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
