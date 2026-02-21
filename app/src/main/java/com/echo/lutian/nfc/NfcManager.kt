package com.echo.lutian.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.util.Log

class NfcManager {

    companion object {
        private const val TAG = "NfcManager"
        private const val HEARTECHO_SCHEME = "heartecho://action/"
    }

    /**
     * 解析 NDEF 消息
     * @param tag NFC 标签
     * @return 解析后的动作字符串，如果解析失败返回 null
     */
    fun parseNdefMessage(tag: Tag): String? {
        try {
            val ndef = android.nfc.tech.Ndef.get(tag) ?: return null
            ndef.connect()
            val message = ndef.ndefMessage
            ndef.close()

            return message?.let { parseNdefRecords(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing NDEF message", e)
            return null
        }
    }

    /**
     * 从 NDEF 记录中提取 heartecho 协议的动作
     */
    private fun parseNdefRecords(message: NdefMessage): String? {
        for (record in message.records) {
            when (record.tnf) {
                NdefRecord.TNF_WELL_KNOWN -> {
                    if (record.type.contentEquals(NdefRecord.RTD_URI)) {
                        val uri = parseUriRecord(record)
                        if (uri?.startsWith(HEARTECHO_SCHEME) == true) {
                            return uri.removePrefix(HEARTECHO_SCHEME)
                        }
                    }
                }
                NdefRecord.TNF_ABSOLUTE_URI, NdefRecord.TNF_EXTERNAL_TYPE -> {
                    val payload = String(record.payload)
                    if (payload.startsWith(HEARTECHO_SCHEME)) {
                        return payload.removePrefix(HEARTECHO_SCHEME)
                    }
                }
            }
        }
        return null
    }

    /**
     * 解析 URI 记录
     */
    private fun parseUriRecord(record: NdefRecord): String? {
        return try {
            val payload = record.payload
            if (payload.isEmpty()) return null

            // URI 记录的第一个字节是 URI 标识符代码
            val uriIdentifierCode = payload[0].toInt() and 0xFF
            val uriPrefix = getUriPrefix(uriIdentifierCode)
            val uriBody = String(payload, 1, payload.size - 1)

            uriPrefix + uriBody
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing URI record", e)
            null
        }
    }

    /**
     * 根据 URI 标识符代码获取前缀
     */
    private fun getUriPrefix(code: Int): String {
        return when (code) {
            0x00 -> ""
            0x01 -> "http://www."
            0x02 -> "https://www."
            0x03 -> "http://"
            0x04 -> "https://"
            else -> ""
        }
    }

    /**
     * 创建 NDEF 消息用于写入标签
     * @param action 动作字符串
     */
    fun createNdefMessage(action: String): NdefMessage {
        val uri = "$HEARTECHO_SCHEME$action"
        val record = NdefRecord.createUri(uri)
        return NdefMessage(arrayOf(record))
    }
}
