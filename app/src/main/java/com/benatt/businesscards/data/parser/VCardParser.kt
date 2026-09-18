package com.benatt.businesscards.data.parser

import com.benatt.businesscards.data.dto.AddressDto
import com.benatt.businesscards.data.dto.AddressType
import com.benatt.businesscards.data.dto.EmailDto
import com.benatt.businesscards.data.dto.EmailType
import com.benatt.businesscards.data.dto.GeoLocationDto
import com.benatt.businesscards.data.dto.OrganizationDto
import com.benatt.businesscards.data.dto.PhoneDto
import com.benatt.businesscards.data.dto.PhoneType
import com.benatt.businesscards.data.dto.SocialPlatform
import com.benatt.businesscards.data.dto.SocialProfileDto
import com.benatt.businesscards.data.dto.VCardDto
import com.benatt.businesscards.data.dto.VCardNameDto
import com.benatt.businesscards.data.dto.WebDto
import com.benatt.businesscards.data.dto.WebType
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.UUID

/**
 * Parser for vCard (.vcf) format compliant with RFC 2426 (vCard 3.0) and RFC 6350 (vCard 4.0).
 * Handles multiple vCards per file, multiline unfolding, escaped values, and property parameters.
 */
object VCardParser {

    /**
     * Parses all vCards found in an [InputStream].
     */
    fun parse(inputStream: InputStream, charset: Charset = Charsets.UTF_8): List<VCardDto> {
        val reader = BufferedReader(InputStreamReader(inputStream, charset))
        val content = reader.use { it.readText() }
        return parse(content)
    }

    /**
     * Parses all vCards found in a [File].
     */
    fun parse(file: File, charset: Charset = Charsets.UTF_8): List<VCardDto> {
        return parse(file.readText(charset))
    }

    /**
     * Parses a single vCard, returning the first card found or null.
     */
    fun parseSingle(vCardText: String): VCardDto? {
        return parse(vCardText).firstOrNull()
    }

    /**
     * Parses all vCards in a raw vCard text string.
     */
    fun parse(vCardText: String): List<VCardDto> {
        if (vCardText.isBlank()) return emptyList()

        val cards = mutableListOf<VCardDto>()
        val unfoldedText = unfoldLines(vCardText)
        val lines = unfoldedText.lines()

        var currentLines: MutableList<String>? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.equals("BEGIN:VCARD", ignoreCase = true)) {
                currentLines = mutableListOf()
            } else if (trimmed.equals("END:VCARD", ignoreCase = true)) {
                currentLines?.let {
                    cards.add(parseCardLines(it))
                }
                currentLines = null
            } else if (currentLines != null && trimmed.isNotEmpty()) {
                currentLines.add(line)
            }
        }

        // Handle case where BEGIN/END tags might be missing for single card
        if (cards.isEmpty() && currentLines != null && currentLines.isNotEmpty()) {
            cards.add(parseCardLines(currentLines))
        }

        return cards
    }

    /**
     * Unfolds lines in accordance with RFC 2426 / 6350:
     * A line starting with space or tab is a continuation of the previous line.
     */
    private fun unfoldLines(text: String): String {
        return text.replace(Regex("\r?\n[ \t]"), "")
    }

    private fun parseCardLines(lines: List<String>): VCardDto {
        var uid: String? = null
        var version = "3.0"
        var formattedName = ""
        var name: VCardNameDto? = null
        var nickname: String? = null
        var organization: OrganizationDto? = null
        var title: String? = null
        var role: String? = null
        val phones = mutableListOf<PhoneDto>()
        val emails = mutableListOf<EmailDto>()
        val addresses = mutableListOf<AddressDto>()
        val websites = mutableListOf<WebDto>()
        val socialProfiles = mutableListOf<SocialProfileDto>()
        var note: String? = null
        var birthday: String? = null
        var anniversary: String? = null
        var photoUri: String? = null
        var logoUri: String? = null
        var gender: String? = null
        var timezone: String? = null
        var geo: GeoLocationDto? = null
        val categories = mutableListOf<String>()
        var revision: String? = null
        val customFields = mutableMapOf<String, String>()

        for (line in lines) {
            val colonIndex = line.indexOf(':')
            if (colonIndex == -1) continue

            val rawKey = line.substring(0, colonIndex).trim()
            val rawValue = line.substring(colonIndex + 1).trim()
            val unescapedValue = unescapeValue(rawValue)

            val (propName, params) = parsePropertyHeader(rawKey)

            when (propName.uppercase()) {
                "VERSION" -> version = unescapedValue
                "UID" -> uid = unescapedValue
                "FN" -> formattedName = unescapedValue
                "N" -> name = parseStructuredName(rawValue)
                "NICKNAME" -> nickname = unescapedValue
                "ORG" -> organization = parseOrganization(rawValue)
                "TITLE" -> title = unescapedValue
                "ROLE" -> role = unescapedValue
                "TEL" -> phones.add(parsePhone(unescapedValue, params))
                "EMAIL" -> emails.add(parseEmail(unescapedValue, params))
                "ADR" -> addresses.add(parseAddress(rawValue, params))
                "URL" -> websites.add(parseWebsite(unescapedValue, params))
                "X-SOCIALPROFILE", "IMPP" -> {
                    val profile = parseSocialProfile(unescapedValue, params)
                    if (profile != null) socialProfiles.add(profile)
                }
                "NOTE" -> note = if (note == null) unescapedValue else "$note\n$unescapedValue"
                "BDAY" -> birthday = unescapedValue
                "ANNIVERSARY" -> anniversary = unescapedValue
                "PHOTO" -> photoUri = rawValue
                "LOGO" -> logoUri = rawValue
                "GENDER" -> gender = unescapedValue
                "TZ" -> timezone = unescapedValue
                "GEO" -> geo = parseGeoLocation(unescapedValue)
                "CATEGORIES" -> {
                    categories.addAll(
                        unescapedValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    )
                }
                "REV" -> revision = unescapedValue
                else -> {
                    if (propName.startsWith("X-", ignoreCase = true)) {
                        // Check if it's a known social network extension like X-TWITTER or X-GITHUB
                        val platform = matchPlatformName(propName.removePrefix("X-").removePrefix("x-"))
                        if (platform != null) {
                            socialProfiles.add(
                                SocialProfileDto(
                                    platform = platform,
                                    username = unescapedValue.substringAfterLast('/'),
                                    url = if (unescapedValue.startsWith("http")) unescapedValue else null
                                )
                            )
                        } else {
                            customFields[propName] = unescapedValue
                        }
                    }
                }
            }
        }

        return VCardDto(
            uid = uid ?: UUID.randomUUID().toString(),
            version = version,
            formattedName = formattedName,
            name = name,
            nickname = nickname,
            organization = organization,
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
            geo = geo,
            categories = categories,
            revision = revision,
            customFields = customFields
        )
    }

    private fun parsePropertyHeader(rawKey: String): Pair<String, Map<String, String>> {
        val parts = rawKey.split(';')
        val propName = parts[0].trim()
        val params = mutableMapOf<String, String>()

        for (i in 1 until parts.size) {
            val paramPart = parts[i].trim()
            val eqIndex = paramPart.indexOf('=')
            if (eqIndex != -1) {
                val pKey = paramPart.substring(0, eqIndex).trim().uppercase()
                val pVal = paramPart.substring(eqIndex + 1).trim()
                params[pKey] = pVal
            } else {
                // vCard 2.1 shorthand, e.g. TEL;WORK;VOICE:...
                params[paramPart.uppercase()] = paramPart
            }
        }
        return Pair(propName, params)
    }

    private fun parseStructuredName(rawValue: String): VCardNameDto {
        val parts = splitPreservingEscapes(rawValue, ';')
        return VCardNameDto(
            familyName = parts.getOrNull(0)?.let { unescapeValue(it) }?.takeIf { it.isNotBlank() },
            givenName = parts.getOrNull(1)?.let { unescapeValue(it) }?.takeIf { it.isNotBlank() },
            middleName = parts.getOrNull(2)?.let { unescapeValue(it) }?.takeIf { it.isNotBlank() },
            prefix = parts.getOrNull(3)?.let { unescapeValue(it) }?.takeIf { it.isNotBlank() },
            suffix = parts.getOrNull(4)?.let { unescapeValue(it) }?.takeIf { it.isNotBlank() }
        )
    }

    private fun parseOrganization(rawValue: String): OrganizationDto {
        val parts = splitPreservingEscapes(rawValue, ';')
        return OrganizationDto(
            name = parts.getOrNull(0)?.let { unescapeValue(it) }.orEmpty(),
            department = parts.getOrNull(1)?.let { unescapeValue(it) }?.takeIf { it.isNotBlank() },
            unit = parts.getOrNull(2)?.let { unescapeValue(it) }?.takeIf { it.isNotBlank() }
        )
    }

    private fun parsePhone(value: String, params: Map<String, String>): PhoneDto {
        val typeParam = (params["TYPE"] ?: params.keys.joinToString(",")).uppercase()
        val isPrimary = typeParam.contains("PREF")

        val type = when {
            typeParam.contains("CELL") || typeParam.contains("MOBILE") -> PhoneType.CELL
            typeParam.contains("WORK") && typeParam.contains("FAX") -> PhoneType.WORK_FAX
            typeParam.contains("HOME") && typeParam.contains("FAX") -> PhoneType.HOME_FAX
            typeParam.contains("WORK") -> PhoneType.WORK
            typeParam.contains("HOME") -> PhoneType.HOME
            typeParam.contains("MAIN") -> PhoneType.MAIN
            typeParam.contains("PAGER") -> PhoneType.PAGER
            else -> PhoneType.OTHER
        }

        return PhoneDto(
            number = value,
            type = type,
            isPrimary = isPrimary
        )
    }

    private fun parseEmail(value: String, params: Map<String, String>): EmailDto {
        val typeParam = (params["TYPE"] ?: params.keys.joinToString(",")).uppercase()
        val isPrimary = typeParam.contains("PREF")

        val type = when {
            typeParam.contains("WORK") -> EmailType.WORK
            typeParam.contains("HOME") -> EmailType.HOME
            else -> EmailType.OTHER
        }

        return EmailDto(
            address = value,
            type = type,
            isPrimary = isPrimary
        )
    }

    private fun parseAddress(rawValue: String, params: Map<String, String>): AddressDto {
        val parts = splitPreservingEscapes(rawValue, ';')
        val typeParam = (params["TYPE"] ?: params.keys.joinToString(",")).uppercase()
        val isPrimary = typeParam.contains("PREF")

        val type = when {
            typeParam.contains("WORK") -> AddressType.WORK
            typeParam.contains("HOME") -> AddressType.HOME
            typeParam.contains("POSTAL") -> AddressType.POSTAL
            else -> AddressType.OTHER
        }

        return AddressDto(
            postOfficeBox = parts.getOrNull(0)?.let { unescapeValue(it) }?.takeIf { it.isNotBlank() },
            extendedAddress = parts.getOrNull(1)?.let { unescapeValue(it) }?.takeIf { it.isNotBlank() },
            street = parts.getOrNull(2)?.let { unescapeValue(it) }?.takeIf { it.isNotBlank() },
            locality = parts.getOrNull(3)?.let { unescapeValue(it) }?.takeIf { it.isNotBlank() },
            region = parts.getOrNull(4)?.let { unescapeValue(it) }?.takeIf { it.isNotBlank() },
            postalCode = parts.getOrNull(5)?.let { unescapeValue(it) }?.takeIf { it.isNotBlank() },
            country = parts.getOrNull(6)?.let { unescapeValue(it) }?.takeIf { it.isNotBlank() },
            type = type,
            isPrimary = isPrimary
        )
    }

    private fun parseWebsite(value: String, params: Map<String, String>): WebDto {
        val typeParam = (params["TYPE"] ?: "").uppercase()
        val type = when {
            typeParam.contains("WORK") -> WebType.WORK
            typeParam.contains("HOME") || typeParam.contains("PERSONAL") -> WebType.PERSONAL
            typeParam.contains("PORTFOLIO") -> WebType.PORTFOLIO
            typeParam.contains("BLOG") -> WebType.BLOG
            else -> WebType.OTHER
        }
        return WebDto(url = value, type = type)
    }

    private fun parseSocialProfile(value: String, params: Map<String, String>): SocialProfileDto? {
        val typeParam = params["TYPE"]?.lowercase().orEmpty()
        val serviceParam = params["X-SERVICE-TYPE"]?.lowercase().orEmpty()

        val platform = matchPlatformName(typeParam)
            ?: matchPlatformName(serviceParam)
            ?: matchPlatformFromUrl(value)
            ?: SocialPlatform.OTHER

        val username = when {
            value.startsWith("http://") || value.startsWith("https://") -> {
                value.trimEnd('/').substringAfterLast('/')
            }
            value.contains(":") -> value.substringAfter(':')
            else -> value
        }

        return SocialProfileDto(
            platform = platform,
            username = username,
            url = if (value.startsWith("http")) value else null
        )
    }

    private fun parseGeoLocation(value: String): GeoLocationDto? {
        val clean = value.removePrefix("geo:").trim()
        val parts = if (clean.contains(',')) clean.split(',') else clean.split(';')
        if (parts.size >= 2) {
            val lat = parts[0].toDoubleOrNull()
            val lng = parts[1].toDoubleOrNull()
            if (lat != null && lng != null) {
                return GeoLocationDto(latitude = lat, longitude = lng)
            }
        }
        return null
    }

    private fun matchPlatformName(name: String): SocialPlatform? {
        val lower = name.lowercase()
        return when {
            lower.contains("linkedin") -> SocialPlatform.LINKEDIN
            lower.contains("github") -> SocialPlatform.GITHUB
            lower.contains("twitter") || lower == "x" -> SocialPlatform.TWITTER
            lower.contains("instagram") -> SocialPlatform.INSTAGRAM
            lower.contains("facebook") -> SocialPlatform.FACEBOOK
            lower.contains("youtube") -> SocialPlatform.YOUTUBE
            lower.contains("telegram") -> SocialPlatform.TELEGRAM
            lower.contains("whatsapp") -> SocialPlatform.WHATSAPP
            lower.contains("signal") -> SocialPlatform.SIGNAL
            lower.contains("discord") -> SocialPlatform.DISCORD
            lower.contains("tiktok") -> SocialPlatform.TIKTOK
            else -> null
        }
    }

    private fun matchPlatformFromUrl(url: String): SocialPlatform? {
        val lower = url.lowercase()
        return when {
            lower.contains("linkedin.com") -> SocialPlatform.LINKEDIN
            lower.contains("github.com") -> SocialPlatform.GITHUB
            lower.contains("twitter.com") || lower.contains("x.com") -> SocialPlatform.TWITTER
            lower.contains("instagram.com") -> SocialPlatform.INSTAGRAM
            lower.contains("facebook.com") -> SocialPlatform.FACEBOOK
            lower.contains("youtube.com") -> SocialPlatform.YOUTUBE
            lower.contains("t.me") || lower.contains("telegram.me") -> SocialPlatform.TELEGRAM
            lower.contains("wa.me") || lower.contains("whatsapp.com") -> SocialPlatform.WHATSAPP
            lower.contains("discord.gg") || lower.contains("discord.com") -> SocialPlatform.DISCORD
            lower.contains("tiktok.com") -> SocialPlatform.TIKTOK
            else -> null
        }
    }

    private fun splitPreservingEscapes(str: String, delimiter: Char): List<String> {
        val results = mutableListOf<String>()
        val current = StringBuilder()
        var escaped = false

        for (ch in str) {
            if (escaped) {
                current.append(ch)
                escaped = false
            } else if (ch == '\\') {
                current.append(ch)
                escaped = true
            } else if (ch == delimiter) {
                results.add(current.toString())
                current.clear()
            } else {
                current.append(ch)
            }
        }
        results.add(current.toString())
        return results
    }

    private fun unescapeValue(value: String): String {
        return value
            .replace("\\n", "\n", ignoreCase = true)
            .replace("\\;", ";")
            .replace("\\,", ",")
            .replace("\\\\", "\\")
    }
}
