package com.rightguard.app.data.repository

import com.rightguard.app.data.local.db.dao.TrustedContactDao
import com.rightguard.app.data.local.db.entity.TrustedContactEntity
import com.rightguard.app.domain.model.TrustedContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrustedContactRepository @Inject constructor(
    private val dao: TrustedContactDao
) {
    suspend fun addContact(contact: TrustedContact): Long {
        return dao.insert(TrustedContactEntity.fromDomain(contact))
    }

    suspend fun removeContact(contact: TrustedContact) {
        dao.delete(TrustedContactEntity.fromDomain(contact))
    }

    suspend fun getAll(): List<TrustedContact> {
        return dao.getAll().map { it.toDomain() }
    }

    fun observeAll(): Flow<List<TrustedContact>> {
        return dao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getById(id: Long): TrustedContact? {
        return dao.getById(id)?.toDomain()
    }
}
