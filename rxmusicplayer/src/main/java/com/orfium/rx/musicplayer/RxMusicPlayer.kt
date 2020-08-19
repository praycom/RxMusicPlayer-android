package com.orfium.rx.musicplayer

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.orfium.rx.musicplayer.common.Action
import com.orfium.rx.musicplayer.common.PlaybackState
import com.orfium.rx.musicplayer.common.QueueData
import com.orfium.rx.musicplayer.media.MediaService
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

object RxMusicPlayer {

    private val playbackStateSubject = BehaviorSubject.create<PlaybackState>()
    private val queueSubject = BehaviorSubject.create<QueueData>()
    private val actionSubject = PublishSubject.create<Action>()
    private val playbackPositionSubject = PublishSubject.create<Long>()

    var notificationIconRes: Int = R.mipmap.ic_notification_small
        private set

    @JvmStatic
    fun setNotificationIconRes(notificationIconRes: Int) {
        this.notificationIconRes = notificationIconRes
    }

    @JvmStatic
    fun start(context: Context, intent: Intent? = null) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, MediaService::class.java).apply {
                putExtra(MediaService.EXTRA_INTENT, intent)
            }
        )
    }

    @JvmStatic
    fun start(context: Context, intent: Intent, notificationIconRes: Int) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, MediaService::class.java).apply {
                putExtra(MediaService.EXTRA_INTENT, intent)
                putExtra(MediaService.EXTRA_NOTIFICATION_ICON_RES, notificationIconRes)
            }
        )
    }

    @JvmStatic
    val state: BehaviorSubject<PlaybackState> =
        playbackStateSubject

    @JvmStatic
    val queue: BehaviorSubject<QueueData> =
        queueSubject

    @JvmStatic
    val action: PublishSubject<Action> =
        actionSubject

    @JvmStatic
    val position: PublishSubject<Long> =
        playbackPositionSubject

}
