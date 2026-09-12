package com.benatt.businesscards.di

import android.app.Application
import com.benatt.businesscards.data.local.AppDatabase
import com.benatt.businesscards.data.local.dao.VCardDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * @author ben-mathu
 * 9/12/26
 */
@Module
@InstallIn(SingletonComponent::class)
class DbModule {
    @Provides
    fun providesDatabase(application: Application): AppDatabase {
        return AppDatabase.getInstance(application.applicationContext)
    }

    @Provides
    fun providesVCardDao(database: AppDatabase): VCardDao {
        return database.vCardDao()
    }
}