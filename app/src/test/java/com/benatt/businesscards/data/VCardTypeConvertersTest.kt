package com.benatt.businesscards.data

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
import com.benatt.businesscards.data.local.converter.VCardTypeConverters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VCardTypeConvertersTest {

    private val converters = VCardTypeConverters()

    @Test
    fun `phone list converts back and forth accurately`() {
        val phones = listOf(
            PhoneDto(number = "+123456789", type = PhoneType.CELL, isPrimary = true),
            PhoneDto(number = "555-4321", type = PhoneType.WORK_FAX, isPrimary = false, customLabel = "Office Fax")
        )

        val encoded = converters.fromPhoneList(phones)
        val decoded = converters.toPhoneList(encoded)

        assertEquals(phones, decoded)
    }

    @Test
    fun `email list converts back and forth accurately`() {
        val emails = listOf(
            EmailDto(address = "test@example.com", type = EmailType.WORK, isPrimary = true),
            EmailDto(address = "personal@home.net", type = EmailType.HOME, isPrimary = false)
        )

        val encoded = converters.fromEmailList(emails)
        val decoded = converters.toEmailList(encoded)

        assertEquals(emails, decoded)
    }

    @Test
    fun `address list converts back and forth accurately`() {
        val addresses = listOf(
            AddressDto(
                street = "123 Main St",
                extendedAddress = "Apt 4B",
                locality = "Boston",
                region = "MA",
                postalCode = "02108",
                country = "USA",
                type = AddressType.HOME,
                isPrimary = true
            )
        )

        val encoded = converters.fromAddressList(addresses)
        val decoded = converters.toAddressList(encoded)

        assertEquals(addresses, decoded)
    }

    @Test
    fun `web and social profiles convert back and forth accurately`() {
        val websites = listOf(WebDto(url = "https://example.com", type = WebType.PORTFOLIO))
        assertEquals(websites, converters.toWebList(converters.fromWebList(websites)))

        val socials = listOf(
            SocialProfileDto(platform = SocialPlatform.GITHUB, username = "octocat"),
            SocialProfileDto(platform = SocialPlatform.LINKEDIN, username = "dev", url = "https://linkedin.com/in/dev")
        )
        assertEquals(socials, converters.toSocialProfileList(converters.fromSocialProfileList(socials)))
    }

    @Test
    fun `empty and null handling returns safe empty collections`() {
        assertTrue(converters.toPhoneList(null).isEmpty())
        assertTrue(converters.toPhoneList("").isEmpty())
        assertTrue(converters.toEmailList("").isEmpty())
        assertTrue(converters.toAddressList("").isEmpty())
        assertTrue(converters.toWebList("").isEmpty())
        assertTrue(converters.toSocialProfileList("").isEmpty())
    }
}
