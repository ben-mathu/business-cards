package com.benatt.businesscards.data.local.converter

import androidx.room.TypeConverter
import com.benatt.businesscards.data.dto.AddressDto
import com.benatt.businesscards.data.dto.AddressType
import com.benatt.businesscards.data.dto.EmailDto
import com.benatt.businesscards.data.dto.EmailType
import com.benatt.businesscards.data.dto.PhoneDto
import com.benatt.businesscards.data.dto.PhoneType
import com.benatt.businesscards.data.dto.SocialPlatform
import com.benatt.businesscards.data.dto.SocialProfileDto
import com.benatt.businesscards.data.dto.WebDto
import com.benatt.businesscards.data.dto.WebType

/**
 * Type converters for Room to serialize and deserialize complex vCard collection fields.
 * Uses ASCII Record/Unit delimiters (\u001E and \u001F) for high performance and zero external dependencies.
 */
class VCardTypeConverters {

    companion object {
        private const val RECORD_SEP = "\u001E"
        private const val UNIT_SEP = "\u001F"
    }

    // PhoneDto list
    @TypeConverter
    fun fromPhoneList(phones: List<PhoneDto>?): String {
        if (phones.isNullOrEmpty()) return ""
        return phones.joinToString(RECORD_SEP) { phone ->
            listOf(
                phone.number,
                phone.type.name,
                phone.isPrimary.toString(),
                phone.customLabel.orEmpty()
            ).joinToString(UNIT_SEP)
        }
    }

    @TypeConverter
    fun toPhoneList(data: String?): List<PhoneDto> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(RECORD_SEP).mapNotNull { item ->
            val parts = item.split(UNIT_SEP)
            if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                val type = runCatching { PhoneType.valueOf(parts.getOrElse(1) { "CELL" }) }
                    .getOrDefault(PhoneType.CELL)
                PhoneDto(
                    number = parts[0],
                    type = type,
                    isPrimary = parts.getOrNull(2)?.toBooleanStrictOrNull() ?: false,
                    customLabel = parts.getOrNull(3)?.takeIf { it.isNotBlank() }
                )
            } else null
        }
    }

    // EmailDto list
    @TypeConverter
    fun fromEmailList(emails: List<EmailDto>?): String {
        if (emails.isNullOrEmpty()) return ""
        return emails.joinToString(RECORD_SEP) { email ->
            listOf(
                email.address,
                email.type.name,
                email.isPrimary.toString(),
                email.customLabel.orEmpty()
            ).joinToString(UNIT_SEP)
        }
    }

    @TypeConverter
    fun toEmailList(data: String?): List<EmailDto> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(RECORD_SEP).mapNotNull { item ->
            val parts = item.split(UNIT_SEP)
            if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                val type = runCatching { EmailType.valueOf(parts.getOrElse(1) { "WORK" }) }
                    .getOrDefault(EmailType.WORK)
                EmailDto(
                    address = parts[0],
                    type = type,
                    isPrimary = parts.getOrNull(2)?.toBooleanStrictOrNull() ?: false,
                    customLabel = parts.getOrNull(3)?.takeIf { it.isNotBlank() }
                )
            } else null
        }
    }

    // AddressDto list
    @TypeConverter
    fun fromAddressList(addresses: List<AddressDto>?): String {
        if (addresses.isNullOrEmpty()) return ""
        return addresses.joinToString(RECORD_SEP) { addr ->
            listOf(
                addr.street.orEmpty(),
                addr.extendedAddress.orEmpty(),
                addr.locality.orEmpty(),
                addr.region.orEmpty(),
                addr.postalCode.orEmpty(),
                addr.country.orEmpty(),
                addr.postOfficeBox.orEmpty(),
                addr.type.name,
                addr.isPrimary.toString(),
                addr.customLabel.orEmpty()
            ).joinToString(UNIT_SEP)
        }
    }

    @TypeConverter
    fun toAddressList(data: String?): List<AddressDto> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(RECORD_SEP).mapNotNull { item ->
            val parts = item.split(UNIT_SEP)
            if (parts.size >= 8) {
                val type = runCatching { AddressType.valueOf(parts[7]) }
                    .getOrDefault(AddressType.WORK)
                AddressDto(
                    street = parts[0].takeIf { it.isNotBlank() },
                    extendedAddress = parts[1].takeIf { it.isNotBlank() },
                    locality = parts[2].takeIf { it.isNotBlank() },
                    region = parts[3].takeIf { it.isNotBlank() },
                    postalCode = parts[4].takeIf { it.isNotBlank() },
                    country = parts[5].takeIf { it.isNotBlank() },
                    postOfficeBox = parts[6].takeIf { it.isNotBlank() },
                    type = type,
                    isPrimary = parts.getOrNull(8)?.toBooleanStrictOrNull() ?: false,
                    customLabel = parts.getOrNull(9)?.takeIf { it.isNotBlank() }
                )
            } else null
        }
    }

    // WebDto list
    @TypeConverter
    fun fromWebList(websites: List<WebDto>?): String {
        if (websites.isNullOrEmpty()) return ""
        return websites.joinToString(RECORD_SEP) { web ->
            listOf(
                web.url,
                web.type.name,
                web.customLabel.orEmpty()
            ).joinToString(UNIT_SEP)
        }
    }

    @TypeConverter
    fun toWebList(data: String?): List<WebDto> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(RECORD_SEP).mapNotNull { item ->
            val parts = item.split(UNIT_SEP)
            if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                val type = runCatching { WebType.valueOf(parts.getOrElse(1) { "WORK" }) }
                    .getOrDefault(WebType.WORK)
                WebDto(
                    url = parts[0],
                    type = type,
                    customLabel = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
                )
            } else null
        }
    }

    // SocialProfileDto list
    @TypeConverter
    fun fromSocialProfileList(profiles: List<SocialProfileDto>?): String {
        if (profiles.isNullOrEmpty()) return ""
        return profiles.joinToString(RECORD_SEP) { social ->
            listOf(
                social.platform.name,
                social.username,
                social.url.orEmpty(),
                social.customPlatform.orEmpty()
            ).joinToString(UNIT_SEP)
        }
    }

    @TypeConverter
    fun toSocialProfileList(data: String?): List<SocialProfileDto> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(RECORD_SEP).mapNotNull { item ->
            val parts = item.split(UNIT_SEP)
            if (parts.size >= 2 && parts[1].isNotBlank()) {
                val platform = runCatching { SocialPlatform.valueOf(parts[0]) }
                    .getOrDefault(SocialPlatform.OTHER)
                SocialProfileDto(
                    platform = platform,
                    username = parts[1],
                    url = parts.getOrNull(2)?.takeIf { it.isNotBlank() },
                    customPlatform = parts.getOrNull(3)?.takeIf { it.isNotBlank() }
                )
            } else null
        }
    }

    // String list (e.g. categories)
    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        if (list.isNullOrEmpty()) return ""
        return list.joinToString(RECORD_SEP)
    }

    @TypeConverter
    fun toStringList(data: String?): List<String> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(RECORD_SEP).filter { it.isNotBlank() }
    }

    // Map<String, String> (e.g. customFields)
    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String {
        if (map.isNullOrEmpty()) return ""
        return map.entries.joinToString(RECORD_SEP) { "${it.key}$UNIT_SEP${it.value}" }
    }

    @TypeConverter
    fun toStringMap(data: String?): Map<String, String> {
        if (data.isNullOrBlank()) return emptyMap()
        return data.split(RECORD_SEP).mapNotNull { entry ->
            val parts = entry.split(UNIT_SEP, limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank()) {
                parts[0] to parts[1]
            } else null
        }.toMap()
    }
}
