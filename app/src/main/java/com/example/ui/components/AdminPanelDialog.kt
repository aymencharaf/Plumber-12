package com.example.ui.components

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MaterialEntity
import com.example.ui.viewmodel.PlumberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelDialog(
    viewModel: PlumberViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: المواد (Materials), 1: العمال والورشة (Workers & Store)

    // Material State
    val libraryMaterials by viewModel.libraryMaterials.collectAsStateWithLifecycle()
    var showAddMaterialDialog by remember { mutableStateOf(false) }
    var editingMaterial by remember { mutableStateOf<MaterialEntity?>(null) }
    var materialToDelete by remember { mutableStateOf<MaterialEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Worker & Store State
    val storeName by viewModel.storeName.collectAsStateWithLifecycle()
    val storePhone by viewModel.storePhone.collectAsStateWithLifecycle()
    val storeWhatsapp by viewModel.storeWhatsapp.collectAsStateWithLifecycle()
    val activeWorker by viewModel.activeWorker.collectAsStateWithLifecycle()
    val managerName by viewModel.managerName.collectAsStateWithLifecycle()
    val workersList by viewModel.workersList.collectAsStateWithLifecycle()

    var storeNameInput by remember { mutableStateOf(storeName) }
    var storePhoneInput by remember { mutableStateOf(storePhone) }
    var storeWhatsappInput by remember { mutableStateOf(storeWhatsapp) }
    var managerNameInput by remember { mutableStateOf(managerName) }

    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var editingWorkerIndex by remember { mutableStateOf<Int?>(null) }
    var editingWorkerNameInput by remember { mutableStateOf("") }
    var workerToDelete by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Header Bar
                Surface(
                    color = Color(0xFF1E3A8A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF3B82F6),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AdminPanelSettings,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "لوحة تحكم المدير الإحترافية",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "حساب المدير النشط (0669964145)",
                                        fontSize = 11.sp,
                                        color = Color(0xFFBFDBFE)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    onClick = {
                                        viewModel.setManagerLoggedIn(false)
                                        Toast.makeText(context, "تم تسجيل الخروج وإخفاء لوحة التحكم", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFDC2626)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ExitToApp,
                                            contentDescription = "تسجيل الخروج",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "خروج",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        // Tab selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E293B))
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedTab == 0) Color(0xFF2563EB) else Color.Transparent)
                                    .clickable { selectedTab = 0 }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Category,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "إضافة وتعديل المواد",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedTab == 1) Color(0xFF2563EB) else Color.Transparent)
                                    .clickable { selectedTab = 1 }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Group,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "تعديل معلومات العمال",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // TAB CONTENT
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (selectedTab == 0) {
                        // TAB 0: MATERIALS MANAGEMENT
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Add Material Button Banner
                            Button(
                                onClick = {
                                    editingMaterial = null
                                    showAddMaterialDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إضافة مادة جديدة للتطبيق",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            // Search Field
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("بحث باسم المادة أو الفئة...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF059669)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                            )

                            val filteredList = libraryMaterials.filter { mat ->
                                searchQuery.isBlank() ||
                                        mat.nameAr.contains(searchQuery, ignoreCase = true) ||
                                        mat.nameFr.contains(searchQuery, ignoreCase = true) ||
                                        mat.category.contains(searchQuery, ignoreCase = true)
                            }

                            if (filteredList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Category,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "لا توجد مواد مضافة مخصصة بعد",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "انقر فوق زر 'إضافة مادة جديدة للتطبيق' بالأعلى لإنشاء مادة",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredList, key = { it.id }) { material ->
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = Color(0xFFECFDF5),
                                                        modifier = Modifier.size(44.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            PlumbingIcon(
                                                                iconType = material.iconType,
                                                                modifier = Modifier.size(28.dp)
                                                            )
                                                        }
                                                    }

                                                    Column {
                                                        Text(
                                                            text = material.nameAr,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 15.sp,
                                                            color = Color(0xFF1E293B)
                                                        )
                                                        Text(
                                                            text = "الفئة: ${material.category} | المقاس: ${material.size}",
                                                            fontSize = 12.sp,
                                                            color = Color(0xFF64748B)
                                                        )
                                                        Text(
                                                            text = "السعر الافتراضي: ${material.price.toInt()} د.ج / ${material.unit}",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF059669)
                                                        )
                                                    }
                                                }

                                                Row {
                                                    IconButton(onClick = {
                                                        editingMaterial = material
                                                        showAddMaterialDialog = true
                                                    }) {
                                                        Icon(
                                                            Icons.Default.Edit,
                                                            contentDescription = "تعديل المادة",
                                                            tint = Color(0xFF2563EB)
                                                        )
                                                    }
                                                    IconButton(onClick = {
                                                        materialToDelete = material
                                                    }) {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            contentDescription = "حذف المادة",
                                                            tint = Color(0xFFDC2626)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // TAB 1: WORKERS & STORE MANAGEMENT
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Manager & Store Settings Card
                            item {
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "معلومات الورشة والإدارة المشرفة",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF1E3A8A)
                                        )

                                        OutlinedTextField(
                                            value = managerNameInput,
                                            onValueChange = { managerNameInput = it },
                                            label = { Text("اسم المدير المشرف") },
                                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2563EB)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = storeNameInput,
                                            onValueChange = { storeNameInput = it },
                                            label = { Text("اسم ورشة / محل السباكة") },
                                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFF2563EB)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = storePhoneInput,
                                            onValueChange = { storePhoneInput = it },
                                            label = { Text("رقم هاتف الاتصال المباشر للورشة") },
                                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF2563EB)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = storeWhatsappInput,
                                            onValueChange = { storeWhatsappInput = it },
                                            label = { Text("رقم الواتساب (WhatsApp) لاستلام الطلبات") },
                                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF2563EB)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )

                                        Button(
                                            onClick = {
                                                viewModel.updateTeamStoreSettings(
                                                    storeNameVal = storeNameInput.ifBlank { "ورشة السباكة الإحترافية" },
                                                    storePhoneVal = storePhoneInput.ifBlank { "0669076802" },
                                                    activeWorkerVal = activeWorker,
                                                    managerNameVal = managerNameInput.ifBlank { "المدير أيمين" },
                                                    storeWhatsappVal = storeWhatsappInput.ifBlank { "0669964145" }
                                                )
                                                Toast.makeText(context, "تم حفظ معلومات الإدارة والورشة والواتساب بنجاح", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("حفظ معلومات الورشة والإدارة", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Workers List Section
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "قائمة العمال والفنيين (${workersList.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF1E293B)
                                    )

                                    Button(
                                        onClick = { showAddWorkerDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("إضافة عامل جديد", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            items(workersList.indices.toList(), key = { it }) { index ->
                                val workerName = workersList[index]
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFDBEAFE),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.Engineering,
                                                        contentDescription = null,
                                                        tint = Color(0xFF1D4ED8),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            Column {
                                                Text(
                                                    text = workerName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Color(0xFF1E293B)
                                                )
                                                Text(
                                                    text = "فني تركيبات سباكة",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }

                                        Row {
                                            IconButton(onClick = {
                                                editingWorkerIndex = index
                                                editingWorkerNameInput = workerName
                                            }) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "تعديل اسم العامل",
                                                    tint = Color(0xFF2563EB)
                                                )
                                            }

                                            IconButton(onClick = {
                                                workerToDelete = workerName
                                            }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "حذف العامل",
                                                    tint = Color(0xFFDC2626)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOGS

    // Add / Edit Material Dialog
    if (showAddMaterialDialog) {
        AddEditMaterialDialog(
            initialMaterial = editingMaterial,
            onDismiss = { showAddMaterialDialog = false },
            onSave = { savedMaterial ->
                if (editingMaterial == null) {
                    viewModel.addMaterialToLibrary(savedMaterial)
                    Toast.makeText(context, "تمت إضافة المادة بنجاح للتطبيق", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateMaterialInLibrary(savedMaterial)
                    Toast.makeText(context, "تم تحديث بيانات المادة بنجاح", Toast.LENGTH_SHORT).show()
                }
                showAddMaterialDialog = false
            }
        )
    }

    // Delete Material Confirmation
    if (materialToDelete != null) {
        AlertDialog(
            onDismissRequest = { materialToDelete = null },
            title = { Text("حذف المادة من التطبيق") },
            text = { Text("هل أنت تأكد من رغبتك في حذف المادة '${materialToDelete?.nameAr}'؟") },
            confirmButton = {
                Button(
                    onClick = {
                        materialToDelete?.let { viewModel.deleteMaterialFromLibrary(it.id) }
                        materialToDelete = null
                        Toast.makeText(context, "تم حذف المادة بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { materialToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Add Worker Dialog with Password & Firestore Link
    if (showAddWorkerDialog) {
        var newWorkerName by remember { mutableStateOf("") }
        var newWorkerPhone by remember { mutableStateOf("") }
        var newWorkerPass by remember { mutableStateOf("") }
        var selectedRole by remember { mutableStateOf("WORKER") }

        AlertDialog(
            onDismissRequest = { showAddWorkerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF059669))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إضافة عامل جديد بكلمة مرور 🔐", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF16A34A).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "🟢 سيتم حفظ الحساب محلياً ومزامنته فورياً مع قاعدة البيانات Firestore.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D),
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = newWorkerName,
                        onValueChange = { newWorkerName = it },
                        label = { Text("اسم العامل الكامل") },
                        placeholder = { Text("مثال: علي فني التركيبات") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newWorkerPhone,
                        onValueChange = { newWorkerPhone = it },
                        label = { Text("رقم الهاتف (اسم الدخول)") },
                        placeholder = { Text("0660000000") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newWorkerPass,
                        onValueChange = { newWorkerPass = it },
                        label = { Text("كلمة المرور للدخول") },
                        placeholder = { Text("••••••••") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الصلاحية:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        FilterChip(
                            selected = selectedRole == "WORKER",
                            onClick = { selectedRole = "WORKER" },
                            label = { Text("عامل") }
                        )
                        FilterChip(
                            selected = selectedRole == "ADMIN",
                            onClick = { selectedRole = "ADMIN" },
                            label = { Text("إدارة") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newWorkerName.isNotBlank() && newWorkerPhone.isNotBlank() && newWorkerPass.isNotBlank()) {
                            viewModel.createWorkerAccount(
                                name = newWorkerName.trim(),
                                phone = newWorkerPhone.trim(),
                                email = "${newWorkerPhone.trim()}@plumber.com",
                                pass = newWorkerPass.trim(),
                                role = selectedRole
                            ) { success, msg ->
                                Toast.makeText(context, "$msg 🟢 (مرتبط بـ Firestore)", Toast.LENGTH_SHORT).show()
                                if (success) {
                                    showAddWorkerDialog = false
                                }
                            }
                        } else {
                            // Fallback if basic details provided
                            val currentList = workersList.toMutableList()
                            currentList.add(newWorkerName.trim())
                            viewModel.updateWorkersList(currentList)
                            Toast.makeText(context, "تمت إضافة العامل بنجاح 🟢", Toast.LENGTH_SHORT).show()
                            showAddWorkerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("حفظ وربط بـ Firestore 🔥", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWorkerDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Edit Worker Name Dialog
    if (editingWorkerIndex != null) {
        AlertDialog(
            onDismissRequest = { editingWorkerIndex = null },
            title = { Text("تعديل اسم العامل") },
            text = {
                OutlinedTextField(
                    value = editingWorkerNameInput,
                    onValueChange = { editingWorkerNameInput = it },
                    label = { Text("الاسم المعدل") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idx = editingWorkerIndex
                        if (idx != null && editingWorkerNameInput.isNotBlank()) {
                            val currentList = workersList.toMutableList()
                            if (idx in currentList.indices) {
                                currentList[idx] = editingWorkerNameInput.trim()
                                viewModel.updateWorkersList(currentList)
                                Toast.makeText(context, "تم تعديل اسم العامل بنجاح", Toast.LENGTH_SHORT).show()
                            }
                            editingWorkerIndex = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("حفظ التعديل")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingWorkerIndex = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Worker Confirmation
    if (workerToDelete != null) {
        AlertDialog(
            onDismissRequest = { workerToDelete = null },
            title = { Text("حذف العامل") },
            text = { Text("هل أنت متأكد من حذف العامل '${workerToDelete}' من قائمة الفريق؟") },
            confirmButton = {
                Button(
                    onClick = {
                        val currentList = workersList.toMutableList()
                        currentList.remove(workerToDelete)
                        viewModel.updateWorkersList(currentList)
                        workerToDelete = null
                        Toast.makeText(context, "تم حذف العامل من القائمة", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { workerToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
