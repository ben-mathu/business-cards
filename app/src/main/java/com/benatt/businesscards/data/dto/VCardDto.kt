package com.benatt.businesscards.data.dto

import java.io.Serializable

/**
 * Data Transfer Object representing a vCard (electronic business card) based on RFC 2426 / RFC 6350.
 */
data class VCardDto(
    val uid: String? = null,
    val version: String = "3.0",
    val formattedName: String = "",
    val name: VCardNameDto? = null,
    val nickname: String? = null,
    val organization: OrganizationDto? = null,
    val title: String? = null,
    val role: String? = null,
    val phones: List<PhoneDto> = emptyList(),
    val emails: List<EmailDto> = emptyList(),
    val addresses: List<AddressDto> = emptyList(),
    val websites: List<WebDto> = emptyList(),
    val socialProfiles: List<SocialProfileDto> = emptyList(),
    val note: String? = null,
    val birthday: String? = null,
    val anniversary: String? = null,
    val photoUri: String? = null,
    val logoUri: String? = null,
    val gender: String? = null,
    val timezone: String? = null,
    val geo: GeoLocationDto? = null,
    val categories: List<String> = emptyList(),
    val revision: String? = null,
    val customFields: Map<String, String> = emptyMap()
) : Serializable {

    val displayName: String
        get() = formattedName.ifBlank {
            name?.toFormattedName()?.takeIf { it.isNotBlank() }
                ?: organization?.name?.takeIf { it.isNotBlank() }
                ?: primaryEmail?.address
                ?: primaryPhone?.number
                ?: ""
        }

    val primaryPhone: PhoneDto?
        get() = phones.find { it.isPrimary } ?: phones.firstOrNull()

    val primaryEmail: EmailDto?
        get() = emails.find { it.isPrimary } ?: emails.firstOrNull()

    val primaryAddress: AddressDto?
        get() = addresses.find { it.isPrimary } ?: addresses.firstOrNull()

    val primaryWebsite: WebDto?
        get() = websites.firstOrNull()

    val companyName: String?
        get() = organization?.name
}

/**
 * Structured name components corresponding to the 'N' property in vCard.
 */
data class VCardNameDto(
    val familyName: String? = null,
    val givenName: String? = null,
    val middleName: String? = null,
    val prefix: String? = null,
    val suffix: String? = null
) : Serializable {

    fun toFormattedName(): String {
        return listOfNotNull(
            prefix?.takeIf { it.isNotBlank() },
            givenName?.takeIf { it.isNotBlank() },
            middleName?.takeIf { it.isNotBlank() },
            familyName?.takeIf { it.isNotBlank() },
            suffix?.takeIf { it.isNotBlank() }
        ).joinToString(" ").trim()
    }
}

/**
 * Organization / company information corresponding to 'ORG'.
 */
data class OrganizationDto(
    val name: String,
    val department: String? = null,
    val unit: String? = null
) : Serializable

/**
 * Telephone entry corresponding to 'TEL'.
 */
data class PhoneDto(
    val number: String,
    val type: PhoneType = PhoneType.CELL,
    val isPrimary: Boolean = false,
    val customLabel: String? = null
) : Serializable

enum class PhoneType {
    CELL,
    WORK,
    HOME,
    MAIN,
    WORK_FAX,
    HOME_FAX,
    PAGER,
    OTHER,
    CUSTOM
}

/**
 * Email entry corresponding to 'EMAIL'.
 */
data class EmailDto(
    val address: String,
    val type: EmailType = EmailType.WORK,
    val isPrimary: Boolean = false,
    val customLabel: String? = null
) : Serializable

enum class EmailType {
    WORK,
    HOME,
    OTHER,
    CUSTOM
}

/**
 * Postal / physical address corresponding to 'ADR'.
 */
data class AddressDto(
    val street: String? = null,
    val extendedAddress: String? = null,
    val locality: String? = null,
    val region: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val postOfficeBox: String? = null,
    val type: AddressType = AddressType.WORK,
    val isPrimary: Boolean = false,
    val customLabel: String? = null
) : Serializable {

    fun formattedAddress(multiline: Boolean = false): String {
        val parts = mutableListOf<String>()

        val streetPart = listOfNotNull(
            street?.takeIf { it.isNotBlank() },
            extendedAddress?.takeIf { it.isNotBlank() }
        ).joinToString(", ")
        if (streetPart.isNotBlank()) parts.add(streetPart)

        val cityStateZip = listOfNotNull(
            locality?.takeIf { it.isNotBlank() },
            region?.takeIf { it.isNotBlank() },
            postalCode?.takeIf { it.isNotBlank() }
        ).joinToString(" ")
        if (cityStateZip.isNotBlank()) parts.add(cityStateZip)

        country?.takeIf { it.isNotBlank() }?.let { parts.add(it) }

        return parts.joinToString(if (multiline) "\n" else ", ")
    }
}

enum class AddressType {
    WORK,
    HOME,
    POSTAL,
    OTHER,
    CUSTOM
}

/**
 * Web URL corresponding to 'URL'.
 */
data class WebDto(
    val url: String,
    val type: WebType = WebType.WORK,
    val customLabel: String? = null
) : Serializable

enum class WebType {
    WORK,
    PERSONAL,
    PORTFOLIO,
    BLOG,
    OTHER,
    CUSTOM
}

/**
 * Social media and messaging profiles.
 */
data class SocialProfileDto(
    val platform: SocialPlatform = SocialPlatform.OTHER,
    val username: String,
    val url: String? = null,
    val customPlatform: String? = null
) : Serializable

enum class SocialPlatform(val displayName: String, val baseUrl: String? = null) {
    LINKEDIN("LinkedIn", "https://linkedin.com/in/"),
    GITHUB("GitHub", "https://github.com/"),
    TWITTER("X (Twitter)", "https://x.com/"),
    INSTAGRAM("Instagram", "https://instagram.com/"),
    FACEBOOK("Facebook", "https://facebook.com/"),
    YOUTUBE("YouTube", "https://youtube.com/@"),
    TELEGRAM("Telegram", "https://t.me/"),
    WHATSAPP("WhatsApp", "https://wa.me/"),
    SIGNAL("Signal", null),
    DISCORD("Discord", null),
    TIKTOK("TikTok", "https://tiktok.com/@"),
    OTHER("Other", null);

    fun buildFullUrl(username: String): String {
        return if (baseUrl != null && !username.startsWith("http")) {
            val cleanUsername = username.removePrefix("@")
            "$baseUrl$cleanUsername"
        } else {
            username
        }
    }
}

/**
 * Geographical coordinates corresponding to 'GEO'.
 */
data class GeoLocationDto(
    val latitude: Double,
    val longitude: Double
) : Serializable
