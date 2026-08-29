package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlumbingMaterial
import com.example.data.preset.PlumbingLibraryData
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuantityPickerDialog(
    material: PlumbingMaterial,
    initialSize: String? = null,
    initialQuantity: Double = 1.0,
    initialUnit: String? = null,
    initialNotes: String = "",
    onDismiss: () -> Unit,
    onConfirm: (selectedSize: String, quantity: Double, selectedUnit: String, notes: String) -> Unit
) {
    var selectedSize by remember { mutableStateOf(initialSize ?: material.defaultSize) }
    var selectedUnit by remember { mutableStateOf(initialUnit ?: material.defaultUnit) }
    var quantityText by remember { mutableStateOf(if (initialQuantity % 1.0 == 0.0) initialQuantity.toInt().toString() else initialQuantity.toString()) }
    var notes by remember { mutableStateOf(initialNotes) }

    val currentQty = quantityText.toDoubleOrNull() ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Close icon + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = material.nameAr,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (material.nameFr.isNotBlank()) {
                        Text(
                            text = material.nameFr,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Product Visual Icon Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PlumbingIcon(
                        iconType = material.iconType,
                        modifier = Modifier.size(72.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "المادة الحالية:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${material.nameAr} - $selectedSize",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (material.descriptionAr.isNotBlank()) {
                            Text(
                                text = material.descriptionAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Size Selector Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "اختر المقاس / Diameter:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(material.availableSizes) { sizeOption ->
                        FilterChip(
                            selected = (selectedSize == sizeOption),
                            onClick = { selectedSize = sizeOption },
                            label = { Text(sizeOption, fontWeight = FontWeight.Bold) },
                            leadingIcon = if (selectedSize == sizeOption) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Unit Selector Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "الوحدة / Unit:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PlumbingLibraryData.UNITS) { unitOption ->
                        FilterChip(
                            selected = (selectedUnit == unitOption),
                            onClick = { selectedUnit = unitOption },
                            label = { Text(unitOption) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Pipe Calculation Helper Alert (if unit == "متر")
            if (selectedUnit == "متر") {
                val pipesNeeded = ceil(currentQty / 4.0).toInt()
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💡 حساب الأنابيب:",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                        Text(
                            text = "المطلوب: $quantityText متر | ما يعادل تقريباً: $pipesNeeded أنبوب بطول 4 متر",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Quantity Control Section (Large Interactive Stepper)
            Text(
                text = "كم تحتاج من هذه القطعة؟",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Decrement Button
                Surface(
                    onClick = {
                        val valDouble = quantityText.toDoubleOrNull() ?: 1.0
                        if (valDouble > 1.0) {
                            val newNum = valDouble - 1.0
                            quantityText = if (newNum % 1.0 == 0.0) newNum.toInt().toString() else newNum.toString()
                        }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "أنقص",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Number Input Box
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .width(130.dp)
                        .height(68.dp),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                // Increment Button
                Surface(
                    onClick = {
                        val valDouble = quantityText.toDoubleOrNull() ?: 0.0
                        val newNum = valDouble + 1.0
                        quantityText = if (newNum % 1.0 == 0.0) newNum.toInt().toString() else newNum.toString()
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "زد",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Number Presets (1, 2, 5, 10, 20, 50)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(1, 2, 5, 10, 20, 50).forEach { presetVal ->
                    SuggestionChip(
                        onClick = { quantityText = presetVal.toString() },
                        label = { Text("$presetVal", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Optional Note Input
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظة إضافية (اختياري)") },
                placeholder = { Text("مثال: الجدار الخارجي / للخط الرئيسي") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    val finalQty = quantityText.toDoubleOrNull() ?: 1.0
                    if (finalQty > 0) {
                        onConfirm(selectedSize, finalQty, selectedUnit, notes)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "✓ إضافة إلى مواد المشروع",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
