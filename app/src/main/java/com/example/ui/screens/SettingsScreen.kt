package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.unit.sp
import com.example.data.sync.SyncCodeManager
import com.example.ui.components.GoogleDriveBackupCard
import com.example.ui.viewmodel.PlumberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PlumberViewModel,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val currentStoreName by viewModel.storeName.collectAsState()
    val currentStorePhone by viewModel.storePhone.collectAsState()
    val currentStoreWhatsapp by viewModel.storeWhatsapp.collectAsState()
    val currentActiveWorker by viewModel.activeWorker.collectAsState()
    val workersList by viewModel.workersList.collectAsState()
    val isManagerLoggedIn by viewModel.isManagerLoggedIn.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var storeNameInput by remember(currentStoreName) { mutableStateOf(currentStoreName) }
    var storePhoneInput by remember(currentStorePhone) { mutableStateOf(currentStorePhone) }
    var storeWhatsappInput by remember(currentStoreWhatsapp) { mutableStateOf(currentStoreWhatsapp) }
    var selectedWorkerInput by remember(currentActiveWorker) { mutableStateOf(currentActiveWorker) }
    var isWorkerDropdownExpanded by remember { mutableStateOf(false) }

    var showEditWorkersDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showManagerLoginDialogSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات وإدارة فريق المحل", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                        }
                    }
                }
            )
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
            // App Info Header
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Plumbing,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Plumber — مساعد السباك والمحل",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "إصدار v1.2.0 • نظام تجهيز وطلب اللوازم لفريق الورشات الـ 10",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 🏬 Store & Workers Team Settings Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إعدادات المحل واستلام الطلبات",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = "حدد اسم المحل ورقم الهاتف المباشر ورقم الواتساب المخصص لاستلام الطلبات:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Store Name
                    OutlinedTextField(
                        value = storeNameInput,
                        onValueChange = { storeNameInput = it },
                        label = { Text("اسم المحل / الورشة الرئيسية") },
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Store Phone
                    OutlinedTextField(
                        value = storePhoneInput,
                        onValueChange = { storePhoneInput = it },
                        label = { Text("رقم هاتف العمل / الاتصال المباشر") },
                        placeholder = { Text("مثال: 0660000000") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Store WhatsApp
                    OutlinedTextField(
                        value = storeWhatsappInput,
                        onValueChange = { storeWhatsappInput = it },
                        label = { Text("رقم الواتساب (WhatsApp) المخصص للطلبيات والمشاركة") },
                        placeholder = { Text("مثال: 0669964145") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // Manager Auth & PIN Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👨‍💼 ", fontSize = 20.sp)
                            Column {
                                Text(
                                    text = "صلاحيات المدير والمالية",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (isManagerLoggedIn) "حالة الحساب: مسجل الدخول كمدير ✅" else "حالة الحساب: وضع الفني / العامل 👷‍♂️",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isManagerLoggedIn) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isManagerLoggedIn) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.logoutManager()
                                    Toast.makeText(context, "تم تسجيل الخروج من حساب المدير", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("تسجيل الخروج")
                            }
                        } else {
                            Button(
                                onClick = { showManagerLoginDialogSettings = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("دخول المدير 🔐")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showChangePinDialog = true }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تغيير رمز PIN للمدير (الافتراضي: 1234) 🔑")
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "هوية الفني مستخدم هذا الهاتف",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Active Worker Selector
                    ExposedDropdownMenuBox(
                        expanded = isWorkerDropdownExpanded,
                        onExpandedChange = { isWorkerDropdownExpanded = !isWorkerDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedWorkerInput,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("الفني الحالي (من طاقم الـ 10 عمال)") },
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
                                    text = { Text(worker) },
                                    onClick = {
                                        selectedWorkerInput = worker
                                        isWorkerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF16A34A).copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(shape = CircleShape, color = Color(0xFF16A34A), modifier = Modifier.size(8.dp)) {}
                            Text(
                                text = "مرتبط ومزامن مع قاعدة البيانات Firestore ☁️🔥",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showEditWorkersDialog = true }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تعديل قائمة أسماء العمال الـ 10 ✏️")
                        }

                        Button(
                            onClick = {
                                viewModel.updateTeamStoreSettings(
                                    storeNameVal = storeNameInput,
                                    storePhoneVal = storePhoneInput,
                                    activeWorkerVal = selectedWorkerInput,
                                    storeWhatsappVal = storeWhatsappInput
                                )
                                Toast.makeText(context, "تم حفظ إعدادات المحل ورقم الهاتف ومزامنتها مع Firestore بنجاح! 🔥✅", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ وتزامن مع Firestore 🔥", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // General Settings Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "إعدادات القياسات والحسابات",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("طول الأنبوب القياسي", fontWeight = FontWeight.Bold)
                            Text("المعتمد لحساب عدد الأنابيب عند التحديد بالأمتار", style = MaterialTheme.typography.bodySmall)
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "4.0 متر",
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("دعم اللغات والترجمة", fontWeight = FontWeight.Bold)
                            Text("أسماء القطع بالعربية والفرنسية (PPR / PVC)", style = MaterialTheme.typography.bodySmall)
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "عربي / Fr",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Google Drive Backup & Restore Card
            GoogleDriveBackupCard()

            // Cloud Sync & Live Data Sharing Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("☁️ ", fontSize = 22.sp)
                            Column {
                                Text(
                                    text = "المزامنة والربط السحابي المباشر",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "مشاركة المشاريع والمواد بين المدير والعمال فورياً",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF16A34A).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(shape = CircleShape, color = Color(0xFF16A34A), modifier = Modifier.size(8.dp)) {}
                                Text(
                                    text = "متصل بالسحابة",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }

                    Text(
                        text = "عند إنشاء أو تعديل أي مشروع أو طلبية مواد في هاتف العامل أو المدير، يتم تحديث البيانات فورياً عبر السحابة لدى باقي أفراد الفريق باستخدام رقم الهاتف أو رمز الربط الخاص بالورشة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var syncCodeInput by remember { mutableStateOf("") }
                    val currentWorkshopCode = currentUser?.workshopId?.ifBlank { SyncCodeManager.getSyncCode(context) }?.ifBlank { "غير مرتبط بورشة" } ?: "غير مرتبط بورشة"

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("رمز الورشة الحالي (Workshop ID)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currentWorkshopCode, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = syncCodeInput,
                            onValueChange = { syncCodeInput = it },
                            label = { Text("أدخل رمز الورشة (Sync Code)") },
                            placeholder = { Text("مثال: WORKSHOP-8A3F") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                if (syncCodeInput.isBlank()) {
                                    Toast.makeText(context, "يرجى إدخال رمز الورشة أولاً", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.connectWorkerToWorkshopWithSyncCode(syncCodeInput) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        if (success) {
                                            syncCodeInput = ""
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("انضمام 🔗", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // About Application Usage Guide Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "عن نظام الربط بين الورشات والمحل",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "يتيح هذا النظام لطاقم العمال الـ 10 التابعين للمحل إنشاء قائمة اللوازم المطلوبة فوراً من أي ورشة عمل ميدانية، وتحديد الكميات بدقة، ثم إرسال الطلب بنقرة واحدة مباشرة إلى واتساب المحل. يقوم مسؤول المحل بتجهيز القائمة وتسليمها لسائق التوصيل لنقلها فوراً إلى موقع العمل الموضح بالطلب.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Dialog to Edit 10 Workers Names
    if (showEditWorkersDialog) {
        val workersTempList = remember { mutableStateListOf(*workersList.toTypedArray()) }
        AlertDialog(
            onDismissRequest = { showEditWorkersDialog = false },
            title = {
                Text(
                    text = "تعديل أسماء طاقم العمال (10 عمال)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    workersTempList.forEachIndexed { index, name ->
                        OutlinedTextField(
                            value = name,
                            onValueChange = { workersTempList[index] = it },
                            label = { Text("العامل رقم ${index + 1}") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateWorkersList(workersTempList.toList())
                        if (workersTempList.isNotEmpty() && !workersTempList.contains(selectedWorkerInput)) {
                            selectedWorkerInput = workersTempList[0]
                        }
                        showEditWorkersDialog = false
                        Toast.makeText(context, "تم تحديث أسماء العمال بنجاح! 👷‍♂️", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("حفظ التعديلات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditWorkersDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Manager Login Dialog in Settings
    var pinInputSettings by remember { mutableStateOf("") }
    if (showManagerLoginDialogSettings) {
        AlertDialog(
            onDismissRequest = {
                showManagerLoginDialogSettings = false
                pinInputSettings = ""
            },
            title = {
                Text("🔐 تسجيل دخول المدير والمالية", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "أدخل رمز PIN الخاص بالمدير للوصول لكافة الصلاحيات وحفظ اللوازم بالمكتبة:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = pinInputSettings,
                        onValueChange = { pinInputSettings = it },
                        label = { Text("رمز PIN للمدير") },
                        placeholder = { Text("الافتراضي: 1234") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel.loginManager(pinInputSettings)) {
                            showManagerLoginDialogSettings = false
                            pinInputSettings = ""
                            Toast.makeText(context, "تم تسجيل دخول المدير بنجاح 👨‍💼", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "رمز PIN غير صحيح!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("دخول", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showManagerLoginDialogSettings = false
                        pinInputSettings = ""
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Change Manager PIN Dialog
    if (showChangePinDialog) {
        var oldPinInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = {
                Text("🔑 تغيير رمز PIN للمدير", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = oldPinInput,
                        onValueChange = { oldPinInput = it },
                        label = { Text("رمز PIN الحالي") },
                        placeholder = { Text("الافتراضي: 1234") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { newPinInput = it },
                        label = { Text("رمز PIN الجديد") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel.updateManagerPin(oldPinInput, newPinInput)) {
                            showChangePinDialog = false
                            Toast.makeText(context, "تم تغيير رمز PIN للمدير بنجاح! 🔑", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "رمز PIN الحالي غير صحيح أو الرمز الجديد فارغ", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تغيير PIN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

