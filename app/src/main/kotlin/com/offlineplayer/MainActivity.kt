package com.offlineplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.offlineplayer.core.playback.PlayerController
import com.offlineplayer.ui.OfflinePlayerApp
import com.offlineplayer.ui.theme.OfflinePlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main Activity — single-activity architecture.
 * Compose handles all navigation from this point.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var playerController: PlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        playerController.connect()
        setContent {
            OfflinePlayerTheme {
                OfflinePlayerApp()
            }
        }
    }

    override fun onDestroy() {
        playerController.disconnect()
        super.onDestroy()
    }
}
