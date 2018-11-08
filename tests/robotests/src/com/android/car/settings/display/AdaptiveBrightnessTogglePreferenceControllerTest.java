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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertThrows;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class AdaptiveBrightnessTogglePreferenceControllerTest {

    private static final String PREFERENCE_KEY = "adaptive_brightness_switch";
    private Context mContext;
    private AdaptiveBrightnessTogglePreferenceController mController;
    private SwitchPreference mSwitchPreference;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new AdaptiveBrightnessTogglePreferenceController(mContext, PREFERENCE_KEY,
                mock(FragmentController.class));
        mSwitchPreference = new SwitchPreference(mContext);
        mSwitchPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void testUpdateState_manualMode_isNotChecked() {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

        mController.updateState(mSwitchPreference);
        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void testUpdateState_automaticMode_isChecked() {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);

        mController.updateState(mSwitchPreference);
        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void testUpdateState_wrongPreferenceType() {
        assertThrows(IllegalStateException.class,
                () -> mController.updateState(new Preference(mContext)));
    }

    @Test
    public void testOnPreferenceChanged_setFalse() {
        mController.onPreferenceChange(mSwitchPreference, false);
        int brightnessMode = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        assertThat(brightnessMode).isEqualTo(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    @Test
    public void testOnPreferenceChanged_setTrue() {
        mController.onPreferenceChange(mSwitchPreference, true);
        int brightnessMode = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        assertThat(brightnessMode).isEqualTo(Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }

    @Test
    public void testOnPreferenceChange_wrongPreferenceType() {
        assertThrows(IllegalStateException.class,
                () -> mController.onPreferenceChange(new Preference(mContext), true));
    }
}
