package com.rightguard.app.di

import android.content.Context
import com.rightguard.app.data.local.crypto.CryptoManager
import com.rightguard.app.data.local.crypto.DatabasePassphraseManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideDatabasePassphraseManager(context: Context): DatabasePassphraseManager =
        DatabasePassphraseManager(context)

    @Provides
    @Singleton
    fun provideCryptoManager(): CryptoManager = CryptoManager()
}
