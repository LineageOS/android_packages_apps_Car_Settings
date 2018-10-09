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
 * limitations under the License
 */
package com.android.car.settings.wifi;

import android.annotation.DrawableRes;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import androidx.annotation.StringRes;

import com.android.car.settings.R;
import com.android.settingslib.wifi.AccessPoint;

/**
 * A collections of util functions for WIFI.
 */
public class WifiUtil {
    @DrawableRes
    public static int getIconRes(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLING:
            case WifiManager.WIFI_STATE_DISABLED:
                return R.drawable.ic_settings_wifi_disabled;
            default:
                return R.drawable.ic_settings_wifi;
        }
    }

    public static boolean isWifiOn(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLING:
            case WifiManager.WIFI_STATE_DISABLED:
                return false;
            default:
                return true;
        }
    }

    /**
     * @return 0 if no proper description can be found.
     */
    @StringRes
    public static Integer getStateDesc(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLING:
                return R.string.wifi_starting;
            case WifiManager.WIFI_STATE_DISABLING:
                return R.string.wifi_stopping;
            case WifiManager.WIFI_STATE_DISABLED:
                return R.string.wifi_disabled;
            default:
                return 0;
        }
    }

    /**
     * Returns {@Code true} if wifi is available on this device.
     */
    public static boolean isWifiAvailable(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI);
    }

    /**
     * Gets a unique key for a {@link AccessPoint}.
     */
    public static String getKey(AccessPoint accessPoint) {
        return String.valueOf(accessPoint.hashCode());
    }

    /**
     * This method is a stripped and negated version of WifiConfigStore.canModifyNetwork.
     *
     * @param context Context of caller
     * @param config  The WiFi config.
     * @return {@code true} if Settings cannot modify the config due to lockDown.
     */
    public static boolean isNetworkLockedDown(Context context, WifiConfiguration config) {
        if (config == null) {
            return false;
        }

        final DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        final PackageManager pm = context.getPackageManager();

        // Check if device has DPM capability. If it has and dpm is still null, then we
        // treat this case with suspicion and bail out.
        if (pm.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN) && dpm == null) {
            return true;
        }

        boolean isConfigEligibleForLockdown = false;
        if (dpm != null) {
            final ComponentName deviceOwner = dpm.getDeviceOwnerComponentOnAnyUser();
            if (deviceOwner != null) {
                final int deviceOwnerUserId = dpm.getDeviceOwnerUserId();
                try {
                    final int deviceOwnerUid = pm.getPackageUidAsUser(deviceOwner.getPackageName(),
                            deviceOwnerUserId);
                    isConfigEligibleForLockdown = deviceOwnerUid == config.creatorUid;
                } catch (PackageManager.NameNotFoundException e) {
                    // don't care
                }
            }
        }
        if (!isConfigEligibleForLockdown) {
            return false;
        }

        final ContentResolver resolver = context.getContentResolver();
        final boolean isLockdownFeatureEnabled = Settings.Global.getInt(resolver,
                Settings.Global.WIFI_DEVICE_OWNER_CONFIGS_LOCKDOWN, 0) != 0;
        return isLockdownFeatureEnabled;
    }

    /**
     * Returns {@code true} if the provided NetworkCapabilities indicate a captive portal network.
     */
    public static boolean canSignIntoNetwork(NetworkCapabilities capabilities) {
        return (capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL));
    }
}
