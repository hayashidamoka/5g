package com.teampansaru.fiveg

import android.Manifest
import android.Manifest.permission.READ_PHONE_STATE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyDisplayInfo.*
import android.telephony.TelephonyManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission()
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // API30以上かどうか
            if (ContextCompat.checkSelfPermission(
                    this,
                    READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(READ_PHONE_STATE),
                    1000
                )
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        READ_PHONE_STATE
                    )
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(READ_PHONE_STATE),
                        1000
                    )
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        1000
                    )
                }
            } else {
                // SDKバージョンが問題なく、全てのパーミッションが取れている場合
                getNetworkType()
            }
        } else {
            Toast.makeText(this, "APIレベルがたりません。", Toast.LENGTH_SHORT).show()
        }
    }

    private fun permissionIntent() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1000) {
            // requestPermissionsで設定した順番で結果が格納されている
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 許可された(されている)ので処理を続行
                getNetworkType()
            } else {
                // パーミッションのリクエストに対して許可せずアプリに戻った場合、ここが走る
                Toast.makeText(this, "パーミッションが許可されていません。", Toast.LENGTH_SHORT).show()
                // FIXME 再度ダイアログを出すかIntentで走るか。
//                permissionIntent()
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun getNetworkType() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(object : PhoneStateListener() {

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                if (ActivityCompat.checkSelfPermission(
                        applicationContext,
                        READ_PHONE_STATE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                super.onDisplayInfoChanged(telephonyDisplayInfo)

                val scope = CoroutineScope(Job() + Dispatchers.Main)
                scope.launch {
                    // 5Gネットワークに接続しているか判別
                    val label = findViewById<TextView>(R.id.network_type_label)
                    label.text = when (telephonyDisplayInfo.overrideNetworkType) {
                        OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> {
                            replaceText(label, "LTE Advanced Pro (5Ge)")
                            "LTE Advanced Pro (5Ge)"
                        }
                        OVERRIDE_NETWORK_TYPE_NR_NSA -> {
                            replaceText(label, "5G NR（Sub-6）network")
                            "5G NR（Sub-6）network"
                        }
                        OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE -> {
                            replaceText(label, "5G mmWave（5G+ / 5G UW）network")
                            "5G mmWave（5G+ / 5G UW）network"
                        }
                        OVERRIDE_NETWORK_TYPE_LTE_CA -> {
                            replaceText(label, "LTE")
                            "LTE"
                        }
                        else -> "other:${telephonyDisplayInfo.overrideNetworkType}"
                    }
                }
            }
        }, PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED)
    }

    private fun replaceText(label: TextView, text: String) {
        Thread {
            label.text = text
        }.start()
    }
}
