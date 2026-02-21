package com.echo.lutian.data.dao

import androidx.room.*
import com.echo.lutian.data.entity.AudioRecord
import kotlinx.coroutines.flow.Flow

/**
 * 音频记录 DAO
 */
@Dao
interface AudioRecordDao {

    @Query("SELECT * FROM audio_records ORDER BY createdAt DESC")
    fun getAllRecords(): Flow<List<AudioRecord>>

    @Query("SELECT * FROM audio_records WHERE id = :id")
    suspend fun getRecordById(id: Long): AudioRecord?

    @Query("SELECT * FROM audio_records WHERE nfcTagId = :tagId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestRecordByTagId(tagId: String): AudioRecord?

    @Query("SELECT * FROM audio_records WHERE isPlayed = 0 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestUnplayedRecord(): AudioRecord?

    @Insert
    suspend fun insertRecord(record: AudioRecord): Long

    @Update
    suspend fun updateRecord(record: AudioRecord)

    @Delete
    suspend fun deleteRecord(record: AudioRecord)

    @Query("UPDATE audio_records SET isPlayed = 1 WHERE id = :id")
    suspend fun markAsPlayed(id: Long)

    // 云端同步相关查询

    @Query("SELECT * FROM audio_records WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getRecordByCloudId(cloudId: String): AudioRecord?

    @Query("UPDATE audio_records SET cloudId = :cloudId, cloudUrl = :cloudUrl, isUploaded = 1 WHERE id = :id")
    suspend fun updateCloudInfo(id: Long, cloudId: String, cloudUrl: String)

    @Query("SELECT * FROM audio_records WHERE isUploaded = 0 ORDER BY createdAt DESC")
    suspend fun getUnuploadedRecords(): List<AudioRecord>
}
