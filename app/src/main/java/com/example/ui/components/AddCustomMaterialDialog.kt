package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream

private fun savePickedImageToAppStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val imagesFolder = File(context.filesDir, "material_images").apply {
            if (!exists()) mkdirs()
        }
        val outputFile = File(imagesFolder, "mat_img_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outputFile).use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()
        outputFile.toURI().toString()
    } catch (e: Exception) {
        e.printStackTrace()
        uri.toString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomMaterialDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        nameAr: String,
        nameFr: String,
        size: String,
        quantity: Double,
        unit: String,
        category: String,
        price: Double,
        imageUri: String?,
        notes: String
    ) -> Unit
) {
    var nameAr by remember { mutableStateOf("") }
    var nameFr by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("25mm") }
    var quantityText by remember { mutableStateOf("1") }
    var priceText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("قطعة") }
    var category by remember { mutableStateOf("PPR") }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    val categories = listOf(
        "PPR",
        "PVC",
        "Robinetterie",
        "Sanitaire",
        "Chauffage",
        "Accessoires",
        "Autre"
    )

    val units = listOf(
        "قطعة",
        "متر",
        "علبة",
        "رول",
        "كيس",
        "طقم",
        "لتر",
        "كرتونة"
    )

    val context = LocalContext.current

    // Image Picker Launcher from Phone Gallery / Files
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val savedPath = savePickedImageToAppStorage(context, uri)
            selectedImageUri = savedPath
            Toast.makeText(context, "تم اختيار وصنع نسخة محلية للصورة بنجاح 📸", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dialog Title Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                    Text(
                        text = "📦 إضافة مادة جديدة",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Image Picker Box (مع خيار التقاط / اختيار صورة من المعرض)
                Text(
                    text = "صورة القطعة / المادة:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { imagePickerLauncher.launch("image/*") }
                ) {
                    if (!selectedImageUri.isNullOrEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "صورة المادة",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف الصورة",
                                    tint = Color.White
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "اضغط هنا لاختيار صورة من معرض الصور",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "تساعد في التعرف السريع على القطعة",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Text Field: Arabic Name
                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text("اسم المادة (بالعربية) *") },
                    placeholder = { Text("مثال: محبس كروي، كوع خاص...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Text Field: French Name
                OutlinedTextField(
                    value = nameFr,
                    onValueChange = { nameFr = it },
                    label = { Text("الاسم بالفرنسية (اختياري)") },
                    placeholder = { Text("مثال: Vanne à sphère") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Dropdown Menu 1: Category (الفئة)
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الفئة *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    category = selectionOption
                                    categoryExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Row: Size & Initial Quantity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = size,
                        onValueChange = { size = it },
                        label = { Text("المقاس") },
                        placeholder = { Text("25mm, 1/2\"...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("الكمية المبدئية") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dropdown Menu 2: Unit (الوحدة)
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = !unitExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الوحدة *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        units.forEach { selectionUnit ->
                            DropdownMenuItem(
                                text = { Text(selectionUnit, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    unit = selectionUnit
                                    unitExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Text Field: Material Price (سعر الوحدة)
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("سعر الوحدة (د.ج / DA) (اختياري)") },
                    placeholder = { Text("مثال: 450, 1200...") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Text Field: Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات / وصف إضافي") },
                    placeholder = { Text("أي تفاصيل إضافية عن المادة...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Button
                Button(
                    onClick = {
                        val qty = quantityText.toDoubleOrNull() ?: 1.0
                        val price = priceText.toDoubleOrNull() ?: 0.0
                        if (nameAr.isNotBlank()) {
                            onConfirm(
                                nameAr,
                                nameFr,
                                size,
                                qty,
                                unit,
                                category,
                                price,
                                selectedImageUri,
                                notes
                            )
                        }
                    },
                    enabled = nameAr.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ المادة في قاعدة البيانات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
