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

package com.android.car.settings.bluetooth;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.pm.PackageManager;
import android.uwb.UwbManager;

import androidx.preference.TwoStatePreference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;

/**
 * Controls uwb feature configuration
 */
public class UwbTogglePreferenceController extends PreferenceController<TwoStatePreference> {

    private final UwbManager mUwbManager;

    public UwbTogglePreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mUwbManager = context.getSystemService(UwbManager.class);
    }

    @Override
    protected Class<TwoStatePreference> getPreferenceType() {
        return TwoStatePreference.class;
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        if (getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_UWB)) {
            return AVAILABLE;
        }
        return UNSUPPORTED_ON_DEVICE;
    }

    @Override
    protected void updateState(TwoStatePreference preference) {
        preference.setChecked(mUwbManager != null ? mUwbManager.isUwbEnabled() : false);
    }

    @Override
    protected boolean handlePreferenceChanged(TwoStatePreference preference, Object newValue) {
        boolean settingsOn = (Boolean) newValue;
        if (mUwbManager != null) {
            mUwbManager.setUwbEnabled(settingsOn);
        }
        return true;
    }
}

