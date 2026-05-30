package com.dilaer.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.dilaer.mobile.ui.DialerScreen
import com.dilaer.mobile.ui.theme.DilaerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DialerViewModel by viewModels()

    private val openDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.importCsv(it) } }

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result[Manifest.permission.CALL_PHONE] == true) {
            viewModel.initMonitor()
            viewModel.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            DilaerTheme {
                val state by viewModel.uiState.collectAsState()
                DialerScreen(
                    state = state,
                    onImportClick = { openDocument.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*")) },
                    onStart = { startWithPermissions() },
                    onPause = viewModel::pause,
                    onStop = viewModel::stop,
                    onSkip = viewModel::skipCurrent,
                    onReset = viewModel::resetAll,
                    onCooldownChange = viewModel::setCooldown,
                    onMessageShown = viewModel::clearMessage,
                )
            }
        }
    }

    private fun startWithPermissions() {
        val needed = listOfNotNull(
            Manifest.permission.CALL_PHONE.takeIf { !granted(it) },
            Manifest.permission.READ_PHONE_STATE.takeIf { !granted(it) },
        )
        if (needed.isEmpty()) {
            viewModel.initMonitor()
            viewModel.start()
        } else {
            requestPermission.launch(needed.toTypedArray())
        }
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
