package com.benatt.businesscards

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.benatt.businesscards.ui.cards.ContactCardsScreen
import com.benatt.businesscards.ui.cards.ContactCardsViewModel
import com.benatt.businesscards.ui.editcontact.EditCardScreen
import com.benatt.businesscards.ui.theme.BusinessCardsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@Serializable
object HomeScreenRoute

@Serializable
data class EditCardRoute(val cardId: Long)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: ContactCardsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        handleIncomingVcf(intent)

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            BusinessCardsTheme {
                NavHost(navController = navController, startDestination = HomeScreenRoute) {
                    composable<HomeScreenRoute> {
                        ContactCardsScreen(
                            viewModel = viewModel,
                            onNavigateToEdit = { cardId ->
                                navController.navigate(EditCardRoute(cardId = cardId))
                            }
                        )
                    }

                    composable<EditCardRoute> {
                        EditCardScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
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