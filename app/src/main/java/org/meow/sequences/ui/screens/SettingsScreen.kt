package org.meow.sequences.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.meow.sequences.data.auth.GoogleAuthManager
import org.meow.sequences.data.auth.TokenStore
import org.meow.sequences.data.debug.DebugSettings

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignedOut: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val authManager: GoogleAuthManager = koinInject()
    val tokenStore: TokenStore = koinInject()
    var accountEmail by remember { mutableStateOf(tokenStore.getAccountEmail()) }
    val debugSettings: DebugSettings = koinInject()
    var debugEnabled by remember { mutableStateOf(debugSettings.isDebugEnabled) }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsSectionLabel("Account")
        ListItem(
            headlineContent = { Text("Google Account") },
            supportingContent = { Text(accountEmail ?: "Connected") },
            trailingContent = {
                TextButton(onClick = {
                    scope.launch {
                        authManager.signOut()
                        accountEmail = null
                        onSignedOut()
                    }
                }) { Text("Disconnect") }
            },
        )
        HorizontalDivider()
        SettingsSectionLabel("Diagnostics")
        ListItem(
            headlineContent = { Text("Debug Mode") },
            supportingContent = { Text("Show exception toasts when errors occur") },
            trailingContent = {
                Switch(
                    checked = debugEnabled,
                    onCheckedChange = {
                        debugEnabled = it
                        debugSettings.isDebugEnabled = it
                    },
                )
            },
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Back") },
            modifier = Modifier.padding(16.dp),
            trailingContent = {
                TextButton(onClick = onBack) { Text("Back") }
            },
        )
    }
}
