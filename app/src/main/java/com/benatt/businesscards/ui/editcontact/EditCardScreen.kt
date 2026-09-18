package com.benatt.businesscards.ui.editcontact

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.benatt.businesscards.ui.cards.ContactCardItem

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.benatt.businesscards.data.dto.VCardDto
import com.benatt.businesscards.ui.nfc.NfcSendDialog

/**
 * @author ben-mathu
 * 9/13/26
 */
@Composable
fun EditCardScreen(
    viewModel: EditCardViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var nfcCardToSend by remember { mutableStateOf<VCardDto?>(null) }

    BackHandler(true) {
        onNavigateBack()
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.card != null -> {
                    ContactCardItem(
                        card = uiState.card!!,
                        onSendNfc = { selectedCard ->
                            nfcCardToSend = selectedCard
                        }
                    )
                }
                else -> {}
            }

            nfcCardToSend?.let { card ->
                NfcSendDialog(
                    card = card,
                    onDismiss = { nfcCardToSend = null }
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewEditCardScreen() {
    EditCardScreen()
}