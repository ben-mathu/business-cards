package com.benatt.businesscards.data.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class VCardDtoTest {

    @Test
    fun `structured name toFormattedName formats correctly`() {
        val name = VCardNameDto(
            prefix = "Dr.",
            givenName = "Jane",
            middleName = "A.",
            familyName = "Doe",
            suffix = "PhD"
        )
        assertEquals("Dr. Jane A. Doe PhD", name.toFormattedName())
    }

    @Test
    fun `displayName falls back to structured name or org or email or phone`() {
        val withFn = VCardDto(formattedName = "Jane Doe")
        assertEquals("Jane Doe", withFn.displayName)

        val withStructured = VCardDto(
            name = VCardNameDto(givenName = "John", familyName = "Smith")
        )
        assertEquals("John Smith", withStructured.displayName)

        val withOrg = VCardDto(
            organization = OrganizationDto(name = "Acme Corp")
        )
        assertEquals("Acme Corp", withOrg.displayName)

        val withEmail = VCardDto(
            emails = listOf(EmailDto(address = "hello@example.com"))
        )
        assertEquals("hello@example.com", withEmail.displayName)

        val withPhone = VCardDto(
            phones = listOf(PhoneDto(number = "+123456789"))
        )
        assertEquals("+123456789", withPhone.displayName)
    }

    @Test
    fun `primaryPhone and primaryEmail select primary first`() {
        val phone1 = PhoneDto(number = "111", isPrimary = false)
        val phone2 = PhoneDto(number = "222", isPrimary = true)
        val dto = VCardDto(
            phones = listOf(phone1, phone2),
            emails = listOf(
                EmailDto(address = "first@example.com", isPrimary = false),
                EmailDto(address = "second@example.com", isPrimary = true)
            )
        )

        assertEquals("222", dto.primaryPhone?.number)
        assertEquals("second@example.com", dto.primaryEmail?.address)
    }

    @Test
    fun `address formatting produces single and multi-line formats`() {
        val address = AddressDto(
            street = "123 Market St",
            extendedAddress = "Suite 500",
            locality = "San Francisco",
            region = "CA",
            postalCode = "94105",
            country = "USA"
        )

        assertEquals(
            "123 Market St, Suite 500, San Francisco CA 94105, USA",
            address.formattedAddress(multiline = false)
        )
        assertEquals(
            "123 Market St, Suite 500\nSan Francisco CA 94105\nUSA",
            address.formattedAddress(multiline = true)
        )
    }

    @Test
    fun `social platform builds full url from username`() {
        val linkedin = SocialPlatform.LINKEDIN.buildFullUrl("johndoe")
        assertEquals("https://linkedin.com/in/johndoe", linkedin)

        val twitter = SocialPlatform.TWITTER.buildFullUrl("@janedoe")
        assertEquals("https://x.com/janedoe", twitter)

        val directUrl = SocialPlatform.GITHUB.buildFullUrl("https://github.com/custom")
        assertEquals("https://github.com/custom", directUrl)
    }

    @Test
    fun `vcard dto is fully serializable`() {
        val original = VCardDto(
            uid = "1234-5678",
            version = "3.0",
            formattedName = "Alex Rivera",
            name = VCardNameDto(givenName = "Alex", familyName = "Rivera"),
            organization = OrganizationDto(name = "Tech Innovations", department = "Mobile"),
            title = "Staff Android Engineer",
            phones = listOf(PhoneDto(number = "+1-555-0199", type = PhoneType.CELL, isPrimary = true)),
            emails = listOf(EmailDto(address = "alex@example.com", type = EmailType.WORK, isPrimary = true)),
            addresses = listOf(
                AddressDto(
                    street = "742 Evergreen Terrace",
                    locality = "Springfield",
                    region = "OR",
                    postalCode = "97477",
                    country = "USA"
                )
            ),
            websites = listOf(WebDto(url = "https://alexrivera.dev", type = WebType.PORTFOLIO)),
            socialProfiles = listOf(SocialProfileDto(platform = SocialPlatform.GITHUB, username = "alexrivera")),
            note = "Contact for Android consulting.",
            birthday = "1988-10-24",
            geo = GeoLocationDto(latitude = 37.7749, longitude = -122.4194),
            categories = listOf("Work", "Tech")
        )

        val byteStream = ByteArrayOutputStream()
        ObjectOutputStream(byteStream).use { it.writeObject(original) }

        val deserialized = ObjectInputStream(ByteArrayInputStream(byteStream.toByteArray())).use {
            it.readObject() as VCardDto
        }

        assertEquals(original, deserialized)
        assertEquals("Alex Rivera", deserialized.displayName)
        assertEquals("+1-555-0199", deserialized.primaryPhone?.number)
        assertEquals("alex@example.com", deserialized.primaryEmail?.address)
        assertEquals("Tech Innovations", deserialized.companyName)
    }
}
