package com.echo.lutian.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

/**
 * 设备识别工具类
 */
object DeviceIdUtil {

    /**
     * 获取设备唯一标识符
     * 使用 Android ID 作为设备标识（适合老人机场景，无需权限）
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }

    /**
     * 获取设备型号信息（用于显示）
     */
    fun getDeviceModel(): String {
        val manufacturer = android.os.Build.MANUFACTURER
        val model = android.os.Build.MODEL
        return if (model.startsWith(manufacturer)) {
            model
        } else {
            "$manufacturer $model"
        }
    }
}
