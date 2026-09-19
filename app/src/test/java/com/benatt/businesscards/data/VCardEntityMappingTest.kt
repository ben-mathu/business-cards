package com.benatt.businesscards.data

import com.benatt.businesscards.data.dto.EmailDto
import com.benatt.businesscards.data.dto.OrganizationDto
import com.benatt.businesscards.data.dto.PhoneDto
import com.benatt.businesscards.data.dto.PhoneType
import com.benatt.businesscards.data.dto.VCardDto
import com.benatt.businesscards.data.dto.VCardNameDto
import com.benatt.businesscards.data.local.entity.VCardEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class VCardEntityMappingTest {

    @Test
    fun `dto to entity and back preserves all core and search fields`() {
        val dto = VCardDto(
            uid = "card-123",
            version = "3.0",
            formattedName = "Morgan Lee",
            name = VCardNameDto(givenName = "Morgan", familyName = "Lee"),
            organization = OrganizationDto(name = "OpenSource Labs", department = "Core"),
            title = "Architect",
            phones = listOf(PhoneDto(number = "+1999888777", type = PhoneType.CELL, isPrimary = true)),
            emails = listOf(EmailDto(address = "morgan@example.org", isPrimary = true)),
            note = "Frequent collaborator",
            categories = listOf("Colleague", "Tech")
        )

        val entity = VCardEntity.fromDto(dto, rawVcf = "RAW_VCF_SAMPLE")

        assertEquals("card-123", entity.id)
        assertEquals("Morgan Lee", entity.formattedName)
        assertEquals("+1999888777", entity.primaryPhone)
        assertEquals("morgan@example.org", entity.primaryEmail)
        assertEquals("OpenSource Labs", entity.organization)
        assertEquals("Architect", entity.title)
        assertEquals("RAW_VCF_SAMPLE", entity.rawVcf)

        val restoredDto = entity.toDto()
        assertEquals(dto.uid, restoredDto.uid)
        assertEquals(dto.formattedName, restoredDto.formattedName)
        assertEquals(dto.name?.givenName, restoredDto.name?.givenName)
        assertEquals(dto.name?.familyName, restoredDto.name?.familyName)
        assertEquals(dto.organization?.name, restoredDto.organization?.name)
        assertEquals(dto.title, restoredDto.title)
        assertEquals(dto.phones, restoredDto.phones)
        assertEquals(dto.emails, restoredDto.emails)
        assertEquals(dto.note, restoredDto.note)
        assertEquals(dto.categories, restoredDto.categories)
    }
}
