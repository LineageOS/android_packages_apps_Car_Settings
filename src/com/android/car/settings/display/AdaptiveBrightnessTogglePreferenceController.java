/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.settings.display;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.NoSetupPreferenceController;
import com.android.car.settings.common.PreferenceUtil;

/** Business logic for controlling the adaptive brightness setting. */
public class AdaptiveBrightnessTogglePreferenceController extends
        NoSetupPreferenceController implements
        Preference.OnPreferenceChangeListener {

    public AdaptiveBrightnessTogglePreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        PreferenceUtil.requirePreferenceType(preference, TwoStatePreference.class);
        ((TwoStatePreference) preference).setChecked(isAdaptiveBrightnessChecked());
    }

    @Override
    public int getAvailabilityStatus() {
        return supportsAdaptiveBrightness() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        PreferenceUtil.requirePreferenceType(preference, TwoStatePreference.class);
        boolean enableAdaptiveBrightness = (boolean) newValue;
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                enableAdaptiveBrightness ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                        : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        return true;
    }

    private boolean isAdaptiveBrightnessChecked() {
        int brightnessMode = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        return brightnessMode != Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
    }

    private boolean supportsAdaptiveBrightness() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);
    }
}
