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
package com.android.car.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.android.car.settings.CarSettingActivity;
import com.android.car.settings.R;

/**
 * Shows details about a bluetooth device, including actions related to the device,
 * e.g. forget etc. The intent should include information about the device, use that to
 * render UI, e.g. show name etc.
 */
public class BluetoothDetailActivity extends CarSettingActivity {
    private static final String TAG = "BluetoothDetailActivity";
    public static final String BT_DEVICE_KEY = "btDeviceKey";

    private BluetoothDevice mDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showMenuIcon();
        setContentView(R.layout.bluetooth_details);
        if (getIntent() != null && getIntent().getExtras() != null) {
            mDevice = getIntent().getExtras().getParcelable(BT_DEVICE_KEY);
        }
        if (mDevice == null) {
            Log.w(TAG, "No bluetooth device set.");
            return;
        }
        TextView nameView = (TextView) findViewById(R.id.bt_name);
        nameView.setText(mDevice.getName());
        findViewById(R.id.bt_forget).setOnClickListener(v -> {
                forget();
                finish();
            });
    }

    private void forget() {
        int state = mDevice.getBondState();

        if (state == BluetoothDevice.BOND_BONDING) {
            mDevice.cancelBondProcess();
        }

        if (state != BluetoothDevice.BOND_NONE) {
            mDevice.removeBond();
        }
    }
}
