package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Project
import com.example.data.model.ProjectItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreInvoiceDialog(
    project: Project,
    items: List<ProjectItem>,
    defaultStoreName: String,
    defaultManagerName: String,
    defaultWorkerName: String,
    workersList: List<String>,
    onDismiss: () -> Unit,
    onSaveFinancials: (updatedWorker: String, updatedManager: String, laborCost: Double, paidAmount: Double) -> Unit
) {
    val context = LocalContext.current

    var isPreviewMode by remember { mutableStateOf(true) }

    val materialsTotal = items.sumOf { it.getTotalPrice() }
    val calculated150InstallationCost = (materialsTotal * 1.5)

    var storeName by remember { mutableStateOf(defaultStoreName.ifBlank { "محل السباكة والتجهيزات" }) }
    var managerName by remember { mutableStateOf(project.managerName.ifBlank { defaultManagerName }) }
    var workerName by remember { mutableStateOf(project.workerName.ifBlank { defaultWorkerName }) }
    var clientName by remember { mutableStateOf(project.clientName) }
    var siteLocation by remember { mutableStateOf(project.location) }

    var installationCostInput by remember {
        mutableStateOf(
            if (project.laborCost > 0) project.laborCost.toInt().toString()
            else if (materialsTotal > 0) calculated150InstallationCost.toInt().toString()
            else "0"
        )
    }
    var fittingsCostInput by remember { mutableStateOf("0") }
    var transportFeeInput by remember { mutableStateOf("0") }
    var paidAmountInput by remember { mutableStateOf(if (project.paidAmount > 0) project.paidAmount.toInt().toString() else "0") }
    var invoiceNotes by remember { mutableStateOf(project.notes) }

    var isWorkerDropdownExpanded by remember { mutableStateOf(false) }

    val installationCost = installationCostInput.toDoubleOrNull() ?: 0.0
    val fittingsCost = fittingsCostInput.toDoubleOrNull() ?: 0.0
    val transportFee = transportFeeInput.toDoubleOrNull() ?: 0.0
    val paidAmount = paidAmountInput.toDoubleOrNull() ?: 0.0

    val grandTotal = materialsTotal + installationCost + fittingsCost + transportFee
    val remainingBalance = (grandTotal - paidAmount).coerceAtLeast(0.0)

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val dateStr = dateFormat.format(Date(project.createdAt))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color(0xFF006B42),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "فاتورة توريد اللوازم والتركيب والعقل",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF006B42)
                            )
                            Text(
                                text = "من المحل إلى موقع ورشة العمل 📍",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Mode Selector Bar (معاينة الفاتورة vs تعديل القيم)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    SegmentedButton(
                        selected = isPreviewMode,
                        onClick = { isPreviewMode = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("📄 معاينة الفاتورة")
                    }
                    SegmentedButton(
                        selected = !isPreviewMode,
                        onClick = { isPreviewMode = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("✏️ تعديل القيم والبيانات")
                    }
                }

                if (!isPreviewMode) {
                    // EDIT MODE FORM
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "💡 يمكنك تعديل بيانات المحل، التركيب، تكلفة العقل، والنقل لتعديل قيم الفاتورة النهائية:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = storeName,
                                onValueChange = { storeName = it },
                                label = { Text("اسم المحل / المورد") },
                                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = managerName,
                                onValueChange = { managerName = it },
                                label = { Text("المدير المشرف") },
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        ExposedDropdownMenuBox(
                            expanded = isWorkerDropdownExpanded,
                            onExpandedChange = { isWorkerDropdownExpanded = !isWorkerDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = workerName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("الفني المكلف بالتركيب والتسليم") },
                                leadingIcon = { Icon(Icons.Default.Engineering, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isWorkerDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = isWorkerDropdownExpanded,
                                onDismissRequest = { isWorkerDropdownExpanded = false }
                            ) {
                                workersList.forEach { w ->
                                    DropdownMenuItem(
                                        text = { Text(w, fontWeight = if (w == workerName) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            workerName = w
                                            isWorkerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = clientName,
                                onValueChange = { clientName = it },
                                label = { Text("اسم العميل") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = siteLocation,
                                onValueChange = { siteLocation = it },
                                label = { Text("موقع ورشة العمل") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("💰 الحسابات والمالية (التركيب/الصيانة = 150% من اللوازم):", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        if (materialsTotal > 0) {
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "⚡ سعر التركيب أو الصيانة (150% حسب اللوازم):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E40AF)
                                        )
                                        Text(
                                            text = "اللوازم (${materialsTotal.toInt()} د.ج) × 150% = ${calculated150InstallationCost.toInt()} د.ج",
                                            fontSize = 11.sp,
                                            color = Color(0xFF2563EB)
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            installationCostInput = calculated150InstallationCost.toInt().toString()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
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
                                label = { Text("أجرة التركيب واليد العاملة (د.ج)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = fittingsCostInput,
                                onValueChange = { fittingsCostInput = it },
                                label = { Text("تكلفة العقل والتوصيلات (د.ج)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = transportFeeInput,
                                onValueChange = { transportFeeInput = it },
                                label = { Text("رسوم النقل والتوصيل للموقع (د.ج)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = paidAmountInput,
                                onValueChange = { paidAmountInput = it },
                                label = { Text("المبلغ المدفوع تسقيعاً (د.ج)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        OutlinedTextField(
                            value = invoiceNotes,
                            onValueChange = { invoiceNotes = it },
                            label = { Text("ملاحظات الفاتورة") },
                            placeholder = { Text("أدخل أي شروط استلام أو ملاحظات إضافية...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = { isPreviewMode = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تطبيق ومعاينة الفاتورة النهائي 📄", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // PAPER PRINTABLE PREVIEW CARD
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        ) {
                            // Invoice Header
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = storeName.ifBlank { "محل السباكة والتجهيزات" },
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF006B42)
                                        )
                                    )
                                    Text(
                                        text = "فاتورة توريد اللوازم والتركيب والعقل من المحل إلى الموقع",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.DarkGray
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = Color(0xFF006B42), thickness = 2.dp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            // Meta Info Box
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF0F7F4), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "رقم الفاتورة: #INV-${project.id}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(text = "التاريخ: $dateStr", color = Color.Gray, fontSize = 12.sp)
                                    }
                                    Text(
                                        text = "العميل: ${clientName.ifBlank { project.title }}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "موقع ورشة العمل: ${siteLocation.ifBlank { "غير محدد" }}",
                                        color = Color.DarkGray,
                                        fontSize = 12.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "👨‍💼 المدير المشرف: ${managerName.ifBlank { "غير محدد" }}", fontSize = 11.sp, color = Color(0xFF006B42), fontWeight = FontWeight.Bold)
                                        Text(text = "👷‍♂️ الفني المكلف: ${workerName.ifBlank { "غير محدد" }}", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Items Table Header
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF006B42), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .padding(vertical = 6.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("#", modifier = Modifier.width(24.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("بيان اللوازم والمواد المسلمة", modifier = Modifier.weight(2f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("الكمية", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                                    Text("السعر (د.ج)", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End)
                                    Text("الإجمالي", modifier = Modifier.weight(1.1f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End)
                                }
                            }

                            // Table Rows
                            itemsIndexed(items) { index, item ->
                                val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                                val unitPriceStr = if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toInt().toString() else item.unitPrice.toString()
                                val totalPriceStr = if (item.getTotalPrice() % 1.0 == 0.0) item.getTotalPrice().toInt().toString() else item.getTotalPrice().toString()
                                val rowBg = if (index % 2 == 0) Color.White else Color(0xFFF9F9F9)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBg)
                                        .border(0.5.dp, Color(0xFFE0E0E0))
                                        .padding(vertical = 6.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${index + 1}", modifier = Modifier.width(24.dp), color = Color.DarkGray, fontSize = 11.sp)
                                    Text("${item.materialNameAr} (${item.size})", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("$qtyStr ${item.unit}", modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.Center)
                                    Text(unitPriceStr, modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End)
                                    Text(totalPriceStr, modifier = Modifier.weight(1.1f), fontWeight = FontWeight.Bold, color = Color(0xFF006B42), fontSize = 11.sp, textAlign = TextAlign.End)
                                }
                            }

                            // FINANCIAL BREAKDOWN CARD
                            item {
                                Spacer(modifier = Modifier.height(14.dp))

                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("🧾 تفصيل المالية والإجمالي:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                        Divider(color = Color(0xFFE2E8F0))

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("إجمالي اللوازم والمواد المسلمة:", fontSize = 11.sp, color = Color.DarkGray)
                                            Text("${materialsTotal.toInt()} د.ج", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        if (installationCost > 0) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("أجرة التركيب واليد العاملة:", fontSize = 11.sp, color = Color.DarkGray)
                                                Text("${installationCost.toInt()} د.ج", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (fittingsCost > 0) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("تكلفة العقل والملحقات والتوصيلات:", fontSize = 11.sp, color = Color.DarkGray)
                                                Text("${fittingsCost.toInt()} د.ج", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (transportFee > 0) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("رسوم النقل والتوصيل للموقع:", fontSize = 11.sp, color = Color.DarkGray)
                                                Text("${transportFee.toInt()} د.ج", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Divider(color = Color(0xFFCBD5E1), thickness = 1.dp)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("المبلغ الإجمالي الكلي للفاتورة:", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF006B42))
                                            Text("${grandTotal.toInt()} د.ج", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF006B42))
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("المبلغ المدفوع تسقيعاً / عربون:", fontSize = 11.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                                            Text("${paidAmount.toInt()} د.ج", fontSize = 11.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("المبلغ المتبقي للتحصيل عند التسليم بالموقع:", fontSize = 11.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                                            Text("${remainingBalance.toInt()} د.ج", fontSize = 12.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }

                                if (invoiceNotes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("ملاحظات الفاتورة:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.DarkGray)
                                    Text(invoiceNotes, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val text = ShareHelper.generateFullInvoiceSummary(
                                project = project,
                                items = items,
                                storeName = storeName,
                                workerName = workerName,
                                managerName = managerName,
                                siteLocation = siteLocation,
                                installationCost = installationCost,
                                fittingsCost = fittingsCost,
                                transportFee = transportFee,
                                paidAmount = paidAmount,
                                invoiceNotes = invoiceNotes
                            )
                            ShareHelper.copyToClipboard(context, text)
                            Toast.makeText(context, "تم نسخ الفاتورة للحافظة 📋", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val text = ShareHelper.generateFullInvoiceSummary(
                                project = project,
                                items = items,
                                storeName = storeName,
                                workerName = workerName,
                                managerName = managerName,
                                siteLocation = siteLocation,
                                installationCost = installationCost,
                                fittingsCost = fittingsCost,
                                transportFee = transportFee,
                                paidAmount = paidAmount,
                                invoiceNotes = invoiceNotes
                            )
                            ShareHelper.shareToWhatsApp(context, text)
                        },
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("واتساب 📲", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            onSaveFinancials(workerName, managerName, installationCost, paidAmount)
                            Toast.makeText(context, "تم حفظ وتحديث الفاتورة والمالية بنجاح! ✅", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006B42)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ الفاتورة ✅", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
