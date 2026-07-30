package com.oscan.android.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DocumentEntity::class, PageEntity::class, FolderEntity::class, DocumentFolderEntity::class],
    version = 2,
    exportSchema = true
)
abstract class OScanDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao

    companion object {
        @Volatile private var instance: OScanDatabase? = null

        fun getInstance(context: Context): OScanDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    OScanDatabase::class.java,
                    "oscan.db"
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN cropCorners TEXT")
            }
        }
    }
}
