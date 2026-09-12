package com.benatt.businesscards.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.benatt.businesscards.data.local.converter.VCardTypeConverters
import com.benatt.businesscards.data.local.dao.VCardDao
import com.benatt.businesscards.data.local.entity.VCardEntity

/**
 * Main Room database for Business Cards.
 */
@Database(
    entities = [VCardEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(VCardTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vCardDao(): VCardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "business_cards.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
