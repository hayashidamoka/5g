package jp.co.pannacotta.fiveg;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;

public class NetworkService extends Service {
    public final String OYAJI_CLICKED = "jp.co.pannacotta.fiveg.OYAJI_CLICKED";
    private final static int FINAL_DANCE_INDEX = 2;
    private int currentDanceIndex = 1;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        RemoteViews remoteViews = new RemoteViews(getApplicationContext().getPackageName(), R.layout.dancing_oldman_widget);

        if(intent.getAction().equals("INIT")){
            Log.d("ろぐ", "いにっと");
            remoteViews.setImageViewResource(R.id.oyaji_image_view, R.drawable.other);
            Intent clickIntent = new Intent(getApplicationContext() , this.getClass());	//明示的インテント
            clickIntent.setAction(OYAJI_CLICKED);
            PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, clickIntent, 0);
            remoteViews.setOnClickPendingIntent(R.id.transparent_button, pendingIntent);
        }

        if(intent.getAction().equals(OYAJI_CLICKED)) {
            remoteViews.setImageViewResource(R.id.oyaji_image_view, getApplicationContext().getResources().getIdentifier("dance" + currentDanceIndex, "drawable", getApplicationContext().getPackageName()));
            if(currentDanceIndex == FINAL_DANCE_INDEX) {
                currentDanceIndex = 1;
            } else {
                currentDanceIndex++;
            }
            Log.d("ろぐ", "くりっく" + currentDanceIndex);
        }

        AppWidgetManager manager = AppWidgetManager.getInstance(getApplicationContext());
        ComponentName widgetId = new ComponentName(getApplicationContext(), DancingOldmanWidget.class);
        manager.updateAppWidget(widgetId, remoteViews);

        return super.onStartCommand(intent, flags, startId);
    }
}
