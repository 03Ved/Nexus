package com.example.nexus.di

import android.content.Context
import androidx.room.Room
import com.example.nexus.data.local.NexusDatabase
import com.example.nexus.data.local.dao.DraftDao
import com.example.nexus.data.local.dao.MessageDao
import com.example.nexus.data.local.dao.PostDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.example.nexus.data.local.dao.NotificationDao

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NexusDatabase {
        return Room.databaseBuilder(
            context,
            NexusDatabase::class.java,
            NexusDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun providePostDao(database: NexusDatabase): PostDao {
        return database.postDao()
    }

    @Provides
    fun provideDraftDao(database: NexusDatabase): DraftDao {
        return database.draftDao()
    }

    @Provides
    fun provideMessageDao(database: NexusDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    fun provideNotificationDao(database: NexusDatabase): NotificationDao {
        return database.notificationDao()
    }
}