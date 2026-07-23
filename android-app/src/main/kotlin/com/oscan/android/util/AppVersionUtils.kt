package com.oscan.android.util

import android.content.Context
import com.oscan.android.BuildConfig

/**
 * Utility for accessing application version information dynamically.
 */
object AppVersionUtils {
    /**
     * Returns the app versionName (e.g., "0.5.0").
     */
    fun getVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: BuildConfig.VERSION_NAME
        } catch (_: Exception) {
            try {
                BuildConfig.VERSION_NAME
            } catch (_: Exception) {
                "0.5.0"
            }
        }
    }

    /**
     * Returns the app versionCode (e.g., 5).
     */
    fun getVersionCode(context: Context): Long {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (_: Exception) {
            try {
                BuildConfig.VERSION_CODE.toLong()
            } catch (_: Exception) {
                5L
            }
        }
    }
}
