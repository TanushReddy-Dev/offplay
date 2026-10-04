package com.offlineplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.offlineplayer.ui.OfflinePlayerApp
import com.offlineplayer.ui.theme.OfflinePlayerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity — single-activity architecture.
 * Compose handles all navigation from this point.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OfflinePlayerTheme {
                OfflinePlayerApp()
            }
        }
    }
}
