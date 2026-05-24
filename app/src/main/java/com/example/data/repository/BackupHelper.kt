package com.example.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.example.data.local.AppDatabase
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.KeyGenerator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object KeystoreHelper {
    private const val ALIAS = "HesabatHabayebKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    suspend fun getOrCreateKeystoreKey(): String = withContext(Dispatchers.IO) {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            if (!keyStore.containsAlias(ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                keyGenerator.init(
                    KeyGenParameterSpec.Builder(
                        ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
                )
                keyGenerator.generateKey()
            }
            val keyEntry = keyStore.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry
            val secretKey = keyEntry.secretKey
            Base64.encodeToString(secretKey.encoded ?: "backupKeyPlaceholder".toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("KeystoreHelper", "Error getting keystore key: ${e.message}", e)
            Base64.encodeToString("fallbackHesabatHabayebBackupKey123".toByteArray(), Base64.NO_WRAP)
        }
    }
}

object BackupHelper {
    private const val TAG = "BackupHelper"

    private suspend fun checkpointDatabase(context: Context) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)")
        } catch (e: Exception) {
            Log.e(TAG, "Error checkpointing database: ${e.message}", e)
        }
    }

    suspend fun createSingleBinaryBackup(context: Context): ByteArray? = withContext(Dispatchers.IO) {
        try {
            checkpointDatabase(context)
            val dbFile = context.getDatabasePath("hesabat_habayeb_db")
            val dbBytes = if (dbFile.exists()) dbFile.readBytes() else ByteArray(0)
            val keystoreKey = KeystoreHelper.getOrCreateKeystoreKey()

            val root = JSONObject()
            root.put("app", "hesabat_habayeb")
            root.put("version", 2)
            root.put("keystore_key", keystoreKey)
            root.put("db_bytes_base64", Base64.encodeToString(dbBytes, Base64.NO_WRAP))
            root.put("timestamp", System.currentTimeMillis())

            val jsonStr = root.toString()
            val bos = ByteArrayOutputStream()
            val gzos = GZIPOutputStream(bos)
            gzos.write(jsonStr.toByteArray(Charsets.UTF_8))
            gzos.close()
            bos.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error generating backup binary: ${e.message}", e)
            null
        }
    }

    suspend fun restoreSingleBinaryBackup(context: Context, backupBytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = GZIPInputStream(backupBytes.inputStream())
            val jsonStr = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }

            val root = JSONObject(jsonStr)
            if (!root.has("app") || root.getString("app") != "hesabat_habayeb") {
                Log.e(TAG, "Signature mismatch in single backup binary")
                return@withContext false
            }

            val dbBytesBase64 = root.getString("db_bytes_base64")
            val dbBytes = Base64.decode(dbBytesBase64, Base64.NO_WRAP)

            // Overwrite database safely
            val dbFile = context.getDatabasePath("hesabat_habayeb_db")
            AppDatabase.getDatabase(context).close()

            val walFile = context.getDatabasePath("hesabat_habayeb_db-wal")
            val shmFile = context.getDatabasePath("hesabat_habayeb_db-shm")
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            dbFile.parentFile?.mkdirs()
            dbFile.writeBytes(dbBytes)
            Log.d(TAG, "Binary backup successfully restored")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Exception restoring binary backup: ${e.message}", e)
            false
        }
    }
}
