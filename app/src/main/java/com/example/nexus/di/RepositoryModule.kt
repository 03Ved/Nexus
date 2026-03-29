package com.example.nexus.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * RepositoryModule
 * Repositories are injected directly via @Inject constructor —
 * no manual @Provides needed. Hilt handles them automatically.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule