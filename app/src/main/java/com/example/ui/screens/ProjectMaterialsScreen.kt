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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectItem
import com.example.ui.components.AddCustomMaterialDialog
import com.example.ui.components.PdfReportDialog
import com.example.ui.components.PlumbingIcon
import com.example.ui.components.SendStoreOrderDialog
import com.example.ui.components.ShareHelper
import com.example.ui.components.StoreInvoiceDialog
import com.example.ui.viewmodel.PlumberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectMaterialsScreen(
    viewModel: PlumberViewModel,
    onBack: () -> Unit,
    onNavigateToAddMore: () -> Unit
) {
    val context = LocalContext.current
    val currentProject by viewModel.currentProject.collectAsState()
    val items by viewModel.currentProjectItems.collectAsState()

    var isShoppingChecklistMode by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var showSendStoreOrderDialog by remember { mutableStateOf(false) }
    var showStoreInvoiceDialog by remember { mutableStateOf(false) }
    var editingItemForQuantity by remember { mutableStateOf<ProjectItem?>(null) }

    val storeName by viewModel.storeName.collectAsState()
    val storePhone by viewModel.storePhone.collectAsState()
    val activeWorker by viewModel.activeWorker.collectAsState()
    val managerName by viewModel.managerName.collectAsState()
    val workersList by viewModel.workersList.collectAsState()

    val totalItemsCount = items.size
    val totalQty = items.sumOf { it.quantity }
    val totalQtyStr = if (totalQty % 1.0 == 0.0) totalQty.toInt().toString() else totalQty.toString()
    val purchasedCount = items.count { it.isPurchased }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "مواد المشروع",
                            fontWeight = FontWeight.Bold
                        )
                        currentProject?.let {
                            Text(
                                text = "${it.title} • ${it.workTypeNameAr}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showStoreInvoiceDialog = true }) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = "فاتورة اللوازم والتركيب والعقل", tint = Color(0xFF006B42))
                    }
                    IconButton(onClick = { showPdfDialog = true }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "عرض PDF / طباعة", tint = Color(0xFF0284C7))
                    }
                    IconButton(onClick = {
                        val text = currentProject?.let { ShareHelper.generateTextSummary(it, items) } ?: ""
                        ShareHelper.shareToWhatsApp(context, text)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة WhatsApp", tint = Color(0xFF25D366))
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToAddMore,
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("+ قطع", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { showSendStoreOrderDialog = true },
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("طلب المحل 🛒", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = { showStoreInvoiceDialog = true },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006B42)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("الفاتورة 🧾", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = { showPdfDialog = true },
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Mode Switcher: قائمة المواد (Table) vs قائمة الشراء (Checklist)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                SegmentedButton(
                    selected = !isShoppingChecklistMode,
                    onClick = { isShoppingChecklistMode = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مواد المشروع")
                }
                SegmentedButton(
                    selected = isShoppingChecklistMode,
                    onClick = { isShoppingChecklistMode = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("قائمة الشراء ($purchasedCount/$totalItemsCount)")
                }
            }

            // Store Order Dispatch Tracker Banner
            if (currentProject != null) {
                Surface(
                    color = Color(0xFFF0F9FF),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "حالة طلب التجهيز بالمحل:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF0369A1)
                                )
                                Text(
                                    text = currentProject!!.getOrderStatusAr(),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF0284C7)
                                )
                            }

                            FilledTonalButton(
                                onClick = { showSendStoreOrderDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تحديث / إرسال", fontSize = 12.sp)
                            }
                        }

                        // Status Change Chips (Quick toggle)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val statuses = listOf(
                                "SENT_TO_STORE" to "📤 أُرسل",
                                "PREPARING" to "📦 للتجهيز",
                                "DISPATCHED" to "🚚 بالطريق",
                                "DELIVERED" to "✅ استُلِم"
                            )
                            statuses.forEach { (statusKey, statusLabel) ->
                                val isSelected = currentProject!!.orderStatus == statusKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.updateProjectOrderStatus(currentProject!!.id, statusKey)
                                    },
                                    label = { Text(statusLabel, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF0284C7),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Summary Totals Banner
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val totalCost = items.sumOf { it.getTotalPrice() }
                    Column {
                        Text(
                            text = "إجمالي المواد المطلوبة:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$totalItemsCount نوع مادة",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        if (totalCost > 0) {
                            val totalCostStr = if (totalCost % 1.0 == 0.0) totalCost.toInt().toString() else totalCost.toString()
                            Text(
                                text = "التكلفة الإجمالية: $totalCostStr د.ج",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF006B42)
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "$totalQtyStr وحدة/متر",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Empty State
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد مواد في القائمة بعد",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "اضغط على زر (+ إضافة قطع) للبدء باختيار المواد وتحديد الكميات",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateToAddMore) {
                            Text("+ اختيار وتحديد المواد")
                        }
                    }
                }
            } else {
                // List of Project Items
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        val qtyDisplay = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.isPurchased)
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Shopping Checklist Checkbox (if in checklist mode)
                                if (isShoppingChecklistMode) {
                                    Checkbox(
                                        checked = item.isPurchased,
                                        onCheckedChange = { viewModel.toggleItemPurchased(item) }
                                    )
                                }

                                // Visual Icon
                                PlumbingIcon(
                                    iconType = item.iconType,
                                    modifier = Modifier.size(52.dp)
                                )

                                // Info Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.materialNameAr,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = if (item.isPurchased) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = item.size,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = item.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Pipe count calculation display if unit is meters
                                    item.getPipesCount()?.let { pipeCount ->
                                        Text(
                                            text = "≈ $pipeCount أنبوب (طول 4م)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (item.unitPrice > 0) {
                                        val totalPrice = item.getTotalPrice()
                                        val unitPriceStr = if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toInt().toString() else item.unitPrice.toString()
                                        val totalPriceStr = if (totalPrice % 1.0 == 0.0) totalPrice.toInt().toString() else totalPrice.toString()
                                        Text(
                                            text = "السعر: $unitPriceStr د.ج | الإجمالي: $totalPriceStr د.ج",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF006B42)
                                        )
                                    }

                                    if (item.notes.isNotBlank()) {
                                        Text(
                                            text = "ملاحظة: ${item.notes}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Interactive Stepper [-] QTY [+]
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.decrementItemQuantity(item) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.RemoveCircleOutline,
                                                contentDescription = "إنقاص",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }

                                        Surface(
                                            onClick = { editingItemForQuantity = item },
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "$qtyDisplay ${item.unit}",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.incrementItemQuantity(item) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.AddCircleOutline,
                                                contentDescription = "زيادة",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                // Delete Action
                                IconButton(
                                    onClick = { viewModel.deleteItem(item) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "حذف",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // PDF Report Dialog
    if (showPdfDialog && currentProject != null) {
        PdfReportDialog(
            project = currentProject!!,
            items = items,
            onDismiss = { showPdfDialog = false }
        )
    }

    // Add Custom Material Dialog
    if (showAddCustomDialog) {
        AddCustomMaterialDialog(
            onDismiss = { showAddCustomDialog = false },
            onConfirm = { nameAr, nameFr, size, qty, unit, category, price, imageUri, notes ->
                viewModel.addCustomItemToProject(
                    nameAr = nameAr,
                    nameFr = nameFr,
                    size = size,
                    quantity = qty,
                    unit = unit,
                    category = category,
                    price = price,
                    imageUri = imageUri,
                    notes = notes
                )
                showAddCustomDialog = false
            }
        )
    }

    // Send Store Order Dialog
    if (showSendStoreOrderDialog && currentProject != null) {
        SendStoreOrderDialog(
            project = currentProject!!,
            items = items,
            defaultStoreName = storeName,
            defaultStorePhone = storePhone,
            defaultWorkerName = activeWorker,
            workersList = workersList,
            onDismiss = { showSendStoreOrderDialog = false },
            onSendOrder = { updatedWorker, updatedPhone, updatedLocation, orderNotes, newStatus ->
                val updatedProj = currentProject!!.copy(
                    workerName = updatedWorker,
                    storePhone = updatedPhone,
                    location = updatedLocation.ifBlank { currentProject!!.location },
                    orderStatus = newStatus,
                    updatedAt = System.currentTimeMillis()
                )
                viewModel.updateProjectInfo(updatedProj)
                showSendStoreOrderDialog = false
            }
        )
    }

    // Full Store to Site Invoice Dialog (اللوازم والتركيب والعقل)
    if (showStoreInvoiceDialog && currentProject != null) {
        StoreInvoiceDialog(
            project = currentProject!!,
            items = items,
            defaultStoreName = storeName,
            defaultManagerName = managerName,
            defaultWorkerName = activeWorker,
            workersList = workersList,
            onDismiss = { showStoreInvoiceDialog = false },
            onSaveFinancials = { updatedWorker, updatedManager, laborCost, paidAmount ->
                viewModel.updateProjectAssignmentAndFinancials(
                    projectId = currentProject!!.id,
                    workerName = updatedWorker,
                    managerName = updatedManager,
                    laborCost = laborCost,
                    paidAmount = paidAmount
                )
            }
        )
    }
}
