package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Project
import com.example.data.model.ProjectItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendStoreOrderDialog(
    project: Project,
    items: List<ProjectItem>,
    defaultStoreName: String,
    defaultStorePhone: String,
    defaultWorkerName: String,
    workersList: List<String>,
    onDismiss: () -> Unit,
    onSendOrder: (updatedWorker: String, updatedPhone: String, updatedLocation: String, orderNotes: String, newStatus: String) -> Unit
) {
    val context = LocalContext.current

    var storeName by remember { mutableStateOf(defaultStoreName) }
    var workerName by remember { mutableStateOf(project.workerName.ifBlank { defaultWorkerName }) }
    var storePhone by remember { mutableStateOf(project.storePhone.ifBlank { defaultStorePhone }) }
    var siteLocation by remember { mutableStateOf(project.location) }
    var orderNotes by remember { mutableStateOf("") }
    var isWorkerDropdownExpanded by remember { mutableStateOf(false) }

    val totalCost = items.sumOf { it.getTotalPrice() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Store,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إرسال طلب اللوازم للمحل",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "سيتم إرسال قائمة اللوازم المحددة مباشرة إلى المحل لتجهيزها وسائق التوصيل ونقلها إلى موقع العمل.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // 1. Worker Selector (من بين العمال الـ 10)
                ExposedDropdownMenuBox(
                    expanded = isWorkerDropdownExpanded,
                    onExpandedChange = { isWorkerDropdownExpanded = !isWorkerDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = workerName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الفني / العامل المرسل (من طاقم المحل)") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
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
                        workersList.forEach { worker ->
                            DropdownMenuItem(
                                text = { Text(worker, fontWeight = if (worker == workerName) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    workerName = worker
                                    isWorkerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // 2. Store Name
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("اسم المحل / المورد") },
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // 3. Store Phone / WhatsApp
                OutlinedTextField(
                    value = storePhone,
                    onValueChange = { storePhone = it },
                    label = { Text("رقم واتساب المحل لتجهيز الطلب") },
                    placeholder = { Text("مثال: 0661234567") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // 4. Job Site Location
                OutlinedTextField(
                    value = siteLocation,
                    onValueChange = { siteLocation = it },
                    label = { Text("عنوان موقع العمل (للتوصيل)") },
                    placeholder = { Text("مثال: حي السلام - الشارع الرئيسي - منزل رقم 12") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // 5. Driver / Store Notes
                OutlinedTextField(
                    value = orderNotes,
                    onValueChange = { orderNotes = it },
                    label = { Text("توجيهات وملاحظات المحل/السائق (اختياري)") },
                    placeholder = { Text("مثال: يرجى إرسال شاحنة صغيرة، الاتصال عند الانطلاق...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Summary of Items to send
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "عدد المواد المطلوب تجهيزها:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${items.size} نوع مادة",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        if (totalCost > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "إجمالي التكلفة المقدرة:",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${if (totalCost % 1.0 == 0.0) totalCost.toInt() else totalCost} د.ج",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF006B42)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val formattedText = ShareHelper.generateStoreOrderSummary(
                            project = project,
                            items = items,
                            storeName = storeName,
                            workerName = workerName,
                            siteLocation = siteLocation,
                            orderNotes = orderNotes
                        )
                        onSendOrder(workerName, storePhone, siteLocation, orderNotes, "SENT_TO_STORE")
                        ShareHelper.shareToWhatsApp(context, formattedText)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)) // WhatsApp Green
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال عبر واتساب المحل 📲", color = Color.White, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val formattedText = ShareHelper.generateStoreOrderSummary(
                            project = project,
                            items = items,
                            storeName = storeName,
                            workerName = workerName,
                            siteLocation = siteLocation,
                            orderNotes = orderNotes
                        )
                        onSendOrder(workerName, storePhone, siteLocation, orderNotes, "SENT_TO_STORE")
                        ShareHelper.shareToGeneral(context, formattedText)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("مشاركة النص أو التطبيقات الأخرى 📋", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = null
    )
}
