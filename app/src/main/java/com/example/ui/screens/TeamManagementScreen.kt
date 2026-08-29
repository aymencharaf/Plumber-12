package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Project
import com.example.ui.viewmodel.PlumberViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamManagementScreen(
    viewModel: PlumberViewModel,
    onBack: (() -> Unit)? = null,
    onNavigateToProjectDetails: (projectId: Long) -> Unit
) {
    val context = LocalContext.current

    val workersList by viewModel.workersList.collectAsState()
    val activeWorker by viewModel.activeWorker.collectAsState()
    val defaultManager by viewModel.managerName.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val storeName by viewModel.storeName.collectAsState()

    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var showEditManagerDialog by remember { mutableStateOf(false) }
    var workerToDelete by remember { mutableStateOf<String?>(null) }
    var selectedWorkerForTasks by remember { mutableStateOf<String?>(null) }
    var paymentTargetProject by remember { mutableStateOf<Project?>(null) }
    var editAssignmentProject by remember { mutableStateOf<Project?>(null) }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إدارة فريق العمل (10 عمال)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddWorkerDialog = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("إضافة عامل جديد", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Header Overview Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Badge,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "طاقم الفنيين والتركيبات",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "التابع لـ: $storeName",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = "${workersList.size} عمال",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Manager info row
                        Surface(
                            onClick = { showEditManagerDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("👨‍💼 ", fontSize = 16.sp)
                                    Text(
                                        text = "المدير المشرف على التعيين والمالية: ",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = defaultManager,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(Icons.Default.Edit, contentDescription = "تعديل المدير", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        val totalLaborAll = allProjects.sumOf { it.laborCost }
                        val totalPaidAll = allProjects.sumOf { it.paidAmount }
                        val totalRemainingAll = allProjects.sumOf { it.getRemainingLaborCost() }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${totalLaborAll.toInt()} د.ج",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "إجمالي أجرة العمال",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${totalPaidAll.toInt()} د.ج",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = Color(0xFF15803D)
                                )
                                Text(
                                    text = "المسدد للمجموعة",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${totalRemainingAll.toInt()} د.ج",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = Color(0xFFB91C1C)
                                )
                                Text(
                                    text = "المتبقي للقبض",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Title Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قائمة أعضاء الفريق والمهام",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "اضغط على العامل لمشاهدة المهام",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Workers List
            items(workersList) { worker ->
                val assignedProjects = allProjects.filter { it.workerName.trim() == worker.trim() || worker.contains(it.workerName) && it.workerName.isNotBlank() }
                val isCurrentDeviceWorker = worker == activeWorker

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentDeviceWorker) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isCurrentDeviceWorker) 2.dp else 1.dp,
                        color = if (isCurrentDeviceWorker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedWorkerForTasks = worker }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isCurrentDeviceWorker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "👷‍♂️",
                                            fontSize = 22.sp
                                        )
                                    }
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = worker,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (isCurrentDeviceWorker) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Smartphone,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "مُحدد كفني لهذا الهاتف 📱",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Delete button
                            IconButton(
                                onClick = { workerToDelete = worker },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "حذف العامل",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Badges for tasks status & Financials
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${assignedProjects.size} ورشة/مشروع",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            val pendingCount = assignedProjects.count { it.orderStatus in listOf("SENT_TO_STORE", "PREPARING") }
                            if (pendingCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = "$pendingCount طلبات قيد التجهيز 🚚",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Financial Summary for Worker
                        val workerLabor = assignedProjects.sumOf { it.laborCost }
                        val workerPaid = assignedProjects.sumOf { it.paidAmount }
                        val workerRemaining = assignedProjects.sumOf { it.getRemainingLaborCost() }

                        if (workerLabor > 0) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "💰 إجمالي الأجرة: ${workerLabor.toInt()} د.ج",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Text(
                                        text = "🟢 مدفوع: ${workerPaid.toInt()} د.ج",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                    Text(
                                        text = "🔴 متبقي: ${workerRemaining.toInt()} د.ج",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (workerRemaining > 0) Color(0xFFB91C1C) else Color(0xFF15803D)
                                    )
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { selectedWorkerForTasks = worker },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("عرض قائمة المهام واللوازم 📋", fontWeight = FontWeight.Bold)
                            }

                            if (!isCurrentDeviceWorker) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateTeamStoreSettings(
                                            storeNameVal = storeName,
                                            storePhoneVal = viewModel.storePhone.value,
                                            activeWorkerVal = worker
                                        )
                                        Toast.makeText(context, "تم تحديد $worker كفني لهذا الهاتف ✅", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("تحديده لهذا الهاتف", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // --- Dialog 0: Edit Manager Name ---
    if (showEditManagerDialog) {
        var mgrInput by remember { mutableStateOf(defaultManager) }

        AlertDialog(
            onDismissRequest = { showEditManagerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تحديد اسم المدير המשرف", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "أدخل اسم المدير / المشرف المباشر المسؤول عن تعيين المهام وإدارة المستحقات المالية:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = mgrInput,
                        onValueChange = { mgrInput = it },
                        label = { Text("اسم المدير المشرف") },
                        placeholder = { Text("مثال: مدير الورشات والإنتاج") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (mgrInput.isNotBlank()) {
                            viewModel.updateTeamStoreSettings(
                                storeNameVal = storeName,
                                storePhoneVal = viewModel.storePhone.value,
                                activeWorkerVal = activeWorker,
                                managerNameVal = mgrInput.trim()
                            )
                            showEditManagerDialog = false
                            Toast.makeText(context, "تم حفظ اسم المدير المشرف بنجاح! 👨‍💼", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حفظ التغييرات", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditManagerDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // --- Dialog 1: Add New Worker / User Account ---
    if (showAddWorkerDialog) {
        var newWorkerName by remember { mutableStateOf("") }
        var newWorkerPhone by remember { mutableStateOf("") }
        var newWorkerEmail by remember { mutableStateOf("") }
        var newWorkerPass by remember { mutableStateOf("") }
        var selectedRole by remember { mutableStateOf("WORKER") }

        AlertDialog(
            onDismissRequest = { showAddWorkerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إنشاء حساب عامل / مشرف جديد", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "أدخل بيانات حساب العامل/المشرف الجديد للولوج للتطبيق:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newWorkerName,
                        onValueChange = { newWorkerName = it },
                        label = { Text("الاسم الكامل") },
                        placeholder = { Text("مثال: أحمد عبد الله") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = newWorkerPhone,
                        onValueChange = { newWorkerPhone = it },
                        label = { Text("رقم الهاتف (اسم المستخدم للولوج)") },
                        placeholder = { Text("0660000000") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = newWorkerEmail,
                        onValueChange = { newWorkerEmail = it },
                        label = { Text("البريد الإلكتروني (اختياري)") },
                        placeholder = { Text("worker@plumber.com") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = newWorkerPass,
                        onValueChange = { newWorkerPass = it },
                        label = { Text("كلمة المرور") },
                        placeholder = { Text("••••••••") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الصلاحية:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        FilterChip(
                            selected = selectedRole == "WORKER",
                            onClick = { selectedRole = "WORKER" },
                            label = { Text("عامل (WORKER)") }
                        )
                        FilterChip(
                            selected = selectedRole == "ADMIN",
                            onClick = { selectedRole = "ADMIN" },
                            label = { Text("إدارة (ADMIN)") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createWorkerAccount(
                            name = newWorkerName,
                            phone = newWorkerPhone,
                            email = newWorkerEmail.ifBlank { "${newWorkerPhone.trim()}@plumber.com" },
                            pass = newWorkerPass,
                            role = selectedRole
                        ) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) {
                                showAddWorkerDialog = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إنشاء الحساب", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWorkerDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // --- Dialog 2: Delete Worker Confirmation ---
    if (workerToDelete != null) {
        val targetWorker = workerToDelete!!
        val assignedProjectsCount = allProjects.count { it.workerName.trim() == targetWorker.trim() || targetWorker.contains(it.workerName) }

        AlertDialog(
            onDismissRequest = { workerToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد حذف العامل", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("هل أنت تأكد من حذف العامل \"$targetWorker\" من فريق المحل؟")
                    if (assignedProjectsCount > 0) {
                        Text(
                            text = "⚠️ تنبيه: توجد $assignedProjectsCount مشاريع/ورشات مسندة لهذا العامل حالياً.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedList = workersList.filter { it != targetWorker }
                        viewModel.updateWorkersList(updatedList)

                        if (activeWorker == targetWorker && updatedList.isNotEmpty()) {
                            viewModel.updateTeamStoreSettings(
                                storeNameVal = storeName,
                                storePhoneVal = viewModel.storePhone.value,
                                activeWorkerVal = updatedList[0]
                            )
                        }

                        workerToDelete = null
                        Toast.makeText(context, "تم حذف العامل من القائمة ✅", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("نعم، حذف", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { workerToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // --- Dialog 3: View Assigned Tasks & Projects for Selected Worker ---
    if (selectedWorkerForTasks != null) {
        val worker = selectedWorkerForTasks!!
        val workerProjects = allProjects.filter {
            it.workerName.trim() == worker.trim() || worker.contains(it.workerName) && it.workerName.isNotBlank()
        }

        AlertDialog(
            onDismissRequest = { selectedWorkerForTasks = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "المهام والورشات المسندة",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = { selectedWorkerForTasks = null }) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "👷‍♂️ العامل: ",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = worker,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (workerProjects.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "لا توجد مشاريع أو ورشات مسندة لهذا العامل حالياً 📭",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "عند إنشاء مشروع جديد وتحديد هذا العامل كفني ورشة، ستظهر قائمته هنا تلقائياً.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(workerProjects) { proj ->
                                Card(
                                    onClick = {
                                        viewModel.selectProject(proj.id)
                                        selectedWorkerForTasks = null
                                        onNavigateToProjectDetails(proj.id)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = proj.title,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall
                                            )

                                            Surface(
                                                color = when (proj.orderStatus) {
                                                    "DELIVERED" -> Color(0xFFDCFCE7)
                                                    "DISPATCHED" -> Color(0xFFFEF3C7)
                                                    "PREPARING" -> Color(0xFFE0F2FE)
                                                    "SENT_TO_STORE" -> Color(0xFFF3E8FF)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = proj.getOrderStatusAr(),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (proj.orderStatus) {
                                                        "DELIVERED" -> Color(0xFF166534)
                                                        "DISPATCHED" -> Color(0xFF92400E)
                                                        "PREPARING" -> Color(0xFF075985)
                                                        "SENT_TO_STORE" -> Color(0xFF6B21A8)
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (proj.clientName.isNotBlank() || proj.location.isNotBlank()) {
                                            Text(
                                                text = "العميل: ${proj.clientName.ifBlank { "غير محدد" }} • الموقع: ${proj.location.ifBlank { "غير محدد" }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (proj.clientName.isNotBlank() || proj.location.isNotBlank()) {
                                            Text(
                                                text = "العميل: ${proj.clientName.ifBlank { "غير محدد" }} • الموقع: ${proj.location.ifBlank { "غير محدد" }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Manager & Financials Info inside Task Card
                                        val mgrName = proj.managerName.ifBlank { defaultManager }
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = "👨‍💼 المشرف التعييني: $mgrName",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "💰 الأجرة: ${proj.laborCost.toInt()} د.ج | 🟢 مدفوع: ${proj.paidAmount.toInt()} د.ج | 🔴 متبقي: ${proj.getRemainingLaborCost().toInt()} د.ج",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        // Action buttons for task card
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = { paymentTargetProject = proj },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("تسديد دفعة 💵", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = { editAssignmentProject = proj },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("تعديل المالية ✏️", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.selectProject(proj.id)
                                                    selectedWorkerForTasks = null
                                                    onNavigateToProjectDetails(proj.id)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("التفاصيل 📋", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedWorkerForTasks = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("حسناً")
                }
            },
            dismissButton = null
        )
    }

    // --- Dialog 4: Add Payment to Worker for Task ---
    if (paymentTargetProject != null) {
        val targetProj = paymentTargetProject!!
        var paymentInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { paymentTargetProject = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسديد دفعة مالية للعامل", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "تسديد مستحقات مالية للعامل (${targetProj.workerName}) عن ورشة \"${targetProj.title}\":",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("إجمالي أجرة اليد العاملة: ${targetProj.laborCost.toInt()} د.ج", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("المبلغ المدفوع سابقاً: ${targetProj.paidAmount.toInt()} د.ج", fontSize = 12.sp, color = Color(0xFF15803D))
                            Text("المتبقي للقبض: ${targetProj.getRemainingLaborCost().toInt()} د.ج", fontSize = 12.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = paymentInput,
                        onValueChange = { paymentInput = it },
                        label = { Text("مبلغ الدفعة المسددة (د.ج)") },
                        placeholder = { Text("مثال: 5000") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = false,
                            onClick = { paymentInput = targetProj.getRemainingLaborCost().toInt().toString() },
                            label = { Text("تسديد المتبقي كاملاً (${targetProj.getRemainingLaborCost().toInt()} د.ج)") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentInput.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.addWorkerPayment(targetProj.id, amount)
                            paymentTargetProject = null
                            Toast.makeText(context, "تم تسجيل تسديد $amount د.ج بنجاح! 💵", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "يرجى إدخال مبلغ صحيح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("توثيق وتسديد الدفعة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentTargetProject = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // --- Dialog 5: Edit Task Assignment & Financials ---
    if (editAssignmentProject != null) {
        val projToEdit = editAssignmentProject!!
        var editManagerInput by remember { mutableStateOf(projToEdit.managerName.ifBlank { defaultManager }) }
        var editWorkerInput by remember { mutableStateOf(projToEdit.workerName.ifBlank { activeWorker }) }
        var editLaborInput by remember { mutableStateOf(if (projToEdit.laborCost > 0) projToEdit.laborCost.toInt().toString() else "") }
        var editPaidInput by remember { mutableStateOf(if (projToEdit.paidAmount > 0) projToEdit.paidAmount.toInt().toString() else "") }

        AlertDialog(
            onDismissRequest = { editAssignmentProject = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تعديل التعيين والمالية للورشة", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "الورشة: ${projToEdit.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = editManagerInput,
                        onValueChange = { editManagerInput = it },
                        label = { Text("المدير المشرف المسؤول") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    var expandedWorkerSelect by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedWorkerSelect,
                        onExpandedChange = { expandedWorkerSelect = !expandedWorkerSelect },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = editWorkerInput,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("العامل الفني المسند") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWorkerSelect) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedWorkerSelect,
                            onDismissRequest = { expandedWorkerSelect = false }
                        ) {
                            workersList.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        editWorkerInput = w
                                        expandedWorkerSelect = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editLaborInput,
                            onValueChange = { editLaborInput = it },
                            label = { Text("أجرة اليد العاملة (د.ج)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = editPaidInput,
                            onValueChange = { editPaidInput = it },
                            label = { Text("المبلغ المدفوع (د.ج)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val laborVal = editLaborInput.toDoubleOrNull() ?: 0.0
                        val paidVal = editPaidInput.toDoubleOrNull() ?: 0.0

                        viewModel.updateProjectAssignmentAndFinancials(
                            projectId = projToEdit.id,
                            workerName = editWorkerInput,
                            managerName = editManagerInput,
                            laborCost = laborVal,
                            paidAmount = paidVal
                        )
                        editAssignmentProject = null
                        Toast.makeText(context, "تم تعديل التعيين والمالية للورشة بنجاح! ✅", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حفظ التحديثات", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editAssignmentProject = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
