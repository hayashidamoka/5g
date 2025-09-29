package com.teampansaru.fiveg

import android.Manifest.permission.READ_PHONE_STATE
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.appwidget.AppWidgetManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.teampansaru.fiveg.compose.WalkthroughScreen

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1000
    }

    // ViewPager関連の変数は不要になったが、将来の参照のためコメントアウト
    // lateinit var viewPager: ViewPager
    // lateinit var viewPagerAdapter : CustomAdapter
    // lateinit var indicatorArea : LinearLayout
    // private var indicatorViewList : ArrayList<View> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Jetpack Composeを使用してUIを設定
        setContent {
            MaterialTheme {
                Surface {
                    WalkthroughScreen(
                        onPermissionRequest = {
                            checkPermission()
                        }
                    )
                }
            }
        }

        // パーミッションチェック
        checkPermission()

        // ウィジェットが存在する場合、NetworkServiceを起動
        checkAndStartServiceForWidget()
    }

    private fun checkAndStartServiceForWidget() {
        try {
            // ウィジェットが存在するかチェック
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val widgetComponent = ComponentName(this, DancingOldmanWidget::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)

            if (widgetIds.isNotEmpty()) {
                // ウィジェットが存在する場合、サービスを起動
                val serviceIntent = Intent(this, NetworkService::class.java).apply {
                    action = NetworkService.INIT
                }
                startForegroundService(serviceIntent)
                android.util.Log.d("MainActivity", "NetworkService started for widget")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to start NetworkService", e)
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
            // 5G検出はComposeのNetworkStatusIndicatorで行うため、ここでは何もしない
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
                // 5G検出はComposeのNetworkStatusIndicatorで行うため、ここでは何もしない
            } else {
                // パーミッションのリクエストに対して許可せずアプリに戻った場合、ここが走る
                finish()
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }



    // 5G検出はComposeのNetworkStatusIndicatorで行うため、このメソッドは不要
    // private fun getNetworkType() { ... }
}
