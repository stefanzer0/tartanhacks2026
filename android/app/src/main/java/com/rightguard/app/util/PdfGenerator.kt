package com.rightguard.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.rightguard.app.domain.model.AiAdviceLog
import com.rightguard.app.domain.model.Incident
import com.rightguard.app.domain.model.Recording
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfGenerator @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val PAGE_WIDTH = 612 // US Letter
        private const val PAGE_HEIGHT = 792
        private const val MARGIN = 48f
        private const val LINE_HEIGHT = 16f
        private const val HEADER_SIZE = 20f
        private const val SUBHEADER_SIZE = 14f
        private const val BODY_SIZE = 11f
    }

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US)

    fun generateIncidentReport(
        incident: Incident,
        recordings: List<Recording>,
        aiLogs: List<AiAdviceLog>,
        outputStream: OutputStream
    ) {
        val document = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var yPos = MARGIN

        val titlePaint = Paint().apply {
            textSize = HEADER_SIZE
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subheaderPaint = Paint().apply {
            textSize = SUBHEADER_SIZE
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            textSize = BODY_SIZE
            isAntiAlias = true
        }

        // Title
        canvas.drawText("RightGuard Incident Report", MARGIN, yPos + HEADER_SIZE, titlePaint)
        yPos += HEADER_SIZE + LINE_HEIGHT * 2

        // Incident Details
        canvas.drawText("Incident Details", MARGIN, yPos + SUBHEADER_SIZE, subheaderPaint)
        yPos += SUBHEADER_SIZE + LINE_HEIGHT

        val details = listOf(
            "Type: ${incident.encounterType.name.replace("_", " ")}",
            "Status: ${incident.status.name}",
            "Start: ${dateFormat.format(Date(incident.startTime))}",
            "End: ${incident.endTime?.let { dateFormat.format(Date(it)) } ?: "Ongoing"}",
            "Location: ${incident.latitude?.let { "%.6f, %.6f".format(it, incident.longitude) } ?: "Unknown"}"
        )
        for (detail in details) {
            canvas.drawText(detail, MARGIN + 16f, yPos + BODY_SIZE, bodyPaint)
            yPos += LINE_HEIGHT
        }
        yPos += LINE_HEIGHT

        // Notes
        if (incident.notes.isNotBlank()) {
            canvas.drawText("Notes", MARGIN, yPos + SUBHEADER_SIZE, subheaderPaint)
            yPos += SUBHEADER_SIZE + LINE_HEIGHT
            for (line in wrapText(incident.notes, bodyPaint, PAGE_WIDTH - 2 * MARGIN - 16f)) {
                if (yPos + LINE_HEIGHT > PAGE_HEIGHT - MARGIN) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = MARGIN
                }
                canvas.drawText(line, MARGIN + 16f, yPos + BODY_SIZE, bodyPaint)
                yPos += LINE_HEIGHT
            }
            yPos += LINE_HEIGHT
        }

        // AI Advice Timeline
        if (aiLogs.isNotEmpty()) {
            canvas.drawText("AI Advice Timeline", MARGIN, yPos + SUBHEADER_SIZE, subheaderPaint)
            yPos += SUBHEADER_SIZE + LINE_HEIGHT

            for (log in aiLogs) {
                if (yPos + LINE_HEIGHT * 4 > PAGE_HEIGHT - MARGIN) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = MARGIN
                }

                canvas.drawText("[${dateFormat.format(Date(log.timestamp))}]", MARGIN + 16f, yPos + BODY_SIZE, bodyPaint)
                yPos += LINE_HEIGHT
                canvas.drawText("Q: ${log.query.take(80)}", MARGIN + 32f, yPos + BODY_SIZE, bodyPaint)
                yPos += LINE_HEIGHT
                for (line in wrapText("A: ${log.response}", bodyPaint, PAGE_WIDTH - 2 * MARGIN - 48f)) {
                    if (yPos + LINE_HEIGHT > PAGE_HEIGHT - MARGIN) {
                        document.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                        page = document.startPage(pageInfo)
                        canvas = page.canvas
                        yPos = MARGIN
                    }
                    canvas.drawText(line, MARGIN + 48f, yPos + BODY_SIZE, bodyPaint)
                    yPos += LINE_HEIGHT
                }
                yPos += LINE_HEIGHT / 2
            }
            yPos += LINE_HEIGHT
        }

        // Recordings
        if (recordings.isNotEmpty()) {
            if (yPos + LINE_HEIGHT * 3 > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPos = MARGIN
            }

            canvas.drawText("Recordings", MARGIN, yPos + SUBHEADER_SIZE, subheaderPaint)
            yPos += SUBHEADER_SIZE + LINE_HEIGHT

            for (recording in recordings) {
                val duration = recording.durationMs / 1000
                val info = "Recording #${recording.id} - ${duration}s - ${recording.status.name}"
                canvas.drawText(info, MARGIN + 16f, yPos + BODY_SIZE, bodyPaint)
                yPos += LINE_HEIGHT
            }
        }

        // Footer
        if (yPos + LINE_HEIGHT * 3 > PAGE_HEIGHT - MARGIN) {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            yPos = MARGIN
        }
        yPos = PAGE_HEIGHT - MARGIN - LINE_HEIGHT
        canvas.drawText(
            "Generated by RightGuard on ${dateFormat.format(Date())}",
            MARGIN, yPos, bodyPaint
        )

        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }
}
