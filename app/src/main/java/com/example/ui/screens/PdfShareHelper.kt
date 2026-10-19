package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.Plant
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfShareHelper {

    fun sharePlantPdf(context: Context, plant: Plant) {
        try {
            // 1. Setup file container
            val sanitizedName = plant.name.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim().ifEmpty { "plant_${plant.id}" }
            val file = File(context.filesDir, "$sanitizedName.pdf")
            if (file.exists()) {
                file.delete()
            }

            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 standard width in points
            val pageHeight = 842 // A4 standard height in points
            val marginX = 40f
            val marginY = 50f
            val contentWidth = pageWidth - (marginX * 2)

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            var currentY = marginY

            // Setup custom display paints
            val textPaint = TextPaint().apply {
                color = Color.rgb(31, 41, 55) // Dark Gray / neutral-800
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val titlePaint = Paint().apply {
                color = Color.rgb(5, 150, 105) // Emerald Green primary (ForestGreenPrimary style)
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val sectionHeaderPaint = Paint().apply {
                color = Color.rgb(17, 24, 39) // Slate neutral-900
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val metadataPaint = Paint().apply {
                color = Color.rgb(100, 116, 139) // slate-500
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                isAntiAlias = true
            }

            // Decorative subsequent page header
            fun drawHeaderOnPage(c: Canvas, plantName: String, width: Int, mX: Float, mY: Float) {
                val headerPaint = Paint().apply {
                    color = Color.rgb(156, 163, 175) // gray-400
                    textSize = 8f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    isAntiAlias = true
                }
                c.drawText("PlantCare Guide: $plantName", mX, mY - 14f, headerPaint)
                val linePaint = Paint().apply {
                    color = Color.rgb(229, 231, 235) // gray-200
                    strokeWidth = 0.8f
                }
                c.drawLine(mX, mY - 8f, width - mX, mY - 8f, linePaint)
            }

            // Standard bottom footer on every page
            fun drawFooter(c: Canvas, pNum: Int, pWidth: Int, pHeight: Int, mX: Float, mY: Float) {
                val linePaint = Paint().apply {
                    color = Color.rgb(229, 231, 235) // gray-200
                    strokeWidth = 0.8f
                }
                c.drawLine(mX, pHeight - mY + 10f, pWidth - mX, pHeight - mY + 10f, linePaint)

                val footerPaint = Paint().apply {
                    color = Color.rgb(100, 116, 139)
                    textSize = 8f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }
                c.drawText("This App Was Created by Raihan Toha", mX, pHeight - mY + 22f, footerPaint)

                val pageNumStr = "Page $pNum"
                val textWidth = footerPaint.measureText(pageNumStr)
                c.drawText(pageNumStr, pWidth - mX - textWidth, pHeight - mY + 22f, footerPaint)
            }

            // Multi-page helper that transitions to a new canvas whenever vertical space is exhausted
            fun ensureSpace(heightNeeded: Float) {
                if (currentY + heightNeeded > pageHeight - marginY) {
                    drawFooter(canvas, pageNumber, pageWidth, pageHeight, marginX, marginY)

                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = marginY + 20f

                    drawHeaderOnPage(canvas, plant.name, pageWidth, marginX, marginY)
                }
            }

            // Word wrap function for wrapping text paragraphs perfectly inside the PDF width
            fun wrapText(rawText: String, paint: TextPaint, maxWidth: Float): List<String> {
                val linesList = mutableListOf<String>()
                val paragraphs = rawText.split("\n")
                for (paragraph in paragraphs) {
                    if (paragraph.trim().isEmpty()) {
                        linesList.add("")
                        continue
                    }
                    val words = paragraph.split(Regex("\\s+"))
                    var currentLine = StringBuilder()
                    for (word in words) {
                        if (word.isEmpty()) continue
                        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                        if (paint.measureText(testLine) <= maxWidth) {
                            currentLine.append(if (currentLine.isEmpty()) word else " $word")
                        } else {
                            linesList.add(currentLine.toString())
                            currentLine = StringBuilder(word)
                        }
                    }
                    if (currentLine.isNotEmpty()) {
                        linesList.add(currentLine.toString())
                    }
                }
                return linesList
            }

            // Draw Section with Title and auto-wrapped lines
            fun printParagraph(text: String, sectionTitle: String? = null) {
                if (text.trim().isEmpty()) return

                if (sectionTitle != null) {
                    ensureSpace(40f) // Reserve padding space for section title + a few lines of context
                    currentY += 15f
                    canvas.drawText(sectionTitle, marginX, currentY, sectionHeaderPaint)
                    currentY += 8f
                    val linePaint = Paint().apply {
                        color = Color.rgb(243, 244, 246) // light grey separator line
                        strokeWidth = 1.2f
                    }
                    canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, linePaint)
                    currentY += 12f
                }

                val lines = wrapText(text, textPaint, contentWidth)
                for (line in lines) {
                    if (line.isEmpty()) {
                        ensureSpace(12f)
                        currentY += 12f
                        continue
                    }
                    ensureSpace(14f)
                    canvas.drawText(line, marginX, currentY, textPaint)
                    currentY += 14f
                }
                currentY += 8f
            }

            // 2. Draft Cover Identity Header on Page 1
            val brandingPaint = Paint().apply {
                color = Color.rgb(16, 185, 129) // Emerald light brand
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("PLANTCARE OFFICIAL GUIDE", marginX, currentY, brandingPaint)
            currentY += 22f

            // Large main title
            canvas.drawText(plant.name, marginX, currentY, titlePaint)
            currentY += 12f

            // Information metadata block
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val metadataLabel = "Category: ${plant.category}  |  Exported on: $dateStr"
            canvas.drawText(metadataLabel, marginX, currentY, metadataPaint)
            currentY += 12f

            // Thick branding line separator on front page
            val brandUnderline = Paint().apply {
                color = Color.rgb(5, 150, 105)
                strokeWidth = 2.5f
            }
            canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, brandUnderline)
            currentY += 20f

            // 3. Render Leaf and Plant images if Base64 uploaded
            val bitmap = decodeBase64ToBitmap(plant.image)
            if (bitmap != null) {
                val targetWidth = 180f
                val ratio = bitmap.height.toFloat() / bitmap.width.toFloat()
                val targetHeight = ratio * targetWidth
                
                // Keep image safely small or wrap page if necessary
                val safeHeight = if (targetHeight > 180f) 180f else targetHeight
                ensureSpace(safeHeight + 20f)
                
                val dstRect = android.graphics.RectF(marginX, currentY, marginX + targetWidth, currentY + safeHeight)
                canvas.drawBitmap(bitmap, null, dstRect, null)
                currentY += safeHeight + 20f
            }

            // 4. Fill in Plant Bio details
            printParagraph(plant.summary, "Summary")
            printParagraph(plant.about, "About Plant")
            printParagraph(plant.growingInstructions, "Growing Instructions")
            printParagraph(plant.careTips, "Care & Maintenance Tips")
            printParagraph(plant.sicknessInfo, "Pests & Disease Diagnosis Info")
            printParagraph(plant.remedies, "Recommended Remediation Guide")

            // 5. Appended Health Issue reports
            if (plant.healthIssues.isNotEmpty()) {
                ensureSpace(40f)
                currentY += 15f
                canvas.drawText("Detailed Sickness Diagnostic Log", marginX, currentY, sectionHeaderPaint)
                currentY += 8f
                val linePaint = Paint().apply {
                    color = Color.rgb(243, 244, 246)
                    strokeWidth = 1.2f
                }
                canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, linePaint)
                currentY += 12f

                plant.healthIssues.forEachIndexed { idx, issue ->
                    ensureSpace(35f)

                    val issueBadgePaint = Paint().apply {
                        color = Color.rgb(220, 38, 38) // deep clinical warning red
                        textSize = 10f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                    }
                    currentY += 12f
                    canvas.drawText("Issue #${idx + 1}: ${issue.issue}", marginX, currentY, issueBadgePaint)
                    currentY += 4f

                    val actionLines = wrapText("Suggested Remedy: ${issue.remedy}", textPaint, contentWidth - 12f)
                    for (line in actionLines) {
                        ensureSpace(14f)
                        canvas.drawText(line, marginX + 12f, currentY + 12f, textPaint)
                        currentY += 14f
                    }
                    currentY += 6f
                }
            }

            // 6. Draw footer on the final page, close, and write to filesystem disk
            drawFooter(canvas, pageNumber, pageWidth, pageHeight, marginX, marginY)
            pdfDocument.finishPage(page)

            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()

            // 7. Fire standard Share Intent sheet with absolute FileProvider Uri
            val authority = "${context.packageName}.fileprovider"
            val infoUri = FileProvider.getUriForFile(context, authority, file)

            val uris = ArrayList<android.net.Uri>()
            uris.add(infoUri)

            // Include the plant's main photo as an attachment
            if (plant.image.isNotEmpty()) {
                try {
                    val decodedBitmap = decodeBase64ToBitmap(plant.image)
                    if (decodedBitmap != null) {
                        val photoFile = File(context.cacheDir, "${sanitizedName}_photo.jpg")
                        if (photoFile.exists()) {
                            photoFile.delete()
                        }
                        val fosPhoto = FileOutputStream(photoFile)
                        decodedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fosPhoto)
                        fosPhoto.close()
                        uris.add(FileProvider.getUriForFile(context, authority, photoFile))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Include any attached PDFs as attachments
            plant.pdfs.forEach { pdf ->
                try {
                    val attachedFile = File(context.filesDir, pdf.filename)
                    if (attachedFile.exists()) {
                        uris.add(FileProvider.getUriForFile(context, authority, attachedFile))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val shareIntent = if (uris.size > 1) {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "*/*" // Mix of PDF and images/JPEGs
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    putExtra(Intent.EXTRA_SUBJECT, "PlantCare: ${plant.name} - Info & Attachments")
                    putExtra(Intent.EXTRA_TEXT, "Here is the compiled detailed report, photos, and guide attachments for the ${plant.name} plant, exported from PlantCare.")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, infoUri)
                    putExtra(Intent.EXTRA_SUBJECT, "PlantCare: ${plant.name} - Report Guide")
                    putExtra(Intent.EXTRA_TEXT, "Here is the comprehensive profile and diagnostics guide for the ${plant.name} plant, exported from PlantCare.")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share Plant Info via")
            chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.startActivity(chooserIntent)

        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Failed to share PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun decodeBase64ToBitmap(base64Str: String): android.graphics.Bitmap? {
        if (base64Str.isEmpty()) return null
        return try {
            val cleanString = if (base64Str.startsWith("data:")) {
                base64Str.substringAfter("base64,")
            } else {
                base64Str
            }
            val decodedBytes = android.util.Base64.decode(cleanString, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
