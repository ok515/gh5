package com.pecmi.studio.services

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

object ServiceDetector {

    private const val TAG = "ServiceDetector"
    private var cachedServiceType: ServiceType? = null

    /**
     * Automatically determines whether Google Mobile Services (GMS),
     * Huawei Mobile Services (HMS), or generic Android services are available.
     */
    fun getServiceType(context: Context): ServiceType {
        cachedServiceType?.let { return it }

        val type = when {
            isGmsAvailable(context) -> {
                Log.d(TAG, "Detected Google Mobile Services (GMS)")
                ServiceType.GMS
            }
            isHmsAvailable(context) -> {
                Log.d(TAG, "Detected Huawei Mobile Services (HMS)")
                ServiceType.HMS
            }
            else -> {
                Log.d(TAG, "Detected Generic/Fallback Android environment")
                ServiceType.GENERIC
            }
        }

        cachedServiceType = type
        return type
    }

    /**
     * Checks if Google Play Services are installed and functional.
     */
    fun isGmsAvailable(context: Context): Boolean {
        return try {
            val availability = GoogleApiAvailability.getInstance()
            val result = availability.isGooglePlayServicesAvailable(context)
            result == ConnectionResult.SUCCESS
        } catch (e: Exception) {
            Log.w(TAG, "GMS check failed: ${e.message}")
            false
        }
    }

    /**
     * Checks if Huawei Mobile Services (HMS Core) are installed and functional.
     */
    fun isHmsAvailable(context: Context): Boolean {
        return try {
            // Try checking via HMS SDK if present
            val huaweiApiAvailabilityClass = Class.forName("com.huawei.hms.api.HuaweiApiAvailability")
            val getInstanceMethod = huaweiApiAvailabilityClass.getMethod("getInstance")
            val instance = getInstanceMethod.invoke(null)
            val isHmsAvailableMethod = huaweiApiAvailabilityClass.getMethod("isHuaweiMobileServicesAvailable", Context::class.java)
            val result = isHmsAvailableMethod.invoke(instance, context) as Int
            result == 0 // 0 = SUCCESS in HMS SDK
        } catch (e: Exception) {
            // Fallback: Check if Huawei HWID package exists on device
            try {
                val packageManager = context.packageManager
                packageManager.getPackageInfo("com.huawei.hwid", 0)
                true
            } catch (pme: Exception) {
                false
            }
        }
    }
}
