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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.caojun.volumemanager.ui.theme.VolumeManagerTheme


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
        Button(
            onClick = {
                locked = !locked
                if (locked) {
                    initVolume(context)
                }
            },
            modifier = Modifier.padding(8.dp)
        ) {
            Text(stringResource(id = if (locked) R.string.locked else R.string.lock))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                getAudioManager(context)?.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
            },
            modifier = Modifier.padding(8.dp)
        ) {
            Text(stringResource(id = R.string.menu))
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
private val streamKeys = arrayOf(AudioManager.STREAM_VOICE_CALL,
    AudioManager.STREAM_SYSTEM,
    AudioManager.STREAM_RING,
    AudioManager.STREAM_MUSIC,
    AudioManager.STREAM_ALARM,
    AudioManager.STREAM_NOTIFICATION,
    AudioManager.STREAM_DTMF,
    AudioManager.STREAM_ACCESSIBILITY)
private val volumes = HashMap<Int, Int>()
private fun initVolume(context: Context) {
    val audioManager = getAudioManager(context)
    for (key in streamKeys) {
        val volume = audioManager?.getStreamVolume(key)
        volumes[key] = volume ?: 0
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
            return
        }
        val audioManager = getAudioManager(context)
        for (key in streamKeys) {
            val volume = volumes[key] ?: 0
            audioManager?.setStreamVolume(key, volume, 0)
        }
    }
}