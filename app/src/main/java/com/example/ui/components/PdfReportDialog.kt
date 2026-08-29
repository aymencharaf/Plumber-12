package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Project
import com.example.data.model.ProjectItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReportDialog(
    project: Project,
    items: List<ProjectItem>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val formattedText = ShareHelper.generateTextSummary(project, items)
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val dateStr = dateFormat.format(Date(project.createdAt))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.Black)
                    }
                    Text(
                        text = "تقرير قائمة المواد (PDF/Print)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF006B42)
                        )
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Printable Paper Document Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Document Header
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "PLUMBER",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF006B42)
                                    )
                                )
                                Text(
                                    text = "قائمة مواد مشروع السباكة والتأسيس",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = Color(0xFF006B42), thickness = 2.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        // Project Info Grid
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF0F7F4), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "المشروع: ${project.title}",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(text = "التاريخ: $dateStr", color = Color.Gray)
                                }
                                if (project.clientName.isNotBlank()) {
                                    Text(text = "العميل: ${project.clientName}", color = Color.DarkGray)
                                }
                                if (project.location.isNotBlank()) {
                                    Text(text = "الموقع: ${project.location}", color = Color.DarkGray)
                                }
                                Text(text = "نوع العمل: ${project.workTypeNameAr}", color = Color(0xFF006B42), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Items Table Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF006B42), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#", modifier = Modifier.width(28.dp), color = Color.White, fontWeight = FontWeight.Bold)
                                Text("المادة", modifier = Modifier.weight(2f), color = Color.White, fontWeight = FontWeight.Bold)
                                Text("المقاس", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold)
                                Text("الكمية", modifier = Modifier.weight(1.2f), color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                            }
                        }

                        // Items Table Rows
                        itemsIndexed(items) { index, item ->
                            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                            val rowBg = if (index % 2 == 0) Color.White else Color(0xFFF9F9F9)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBg)
                                    .border(0.5.dp, Color(0xFFE0E0E0))
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${index + 1}", modifier = Modifier.width(28.dp), color = Color.DarkGray)
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(item.materialNameAr, fontWeight = FontWeight.Bold, color = Color.Black)
                                    if (item.notes.isNotBlank()) {
                                        Text(item.notes, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                                Text(item.size, modifier = Modifier.weight(1f), color = Color.DarkGray)
                                Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                                    Text("$qtyStr ${item.unit}", fontWeight = FontWeight.Bold, color = Color(0xFF006B42))
                                    item.getPipesCount()?.let {
                                        Text("(≈ $it أنبوب)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        // Summary Footer
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            val totalItemsCount = items.size
                            val totalQty = items.sumOf { it.quantity }
                            val totalQtyStr = if (totalQty % 1.0 == 0.0) totalQty.toInt().toString() else totalQty.toString()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "إجمالي المواد: $totalItemsCount نوع",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF006B42)
                                )
                                Text(
                                    text = "إجمالي الكميات: $totalQtyStr",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF006B42)
                                )
                            }

                            if (project.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("ملاحظات:", fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(project.notes, color = Color.DarkGray, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Buttons: Copy / Share / Print
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            ShareHelper.copyToClipboard(context, formattedText)
                            Toast.makeText(context, "تم نسخ التقرير إلى الحافظة", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ")
                    }

                    Button(
                        onClick = {
                            ShareHelper.shareToGeneral(context, formattedText)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006B42))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة")
                    }
                }
            }
        }
    }
}
