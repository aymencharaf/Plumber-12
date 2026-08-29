package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Project
import com.example.data.model.ProjectItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfInvoiceExporter {

    fun generateInvoicePdf(
        context: Context,
        project: Project,
        items: List<ProjectItem>,
        storeName: String,
        managerName: String,
        workerName: String,
        siteLocation: String,
        installationCost: Double,
        fittingsCost: Double,
        transportFee: Double,
        paidAmount: Double,
        invoiceNotes: String
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            // Standard A4 size: 595 x 842 points
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint()
            val textPaint = Paint().apply {
                isAntiAlias = true
            }

            // Green Top Banner Header
            paint.color = Color.parseColor("#006B42")
            canvas.drawRect(0f, 0f, 595f, 90f, paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 20f
            textPaint.isFakeBoldText = true
            textPaint.textAlign = Paint.Align.CENTER
            val titleStr = if (storeName.isNotBlank()) storeName else "محل السباكة والتجهيزات"
            canvas.drawText(titleStr, 297.5f, 40f, textPaint)

            textPaint.textSize = 12f
            textPaint.isFakeBoldText = false
            canvas.drawText("فاتورة توريد اللوازم والتركيب والعقل من المحل إلى موقع العمل", 297.5f, 65f, textPaint)

            // Invoice Metadata Section
            var y = 120f
            textPaint.color = Color.BLACK
            textPaint.textSize = 11f

            val dateFormat = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date())

            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("رقم الفاتورة: #INV-${project.id}", 540f, y, textPaint)
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("التاريخ: $dateStr", 55f, y, textPaint)

            y += 20f
            textPaint.textAlign = Paint.Align.RIGHT
            val clientStr = if (project.clientName.isNotBlank()) project.clientName else project.title
            canvas.drawText("اسم العميل: $clientStr", 540f, y, textPaint)
            textPaint.textAlign = Paint.Align.LEFT
            val siteStr = if (siteLocation.isNotBlank()) siteLocation else if (project.location.isNotBlank()) project.location else "ورشة العمل"
            canvas.drawText("موقع العمل: $siteStr", 55f, y, textPaint)

            y += 20f
            textPaint.textAlign = Paint.Align.RIGHT
            val mgrStr = if (managerName.isNotBlank()) managerName else "غير محدد"
            canvas.drawText("المدير المشرف: $mgrStr", 540f, y, textPaint)
            textPaint.textAlign = Paint.Align.LEFT
            val wrkStr = if (workerName.isNotBlank()) workerName else "فني الورشة"
            canvas.drawText("الفني المكلف: $wrkStr", 55f, y, textPaint)

            // Table Header Bar
            y += 25f
            paint.color = Color.parseColor("#006B42")
            canvas.drawRect(45f, y, 550f, y + 24f, paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 10f
            textPaint.isFakeBoldText = true

            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("بيان اللوازم والمواد المسلمة", 535f, y + 16f, textPaint)

            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("الكمية", 290f, y + 16f, textPaint)

            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("سعر الوحدة", 170f, y + 16f, textPaint)
            canvas.drawText("الإجمالي", 60f, y + 16f, textPaint)

            y += 24f
            val materialsTotal = items.sumOf { it.getTotalPrice() }

            // Item Rows
            items.forEachIndexed { idx, item ->
                y += 20f

                // Row zebra striping
                if (idx % 2 == 1) {
                    paint.color = Color.parseColor("#F8FAFC")
                    canvas.drawRect(45f, y - 14f, 550f, y + 6f, paint)
                }

                textPaint.color = Color.BLACK
                textPaint.textSize = 10f
                textPaint.isFakeBoldText = false

                textPaint.textAlign = Paint.Align.RIGHT
                val itemName = "${idx + 1}. ${item.materialNameAr} (${item.size})"
                canvas.drawText(itemName, 535f, y, textPaint)

                textPaint.textAlign = Paint.Align.CENTER
                val qtyVal = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                canvas.drawText("$qtyVal ${item.unit}", 290f, y, textPaint)

                textPaint.textAlign = Paint.Align.LEFT
                val uPrice = if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toInt().toString() else item.unitPrice.toString()
                val tPrice = if (item.getTotalPrice() % 1.0 == 0.0) item.getTotalPrice().toInt().toString() else item.getTotalPrice().toString()
                canvas.drawText("$uPrice د.ج", 170f, y, textPaint)

                textPaint.isFakeBoldText = true
                textPaint.color = Color.parseColor("#006B42")
                canvas.drawText("$tPrice د.ج", 60f, y, textPaint)
            }

            y += 25f
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(45f, y, 550f, y, paint)

            // Financial Summary Block
            y += 20f
            textPaint.color = Color.BLACK
            textPaint.textSize = 11f
            textPaint.isFakeBoldText = true

            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("إجمالي المواد واللوازم:", 535f, y, textPaint)
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("${materialsTotal.toInt()} د.ج", 60f, y, textPaint)

            if (installationCost > 0) {
                y += 18f
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("أجرة التركيب واليد العاملة:", 535f, y, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("${installationCost.toInt()} د.ج", 60f, y, textPaint)
            }

            if (fittingsCost > 0) {
                y += 18f
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("تكلفة العقل والملحقات والتوصيلات:", 535f, y, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("${fittingsCost.toInt()} د.ج", 60f, y, textPaint)
            }

            if (transportFee > 0) {
                y += 18f
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("رسوم النقل والتوصيل للموقع:", 535f, y, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("${transportFee.toInt()} د.ج", 60f, y, textPaint)
            }

            val grandTotal = materialsTotal + installationCost + fittingsCost + transportFee
            val remaining = (grandTotal - paidAmount).coerceAtLeast(0.0)

            // Grand Total Box
            y += 25f
            paint.color = Color.parseColor("#F0FDF4")
            canvas.drawRect(45f, y - 15f, 550f, y + 12f, paint)

            textPaint.color = Color.parseColor("#006B42")
            textPaint.textSize = 13f
            textPaint.isFakeBoldText = true

            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("المبلغ الإجمالي النهائي للفاتورة:", 535f, y, textPaint)
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("${grandTotal.toInt()} د.ج", 60f, y, textPaint)

            y += 25f
            textPaint.color = Color.parseColor("#15803D")
            textPaint.textSize = 11f
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("المبلغ المدفوع تسقيعاً / عربون:", 535f, y, textPaint)
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("${paidAmount.toInt()} د.ج", 60f, y, textPaint)

            y += 18f
            textPaint.color = Color.parseColor("#B91C1C")
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("المبلغ المتبقي للتحصيل عند التسليم:", 535f, y, textPaint)
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("${remaining.toInt()} د.ج", 60f, y, textPaint)

            if (invoiceNotes.isNotBlank()) {
                y += 30f
                textPaint.color = Color.DKGRAY
                textPaint.textSize = 10f
                textPaint.isFakeBoldText = false
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("ملاحظات وشروط الفاتورة: $invoiceNotes", 535f, y, textPaint)
            }

            pdfDocument.finishPage(page)

            val cacheDir = context.cacheDir
            val pdfFile = File(cacheDir, "WorkInvoice_Project_${project.id}_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sharePdf(context: Context, pdfFile: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "تصدير / مشاركة الفاتورة ملف PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "حدث خطأ أثناء تصدير ملف PDF", Toast.LENGTH_SHORT).show()
        }
    }
}
