/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.settings.testutils;

import android.content.Context;
import android.net.wifi.WifiManager;

import com.android.car.settings.wifi.CarWifiManager;
import com.android.settingslib.wifi.AccessPoint;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.List;

@Implements(CarWifiManager.class)
public class ShadowCarWifiManager {

    private static CarWifiManager sInstance;

    public static void setInstance(CarWifiManager wifiManager) {
        sInstance = wifiManager;
    }

    @Resetter
    public static void reset() {
        sInstance = null;
    }

    @Implementation
    public void __constructor__(Context context) {
    }

    @Implementation
    public void start() {
        sInstance.start();
    }

    @Implementation
    public boolean setWifiEnabled(boolean enabled) {
        return sInstance.setWifiEnabled(enabled);
    }

    @Implementation
    public boolean isWifiEnabled() {
        return sInstance.isWifiEnabled();
    }

    @Implementation
    public boolean isWifiApEnabled() {
        return sInstance.isWifiApEnabled();
    }

    @Implementation
    public List<AccessPoint> getAllAccessPoints() {
        return sInstance.getAllAccessPoints();
    }

    @Implementation
    public List<AccessPoint> getSavedAccessPoints() {
        return sInstance.getSavedAccessPoints();
    }

    @Implementation
    public void connectToPublicWifi(AccessPoint accessPoint, WifiManager.ActionListener listener) {
        sInstance.connectToPublicWifi(accessPoint, listener);
    }
}
