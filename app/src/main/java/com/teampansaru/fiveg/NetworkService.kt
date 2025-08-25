package com.teampansaru.fiveg

import android.Manifest
import android.app.*
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.SubscriptionManager
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

class NetworkService : Service() {
    companion object {
        const val INIT = "INIT"
        private const val OYAJI_CLICKED = "OYAJI_CLICKED"
        // 最後の踊り画像の数字(R.drawable.dance? の一番大きい数字を設定する)
        private const val FINAL_DANCE_INDEX = 53
    }

    // 今何枚目の画像か　R.drawable.dance1 から始まるので1で初期化
    private var currentDanceIndex = 1

    // 5Gエリアに入ったフラグ
    private var isFiveg = false

    // 見る必要ない
    private var telephonyManager: TelephonyManager? = null
    private var telephonyManagerForData: TelephonyManager? = null
    private var mCurrentSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    private var mHasTemporarilyNotMetered = false
    private var mHasNotMetered = false
    private var mBandWidthDown = 0
    private var mBandWidthUp = 0

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val builder = NotificationCompat.Builder(
            applicationContext,
            createNotificationChannel("fiveg", getString(R.string.fiveg))
        )
            .setSmallIcon(R.mipmap.ic_stat_fiveg_notification_icon)
            .setContentTitle(getString(R.string.fiveg))
            .setContentText(getString(R.string.fiveg_notification_text))
            .setPriority(NotificationManager.IMPORTANCE_LOW)
        startForeground(100, builder.build())
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        if(INIT == intent.action) {
            startListening()
        }
        updateWidget(intent)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startListening() {
        if (!isPermissionGranted) {
            return
        }

        // データ通信用SIMのTelephonyManagerインスタンスを取得
        // (Pause中にユーザがデータ通信用SIMを変更した可能性があるため)
        createTelephonyManagerForData(SubscriptionManager.getDefaultDataSubscriptionId())

        // PhoneStateListenerの監視開始
        telephonyManager?.listen(
            mBasePhoneStateListener,
            PhoneStateListener.LISTEN_ACTIVE_DATA_SUBSCRIPTION_ID_CHANGE
        )
        startListenPhoneStateForData()

        // NetworkCallbackの監視開始
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val builder = NetworkRequest.Builder()
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        connectivityManager.registerNetworkCallback(builder.build(), mNetworkCallback)
    }

    private val mBasePhoneStateListener: PhoneStateListener = object : PhoneStateListener() {
        override fun onActiveDataSubscriptionIdChanged(subId: Int) {
            if (mCurrentSubId == subId) {
                // データ通信用subIdに変化がない場合は処理を行わない
                return
            }
            mCurrentSubId = subId

            // データ通信用subIdに変化があった場合、データ通信SIM用TelephonyManagerの再生成とPhoneStateListenerの監視やり直し
            stopListenPhoneStateForData()
            createTelephonyManagerForData(subId)
            startListenPhoneStateForData()
            if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                // データ通信できない状態の場合、棒立ちになってもらう
                isFiveg = false
                updateWidget(R.drawable.other)
            }
        }
    }
    private val mNetworkCallback: NetworkCallback = object : NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
//            Log.d("ろぐ", "onCapabilitiesChanged network=$network, capa=$networkCapabilities")
            // モバイル通信関係だけ拾いたいので念のため判定
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                mHasNotMetered =
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                mHasTemporarilyNotMetered =
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED)
                mBandWidthDown = networkCapabilities.linkDownstreamBandwidthKbps
                mBandWidthUp = networkCapabilities.linkUpstreamBandwidthKbps
            }
        }
    }
    private val mDataPhoneStateListener: PhoneStateListener = object : PhoneStateListener() {
        override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
            val overrideNetworkType = telephonyDisplayInfo.overrideNetworkType
            if (overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA ||
                overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE
            ) {
                // 構え画像
                if (!isFiveg) {
                    isFiveg = true
                    currentDanceIndex = 1
                    updateWidget(R.drawable.fiveg)
                }
            } else {
                // 棒立ち画像
                updateWidget(R.drawable.other)
                isFiveg = false
            }
        }
    }

    // データ通信SIM用TelephonyManagerインスタンス生成
    private fun createTelephonyManagerForData(subId: Int) {
        telephonyManagerForData = if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            null
        } else {
            telephonyManager!!.createForSubscriptionId(subId)
        }
    }

    // データ通信SIM用PhoneStateListenerの監視開始
    private fun startListenPhoneStateForData() {
        if (telephonyManagerForData == null) {
            return
        }
        telephonyManagerForData!!.listen(
            mDataPhoneStateListener,
            PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED
        )
    }

    // データ通信SIM用PhoneStateListenerの監視停止
    private fun stopListenPhoneStateForData() {
        if (telephonyManagerForData == null) {
            return
        }
        telephonyManagerForData!!.listen(mDataPhoneStateListener, PhoneStateListener.LISTEN_NONE)
    }

    private val isPermissionGranted: Boolean
        get() = checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

    private fun updateWidget(intent: Intent) {
        val remoteViews =
            RemoteViews(applicationContext.packageName, R.layout.dancing_oldman_widget)
        if (intent.action == INIT) {
            remoteViews.setImageViewResource(R.id.oyaji_image_view, R.drawable.other)
            val clickIntent = Intent(applicationContext, this.javaClass) //明示的インテント
            clickIntent.action = OYAJI_CLICKED
            val pendingIntent = PendingIntent.getService(applicationContext, 0, clickIntent, PendingIntent.FLAG_IMMUTABLE)
            remoteViews.setOnClickPendingIntent(R.id.transparent_button, pendingIntent)
        }
        if (intent.action == OYAJI_CLICKED) {
            if(!isFiveg) {
                return
            }
            remoteViews.setImageViewResource(
                R.id.oyaji_image_view, applicationContext.resources.getIdentifier(
                    "dance$currentDanceIndex", "drawable", applicationContext.packageName
                )
            )
            if (currentDanceIndex == FINAL_DANCE_INDEX) {
                currentDanceIndex = 1
            } else {
                currentDanceIndex++
            }
        }
        val manager = AppWidgetManager.getInstance(applicationContext)
        val widgetId = ComponentName(applicationContext, DancingOldmanWidget::class.java)
        manager.updateAppWidget(widgetId, remoteViews)
    }

    private fun updateWidget(imageResId: Int) {
        val remoteViews =
            RemoteViews(applicationContext.packageName, R.layout.dancing_oldman_widget)
        remoteViews.setImageViewResource(R.id.oyaji_image_view, imageResId)
        val manager = AppWidgetManager.getInstance(applicationContext)
        val widgetId = ComponentName(applicationContext, DancingOldmanWidget::class.java)
        manager.updateAppWidget(widgetId, remoteViews)
    }

    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val channel = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service =
            applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return channelId
    }
}