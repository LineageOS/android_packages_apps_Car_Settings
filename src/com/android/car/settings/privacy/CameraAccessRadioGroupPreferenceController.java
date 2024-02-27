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

package com.android.car.settings.privacy;

import android.annotation.FlaggedApi;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.hardware.SensorPrivacyManager;
import android.hardware.SensorPrivacyManager.OnSensorPrivacyChangedListener;
import android.hardware.SensorPrivacyManager.OnSensorPrivacyChangedListener.SensorPrivacyChangedParams;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceGroup;
import androidx.preference.TwoStatePreference;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.GroupSelectionPreferenceController;
import com.android.car.ui.preference.CarUiRadioButtonPreference;
import com.android.internal.camera.flags.Flags;

import java.util.ArrayList;
import java.util.List;

@FlaggedApi(Flags.FLAG_CAMERA_PRIVACY_ALLOWLIST)
public class CameraAccessRadioGroupPreferenceController extends GroupSelectionPreferenceController {
    private static final String TAG =
            CameraAccessRadioGroupPreferenceController.class.getSimpleName();
    private String mSelectedKey;
    private final SensorPrivacyManager mSensorPrivacyManager;
    private final OnSensorPrivacyChangedListener mListener =
            new SensorPrivacyManager.OnSensorPrivacyChangedListener() {
                @Override
                public void onSensorPrivacyChanged(SensorPrivacyChangedParams params) {
                    mSelectedKey = getSensorPrivacyKey(params.getState());
                    refreshUi();
                }

                @Override
                public void onSensorPrivacyChanged(int sensor, boolean enabled) {
                    // handled in onSensorPrivacyChanged(SensorPrivacyChangedParams)
                }
            };

    public CameraAccessRadioGroupPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        this(context, preferenceKey, fragmentController, uxRestrictions,
                SensorPrivacyManager.getInstance(context));
    }

    CameraAccessRadioGroupPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions,
            SensorPrivacyManager sensorPrivacyManager) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mSensorPrivacyManager = sensorPrivacyManager;
        int state = mSensorPrivacyManager.getSensorPrivacyState(
                SensorPrivacyManager.TOGGLE_TYPE_SOFTWARE,
                SensorPrivacyManager.Sensors.CAMERA);
        mSelectedKey = getSensorPrivacyKey(state);
    }

    @Override
    protected Class<PreferenceGroup> getPreferenceType() {
        return PreferenceGroup.class;
    }

    @Override
    protected void onStartInternal() {
        mSensorPrivacyManager.addSensorPrivacyListener(SensorPrivacyManager.Sensors.CAMERA,
                mListener);
    }

    @Override
    protected void onStopInternal() {
        mSensorPrivacyManager.removeSensorPrivacyListener(SensorPrivacyManager.Sensors.CAMERA,
                mListener);
    }

    @Override
    protected void checkInitialized() {
        if (mSelectedKey == null) {
            throw new IllegalStateException(
                    "Default selection should be set before calling this function");
        }
    }

    @Override
    protected String getCurrentCheckedKey() {
        int state = mSensorPrivacyManager.getSensorPrivacyState(
                SensorPrivacyManager.TOGGLE_TYPE_SOFTWARE,
                SensorPrivacyManager.Sensors.CAMERA);
        mSelectedKey = getSensorPrivacyKey(state);
        return mSelectedKey;
    }

    @NonNull
    @Override
    protected List<TwoStatePreference> getGroupPreferences() {
        List<TwoStatePreference> entries = new ArrayList<>();

        CarUiRadioButtonPreference anyApp = new CarUiRadioButtonPreference(getContext());
        anyApp.setKey(getContext().getString(R.string.pk_camera_access_radio_any_app));
        anyApp.setTitle(R.string.camera_access_radio_any_app_title);
        anyApp.setSummary(R.string.camera_access_radio_any_app_summary);
        entries.add(anyApp);

        CarUiRadioButtonPreference requiredApps = new CarUiRadioButtonPreference(getContext());
        requiredApps.setKey(getContext().getString(R.string.pk_camera_access_radio_required_apps));
        requiredApps.setTitle(R.string.camera_access_radio_required_apps_title);
        requiredApps.setSummary(R.string.camera_access_radio_required_apps_summary);
        entries.add(requiredApps);

        return entries;
    }

    @Override
    protected boolean handleGroupItemSelected(TwoStatePreference preference) {
        String selectedKey = preference.getKey();
        if (TextUtils.equals(selectedKey, getCurrentCheckedKey())) {
            return false;
        }

        mSelectedKey = selectedKey;
        setSensorPrivacyKey(mSelectedKey);
        return true;
    }

    private void setSensorPrivacyKey(String key) {
        int state = SensorPrivacyManager.StateTypes.DISABLED;
        if (key.equals(getContext().getString(R.string.pk_camera_access_radio_no_apps))) {
            state = SensorPrivacyManager.StateTypes.ENABLED;
        } else if (key.equals(getContext()
                .getString(R.string.pk_camera_access_radio_required_apps))) {
            state = SensorPrivacyManager.StateTypes.ENABLED_EXCEPT_ALLOWLISTED_APPS;
        }

        mSensorPrivacyManager.setSensorPrivacyStateForProfileGroup(
                SensorPrivacyManager.Sources.SETTINGS,
                SensorPrivacyManager.Sensors.CAMERA,
                state);
    }

    String getSensorPrivacyKey(int state) {
        String key = getContext().getString(R.string.pk_camera_access_radio_any_app);
        if (state == SensorPrivacyManager.StateTypes.ENABLED) {
            key = getContext().getString(R.string.pk_camera_access_radio_no_apps);
        } else if (state
                == SensorPrivacyManager.StateTypes.ENABLED_EXCEPT_ALLOWLISTED_APPS) {
            key = getContext().getString(R.string.pk_camera_access_radio_required_apps);
        }

        return key;
    }
}
