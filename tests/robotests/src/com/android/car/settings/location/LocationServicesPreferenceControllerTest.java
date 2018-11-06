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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.location.SettingInjectorService;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;
import com.android.settingslib.location.SettingsInjector;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class LocationServicesPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "location_services";

    @Mock
    private SettingsInjector mSettingsInjector;

    private Context mContext;
    private LocationServicesPreferenceController mController;
    private PreferenceScreen mScreen;
    private PreferenceCategory mCategory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new LocationServicesPreferenceController(mContext, PREFERENCE_KEY,
                mock(FragmentController.class), mSettingsInjector);

        mCategory = new PreferenceCategory(mContext);
        mCategory.setKey(PREFERENCE_KEY);
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mScreen.addPreference(mCategory);
    }

    @Test
    public void onStart_RegistersBroadcastReceiver() {
        mController.onStart();
        mContext.sendBroadcast(new Intent(SettingInjectorService.ACTION_INJECTED_SETTING_CHANGED));
        verify(mSettingsInjector).reloadStatusMessages();
    }

    @Test
    public void onStop_ShouldUnregistersBroadcastReceiver() {
        mController.onStart();
        mContext.sendBroadcast(new Intent(SettingInjectorService.ACTION_INJECTED_SETTING_CHANGED));
        verify(mSettingsInjector).reloadStatusMessages();

        mController.onStop();
        mContext.sendBroadcast(new Intent(SettingInjectorService.ACTION_INJECTED_SETTING_CHANGED));
        verifyNoMoreInteractions(mSettingsInjector);
    }

    @Test
    public void displayPreference_addsInjectedSettingsToPreferenceCategory() {
        List<Preference> samplePrefs = getSamplePreferences();
        when(mSettingsInjector.hasInjectedSettings(anyInt())).thenReturn(true);
        doReturn(samplePrefs).when(mSettingsInjector)
                .getInjectedSettings(any(Context.class), anyInt());
        mController.displayPreference(mScreen);

        assertThat(mCategory.getPreferenceCount()).isEqualTo(samplePrefs.size());
    }

    @Test
    public void preferenceCategory_isVisibleIfThereAreInjectedSettings() {
        doReturn(true).when(mSettingsInjector).hasInjectedSettings(anyInt());
        doReturn(getSamplePreferences()).when(mSettingsInjector)
                .getInjectedSettings(any(Context.class), anyInt());
        mController.displayPreference(mScreen);

        assertThat(mCategory.isVisible()).isTrue();
    }

    @Test
    public void preferenceCategory_isHiddenIfThereAreNoInjectedSettings() {
        doReturn(false).when(mSettingsInjector).hasInjectedSettings(anyInt());
        mController.displayPreference(mScreen);

        assertThat(mCategory.isVisible()).isFalse();
    }

    private List<Preference> getSamplePreferences() {
        return new ArrayList<>(Arrays.asList(
                new Preference(mContext), new Preference(mContext), new Preference(mContext)));
    }
}
