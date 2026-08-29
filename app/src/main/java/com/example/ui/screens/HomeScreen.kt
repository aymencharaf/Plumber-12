package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.AdminPanelDialog
import com.example.ui.components.WorkAlertsDialog
import com.example.ui.theme.PlumberBluePrimary
import com.example.ui.viewmodel.PlumberViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PlumberViewModel,
    onNavigateToNewProject: (workTypeKey: String?) -> Unit,
    onNavigateToProjectDetails: (projectId: Long) -> Unit,
    onNavigateToEstimator: () -> Unit,
    onNavigateToTeam: () -> Unit = {},
    onNavigateToInvoices: () -> Unit = {},
    onNavigateToProjects: () -> Unit = {},
    onNavigateToCatalog: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAppointment: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val projects by viewModel.visibleProjects.collectAsStateWithLifecycle()
    val libraryMaterials by viewModel.libraryMaterials.collectAsStateWithLifecycle()
    val appointments by viewModel.allAppointments.collectAsStateWithLifecycle()
    val workAlerts by viewModel.allWorkAlerts.collectAsStateWithLifecycle()
    val managerName by viewModel.managerName.collectAsStateWithLifecycle()
    val storeName by viewModel.storeName.collectAsStateWithLifecycle()
    val isManagerLoggedIn by viewModel.isManagerLoggedIn.collectAsStateWithLifecycle()
    var showAdminPanelDialog by remember { mutableStateOf(false) }
    var showWorkAlertsDialog by remember { mutableStateOf(false) }

    val unreadAlertsCount = workAlerts.count { it.status == "جديد" }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    val totalMaterialCount = libraryMaterials.size.let { if (it > 0) it else 24 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar & Greeting (Right)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "المستخدم",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentUser?.name ?: managerName.ifBlank { "أيمين" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = if (currentRole == "ADMIN") Color(0xFFDCFCE7) else Color(0xFFE0F2FE),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (currentRole == "ADMIN") "ADMIN" else "WORKER",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (currentRole == "ADMIN") Color(0xFF15803D) else Color(0xFF0369A1),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (currentRole == "ADMIN") "لوحة التحكم الرئيسية الإدارية" else "لوحة مهام العامل الفني",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Top Admin Panel Button & Logout Button & Notifications (Left)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Logout Button
                            IconButton(onClick = {
                                viewModel.logoutTeamUser()
                                Toast.makeText(context, "تم تسجيل الخروج", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "تسجيل الخروج",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            if (isManagerLoggedIn) {
                                Surface(
                                    onClick = { showAdminPanelDialog = true },
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFF2563EB),
                                    shadowElevation = 3.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AdminPanelSettings,
                                            contentDescription = "لوحة التحكم",
                                            tint = Color.White,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            text = "لوحة التحكم",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Surface(
                                    onClick = {
                                        viewModel.setManagerLoggedIn(false)
                                        showAdminPanelDialog = false
                                        Toast.makeText(context, "تم تسجيل الخروج بنجاح وإخفاء لوحة التحكم", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFDC2626),
                                    shadowElevation = 3.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ExitToApp,
                                            contentDescription = "الخروج",
                                            tint = Color.White,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            text = "الخروج",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Box {
                                IconButton(
                                    onClick = { showWorkAlertsDialog = true },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsNone,
                                        contentDescription = "منبه إشعارات العمل",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (unreadAlertsCount > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFDC2626),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .align(Alignment.TopEnd)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (unreadAlertsCount > 9) "9+" else unreadAlertsCount.toString(),
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Admin Hero Banner (Only shown if manager is logged in)
            if (isManagerLoggedIn) {
                item {
                    Card(
                        onClick = { showAdminPanelDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.AdminPanelSettings,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = "لوحة تحكم المدير الإحترافية",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "إضافة مواد التطبيق | تعديل قائمة العمال",
                                        fontSize = 12.sp,
                                        color = Color(0xFFBFDBFE)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF3B82F6)
                            ) {
                                Text(
                                    text = "فتح اللوحة",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
            // Hero Blue Gradient Banner ("المشروعات الحالية")
            item {
                Card(
                    onClick = onNavigateToProjects,
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF0052D4),
                                        Color(0xFF1665FF),
                                        Color(0xFF2384FF)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Assignment,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "المشروعات الحالية",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )

                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "${projects.size}",
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "مشروع",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = "إجمالي المشاريع",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Prominent "طلب موعد" Banner Card
            item {
                val pendingAppointmentsCount = appointments.count { it.status == "قيد الانتظار" }
                Card(
                    onClick = onNavigateToAppointment,
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFCD34D)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Event,
                                            contentDescription = "طلب موعد",
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "طلب موعد وصيانة",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = Color(0xFF78350F)
                                        )
                                        if (pendingAppointmentsCount > 0) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFDC2626)
                                            ) {
                                                Text(
                                                    text = "$pendingAppointmentsCount معلق",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "فحص • صيانة • تركيب • أعمال أخرى",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Quick Service Options Tags
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("🔍 فحص", "🛠️ صيانة", "🚰 تركيب", "📋 أعمال أخرى").forEach { optionTag ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = optionTag,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                         color = Color(0xFF92400E),
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Prominent "منبه الرسائل والطلبيات (العامل ↔ المدير)" Banner Card
            item {
                Card(
                    onClick = { showWorkAlertsDialog = true },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF93C5FD)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsNone,
                                            contentDescription = "منبه الرسائل",
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "منبه الرسائل (العامل ↔ المدير)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = Color(0xFF1E3A8A)
                                        )
                                        if (unreadAlertsCount > 0) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFDC2626)
                                            ) {
                                                Text(
                                                    text = "$unreadAlertsCount جديد",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "طلبية • تحديد موعد • إرسال لوازم • استلام",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // 4 Quick Options Tags
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("📦 طلبية", "📅 مواعيد", "🚚 إرسال", "✅ استلام").forEach { optionTag ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = optionTag,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E40AF),
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6 Grid Action Cards (2 Columns x 3 Rows) with custom distinct colors
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Row 1: المشاريع | المواد
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HomeGridCard(
                            title = "المشاريع",
                            subtitle = "${projects.size} مشروع",
                            icon = Icons.Default.Folder,
                            cardBgColor = Color(0xFFEFF6FF),
                            borderColor = Color(0xFFBFDBFE),
                            iconBgColor = Color(0xFF2563EB),
                            iconTintColor = Color.White,
                            titleColor = Color(0xFF1E3A8A),
                            onClick = onNavigateToProjects,
                            modifier = Modifier.weight(1f)
                        )
                        HomeGridCard(
                            title = "المواد",
                            subtitle = "$totalMaterialCount مادة",
                            icon = Icons.Default.Link,
                            cardBgColor = Color(0xFFECFDF5),
                            borderColor = Color(0xFFA7F3D0),
                            iconBgColor = Color(0xFF059669),
                            iconTintColor = Color.White,
                            titleColor = Color(0xFF064E3B),
                            onClick = onNavigateToCatalog,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: التقارير | العملاء
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HomeGridCard(
                            title = "التقارير",
                            subtitle = "تقارير مفصلة",
                            icon = Icons.Default.BarChart,
                            cardBgColor = Color(0xFFF5F3FF),
                            borderColor = Color(0xFFDDD6FE),
                            iconBgColor = Color(0xFF7C3AED),
                            iconTintColor = Color.White,
                            titleColor = Color(0xFF4C1D95),
                            onClick = onNavigateToInvoices,
                            modifier = Modifier.weight(1f)
                        )
                        HomeGridCard(
                            title = "العملاء",
                            subtitle = "85 عميل",
                            icon = Icons.Default.Group,
                            cardBgColor = Color(0xFFFFF7ED),
                            borderColor = Color(0xFFFED7AA),
                            iconBgColor = Color(0xFFEA580C),
                            iconTintColor = Color.White,
                            titleColor = Color(0xFF7C2D12),
                            onClick = onNavigateToTeam,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 3: الموردين | الإعدادات
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HomeGridCard(
                            title = "الموردين",
                            subtitle = "28 مورد",
                            icon = Icons.Default.LocalShipping,
                            cardBgColor = Color(0xFFFFF1F2),
                            borderColor = Color(0xFFFECDD3),
                            iconBgColor = Color(0xFFE11D48),
                            iconTintColor = Color.White,
                            titleColor = Color(0xFF881337),
                            onClick = onNavigateToTeam,
                            modifier = Modifier.weight(1f)
                        )
                        HomeGridCard(
                            title = "الإعدادات",
                            subtitle = "تخصيص التطبيق",
                            icon = Icons.Default.Settings,
                            cardBgColor = Color(0xFFF1F5F9),
                            borderColor = Color(0xFFCBD5E1),
                            iconBgColor = Color(0xFF475569),
                            iconTintColor = Color.White,
                            titleColor = Color(0xFF0F172A),
                            onClick = onNavigateToSettings,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick Shortcut: Create New Project Button
            item {
                Button(
                    onClick = { onNavigateToNewProject(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إنشاء مشروع جديد",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Recent Projects List
            if (projects.isNotEmpty()) {
                item {
                    Text(
                        text = "المشاريع الأخيرة",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(projects.take(3)) { project ->
                    Card(
                        onClick = {
                            viewModel.selectProject(project.id)
                            onNavigateToProjectDetails(project.id)
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFDBEAFE),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Build,
                                            contentDescription = null,
                                            tint = Color(0xFF1D4ED8),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = project.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B)
                                        )
                                    )
                                    Text(
                                        text = "العميل: ${project.clientName.ifBlank { "عميل مباشر" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFDCFCE7),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC))
                            ) {
                                Text(
                                    text = "جاري التنفيذ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showAdminPanelDialog) {
        AdminPanelDialog(
            viewModel = viewModel,
            onDismiss = { showAdminPanelDialog = false }
        )
    }

    if (showWorkAlertsDialog) {
        WorkAlertsDialog(
            viewModel = viewModel,
            onDismiss = { showWorkAlertsDialog = false }
        )
    }
}

@Composable
fun HomeGridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardBgColor: Color = Color.White,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    iconBgColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTintColor: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = Color.Unspecified
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = iconBgColor,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTintColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = titleColor.copy(alpha = 0.75f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
