package com.rightguard.app.data.repository

import com.rightguard.app.data.local.db.dao.UserProfileDao
import com.rightguard.app.data.local.db.entity.UserProfileEntity
import com.rightguard.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    private val dao: UserProfileDao
) {
    suspend fun saveProfile(profile: UserProfile): Long {
        return dao.insert(UserProfileEntity.fromDomain(profile))
    }

    suspend fun updateProfile(profile: UserProfile) {
        dao.update(UserProfileEntity.fromDomain(profile))
    }

    suspend fun getProfile(): UserProfile? {
        return dao.getProfile()?.toDomain()
    }

    fun observeProfile(): Flow<UserProfile?> {
        return dao.observeProfile().map { it?.toDomain() }
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}
