package com.dentia.patient.data.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.dentia.patient.data.model.AuthUser
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var accessToken: String?
        get() = readEncrypted(KEY_ACCESS_TOKEN)
        set(value) = writeEncrypted(KEY_ACCESS_TOKEN, value)

    var refreshCookie: String?
        get() = readEncrypted(KEY_REFRESH_COOKIE)
        set(value) = writeEncrypted(KEY_REFRESH_COOKIE, value)

    var user: AuthUser?
        get() = readEncrypted(KEY_USER)?.let {
            runCatching { AuthUser.fromJson(JSONObject(it)) }.getOrNull()
        }
        set(value) = writeEncrypted(KEY_USER, value?.toJson()?.toString())

    fun saveSession(token: String, user: AuthUser) {
        accessToken = token
        this.user = user
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun writeEncrypted(key: String, value: String?) {
        if (value == null) {
            preferences.edit().remove(key).apply()
            return
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val payload = ByteArray(cipher.iv.size + encrypted.size)
        cipher.iv.copyInto(payload)
        encrypted.copyInto(payload, cipher.iv.size)

        preferences.edit()
            .putString(key, Base64.encodeToString(payload, Base64.NO_WRAP))
            .apply()
    }

    private fun readEncrypted(key: String): String? {
        val encoded = preferences.getString(key, null) ?: return null

        return runCatching {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = payload.copyOfRange(0, IV_SIZE)
            val encrypted = payload.copyOfRange(IV_SIZE, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(TAG_LENGTH_BITS, iv),
            )
            cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        ).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "dentia_secure_session"
        const val KEY_ALIAS = "dentia_session_key"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_COOKIE = "refresh_cookie"
        const val KEY_USER = "user"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_LENGTH_BITS = 128
    }
}

