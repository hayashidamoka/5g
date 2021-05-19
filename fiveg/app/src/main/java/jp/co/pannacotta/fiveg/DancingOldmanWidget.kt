package jp.co.pannacotta.fiveg

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast

/**
 * Implementation of App Widget functionality.
 */
class DancingOldmanWidget : AppWidgetProvider() {
    companion object {
        const val OYAJI_CLICKED = "jp.co.pannacotta.fiveg.OYAJI_CLICKED"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            val widgetId = ComponentName(context, DancingOldmanWidget::class.java)
            updateAppWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        Log.d("ろぐ", "おんれしーぶ")
        Toast.makeText(context, "おんれしーぶ", Toast.LENGTH_SHORT).show()
        var action = ""
        intent?.also {
            action = it.action.toString()
        }
        if(action == OYAJI_CLICKED) {
            Log.d("ろぐ", "たっぷ")
            Toast.makeText(context, "たっぷ", Toast.LENGTH_SHORT).show()
            context?.let {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val views = RemoteViews(it.packageName, R.layout.dancing_oldman_wdget)
                views.setImageViewResource(R.id.oyaji_image_view, R.drawable.fiveg)
                val widgetId = ComponentName(it, DancingOldmanWidget::class.java)
                appWidgetManager.updateAppWidget(widgetId, views)
            }

        }
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: ComponentName
) {
    val widgetText = context.getString(R.string.appwidget_text)
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.dancing_oldman_wdget)

    val clickIntent = Intent(context, DancingOldmanWidget::class.java)
    clickIntent.action = DancingOldmanWidget.OYAJI_CLICKED
    val clickPendingIntent: PendingIntent = PendingIntent.getBroadcast(context, 0, clickIntent, 0)
    views.setOnClickPendingIntent(R.id.transparent_button, clickPendingIntent)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}