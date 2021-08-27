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

import android.content.ContentResolver;
import android.net.Uri;
import android.util.ArrayMap;

import androidx.annotation.VisibleForTesting;

import java.util.Map;

/**
 * Registry of valid Quick Control Uris provided by CarSettings.
 */
public class SettingsQCRegistry {
    public static final String AUTHORITY = "com.android.car.settings.qc";

    // Start Uris
    public static final Uri BLUETOOTH_SWITCH_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(AUTHORITY)
            .appendPath("bluetooth_switch")
            .build();

    public static final Uri PAIRED_BLUETOOTH_DEVICES_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(AUTHORITY)
            .appendPath("paired_bluetooth_devices")
            .build();

    public static final Uri WIFI_TILE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(AUTHORITY)
            .appendPath("wifi_tile")
            .build();

    public static final Uri HOTSPOT_TILE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(AUTHORITY)
            .appendPath("hotspot_tile")
            .build();

    public static final Uri MOBILE_DATA_TILE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(AUTHORITY)
            .appendPath("mobile_data_tile")
            .build();

    public static final Uri BRIGHTNESS_SLIDER_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(AUTHORITY)
            .appendPath("brightness_slider")
            .build();
    // End Uris

    @VisibleForTesting
    static final Map<Uri, Class<? extends SettingsQCItem>> sUriToQC = createUriToQCMap();

    private static Map<Uri, Class<? extends SettingsQCItem>> createUriToQCMap() {
        Map<Uri, Class<? extends SettingsQCItem>> map = new ArrayMap<>();

        map.put(BLUETOOTH_SWITCH_URI, BluetoothSwitch.class);
        map.put(PAIRED_BLUETOOTH_DEVICES_URI, PairedBluetoothDevices.class);
        map.put(WIFI_TILE_URI, WifiTile.class);
        map.put(HOTSPOT_TILE_URI, HotspotTile.class);
        map.put(MOBILE_DATA_TILE_URI, MobileDataTile.class);
        map.put(BRIGHTNESS_SLIDER_URI, BrightnessSlider.class);

        return map;
    }

    /**
     * Returns the relevant {@link SettingsQCItem} class that corresponds to the provided uri.
     */
    public static Class<? extends SettingsQCItem> getQCClassByUri(Uri uri) {
        return sUriToQC.get(removeParameterFromUri(uri));
    }

    /**
     * Returns a uri without its parameters (or null if the provided uri is null).
     */
    public static Uri removeParameterFromUri(Uri uri) {
        return uri != null ? uri.buildUpon().clearQuery().build() : null;
    }

    /**
     * Returns {@code true} if the provided uri is a valid QCItem Uri handled by
     * {@link SettingsQCRegistry}.
     */
    public static boolean isValidUri(Uri uri) {
        return sUriToQC.containsKey(removeParameterFromUri(uri));
    }

    /**
     * Returns {@code true} if the provided action is a valid intent action handled by
     * {@link SettingsQCRegistry}.
     */
    public static boolean isValidAction(String action) {
        return isValidUri(Uri.parse(action));
    }
}