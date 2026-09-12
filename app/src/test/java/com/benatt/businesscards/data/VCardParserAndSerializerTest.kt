package com.benatt.businesscards.data

import com.benatt.businesscards.data.dto.AddressType
import com.benatt.businesscards.data.dto.EmailType
import com.benatt.businesscards.data.dto.PhoneType
import com.benatt.businesscards.data.dto.SocialPlatform
import com.benatt.businesscards.data.parser.VCardParser
import com.benatt.businesscards.data.parser.VCardSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class VCardParserAndSerializerTest {

    private val sampleVcf = """
        BEGIN:VCARD
        VERSION:3.0
        FN:Jane Doe
        N:Doe;Jane;Marie;Dr.;PhD
        ORG:Acme Corp;Mobile Engineering;Android
        TITLE:Senior Staff Engineer
        ROLE:Technical Lead
        TEL;TYPE=CELL,PREF:+1-555-123-4567
        TEL;TYPE=WORK:+1-555-987-6543
        EMAIL;TYPE=WORK,PREF:jane.doe@acme.com
        EMAIL;TYPE=HOME:jane.personal@example.com
        ADR;TYPE=WORK:;;100 Technology Way;San Francisco;CA;94105;USA
        URL;TYPE=WORK:https://acme.com
        X-SOCIALPROFILE;TYPE=linkedin:https://linkedin.com/in/janedoe
        X-SOCIALPROFILE;TYPE=github:https://github.com/janedoe
        NOTE:Specializes in Android architectural design\nand open source development.
        BDAY:1990-05-15
        UID:sample-uuid-1234
        END:VCARD
    """.trimIndent()

    @Test
    fun `parse single vcard string correctly`() {
        val card = VCardParser.parseSingle(sampleVcf)
        assertNotNull(card)
        card!!

        assertEquals("Jane Doe", card.formattedName)
        assertEquals("Doe", card.name?.familyName)
        assertEquals("Jane", card.name?.givenName)
        assertEquals("Marie", card.name?.middleName)
        assertEquals("Dr.", card.name?.prefix)
        assertEquals("PhD", card.name?.suffix)

        assertEquals("Acme Corp", card.organization?.name)
        assertEquals("Mobile Engineering", card.organization?.department)
        assertEquals("Senior Staff Engineer", card.title)
        assertEquals("Technical Lead", card.role)

        assertEquals(2, card.phones.size)
        assertEquals("+1-555-123-4567", card.primaryPhone?.number)
        assertEquals(PhoneType.CELL, card.primaryPhone?.type)
        assertTrue(card.primaryPhone?.isPrimary == true)

        assertEquals(2, card.emails.size)
        assertEquals("jane.doe@acme.com", card.primaryEmail?.address)
        assertEquals(EmailType.WORK, card.primaryEmail?.type)
        assertTrue(card.primaryEmail?.isPrimary == true)

        assertEquals(1, card.addresses.size)
        val address = card.addresses.first()
        assertEquals("100 Technology Way", address.street)
        assertEquals("San Francisco", address.locality)
        assertEquals("CA", address.region)
        assertEquals("94105", address.postalCode)
        assertEquals("USA", address.country)
        assertEquals(AddressType.WORK, address.type)

        assertEquals(2, card.socialProfiles.size)
        val linkedin = card.socialProfiles.find { it.platform == SocialPlatform.LINKEDIN }
        assertNotNull(linkedin)
        assertEquals("janedoe", linkedin?.username)

        assertEquals("1990-05-15", card.birthday)
        assertEquals("sample-uuid-1234", card.uid)
        assertTrue(card.note?.contains("Specializes in Android") == true)
    }

    @Test
    fun `parse multiple vcards from input stream`() {
        val multipleVcf = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Alice Smith
            TEL:+11111111
            END:VCARD
            BEGIN:VCARD
            VERSION:3.0
            FN:Bob Johnson
            TEL:+22222222
            END:VCARD
        """.trimIndent()

        val inputStream = ByteArrayInputStream(multipleVcf.toByteArray(Charsets.UTF_8))
        val cards = VCardParser.parse(inputStream)

        assertEquals(2, cards.size)
        assertEquals("Alice Smith", cards[0].formattedName)
        assertEquals("+11111111", cards[0].phones.first().number)
        assertEquals("Bob Johnson", cards[1].formattedName)
        assertEquals("+22222222", cards[1].phones.first().number)
    }

    @Test
    fun `parse handles line unfolding`() {
        val foldedVcf = "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Folded\r\n Name With Long Line\r\nNOTE:This is a very lo\r\n ng note text.\r\nEND:VCARD"
        val card = VCardParser.parseSingle(foldedVcf)
        assertNotNull(card)
        assertEquals("FoldedName With Long Line", card?.formattedName)
        assertEquals("This is a very long note text.", card?.note)
    }

    @Test
    fun `serializer roundtrip produces valid vcard text`() {
        val original = VCardParser.parseSingle(sampleVcf)
        assertNotNull(original)

        val serialized = VCardSerializer.serialize(original!!)
        val reParsed = VCardParser.parseSingle(serialized)
        assertNotNull(reParsed)

        assertEquals(original.formattedName, reParsed?.formattedName)
        assertEquals(original.name?.givenName, reParsed?.name?.givenName)
        assertEquals(original.name?.familyName, reParsed?.name?.familyName)
        assertEquals(original.primaryPhone?.number, reParsed?.primaryPhone?.number)
        assertEquals(original.primaryEmail?.address, reParsed?.primaryEmail?.address)
        assertEquals(original.organization?.name, reParsed?.organization?.name)
    }
}
