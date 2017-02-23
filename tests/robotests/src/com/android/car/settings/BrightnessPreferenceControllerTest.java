/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.support.v7.preference.SeekBarPreference;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import com.android.car.settings.display.BrightnessPreferenceController;
import com.android.car.settings.display.DisplaySettings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.Robolectric;

import static com.google.common.truth.Truth.assertThat;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BrightnessPreferenceControllerTest {
    private Context mContext;

    private SeekBarPreference mSeekBarPreference;

    private BrightnessPreferenceController mPreferenceController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mSeekBarPreference = new SeekBarPreference(mContext);
        mPreferenceController = new BrightnessPreferenceController(mContext);
    }

    @Test
    public void testStateUpdate() throws Exception {
        // for some reason in robolectric test, I can't set this value over 100
        for (int brightness = 0; brightness < 100; ++brightness) {
            Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS, brightness);
            mPreferenceController.updateState(mSeekBarPreference);
            assertThat(mSeekBarPreference.getValue()).isEqualTo(brightness);
        }
    }

    @Test
    public void testOnPreferenceChange() throws Exception {
        for (int brightness = 0; brightness < 255; ++brightness) {
            mPreferenceController.onPreferenceChange(mSeekBarPreference, brightness);
            assertThat(Settings.System.getInt(mContext.getContentResolver(),
                    SCREEN_BRIGHTNESS)).isEqualTo(brightness);
        }
    }
}
