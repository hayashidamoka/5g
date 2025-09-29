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
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
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

    // TelephonyManager
    private lateinit var telephonyManager: TelephonyManager
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyCallback: Any? = null

    // 現在の5G状態（TelephonyDisplayInfoから）
    private var is5GFromDisplayInfo = false

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val builder = createNotification()
        startForeground(100, builder.build())
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        createNotificationChannel(getString(R.string.fiveg))
        if(INIT == intent.action) {
            startNetworkMonitoring()
            startTelephonyMonitoring()
            Log.d(TAG, "Service initialized with INIT action")
            // INITアクション時はupdateWidgetをスキップ（5Gチェック後に適切な画像を設定）
        } else {
            updateWidget(intent)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private val isPermissionGranted: Boolean
        get() = checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

    private fun updateWidget(intent: Intent) {
        val remoteViews =
            RemoteViews(applicationContext.packageName, R.layout.dancing_oldman_widget)
        if (intent.action == INIT) {
            // INITアクション時は画像を設定せず、クリックリスナーのみ設定
            // 5G状態は別途checkNetworkChangeで設定される
            val clickIntent = Intent(applicationContext, this.javaClass) //明示的インテント
            clickIntent.action = OYAJI_CLICKED
            val pendingIntent = PendingIntent.getService(applicationContext, 0, clickIntent, PendingIntent.FLAG_IMMUTABLE)
            remoteViews.setOnClickPendingIntent(R.id.transparent_button, pendingIntent)
            // ウィジェットを更新（クリックリスナーのみ）
            val manager = AppWidgetManager.getInstance(applicationContext)
            val widgetId = ComponentName(applicationContext, DancingOldmanWidget::class.java)
            manager.updateAppWidget(widgetId, remoteViews)
            return
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

        // 画像を設定
        remoteViews.setImageViewResource(R.id.oyaji_image_view, imageResId)

        // クリックリスナーも必ず設定（重要！）
        val clickIntent = Intent(applicationContext, this.javaClass)
        clickIntent.action = OYAJI_CLICKED
        val pendingIntent = PendingIntent.getService(applicationContext, 0, clickIntent, PendingIntent.FLAG_IMMUTABLE)
        remoteViews.setOnClickPendingIntent(R.id.transparent_button, pendingIntent)

        // ウィジェットを更新
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
        stopTelephonyMonitoring()
    }

    private fun startNetworkMonitoring() {
        if (!isPermissionGranted) {
            return
        }

        // 初期状態をチェック
        checkNetworkChange()

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
        val is5GNow = is5g()

        if (is5GNow) {
            Log.d(TAG, "Network type: 5G")
            // 構えのポーズ
            updateWidget(R.drawable.fiveg)
            isFiveg = true
        } else {
            Log.d(TAG, "Network type: not 5G")
            // 棒立ち
            updateWidget(R.drawable.other)
            isFiveg = false
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun is5g(): Boolean {
        // モバイルネットワークの5G状態をチェック（WiFi使用中でも確認）
        // TelephonyDisplayInfoから取得した5G状態を使用
        // または直接dataNetworkTypeを確認
        val dataNetworkType = telephonyManager.dataNetworkType
        val result = is5GFromDisplayInfo || dataNetworkType == TelephonyManager.NETWORK_TYPE_NR

        // ネットワーク状態をログ出力
        val network = connectivityManager.activeNetwork
        val networkCapabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val transportInfo = when {
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
            else -> "Other/None"
        }

        Log.d(TAG, "is5g: Transport=$transportInfo, is5GFromDisplayInfo=$is5GFromDisplayInfo, dataNetworkType=$dataNetworkType, result=$result")

        return result
    }

    private fun startTelephonyMonitoring() {
        if (!isPermissionGranted) {
            Log.d(TAG, "startTelephonyMonitoring: Permission not granted")
            return
        }

        Log.d(TAG, "startTelephonyMonitoring: Starting telephony monitoring")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12以上
            val callback = object : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
                override fun onDisplayInfoChanged(displayInfo: TelephonyDisplayInfo) {
                    handleDisplayInfoChanged(displayInfo)
                }
            }
            telephonyCallback = callback
            telephonyManager.registerTelephonyCallback(applicationContext.mainExecutor, callback)
            Log.d(TAG, "Registered TelephonyCallback (Android 12+)")
        } else {
            // Android 11以下
            phoneStateListener = object : PhoneStateListener() {
                override fun onDisplayInfoChanged(displayInfo: TelephonyDisplayInfo) {
                    super.onDisplayInfoChanged(displayInfo)
                    handleDisplayInfoChanged(displayInfo)
                }
            }
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED)
            Log.d(TAG, "Registered PhoneStateListener (Android 11-)")
        }

        // 初期状態を取得する（少し遅延を入れる）
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            // 初期チェックを再実行
            Log.d(TAG, "Performing initial 5G check after telephony monitoring setup")
            checkNetworkChange()
        }, 500)
    }

    private fun handleDisplayInfoChanged(displayInfo: TelephonyDisplayInfo) {
        val was5G = is5GFromDisplayInfo

        // 5G判定
        is5GFromDisplayInfo = when (displayInfo.overrideNetworkType) {
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA,
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE,
            5 -> true // OVERRIDE_NETWORK_TYPE_NR_ADVANCED
            else -> displayInfo.networkType == TelephonyManager.NETWORK_TYPE_NR
        }

        Log.d(TAG, "DisplayInfo changed - is5G: $is5GFromDisplayInfo, overrideType: ${displayInfo.overrideNetworkType}, networkType: ${displayInfo.networkType}")

        // 状態が変化した場合、ウィジェットを更新
        if (was5G != is5GFromDisplayInfo) {
            checkNetworkChange()
        }
    }

    private fun stopTelephonyMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (telephonyCallback as? TelephonyCallback)?.let {
                telephonyManager.unregisterTelephonyCallback(it)
            }
        } else {
            phoneStateListener?.let {
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
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