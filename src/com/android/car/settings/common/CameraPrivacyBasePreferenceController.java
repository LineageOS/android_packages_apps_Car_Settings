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

package com.android.car.settings.common;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.hardware.SensorPrivacyManager;

import androidx.preference.Preference;

/**
 * Abstract PreferenceController that listens to OnSensorPrivacyChangedListener
 * and will refresh the UI when the sensor privacy changed event happens.
 *
 * @param <V> the upper bound on the type of {@link Preference} on which the controller expects
 *         to operate.
 */
public abstract class CameraPrivacyBasePreferenceController<V extends Preference> extends
        PreferenceController<V> {
    private final SensorPrivacyManager mSensorPrivacyManager;
    private final SensorPrivacyManager.OnSensorPrivacyChangedListener mListener =
            new SensorPrivacyManager.OnSensorPrivacyChangedListener() {
                @Override
                public void onSensorPrivacyChanged(SensorPrivacyChangedParams params) {
                    refreshUi();
                }

                @Override
                public void onSensorPrivacyChanged(int sensor, boolean enabled) {
                    // handled in onSensorPrivacyChanged(SensorPrivacyChangedParams)
                }
            };

    public CameraPrivacyBasePreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions,
            SensorPrivacyManager sensorPrivacyManager) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mSensorPrivacyManager = sensorPrivacyManager;
    }

    @Override
    protected void onStartInternal() {
        super.onStartInternal();
        mSensorPrivacyManager.addSensorPrivacyListener(
                SensorPrivacyManager.Sensors.CAMERA, mListener);
    }

    @Override
    protected void onStopInternal() {
        super.onStopInternal();
        mSensorPrivacyManager.removeSensorPrivacyListener(SensorPrivacyManager.Sensors.CAMERA,
                mListener);
    }

    public SensorPrivacyManager getSensorPrivacyManager() {
        return mSensorPrivacyManager;
    }
}

