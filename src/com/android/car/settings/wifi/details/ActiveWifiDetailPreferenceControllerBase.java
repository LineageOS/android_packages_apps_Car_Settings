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
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;

import androidx.preference.PreferenceScreen;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.wifi.WifiUtil;

/**
 * Controller for logic pertaining to displaying Wifi information for the
 * {@link WifiDetailsFragment}. Only available when wifi is active.
 *
 * <p>
 * Subclass should use {@link updateInfo} to render UI with latest info if desired.
 */
public abstract class ActiveWifiDetailPreferenceControllerBase extends WifiControllerBase {

    protected WifiDetailPreference mWifiDetailPreference;

    public ActiveWifiDetailPreferenceControllerBase(
            Context context, String preferenceKey, FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mWifiDetailPreference =
                (WifiDetailPreference) screen.findPreference(getPreferenceKey());
        updateIfAvailable();
    }

    @Override
    public void onLost(Network network) {
        super.onLost(network);
        mWifiDetailPreference.setEnabled(false);
    }

    @Override
    public void onWifiChanged(NetworkInfo networkInfo, WifiInfo wifiInfo) {
        super.onWifiChanged(networkInfo, wifiInfo);
        mWifiDetailPreference.setEnabled(true);
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

    /**
     * Updates UI based on new network/wifi info.
     */
    protected abstract void updateInfo();
}
