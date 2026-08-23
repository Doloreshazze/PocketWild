package com.playeverywhere.pocketwild

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.playeverywhere.pocketwild.game.GameViewModel
import com.playeverywhere.pocketwild.ui.PocketWildApp
import com.playeverywhere.pocketwild.ui.PocketWildTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketWildTheme {
                val gameViewModel: GameViewModel = viewModel()
                PocketWildApp(gameViewModel)
            }
        }
    }
}
