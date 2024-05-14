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

import androidx.preference.Preference;

import com.android.car.settings.Flags;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;

/**
 * Controller for displaying microphone access page.
 */
@FlaggedApi(Flags.FLAG_MICROPHONE_PRIVACY_UPDATES)
public class MicrophoneAccessPreferenceController extends PreferenceController<Preference> {
    private final SensorPrivacyManager mSensorPrivacyManager;

    public MicrophoneAccessPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mSensorPrivacyManager = SensorPrivacyManager.getInstance(context);
    }

    @Override
    protected Class<Preference> getPreferenceType() {
        return Preference.class;
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        if (!Flags.microphonePrivacyUpdates()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        boolean hasFeatureMicrophoneToggle = mSensorPrivacyManager.supportsSensorToggle(
                SensorPrivacyManager.Sensors.MICROPHONE);
        return hasFeatureMicrophoneToggle ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }
}
