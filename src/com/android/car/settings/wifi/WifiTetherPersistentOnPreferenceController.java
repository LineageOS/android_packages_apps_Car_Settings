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

package com.android.car.settings.wifi;

import static android.car.settings.CarSettings.Global.ENABLE_PERSISTENT_TETHERING;

import android.car.Car;
import android.car.drivingstate.CarUxRestrictions;
import android.car.feature.Flags;
import android.car.wifi.CarWifiManager;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.TwoStatePreference;

import com.android.car.settings.CarSettingsApplication;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;

/**
 * Controls wifi tethering persistent on configuration
 */
public class WifiTetherPersistentOnPreferenceController extends
        PreferenceController<TwoStatePreference> {

    private static final String ENABLED = "true";
    private static final String DISABLED = "false";

    public WifiTetherPersistentOnPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<TwoStatePreference> getPreferenceType() {
        return TwoStatePreference.class;
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        if (!Flags.persistApSettings()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        CarWifiManager carWifiManager = getCarWifiManager();
        if (carWifiManager != null && carWifiManager.canControlPersistTetheringSettings()) {
            return AVAILABLE;
        }
        return DISABLED_FOR_PROFILE;
    }

    @Override
    protected void updateState(TwoStatePreference preference) {
        boolean isChecked = TextUtils.equals(ENABLED,
                Settings.Global.getString(getContext().getContentResolver(),
                        ENABLE_PERSISTENT_TETHERING));
        preference.setChecked(isChecked);
    }

    @Override
    protected boolean handlePreferenceChanged(TwoStatePreference preference, Object newValue) {
        boolean settingsOn = (Boolean) newValue;

        Settings.Global.putString(getContext().getContentResolver(),
                ENABLE_PERSISTENT_TETHERING, settingsOn ? ENABLED : DISABLED);
        return true;
    }

    private CarWifiManager getCarWifiManager() {
        return ((CarSettingsApplication) getContext().getApplicationContext())
                .getCarWifiManager();
    }
}
