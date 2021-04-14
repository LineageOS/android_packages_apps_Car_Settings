/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.TetheringManager;
import android.net.wifi.WifiManager;

import androidx.annotation.VisibleForTesting;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.car.settings.R;
import com.android.car.settings.common.ColoredSwitchPreference;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;
import com.android.internal.util.ConcurrentUtils;

/**
 * Enables/disables tether state via SwitchPreference.
 */
public class WifiTetherStateSwitchPreferenceController extends
        PreferenceController<ColoredSwitchPreference> {

    private CarWifiManager mCarWifiManager;
    private TetheringManager mTetheringManager;
    private boolean mRestartBooked = false;
    private WifiManager.SoftApCallback mSoftApCallback = new WifiManager.SoftApCallback() {
        @Override
        public void onStateChanged(int state, int failureReason) {
            handleWifiApStateChanged(state);
        }
    };

    private final BroadcastReceiver mRestartReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mCarWifiManager != null && mCarWifiManager.isWifiApEnabled()) {
                restartTethering();
            }
        }
    };

    public WifiTetherStateSwitchPreferenceController(Context context,
            String preferenceKey,
            FragmentController fragmentController,
            CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mCarWifiManager = new CarWifiManager(context,
                getFragmentController().getSettingsLifecycle());
        mTetheringManager = getContext().getSystemService(TetheringManager.class);
    }

    @Override
    protected Class<ColoredSwitchPreference> getPreferenceType() {
        return ColoredSwitchPreference.class;
    }

    @Override
    protected void updateState(ColoredSwitchPreference preference) {
        updateSwitchPreference(mCarWifiManager.isWifiApEnabled());
    }

    @Override
    protected boolean handlePreferenceChanged(ColoredSwitchPreference preference, Object newValue) {
        boolean switchOn = (Boolean) newValue;
        updateSwitchPreference(switchOn);
        if (switchOn) {
            startTethering();
        } else {
            stopTethering();
        }
        return true;
    }

    @Override
    protected void onCreateInternal() {
        getPreference().setContentDescription(
                getContext().getString(R.string.wifi_hotspot_state_switch_content_description));
    }

    @Override
    protected void onStartInternal() {
        mCarWifiManager.registerSoftApCallback(getContext().getMainExecutor(), mSoftApCallback);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mRestartReceiver,
                new IntentFilter(
                        WifiTetherBasePreferenceController.ACTION_RESTART_WIFI_TETHERING));
    }

    @Override
    protected void onStopInternal() {
        mCarWifiManager.unregisterSoftApCallback(mSoftApCallback);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mRestartReceiver);
    }

    @VisibleForTesting
    void setCarWifiManager(CarWifiManager carWifiManager) {
        mCarWifiManager = carWifiManager;
    }

    @VisibleForTesting
    void setTetheringManager(TetheringManager tetheringManager) {
        mTetheringManager = tetheringManager;
    }

    /**
     * When the state of the hotspot changes, update the state of the tethering switch as well
     */
    @VisibleForTesting
    void handleWifiApStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_AP_STATE_ENABLING:
                getPreference().setEnabled(false);
                break;
            case WifiManager.WIFI_AP_STATE_ENABLED:
                getPreference().setEnabled(true);
                if (!getPreference().isChecked()) {
                    updateSwitchPreference(/* switchOn= */ true);
                }
                break;
            case WifiManager.WIFI_AP_STATE_DISABLING:
                getPreference().setEnabled(false);
                if (getPreference().isChecked()) {
                    updateSwitchPreference(/* switchOn= */ false);
                }
                break;
            case WifiManager.WIFI_AP_STATE_DISABLED:
                updateSwitchPreference(/* switchOn= */ false);
                getPreference().setEnabled(true);
                if (mRestartBooked) {
                    // Hotspot was disabled as part of a restart request - we can now re-enable it
                    getPreference().setEnabled(false);
                    startTethering();
                    mRestartBooked = false;
                }
                break;
            default:
                updateSwitchPreference(/* switchOn= */ false);
                getPreference().setEnabled(true);
                break;
        }
    }

    private void startTethering() {
        mTetheringManager.startTethering(ConnectivityManager.TETHERING_WIFI,
                ConcurrentUtils.DIRECT_EXECUTOR,
                new TetheringManager.StartTetheringCallback() {
                    @Override
                    public void onTetheringFailed(final int result) {
                        updateSwitchPreference(/* switchOn= */ false);
                        getPreference().setEnabled(true);
                    }
                });
    }

    private void stopTethering() {
        mTetheringManager.stopTethering(ConnectivityManager.TETHERING_WIFI);
    }

    private void restartTethering() {
        stopTethering();
        mRestartBooked = true;
    }

    private void updateSwitchPreference(boolean switchOn) {
        getPreference().setTitle(switchOn ? R.string.car_ui_preference_switch_on
                : R.string.car_ui_preference_switch_off);
        getPreference().setChecked(switchOn);
    }
}
