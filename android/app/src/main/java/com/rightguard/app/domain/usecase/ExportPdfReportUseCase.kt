package com.rightguard.app.domain.usecase

import android.content.Context
import android.net.Uri
import com.rightguard.app.util.PdfGenerator
import javax.inject.Inject

class ExportPdfReportUseCase @Inject constructor(
    private val context: Context,
    private val generateReport: GenerateIncidentReportUseCase,
    private val pdfGenerator: PdfGenerator
) {
    suspend operator fun invoke(incidentId: Long, outputUri: Uri): Boolean {
        val report = generateReport(incidentId) ?: return false

        return try {
            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                pdfGenerator.generateIncidentReport(
                    incident = report.incident,
                    recordings = report.recordings,
                    aiLogs = report.aiLogs,
                    outputStream = outputStream
                )
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
