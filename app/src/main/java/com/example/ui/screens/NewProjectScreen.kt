package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preset.PlumbingLibraryData
import com.example.ui.viewmodel.PlumberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectScreen(
    viewModel: PlumberViewModel,
    initialWorkTypeKey: String? = null,
    onBack: () -> Unit,
    onProjectCreated: (projectId: Long) -> Unit
) {
    val defaultManager by viewModel.managerName.collectAsState()
    val activeWorker by viewModel.activeWorker.collectAsState()
    val workersList by viewModel.workersList.collectAsState()

    var title by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var managerNameInput by remember { mutableStateOf(defaultManager) }
    var selectedWorkerInput by remember { mutableStateOf(activeWorker) }
    var laborCostInput by remember { mutableStateOf("") }
    var paidAmountInput by remember { mutableStateOf("") }
    var selectedWorkTypeKey by remember { mutableStateOf(initialWorkTypeKey ?: "BATHROOM") }

    // Auto update title example if work type changes and title is empty
    LaunchedEffect(selectedWorkTypeKey) {
        if (title.isBlank()) {
            val wt = PlumbingLibraryData.WORK_TYPES.find { it.key == selectedWorkTypeKey }
            if (wt != null && wt.key != "CUSTOM") {
                title = "${wt.titleAr} جديد"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إنشاء مشروع جديد وتخصيص المهمة", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = {
                            val laborVal = laborCostInput.toDoubleOrNull() ?: 0.0
                            val paidVal = paidAmountInput.toDoubleOrNull() ?: 0.0

                            viewModel.createProject(
                                title = title.ifBlank { "مشروع جديد" },
                                clientName = clientName,
                                location = location,
                                notes = notes,
                                workTypeKey = selectedWorkTypeKey,
                                workerName = selectedWorkerInput,
                                managerName = managerNameInput,
                                laborCost = laborVal,
                                paidAmount = paidVal,
                                onProjectCreated = onProjectCreated
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = "🚀 بدء حساب وتحديد المواد",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Project Name Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("اسم المشروع / الورشة *") },
                placeholder = { Text("مثال: تركيب ماء الطابق الأول / شقة 4") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Client Name & Location Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("اسم العميل") },
                    placeholder = { Text("أبو محمد") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("الموقع / العنوان") },
                    placeholder = { Text("حي السلام") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // --- Section: Manager & Worker Assignment ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "👨‍💼 تخصيص المهمة (المدير والعامل المسند)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = managerNameInput,
                        onValueChange = { managerNameInput = it },
                        label = { Text("اسم المدير المشرف") },
                        placeholder = { Text("مثال: مدير الورشات والإنتاج") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Text(
                        text = "اختر العامل المسند له الورشة:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )

                    var expandedWorkerDropdown by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedWorkerDropdown,
                        onExpandedChange = { expandedWorkerDropdown = !expandedWorkerDropdown },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedWorkerInput,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("العامل الفني المسؤول") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWorkerDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedWorkerDropdown,
                            onDismissRequest = { expandedWorkerDropdown = false }
                        ) {
                            workersList.forEach { worker ->
                                DropdownMenuItem(
                                    text = { Text(worker, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedWorkerInput = worker
                                        expandedWorkerDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- Section: Financials & Labor Costs (المال) ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "💰 مستحقات المال واليد العاملة",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = laborCostInput,
                            onValueChange = { laborCostInput = it },
                            label = { Text("أجرة اليد العاملة (د.ج)") },
                            placeholder = { Text("مثال: 15000") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = paidAmountInput,
                            onValueChange = { paidAmountInput = it },
                            label = { Text("المبلغ المدفوع تسقيعاً (د.ج)") },
                            placeholder = { Text("مثال: 5000") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // Notes Input
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات أخرى (اختياري)") },
                placeholder = { Text("ملاحظات خاصة عن طريق التمديد أوالضغوط المطلوب...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Work Types Grid Title
            Text(
                text = "اختر نوع العمل / Job Type:",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            // Work Types Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlumbingLibraryData.WORK_TYPES.chunked(2).forEach { rowWorkTypes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowWorkTypes.forEach { workType ->
                            val isSelected = selectedWorkTypeKey == workType.key
                            Card(
                                onClick = { selectedWorkTypeKey = workType.key },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        Color.White
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(text = workType.iconEmoji, fontSize = 28.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = workType.titleAr,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = workType.titleFr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (rowWorkTypes.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
