package com.example.ui.components

import android.content.Context
import android.content.Intent
import com.example.data.model.Project
import com.example.data.model.ProjectItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShareHelper {

    fun generateTextSummary(project: Project, items: List<ProjectItem>): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val dateStr = dateFormat.format(Date(project.createdAt))

        val builder = StringBuilder()
        builder.append("🔧 *PLUMBER — قائمة مواد المشروع*\n")
        builder.append("━━━━━━━━━━━━━━━━━━━\n")
        builder.append("📌 *اسم المشروع:* ${project.title}\n")
        if (project.clientName.isNotBlank()) {
            builder.append("👤 *العميل:* ${project.clientName}\n")
        }
        if (project.location.isNotBlank()) {
            builder.append("📍 *الموقع:* ${project.location}\n")
        }
        if (project.managerName.isNotBlank()) {
            builder.append("👨‍💼 *المدير المشرف:* ${project.managerName}\n")
        }
        if (project.workerName.isNotBlank()) {
            builder.append("👷‍♂️ *الفني المسند:* ${project.workerName}\n")
        }
        builder.append("📅 *التاريخ:* $dateStr\n")
        builder.append("🏷️ *نوع العمل:* ${project.workTypeNameAr}\n")
        builder.append("━━━━━━━━━━━━━━━━━━━\n\n")

        if (items.isEmpty()) {
            builder.append("لا توجد مواد مضافة بعد.\n")
        } else {
            builder.append("📋 *المواد والكميات المطلوبة:*\n\n")
            items.forEachIndexed { index, item ->
                val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                val pipesInfo = item.getPipesCount()?.let { " (≈ $it أنبوب 4م)" } ?: ""
                val priceInfo = if (item.unitPrice > 0) {
                    val priceVal = if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toInt().toString() else item.unitPrice.toString()
                    val totalVal = if (item.getTotalPrice() % 1.0 == 0.0) item.getTotalPrice().toInt().toString() else item.getTotalPrice().toString()
                    " [السعر: $priceVal د.ج | الإجمالي: $totalVal د.ج]"
                } else ""
                val notesStr = if (item.notes.isNotBlank()) " [ملاحظة: ${item.notes}]" else ""

                builder.append("${index + 1}. *${item.materialNameAr}* (${item.size}) — *$qtyStr ${item.unit}*$pipesInfo$priceInfo$notesStr\n")
            }

            val totalQty = items.sumOf { it.quantity }
            val totalQtyStr = if (totalQty % 1.0 == 0.0) totalQty.toInt().toString() else totalQty.toString()
            val totalCost = items.sumOf { it.getTotalPrice() }

            builder.append("\n━━━━━━━━━━━━━━━━━━━\n")
            builder.append("📊 *إجمالي أنواع المواد:* ${items.size} نوع\n")
            builder.append("📦 *إجمالي الكميات:* $totalQtyStr وحدة/متر/قطعة\n")
            if (totalCost > 0) {
                val totalCostStr = if (totalCost % 1.0 == 0.0) totalCost.toInt().toString() else totalCost.toString()
                builder.append("💰 *إجمالي تكلفة المواد:* $totalCostStr د.ج\n")
            }
            if (project.laborCost > 0) {
                builder.append("💵 *أجرة اليد العاملة:* ${project.laborCost.toInt()} د.ج (المدفوع: ${project.paidAmount.toInt()} د.ج | المتبقي: ${project.getRemainingLaborCost().toInt()} د.ج)\n")
            }
        }

        if (project.notes.isNotBlank()) {
            builder.append("\n📝 *ملاحظات المشروع:*\n${project.notes}\n")
        }

        builder.append("\n✨ *تم استخراج القائمة بواسطة تطبيق Plumber للسباكين*")
        return builder.toString()
    }

    fun generateStoreOrderSummary(
        project: Project,
        items: List<ProjectItem>,
        storeName: String,
        workerName: String,
        siteLocation: String,
        orderNotes: String
    ): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date())

        val builder = StringBuilder()
        builder.append("🛒 *طلب تجهيز لوازم ورشة - المحل*\n")
        builder.append("━━━━━━━━━━━━━━━━━━━\n")
        builder.append("🏪 *إلى المحل:* ${if (storeName.isNotBlank()) storeName else "محل التجهيزات والسباكة"}\n")
        builder.append("👷‍♂️ *الفني المرسل:* ${if (workerName.isNotBlank()) workerName else if (project.workerName.isNotBlank()) project.workerName else "فني الورشة الميداني"}\n")
        val finalLocation = siteLocation.ifBlank { project.location }.ifBlank { "موقع الورشة الميدانية" }
        builder.append("📍 *عنوان موقع العمل (للتوصيل):* $finalLocation\n")
        if (project.clientName.isNotBlank()) {
            builder.append("👤 *الورشة / الزبون:* ${project.title} (${project.clientName})\n")
        } else {
            builder.append("📌 *اسم الورشة:* ${project.title}\n")
        }
        builder.append("📅 *وقت الطلب:* $dateStr\n")
        builder.append("━━━━━━━━━━━━━━━━━━━\n\n")

        if (items.isEmpty()) {
            builder.append("⚠️ لا توجد مواد محددة في القائمة بعد.\n")
        } else {
            builder.append("📦 *قائمة القطع واللوازم المطلوب تجهيزها:*\n\n")
            items.forEachIndexed { index, item ->
                val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                val pipesInfo = item.getPipesCount()?.let { " (≈ $it أنبوب 4م)" } ?: ""
                val priceInfo = if (item.unitPrice > 0) {
                    val priceVal = if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toInt().toString() else item.unitPrice.toString()
                    " [السعر: $priceVal د.ج]"
                } else ""
                val itemNote = if (item.notes.isNotBlank()) " [تنبيه: ${item.notes}]" else ""

                builder.append("${index + 1}. *${item.materialNameAr}* (${item.size})\n")
                builder.append("   ← *الكمية:* $qtyStr ${item.unit}$pipesInfo $priceInfo$itemNote\n")
            }

            val totalCost = items.sumOf { it.getTotalPrice() }
            builder.append("\n━━━━━━━━━━━━━━━━━━━\n")
            builder.append("📊 *إجمالي الأصناف:* ${items.size} نوع مادة\n")
            if (totalCost > 0) {
                val totalCostStr = if (totalCost % 1.0 == 0.0) totalCost.toInt().toString() else totalCost.toString()
                builder.append("💰 *إجمالي قيمة اللوازم:* $totalCostStr د.ج\n")
            }
        }

        if (orderNotes.isNotBlank()) {
            builder.append("\n📝 *ملاحظات وتوجيهات السائق/المحل:*\n$orderNotes\n")
        }

        builder.append("\n🚚 *تنبيه للمحل:* يُرجى إعداد هذه اللوازم وإرسالها سائق التوصيل فوراً إلى عنوان موقع العمل الموضح أعلاه.")
        builder.append("\n✨ *أُرسلت بواسطة تطبيق إدارة عمال ورشات السباكة*")
        return builder.toString()
    }

    fun generateFullInvoiceSummary(
        project: Project,
        items: List<ProjectItem>,
        storeName: String,
        workerName: String,
        managerName: String,
        siteLocation: String,
        installationCost: Double,
        fittingsCost: Double,
        transportFee: Double,
        paidAmount: Double,
        invoiceNotes: String
    ): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date())

        val builder = StringBuilder()
        builder.append("📄 *فاتورة توريد اللوازم والتركيب والعقل*\n")
        builder.append("📍 *من المحل إلى موقع ورشة العمل*\n")
        builder.append("━━━━━━━━━━━━━━━━━━━\n")
        builder.append("🏪 *المحل/المورد:* ${storeName.ifBlank { "محل السباكة والتجهيزات" }}\n")
        builder.append("👨‍💼 *المدير المشرف:* ${managerName.ifBlank { project.managerName.ifBlank { "غير محدد" } }}\n")
        builder.append("👷‍♂️ *الفني المكلف:* ${workerName.ifBlank { project.workerName.ifBlank { "فني الورشة" } }}\n")
        builder.append("👤 *العميل:* ${project.clientName.ifBlank { project.title }}\n")
        builder.append("📍 *عنوان موقع العمل:* ${siteLocation.ifBlank { project.location.ifBlank { "موقع الورشة" } }}\n")
        builder.append("📅 *التاريخ والوقت:* $dateStr\n")
        builder.append("━━━━━━━━━━━━━━━━━━━\n\n")

        val materialsTotal = items.sumOf { it.getTotalPrice() }

        if (items.isNotEmpty()) {
            builder.append("📦 *تفاصيل اللوازم والمواد المسلمة:*\n")
            items.forEachIndexed { index, item ->
                val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                val priceVal = if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toInt().toString() else item.unitPrice.toString()
                val totalVal = if (item.getTotalPrice() % 1.0 == 0.0) item.getTotalPrice().toInt().toString() else item.getTotalPrice().toString()
                builder.append("${index + 1}. ${item.materialNameAr} (${item.size}) — $qtyStr ${item.unit}")
                if (item.unitPrice > 0) {
                    builder.append(" [السعر: $priceVal د.ج | الإجمالي: $totalVal د.ج]")
                }
                builder.append("\n")
            }
            builder.append("\n")
        }

        val grandTotal = materialsTotal + installationCost + fittingsCost + transportFee
        val remaining = (grandTotal - paidAmount).coerceAtLeast(0.0)

        builder.append("💰 *ملخص المبالغ والمالية للفاتورة:*\n")
        builder.append("• إجمالي اللوازم والمواد: ${materialsTotal.toInt()} د.ج\n")
        if (installationCost > 0) {
            builder.append("• أجرة التركيب واليد العاملة: ${installationCost.toInt()} د.ج\n")
        }
        if (fittingsCost > 0) {
            builder.append("• تكلفة العقل والتوصيلات: ${fittingsCost.toInt()} د.ج\n")
        }
        if (transportFee > 0) {
            builder.append("• رسوم النقل والتوصيل للموقع: ${transportFee.toInt()} د.ج\n")
        }
        builder.append("━━━━━━━━━━━━━━━━━━━\n")
        builder.append("🔮 *المبلغ الإجمالي النهائي:* ${grandTotal.toInt()} د.ج\n")
        builder.append("🟢 *المبلغ المدفوع تسقيعاً:* ${paidAmount.toInt()} د.ج\n")
        builder.append("🔴 *المبلغ المتبقي للتحصيل عند الاستلام:* ${remaining.toInt()} د.ج\n")

        if (invoiceNotes.isNotBlank()) {
            builder.append("\n📝 *ملاحظات الفاتورة:*\n$invoiceNotes\n")
        }

        builder.append("\n✨ *تم استخراج الفاتورة بواسطة تطبيق إدارة عمال وورشات السباكة*")
        return builder.toString()
    }

    fun shareToWhatsApp(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general chooser
            shareToGeneral(context, text)
        }
    }

    fun shareToGeneral(context: Context, text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "مشاركة قائمة مواد المشروع عبر:")
        context.startActivity(shareIntent)
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("قائمة المواد", text)
        clipboard.setPrimaryClip(clip)
    }
}
