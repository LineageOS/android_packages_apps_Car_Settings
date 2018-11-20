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
import android.net.LinkAddress;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;

import androidx.core.text.BidiFormatter;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.wifi.WifiUtil;

import java.net.Inet6Address;
import java.util.StringJoiner;

/**
 * Shows info about Wifi IPv6 address.
 */
public class WifiIpv6AddressPreferenceController extends WifiControllerBase {
    private Preference mPreference;

    public WifiIpv6AddressPreferenceController(
            Context context, String preferenceKey, FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
    }

    @Override
    public void onWifiChanged(NetworkInfo networkInfo, WifiInfo wifiInfo) {
        super.onWifiChanged(networkInfo, wifiInfo);
        mPreference.setEnabled(true);
        updateIfAvailable();
    }

    @Override
    public void onLost(Network network) {
        mPreference.setEnabled(false);
    }

    @Override
    public void onWifiConfigurationChanged(WifiConfiguration wifiConfiguration,
            NetworkInfo networkInfo, WifiInfo wifiInfo) {
        super.onWifiConfigurationChanged(wifiConfiguration, networkInfo, wifiInfo);
        updateIfAvailable();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = screen.findPreference(getPreferenceKey());
        updateIfAvailable();
    }

    @Override
    public int getAvailabilityStatus() {
        if (!WifiUtil.isWifiAvailable(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return mAccessPoint.isActive() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    protected final void updateIfAvailable() {
        if (isAvailable()) {
            updateInfo();
        }
    }

    private void updateInfo() {
        StringJoiner ipv6Addresses = new StringJoiner(System.lineSeparator());

        for (LinkAddress addr : mWifiInfoProvider.getLinkProperties().getLinkAddresses()) {
            if (addr.getAddress() instanceof Inet6Address) {
                ipv6Addresses.add(addr.getAddress().getHostAddress());
            }
        }

        if (ipv6Addresses.length() > 0) {
            mPreference.setSummary(
                    BidiFormatter.getInstance().unicodeWrap(ipv6Addresses.toString()));
            mPreference.setVisible(true);
        } else {
            mPreference.setVisible(false);
        }
    }
}
