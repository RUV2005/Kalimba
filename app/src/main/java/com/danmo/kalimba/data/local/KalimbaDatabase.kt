// 文件名: KalimbaDatabase.kt
package com.danmo.kalimba.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 卡林巴琴本地数据库
 */
@Database(
    entities = [SheetMusicEntity::class],
    version = 1,
    exportSchema = true
)
abstract class KalimbaDatabase : RoomDatabase() {

    abstract fun sheetMusicDao(): SheetMusicDao

    companion object {
        @Volatile
        private var INSTANCE: KalimbaDatabase? = null

        fun getDatabase(context: Context): KalimbaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KalimbaDatabase::class.java,
                    "kalimba_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // 未来版本升级时的迁移示例
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加新列示例
                // database.execSQL("ALTER TABLE sheet_music ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}