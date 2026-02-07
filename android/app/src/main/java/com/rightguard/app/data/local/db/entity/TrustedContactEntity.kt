package com.rightguard.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rightguard.app.domain.model.TrustedContact

@Entity(tableName = "trusted_contacts")
data class TrustedContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val relationship: String
) {
    fun toDomain() = TrustedContact(
        id = id,
        name = name,
        phoneNumber = phoneNumber,
        relationship = relationship
    )

    companion object {
        fun fromDomain(contact: TrustedContact) = TrustedContactEntity(
            id = contact.id,
            name = contact.name,
            phoneNumber = contact.phoneNumber,
            relationship = contact.relationship
        )
    }
}
