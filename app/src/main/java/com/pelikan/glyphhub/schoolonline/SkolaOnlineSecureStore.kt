package com.pelikan.glyphhub.schoolonline

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class SkolaOnlineSecureStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun accountStatus(): SkolaOnlineAccountStatus =
        SkolaOnlineAccountStatus(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            server = SkolaOnlineServer.fromId(prefs.getString(KEY_SERVER, SkolaOnlineServer.Default.id)),
            username = getString(KEY_USERNAME).orEmpty(),
            hasPassword = getString(KEY_PASSWORD)?.isNotBlank() == true,
            hasRefreshToken = getString(KEY_REFRESH_TOKEN)?.isNotBlank() == true,
            lastSyncAtMs = prefs.getLong(KEY_LAST_SYNC_AT, 0L),
            lastSyncStatus = prefs.getString(KEY_LAST_SYNC_STATUS, "").orEmpty()
        )

    fun saveAccount(enabled: Boolean, server: SkolaOnlineServer, username: String, password: String?) {
        val normalizedUsername = username.trim()
        val previousUsername = username()
        val previousServer = server()
        prefs.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_SERVER, server.id)
            .apply()
        putString(KEY_USERNAME, normalizedUsername)
        if (normalizedUsername != previousUsername && password == null) {
            putString(KEY_PASSWORD, "")
        }
        if (password != null) {
            putString(KEY_PASSWORD, password)
        }
        if (password != null || normalizedUsername != previousUsername || server != previousServer) {
            clearTokens()
        }
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun username(): String = getString(KEY_USERNAME).orEmpty()

    fun password(): String? = getString(KEY_PASSWORD)

    fun server(): SkolaOnlineServer =
        SkolaOnlineServer.fromId(prefs.getString(KEY_SERVER, SkolaOnlineServer.Default.id))

    fun tokenSet(): SkolaOnlineTokenSet? {
        val access = getString(KEY_ACCESS_TOKEN)?.takeIf { it.isNotBlank() } ?: return null
        return SkolaOnlineTokenSet(
            accessToken = access,
            refreshToken = getString(KEY_REFRESH_TOKEN)?.takeIf { it.isNotBlank() },
            expiresAtMs = prefs.getLong(KEY_EXPIRES_AT, 0L)
        )
    }

    fun saveTokens(tokens: SkolaOnlineTokenSet) {
        putString(KEY_ACCESS_TOKEN, tokens.accessToken)
        putString(KEY_REFRESH_TOKEN, tokens.refreshToken.orEmpty())
        prefs.edit().putLong(KEY_EXPIRES_AT, tokens.expiresAtMs).apply()
    }

    fun clearTokens() {
        prefs.edit().remove(KEY_EXPIRES_AT).apply()
        putString(KEY_ACCESS_TOKEN, "")
        putString(KEY_REFRESH_TOKEN, "")
    }

    fun saveSyncStatus(status: String, syncedAtMs: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putLong(KEY_LAST_SYNC_AT, syncedAtMs)
            .putString(KEY_LAST_SYNC_STATUS, status.take(80))
            .apply()
    }

    private fun putString(key: String, value: String) {
        prefs.edit().putString(key, encrypt(value)).apply()
    }

    private fun getString(key: String): String? {
        val encoded = prefs.getString(key, null) ?: return null
        if (encoded.isBlank()) return ""
        return decrypt(encoded).getOrNull()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(CIPHER)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return base64(iv) + ":" + base64(encrypted)
    }

    private fun decrypt(value: String): Result<String> = runCatching {
        val parts = value.split(':')
        require(parts.size == 2)
        val cipher = Cipher.getInstance(CIPHER)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, Base64.decode(parts[0], Base64.NO_WRAP)))
        String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun base64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private companion object {
        const val PREFS = "glyphhub_school_online_secure"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "glyphhub_school_online_v1"
        const val CIPHER = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val KEY_ENABLED = "enabled"
        const val KEY_SERVER = "server"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_LAST_SYNC_AT = "last_sync_at"
        const val KEY_LAST_SYNC_STATUS = "last_sync_status"
    }
}
