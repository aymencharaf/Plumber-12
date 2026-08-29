package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.PlumberViewModel
import kotlin.math.ceil
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipeCalculatorScreen(
    viewModel: PlumberViewModel,
    onBack: () -> Unit,
    onNavigateToProjectMaterials: () -> Unit
) {
    val context = LocalContext.current
    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Lengths, 1: Area, 2: Water Points

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("الحاسبة التفاعلية للأنابيب والمستلزمات", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("حساب الأطوال، المساحات، والهدر مع التخزين", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("أطوال", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.SquareFoot, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("مساحة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("نقاط ماء", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("السجل (Room)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> LengthBasedCalculator(
                        viewModel = viewModel,
                        currentProjectId = currentProject?.id,
                        onSaved = onNavigateToProjectMaterials
                    )
                    1 -> AreaBasedCalculator(
                        viewModel = viewModel,
                        currentProjectId = currentProject?.id,
                        onSaved = onNavigateToProjectMaterials
                    )
                    2 -> WaterPointsEstimatorContent(
                        viewModel = viewModel,
                        currentProjectId = currentProject?.id,
                        onSaved = onNavigateToProjectMaterials
                    )
                    3 -> SavedCalculationsHistoryTab(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun LengthBasedCalculator(
    viewModel: PlumberViewModel,
    currentProjectId: Long?,
    onSaved: () -> Unit
) {
    val context = LocalContext.current

    var category by remember { mutableStateOf("PPR") }
    var pipeSize by remember { mutableStateOf("25mm") }
    var singlePipeLength by remember { mutableDoubleStateOf(4.0) } // 4 meters
    var totalLengthInput by remember { mutableStateOf("15.0") }
    var wastePercentage by remember { mutableDoubleStateOf(10.0) } // 10%
    var clampSpacing by remember { mutableDoubleStateOf(0.8) } // clamps every 0.8m

    val rawLength = totalLengthInput.toDoubleOrNull() ?: 0.0
    val totalMetersWithWaste = rawLength * (1 + wastePercentage / 100.0)
    val totalPipesCount = ceil(totalMetersWithWaste / max(0.5, singlePipeLength)).toInt()
    val wasteMeters = totalMetersWithWaste - rawLength
    val couplingsCount = max(0, totalPipesCount - 1)
    val clampsCount = if (rawLength > 0) ceil(rawLength / max(0.2, clampSpacing)).toInt() + 1 else 0

    val pipeCategories = listOf("PPR", "PVC", "PEX", "Multilayer")
    val pipeSizesMap = mapOf(
        "PPR" to listOf("20mm", "25mm", "32mm", "40mm", "50mm", "63mm"),
        "PVC" to listOf("32mm", "40mm", "50mm", "75mm", "110mm", "160mm"),
        "PEX" to listOf("16mm", "20mm", "25mm", "32mm"),
        "Multilayer" to listOf("16mm", "20mm", "26mm", "32mm")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Category Selector
        Text("فئة ونوع الأنبوب", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            pipeCategories.forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = {
                        category = cat
                        pipeSize = pipeSizesMap[cat]?.firstOrNull() ?: "25mm"
                    },
                    label = { Text(cat, fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Pipe Size Selector
        Text("قطر المقاس القياسي", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val sizes = pipeSizesMap[category] ?: listOf("25mm")
            sizes.forEach { sz ->
                FilterChip(
                    selected = pipeSize == sz,
                    onClick = { pipeSize = sz },
                    label = { Text(sz) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Inputs Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("المقياس والأطوال المطلوبة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = totalLengthInput,
                    onValueChange = { totalLengthInput = it },
                    label = { Text("إجمالي الطول المطلوب (متر)") },
                    trailingIcon = { Text("متر  ", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("طول الأنبوب القياسي", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(4.0, 3.0, 6.0).forEach { len ->
                                FilterChip(
                                    selected = singlePipeLength == len,
                                    onClick = { singlePipeLength = len },
                                    label = { Text("${len.toInt()}م") }
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("نسبة الهدر والقطع", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(5.0, 10.0, 15.0).forEach { pct ->
                                FilterChip(
                                    selected = wastePercentage == pct,
                                    onClick = { wastePercentage = pct },
                                    label = { Text("${pct.toInt()}%") }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Calculated Results Card
        ResultsSummaryCard(
            totalPipesCount = totalPipesCount,
            totalMetersWithWaste = totalMetersWithWaste,
            wasteMeters = wasteMeters,
            couplingsCount = couplingsCount,
            clampsCount = clampsCount,
            pipeCategory = category,
            pipeSize = pipeSize,
            singlePipeLength = singlePipeLength
        )

        // Save Action Button
        Button(
            onClick = {
                if (totalPipesCount <= 0) {
                    Toast.makeText(context, "يرجى أدخال طول صحيح بحساب الأنابيب", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val notes = "حساب بالأطوال: $rawLength م + هدر ${wastePercentage.toInt()}% (طول العود $singlePipeLength م)"
                if (currentProjectId != null) {
                    viewModel.savePipeCalculationResult(
                        projectId = currentProjectId,
                        pipeCategory = category,
                        pipeSize = pipeSize,
                        totalPipesCount = totalPipesCount,
                        totalMetersWithWaste = totalMetersWithWaste,
                        couplingsCount = couplingsCount,
                        clampsCount = clampsCount,
                        calculationSummaryNotes = notes
                    )
                    Toast.makeText(context, "تم حفظ نتيجة الأنابيب في قاعدة البيانات! 💾", Toast.LENGTH_SHORT).show()
                    onSaved()
                } else {
                    viewModel.createProject(
                        title = "مشروع تمديد أنابيب $category",
                        notes = notes,
                        onProjectCreated = { projId ->
                            viewModel.savePipeCalculationResult(
                                projectId = projId,
                                pipeCategory = category,
                                pipeSize = pipeSize,
                                totalPipesCount = totalPipesCount,
                                totalMetersWithWaste = totalMetersWithWaste,
                                couplingsCount = couplingsCount,
                                clampsCount = clampsCount,
                                calculationSummaryNotes = notes
                            )
                            Toast.makeText(context, "تم إنشاء المشروع وحفظ النتائج بنجاح!", Toast.LENGTH_SHORT).show()
                            onSaved()
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("حفظ النتائج في قاعدة البيانات للمشروع", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun AreaBasedCalculator(
    viewModel: PlumberViewModel,
    currentProjectId: Long?,
    onSaved: () -> Unit
) {
    val context = LocalContext.current

    var roomLengthInput by remember { mutableStateOf("4.0") }
    var roomWidthInput by remember { mutableStateOf("3.5") }
    var pipePitchCm by remember { mutableDoubleStateOf(20.0) } // 20 cm spacing between lines
    var extraFeederMetersInput by remember { mutableStateOf("3.0") }
    var category by remember { mutableStateOf("PEX") }
    var pipeSize by remember { mutableStateOf("20mm") }
    var wastePercentage by remember { mutableDoubleStateOf(10.0) }

    val length = roomLengthInput.toDoubleOrNull() ?: 0.0
    val width = roomWidthInput.toDoubleOrNull() ?: 0.0
    val area = length * width
    val extraFeeder = extraFeederMetersInput.toDoubleOrNull() ?: 0.0

    // Net pipe meters required = Area / Pitch (in meters) + extra feeder
    val pitchInMeters = pipePitchCm / 100.0
    val netPipesLengthMeters = if (pitchInMeters > 0 && area > 0) (area / pitchInMeters) + extraFeeder else 0.0
    val totalMetersWithWaste = netPipesLengthMeters * (1 + wastePercentage / 100.0)
    val totalPipesCount = ceil(totalMetersWithWaste / 4.0).toInt() // Standard 4m pieces or rolls
    val wasteMeters = totalMetersWithWaste - netPipesLengthMeters
    val couplingsCount = max(0, totalPipesCount - 1)
    val clampsCount = if (netPipesLengthMeters > 0) ceil(netPipesLengthMeters / 0.8).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Room Dimension Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("مساحة الغرفة والتمديد الأرضي", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = roomLengthInput,
                        onValueChange = { roomLengthInput = it },
                        label = { Text("الطول (متر)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = roomWidthInput,
                        onValueChange = { roomWidthInput = it },
                        label = { Text("العرض (متر)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إجمالي المساحة المحسوبة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${String.format("%.2f", area)} م²", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                    }
                }
            }
        }

        // Pipe Pitch and Layout Settings
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("إعدادات شبكة الخطوط والأنابيب", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                Text("المسافة بين خطوط الأنبوب (الخطوة / Pitch)", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(15.0, 20.0, 25.0, 30.0).forEach { p ->
                        FilterChip(
                            selected = pipePitchCm == p,
                            onClick = { pipePitchCm = p },
                            label = { Text("${p.toInt()} سم") }
                        )
                    }
                }

                OutlinedTextField(
                    value = extraFeederMetersInput,
                    onValueChange = { extraFeederMetersInput = it },
                    label = { Text("طول التغذية الإضافي للموزع (متر)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }
        }

        // Results Card
        ResultsSummaryCard(
            totalPipesCount = totalPipesCount,
            totalMetersWithWaste = totalMetersWithWaste,
            wasteMeters = wasteMeters,
            couplingsCount = couplingsCount,
            clampsCount = clampsCount,
            pipeCategory = category,
            pipeSize = pipeSize,
            singlePipeLength = 4.0
        )

        // Save Button
        Button(
            onClick = {
                if (area <= 0) {
                    Toast.makeText(context, "يرجى إدخال أبعاد مساحة صحيحة", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val notes = "حساب بالمساحة: ${String.format("%.2f", area)}م² (خطوة $pipePitchCm سم)"
                if (currentProjectId != null) {
                    viewModel.savePipeCalculationResult(
                        projectId = currentProjectId,
                        pipeCategory = category,
                        pipeSize = pipeSize,
                        totalPipesCount = totalPipesCount,
                        totalMetersWithWaste = totalMetersWithWaste,
                        couplingsCount = couplingsCount,
                        clampsCount = clampsCount,
                        calculationSummaryNotes = notes
                    )
                    Toast.makeText(context, "تم حفظ نتائج الشبكة والأنابيب في قاعدة البيانات! 💾", Toast.LENGTH_SHORT).show()
                    onSaved()
                } else {
                    viewModel.createProject(
                        title = "مشروع شبكة أرضية $category",
                        notes = notes,
                        onProjectCreated = { projId ->
                            viewModel.savePipeCalculationResult(
                                projectId = projId,
                                pipeCategory = category,
                                pipeSize = pipeSize,
                                totalPipesCount = totalPipesCount,
                                totalMetersWithWaste = totalMetersWithWaste,
                                couplingsCount = couplingsCount,
                                clampsCount = clampsCount,
                                calculationSummaryNotes = notes
                            )
                            Toast.makeText(context, "تم إنشاء المشروع وحفظ النتائج بنجاح!", Toast.LENGTH_SHORT).show()
                            onSaved()
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("حفظ نتائج المساحة في قاعدة البيانات", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun WaterPointsEstimatorContent(
    viewModel: PlumberViewModel,
    currentProjectId: Long?,
    onSaved: () -> Unit
) {
    val context = LocalContext.current

    var bathroomsCount by remember { mutableIntStateOf(1) }
    var kitchensCount by remember { mutableIntStateOf(1) }
    var sinksCount by remember { mutableIntStateOf(2) }
    var showersCount by remember { mutableIntStateOf(1) }
    var toiletsCount by remember { mutableIntStateOf(1) }

    val totalPoints = (bathroomsCount * 4) + (kitchensCount * 3) + sinksCount + showersCount + toiletsCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("💡 التقدير الذكي التلقائي لنقاط الماء", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("احسب كميات الأنابيب والأكواع والمستلزمات فوراً بناءً على عدد الحمامات والمطابخ.", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Counter Rows
        CounterRow("عدد الحمامات (4 نقاط لكل حمام)", bathroomsCount) { bathroomsCount = max(0, bathroomsCount + it) }
        CounterRow("عدد المطابخ (3 نقاط لكل مطبخ)", kitchensCount) { kitchensCount = max(0, kitchensCount + it) }
        CounterRow("مغاسل إضافية", sinksCount) { sinksCount = max(0, sinksCount + it) }
        CounterRow("دش / شور إضافي", showersCount) { showersCount = max(0, showersCount + it) }
        CounterRow("مراحيض / تواليت إضافية", toiletsCount) { toiletsCount = max(0, toiletsCount + it) }

        // Total Indicator
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("إجمالي نقاط الماء المحسوبة:", fontWeight = FontWeight.Bold)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Text("$totalPoints نقطة", modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Button(
            onClick = {
                if (totalPoints <= 0) {
                    Toast.makeText(context, "يرجى اختيار نقاط ماء صحيحة", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (currentProjectId != null) {
                    viewModel.estimateMaterialsByWaterPoints(
                        projectId = currentProjectId,
                        bathroomsCount = bathroomsCount,
                        kitchensCount = kitchensCount,
                        sinksCount = sinksCount,
                        showersCount = showersCount,
                        toiletsCount = toiletsCount
                    )
                    Toast.makeText(context, "تم توليد وتخزين المواد في قاعدة البيانات!", Toast.LENGTH_SHORT).show()
                    onSaved()
                } else {
                    viewModel.createProject(
                        title = "تقدير مشروع جديد ($totalPoints نقطة)",
                        notes = "تم إنشاء التقدير التلقائي لنقاط الماء",
                        onProjectCreated = { projId ->
                            viewModel.estimateMaterialsByWaterPoints(
                                projectId = projId,
                                bathroomsCount = bathroomsCount,
                                kitchensCount = kitchensCount,
                                sinksCount = sinksCount,
                                showersCount = showersCount,
                                toiletsCount = toiletsCount
                            )
                            Toast.makeText(context, "تم إنشاء المشروع وحفظ المواد التلقائية!", Toast.LENGTH_SHORT).show()
                            onSaved()
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("حفظ وتوليد مواد النقاط في قاعدة البيانات", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ResultsSummaryCard(
    totalPipesCount: Int,
    totalMetersWithWaste: Double,
    wasteMeters: Double,
    couplingsCount: Int,
    clampsCount: Int,
    pipeCategory: String,
    pipeSize: String,
    singlePipeLength: Double
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("نتائج الحساب التفاعلية", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("عدد الأنابيب القياسية:", style = MaterialTheme.typography.labelMedium)
                    Text("$totalPipesCount عود / أنبوب (طول ${singlePipeLength.toInt()}م)", fontWeight = FontWeight.Black, fontSize = 15.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("إجمالي الأمتار مع الهدر:", style = MaterialTheme.typography.labelMedium)
                    Text("${String.format("%.2f", totalMetersWithWaste)} متر", fontWeight = FontWeight.Black, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("الوصلات والمانشون المقدرة:", style = MaterialTheme.typography.labelMedium)
                    Text("$couplingsCount قطعة ($pipeCategory $pipeSize)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("قفيز ومشاربك التثبيت الجداري:", style = MaterialTheme.typography.labelMedium)
                    Text("$clampsCount قطعة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ملاحظة: شامل نسبة هدر وتضييع قدرها ${String.format("%.2f", wasteMeters)} متر",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun CounterRow(
    title: String,
    count: Int,
    onDelta: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledIconButton(
                    onClick = { onDelta(-1) },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                }

                Text("$count", fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.widthIn(min = 24.dp), textAlign = TextAlign.Center)

                FilledIconButton(
                    onClick = { onDelta(1) },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SavedCalculationsHistoryTab(viewModel: PlumberViewModel) {
    val caches by viewModel.calculatedMaterialCaches.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("سجل الحسابات المحفوظة محلياً 💾", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("يمكنك الوصول لنتائج حساباتك السابقة بدون شبكة إنترنت عبر Room Database", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (caches.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                    Text("لا توجد حسابات مخزنة محلياً بعد", fontWeight = FontWeight.Bold)
                    Text("قم بإجراء حاسبة أطوال، مساحة، أو نقاط ماء وسيتم تخزينها هنا تلقائياً لعملها دون إنترنت.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
        } else {
            caches.forEach { item ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = true,
                                    onClick = { },
                                    label = { Text(item.pipeCategory, fontWeight = FontWeight.Bold) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(item.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                            IconButton(onClick = { viewModel.deleteCalculationCache(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Text("مدخلات: ${item.inputValuesSummary}", style = MaterialTheme.typography.bodyMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("القطع: ${item.estimatedPipesCount.toInt()} قطعة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("المحلقات: ${item.estimatedFittingsCount}", fontWeight = FontWeight.Bold)
                            Text(item.estimatedGlueOrSolder, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
