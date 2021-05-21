package jp.co.pannacotta.fiveg

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
        // There may be multiple widgets active, so update all of them
        intent = Intent(context, NetworkService::class.java)
        intent?.also {
            it.action = NetworkService.INIT
        }
        context.startForegroundService(intent)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}