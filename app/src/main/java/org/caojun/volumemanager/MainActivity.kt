package org.caojun.volumemanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.text.TextUtils
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshotFlow

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

//        val dynamicValue = remember { mutableStateOf(50f) }
//        SliderWithDynamicValue(dynamicValue)

        LazyColumn {
            val values = Constant.AudioStream.values()
            items(values.size) { index ->
                val dynamicValue = remember { mutableStateOf(getStreamVolume(context, values[index])) }
                volumesReal[values[index]] = dynamicValue
                VolumeSlider(context, values[index], dynamicValue)
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
private val volumesReal = HashMap<Constant.AudioStream, MutableState<Int>>()
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
        for (key in Constant.AudioStream.values()) {
            val volume = getStreamVolume(context, key)
            volumesReal[key]?.value = volume
            Log.i("VolumeChangeReceiver", "volumesReal[$key]: ${volumesReal[key]}")
        }
        if (!locked) {
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
    volumesReal[audioStream]?.value = volume
}

////////////////////////////////////////////////////////////////////////////////////////////////////

@Composable
fun SliderWithDynamicValue(dynamicValue: MutableState<Float>) {
    var sliderValue by remember { mutableStateOf(dynamicValue.value) }

    LaunchedEffect(dynamicValue) {
        snapshotFlow { dynamicValue.value }
            .collect { newValue ->
                sliderValue = newValue
            }
    }

    Slider(
        value = sliderValue,
        onValueChange = { newValue ->
            sliderValue = newValue
            dynamicValue.value = newValue // 更新数据值
        },
        valueRange = 0f..100f
    )

    Button(onClick = {
        dynamicValue.value += 10f // 改变 dynamicValue 的值
    }) {
        Text("Increase dynamicValue")
    }
}

@Composable
private fun VolumeSlider(context: Context, audioStream: Constant.AudioStream, dynamicValue: MutableState<Int>) {
    var sliderValue by remember { mutableStateOf(dynamicValue.value) }

    LaunchedEffect(dynamicValue) {
        snapshotFlow { dynamicValue.value }
            .collect { newValue ->
                sliderValue = newValue
            }
    }

    Column(
        modifier = Modifier.padding(Constant.SPACE_SMALL),
        horizontalAlignment = Alignment.Start
    ) {
        Row {
            Text(text = "${audioStream.name}: ")
            BasicTextField(
                value = sliderValue.toString(),
                onValueChange = {
                    sliderValue = it.toInt()
                }
            )
        }
        Slider(
//            value = getStreamVolume(context, audioStream).toFloat(),
            value = sliderValue.toFloat(),
            onValueChange = { newValue ->
                if (locked) {
                    return@Slider
                }

                val volume = newValue.toInt()
                Log.i("VolumeChangeReceiver", "onValueChange: $volume")
                setStreamVolume(context, audioStream, volume)

                sliderValue = volume
            },
            valueRange = 0f..(volumesMax[audioStream] ?: Constant.VOLUME_MAX).toFloat(),
            steps = 1,
            onValueChangeFinished = {
                volumes[audioStream] = getStreamVolume(context, audioStream)
            }
        )
    }
}