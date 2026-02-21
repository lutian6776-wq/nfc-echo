package com.echo.lutian.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.echo.lutian.data.dao.AudioRecordDao
import com.echo.lutian.data.dao.UserDao
import com.echo.lutian.data.entity.AudioRecord
import com.echo.lutian.data.entity.User

/**
 * HeartEcho 数据库
 */
@Database(
    entities = [AudioRecord::class, User::class],
    version = 3,
    exportSchema = false
)
abstract class HeartEchoDatabase : RoomDatabase() {

    abstract fun audioRecordDao(): AudioRecordDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: HeartEchoDatabase? = null

        // 数据库迁移：版本 1 -> 2（添加云端同步字段）
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE audio_records ADD COLUMN cloudId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE audio_records ADD COLUMN cloudUrl TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE audio_records ADD COLUMN isUploaded INTEGER NOT NULL DEFAULT 0")
            }
        }

        // 数据库迁移：版本 2 -> 3（添加用户表和用户隔离字段）
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建用户表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        userId TEXT PRIMARY KEY NOT NULL,
                        deviceId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        nfcTagId TEXT,
                        role TEXT NOT NULL DEFAULT 'user',
                        createdAt INTEGER NOT NULL,
                        lastActiveAt INTEGER NOT NULL,
                        isCurrentUser INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // 为 audio_records 添加用户隔离字段
                db.execSQL("ALTER TABLE audio_records ADD COLUMN senderId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE audio_records ADD COLUMN receiverId TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): HeartEchoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HeartEchoDatabase::class.java,
                    "heartecho_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
