package com.benatt.businesscards.ui.cards

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benatt.businesscards.data.dto.AddressDto
import com.benatt.businesscards.data.dto.AddressType
import com.benatt.businesscards.data.dto.EmailDto
import com.benatt.businesscards.data.dto.EmailType
import com.benatt.businesscards.data.dto.OrganizationDto
import com.benatt.businesscards.data.dto.PhoneDto
import com.benatt.businesscards.data.dto.PhoneType
import com.benatt.businesscards.data.dto.SocialPlatform
import com.benatt.businesscards.data.dto.SocialProfileDto
import com.benatt.businesscards.data.dto.VCardDto
import com.benatt.businesscards.data.dto.VCardNameDto
import com.benatt.businesscards.data.dto.WebDto
import com.benatt.businesscards.data.dto.WebType
import com.benatt.businesscards.data.local.dao.VCardDao
import com.benatt.businesscards.data.local.entity.VCardEntity
import com.benatt.businesscards.data.local.importAndSaveFromIntent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ContactCardsUiState(
    val isLoading: Boolean = true,
    val cards: List<VCardDto> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class ContactCardsViewModel @Inject constructor(
    private val vCardDao: VCardDao
) : ViewModel() {

    val uiState: StateFlow<ContactCardsUiState> = vCardDao.getAllCards()
        .map { entities ->
            ContactCardsUiState(
                isLoading = false,
                cards = entities.map { it.toDto() }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ContactCardsUiState(isLoading = true)
        )

    fun importFromIntent(context: Context, intent: Intent?) {
        viewModelScope.launch {
            try {
                vCardDao.importAndSaveFromIntent(context, intent)
            } catch (e: Exception) {
                // Log or handle error if needed
            }
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            vCardDao.deleteCardById(cardId)
        }
    }

    fun addSampleCard() {
        viewModelScope.launch {
            val sampleCard = VCardDto(
                id = 0,
                uid = UUID.randomUUID().toString(),
                formattedName = "Alex Rivera",
                name = VCardNameDto(
                    givenName = "Alex",
                    familyName = "Rivera",
                    prefix = "Dr."
                ),
                organization = OrganizationDto(
                    name = "Nexis Technologies",
                    department = "Mobile Architecture"
                ),
                title = "Principal Android Engineer",
                role = "Core Platform Lead",
                phones = listOf(
                    PhoneDto(number = "+1 (555) 234-5678", type = PhoneType.CELL, isPrimary = true),
                    PhoneDto(number = "+1 (555) 876-5432", type = PhoneType.WORK, isPrimary = false)
                ),
                emails = listOf(
                    EmailDto(address = "alex.rivera@nexis.io", type = EmailType.WORK, isPrimary = true)
                ),
                addresses = listOf(
                    AddressDto(
                        street = "100 Innovation Boulevard",
                        locality = "San Francisco",
                        region = "CA",
                        postalCode = "94105",
                        country = "USA",
                        type = AddressType.WORK
                    )
                ),
                websites = listOf(
                    WebDto(url = "https://nexis.io", type = WebType.WORK)
                ),
                socialProfiles = listOf(
                    SocialProfileDto(platform = SocialPlatform.LINKEDIN, username = "alexrivera"),
                    SocialProfileDto(platform = SocialPlatform.GITHUB, username = "alexrivera")
                ),
                note = "Speaks at Android dev summits. Specializes in Jetpack Compose, Kotlin Multiplatform, and local-first architecture."
            )
            vCardDao.insertCard(VCardEntity.fromDto(sampleCard))
        }
    }
}
