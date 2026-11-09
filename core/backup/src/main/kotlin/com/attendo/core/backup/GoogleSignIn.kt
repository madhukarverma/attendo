package com.attendo.core.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * Returns a function that starts Google sign-in for [backup] from a screen: it shows Google's
 * account picker / consent screen when needed and hands the result back to [backup].
 */
@Composable
fun rememberGoogleSignIn(backup: GoogleDriveBackup): () -> Unit {
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        backup.onSignInResult(result.resultCode, result.data)
    }
    return remember(backup, launcher) {
        {
            scope.launch {
                backup.signIn()?.let { sender -> launcher.launch(IntentSenderRequest.Builder(sender).build()) }
            }
        }
    }
}
