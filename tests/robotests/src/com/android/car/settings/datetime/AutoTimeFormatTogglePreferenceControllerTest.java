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

package com.android.car.settings.datetime;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.preference.SwitchPreference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import java.util.List;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class AutoTimeFormatTogglePreferenceControllerTest {
    private static final String PREFERENCE_KEY = "use_24hour_switch_entry";

    private ShadowApplication mApplication;
    private Context mContext;
    private SwitchPreference mPreference;
    private AutoTimeFormatTogglePreferenceController mController;

    @Before
    public void setUp() {
        mApplication = ShadowApplication.getInstance();
        mContext = RuntimeEnvironment.application;
        mController = new AutoTimeFormatTogglePreferenceController(mContext, PREFERENCE_KEY);
        mPreference = new SwitchPreference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void updateState_24HourSet_shouldCheckPreference() {
        Settings.System.putString(mContext.getContentResolver(), Settings.System.TIME_12_24,
                AutoTimeFormatTogglePreferenceController.HOURS_24);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_12HourSet_shouldUncheckPreference() {
        Settings.System.putString(mContext.getContentResolver(), Settings.System.TIME_12_24,
                AutoTimeFormatTogglePreferenceController.HOURS_12);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updatePreference_24HourSet_shouldSendIntent() {
        mPreference.setChecked(true);
        mController.onPreferenceChange(mPreference, true);

        List<Intent> intentsFired = mApplication.getBroadcastIntents();
        assertThat(intentsFired.size()).isEqualTo(1);
        Intent intentFired = intentsFired.get(0);
        assertThat(intentFired.getAction()).isEqualTo(Intent.ACTION_TIME_CHANGED);
        assertThat(intentFired.getIntExtra(Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT, -1))
                .isEqualTo(Intent.EXTRA_TIME_PREF_VALUE_USE_24_HOUR);
    }

    @Test
    public void updatePreference_12HourSet_shouldSendIntent() {
        mPreference.setChecked(false);
        mController.onPreferenceChange(mPreference, false);

        List<Intent> intentsFired = mApplication.getBroadcastIntents();
        assertThat(intentsFired.size()).isEqualTo(1);
        Intent intentFired = intentsFired.get(0);
        assertThat(intentFired.getAction()).isEqualTo(Intent.ACTION_TIME_CHANGED);
        assertThat(intentFired.getIntExtra(Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT, -1))
                .isEqualTo(Intent.EXTRA_TIME_PREF_VALUE_USE_12_HOUR);
    }
}
