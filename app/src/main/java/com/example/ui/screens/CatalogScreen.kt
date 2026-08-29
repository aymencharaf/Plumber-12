package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.MaterialEntity
import com.example.data.preset.PlumbingLibraryData
import com.example.ui.components.AddEditMaterialDialog
import com.example.ui.components.PlumbingIcon
import com.example.ui.viewmodel.PlumberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    viewModel: PlumberViewModel,
    onNavigateToProject: () -> Unit
) {
    val context = LocalContext.current
    val projects by viewModel.allProjects.collectAsStateWithLifecycle()
    val libraryMaterials by viewModel.libraryMaterials.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }

    var selectedDetailMaterial by remember { mutableStateOf<MaterialEntity?>(null) }
    var selectedAddToProjectMaterial by remember { mutableStateOf<MaterialEntity?>(null) }
    var showAddMaterialDialog by remember { mutableStateOf(false) }
    var editingMaterialInDialog by remember { mutableStateOf<MaterialEntity?>(null) }

    val categories = listOf("الكل", "PPR", "PVC", "PEHD", "أخرى")

    val presetEntities = remember {
        PlumbingLibraryData.ALL_MATERIALS.map { pm ->
            MaterialEntity(
                id = pm.key,
                nameAr = pm.nameAr,
                nameFr = pm.nameFr,
                code = pm.key.uppercase().replace("_", "-"),
                category = pm.category,
                size = pm.defaultSize,
                unit = pm.defaultUnit,
                price = pm.defaultUnitPrice ?: 0.0,
                image = pm.imageUri ?: "",
                iconType = pm.iconType,
                isCustom = false
            )
        }
    }

    val combinedLibraryMaterials = (libraryMaterials + presetEntities).distinctBy { it.id }

    val filteredMaterials = combinedLibraryMaterials.filter { mat ->
        val matchesCategory = (selectedCategory == "الكل") || mat.category.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                mat.nameAr.contains(searchQuery, ignoreCase = true) ||
                mat.nameFr.contains(searchQuery, ignoreCase = true) ||
                mat.code.contains(searchQuery, ignoreCase = true) ||
                mat.size.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المواد", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Menu, contentDescription = "القائمة")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddMaterialDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة مادة")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Input with Filter Icon
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن مادة...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = { Icon(Icons.Default.FilterList, contentDescription = "فلترة", tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                singleLine = true
            )

            // Category Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        onClick = { selectedCategory = cat },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    ) {
                        Text(
                            text = cat,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Products List
            if (filteredMaterials.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📦", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لم يتم العثور على مواد",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredMaterials, key = { it.id }) { material ->
                        MaterialRowCard(
                            material = material,
                            onItemClick = { selectedDetailMaterial = material },
                            onMoreClick = { editingMaterialInDialog = material }
                        )
                    }
                }
            }
        }
    }

    // Modal 1: Material Details Sheet (Mockup Screen 4)
    selectedDetailMaterial?.let { material ->
        MaterialDetailModal(
            material = material,
            onDismiss = { selectedDetailMaterial = null },
            onAddToProject = {
                val targetMat = selectedDetailMaterial
                selectedDetailMaterial = null
                selectedAddToProjectMaterial = targetMat
            },
            onEdit = {
                val targetMat = selectedDetailMaterial
                selectedDetailMaterial = null
                editingMaterialInDialog = targetMat
            }
        )
    }

    // Modal 2: Add to Project Sheet (Mockup Screen 5)
    selectedAddToProjectMaterial?.let { material ->
        AddToProjectModal(
            material = material,
            projects = projects,
            onDismiss = { selectedAddToProjectMaterial = null },
            onConfirmAdd = { projectId, qty ->
                viewModel.selectProject(projectId)
                viewModel.addLibraryMaterialToProject(
                    material = material,
                    size = material.size,
                    quantity = qty.toDouble(),
                    unit = material.unit,
                    notes = "تمت الإضافة من مكتبة المواد الشاملة"
                )
                selectedAddToProjectMaterial = null
                Toast.makeText(context, "تمت إضافة المادة إلى المشروع بنجاح", Toast.LENGTH_SHORT).show()
                onNavigateToProject()
            }
        )
    }

    // Dialog 3: Add / Edit Material
    if (showAddMaterialDialog || editingMaterialInDialog != null) {
        AddEditMaterialDialog(
            initialMaterial = editingMaterialInDialog,
            onDismiss = {
                showAddMaterialDialog = false
                editingMaterialInDialog = null
            },
            onSave = { savedMaterial ->
                if (editingMaterialInDialog != null) {
                    viewModel.updateMaterialInLibrary(savedMaterial)
                } else {
                    viewModel.addMaterialToLibrary(savedMaterial)
                }
                showAddMaterialDialog = false
                editingMaterialInDialog = null
                Toast.makeText(context, "تم حفظ المادة بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun MaterialRowCard(
    material: MaterialEntity,
    onItemClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Card(
        onClick = onItemClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Right Price Info
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "دج ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = material.price.toInt().toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = material.unit,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Center Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = material.nameAr,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (material.nameFr.isNotBlank()) {
                    Text(
                        text = material.nameFr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = material.code.ifBlank { "PPR-C90-25" },
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = material.size,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Left Image Thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                if (material.image.isNotEmpty()) {
                    AsyncImage(
                        model = material.image,
                        contentDescription = material.nameAr,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    PlumbingIcon(
                        iconType = material.iconType,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}

// Modal 1: Material Detail Sheet (Mockup Screen 4)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialDetailModal(
    material: MaterialEntity,
    onDismiss: () -> Unit,
    onAddToProject: () -> Unit,
    onEdit: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                }

                Text(
                    text = "تفاصيل المادة",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "إغلاق")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Large Image Display Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF8FAFC)),
                contentAlignment = Alignment.Center
            ) {
                if (material.image.isNotEmpty()) {
                    AsyncImage(
                        model = material.image,
                        contentDescription = material.nameAr,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    PlumbingIcon(
                        iconType = material.iconType,
                        modifier = Modifier.size(110.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = material.nameAr,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (material.nameFr.isNotBlank()) {
                Text(
                    text = material.nameFr,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Specs Table
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecRow(label = "الكود", value = material.code.ifBlank { "PPR-C90-25" }, icon = Icons.Default.QrCode)
                SpecRow(label = "الفئة", value = material.category, icon = Icons.Default.Category)
                SpecRow(label = "المقاس", value = material.size, icon = Icons.Default.Straighten)
                SpecRow(label = "الوحدة", value = material.unit, icon = Icons.Default.Inventory2)
                SpecRow(label = "السعر", value = "${material.price.toInt()} دج", icon = Icons.Default.Sell)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = onAddToProject,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إضافة إلى المشروع", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تعديل المادة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SpecRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Modal 2: Add to Project Sheet (Mockup Screen 5)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToProjectModal(
    material: MaterialEntity,
    projects: List<com.example.data.model.Project>,
    onDismiss: () -> Unit,
    onConfirmAdd: (projectId: Long, quantity: Int) -> Unit
) {
    var quantity by remember { mutableIntStateOf(12) }
    var selectedProject by remember { mutableStateOf(projects.firstOrNull()) }
    var expandedProjectDropdown by remember { mutableStateOf(false) }

    val totalPrice = material.price * quantity

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "إضافة إلى المشروع",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Material Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(material.nameAr, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (material.nameFr.isNotBlank()) {
                            Text(material.nameFr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                material.size,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        if (material.image.isNotEmpty()) {
                            AsyncImage(
                                model = material.image,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            PlumbingIcon(iconType = material.iconType, modifier = Modifier.size(38.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quantity Selector Section
            Text(
                text = "الكمية المطلوبة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedIconButton(
                        onClick = { if (quantity > 1) quantity-- },
                        shape = CircleShape
                    ) {
                        Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "$quantity",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )

                    OutlinedIconButton(
                        onClick = { quantity++ },
                        shape = CircleShape
                    ) {
                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = material.unit,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Price Summary Rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("السعر الفردي:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${material.price.toInt()} دج", fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("الإجمالي:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "${totalPrice.toInt()} دج",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Choose Project Dropdown
            Text(
                text = "اختر المشروع",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            ExposedDropdownMenuBox(
                expanded = expandedProjectDropdown,
                onExpandedChange = { expandedProjectDropdown = !expandedProjectDropdown },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedProject?.title ?: "مشروع جديد",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProjectDropdown) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = expandedProjectDropdown,
                    onDismissRequest = { expandedProjectDropdown = false }
                ) {
                    projects.forEach { proj ->
                        DropdownMenuItem(
                            text = { Text(proj.title, fontWeight = FontWeight.Bold) },
                            onClick = {
                                selectedProject = proj
                                expandedProjectDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    selectedProject?.let { proj ->
                        onConfirmAdd(proj.id, quantity)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = selectedProject != null
            ) {
                Text("إضافة إلى المشروع", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
