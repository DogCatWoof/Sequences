package org.meow.sequences

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
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
import org.meow.sequences.data.firestore.FirestoreSyncService
import org.meow.sequences.ui.screens.AuthScreen
import org.meow.sequences.ui.screens.SequenceListScreen
import org.meow.sequences.ui.screens.SettingsScreen
import org.meow.sequences.ui.theme.SequencesTheme

class MainActivity : ComponentActivity() {
    private val authManager: GoogleAuthManager by inject()
    private val syncService: FirestoreSyncService by inject()
    private var isAuthenticated by mutableStateOf(false)
    private var isSyncing by mutableStateOf(false)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        registerNotificationChannels(this)
        isAuthenticated = authManager.isAuthenticated()
        if (isAuthenticated) {
            lifecycleScope.launch { syncFromFirestore() }
        }

        setContent {
            SequencesTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!isAuthenticated) {
                        AuthScreen(onSignInClick = {
                            lifecycleScope.launch {
                                if (authManager.signIn(this@MainActivity)) {
                                    isAuthenticated = true
                                    syncFromFirestore()
                                }
                            }
                        })
                    } else {
                        var showSettings by remember { mutableStateOf(false) }
                        if (showSettings) {
                            Scaffold { padding ->
                                SettingsScreen(
                                    onBack = { showSettings = false },
                                    onSignedOut = { isAuthenticated = false },
                                    modifier = Modifier.padding(padding),
                                )
                            }
                        } else {
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = { Text("Sequences") },
                                        actions = {
                                            if (isSyncing) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.padding(horizontal = 12.dp),
                                                    strokeWidth = 3.dp,
                                                )
                                            } else {
                                                IconButton(onClick = { syncFromFirestore() }) {
                                                    Icon(Icons.Default.Refresh, contentDescription = "Sync")
                                                }
                                            }
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

    private fun syncFromFirestore() {
        lifecycleScope.launch {
            isSyncing = true
            try {
                val uid = authManager.getFirebaseUid()
                syncService.pullAndMerge(uid, null)
                isSyncing = false
                Toast.makeText(this@MainActivity, "Sync complete", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                isSyncing = false
                Log.w("Sync Failed", "Sync failed: ${e.message}")
                Toast.makeText(this@MainActivity, "Sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
