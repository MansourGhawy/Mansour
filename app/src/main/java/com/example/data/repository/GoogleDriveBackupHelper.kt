package com.example.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object GoogleDriveBackupHelper {
    private const val TAG = "GDriveBackup"
    private const val BACKUP_FILE_NAME = "hesabat_habayeb_backup.hbhabayeb"
    private val client = OkHttpClient()

    // Preferences for GDrive backup
    private const val PREFS_NAME = "gdrive_backup_prefs"
    private const val KEY_AUTO_BACKUP = "gdrive_auto_backup"
    private const val KEY_LAST_BACKUP = "gdrive_last_backup_time"
    private const val KEY_BACKUP_STATUS = "gdrive_backup_status"

    fun getGoogleSignInClient(context: Context): GoogleSignInClient? {
        return try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
                .build()
            GoogleSignIn.getClient(context, gso)
        } catch (e: Throwable) {
            Log.e(TAG, "getGoogleSignInClient error: ${e.message}", e)
            null
        }
    }

    fun isUserSignedIn(context: Context): Boolean {
        return try {
            GoogleSignIn.getLastSignedInAccount(context) != null
        } catch (e: Throwable) {
            Log.e(TAG, "isUserSignedIn error: ${e.message}", e)
            false
        }
    }

    fun getSignedInEmail(context: Context): String? {
        return try {
            GoogleSignIn.getLastSignedInAccount(context)?.email
        } catch (e: Throwable) {
            Log.e(TAG, "getSignedInEmail error: ${e.message}", e)
            null
        }
    }

    fun getSignedInName(context: Context): String? {
        return try {
            GoogleSignIn.getLastSignedInAccount(context)?.displayName
        } catch (e: Throwable) {
            Log.e(TAG, "getSignedInName error: ${e.message}", e)
            null
        }
    }

    // Toggle Auto Backup Settings
    fun isAutoBackupEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_BACKUP, false)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_BACKUP, enabled).apply()
    }

    fun getLastBackupTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_BACKUP, 0L)
    }

    fun setLastBackupTime(context: Context, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_BACKUP, timestamp).apply()
    }

    fun getBackupStatus(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_BACKUP_STATUS, "لا توجد نسخ سحابية حالياً") ?: "لا توجد نسخ سحابية حالياً"
    }

    fun setBackupStatus(context: Context, status: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BACKUP_STATUS, status).apply()
    }

    // Fetches fresh OAuth access token in background thread
    suspend fun fetchAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val account = try {
                GoogleSignIn.getLastSignedInAccount(context)
            } catch (e: Throwable) {
                null
            }
            val gAccount = account?.account ?: return@withContext null
            val scopes = "oauth2:https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/userinfo.email"
            
            GoogleAuthUtil.getToken(context, gAccount, scopes)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to fetch access token: ${e.message}", e)
            null
        }
    }

    // Searches for existing backup file on Google Drive (returns fileId if found, null otherwise)
    private suspend fun searchBackupFile(accessToken: String): String? = withContext(Dispatchers.IO) {
        try {
            val queryUrl = "https://www.googleapis.com/drive/v3/files?q=name='$BACKUP_FILE_NAME' and trashed=false&fields=files(id)"
            val request = Request.Builder()
                .url(queryUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Search file failed: ${response.code} ${response.message}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)
                val filesJsonArray = json.optJSONArray("files")
                if (filesJsonArray != null && filesJsonArray.length() > 0) {
                    val firstFile = filesJsonArray.getJSONObject(0)
                    return@withContext if (firstFile.has("id")) firstFile.getString("id") else null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception searching backup file: ${e.message}", e)
        }
        null
    }

    // Upload Backup content (Create or Update existing)
    suspend fun uploadBackup(context: Context, backupBytes: ByteArray): GDriveResult = withContext(Dispatchers.IO) {
        val accessToken = fetchAccessToken(context)
        if (accessToken == null) {
            return@withContext GDriveResult.Error("فشل الحصول على تصريح الوصول لأمان جوجل")
        }

        try {
            val fileId = searchBackupFile(accessToken)
            if (fileId != null) {
                // Update existing file content directly via PATCH upload
                val updateUrl = "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media"
                val mediaType = "application/octet-stream".toMediaTypeOrNull()
                val requestBody = backupBytes.toRequestBody(mediaType)
                
                val request = Request.Builder()
                    .url(updateUrl)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .patch(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        setLastBackupTime(context, System.currentTimeMillis())
                        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd hh:mm a", java.util.Locale.getDefault())
                        val nowStr = sdf.format(java.util.Date())
                        setBackupStatus(context, "النسخة الأخيرة: $nowStr")
                        return@withContext GDriveResult.Success("تم تحديث النسخة الاحتياطية على درايف بنجاح".toByteArray(Charsets.UTF_8))
                    } else {
                        Log.e(TAG, "Update upload failed: ${response.code} ${response.message}")
                        return@withContext GDriveResult.Error("خطأ في الاتصال بالدرايف لتحديث الملف: ${response.code}")
                    }
                }
            } else {
                // First-time creation process (A: Create File Metadata -> B: Upload raw representation)
                val createMetaUrl = "https://www.googleapis.com/drive/v3/files"
                val mediaTypeJson = "application/json; charset=utf-8".toMediaTypeOrNull()
                
                val metaJson = JSONObject()
                metaJson.put("name", BACKUP_FILE_NAME)
                metaJson.put("mimeType", "application/octet-stream")
                val metaRequestBody = metaJson.toString().toRequestBody(mediaTypeJson)

                val createRequest = Request.Builder()
                    .url(createMetaUrl)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(metaRequestBody)
                    .build()

                var newFileId: String? = null
                client.newCall(createRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string()
                        if (bodyStr != null) {
                            val respJson = JSONObject(bodyStr)
                            newFileId = if (respJson.has("id")) respJson.getString("id") else null
                        }
                    } else {
                        Log.e(TAG, "Metadata creation failed: ${response.code} ${response.message}")
                        return@withContext GDriveResult.Error("فشل إنشاء ملف النسخة الاحتياطية سحابياً")
                    }
                }

                val finalFileId = newFileId ?: return@withContext GDriveResult.Error("فشل الحصول على مُعرِّف الملف السحابي")

                // B: Upload real backup content to that newly created FileId
                val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files/$finalFileId?uploadType=media"
                val mediaType = "application/octet-stream".toMediaTypeOrNull()
                val uploadRequestBody = backupBytes.toRequestBody(mediaType)
                val uploadRequest = Request.Builder()
                    .url(uploadUrl)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .patch(uploadRequestBody)
                    .build()

                client.newCall(uploadRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        setLastBackupTime(context, System.currentTimeMillis())
                        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd hh:mm a", java.util.Locale.getDefault())
                        val nowStr = sdf.format(java.util.Date())
                        setBackupStatus(context, "النسخة الأخيرة: $nowStr")
                        return@withContext GDriveResult.Success("تم إنشاء وحفظ أول نسخة احتياطية سحابية بنجاح!".toByteArray(Charsets.UTF_8))
                    } else {
                        Log.e(TAG, "Content upload failed: ${response.code} ${response.message}")
                        return@withContext GDriveResult.Error("فشل رفع شفرة المحتوى للملف السحابي")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception uploading backup: ${e.message}", e)
            return@withContext GDriveResult.Error("خطأ استثنائي أثناء مزامنة درايف: ${e.localizedMessage}")
        }
    }

    // Read Backup from Google Drive directly
    suspend fun downloadBackup(context: Context): GDriveResult = withContext(Dispatchers.IO) {
        val accessToken = fetchAccessToken(context)
        if (accessToken == null) {
            return@withContext GDriveResult.Error("فشل الحصول على تصريح الوصول لأمان جوجل")
        }

        try {
            val fileId = searchBackupFile(accessToken)
            if (fileId == null) {
                return@withContext GDriveResult.Error("لم يتم العثور على أي ملف نسخة احتياطية على حسابك بجوجل درايف")
            }

            // Download media call
            val downloadUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes == null || bytes.isEmpty()) {
                        return@withContext GDriveResult.Error("ملف النسخة الاحتياطية فارغ")
                    }
                    return@withContext GDriveResult.Success(bytes)
                } else {
                    return@withContext GDriveResult.Error("خطأ في تنزيل البيانات من على خوادم درايف: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception downloading backup: ${e.message}", e)
            return@withContext GDriveResult.Error("حدث خطأ أثناء تنزيل معلومات درايف: ${e.localizedMessage}")
        }
    }
}

sealed class GDriveResult {
    data class Success(val data: ByteArray) : GDriveResult()
    data class Error(val message: String) : GDriveResult()
}
