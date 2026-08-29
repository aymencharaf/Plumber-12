package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Project
import com.example.data.model.ProjectItem
import com.example.ui.components.PdfInvoiceExporter
import com.example.ui.components.ShareHelper
import com.example.ui.viewmodel.PlumberViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesManagementScreen(
    viewModel: PlumberViewModel,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
    val currentItems by viewModel.currentProjectItems.collectAsStateWithLifecycle()

    val storeNamePref by viewModel.storeName.collectAsStateWithLifecycle()
    val managerNamePref by viewModel.managerName.collectAsStateWithLifecycle()
    val activeWorkerPref by viewModel.activeWorker.collectAsStateWithLifecycle()
    val isManagerLoggedIn by viewModel.isManagerLoggedIn.collectAsStateWithLifecycle()
    val workersList by viewModel.workersList.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") } // "الكل", "متبقي", "مكتمل"

    var showManagerLoginDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    // Invoice Form Editable State
    var storeNameInput by remember(currentProject, storeNamePref) { mutableStateOf(storeNamePref.ifBlank { "محل السباكة والتجهيزات" }) }
    var managerNameInput by remember(currentProject, managerNamePref) { mutableStateOf(currentProject?.managerName?.ifBlank { managerNamePref } ?: managerNamePref) }
    var workerNameInput by remember(currentProject, activeWorkerPref) { mutableStateOf(currentProject?.workerName?.ifBlank { activeWorkerPref } ?: activeWorkerPref) }
    var clientNameInput by remember(currentProject) { mutableStateOf(currentProject?.clientName ?: "") }
    var siteLocationInput by remember(currentProject) { mutableStateOf(currentProject?.location ?: "") }

    // Calculations
    val materialsTotal = currentItems.sumOf { it.getTotalPrice() }
    val calculated150InstallationCost = (materialsTotal * 1.5)

    var installationCostInput by remember(currentProject, currentItems) {
        mutableStateOf(
            if (currentProject != null && currentProject!!.laborCost > 0)
                currentProject!!.laborCost.toInt().toString()
            else if (currentItems.isNotEmpty() && materialsTotal > 0)
                calculated150InstallationCost.toInt().toString()
            else "0"
        )
    }
    var fittingsCostInput by remember { mutableStateOf("0") }
    var transportFeeInput by remember { mutableStateOf("0") }
    var paidAmountInput by remember(currentProject) {
        mutableStateOf(if (currentProject != null && currentProject!!.paidAmount > 0) currentProject!!.paidAmount.toInt().toString() else "0")
    }
    var invoiceNotesInput by remember(currentProject) { mutableStateOf(currentProject?.notes ?: "") }

    var isWorkerDropdownExpanded by remember { mutableStateOf(false) }

    val installationCost = installationCostInput.toDoubleOrNull() ?: 0.0
    val fittingsCost = fittingsCostInput.toDoubleOrNull() ?: 0.0
    val transportFee = transportFeeInput.toDoubleOrNull() ?: 0.0
    val paidAmount = paidAmountInput.toDoubleOrNull() ?: 0.0

    val grandTotal = materialsTotal + installationCost + fittingsCost + transportFee
    val remainingBalance = (grandTotal - paidAmount).coerceAtLeast(0.0)

    val filteredProjects = remember(allProjects, searchQuery, selectedFilter) {
        allProjects.filter { p ->
            val matchesSearch = p.title.contains(searchQuery, ignoreCase = true) ||
                    p.clientName.contains(searchQuery, ignoreCase = true) ||
                    p.location.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "متبقي" -> p.paidAmount < p.laborCost || p.paidAmount == 0.0
                "مكتمل" -> p.paidAmount > 0 && p.paidAmount >= p.laborCost
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    // High Level Overall Statistics
    val totalInvoicesVal = allProjects.sumOf { it.laborCost }
    val totalPaidVal = allProjects.sumOf { it.paidAmount }
    val totalRemainingVal = (totalInvoicesVal - totalPaidVal).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color(0xFF006B42),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "إدارة فواتير العمل والورشات",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "إصدار وتصدير فواتير اللوازم والتركيب والعقل 📍",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                        }
                    }
                },
                actions = {
                    if (isManagerLoggedIn) {
                        Surface(
                            onClick = {
                                viewModel.logoutManager()
                                Toast.makeText(context, "تم خروج المدير", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFDCFCE7),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "👨‍💼 المدير (خروج)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { showManagerLoginDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("🔐 دخول المدير", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Financial Statistics Header Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("عدد الورشات", fontSize = 11.sp, color = Color.Gray)
                        Text("${allProjects.size}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    }
                    Divider(modifier = Modifier.height(28.dp).width(1.dp), color = Color(0xFF86EFAC))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("إجمالي التركيبات", fontSize = 11.sp, color = Color.Gray)
                        Text("${totalInvoicesVal.toInt()} د.ج", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006B42))
                    }
                    Divider(modifier = Modifier.height(28.dp).width(1.dp), color = Color(0xFF86EFAC))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("المبالغ المحصلة", fontSize = 11.sp, color = Color.Gray)
                        Text("${totalPaidVal.toInt()} د.ج", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    }
                    Divider(modifier = Modifier.height(28.dp).width(1.dp), color = Color(0xFF86EFAC))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("المتبقي للتحصيل", fontSize = 11.sp, color = Color.Gray)
                        Text("${totalRemainingVal.toInt()} د.ج", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                    }
                }
            }

            // Project Selector Carousel Header
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اختر مشروعاً / ورشة لإصدار الفاتورة:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${filteredProjects.size} ورشة",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث باسم العميل، المشروع، أو الموقع...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Horizontal Carousel of Projects
                if (filteredProjects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد ورشات مطابقة للبحث", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredProjects) { p ->
                            val isSelected = p.id == selectedProjectId
                            val cardBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

                            Card(
                                modifier = Modifier
                                    .width(170.dp)
                                    .clickable { viewModel.selectProject(p.id) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.5.dp, borderColor)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = p.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (p.clientName.isNotBlank()) "👤 ${p.clientName}" else "📍 ${p.location.ifBlank { "الموقع" }}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "تركيب: ${p.laborCost.toInt()} د.ج",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF006B42)
                                        )
                                        Text(
                                            text = p.getOrderStatusAr(),
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Divider()

            // Active Invoice Form & Details
            if (currentProject == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "يرجى اختيار مشروع من الشريط أعلاه لإصدار ومعاينة الفاتورة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Store & Staff Information Box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📋 بيانات الفاتورة والأطراف:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = storeNameInput,
                                    onValueChange = { storeNameInput = it },
                                    label = { Text("المحل / المورد") },
                                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = managerNameInput,
                                    onValueChange = { managerNameInput = it },
                                    label = { Text("المدير المشرف") },
                                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ExposedDropdownMenuBox(
                                    expanded = isWorkerDropdownExpanded,
                                    onExpandedChange = { isWorkerDropdownExpanded = !isWorkerDropdownExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = workerNameInput,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("الفني المكلف") },
                                        leadingIcon = { Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isWorkerDropdownExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isWorkerDropdownExpanded,
                                        onDismissRequest = { isWorkerDropdownExpanded = false }
                                    ) {
                                        workersList.forEach { w ->
                                            DropdownMenuItem(
                                                text = { Text(w, fontWeight = if (w == workerNameInput) FontWeight.Bold else FontWeight.Normal) },
                                                onClick = {
                                                    workerNameInput = w
                                                    isWorkerDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = clientNameInput,
                                    onValueChange = { clientNameInput = it },
                                    label = { Text("اسم العميل") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }

                            OutlinedTextField(
                                value = siteLocationInput,
                                onValueChange = { siteLocationInput = it },
                                label = { Text("عنوان ورشة العمل / موقع التسليم") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }
                    }

                    // Items & Materials Section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📦 اللوازم والمواد المسلمة (${currentItems.size} بند):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                TextButton(
                                    onClick = { showAddItemDialog = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("+ إضافة بند/خدمة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (currentItems.isEmpty()) {
                                Text(
                                    text = "لا توجد مواد مضافة لهذا المشروع بعد. يمكن الضغط على (+ إضافة بند/خدمة) لإضافة اللوازم.",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    currentItems.forEachIndexed { idx, item ->
                                        val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                                        val unitPriceStr = if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toInt().toString() else item.unitPrice.toString()
                                        val totalPriceStr = if (item.getTotalPrice() % 1.0 == 0.0) item.getTotalPrice().toInt().toString() else item.getTotalPrice().toString()

                                        Surface(
                                            color = Color(0xFFF8FAFC),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1.8f)) {
                                                    Text("${idx + 1}. ${item.materialNameAr} (${item.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text("الكمية: $qtyStr ${item.unit} × $unitPriceStr د.ج", fontSize = 11.sp, color = Color.Gray)
                                                }
                                                Text("$totalPriceStr د.ج", fontWeight = FontWeight.Black, color = Color(0xFF006B42), fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Financial Calculations Input Panel
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "💰 التكاليف الحسابية للفاتورة (التركيب/الصيانة = 150% من اللوازم):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.Black
                            )

                            if (materialsTotal > 0) {
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "⚡ سعر التركيب أو الصيانة القياسي (150% من اللوازم):",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E40AF)
                                            )
                                            Text(
                                                text = "${materialsTotal.toInt()} د.ج × 150% = ${calculated150InstallationCost.toInt()} د.ج",
                                                fontSize = 10.sp,
                                                color = Color(0xFF2563EB)
                                            )
                                        }
                                        Button(
                                            onClick = { installationCostInput = calculated150InstallationCost.toInt().toString() },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                        ) {
                                            Text("تطبيق 150%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = installationCostInput,
                                    onValueChange = { installationCostInput = it },
                                    label = { Text("أجرة التركيب والعمل (د.ج)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = fittingsCostInput,
                                    onValueChange = { fittingsCostInput = it },
                                    label = { Text("تكلفة العقل والخدمات (د.ج)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = transportFeeInput,
                                    onValueChange = { transportFeeInput = it },
                                    label = { Text("رسوم النقل والتوصيل (د.ج)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = paidAmountInput,
                                    onValueChange = { paidAmountInput = it },
                                    label = { Text("المبلغ المدفوع / عربون (د.ج)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            OutlinedTextField(
                                value = invoiceNotesInput,
                                onValueChange = { invoiceNotesInput = it },
                                label = { Text("ملاحظات الفاتورة") },
                                placeholder = { Text("أدخل الشروط، طريقة الدفع، أو أي تفاصيل إضافية...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Divider(color = Color(0xFFCBD5E1), thickness = 1.dp)

                            // Calculated Totals Box
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFDCFCE7), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("إجمالي المواد:", fontSize = 11.sp, color = Color.DarkGray)
                                    Text("${materialsTotal.toInt()} د.ج", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🔮 المبلغ الإجمالي الكلي للفاتورة:", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF006B42))
                                    Text("${grandTotal.toInt()} د.ج", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF006B42))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("🟢 المدفوع تسقيعاً:", fontSize = 11.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                                    Text("${paidAmount.toInt()} د.ج", fontSize = 11.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("🔴 المبلغ المتبقي للتحصيل عند التسليم:", fontSize = 12.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                                    Text("${remainingBalance.toInt()} د.ج", fontSize = 13.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }

                    // Action Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val text = ShareHelper.generateFullInvoiceSummary(
                                    project = currentProject!!,
                                    items = currentItems,
                                    storeName = storeNameInput,
                                    workerName = workerNameInput,
                                    managerName = managerNameInput,
                                    siteLocation = siteLocationInput,
                                    installationCost = installationCost,
                                    fittingsCost = fittingsCost,
                                    transportFee = transportFee,
                                    paidAmount = paidAmount,
                                    invoiceNotes = invoiceNotesInput
                                )
                                ShareHelper.copyToClipboard(context, text)
                                Toast.makeText(context, "تم نسخ الفاتورة للحافظة 📋", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val text = ShareHelper.generateFullInvoiceSummary(
                                    project = currentProject!!,
                                    items = currentItems,
                                    storeName = storeNameInput,
                                    workerName = workerNameInput,
                                    managerName = managerNameInput,
                                    siteLocation = siteLocationInput,
                                    installationCost = installationCost,
                                    fittingsCost = fittingsCost,
                                    transportFee = transportFee,
                                    paidAmount = paidAmount,
                                    invoiceNotes = invoiceNotesInput
                                )
                                ShareHelper.shareToWhatsApp(context, text)
                            },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("واتساب 📲", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = {
                                val pdfFile = PdfInvoiceExporter.generateInvoicePdf(
                                    context = context,
                                    project = currentProject!!,
                                    items = currentItems,
                                    storeName = storeNameInput,
                                    managerName = managerNameInput,
                                    workerName = workerNameInput,
                                    siteLocation = siteLocationInput,
                                    installationCost = installationCost,
                                    fittingsCost = fittingsCost,
                                    transportFee = transportFee,
                                    paidAmount = paidAmount,
                                    invoiceNotes = invoiceNotesInput
                                )
                                if (pdfFile != null) {
                                    PdfInvoiceExporter.sharePdf(context, pdfFile)
                                } else {
                                    Toast.makeText(context, "فشل إنشاء ملف PDF", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.3f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصدير PDF 📄", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = {
                                viewModel.updateProjectAssignmentAndFinancials(
                                    projectId = currentProject!!.id,
                                    workerName = workerNameInput,
                                    managerName = managerNameInput,
                                    laborCost = installationCost,
                                    paidAmount = paidAmount
                                )
                                // Update client name location notes if modified
                                viewModel.updateProjectInfo(
                                    currentProject!!.copy(
                                        clientName = clientNameInput,
                                        location = siteLocationInput,
                                        notes = invoiceNotesInput
                                    )
                                )
                                Toast.makeText(context, "تم حفظ الفاتورة بنجاح! ✅", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006B42)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حفظ ✅", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Manager Login Dialog
    if (showManagerLoginDialog) {
        var pinInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManagerLoginDialog = false },
            title = {
                Text("🔐 تسجيل دخول المدير والمالية", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "أدخل رمز PIN للمدير للتحكم الكامل بفواتير الورشات والمالية:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("رمز PIN للمدير") },
                        placeholder = { Text("الافتراضي: 1234") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel.loginManager(pinInput)) {
                            showManagerLoginDialog = false
                            Toast.makeText(context, "تم تسجيل دخول المدير بنجاح 👨‍💼", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "رمز PIN غير صحيح!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("دخول", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManagerLoginDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Dialog to Add Custom Item/Service directly to invoice
    if (showAddItemDialog && currentProject != null) {
        var itemNameAr by remember { mutableStateOf("") }
        var itemSize by remember { mutableStateOf("قياسي") }
        var itemQty by remember { mutableStateOf("1") }
        var itemUnit by remember { mutableStateOf("قطعة") }
        var itemPrice by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = {
                Text("➕ إضافة بند / خدمة للفاتورة", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = itemNameAr,
                        onValueChange = { itemNameAr = it },
                        label = { Text("اسم المادة / بيان الخدمة") },
                        placeholder = { Text("مثال: أنبوب PPR 25mm / خدمة حفر وترميم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = itemSize,
                            onValueChange = { itemSize = it },
                            label = { Text("المقاس / النوع") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = itemQty,
                            onValueChange = { itemQty = it },
                            label = { Text("الكمية") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = itemUnit,
                            onValueChange = { itemUnit = it },
                            label = { Text("الوحدة") },
                            placeholder = { Text("متر، قطعة، طقم...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = itemPrice,
                            onValueChange = { itemPrice = it },
                            label = { Text("سعر الوحدة (د.ج)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        if (itemNameAr.isNotBlank()) {
                            val qtyVal = itemQty.toDoubleOrNull() ?: 1.0
                            val priceVal = itemPrice.toDoubleOrNull() ?: 0.0

                            viewModel.addCustomItemToProject(
                                nameAr = itemNameAr,
                                size = itemSize.ifBlank { "قياسي" },
                                quantity = qtyVal,
                                unit = itemUnit.ifBlank { "قطعة" },
                                category = "مستلزمات عامة",
                                price = priceVal
                            )
                            showAddItemDialog = false
                            Toast.makeText(context, "تم إضافة البند للفاتورة بنجاح", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "يرجى كتابة اسم البند أو المادة", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إضافة للفاتورة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
