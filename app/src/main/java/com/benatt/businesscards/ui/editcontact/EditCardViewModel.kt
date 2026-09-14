package com.benatt.businesscards.ui.editcontact

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.benatt.businesscards.EditCardRoute
import com.benatt.businesscards.MainActivity
import com.benatt.businesscards.data.dto.VCardDto
import com.benatt.businesscards.data.local.dao.VCardDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * @author ben-mathu
 * 9/13/26
 */
data class EditCardUiState(
    val isLoading: Boolean,
    val cardId: Long? = null,
    val card: VCardDto? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class EditCardViewModel
@Inject
constructor(
    private val vCardDao: VCardDao,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val args = savedStateHandle.toRoute<EditCardRoute>()

    val uiState : StateFlow<EditCardUiState> = vCardDao.getCardByIdFlow(args.cardId)
        .map { entity ->
            EditCardUiState(
                isLoading = false,
                card = entity?.toDto()
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EditCardUiState(isLoading = true)
        )
}