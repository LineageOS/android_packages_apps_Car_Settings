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

package com.android.car.settings.location;

import static android.car.hardware.power.PowerComponent.LOCATION;

import android.car.drivingstate.CarUxRestrictions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;

import androidx.annotation.VisibleForTesting;

import com.android.car.settings.R;
import com.android.car.settings.common.ColoredSwitchPreference;
import com.android.car.settings.common.ConfirmationDialogFragment;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PowerPolicyListener;
import com.android.car.settings.common.PreferenceController;

/**
 * The preference on Driver assistance page for enabling or disabling ADAS GNSS bypass.
 */
public class AdasLocationSwitchSubMenuPreferenceController extends
        PreferenceController<ColoredSwitchPreference> {

    private static final IntentFilter INTENT_FILTER_ADAS_GNSS_ENABLED_CHANGED = new IntentFilter(
            LocationManager.ACTION_ADAS_GNSS_ENABLED_CHANGED);

    private final LocationManager mLocationManager;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshUi();
        }
    };

    @VisibleForTesting
    final PowerPolicyListener mPowerPolicyListener;

    public AdasLocationSwitchSubMenuPreferenceController(Context context,
            String preferenceKey,
            FragmentController fragmentController,
            CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mLocationManager = getContext().getSystemService(LocationManager.class);
        mPowerPolicyListener = new PowerPolicyListener(context, LOCATION,
                isOn -> {
                    handlePowerPolicyChange(getPreference(), isOn);
                });
    }

    @Override
    protected Class<ColoredSwitchPreference> getPreferenceType() {
        return ColoredSwitchPreference.class;
    }

    @Override
    protected void updateState(ColoredSwitchPreference preference) {
        updateSwitchPreference(preference, mLocationManager.isAdasGnssLocationEnabled());
    }

    @Override
    protected boolean handlePreferenceChanged(ColoredSwitchPreference preference, Object newValue) {
        boolean locationEnabled = (Boolean) newValue;

        if (!locationEnabled) {
            // Shows confirmation dialog when users trying toggle Driver assistance off.
            getFragmentController().showDialog(getConfirmationDialog(),
                    ConfirmationDialogFragment.TAG);
            return false;
        }
        // Enables ADAS GNSS bypass when users toggling Driver assistance on.
        mLocationManager.setAdasGnssLocationEnabled(true);
        return true;
    }

    @Override
    protected void onStartInternal() {
        getContext().registerReceiver(mReceiver, INTENT_FILTER_ADAS_GNSS_ENABLED_CHANGED);
    }

    @Override
    protected void onResumeInternal() {
        mPowerPolicyListener.handleCurrentPolicy();
    }

    @Override
    protected void onStopInternal() {
        getContext().unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroyInternal() {
        mPowerPolicyListener.release();
    }

    private void updateSwitchPreference(ColoredSwitchPreference preference,
            boolean enabled) {
        preference.setChecked(enabled);
    }

    /**
     * It's required that Driver assistance toggle has to be on and disabled when the main location
     * switch is on. But there's no need to consider the state of the main location switch here when
     * deciding the enableness of Driver assistance toggle. Because
     * {@link AdasLocationSwitchPreferenceController} guarantees the main location switch is off
     * when users can access Driver assistance page, also users can't change the state of the main
     * location switch until power policy is on.
     */
    private void handlePowerPolicyChange(ColoredSwitchPreference preference,
            boolean enabled) {
        preference.setEnabled(enabled);
    }

    private ConfirmationDialogFragment getConfirmationDialog() {
        return new ConfirmationDialogFragment.Builder(getContext())
                .setMessage(getContext()
                        .getString(R.string.location_driver_assistance_toggle_off_warning))
                .setNegativeButton(getContext()
                        .getString(R.string.driver_assistance_warning_confirm_label), arguments -> {
                                mLocationManager.setAdasGnssLocationEnabled(false);
                        })
                .setPositiveButton(android.R.string.cancel,
                        /* rejectListener= */ null)
                .build();
    }
}
