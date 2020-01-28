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

package com.android.car.settings.wifi;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.res.Resources;
import android.net.wifi.SoftApConfiguration;
import android.util.Log;

import androidx.preference.ListPreference;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;

/**
 * Controls WiFi Hotspot AP Band configuration.
 */
public class WifiTetherApBandPreferenceController extends
        WifiTetherBasePreferenceController<ListPreference> {
    private static final String TAG = "CarWifiTetherApBandPref";

    private String[] mBandEntries;
    private String[] mBandSummaries;
    private int mBand;
    private boolean mIsDualMode;

    public WifiTetherApBandPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<ListPreference> getPreferenceType() {
        return ListPreference.class;
    }

    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();
        mIsDualMode = getCarWifiManager().isDualModeSupported();
        updatePreferenceEntries();
        getPreference().setEntries(mBandSummaries);
        getPreference().setEntryValues(mBandEntries);
    }

    @Override
    public void updateState(ListPreference preference) {
        super.updateState(preference);

        SoftApConfiguration config = getCarSoftApConfig();
        if (config == null) {
            mBand = SoftApConfiguration.BAND_2GHZ;
        } else if (is5GhzBandSupported()) {
            mBand = validateSelection(config.getBand());
        } else {
            SoftApConfiguration newConfig = new SoftApConfiguration.Builder(config)
                    .setBand(SoftApConfiguration.BAND_2GHZ)
                    .build();
            setCarSoftApConfig(newConfig);
            mBand = config.getBand();
        }

        if (!is5GhzBandSupported()) {
            preference.setEnabled(false);
            preference.setSummary(R.string.wifi_ap_choose_2G);
        } else {
            preference.setValue(Integer.toString(config.getBand()));
            preference.setSummary(getSummary());
        }

    }

    @Override
    protected String getSummary() {
        if (!is5GhzBandSupported()) {
            return getContext().getString(R.string.wifi_ap_choose_2G);
        }
        switch (mBand) {
            case SoftApConfiguration.BAND_2GHZ | SoftApConfiguration.BAND_5GHZ:
                return getContext().getString(R.string.wifi_ap_prefer_5G);
            case SoftApConfiguration.BAND_2GHZ:
                return mBandSummaries[0];
            case SoftApConfiguration.BAND_5GHZ:
                return mBandSummaries[1];
            default:
                Log.e(TAG, "Unknown band: " + mBand);
                return getContext().getString(R.string.wifi_ap_prefer_5G);
        }
    }

    @Override
    protected String getDefaultSummary() {
        return null;
    }

    @Override
    public boolean handlePreferenceChanged(ListPreference preference, Object newValue) {
        mBand = validateSelection(Integer.parseInt((String) newValue));
        updateApBand(); // updating AP band because mBandIndex may have been assigned a new value.
        refreshUi();
        return true;
    }

    private int validateSelection(int band) {
        // Reset the band to 2.4 GHz if we get a weird config back to avoid a crash.
        final boolean isDualMode = getCarWifiManager().isDualModeSupported();

        if (!isDualMode
                && ((band & SoftApConfiguration.BAND_5GHZ) != 0)
                && ((band & SoftApConfiguration.BAND_2GHZ) != 0)) {
            return SoftApConfiguration.BAND_5GHZ;
        } else if (!is5GhzBandSupported() && SoftApConfiguration.BAND_5GHZ == band) {
            return SoftApConfiguration.BAND_2GHZ;
        } else if (isDualMode && SoftApConfiguration.BAND_5GHZ == band) {
            return SoftApConfiguration.BAND_5GHZ | SoftApConfiguration.BAND_2GHZ;
        }

        return band;
    }

    private void updatePreferenceEntries() {
        Resources res = getContext().getResources();
        int entriesRes = R.array.wifi_ap_band_config_full;
        int summariesRes = R.array.wifi_ap_band_summary_full;
        // change the list options if this is a dual mode device
        if (mIsDualMode) {
            entriesRes = R.array.wifi_ap_band_dual_mode;
            summariesRes = R.array.wifi_ap_band_dual_mode_summary;
        }
        mBandEntries = res.getStringArray(entriesRes);
        mBandSummaries = res.getStringArray(summariesRes);
    }

    private void updateApBand() {
        SoftApConfiguration config = new SoftApConfiguration.Builder(getCarSoftApConfig())
                .setBand(mBand)
                .build();
        setCarSoftApConfig(config);
        getPreference().setValue(getBandEntry());
    }

    private String getBandEntry() {
        switch (mBand) {
            case SoftApConfiguration.BAND_2GHZ | SoftApConfiguration.BAND_5GHZ:
            case SoftApConfiguration.BAND_2GHZ:
                return mBandEntries[0];
            case SoftApConfiguration.BAND_5GHZ:
                return mBandEntries[1];
            default:
                Log.e(TAG, "Unknown band: " + mBand + ", defaulting to 2GHz");
                return mBandEntries[0];
        }
    }

    private boolean is5GhzBandSupported() {
        String countryCode = getCarWifiManager().getCountryCode();
        return getCarWifiManager().is5GhzBandSupported() && countryCode != null;
    }
}
