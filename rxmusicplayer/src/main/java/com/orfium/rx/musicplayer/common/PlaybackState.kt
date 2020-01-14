package com.orfium.rx.musicplayer.common

import com.orfium.rx.musicplayer.media.Media

sealed class PlaybackState {

    object Idle : PlaybackState()
    data class Stopped(val position: Long = -1) : PlaybackState()
    data class Buffering(var media: Media?, val position: Long = -1) : PlaybackState()
    data class Playing(var media: Media?, val position: Long = -1) : PlaybackState()
    data class Paused(var media: Media?, val position: Long = -1) : PlaybackState()
    data class Completed(var media: Media?, val position: Long = -1) : PlaybackState()

    companion object {
        fun idle(): PlaybackState =
            Idle

        /**
         * When this method gets called, MediaService gets destroyed.
         * [com.orfium.rx.musicplayer.RxMusicPlayer.start] method needs to be called again
         */
        fun stopped(position: Long): PlaybackState =
            Stopped(position)

        fun buffering(media: Media?, position: Long): PlaybackState =
            Buffering(media, position)

        fun playing(media: Media?, position: Long): PlaybackState =
            Playing(media, position)

        fun paused(media: Media?, position: Long): PlaybackState =
            Paused(media, position)

        fun completed(media: Media?, position: Long): PlaybackState =
            Completed(media, position)
    }
}