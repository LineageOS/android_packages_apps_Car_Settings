/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.car.settings;

import android.app.Activity;
import android.support.v14.preference.SwitchPreference;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import com.android.car.settings.display.AutoBrightnessPreferenceController;
import com.android.car.settings.display.DisplaySettings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.Robolectric;

import static com.google.common.truth.Truth.assertThat;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AutoBrightnessPreferenceControllerTest {
    private Context mContext;

    private SwitchPreference mSwitchPreference;

    private AutoBrightnessPreferenceController mPreferenceController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mSwitchPreference = new SwitchPreference(mContext);
        mPreferenceController = new AutoBrightnessPreferenceController(mContext);
    }

    @Test
    public void testStateUpdate() {
        Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE,
            SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        mPreferenceController.updateState(mSwitchPreference);
        assertThat(mSwitchPreference.isChecked()).isTrue();
        Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE,
            SCREEN_BRIGHTNESS_MODE_MANUAL);
        mPreferenceController.updateState(mSwitchPreference);
        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void testOnPreferenceChange() {
        mPreferenceController.onPreferenceChange(mSwitchPreference, true);
        assertThat(Settings.System.getInt(mContext.getContentResolver(),
            SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL))
            .isEqualTo(SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        mPreferenceController.onPreferenceChange(mSwitchPreference, false);
        assertThat(Settings.System.getInt(mContext.getContentResolver(),
            SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL))
            .isEqualTo(SCREEN_BRIGHTNESS_MODE_MANUAL);
    }
}
