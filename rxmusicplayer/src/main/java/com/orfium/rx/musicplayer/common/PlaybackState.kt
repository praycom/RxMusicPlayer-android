package com.orfium.rx.musicplayer.common

import com.orfium.rx.musicplayer.media.Media

sealed class PlaybackState {

    object Idle : PlaybackState()
    data class Stopped(val position: Long = -1) : PlaybackState()

    /**
     * [position] is mutable so that the [equals] method can only check the object's type and [media]
     */
    data class Buffering(var media: Media?) : PlaybackState() {
        var position: Long = -1
    }

    /**
     * [position] is mutable so that the [equals] method can only check the object's type and [media]
     */
    data class Playing(var media: Media?) : PlaybackState() {
        var position: Long = -1
    }

    /**
     * [position] is mutable so that the [equals] method can only check the object's type and [media]
     */
    data class Paused(var media: Media?) : PlaybackState() {
        var position: Long = -1
    }

    /**
     * [position] is mutable so that the [equals] method can only check the object's type and [media]
     */
    data class Completed(var media: Media?) : PlaybackState() {
        var position: Long = -1
    }

    companion object {
        fun idle(): PlaybackState =
            Idle

        /**
         * When this method gets called, MediaService gets destroyed.
         * [com.orfium.rx.musicplayer.RxMusicPlayer.start] method needs to be called again
         */
        fun stopped(position: Long): PlaybackState =
            Stopped(position)

        fun buffering(media: Media?, position: Long): PlaybackState {
            val buffering = Buffering(media)
            buffering.position = position
            return buffering
        }

        fun playing(media: Media?, position: Long): PlaybackState {
            val playing = Playing(media)
            playing.position = position
            return playing
        }

        fun paused(media: Media?, position: Long): PlaybackState {
            val paused = Paused(media)
            paused.position = position
            return paused
        }

        fun completed(media: Media?, position: Long): PlaybackState {
            val completed = Completed(media)
            completed.position = position
            return completed
        }

    }
}