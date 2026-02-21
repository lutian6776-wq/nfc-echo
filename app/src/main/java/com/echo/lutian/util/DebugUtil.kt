package com.echo.lutian.util

import android.content.Context
import android.util.Log

/**
 * 调试工具 - 用于排查用户识别问题
 */
object DebugUtil {

    /**
     * 打印当前设备信息和用户状态
     */
    fun logUserDebugInfo(context: Context, currentUser: com.echo.lutian.data.entity.User?) {
        val deviceId = DeviceIdUtil.getDeviceId(context)
        val deviceModel = DeviceIdUtil.getDeviceModel()

        Log.d("UserDebug", "=== 用户调试信息 ===")
        Log.d("UserDebug", "设备ID: $deviceId")
        Log.d("UserDebug", "设备型号: $deviceModel")
        Log.d("UserDebug", "当前用户: ${currentUser?.name ?: "未识别"}")
        Log.d("UserDebug", "用户ID: ${currentUser?.userId ?: "N/A"}")
        Log.d("UserDebug", "用户角色: ${currentUser?.role ?: "N/A"}")
        Log.d("UserDebug", "是否管理员: ${currentUser?.role == "admin"}")
        Log.d("UserDebug", "==================")
    }
}
