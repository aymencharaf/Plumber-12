package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MaterialEntity
import com.example.data.preset.PlumbingLibraryData
import com.example.ui.util.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMaterialDialog(
    initialMaterial: MaterialEntity? = null,
    onDismiss: () -> Unit,
    onSave: (MaterialEntity) -> Unit
) {
    val context = LocalContext.current

    var nameAr by remember { mutableStateOf(initialMaterial?.nameAr ?: "") }
    var nameFr by remember { mutableStateOf(initialMaterial?.nameFr ?: "") }
    var code by remember { mutableStateOf(initialMaterial?.code ?: "") }
    var selectedCategory by remember { mutableStateOf(initialMaterial?.category ?: "PPR") }
    var size by remember { mutableStateOf(initialMaterial?.size ?: "25mm") }
    var selectedUnit by remember { mutableStateOf(initialMaterial?.unit ?: "قطعة") }
    var priceInput by remember { mutableStateOf(initialMaterial?.price?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "0") }
    var imagePath by remember { mutableStateOf(initialMaterial?.image ?: "") }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val savedPath = ImageUtils.saveBitmapToFile(context, bitmap)
            if (savedPath.isNotEmpty()) {
                imagePath = savedPath
                Toast.makeText(context, "تم التقاط الصورة بنجاح", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageUtils.copyUriToFile(context, uri)
            if (savedPath.isNotEmpty()) {
                imagePath = savedPath
                Toast.makeText(context, "تم اختيار الصورة بنجاح", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val categories = listOf("PPR", "PVC", "PEHD", "Robinetterie", "Accessoires")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialMaterial == null) "إضافة مادة جديدة للمكتبة" else "تعديل مادة في المكتبة",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Image Preview & Photo Controls
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imagePath.isNotEmpty()) {
                                AsyncImage(
                                    model = imagePath,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                PlumbingIcon(
                                    iconType = when (selectedCategory) {
                                        "PPR" -> "elbow"
                                        "PVC" -> "pipe"
                                        "PEHD" -> "pipe"
                                        "Robinetterie" -> "valve"
                                        else -> "fitting"
                                    },
                                    modifier = Modifier.size(80.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { cameraLauncher.launch(null) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("كاميرا", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("معرض", fontSize = 11.sp)
                            }

                            if (imagePath.isNotEmpty()) {
                                IconButton(
                                    onClick = { imagePath = "" }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "حذف الصورة",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // Name Ar
                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text("الاسم بالعربية *") },
                    placeholder = { Text("مثال: كوع PPR 90 درجة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Name Fr
                OutlinedTextField(
                    value = nameFr,
                    onValueChange = { nameFr = it },
                    label = { Text("الاسم بالفرنسية (اختياري)") },
                    placeholder = { Text("مثال: Coude PPR 90°") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Code
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("كود المادة Code") },
                    placeholder = { Text("مثال: PPR-C90-25") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category
                Text("الفئة / Type:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = (selectedCategory == cat),
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                // Size
                OutlinedTextField(
                    value = size,
                    onValueChange = { size = it },
                    label = { Text("المقاس Size") },
                    placeholder = { Text("مثال: 25mm, 110mm, 1/2...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Unit & Price Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("السعر (د.ج)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = selectedUnit,
                        onValueChange = { selectedUnit = it },
                        label = { Text("الوحدة") },
                        placeholder = { Text("قطعة، متر...") },
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
                    if (nameAr.isBlank()) {
                        Toast.makeText(context, "يرجى إدخال اسم المادة بالعربية", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val priceVal = priceInput.toDoubleOrNull() ?: 0.0
                    val finalCode = code.ifBlank { "${selectedCategory.uppercase()}-${System.currentTimeMillis() % 10000}" }
                    val finalId = initialMaterial?.id ?: "mat_${System.currentTimeMillis()}"

                    val item = MaterialEntity(
                        id = finalId,
                        nameAr = nameAr.trim(),
                        nameFr = nameFr.trim(),
                        code = finalCode.trim(),
                        category = selectedCategory,
                        size = size.trim().ifBlank { "قياسي" },
                        unit = selectedUnit.trim().ifBlank { "قطعة" },
                        price = priceVal,
                        image = imagePath,
                        iconType = when (selectedCategory) {
                            "PPR" -> "elbow"
                            "PVC" -> "pipe"
                            "PEHD" -> "pipe"
                            "Robinetterie" -> "valve"
                            else -> "fitting"
                        },
                        isCustom = true
                    )
                    onSave(item)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("حفظ المادة", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
