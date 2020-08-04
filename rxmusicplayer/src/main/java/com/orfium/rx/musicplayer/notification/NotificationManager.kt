package com.orfium.rx.musicplayer.notification

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.orfium.rx.musicplayer.R
import com.orfium.rx.musicplayer.common.PlaybackState
import com.orfium.rx.musicplayer.media.Media
import com.orfium.rx.musicplayer.media.MediaService

internal class NotificationManager(
    private val service: Service,
    private val token: MediaSessionCompat.Token,
    private val notificationManager: NotificationManagerCompat,
    private var notificationIconRes: Int = R.mipmap.ic_notification_small
) {

    companion object {
        private const val NOTIFICATION_ID = 100

        private const val PLAYER_PENDING_INTENT_ID = 10
        private const val PAUSE_PENDING_INTENT_ID = 20
        private const val PLAY_PENDING_INTENT_ID = 30
        private const val PLAY_NEXT_PENDING_INTENT_ID = 40
        private const val PLAY_PREV_PENDING_INTENT_ID = 50
        private const val STOP_PENDING_INTENT_ID = 60
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private lateinit var notificationChannels: NotificationChannels
    private var intent: Intent? = null

    @Volatile
    private var hasStarted: Boolean = false
    private var handler = Handler()
    private var state: PlaybackState = PlaybackState.Idle
    private var media: Media? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannels = NotificationChannels(service)
        }
    }

    fun setIntent(intent: Intent?) {
        this.intent = intent
    }

    fun updateMedia(media: Media?) {
        this.media = media
    }

    fun updateState(state: PlaybackState) {
        this.state = state
        when (state) {
            is PlaybackState.Buffering -> updateNotification()
            is PlaybackState.Playing -> updateNotification()
            is PlaybackState.Paused -> pauseNotification()
            is PlaybackState.Completed -> pauseNotification()
            else -> stopNotification()
        }
    }

    fun setNotificationIconRes(notificationIconRes: Int) {
        this.notificationIconRes = notificationIconRes
    }

    fun startNotification() {
        val builder = createBuilder()
        Glide.with(service)
            .asBitmap()
            .load(media?.image)
            .apply(
                RequestOptions
                    .diskCacheStrategyOf(DiskCacheStrategy.DATA)
                    .onlyRetrieveFromCache(true)
                    .priority(Priority.IMMEDIATE)
            )
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    showNotification(builder, resource)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    showNotification(builder, null)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    showNotification(builder, null)
                }
            })
    }

    private fun updateNotification() {
        if (!hasStarted) {
            val intent = Intent(service, MediaService::class.java)
            ContextCompat.startForegroundService(service, intent)
        }
        startNotification()
    }

    private fun pauseNotification() {
        startNotification()
        handler.postDelayed({
            service.stopForeground(false)
            hasStarted = false
        }, 100)
    }

    private fun stopNotification() {
        hasStarted = false
        media = null
        notificationManager.cancel(NOTIFICATION_ID)
        service.stopForeground(true)
        service.stopSelf()
    }

    private fun pause(context: Context): NotificationCompat.Action {
        val pendingIntent = getPendingIntent(
            context,
            PAUSE_PENDING_INTENT_ID,
            MediaService.ACTION_PAUSE
        )
        return NotificationCompat.Action(R.drawable.ic_music_player_pause, "Pause", pendingIntent)
    }

    private fun next(context: Context): NotificationCompat.Action {
        val pendingIntent = getPendingIntent(
            context,
            PLAY_NEXT_PENDING_INTENT_ID,
            MediaService.ACTION_NEXT
        )
        return NotificationCompat.Action(R.drawable.ic_music_player_next, "Next", pendingIntent)
    }

    private fun prev(context: Context): NotificationCompat.Action {
        val pendingIntent = getPendingIntent(
            context,
            PLAY_PREV_PENDING_INTENT_ID,
            MediaService.ACTION_PREV
        )
        return NotificationCompat.Action(R.drawable.ic_music_player_prev, "Previous", pendingIntent)
    }

    private fun play(context: Context): NotificationCompat.Action {
        val pendingIntent = getPendingIntent(
            context,
            PLAY_PENDING_INTENT_ID,
            MediaService.ACTION_PLAY
        )
        return NotificationCompat.Action(R.drawable.ic_music_player_play, "Start", pendingIntent)
    }

    private fun dismiss(context: Context): PendingIntent {
        return getPendingIntent(
            context,
            STOP_PENDING_INTENT_ID,
            MediaService.ACTION_STOP
        )
    }

    private fun getPendingIntent(context: Context, intentId: Int, action: String): PendingIntent {
        val prevIntent = Intent(context, MediaService::class.java)
        prevIntent.action = action

        return PendingIntent.getService(
            context,
            intentId, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun showNotification(builder: NotificationCompat.Builder, bitmap: Bitmap?) {
        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(token)
                .setShowActionsInCompactView(0, 1, 2)
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setUsesChronometer(false)
            .setSmallIcon(notificationIconRes)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setContentTitle(media?.title)
            .setContentText(media?.artist)
            .setDeleteIntent(dismiss(service))

        if (state !is PlaybackState.Idle) {
            builder.addAction(prev(service))

            if (state is PlaybackState.Paused || state is PlaybackState.Completed) {
                builder.addAction(play(service))
                builder.setOngoing(false)
            } else {
                builder.addAction(pause(service))
                builder.setOngoing(true)
            }

            builder.addAction(next(service))
        }

        builder.setLargeIcon(bitmap)

        val notificationIntent = intent ?: Intent()
        builder.setContentIntent(
            PendingIntent.getActivity(
                service,
                PLAYER_PENDING_INTENT_ID, notificationIntent, 0
            )
        )
        notify(builder.build())
    }

    private fun notify(notification: Notification) {
        notificationManager.notify(NOTIFICATION_ID, notification)
        if (!hasStarted) {
            service.startForeground(NOTIFICATION_ID, notification)
            hasStarted = true
        }
    }

    private fun createBuilder(): NotificationCompat.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(service, notificationChannels.primaryChannel)
        } else {
            @Suppress("DEPRECATION")
            NotificationCompat.Builder(service)
        }
    }

}
