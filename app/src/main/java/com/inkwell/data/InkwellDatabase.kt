package com.inkwell.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DocumentEntity::class, DocumentContentEntity::class, DocumentNoteEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class InkwellDatabase : RoomDatabase() {
    abstract fun dao(): InkwellDao

    companion object {
        @Volatile
        private var instance: InkwellDatabase? = null

        fun getInstance(context: Context): InkwellDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    InkwellDatabase::class.java,
                    "inkwell.db",
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}
