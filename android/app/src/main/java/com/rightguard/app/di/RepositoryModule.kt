package com.rightguard.app.di

import android.content.Context
import com.rightguard.app.data.local.db.dao.IncidentDao
import com.rightguard.app.data.local.db.dao.RecordingDao
import com.rightguard.app.data.local.db.dao.TrustedContactDao
import com.rightguard.app.data.local.db.dao.UserProfileDao
import com.rightguard.app.data.remote.api.RightGuardApiService
import com.rightguard.app.data.repository.IncidentRepository
import com.rightguard.app.data.repository.LegalAdviceRepository
import com.rightguard.app.data.repository.RecordingRepository
import com.rightguard.app.data.repository.TrustedContactRepository
import com.rightguard.app.data.repository.UserProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideUserProfileRepository(dao: UserProfileDao): UserProfileRepository =
        UserProfileRepository(dao)

    @Provides
    @Singleton
    fun provideTrustedContactRepository(dao: TrustedContactDao): TrustedContactRepository =
        TrustedContactRepository(dao)

    @Provides
    @Singleton
    fun provideIncidentRepository(dao: IncidentDao): IncidentRepository =
        IncidentRepository(dao)

    @Provides
    @Singleton
    fun provideRecordingRepository(dao: RecordingDao): RecordingRepository =
        RecordingRepository(dao)

    @Provides
    @Singleton
    fun provideLegalAdviceRepository(
        apiService: RightGuardApiService,
        context: Context
    ): LegalAdviceRepository = LegalAdviceRepository(apiService, context)
}
