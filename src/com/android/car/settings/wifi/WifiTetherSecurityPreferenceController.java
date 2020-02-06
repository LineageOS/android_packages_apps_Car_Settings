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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.SoftApConfiguration;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.ListPreference;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;

/**
 * Controls WiFi Hotspot Security Type configuration.
 */
public class WifiTetherSecurityPreferenceController extends
        WifiTetherBasePreferenceController<ListPreference> {

    public static final String KEY_SECURITY_TYPE = "KEY_SECURITY_TYPE";
    public static final String ACTION_SECURITY_TYPE_CHANGED =
            "com.android.car.settings.wifi.ACTION_WIFI_TETHER_SECURITY_TYPE_CHANGED";

    private int mSecurityType;

    public WifiTetherSecurityPreferenceController(Context context, String preferenceKey,
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
        mSecurityType = getCarSoftApConfig().getSecurityType();
        getPreference().setEntries(
                getContext().getResources().getStringArray(R.array.wifi_tether_security));
        String[] entryValues = {
                Integer.toString(SoftApConfiguration.SECURITY_TYPE_WPA2_PSK),
                Integer.toString(SoftApConfiguration.SECURITY_TYPE_OPEN)};
        getPreference().setEntryValues(entryValues);
        getPreference().setValue(String.valueOf(mSecurityType));
    }

    @Override
    protected boolean handlePreferenceChanged(ListPreference preference,
            Object newValue) {
        mSecurityType = Integer.parseInt(newValue.toString());
        updateSecurityType();
        refreshUi();
        return true;
    }

    @Override
    protected void updateState(ListPreference preference) {
        super.updateState(preference);
        preference.setValue(Integer.toString(mSecurityType));
    }

    @Override
    protected String getSummary() {
        int stringResId = mSecurityType == SoftApConfiguration.SECURITY_TYPE_WPA2_PSK
                ? R.string.wifi_hotspot_wpa2_personal : R.string.wifi_hotspot_security_none;
        return getContext().getString(stringResId);
    }

    @Override
    protected String getDefaultSummary() {
        return null;
    }

    /** Overriding to orchestrate the order in which the intents are broadcast. */
    @Override
    protected void setCarSoftApConfig(SoftApConfiguration configuration) {
        getCarWifiManager().setSoftApConfig(configuration);
        broadcastSecurityTypeChanged();
        requestWifiTetherRestart();
    }

    private void updateSecurityType() {
        String passphrase = mSecurityType == SoftApConfiguration.SECURITY_TYPE_OPEN
                ? null : getSavedPassword();
        try {
            SoftApConfiguration config = new SoftApConfiguration.Builder(getCarSoftApConfig())
                    .setPassphrase(passphrase, mSecurityType)
                    .build();
            setCarSoftApConfig(config);
        } catch (IllegalArgumentException e) {
            // setPassphrase() performs validation that the (securityType, passphrase) pair is
            // consistent.
            // e.g. if securityType == OPEN then passphrase == null,
            // if securityType == WPA2_PSK then 8 <= passphrase.length() <= 63, etc.
            // However, the (securityType, passphrase) pair is not updated simultaneously in this
            // architecture, allowing there to be a transient period where the
            // (securityType, passphrase) pair is not consistent.

            // e.g.
            // 1. (passphrase, securityType) = (null, OPEN)
            // 2. securityType => WPA2_PSK
            // 3. (passphrase, securityType) = (null, WPA2_PSK), illegal, WPA2_PSK must have a
            // password

            // During this transient state, do not save the SoftApConfiguration. Instead,
            // broadcast the securityType change so that WifiTetherPasswordPreferenceController
            // gets the latest securityType, and once the passphrase has been updated the
            // (securityType, passphrase) pair can be updated simultaneously.
            broadcastSecurityTypeChanged();
        }
    }

    private void broadcastSecurityTypeChanged() {
        Intent intent = new Intent(ACTION_SECURITY_TYPE_CHANGED);
        intent.putExtra(KEY_SECURITY_TYPE, mSecurityType);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    private String getSavedPassword() {
        SharedPreferences sp = getContext().getSharedPreferences(
                WifiTetherPasswordPreferenceController.SHARED_PREFERENCE_PATH,
                Context.MODE_PRIVATE);
        String savedPassword =
                sp.getString(WifiTetherPasswordPreferenceController.KEY_SAVED_PASSWORD,
                        /* defaultValue= */ null);
        return savedPassword;
    }
}
