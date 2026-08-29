package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.PlumberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterPointsEstimatorScreen(
    viewModel: PlumberViewModel,
    onBack: () -> Unit,
    onNavigateToProjectMaterials: () -> Unit
) {
    val context = LocalContext.current
    val currentProject by viewModel.currentProject.collectAsState()

    var bathroomsCount by remember { mutableStateOf(1) }
    var kitchensCount by remember { mutableStateOf(1) }
    var sinksCount by remember { mutableStateOf(2) }
    var showersCount by remember { mutableStateOf(1) }
    var toiletsCount by remember { mutableStateOf(1) }

    val totalPoints = (bathroomsCount * 4) + (kitchensCount * 3) + sinksCount + showersCount + toiletsCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التقدير التلقائي للمواد (نقاط الماء)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = {
                            viewModel.saveCalculationCache(
                                title = "تقدير نقاط الماء ($totalPoints نقطة)",
                                calculationType = "WATER_POINTS",
                                pipeCategory = "PPR",
                                size = "25mm / 20mm",
                                inputValuesSummary = "$bathroomsCount حمام، $kitchensCount مطبخ، $sinksCount مغسلة، $showersCount دش، $toiletsCount مرحاض",
                                estimatedPipesCount = (totalPoints * 1.5),
                                estimatedFittingsCount = totalPoints * 4,
                                estimatedGlueOrSolder = "لحام حراري PPR",
                                generatedMaterialsJson = "[]"
                            )
                            if (currentProject == null) {
                                // Create default project first
                                viewModel.createProject(
                                    title = "مشروع تقديري جديد",
                                    notes = "تم إنشاء التقدير التلقائي بناءً على $totalPoints نقطة ماء",
                                    onProjectCreated = { projId ->
                                        viewModel.estimateMaterialsByWaterPoints(
                                            projectId = projId,
                                            bathroomsCount = bathroomsCount,
                                            kitchensCount = kitchensCount,
                                            sinksCount = sinksCount,
                                            showersCount = showersCount,
                                            toiletsCount = toiletsCount
                                        )
                                        Toast.makeText(context, "تم حساب التقدير وتخزينه في قواعد البيانات محلياً!", Toast.LENGTH_SHORT).show()
                                        onNavigateToProjectMaterials()
                                    }
                                )
                            } else {
                                viewModel.estimateMaterialsByWaterPoints(
                                    projectId = currentProject!!.id,
                                    bathroomsCount = bathroomsCount,
                                    kitchensCount = kitchensCount,
                                    sinksCount = sinksCount,
                                    showersCount = showersCount,
                                    toiletsCount = toiletsCount
                                )
                                Toast.makeText(context, "تم تخزين التقدير في Room وقائمة المواد محلياً!", Toast.LENGTH_SHORT).show()
                                onNavigateToProjectMaterials()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "توليد وإضافة المواد للمشروع ($totalPoints نقطة)",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💡 كيف يعمل التقدير التلقائي؟",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "حدد عدد المرافق ونقاط الماء المطلوبة، وسيقوم التطبيق بحساب تقديري ذكي لأكواع PPR، أنابيب الصرف PVC، التيفلون، والأكواع الجدارية المناسبة للعمل.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Total Points Indicator
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إجمالي نقاط الماء المقدرة:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "$totalPoints نقطة",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Counter Item Row Helper
            @Composable
            fun PointCounterRow(
                title: String,
                subtitle: String,
                count: Int,
                onCountChanged: (Int) -> Unit
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                onClick = { if (count > 0) onCountChanged(count - 1) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }

                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(36.dp)
                            )

                            Surface(
                                onClick = { onCountChanged(count + 1) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }
            }

            PointCounterRow(
                title = "🚿 عدد الحمامات الكاملة",
                subtitle = "تغطي الدوش والكرسي والمغسلة",
                count = bathroomsCount,
                onCountChanged = { bathroomsCount = it }
            )

            PointCounterRow(
                title = "🍳 عدد المطابخ",
                subtitle = "تغطي المجلى والغسالة",
                count = kitchensCount,
                onCountChanged = { kitchensCount = it }
            )

            PointCounterRow(
                title = "🚰 عدد المغاسل الإضافية",
                subtitle = "مغاسل الصالة والضيوف",
                count = sinksCount,
                onCountChanged = { sinksCount = it }
            )

            PointCounterRow(
                title = "🚿 عدد الدوش المستقل",
                subtitle = "كبائن دوش منفصلة",
                count = showersCount,
                onCountChanged = { showersCount = it }
            )

            PointCounterRow(
                title = "🚽 عدد مراحيض WC منفصلة",
                subtitle = "دورات مياه ضيوف إضافية",
                count = toiletsCount,
                onCountChanged = { toiletsCount = it }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
