package com.benatt.businesscards.data.parser

import com.benatt.businesscards.data.dto.AddressType
import com.benatt.businesscards.data.dto.EmailType
import com.benatt.businesscards.data.dto.PhoneType
import com.benatt.businesscards.data.dto.VCardDto
import com.benatt.businesscards.data.dto.WebType

/**
 * Serializer for converting [VCardDto] into RFC-compliant vCard (.vcf) format.
 */
object VCardSerializer {

    fun serialize(card: VCardDto): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCARD")
        sb.appendLine("VERSION:${card.version.ifBlank { "3.0" }}")

        // Formatted Name (mandatory in vCard 3.0/4.0)
        val fn = card.displayName.ifBlank { "Contact" }
        sb.appendLine("FN:${escape(fn)}")

        // Structured Name (N)
        card.name?.let { n ->
            val family = escape(n.familyName.orEmpty())
            val given = escape(n.givenName.orEmpty())
            val middle = escape(n.middleName.orEmpty())
            val prefix = escape(n.prefix.orEmpty())
            val suffix = escape(n.suffix.orEmpty())
            sb.appendLine("N:$family;$given;$middle;$prefix;$suffix")
        }

        // Nickname
        card.nickname?.takeIf { it.isNotBlank() }?.let {
            sb.appendLine("NICKNAME:${escape(it)}")
        }

        // Organization
        card.organization?.let { org ->
            val name = escape(org.name)
            val dept = escape(org.department.orEmpty())
            val unit = escape(org.unit.orEmpty())
            sb.appendLine("ORG:$name;$dept;$unit")
        }

        // Title & Role
        card.title?.takeIf { it.isNotBlank() }?.let { sb.appendLine("TITLE:${escape(it)}") }
        card.role?.takeIf { it.isNotBlank() }?.let { sb.appendLine("ROLE:${escape(it)}") }

        // Phone numbers
        for (phone in card.phones) {
            val typeStr = buildPhoneType(phone.type, phone.isPrimary)
            sb.appendLine("TEL;TYPE=$typeStr:${phone.number}")
        }

        // Emails
        for (email in card.emails) {
            val typeStr = buildEmailType(email.type, email.isPrimary)
            sb.appendLine("EMAIL;TYPE=$typeStr:${email.address}")
        }

        // Addresses
        for (addr in card.addresses) {
            val typeStr = buildAddressType(addr.type, addr.isPrimary)
            val poBox = escape(addr.postOfficeBox.orEmpty())
            val ext = escape(addr.extendedAddress.orEmpty())
            val street = escape(addr.street.orEmpty())
            val locality = escape(addr.locality.orEmpty())
            val region = escape(addr.region.orEmpty())
            val postal = escape(addr.postalCode.orEmpty())
            val country = escape(addr.country.orEmpty())
            sb.appendLine("ADR;TYPE=$typeStr:$poBox;$ext;$street;$locality;$region;$postal;$country")
        }

        // Websites
        for (web in card.websites) {
            val typeStr = when (web.type) {
                WebType.WORK -> "WORK"
                WebType.PERSONAL -> "HOME"
                WebType.PORTFOLIO -> "PORTFOLIO"
                WebType.BLOG -> "BLOG"
                WebType.OTHER, WebType.CUSTOM -> "OTHER"
            }
            sb.appendLine("URL;TYPE=$typeStr:${web.url}")
        }

        // Social Profiles
        for (social in card.socialProfiles) {
            val profileUrl = social.url ?: social.platform.buildFullUrl(social.username)
            sb.appendLine("X-SOCIALPROFILE;TYPE=${social.platform.name.lowercase()}:$profileUrl")
        }

        // Birthday & Anniversary
        card.birthday?.takeIf { it.isNotBlank() }?.let { sb.appendLine("BDAY:$it") }
        card.anniversary?.takeIf { it.isNotBlank() }?.let { sb.appendLine("ANNIVERSARY:$it") }

        // Note
        card.note?.takeIf { it.isNotBlank() }?.let { sb.appendLine("NOTE:${escape(it)}") }

        // Photo & Logo
        card.photoUri?.takeIf { it.isNotBlank() }?.let { sb.appendLine("PHOTO:$it") }
        card.logoUri?.takeIf { it.isNotBlank() }?.let { sb.appendLine("LOGO:$it") }

        // Timezone & Geo
        card.timezone?.takeIf { it.isNotBlank() }?.let { sb.appendLine("TZ:$it") }
        card.geo?.let { sb.appendLine("GEO:${it.latitude};${it.longitude}") }

        // Categories
        if (card.categories.isNotEmpty()) {
            sb.appendLine("CATEGORIES:${card.categories.joinToString(",") { escape(it) }}")
        }

        // UID & Revision
        card.uid?.takeIf { it.isNotBlank() }?.let { sb.appendLine("UID:$it") }
        card.revision?.takeIf { it.isNotBlank() }?.let { sb.appendLine("REV:$it") }

        // Custom fields
        for ((key, value) in card.customFields) {
            sb.appendLine("$key:${escape(value)}")
        }

        sb.appendLine("END:VCARD")
        return sb.toString()
    }

    fun serializeAll(cards: List<VCardDto>): String {
        return cards.joinToString("\n") { serialize(it) }
    }

    private fun buildPhoneType(type: PhoneType, isPrimary: Boolean): String {
        val base = when (type) {
            PhoneType.CELL -> "CELL"
            PhoneType.WORK -> "WORK"
            PhoneType.HOME -> "HOME"
            PhoneType.MAIN -> "MAIN"
            PhoneType.WORK_FAX -> "WORK,FAX"
            PhoneType.HOME_FAX -> "HOME,FAX"
            PhoneType.PAGER -> "PAGER"
            PhoneType.OTHER, PhoneType.CUSTOM -> "VOICE"
        }
        return if (isPrimary) "$base,PREF" else base
    }

    private fun buildEmailType(type: EmailType, isPrimary: Boolean): String {
        val base = when (type) {
            EmailType.WORK -> "WORK"
            EmailType.HOME -> "HOME"
            EmailType.OTHER, EmailType.CUSTOM -> "INTERNET"
        }
        return if (isPrimary) "$base,PREF" else base
    }

    private fun buildAddressType(type: AddressType, isPrimary: Boolean): String {
        val base = when (type) {
            AddressType.WORK -> "WORK"
            AddressType.HOME -> "HOME"
            AddressType.POSTAL -> "POSTAL"
            AddressType.OTHER, AddressType.CUSTOM -> "DOM"
        }
        return if (isPrimary) "$base,PREF" else base
    }

    private fun escape(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
    }
}

fun VCardDto.toVCardString(): String = VCardSerializer.serialize(this)
