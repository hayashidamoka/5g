package com.teampansaru.fiveg

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat

class NetworkService : Service() {

    // 今何枚目の画像か　R.drawable.dance1 から始まるので1で初期化
    private var currentDanceIndex = 1

    // 5Gエリアに入ったフラグ
    private var isFiveg = false

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val builder = createNotification()
        startForeground(100, builder.build())
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        createNotificationChannel(getString(R.string.fiveg))
        if(INIT == intent.action) {
            startNetworkMonitoring()
        }
        updateWidget(intent)
        return super.onStartCommand(intent, flags, startId)
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

    private fun createNotificationChannel(channelName: String): String {
        val channelId = "fiveg"
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

    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: NetworkCallback? = null

    companion object {
        private const val TAG = "NetworkMonitorService"

        const val INIT = "INIT"
        private const val OYAJI_CLICKED = "OYAJI_CLICKED"
        // 最後の踊り画像の数字(R.drawable.dance? の一番大きい数字を設定する)
        private const val FINAL_DANCE_INDEX = 53
    }

    override fun onDestroy() {
        super.onDestroy()
        stopNetworkMonitoring()
    }

    private fun startNetworkMonitoring() {
        if (!isPermissionGranted) {
            return
        }
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : NetworkCallback() {
            @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(TAG, "Network available: $network")
                checkNetworkChange()
            }

            @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d(TAG, "Network lost: $network")
                checkNetworkChange()
            }

            @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                Log.d(TAG, "Network capabilities changed: $network")
                checkNetworkChange()
            }
        }

        networkCallback?.let {
            connectivityManager.registerNetworkCallback(networkRequest, it)
        }
    }

    private fun stopNetworkMonitoring() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun checkNetworkChange() {
        if (is5g()) {
            if(!isFiveg) {
                Log.d(TAG, "Network type changed: 5G")
                isFiveg = true
                // 構えのポーズ
                updateWidget(R.drawable.fiveg)
            }
        } else {
            Log.d(TAG, "Network type changed: not 5G")
            // 棒立ち
            updateWidget(R.drawable.other)
            isFiveg = false
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun is5g(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                false
            }
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

                return telephonyManager.dataNetworkType == TelephonyManager.NETWORK_TYPE_NR
            }
            else -> false
        }
    }

    private fun createNotification(): NotificationCompat.Builder {
        return NotificationCompat.Builder(
            applicationContext,
            createNotificationChannel(getString(R.string.fiveg))
        )
            .setSmallIcon(R.mipmap.ic_stat_fiveg_notification_icon)
            .setContentTitle(getString(R.string.fiveg))
            .setContentText(getString(R.string.fiveg_notification_text))
            .setPriority(NotificationManager.IMPORTANCE_LOW)
    }
}