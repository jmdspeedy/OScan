package com.oscan.android.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DocumentEntity::class, PageEntity::class, FolderEntity::class, DocumentFolderEntity::class],
    version = 1,
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
                ).build().also { instance = it }
            }
    }
}
