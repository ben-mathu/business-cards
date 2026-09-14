package com.benatt.businesscards.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.benatt.businesscards.data.dto.AddressDto
import com.benatt.businesscards.data.dto.EmailDto
import com.benatt.businesscards.data.dto.GeoLocationDto
import com.benatt.businesscards.data.dto.OrganizationDto
import com.benatt.businesscards.data.dto.PhoneDto
import com.benatt.businesscards.data.dto.SocialProfileDto
import com.benatt.businesscards.data.dto.VCardDto
import com.benatt.businesscards.data.dto.VCardNameDto
import com.benatt.businesscards.data.dto.WebDto
import java.util.UUID

/**
 * Room Entity representing a stored vCard / business card in SQLite.
 * Includes indexed columns for fast contact search and query operations,
 * as well as full structured contact details.
 */
@Entity(
    tableName = "vcards",
    indices = [
        Index(value = ["formattedName"]),
        Index(value = ["primaryPhone"]),
        Index(value = ["primaryEmail"]),
        Index(value = ["organization"])
    ]
)
data class VCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val uid: String = UUID.randomUUID().toString(),
    val version: String = "3.0",
    val formattedName: String = "",
    val familyName: String? = null,
    val givenName: String? = null,
    val middleName: String? = null,
    val prefix: String? = null,
    val suffix: String? = null,
    val nickname: String? = null,
    val organization: String? = null,
    val department: String? = null,
    val unit: String? = null,
    val title: String? = null,
    val role: String? = null,
    val primaryPhone: String? = null,
    val primaryEmail: String? = null,
    val primaryAddress: String? = null,
    val photoUri: String? = null,
    val logoUri: String? = null,
    val birthday: String? = null,
    val anniversary: String? = null,
    val note: String? = null,
    val gender: String? = null,
    val timezone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val revision: String? = null,
    val rawVcf: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Structured collections mapped via Room TypeConverters
    val phones: List<PhoneDto> = emptyList(),
    val emails: List<EmailDto> = emptyList(),
    val addresses: List<AddressDto> = emptyList(),
    val websites: List<WebDto> = emptyList(),
    val socialProfiles: List<SocialProfileDto> = emptyList(),
    val categories: List<String> = emptyList(),
    val customFields: Map<String, String> = emptyMap()
) {
    /**
     * Converts this database entity back into a clean [VCardDto].
     */
    fun toDto(): VCardDto {
        return VCardDto(
            id = id,
            uid = uid,
            version = version,
            formattedName = formattedName,
            name = if (familyName != null || givenName != null || middleName != null || prefix != null || suffix != null) {
                VCardNameDto(
                    familyName = familyName,
                    givenName = givenName,
                    middleName = middleName,
                    prefix = prefix,
                    suffix = suffix
                )
            } else null,
            nickname = nickname,
            organization = organization?.let {
                OrganizationDto(
                    name = it,
                    department = department,
                    unit = unit
                )
            },
            title = title,
            role = role,
            phones = phones,
            emails = emails,
            addresses = addresses,
            websites = websites,
            socialProfiles = socialProfiles,
            note = note,
            birthday = birthday,
            anniversary = anniversary,
            photoUri = photoUri,
            logoUri = logoUri,
            gender = gender,
            timezone = timezone,
            geo = if (latitude != null && longitude != null) {
                GeoLocationDto(latitude = latitude, longitude = longitude)
            } else null,
            categories = categories,
            revision = revision,
            customFields = customFields
        )
    }

    companion object {
        /**
         * Creates a [VCardEntity] from a [VCardDto], automatically extracting
         * primary search fields and preserving the raw .vcf payload if available.
         */
        fun fromDto(dto: VCardDto, rawVcf: String? = null): VCardEntity {
            val now = System.currentTimeMillis()
            return VCardEntity(
                uid = dto.uid?.ifBlank { null } ?: UUID.randomUUID().toString(),
                version = dto.version,
                formattedName = dto.displayName,
                familyName = dto.name?.familyName,
                givenName = dto.name?.givenName,
                middleName = dto.name?.middleName,
                prefix = dto.name?.prefix,
                suffix = dto.name?.suffix,
                nickname = dto.nickname,
                organization = dto.organization?.name,
                department = dto.organization?.department,
                unit = dto.organization?.unit,
                title = dto.title,
                role = dto.role,
                primaryPhone = dto.primaryPhone?.number,
                primaryEmail = dto.primaryEmail?.address,
                primaryAddress = dto.primaryAddress?.formattedAddress(),
                photoUri = dto.photoUri,
                logoUri = dto.logoUri,
                birthday = dto.birthday,
                anniversary = dto.anniversary,
                note = dto.note,
                gender = dto.gender,
                timezone = dto.timezone,
                latitude = dto.geo?.latitude,
                longitude = dto.geo?.longitude,
                revision = dto.revision,
                rawVcf = rawVcf,
                createdAt = now,
                updatedAt = now,
                phones = dto.phones,
                emails = dto.emails,
                addresses = dto.addresses,
                websites = dto.websites,
                socialProfiles = dto.socialProfiles,
                categories = dto.categories,
                customFields = dto.customFields
            )
        }
    }
}
