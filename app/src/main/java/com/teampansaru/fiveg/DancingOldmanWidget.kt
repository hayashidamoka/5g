package com.teampansaru.fiveg

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Implementation of App Widget functionality.
 */
class DancingOldmanWidget : AppWidgetProvider() {
    private var intent: Intent? = null

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 初期画像を設定
        val remoteViews = android.widget.RemoteViews(context.packageName, R.layout.dancing_oldman_widget)
        remoteViews.setImageViewResource(R.id.oyaji_image_view, R.drawable.other)

        // すべてのウィジェットインスタンスを更新
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }

        // NetworkServiceの起動は、onEnabled()で表示される通知から行うため、ここでは何もしない
    }

    override fun onEnabled(context: Context) {
        // 最初のウィジェットが作成された時に通知を表示
        showSetupNotification(context)
    }

    override fun onDisabled(context: Context) {
        // 最後のウィジェットが削除された時にNetworkServiceを停止
        try {
            val serviceIntent = Intent(context, NetworkService::class.java)
            context.stopService(serviceIntent)
            android.util.Log.d("DancingOldmanWidget", "NetworkService stopped")
        } catch (e: Exception) {
            android.util.Log.e("DancingOldmanWidget", "Failed to stop NetworkService", e)
        }
    }

    /**
     * ウィジェット設置時の通知を表示
     */
    private fun showSetupNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 通知チャンネルを作成（Android 8.0以降）
        val channel = NotificationChannel(
            context.getString(R.string.widget_setup_notification_channel_id),
            context.getString(R.string.widget_setup_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        // ServiceLauncherActivityを起動するPendingIntentを作成
        val intent = Intent(context, ServiceLauncherActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 通知を作成
        val notification = NotificationCompat.Builder(
            context,
            context.getString(R.string.widget_setup_notification_channel_id)
        )
            .setSmallIcon(R.mipmap.ic_stat_fiveg_notification_icon)
            .setContentTitle(context.getString(R.string.widget_setup_notification_title))
            .setContentText(context.getString(R.string.widget_setup_notification_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // 通知タップ後に自動削除
            .setContentIntent(pendingIntent)
            .build()

        // 通知を表示
        notificationManager.notify(NOTIFICATION_ID, notification)
        android.util.Log.d("DancingOldmanWidget", "Setup notification displayed")
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_REQUEST_CODE = 1001
    }
}