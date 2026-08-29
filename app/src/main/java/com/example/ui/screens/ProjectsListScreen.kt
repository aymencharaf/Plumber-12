package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Project
import com.example.ui.viewmodel.PlumberViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsListScreen(
    viewModel: PlumberViewModel,
    onNavigateToNewProject: () -> Unit,
    onNavigateToProjectDetails: (projectId: Long) -> Unit
) {
    val context = LocalContext.current
    val projects by viewModel.allProjects.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("الكل") }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    var projectToDelete by remember { mutableStateOf<Project?>(null) }
    var projectToDuplicate by remember { mutableStateOf<Project?>(null) }
    var duplicateTitleInput by remember { mutableStateOf("") }

    val statusFilters = listOf("الكل", "جاري التنفيذ", "مكتمل", "ملفي")

    val filteredProjects = projects.filter { proj ->
        val matchesSearch = searchQuery.isBlank() ||
                proj.title.contains(searchQuery, ignoreCase = true) ||
                proj.clientName.contains(searchQuery, ignoreCase = true) ||
                proj.location.contains(searchQuery, ignoreCase = true)

        val matchesStatus = when (selectedStatusFilter) {
            "الكل" -> true
            "جاري التنفيذ" -> proj.orderStatus != "DELIVERED" && proj.orderStatus != "ARCHIVED"
            "مكتمل" -> proj.orderStatus == "DELIVERED"
            "ملفي" -> proj.orderStatus == "ARCHIVED"
            else -> true
        }

        matchesSearch && matchesStatus
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المشاريع", fontWeight = FontWeight.Bold) },
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
                onClick = onNavigateToNewProject,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "مشروع جديد")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar with Filter Icon
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن مشروع...") },
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

            // Status Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(statusFilters) { status ->
                    val isSelected = selectedStatusFilter == status
                    Surface(
                        onClick = { selectedStatusFilter = status },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    ) {
                        Text(
                            text = status,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (filteredProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "لا توجد مشاريع مضافة" else "لا توجد نتائج مطابقة",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp)
                ) {
                    items(filteredProjects, key = { it.id }) { project ->
                        ProjectItemCard(
                            project = project,
                            dateFormat = dateFormat,
                            onClick = {
                                viewModel.selectProject(project.id)
                                onNavigateToProjectDetails(project.id)
                            },
                            onDuplicate = {
                                projectToDuplicate = project
                                duplicateTitleInput = "${project.title} (نسخة)"
                            },
                            onDelete = { projectToDelete = project }
                        )
                    }
                }
            }
        }
    }

    // Confirm Delete Dialog
    projectToDelete?.let { proj ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("حذف المشروع") },
            text = { Text("هل أنت تأكد من حذف مشروع \"${proj.title}\" وجميع المواد التابعة له؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProject(proj.id)
                        projectToDelete = null
                        Toast.makeText(context, "تم حذف المشروع", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Duplicate Project Dialog
    projectToDuplicate?.let { proj ->
        AlertDialog(
            onDismissRequest = { projectToDuplicate = null },
            title = { Text("تكرار المشروع") },
            text = {
                Column {
                    Text("أدخل اسم للمشروع الجديد:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = duplicateTitleInput,
                        onValueChange = { duplicateTitleInput = it },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (duplicateTitleInput.isNotBlank()) {
                            viewModel.duplicateProject(proj.id, duplicateTitleInput)
                            projectToDuplicate = null
                            Toast.makeText(context, "تم تكرار المشروع بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("نسخ وتكرار")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDuplicate = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ProjectItemCard(
    project: Project,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Building Photo Thumbnail
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE0F2FE)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_plumbing_banner_1787518666435),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Center Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )

                if (project.clientName.isNotBlank()) {
                    Text(
                        text = "العميل: ${project.clientName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateFormat.format(Date(project.updatedAt)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "28 مادة",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right Status Badge & Menu
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (project.orderStatus) {
                        "DELIVERED" -> Color(0xFFDCFCE7)
                        "ARCHIVED" -> Color(0xFFF1F5F9)
                        else -> Color(0xFFEBF2FF)
                    }
                ) {
                    Text(
                        text = when (project.orderStatus) {
                            "DELIVERED" -> "مكتمل"
                            "ARCHIVED" -> "ملفي"
                            else -> "جاري التنفيذ"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (project.orderStatus) {
                            "DELIVERED" -> Color(0xFF16A34A)
                            "ARCHIVED" -> Color(0xFF64748B)
                            else -> Color(0xFF1665FF)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "تكرار", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
