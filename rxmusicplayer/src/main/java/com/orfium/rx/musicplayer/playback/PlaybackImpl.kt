package com.orfium.rx.musicplayer.playback

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.orfium.rx.musicplayer.R

internal class PlaybackImpl(
    context: Context,
    audioManager: AudioManager,
    wifiLock: WifiManager.WifiLock
) : BasePlayback(context, audioManager, wifiLock), Player.EventListener {

    private val player = SimpleExoPlayer.Builder(context)
        .run {
            setTrackSelector(DefaultTrackSelector(context))
            setLoadControl(DefaultLoadControl())
            build()
        }
        .apply {
            audioAttributes = AudioAttributes.Builder().run {
                setUsage(C.USAGE_MEDIA)
                setContentType(C.CONTENT_TYPE_SPEECH)
                build()
            }
        }

    override val isPlaying: Boolean
        get() = player.playWhenReady

    override val position: Long
        get() = player.currentPosition

    override val duration: Long
        get() = player.duration

    override fun startPlayer() {
        play()
    }

    override fun pausePlayer() {
        player.playWhenReady = false
    }

    override fun stopPlayer() {
        player.release()
        player.removeListener(this)
        player.playWhenReady = false
    }

    override fun resumePlayer() {
        player.playWhenReady = true
    }

    override fun seekTo(position: Long) {
        player.seekTo(position)
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            Player.STATE_IDLE -> playbackCallback?.onIdle()
            Player.STATE_READY -> if (playWhenReady) playbackCallback?.onPlay() else playbackCallback?.onPause()
            Player.STATE_BUFFERING -> if(playWhenReady) playbackCallback?.onBuffer() else playbackCallback?.onPause()
            Player.STATE_ENDED -> playbackCallback?.onCompletion()
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        playbackCallback?.onError()
    }

    private fun play() {
        val dataSourceFactory = DefaultDataSourceFactory(
            context, Util.getUserAgent(context, context.getString(R.string.app_name)), null
        )
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory, DefaultExtractorsFactory())
            .createMediaSource(Uri.parse(currentMedia?.streamUrl))
        player.addListener(this)
        player.prepare(mediaSource)
        player.playWhenReady = true
    }
}
