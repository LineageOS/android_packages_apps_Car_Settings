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

import static com.android.settingslib.display.BrightnessUtils.GAMMA_SPACE_MAX;
import static com.android.settingslib.display.BrightnessUtils.convertLinearToGamma;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertThrows;

import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.SeekBarPreference;
import com.android.car.settings.testutils.ShadowCarUserManagerHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class BrightnessLevelPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "brightness_level";
    private static final int CURRENT_USER = 10;
    private Context mContext;
    private BrightnessLevelPreferenceController mController;
    private PreferenceScreen mPreferenceScreen;
    private SeekBarPreference mSeekBarPreference;
    private int mMin;
    private int mMax;
    private int mMid;
    @Mock
    private CarUserManagerHelper mCarUserManagerHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mMin = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_screenBrightnessSettingMinimum);
        mMax = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_screenBrightnessSettingMaximum);
        mMid = (mMax + mMin) / 2;

        ShadowCarUserManagerHelper.setMockInstance(mCarUserManagerHelper);
        when(mCarUserManagerHelper.getCurrentProcessUserId()).thenReturn(CURRENT_USER);

        mController = new BrightnessLevelPreferenceController(mContext, PREFERENCE_KEY,
                mock(FragmentController.class));
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mSeekBarPreference = new SeekBarPreference(mContext);
        mSeekBarPreference.setKey(mController.getPreferenceKey());
    }

    @After
    public void tearDown() {
        ShadowCarUserManagerHelper.reset();
    }

    @Test
    public void testDisplayPreferences_maxSet() {
        mPreferenceScreen.addPreference(mSeekBarPreference);
        mController.displayPreference(mPreferenceScreen);
        assertThat(mSeekBarPreference.getMax()).isEqualTo(GAMMA_SPACE_MAX);
    }

    @Test
    public void testDisplayPreferences_minValue() {
        mPreferenceScreen.addPreference(mSeekBarPreference);
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, mMin,
                mCarUserManagerHelper.getCurrentProcessUserId());

        mController.displayPreference(mPreferenceScreen);
        assertThat(mSeekBarPreference.getValue()).isEqualTo(0);
    }

    @Test
    public void testDisplayPreferences_maxValue() {
        mPreferenceScreen.addPreference(mSeekBarPreference);
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, mMax,
                mCarUserManagerHelper.getCurrentProcessUserId());

        mController.displayPreference(mPreferenceScreen);
        assertThat(mSeekBarPreference.getValue()).isEqualTo(GAMMA_SPACE_MAX);
    }

    @Test
    public void testDisplayPreferences_midValue() {
        mPreferenceScreen.addPreference(mSeekBarPreference);
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, mMid,
                mCarUserManagerHelper.getCurrentProcessUserId());

        mController.displayPreference(mPreferenceScreen);
        assertThat(mSeekBarPreference.getValue()).isEqualTo(
                convertLinearToGamma(mMid,
                        mMin, mMax));
    }

    @Test
    public void testDisplayPreferences_wrongPreferenceType() {
        androidx.preference.SeekBarPreference preference =
                new androidx.preference.SeekBarPreference(mContext);
        preference.setKey(PREFERENCE_KEY);
        mPreferenceScreen.addPreference(preference);
        assertThrows(IllegalStateException.class,
                () -> mController.displayPreference(mPreferenceScreen));
    }

    @Test
    public void testOnPreferenceChange_minValue() throws Settings.SettingNotFoundException {
        mController.onPreferenceChange(mSeekBarPreference, 0);
        int currentSettingsVal = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS,
                mCarUserManagerHelper.getCurrentProcessUserId());
        assertThat(currentSettingsVal).isEqualTo(mMin);
    }

    @Test
    public void testOnPreferenceChange_maxValue() throws Settings.SettingNotFoundException {
        mController.onPreferenceChange(mSeekBarPreference, GAMMA_SPACE_MAX);
        int currentSettingsVal = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS,
                mCarUserManagerHelper.getCurrentProcessUserId());
        assertThat(currentSettingsVal).isEqualTo(mMax);
    }

    @Test
    public void testOnPreferenceChange_midValue() throws Settings.SettingNotFoundException {
        mController.onPreferenceChange(mSeekBarPreference, convertLinearToGamma(mMid, mMin, mMax));
        int currentSettingsVal = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS,
                mCarUserManagerHelper.getCurrentProcessUserId());
        assertThat(currentSettingsVal).isEqualTo(mMid);
    }

    @Test
    public void testOnPreferenceChange_wrongPreferenceType() {
        assertThrows(IllegalStateException.class,
                () -> mController.onPreferenceChange(
                        new androidx.preference.SeekBarPreference(mContext), true));
    }
}
