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

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class TimeZonePickerPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "timezone_picker_entry";

    private Context mContext;
    private Preference mPreference;
    private PreferenceScreen mPreferenceScreen;
    private TimeZonePickerPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mController = new TimeZonePickerPreferenceController(mContext, PREFERENCE_KEY);
        mPreference = new Preference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void testUpdateState_disabled() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.AUTO_TIME_ZONE, 1);
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void testUpdateState_enabled() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.AUTO_TIME_ZONE, 0);
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void testUpdateState_fromBroadcastReceiver_disabled() {
        // This setup is necessary for the BroadcastReceiver to have a reference to the appropriate
        // preference.
        mPreferenceScreen.addPreference(mPreference);
        mController.displayPreference(mPreferenceScreen);
        mPreference.setEnabled(true);
        mController.onStart();

        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.AUTO_TIME_ZONE, 1);
        mContext.sendBroadcast(new Intent(Intent.ACTION_TIME_CHANGED));
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void testUpdateState_fromBroadcastReceiver_enabled() {
        // This setup is necessary for the BroadcastReceiver to have a reference to the appropriate
        // preference.
        mPreferenceScreen.addPreference(mPreference);
        mController.displayPreference(mPreferenceScreen);
        mPreference.setEnabled(false);
        mController.onStart();

        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.AUTO_TIME_ZONE, 0);
        mContext.sendBroadcast(new Intent(Intent.ACTION_TIME_CHANGED));
        assertThat(mPreference.isEnabled()).isTrue();
    }
}
