package com.rightguard.app.di

import android.content.Context
import androidx.room.Room
import com.rightguard.app.data.local.crypto.DatabasePassphraseManager
import com.rightguard.app.data.local.db.RightGuardDatabase
import com.rightguard.app.data.local.db.dao.IncidentDao
import com.rightguard.app.data.local.db.dao.RecordingDao
import com.rightguard.app.data.local.db.dao.TrustedContactDao
import com.rightguard.app.data.local.db.dao.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        context: Context,
        passphraseManager: DatabasePassphraseManager
    ): RightGuardDatabase {
        val passphrase = passphraseManager.getPassphrase()
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            RightGuardDatabase::class.java,
            "rightguard.db"
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUserProfileDao(db: RightGuardDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    fun provideTrustedContactDao(db: RightGuardDatabase): TrustedContactDao = db.trustedContactDao()

    @Provides
    fun provideIncidentDao(db: RightGuardDatabase): IncidentDao = db.incidentDao()

    @Provides
    fun provideRecordingDao(db: RightGuardDatabase): RecordingDao = db.recordingDao()
}
