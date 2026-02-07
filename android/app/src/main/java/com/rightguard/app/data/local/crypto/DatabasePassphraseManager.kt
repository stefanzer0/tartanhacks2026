package com.rightguard.app.data.local.crypto

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabasePassphraseManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "rightguard_secure_prefs"
        private const val KEY_DB_PASSPHRASE = "db_passphrase"
        private const val PASSPHRASE_LENGTH = 32
    }

    fun getPassphrase(): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val existing = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (existing != null) {
            return existing.toByteArray(Charsets.UTF_8)
        }

        val passphrase = generatePassphrase()
        prefs.edit().putString(KEY_DB_PASSPHRASE, String(passphrase, Charsets.UTF_8)).apply()
        return passphrase
    }

    private fun generatePassphrase(): ByteArray {
        val random = SecureRandom()
        val bytes = ByteArray(PASSPHRASE_LENGTH)
        random.nextBytes(bytes)
        // Encode to Base64-like safe string to avoid null bytes
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$"
        return ByteArray(PASSPHRASE_LENGTH) { chars[bytes[it].toInt() and 0x3F].code.toByte() }
    }
}
