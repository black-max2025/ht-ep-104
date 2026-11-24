package app.ht104.control.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ht104.control.MainViewModel
import app.ht104.control.R
import kotlinx.coroutines.delay

@Composable
fun AudioScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    var playing by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val max = vm.audio.maxVolume().toFloat()

    LaunchedEffect(Unit) {
        while (true) {
            playing = vm.audio.isPlaying()
            if (!dragging) volume = vm.audio.volume().toFloat()
            delay(1000)
        }
    }

    LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard(stringResource(R.string.audio_playback)) {
                KeyValueRow(
                    stringResource(R.string.audio_status),
                    stringResource(if (playing) R.string.audio_playing else R.string.audio_not_playing),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.audio.previous() }) { Text(stringResource(R.string.audio_prev)) }
                    Button(onClick = { vm.audio.play() }) { Text(stringResource(R.string.audio_play)) }
                    Button(onClick = { vm.audio.pause() }) { Text(stringResource(R.string.audio_pause)) }
                    OutlinedButton(onClick = { vm.audio.next() }) { Text(stringResource(R.string.audio_next)) }
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.audio_volume)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.audio.volumeDown() }) { Text(stringResource(R.string.audio_vol_down)) }
                    OutlinedButton(onClick = { vm.audio.volumeUp() }) { Text(stringResource(R.string.audio_vol_up)) }
                }
                Slider(
                    value = volume,
                    onValueChange = { dragging = true; volume = it },
                    onValueChangeFinished = {
                        vm.audio.setVolume(volume.toInt())
                        dragging = false
                    },
                    valueRange = 0f..max,
                    steps = (max.toInt() - 1).coerceAtLeast(0),
                )
                Text(stringResource(R.string.audio_note))
            }
        }
    }
}
