package com.teampansaru.fiveg

import android.Manifest.permission.READ_PHONE_STATE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.viewpager.widget.ViewPager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1000
    }

    lateinit var viewPager: ViewPager
    lateinit var viewPagerAdapter : CustomAdapter
    lateinit var indicatorArea : LinearLayout
    private var indicatorViewList : ArrayList<View> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission()

        /**
         * WalkThrough
         */
        viewPager = findViewById(R.id.viewPager)
        viewPagerAdapter = CustomAdapter(supportFragmentManager)
        viewPager.adapter = viewPagerAdapter
        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener(){
            override fun onPageSelected(position: Int) {
                for (i in 0 until indicatorViewList.size){
                    if (i == position)
                        indicatorViewList[i].background = ResourcesCompat.getDrawable(resources,R.drawable.indicator_active, null)
                    else
                        indicatorViewList[i].background = ResourcesCompat.getDrawable(resources,R.drawable.indicator_inactive, null)

                }
            }
        })
        indicatorArea = findViewById(R.id.indicator_area)
        val indicatorWidth = resources.getDimension(R.dimen.walk_through_indicator_size).toInt()
        val indicatorHeight = resources.getDimension(R.dimen.walk_through_indicator_size).toInt()
        val indicatorMarginStart = resources.getDimension(R.dimen.walk_through_indicator_margin_start).toInt()
        for (i in 0 until WalkThroughType.entries.size){
            var view = View(this)
            if (i == 0){
                view.background = AppCompatResources.getDrawable(this, R.drawable.indicator_active)
                val layoutParams = LinearLayout.LayoutParams(indicatorWidth, indicatorHeight)
                view.layoutParams = layoutParams
            }else {
                view.background = AppCompatResources.getDrawable(this, R.drawable.indicator_inactive)
                val layoutParams = LinearLayout.LayoutParams(indicatorWidth,indicatorHeight)
                layoutParams.marginStart = indicatorMarginStart
                view.layoutParams = layoutParams
            }
            indicatorArea.addView(view)
            indicatorViewList.add(view)
        }

    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // パーミッション説明ダイアログを表示
            showPermissionExplanationDialog()
        } else {
            // パーミッションが既に許可されている場合
            getNetworkType()
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_dialog_title))
            .setMessage(getString(R.string.permission_dialog_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.permission_dialog_ok)) { _, _ ->
                if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        READ_PHONE_STATE
                    )
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(READ_PHONE_STATE),
                        PERMISSION_REQUEST_CODE
                    )
                } else {
                    // 権限が以前に拒否された場合は直接リクエスト
                    permissionIntent()
                }
            }
            .setNegativeButton(getString(R.string.permission_dialog_close)) { _, _ ->
                // 閉じるボタンが押されたらアプリを終了
                finish()
            }
            .show()
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
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // requestPermissionsで設定した順番で結果が格納されている
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 許可された(されている)ので処理を続行
                getNetworkType()
            } else {
                // パーミッションのリクエストに対して許可せずアプリに戻った場合、ここが走る
                finish()
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }



    private fun getNetworkType() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(object : PhoneStateListener() {

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
                        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> {
                            "LTE Advanced Pro (5Ge)"
                        }
                        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> {
                            "5G NR（Sub-6）network"
                        }
                        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE -> {
                            "5G mmWave（5G+ / 5G UW）network"
                        }
                        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA -> {
                            "LTE"
                        }
                        else -> "other:${telephonyDisplayInfo.overrideNetworkType}"
                    }
                }
            }
        }, PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED)
    }
}
