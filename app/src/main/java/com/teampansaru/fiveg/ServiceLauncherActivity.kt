package com.teampansaru.fiveg

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * ウィジェット設置時の通知からServiceを起動するための専用Activity
 * UIは表示せず、Serviceを起動後すぐに終了する
 */
class ServiceLauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // NetworkServiceを起動
        try {
            val serviceIntent = Intent(this, NetworkService::class.java).apply {
                action = NetworkService.INIT
            }
            startForegroundService(serviceIntent)
            android.util.Log.d("ServiceLauncherActivity", "NetworkService started from notification")
        } catch (e: Exception) {
            android.util.Log.e("ServiceLauncherActivity", "Failed to start NetworkService", e)
        }

        // Activityを即座に終了
        finish()
    }
}
