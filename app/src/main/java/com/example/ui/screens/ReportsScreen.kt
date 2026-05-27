package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.theme.*
import com.example.ui.viewmodel.CustomerViewModel
import com.example.ui.viewmodel.CustomerWithBalance
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsScreen(
    viewModel: CustomerViewModel,
    isDark: Boolean
) {
    val context = LocalContext.current
    val customersList by viewModel.customersWithBalances.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()

    val curTime = System.currentTimeMillis()

    val todayCalendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayStartMs = todayCalendar.timeInMillis
    val todayEndMs = todayStartMs + 24 * 60 * 60 * 1000L

    val todayPayments = remember(transactions) {
        transactions.filter { it.type == "PAYMENT" && it.timestamp in todayStartMs until todayEndMs }
            .sumOf { it.amount }
    }
    val todayDebts = remember(transactions) {
        transactions.filter { it.type == "DEBT" && it.timestamp in todayStartMs until todayEndMs }
            .sumOf { it.amount }
    }

    val thirtyDaysAgoMs = curTime - 30L * 24 * 60 * 60 * 1000L
    val urgentCustomers = remember(customersList) {
        customersList.filter { debtor ->
            val lastTime = debtor.lastTransactionTime ?: debtor.customer.createdAt
            debtor.netBalance > 0 && lastTime < thirtyDaysAgoMs
        }.sortedByDescending { it.netBalance }
    }

    val totalTheyOweMe = summary.first
    val totalPayments = transactions.filter { it.type == "PAYMENT" }.sumOf { it.amount }

    fun formatCustomCurrency(amount: Double): String {
        val formatter = java.text.NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        return "${formatter.format(amount)} ر.ي"
    }

    fun formatArabicDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd • hh:mm a", Locale("ar"))
        return sdf.format(Date(timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) DarkBg else LightBg)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "التقارير",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF1E1F30)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "إجمالي لي عند الناس: ${formatCustomCurrency(totalTheyOweMe)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NegativeRed
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "صندوق اليوم",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFC5C5D8) else Color(0xFF555268),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) PositiveGreen.copy(alpha = 0.12f) else PositiveGreen.copy(alpha = 0.08f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "محصلات اليوم",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFB0F2C2) else Color(0xFF27AE60)
                                )
                                AutoSizeText(
                                    text = formatCustomCurrency(todayPayments),
                                    baseFontSize = 18f,
                                    color = PositiveGreen
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) NegativeRed.copy(alpha = 0.12f) else NegativeRed.copy(alpha = 0.08f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "ديون اليوم",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFFFC0BD) else Color(0xFFC0392B)
                                )
                                AutoSizeText(
                                    text = formatCustomCurrency(todayDebts),
                                    baseFontSize = 18f,
                                    color = NegativeRed
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "أجندة التحصيل (${urgentCustomers.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFC5C5D8) else Color(0xFF555268),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (urgentCustomers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1E1D2F) else Color.White
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا يوجد عملاء متأخرين عن السداد لأكثر من ٣٠ يوماً حالياً.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(urgentCustomers) { debtor ->
                    val lastTime = debtor.lastTransactionTime ?: debtor.customer.createdAt
                    val formattedLastDate = formatArabicDateTime(lastTime)
                    val formattedBalance = formatCustomCurrency(debtor.netBalance)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1B1A2F) else Color.White
                        ),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF29283F) else Color(0xFFECECFA))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = debtor.customer.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF2D3436)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formattedLastDate,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                AutoSizeText(
                                    text = formattedBalance,
                                    baseFontSize = 14f,
                                    color = NegativeRed
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryPurple.copy(alpha = 0.15f))
                                        .clickable {
                                            try {
                                                val waMessage = "السلام عليكم ورحمة الله.. تذكير لطيف ببيان الحساب الحالي لدينا، حيث يبلغ الرصيد المستحق كالتالي: $formattedBalance. نسعد ونتشرف بخدمتكم دائماً."
                                                val formattedPhone = debtor.customer.phone.replace("+", "").replace(" ", "")
                                                val intent = if (debtor.customer.phone.isNotBlank()) {
                                                    Intent(Intent.ACTION_VIEW).apply {
                                                        data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(waMessage)}")
                                                    }
                                                } else {
                                                    Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, waMessage)
                                                    }
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "لم نتمكن من فتح تطبيق المشاركة", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share, 
                                        contentDescription = "WhatsApp Link",
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryPurple.copy(alpha = 0.15f))
                                        .clickable {
                                            if (debtor.customer.phone.isNotBlank()) {
                                                try {
                                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                                        data = Uri.parse("tel:${debtor.customer.phone}")
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "لم نتمكن من إجراء الاتصال", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "رقم الهاتف غير مسجل لهذا العميل", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone, 
                                        contentDescription = "Call Customer",
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    val topAging = customersList.filter { it.netBalance > 0 }
                        .sortedByDescending { it.netBalance }
                        .take(5)
                    generatePdfReport(context, totalTheyOweMe, totalPayments, summary.third, topAging)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("export_pdf_button"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(25.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share, 
                    contentDescription = "Export PDF", 
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "تصدير تقرير PDF شامل",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

private fun generatePdfReport(
    context: Context,
    totalDebt: Double,
    totalPayment: Double,
    net: Double,
    top5: List<CustomerWithBalance>
) {
    try {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            textSize = 24f
            color = 0xFF5F4BDB.toInt()
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }

        val textPaint = Paint().apply {
            textSize = 14f
            color = 0xFF2D3436.toInt()
            textAlign = Paint.Align.RIGHT
        }

        val greenPaint = Paint().apply {
            textSize = 15f
            color = 0xFF2ECC71.toInt()
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }

        val redPaint = Paint().apply {
            textSize = 15f
            color = 0xFFE74C3C.toInt()
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }

        val linePaint = Paint().apply {
            strokeWidth = 1f
            color = 0xFFDCDDE1.toInt()
        }

        var yPos = 60f

        canvas.drawText("تقرير حسابات حبايب المالي", 540f, yPos, titlePaint)
        yPos += 30f
        canvas.drawLine(50f, yPos, 545f, yPos, linePaint)
        yPos += 40f

        val format = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar"))
        canvas.drawText("التاريخ: ${format.format(Date())}", 540f, yPos, textPaint)
        yPos += 40f

        canvas.drawText("الملخص المالي العام:", 540f, yPos, Paint(titlePaint).apply { textSize = 16f })
        yPos += 30f

        canvas.drawText("إجمالي المبالغ المستحقة لي: ${totalDebt.toInt()} ر.ي", 540f, yPos, redPaint)
        yPos += 24f
        canvas.drawText("إجمالي المبالغ المسددة: ${totalPayment.toInt()} ر.ي", 540f, yPos, greenPaint)
        yPos += 24f
        canvas.drawText("الرصيد الكلي الصافي: ${net.toInt()} ر.ي", 540f, yPos, if (net >= 0) greenPaint else redPaint)
        yPos += 35f

        canvas.drawLine(50f, yPos, 545f, yPos, linePaint)
        yPos += 40f

        canvas.drawText("أعلى زبائن مدينين ومتبقي عليهم مبالغ:", 540f, yPos, Paint(titlePaint).apply { textSize = 16f })
        yPos += 30f

        if (top5.isEmpty()) {
            canvas.drawText("لا يوجد زبائن ذوي متبقي عليهم ديون في الوقت الحالي.", 540f, yPos, textPaint)
        } else {
            top5.forEachIndexed { index, person ->
                canvas.drawText(
                    "${index + 1}. ${person.customer.name} - متبقي عليه: ${person.netBalance.toInt()} ر.ي",
                    540f,
                    yPos,
                    textPaint
                )
                yPos += 28f
            }
        }

        yPos = 780f
        canvas.drawLine(50f, yPos, 545f, yPos, linePaint)
        yPos += 20f
        canvas.drawText("تم توليده بواسطة تطبيق حسابات حبايب للهواتف - محلي وآمن ١٠٠٪", 540f, yPos, Paint(textPaint).apply { textSize = 10f })

        document.finishPage(page)

        val cacheDir = context.cacheDir
        val pdfFile = File(cacheDir, "hesabat_habayeb_report.pdf")
        val stream = FileOutputStream(pdfFile)
        document.writeTo(stream)
        document.close()
        stream.close()

        val pdfUri = FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "تصدير ومشاركة التقرير عبر:"))

    } catch (e: Exception) {
        Toast.makeText(context, "حدث خطأ أثناء تصدير ملف الـ PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
