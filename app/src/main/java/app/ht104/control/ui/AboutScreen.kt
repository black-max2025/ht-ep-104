package app.ht104.control.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ht104.control.BuildConfig
import app.ht104.control.R

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard(stringResource(R.string.app_name)) {
                KeyValueRow(stringResource(R.string.about_version), BuildConfig.VERSION_NAME)
                Text(stringResource(R.string.about_desc))
            }
        }
        item {
            SectionCard(stringResource(R.string.about_principles)) {
                Text(stringResource(R.string.about_local))
                Text(stringResource(R.string.about_no_fabrication))
                Text(stringResource(R.string.about_disconnect_vs_power))
                Text(stringResource(R.string.about_protocol_status))
            }
        }
    }
}
