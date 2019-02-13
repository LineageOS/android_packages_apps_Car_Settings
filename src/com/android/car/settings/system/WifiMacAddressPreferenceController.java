/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.settings.system;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import androidx.preference.Preference;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;

/** Updates the vehicle Wi-Fi mac address summary. */
public class WifiMacAddressPreferenceController extends PreferenceController<Preference> {

    private WifiInfo mWifiInfo;

    public WifiMacAddressPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        init(context);
    }

    @Override
    protected Class<Preference> getPreferenceType() {
        return Preference.class;
    }

    @Override
    protected int getAvailabilityStatus() {
        if (!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)
                || !getContext().getResources().getBoolean(R.bool.config_show_wifi_mac_address)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return super.getAvailabilityStatus();
    }

    protected void init(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mWifiInfo = wifiManager.getConnectionInfo();
    }

    @Override
    protected void updateState(Preference preference) {
        String macAddress = mWifiInfo == null ? getContext().getString(R.string.status_unavailable)
                : mWifiInfo.getMacAddress();
        preference.setSummary(macAddress);
    }
}
