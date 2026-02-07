package com.rightguard.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rightguard.app.data.local.db.entity.TrustedContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: TrustedContactEntity): Long

    @Delete
    suspend fun delete(contact: TrustedContactEntity)

    @Query("SELECT * FROM trusted_contacts")
    suspend fun getAll(): List<TrustedContactEntity>

    @Query("SELECT * FROM trusted_contacts")
    fun observeAll(): Flow<List<TrustedContactEntity>>

    @Query("SELECT * FROM trusted_contacts WHERE id = :id")
    suspend fun getById(id: Long): TrustedContactEntity?

    @Query("DELETE FROM trusted_contacts")
    suspend fun deleteAll()
}
