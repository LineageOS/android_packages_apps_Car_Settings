/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.car.settings.display;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;

import android.content.Context;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v7.preference.Preference;
import android.support.v7.preference.SeekBarPreference;
import android.util.Log;
import com.android.car.settings.core.CarPreferenceController;
import com.android.internal.annotations.VisibleForTesting;


public class BrightnessPreferenceController extends CarPreferenceController{
    private static final String TAG = "BrightnessPreferenceController";

    // key string to match the key defined in display_settings.xml file.
    @VisibleForTesting
    public static final String KEY_BRIGHTNESS = "brightness";

    public BrightnessPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BRIGHTNESS;
    }

    @Override
    public void updateState(Preference preference) {
        int currentBrightness = 0;
        try {
            currentBrightness = Settings.System.getInt(mContext.getContentResolver(),
                SCREEN_BRIGHTNESS);
        } catch (SettingNotFoundException e) {
            Log.w(TAG, "Can't find setting for SCREEN_BRIGHTNESS.");
        }
        ((SeekBarPreference) preference).setValue(currentBrightness);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Integer brightness = (Integer) newValue;
        Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS, brightness);
        return true;
    }
}
