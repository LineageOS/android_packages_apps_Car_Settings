/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.car.settings.wifi;

import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settingslib.wifi.AccessPoint;

import com.android.car.settings.CarSettingActivity;
import com.android.car.settings.R;

/**
 * Shows details about a wifi network, including actions related to the network,
 * e.g. ignore, disconnect, etc. The intent should include information about
 * access point, use that to render UI, e.g. show SSID etc.
 */
public class WifiDetailActivity extends CarSettingActivity {
    private static final String TAG = "WifiDetailActivity";
    private AccessPoint mAccessPoint;
    private WifiManager mWifiManager;
    private final WifiManager.ActionListener mForgetListener =  new WifiManager.ActionListener() {
        @Override
        public void onSuccess() {
        }
        @Override
        public void onFailure(int reason) {
            Toast.makeText(WifiDetailActivity.this,
                    R.string.wifi_failed_forget_message,
                    Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWifiManager = (WifiManager) getSystemService(WifiManager.class);
        showMenuIcon();
        setContentView(R.layout.wifi_details);
        mAccessPoint = new AccessPoint(this, getIntent().getExtras());
        TextView wifiNameView = (TextView) findViewById(R.id.wifi_ssid);
        TextView wifiSummaryView = (TextView) findViewById(R.id.wifi_summary);
        wifiNameView.setText(mAccessPoint.getSsid());
        wifiSummaryView.setText(mAccessPoint.getSummary());
        findViewById(R.id.wifi_forget).setOnClickListener(v -> {
                forget();
                finish();
            });
    }

    private void forget() {
        if (!mAccessPoint.isSaved()) {
            if (mAccessPoint.getNetworkInfo() != null &&
                    mAccessPoint.getNetworkInfo().getState() != State.DISCONNECTED) {
                // Network is active but has no network ID - must be ephemeral.
                mWifiManager.disableEphemeralNetwork(
                        AccessPoint.convertToQuotedString(mAccessPoint.getSsidStr()));
            } else {
                // Should not happen, but a monkey seems to trigger it
                Log.e(TAG, "Failed to forget invalid network " + mAccessPoint.getConfig());
                return;
            }
        } else {
            mWifiManager.forget(mAccessPoint.getConfig().networkId, mForgetListener);
        }
    }
}
