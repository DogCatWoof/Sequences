package org.meow.sequences

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.meow.sequences.core.notifications.registerNotificationChannels
import org.meow.sequences.data.auth.GoogleAuthManager
import org.meow.sequences.ui.screens.AuthScreen
import org.meow.sequences.ui.screens.SequenceListScreen
import org.meow.sequences.ui.screens.SettingsScreen
import org.meow.sequences.ui.theme.SequencesTheme

class MainActivity : ComponentActivity() {
    private val authManager: GoogleAuthManager by inject()
    private var isAuthenticated by mutableStateOf(false)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        registerNotificationChannels(this)
        isAuthenticated = authManager.isAuthenticated()

        setContent {
            SequencesTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!isAuthenticated) {
                        AuthScreen(onSignInClick = {
                            lifecycleScope.launch {
                                if (authManager.signIn(this@MainActivity)) {
                                    isAuthenticated = true
                                }
                            }
                        })
                    } else {
                        var showSettings by remember { mutableStateOf(false) }
                        if (showSettings) {
                            SettingsScreen(
                                onBack = { showSettings = false },
                                onSignedOut = { isAuthenticated = false },
                            )
                        } else {
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = { Text("Sequences") },
                                        actions = {
                                            IconButton(onClick = { showSettings = true }) {
                                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                                            }
                                        },
                                    )
                                }
                            ) { padding ->
                                SequenceListScreen(modifier = Modifier.padding(padding))
                            }
                        }
                    }
                }
            }
        }
    }
}
