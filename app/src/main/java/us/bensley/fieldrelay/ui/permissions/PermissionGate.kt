package us.bensley.fieldrelay.ui.permissions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import us.bensley.fieldrelay.permissions.hasBackgroundLocationPermission

@Composable
fun PermissionGate(
    active: Boolean,
    onReady: () -> Unit,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    var showLocationDenied by remember { mutableStateOf(false) }
    var showNotificationDenied by remember { mutableStateOf(false) }
    var showBackgroundPrompt by remember { mutableStateOf(false) }
    var showBackgroundDenied by remember { mutableStateOf(false) }

    fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun complete() {
        onReady()
        onFinished()
    }

    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        if (!it) showBackgroundDenied = true else complete()
    }

    val backgroundSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            complete()
        } else {
            showBackgroundDenied = true
        }
    }

    fun requestBackgroundOrComplete() {
        if (context.hasBackgroundLocationPermission()) {
            complete()
        } else {
            showBackgroundPrompt = true
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        if (it || hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            requestBackgroundOrComplete()
        } else {
            showNotificationDenied = true
            onFinished()
        }
    }

    fun requestNotificationOrNext() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            requestBackgroundOrComplete()
        }
    }

    val fineLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val fineGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineGranted || hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestNotificationOrNext()
        } else {
            showLocationDenied = true
            onFinished()
        }
    }

    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestNotificationOrNext()
        } else {
            fineLocationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    if (showLocationDenied) {
        AlertDialog(
            onDismissRequest = { showLocationDenied = false },
            title = { Text("Location required") },
            text = { Text("FieldRelay needs precise location permission before it can share your position.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationDenied = false
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", context.packageName, null)),
                        )
                    },
                ) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDenied = false }) { Text("Cancel") }
            },
        )
    }

    if (showBackgroundPrompt) {
        AlertDialog(
            onDismissRequest = {
                showBackgroundPrompt = false
                onFinished()
            },
            title = { Text("Allow background location") },
            text = {
                Text(backgroundPermissionMessage())
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackgroundPrompt = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            backgroundSettingsLauncher.launch(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.fromParts("package", context.packageName, null)),
                            )
                        } else {
                            backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    },
                ) { Text(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "Open settings" else "Continue") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBackgroundPrompt = false
                        onFinished()
                    },
                ) { Text("Cancel") }
            },
        )
    }

    if (showNotificationDenied) {
        AlertDialog(
            onDismissRequest = { showNotificationDenied = false },
            title = { Text("Notifications required") },
            text = { Text("FieldRelay needs notification permission before the unattended location beacon can start.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationDenied = false
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", context.packageName, null)),
                        )
                    },
                ) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationDenied = false }) { Text("Cancel") }
            },
        )
    }

    if (showBackgroundDenied) {
        AlertDialog(
            onDismissRequest = {
                showBackgroundDenied = false
                onFinished()
            },
            title = { Text("Background location off") },
            text = { Text("Background location is required before the unattended location beacon can start.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackgroundDenied = false
                        onFinished()
                    },
                ) { Text("OK") }
            },
        )
    }
}

private fun backgroundPermissionMessage(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        "To keep the location beacon running while the app is closed or the screen is off, open app settings and set Location to Allow all the time."
    } else {
        "To keep the location beacon running while the app is closed or the screen is off, allow background location."
    }
