package org.caojun.volumemanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.caojun.volumemanager.ui.theme.VolumeManagerTheme
import androidx.compose.material.Slider
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VolumeManagerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting(this)
                }
            }
        }

        initVolume(this)
        registerReceiver(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(volumeChangeReceiver)
    }
}

@Composable
fun Greeting(context: Context) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    locked = !locked
                    if (locked) {
                        initVolume(context)
                    }
                },
                modifier = Modifier.padding(Constant.SPACE_SMALL)
            ) {
                Text(stringResource(id = if (locked) R.string.locked else R.string.lock))
            }
            Spacer(modifier = Modifier.width(Constant.SPACE_NORMAL))
            Button(
                onClick = {
                    getAudioManager(context)?.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                },
                modifier = Modifier.padding(Constant.SPACE_SMALL)
            ) {
                Text(stringResource(id = R.string.menu))
            }
        }

        LazyColumn {
            val values = Constant.AudioStream.values()
            items(values.size) { index ->
                SoundTypeSlider(context, values[index])/* { newProgress ->
                    volumes[values[index]] = newProgress
                }*/
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VolumeManagerTheme {
        Greeting(LocalContext.current)
    }
}

private var locked by mutableStateOf(false)
private val volumes = HashMap<Constant.AudioStream, Int>()
private val volumesMax = HashMap<Constant.AudioStream, Int>()
private fun initVolume(context: Context) {
    val audioManager = getAudioManager(context)
    val keys = Constant.AudioStream.values()
    for (key in keys) {
        val int = key.int
        val volume = audioManager?.getStreamVolume(int) ?: Constant.VOLUME_MIN
        volumes[key] = volume
        val max = audioManager?.getStreamMaxVolume(int) ?: Constant.VOLUME_MAX
        volumesMax[key] = max
    }
}

private fun registerReceiver(context: Context) {
    val filter = IntentFilter()
    filter.addAction("android.media.VOLUME_CHANGED_ACTION")
    context.registerReceiver(volumeChangeReceiver, filter)
}

private fun getAudioManager(context: Context): AudioManager? {
    return try {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    } catch (e: Exception) {
        null
    }
}

private val volumeChangeReceiver = VolumeChangeReceiver()
private class VolumeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("VolumeChangeReceiver", "locked($locked), ${intent.action}")
        if (!locked) {
//            val audioManager = getAudioManager(context)
//            for (key in Constant.AudioStream.values()) {
//                val volume = audioManager?.getStreamVolume(key.int) ?: Constant.VOLUME_MIN
//                volumes[key] = volume
//                Log.d("LaunchedEffect", "volumes($key), ${volumes[key]}")
//            }
            return
        }
        for (key in Constant.AudioStream.values()) {
            val volume = volumes[key] ?: Constant.VOLUME_MIN
            setStreamVolume(context, key, volume)
        }
    }
}

private fun getStreamVolume(context: Context, audioStream: Constant.AudioStream): Int {
    val audioManager = getAudioManager(context)
    return audioManager?.getStreamVolume(audioStream.int) ?: Constant.VOLUME_MIN
}

private fun setStreamVolume(context: Context, audioStream: Constant.AudioStream, volume: Int) {
    val audioManager = getAudioManager(context)
    audioManager?.setStreamVolume(audioStream.int, volume, 0)
}

////////////////////////////////////////////////////////////////////////////////////////////////////

@Composable
private fun SoundTypeSlider(context: Context, audioStream: Constant.AudioStream/*, onProgressChange: (Int) -> Unit*/) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(Constant.SPACE_SMALL),
        horizontalAlignment = Alignment.Start
    ) {
        Row {
            Text(text = "${audioStream.name}: ")
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
//                    if (it.isNotEmpty()) {
//                        onProgressChange(it.toInt())
//                    }
                }
            )
        }
        Slider(
            value = getStreamVolume(context, audioStream).toFloat(),
            onValueChange = { newValue ->
                if (locked) {
                    return@Slider
                }

                val volume = newValue.toInt()
                Log.i("VolumeChangeReceiver", "onValueChange: $volume")
                setStreamVolume(context, audioStream, volume)

                text = newValue.toInt().toString()
//                onProgressChange(volume)
            },
            valueRange = 0f..(volumesMax[audioStream] ?: Constant.VOLUME_MAX).toFloat(),
            steps = 1,
            onValueChangeFinished = {
                volumes[audioStream] = getStreamVolume(context, audioStream)
            }
        )
    }
}