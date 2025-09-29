package com.teampansaru.fiveg

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

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

        // NetworkServiceを起動（アプリが起動していない場合はスキップ）
        // Android 12以降の制限により、バックグラウンドからフォアグラウンドサービスを起動できない
        // MainActivityから起動するように変更
        try {
            intent = Intent(context, NetworkService::class.java)
            intent?.also {
                it.action = NetworkService.INIT
            }
            // サービスが既に起動している場合のみ更新を試みる
            context.startService(intent)
        } catch (e: Exception) {
            // サービス起動に失敗した場合は無視（MainActivityから起動される）
            android.util.Log.d("DancingOldmanWidget", "Service start failed: ${e.message}")
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}