/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.car.settings.qc;

import static com.android.car.qc.QCItem.QC_ACTION_TOGGLE_STATE;
import static com.android.car.settings.qc.SettingsQCRegistry.WIFI_TILE_URI;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.wifi.WifiManager;

import com.android.car.qc.QCItem;
import com.android.car.qc.QCTile;
import com.android.car.settings.R;
import com.android.car.settings.wifi.WifiUtil;

/**
 * QCItem for showing a wifi toggle.
 */
public class WifiTile extends SettingsQCItem {
    private final WifiManager mWifiManager;

    public WifiTile(Context context) {
        super(context);
        mWifiManager = context.getSystemService(WifiManager.class);
    }

    @Override
    QCItem getQCItem() {
        boolean wifiEnabled = mWifiManager.isWifiEnabled();
        Icon icon = Icon.createWithResource(getContext(), R.drawable.ic_qc_wifi);

        return new QCTile.Builder()
                .setIcon(icon)
                .setChecked(wifiEnabled)
                .setAction(getBroadcastIntent())
                .setSubtitle(getSubtitle())
                .build();
    }

    @Override
    Uri getUri() {
        return WIFI_TILE_URI;
    }

    @Override
    void onNotifyChange(Intent intent) {
        boolean newState = intent.getBooleanExtra(QC_ACTION_TOGGLE_STATE,
                !mWifiManager.isWifiEnabled());
        mWifiManager.setWifiEnabled(newState);
    }

    @Override
    Class getBackgroundWorkerClass() {
        return WifiTileWorker.class;
    }

    private String getSubtitle() {
        int wifiState = mWifiManager.getWifiState();
        int stringId = WifiUtil.getStateDesc(wifiState);
        if (stringId != 0) {
            return getContext().getString(stringId);
        }
        if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
            String wifiName = mWifiManager.getConnectionInfo().getSSID();
            if (wifiName.equals(WifiManager.UNKNOWN_SSID)) {
                return getContext().getString(R.string.wifi_disconnected);
            }
            return cleanWifiName(wifiName);
        }
        return getContext().getString(R.string.wifi_disabled);
    }

    private String cleanWifiName(String name) {
        if (name != null && name.length() >= 2 && name.startsWith("\"") && name.endsWith("\"")) {
            return name.substring(1, name.length() - 1);
        }
        return name;
    }
}
