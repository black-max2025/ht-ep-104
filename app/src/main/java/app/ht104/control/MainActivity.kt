package app.ht104.control

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import android.os.Bundle
import app.ht104.control.data.LocaleHelper
import app.ht104.control.ui.AppRoot
import app.ht104.control.ui.HtTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HtTheme {
                AppRoot(
                    vm = vm,
                    onShare = { file, mime ->
                        startActivity(Intent.createChooser(vm.shareIntent(file, mime), getString(R.string.diag_export)))
                    },
                    onOpenBluetoothSettings = { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                    onOpenMap = { lat, lon ->
                        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon"))) }
                    },
                    onLanguage = { lang ->
                        vm.setLanguage(lang)
                        recreate()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onPermissionResult()
    }
}
