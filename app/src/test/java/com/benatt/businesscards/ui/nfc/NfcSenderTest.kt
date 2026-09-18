package com.benatt.businesscards.ui.nfc

import com.benatt.businesscards.data.dto.AddressDto
import com.benatt.businesscards.data.dto.EmailDto
import com.benatt.businesscards.data.dto.OrganizationDto
import com.benatt.businesscards.data.dto.PhoneDto
import com.benatt.businesscards.data.dto.VCardDto
import com.benatt.businesscards.data.dto.VCardNameDto
import com.benatt.businesscards.data.parser.toVCardString
import com.benatt.businesscards.ui.nfc.utils.NfcSendResult
import com.benatt.businesscards.ui.nfc.utils.NfcSender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcSenderTest {

    @Test
    fun `NfcSendResult models error states correctly`() {
        val readOnly = NfcSendResult.TagReadOnly
        val tagLost = NfcSendResult.TagLost
        val success = NfcSendResult.Success
        val capacity = NfcSendResult.InsufficientCapacity(requiredBytes = 500, maxBytes = 144)
        val error = NfcSendResult.Error("Device error")

        assertTrue(readOnly is NfcSendResult.TagReadOnly)
        assertTrue(tagLost is NfcSendResult.TagLost)
        assertTrue(success is NfcSendResult.Success)

        assertEquals(500, capacity.requiredBytes)
        assertEquals(144, capacity.maxBytes)
        assertEquals("Device error", error.message)
    }

    @Test
    fun `max NFC bytes constant is 144 bytes`() {
        assertEquals(144, NfcSender.MAX_NFC_BYTES)
    }

    @Test
    fun `calculateNdefSize includes MIME overhead correctly`() {
        val text = "BEGIN:VCARD\nVERSION:3.0\nFN:Alex\nEND:VCARD\n"
        val payloadSize = NfcSender.calculatePayloadSize(text)
        val ndefSize = NfcSender.calculateNdefSize(text)

        assertEquals(text.toByteArray(Charsets.UTF_8).size, payloadSize)
        // For payload < 256 bytes, header overhead is 13 bytes
        assertEquals(payloadSize + 13, ndefSize)
    }

    @Test
    fun `field selection reduces vCard size to fit under 144 bytes`() {
        val fullCard = VCardDto(
            id = 1,
            formattedName = "Alex Rivera",
            name = VCardNameDto(givenName = "Alex", familyName = "Rivera"),
            organization = OrganizationDto(name = "Nexis Technologies"),
            title = "Principal Android Engineer",
            phones = listOf(PhoneDto(number = "+1 (555) 234-5678", isPrimary = true)),
            emails = listOf(EmailDto(address = "alex.rivera@nexis.io", isPrimary = true)),
            addresses = listOf(AddressDto(street = "100 Innovation Boulevard", locality = "San Francisco", region = "CA")),
            note = "Speaks at Android dev summits. Specializes in Jetpack Compose, Kotlin Multiplatform, and local-first architecture."
        )

        val fullVCardString = fullCard.toVCardString()
        val fullNdefSize = NfcSender.calculateNdefSize(fullVCardString)

        // Full card exceeds 144 bytes because of note and address
        assertTrue(fullNdefSize > 144)

        // Trimmed card (name, primary phone; omit note, address, title, org, email)
        val trimmedCard = fullCard.copy(
            title = null,
            organization = null,
            emails = emptyList(),
            addresses = emptyList(),
            note = null
        )
        val trimmedVCardString = trimmedCard.toVCardString()
        val trimmedNdefSize = NfcSender.calculateNdefSize(trimmedVCardString)

        // Now it fits under 144 bytes (approx 117 bytes)
        assertTrue("Trimmed size $trimmedNdefSize should be <= 144 bytes", trimmedNdefSize <= 144)
    }
}
