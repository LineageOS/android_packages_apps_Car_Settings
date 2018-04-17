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
 * limitations under the License
 */

package com.android.car.settings.display;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AutoBrightnessLineItemTest {
    private Context mContext;

    private AutoBrightnessLineItem mAutoBrightnessLineItem;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mAutoBrightnessLineItem = new AutoBrightnessLineItem(mContext);
    }

    @Test
    public void testIsChecked() {
        Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE,
                SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        assertThat(mAutoBrightnessLineItem.isChecked()).isTrue();
        Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE,
                SCREEN_BRIGHTNESS_MODE_MANUAL);
        assertThat(mAutoBrightnessLineItem.isChecked()).isFalse();
    }

    @Test
    public void testOnClick() {
        ViewGroup parent = new LinearLayout(mContext);
        Switch toggleSwitch = new Switch(mContext);
        toggleSwitch.setId(R.id.toggle_switch);
        parent.addView(toggleSwitch);
        toggleSwitch.setChecked(false);
        mAutoBrightnessLineItem.onClick(parent);
        assertThat(Settings.System.getInt(mContext.getContentResolver(),
                SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL))
                .isEqualTo(SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        toggleSwitch.setChecked(true);
        mAutoBrightnessLineItem.onClick(parent);
        assertThat(Settings.System.getInt(mContext.getContentResolver(),
                SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL))
                .isEqualTo(SCREEN_BRIGHTNESS_MODE_MANUAL);
    }
}
