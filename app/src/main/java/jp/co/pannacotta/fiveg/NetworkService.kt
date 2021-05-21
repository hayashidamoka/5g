package jp.co.pannacotta.fiveg;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class NetworkService extends Service {
    private TelephonyManager telephonyManager;
    // TelephonyManagerインスタンス(データ通信SIM用)
    private TelephonyManager telephonyManagerForData;

    private String mCurrentOverrideNetworkType = "NONE";

    // 最新のSubIdの値
    private int mCurrentSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    // Capabilityフラグ関係
    private boolean mHasTemporarilyNotMetered = false;
    private boolean mHasNotMetered = false;

    // BandWidth関係
    private int mBandWidthDown = 0;
    private int mBandWidthUp = 0;

    // widget関連
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
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), createNotificationChannel("fiveg", "oyaji"))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("たいとる")
                .setContentText("こんてんと")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        startForeground(100, builder.build());
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        startListening();
        updateWidget(intent);

        return super.onStartCommand(intent, flags, startId);
    }

    private void startListening() {

        if (!isPermissionGranted()) {
            return;
        }

        // データ通信用SIMのTelephonyManagerインスタンスを取得
        // (Pause中にユーザがデータ通信用SIMを変更した可能性があるため)
        createTelephonyManagerForData(SubscriptionManager.getDefaultDataSubscriptionId());

        // PhoneStateListenerの監視開始
        telephonyManager.listen(mBasePhoneStateListener, PhoneStateListener.LISTEN_ACTIVE_DATA_SUBSCRIPTION_ID_CHANGE);
        startListenPhoneStateForData();

        // NetworkCallbackの監視開始
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
            connectivityManager.registerNetworkCallback(builder.build(), mNetworkCallback);
        }
    }

    private final PhoneStateListener mBasePhoneStateListener = new PhoneStateListener() {
        @Override
        public void onActiveDataSubscriptionIdChanged(int subId) {
            Log.d("ろぐ", "subId=" + subId);
            if (mCurrentSubId == subId) {
                // データ通信用subIdに変化がない場合は処理を行わない
                return;
            }
            mCurrentSubId = subId;

            // データ通信用subIdに変化があった場合、データ通信SIM用TelephonyManagerの再生成とPhoneStateListenerの監視やり直し
            stopListenPhoneStateForData();
            createTelephonyManagerForData(subId);
            startListenPhoneStateForData();

            if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                // データ通信できない状態の場合、最新のOverrideNetworkTypeの値をリセット
                mCurrentOverrideNetworkType = "NONE";
            }
        }
    };

    private final ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onCapabilitiesChanged(@NonNull Network network, NetworkCapabilities networkCapabilities) {
            Log.d("ろぐ", "onCapabilitiesChanged network=" + network + ", capa=" + networkCapabilities);
            // モバイル通信関係だけ拾いたいので念のため判定
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                mHasNotMetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
                mHasTemporarilyNotMetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED);

                mBandWidthDown = networkCapabilities.getLinkDownstreamBandwidthKbps();
                mBandWidthUp = networkCapabilities.getLinkUpstreamBandwidthKbps();
            }
        }
    };

    private final PhoneStateListener mDataPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onDisplayInfoChanged(@NonNull TelephonyDisplayInfo telephonyDisplayInfo) {

            int overrideNetworkType = telephonyDisplayInfo.getOverrideNetworkType();
            if (overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA ||
                    overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE) {
                // 構え画像
                currentDanceIndex = 1;
                updateWidget(R.drawable.fiveg);
            } else {
                // 棒立ち画像
                updateWidget(R.drawable.other);
            }
        }
    };

    // データ通信SIM用TelephonyManagerインスタンス生成
    private void createTelephonyManagerForData(int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            telephonyManagerForData = null;
        } else {
            telephonyManagerForData = telephonyManager.createForSubscriptionId(subId);
        }
    }

    // データ通信SIM用PhoneStateListenerの監視開始
    private void startListenPhoneStateForData() {
        if (telephonyManagerForData == null) {
            return;
        }
        telephonyManagerForData.listen(mDataPhoneStateListener, PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED);
    }

    // データ通信SIM用PhoneStateListenerの監視停止
    private void stopListenPhoneStateForData() {
        if (telephonyManagerForData == null) {
            return;
        }
        telephonyManagerForData.listen(mDataPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    private boolean isPermissionGranted() {
        return this.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    private void updateWidget(Intent intent) {
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
    }

    private void updateWidget(int imageResId) {
        RemoteViews remoteViews = new RemoteViews(getApplicationContext().getPackageName(), R.layout.dancing_oldman_widget);
        remoteViews.setImageViewResource(R.id.oyaji_image_view, imageResId);
        AppWidgetManager manager = AppWidgetManager.getInstance(getApplicationContext());
        ComponentName widgetId = new ComponentName(getApplicationContext(), DancingOldmanWidget.class);
        manager.updateAppWidget(widgetId, remoteViews);
    }

    private String createNotificationChannel(String channelId, String channelName){
        NotificationChannel channel = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(channel);
        return channelId;
    }
}
