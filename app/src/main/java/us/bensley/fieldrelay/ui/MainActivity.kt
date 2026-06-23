package us.bensley.fieldrelay.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import us.bensley.fieldrelay.ui.navigation.FieldRelayNavHost
import us.bensley.fieldrelay.ui.theme.FieldRelayTheme

class MainActivity : ComponentActivity() {
    private var showDurationDialogRequest by mutableStateOf(false)
    private var confirmStopDialogRequest by mutableStateOf(false)
    private var requestPermissions by mutableStateOf(false)
    private var openReportRequest by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeIntent(intent)
        enableEdgeToEdge()
        setContent {
            FieldRelayTheme {
                FieldRelayNavHost(
                    showDurationDialogRequest = showDurationDialogRequest,
                    confirmStopDialogRequest = confirmStopDialogRequest,
                    requestPermissions = requestPermissions,
                    openReportRequest = openReportRequest,
                    onDurationDialogConsumed = { showDurationDialogRequest = false },
                    onConfirmStopDialogConsumed = { confirmStopDialogRequest = false },
                    onPermissionsConsumed = { requestPermissions = false },
                    onOpenReportConsumed = { openReportRequest = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        showDurationDialogRequest = intent?.getBooleanExtra("show_duration_dialog", false) == true
        confirmStopDialogRequest = intent?.getBooleanExtra("confirm_stop_dialog", false) == true
        requestPermissions = intent?.getBooleanExtra("request_permissions", false) == true
        openReportRequest = intent?.getBooleanExtra("open_report", false) == true
    }
}
