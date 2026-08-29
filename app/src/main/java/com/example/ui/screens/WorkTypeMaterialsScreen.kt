package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlumbingMaterial
import com.example.data.preset.PlumbingLibraryData
import com.example.ui.components.AddCustomMaterialDialog
import com.example.ui.components.PlumbingIcon
import com.example.ui.components.QuantityPickerDialog
import com.example.ui.viewmodel.PlumberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkTypeMaterialsScreen(
    viewModel: PlumberViewModel,
    onBack: () -> Unit,
    onNavigateToProjectMaterials: () -> Unit
) {
    val currentProject by viewModel.currentProject.collectAsState()
    val projectItems by viewModel.currentProjectItems.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }

    // State for Picker Modal
    var activeMaterialForPicker by remember { mutableStateOf<PlumbingMaterial?>(null) }
    var showAddCustomDialog by remember { mutableStateOf(false) }

    val customMaterials by viewModel.customMaterials.collectAsState()
    val dbLibraryMaterials by viewModel.libraryMaterials.collectAsState()

    val dbPlumbingMaterials = dbLibraryMaterials.map { entity ->
        PlumbingMaterial(
            key = entity.id,
            nameAr = entity.nameAr,
            nameFr = entity.nameFr,
            category = entity.category,
            availableSizes = listOf(entity.size),
            defaultSize = entity.size,
            defaultUnit = entity.unit,
            defaultUnitPrice = entity.price,
            iconType = entity.iconType,
            imageUri = entity.image.ifBlank { null },
            descriptionAr = "الكود: ${entity.code}"
        )
    }

    val customPlumbingMaterials = customMaterials.map { custom ->
        PlumbingMaterial(
            key = "custom_${custom.id}",
            nameAr = custom.nameAr,
            nameFr = custom.nameFr,
            category = custom.category,
            availableSizes = listOf(custom.defaultSize),
            defaultSize = custom.defaultSize,
            defaultUnit = custom.defaultUnit,
            defaultUnitPrice = custom.defaultPrice,
            iconType = custom.iconType,
            imageUri = custom.imageUri,
            descriptionAr = custom.notes
        )
    }

    // Resolve materials to show
    val workTypeObj = PlumbingLibraryData.WORK_TYPES.find { it.key == currentProject?.workTypeKey }
    val combinedMaterials = (dbPlumbingMaterials + PlumbingLibraryData.ALL_MATERIALS + customPlumbingMaterials)
        .distinctBy { it.key }

    val filteredMaterials = combinedMaterials.filter { mat ->
        val matchesCategory = (selectedCategory == "الكل") || mat.category.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                mat.nameAr.contains(searchQuery, ignoreCase = true) ||
                mat.nameFr.contains(searchQuery, ignoreCase = true) ||
                mat.category.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentProject?.title ?: "تحديد مواد المشروع",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "اضغط على صورة المادة لتحديد الكمية",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddCustomDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة مادة مخصصة")
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
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onNavigateToProjectMaterials,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.ListAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "عرض مواد المشروع (${projectItems.size} نوع)",
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
                .padding(horizontal = 12.dp)
        ) {
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن قطعة (كوع، أنبوب، Coude، Té...)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Category Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlumbingLibraryData.CATEGORIES.forEach { (catAr, catFr) ->
                    FilterChip(
                        selected = (selectedCategory == catAr),
                        onClick = { selectedCategory = catAr },
                        label = { Text(catAr, fontSize = 12.sp) }
                    )
                }
            }

            // Products Grid
            if (filteredMaterials.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("لم يتم العثور على مواد مطابقة", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showAddCustomDialog = true }) {
                            Text("+ إضافة مادة مخصصة جديد")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredMaterials) { material ->
                        // Check if already in project
                        val addedItem = projectItems.find { it.materialKey == material.key }
                        val isAdded = addedItem != null

                        Card(
                            onClick = { activeMaterialForPicker = material },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            border = if (isAdded) 
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else 
                                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Product Image / Icon Target Box
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isAdded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(84.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        PlumbingIcon(
                                            iconType = material.iconType,
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = material.nameAr,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = material.nameFr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = material.defaultSize,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (isAdded && addedItem != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${addedItem.quantity.toInt()} ${addedItem.unit}",
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = { activeMaterialForPicker = material },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text("+ حدد الكمية", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Active Picker Sheet
    activeMaterialForPicker?.let { material ->
        val existingItem = projectItems.find { it.materialKey == material.key }
        QuantityPickerDialog(
            material = material,
            initialSize = existingItem?.size ?: material.defaultSize,
            initialQuantity = existingItem?.quantity ?: 1.0,
            initialUnit = existingItem?.unit ?: material.defaultUnit,
            initialNotes = existingItem?.notes ?: "",
            onDismiss = { activeMaterialForPicker = null },
            onConfirm = { size, qty, unit, notes ->
                viewModel.addMaterialToProject(
                    material = material,
                    size = size,
                    quantity = qty,
                    unit = unit,
                    notes = notes
                )
                activeMaterialForPicker = null
            }
        )
    }

    // Custom Material Dialog
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
}
