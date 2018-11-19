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
import com.android.car.settings.common.Logger;
import com.android.settingslib.wifi.AccessPoint;

/**
 * Shows frequency info about the Wifi connection.
 */
public class WifiFrequencyPreferenceController extends ActiveWifiDetailPreferenceControllerBase {
    private static final Logger LOG = new Logger(WifiFrequencyPreferenceController.class);

    public WifiFrequencyPreferenceController(
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
        final int frequency = mWifiInfoProvider.getWifiInfo().getFrequency();
        String band = null;
        if (frequency >= AccessPoint.LOWER_FREQ_24GHZ
                && frequency < AccessPoint.HIGHER_FREQ_24GHZ) {
            band = mContext.getResources().getString(R.string.wifi_band_24ghz);
        } else if (frequency >= AccessPoint.LOWER_FREQ_5GHZ
                && frequency < AccessPoint.HIGHER_FREQ_5GHZ) {
            band = mContext.getResources().getString(R.string.wifi_band_5ghz);
        } else {
            LOG.e("Unexpected frequency " + frequency);
        }
        mWifiDetailPreference.setDetailText(band);
    }
}
