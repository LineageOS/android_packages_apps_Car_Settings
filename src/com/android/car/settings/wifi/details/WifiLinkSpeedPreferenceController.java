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
package com.android.car.settings.wifi.details;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;

/**
 * Shows info about Wifi link speed info.
 */
public class WifiLinkSpeedPreferenceController extends ActiveWifiDetailPreferenceControllerBase {

    public WifiLinkSpeedPreferenceController(
            Context context, String preferenceKey, FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
    }

    @Override
    public void onWifiChanged(NetworkInfo networkInfo, WifiInfo wifiInfo) {
        super.onWifiChanged(networkInfo, wifiInfo);
        updateIfAvailable();
    }

    @Override
    public void onWifiConfigurationChanged(WifiConfiguration wifiConfiguration,
            NetworkInfo networkInfo, WifiInfo wifiInfo) {
        super.onWifiConfigurationChanged(wifiConfiguration, networkInfo, wifiInfo);
        updateIfAvailable();
    }

    @Override
    protected void updateInfo() {
        int linkSpeedMbps = mWifiInfoProvider.getWifiInfo().getLinkSpeed();
        mWifiDetailPreference.setVisible(linkSpeedMbps >= 0);
        mWifiDetailPreference.setDetailText(mContext.getString(
                R.string.link_speed, mWifiInfoProvider.getWifiInfo().getLinkSpeed()));

    }
}
