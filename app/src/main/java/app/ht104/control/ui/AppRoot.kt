package app.ht104.control.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.ht104.control.MainViewModel
import app.ht104.control.R
import java.io.File

enum class Screen(@StringRes val title: Int) {
    Dashboard(R.string.nav_dashboard),
    Battery(R.string.nav_battery),
    Audio(R.string.nav_audio),
    Calls(R.string.nav_calls),
    Find(R.string.nav_find),
    Settings(R.string.nav_settings),
    Diagnostics(R.string.nav_diagnostics),
    About(R.string.nav_about),
}

/** Everything a screen needs from the Android framework that is not Compose state. */
class UiActions(
    val requestPermissions: (List<String>) -> Unit,
    val openBluetoothSettings: () -> Unit,
    val share: (File, String) -> Unit,
    val openMap: (Double, Double) -> Unit,
    val setLanguage: (String) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: MainViewModel, onShare: (File, String) -> Unit, onOpenBluetoothSettings: () -> Unit,
            onOpenMap: (Double, Double) -> Unit, onLanguage: (String) -> Unit) {
    var current by rememberSaveable { mutableStateOf(Screen.Dashboard) }
    BackHandler(enabled = current != Screen.Dashboard) { current = Screen.Dashboard }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.onPermissionResult()
    }
    val actions = UiActions(
        requestPermissions = { launcher.launch(it.toTypedArray()) },
        openBluetoothSettings = onOpenBluetoothSettings,
        share = onShare,
        openMap = onOpenMap,
        setLanguage = onLanguage,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(current.title)) },
                navigationIcon = {
                    if (current != Screen.Dashboard) {
                        IconButton(onClick = { current = Screen.Dashboard }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (current) {
            Screen.Dashboard -> DashboardScreen(vm, actions, { current = it }, modifier)
            Screen.Battery -> BatteryScreen(vm, modifier)
            Screen.Audio -> AudioScreen(vm, modifier)
            Screen.Calls -> CallsScreen(vm, actions, modifier)
            Screen.Find -> FindScreen(vm, actions, modifier)
            Screen.Settings -> SettingsScreen(vm, actions, modifier)
            Screen.Diagnostics -> DiagnosticsScreen(vm, actions, modifier)
            Screen.About -> AboutScreen(modifier)
        }
    }
}
