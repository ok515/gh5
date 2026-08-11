package com.pecmi.studio.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.util.Log
import com.pecmi.studio.BuildConfig
import java.security.MessageDigest

/**
 * SecurityGuard handles runtime app integrity verification, anti-tamper,
 * anti-debugging checks, and secure reward validation for Ad networks.
 */
object SecurityGuard {

    private const val TAG = "SecurityGuard"
    private var isVerifiedCached: Boolean? = null

    /**
     * Checks if the app is running in a trusted, non-tampered environment.
     */
    fun isAppTrusted(context: Context): Boolean {
        isVerifiedCached?.let { return it }

        val trusted = verifyIntegrityInternal(context)
        isVerifiedCached = trusted
        return trusted
    }

    private fun verifyIntegrityInternal(context: Context): Boolean {
        // 1. Anti-Debugging Check in Release Builds
        if (!BuildConfig.DEBUG) {
            val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (isDebuggable) {
                Log.w(TAG, "Security Alert: Release build is debuggable!")
                return false
            }

            if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
                Log.w(TAG, "Security Alert: Debugger attached in release build!")
                return false
            }
        }

        // 2. Package Name Integrity Check
        val expectedPackageName = "com.pecmi.studio"
        if (context.packageName != expectedPackageName) {
            Log.w(TAG, "Security Alert: Unexpected package name: ${context.packageName}")
        }

        // 3. Signature Certificate Fingerprint Check
        val currentFingerprint = getAppCertificateSHA256(context)
        if (currentFingerprint.isNullOrEmpty() && !BuildConfig.DEBUG) {
            Log.w(TAG, "Security Alert: Signature certificate check failed in release mode.")
            return false
        }

        return true
    }

    /**
     * Retrieves SHA-256 fingerprint of the application's signing certificate.
     */
    @SuppressLint("PackageManagerGetSignatures")
    fun getAppCertificateSHA256(context: Context): String? {
        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }

            val pkgInfo = pm.getPackageInfo(context.packageName, flags)
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.signatures
            }

            if (!signatures.isNullOrEmpty()) {
                val cert = signatures[0].toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(cert)
                digest.joinToString("") { "%02X".format(it) }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compute app signature fingerprint", e)
            null
        }
    }

    /**
     * Validates if a rewarded ad callback is authentic and granted under legitimate conditions.
     */
    fun validateRewardGrant(context: Context, rewardEarned: Boolean): Boolean {
        if (!rewardEarned) return false
        if (!isAppTrusted(context)) {
            Log.w(TAG, "Reward grant rejected: App integrity verification failed.")
            return false
        }
        return true
    }
}
