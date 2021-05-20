package jp.co.pannacotta.fiveg;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
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

    private TextView networkTypeTextView;

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        networkTypeTextView = findViewById(R.id.network_type_label);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startListening();
    }

    private void updateUI(){
        Log.d("ろぐ", "updateUI");
        networkTypeTextView.setText(mCurrentOverrideNetworkType);
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
            Log.d("ろぐ", "onActiveDataSubscriptionIdChanged: subId=" + subId);
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

            updateUI();
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

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateUI();
                }
            });
        }

    };

    private final PhoneStateListener mDataPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onDisplayInfoChanged(@NonNull TelephonyDisplayInfo telephonyDisplayInfo) {
            String networkType;
            switch (telephonyDisplayInfo.getOverrideNetworkType()) {
                case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE:
                    networkType = "NONE";
                    break;
                case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA:
                    networkType = "LTE-CA";
                    break;
                case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO:
                    networkType = "LTE-ADV-PRO";
                    break;
                case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA:
                    networkType = "NR-NSA";
                    break;
                case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE:
                    networkType = "NR-NSA-MMWAVE";
                    break;
                default:
                    networkType = "-";
                    break;
            }
            Log.d("ろぐ", "onDisplayInfoChanged: overrideNetworkType=" + networkType);
            mCurrentOverrideNetworkType = networkType;

            updateUI();
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
}
