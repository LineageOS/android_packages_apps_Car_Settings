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
package com.android.car.settings.location;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.testutils.ShadowLocationManager;
import com.android.car.settings.testutils.ShadowSecureSettings;
import com.android.settingslib.Utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowSecureSettings.class, ShadowLocationManager.class})
public class RecentLocationRequestsEntryPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "location_recent_requests_entry";

    private RecentLocationRequestsEntryPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.application;
        mController = new RecentLocationRequestsEntryPreferenceController(context, PREFERENCE_KEY,
                mock(FragmentController.class));
        mPreference = new Preference(context);
        mPreference.setKey(PREFERENCE_KEY);
        PreferenceScreen screen = new PreferenceManager(context).createPreferenceScreen(context);
        screen.addPreference(mPreference);
    }

    @Test
    public void updateState_locationOn_preferenceIsEnabled() {
        setLocationEnabled(true);
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_locationOff_preferenceIsDisabled() {
        setLocationEnabled(false);
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void locationModeChangedBroadcastSent_locationOff_preferenceIsDisabled() {
        setLocationEnabled(true);
        mController.updateState(mPreference);
        mController.onStart();
        setLocationEnabled(false);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void locationModeChangedBroadcastSent_locationOn_preferenceIsEnabled() {
        setLocationEnabled(false);
        mController.updateState(mPreference);
        mController.onStart();
        setLocationEnabled(true);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    private void setLocationEnabled(boolean enabled) {
        Utils.updateLocationEnabled(RuntimeEnvironment.application, enabled, UserHandle.myUserId(),
                Settings.Secure.LOCATION_CHANGER_SYSTEM_SETTINGS);
    }
}
