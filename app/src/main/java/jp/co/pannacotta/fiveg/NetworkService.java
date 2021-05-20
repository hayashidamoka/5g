package jp.co.pannacotta.fiveg;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;

public class NetworkService extends Service {
    public final String OYAJI_CLICKED = "jp.co.pannacotta.fiveg.OYAJI_CLICKED";
    private RemoteViews remoteviews;
    private Context context;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getAction().equals("INIT")){
            Log.d("ろぐ", "いにっと");
            init();
        }
        /*
         * イベント受信処理をここに記述する
         */
        if(intent.getAction().equals(OYAJI_CLICKED)) {
            Log.d("ろぐ", "くりっく");
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.dancing_oldman_widget);
            views.setImageViewResource(R.id.oyaji_image_view, R.drawable.anim1);
        }

        //ウィジェット画面の更新処理　：　必須
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName widgetId = new ComponentName(context, DancingOldmanWidget.class);
        manager.updateAppWidget(widgetId, remoteviews);

        return super.onStartCommand(intent, flags, startId);
    }

    private void init(){
        this.context = getApplicationContext();
        remoteviews = new RemoteViews(getPackageName(),R.layout.dancing_oldman_widget);
        remoteviews.setImageViewResource(R.id.oyaji_image_view, R.drawable.fiveg);
        /*
         * 明示的インテント
         * マニフェストへの記述不要
         */
        Intent intent = new Intent(context , this.getClass());	//明示的インテント
        intent.setAction(OYAJI_CLICKED);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        remoteviews.setOnClickPendingIntent(R.id.transparent_button, pendingIntent);
    }
}
