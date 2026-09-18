package com.benatt.businesscards

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.benatt.businesscards.ui.cards.ContactCardsScreen
import com.benatt.businesscards.ui.cards.ContactCardsViewModel
import com.benatt.businesscards.ui.theme.BusinessCardsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: ContactCardsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        handleIncomingVcf(intent)

        enableEdgeToEdge()
        setContent {
            BusinessCardsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ContactCardsScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingVcf(intent)
    }

    private fun handleIncomingVcf(intent: Intent?) {
        viewModel.importFromIntent(applicationContext, intent)
    }
}