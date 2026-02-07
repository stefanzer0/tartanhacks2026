package com.rightguard.app.util

import android.content.Context
import com.rightguard.app.data.local.crypto.CryptoManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedFileManager @Inject constructor(
    private val context: Context,
    private val cryptoManager: CryptoManager
) {
    private val recordingsDir: File
        get() = File(context.filesDir, "recordings").also { it.mkdirs() }

    fun getRecordingFile(filename: String): File {
        return File(recordingsDir, filename)
    }

    fun createEncryptedOutputStream(filename: String): CipherOutputStream {
        val file = getRecordingFile(filename)
        val fos = FileOutputStream(file)
        return cryptoManager.encryptStream(fos)
    }

    fun createDecryptedInputStream(filename: String): CipherInputStream {
        val file = getRecordingFile(filename)
        val fis = FileInputStream(file)
        return cryptoManager.decryptStream(fis)
    }

    fun createEncryptedOutputStream(file: File): CipherOutputStream {
        val fos = FileOutputStream(file)
        return cryptoManager.encryptStream(fos)
    }

    fun createDecryptedInputStream(file: File): CipherInputStream {
        val fis = FileInputStream(file)
        return cryptoManager.decryptStream(fis)
    }

    fun deleteRecording(filename: String): Boolean {
        return getRecordingFile(filename).delete()
    }

    fun getRecordingFiles(): List<File> {
        return recordingsDir.listFiles()?.toList() ?: emptyList()
    }

    fun generateRecordingFilename(incidentId: Long): String {
        return "recording_${incidentId}_${System.currentTimeMillis()}.enc"
    }
}
