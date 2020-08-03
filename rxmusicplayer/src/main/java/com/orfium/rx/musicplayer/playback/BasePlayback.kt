package com.orfium.rx.musicplayer.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import com.orfium.rx.musicplayer.media.Media

internal abstract class BasePlayback(
    protected val context: Context,
    private val audioManager: AudioManager,
    private val wifiLock: WifiManager.WifiLock
) : Playback.PlayerCallback {

    protected var playbackCallback: Playback.ManagerCallback? = null

    @Volatile
    protected var currentMedia: Media? = null

    private val audioBecomingNoisyIntent = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

    private val audioNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                pause()
            }
        }
    }

    private val audioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    play(currentMedia)
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (isPlaying) {
                    pause()
                }
                AudioManager.AUDIOFOCUS_LOSS -> if (isPlaying) {
                    pause()
                }
            }
        }

    private val handler = Handler()

    @RequiresApi(Build.VERSION_CODES.O)
    private val audioFocusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener(audioFocusChangeListener, handler)
            .build()

    private var receiverRegistered = false

    abstract fun startPlayer()

    abstract fun pausePlayer()

    abstract fun resumePlayer()

    abstract fun stopPlayer()

    override fun play(media: Media?) {
        if (media != null) {
            requestFocus()
            registerWifiLock()
            registerNoiseReceiver()
            if (media == currentMedia) {
                resumePlayer()
            } else {
                currentMedia = media
                startPlayer()
            }
        }
    }

    override fun invalidateCurrent() {
        currentMedia = null
    }

    override fun pause() {
        pausePlayer()
        unregisterWifiLock()
        unregisterNoiseReceiver()
    }

    override fun complete() {
        invalidateCurrent()
        unregisterWifiLock()
        unregisterNoiseReceiver()
    }

    override fun stop() {
        releaseFocus()
        stopPlayer()
        invalidateCurrent()
        unregisterWifiLock()
        unregisterNoiseReceiver()
    }

    override fun setCallback(callback: Playback.ManagerCallback) {
        playbackCallback = callback
    }

    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(audioFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun releaseFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun registerWifiLock() {
        if (!wifiLock.isHeld) {
            wifiLock.acquire()
        }
    }

    private fun unregisterWifiLock() {
        if (wifiLock.isHeld) {
            wifiLock.release()
        }
    }

    private fun registerNoiseReceiver() {
        if (!receiverRegistered) {
            context.registerReceiver(audioNoisyReceiver, audioBecomingNoisyIntent)
            receiverRegistered = true
        }
    }

    private fun unregisterNoiseReceiver() {
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(audioNoisyReceiver)
            } catch (ignore: IllegalAccessException) {
            }
            receiverRegistered = false
        }
    }

}
