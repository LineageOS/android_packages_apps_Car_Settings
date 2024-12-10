/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.car.settings.datetime;

import android.app.time.TimeConfiguration;
import android.app.time.TimeManager;
import android.app.time.TimeZoneConfiguration;
import android.car.drivingstate.CarUxRestrictions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;

import androidx.preference.SwitchPreference;

import com.android.car.settings.R;
import com.android.car.settings.common.ConfirmationDialogFragment;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;
import com.android.car.settings.location.LocationAccessFragment;

/**
 * Business logic which controls the auto local time toggle.
 */
public class AutoLocalTimeTogglePreferenceController extends
        PreferenceController<SwitchPreference> {
    private final TimeManager mTimeManager;
    private final LocationManager mLocationManager;
    private final BroadcastReceiver mLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshUi();
        }
    };

    public AutoLocalTimeTogglePreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mTimeManager = context.getSystemService(TimeManager.class);
        mLocationManager = context.getSystemService(LocationManager.class);
    }

    @Override
    protected Class<SwitchPreference> getPreferenceType() {
        return SwitchPreference.class;
    }

    @Override
    protected void onCreateInternal() {
        setClickableWhileDisabled(getPreference(), /* clickable= */ true, p ->
                DatetimeUtils.runClickableWhileDisabled(getContext(), getFragmentController()));
    }

    @Override
    protected void onStartInternal() {
        IntentFilter locationChangeFilter = new IntentFilter();
        locationChangeFilter.addAction(LocationManager.MODE_CHANGED_ACTION);
        getContext().registerReceiver(
                mLocationReceiver, locationChangeFilter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onStopInternal() {
        getContext().unregisterReceiver(mLocationReceiver);
    }

    @Override
    protected void updateState(SwitchPreference preference) {
        boolean isEnabled = DatetimeUtils.isAutoLocalTimeDetectionEnabled(mTimeManager);
        preference.setChecked(isEnabled);
        if (isEnabled && !mLocationManager.isLocationEnabled()) {
            preference.setSummary(R.string.auto_local_time_toggle_summary);
        } else {
            preference.setSummary("");
        }
    }

    @Override
    protected boolean handlePreferenceChanged(SwitchPreference preference, Object newValue) {
        if (!DatetimeUtils.isAutoTimeDetectionCapabilityPossessed(mTimeManager)
                || !DatetimeUtils.isAutoTimeZoneDetectionCapabilityPossessed(mTimeManager)) {
            return false;
        }

        boolean setAutoLocalTimeEnabled = (boolean) newValue;
        updateTimeAndTimeZoneConfiguration(setAutoLocalTimeEnabled);

        if (setAutoLocalTimeEnabled && !mLocationManager.isLocationEnabled()) {
            preference.setSummary(R.string.auto_local_time_toggle_summary);
            getFragmentController().showDialog(getConfirmationDialog(),
                    ConfirmationDialogFragment.TAG);
        } else {
            preference.setSummary("");
        }
        return true;
    }

    @Override
    public int getDefaultAvailabilityStatus() {
        return DatetimeUtils.getAvailabilityStatus(getContext());
    }

    private void updateTimeAndTimeZoneConfiguration(boolean setAutoDatetimeEnabled) {
        mTimeManager.updateTimeConfiguration(new TimeConfiguration.Builder()
                .setAutoDetectionEnabled(setAutoDatetimeEnabled).build());
        mTimeManager.updateTimeZoneConfiguration(new TimeZoneConfiguration.Builder()
                .setAutoDetectionEnabled(setAutoDatetimeEnabled).build());
        getContext().sendBroadcast(new Intent(Intent.ACTION_TIME_CHANGED));
    }

    private ConfirmationDialogFragment getConfirmationDialog() {
        return new ConfirmationDialogFragment.Builder(getContext())
                .setMessage(R.string.auto_local_time_dialog_msg)
                .setNegativeButton(R.string.auto_local_time_dialog_negative_button_text,
                        /* listener= */ null)
                .setPositiveButton(R.string.auto_local_time_dialog_positive_button_text,
                        arguments -> {
                            getFragmentController().launchFragment(new LocationAccessFragment());
                        })
                .build();
    }
}
