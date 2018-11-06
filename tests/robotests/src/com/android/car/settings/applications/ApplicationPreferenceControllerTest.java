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

package com.android.car.settings.applications;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertThrows;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class ApplicationPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "application_details_screen";
    private static final String PACKAGE_NAME = "Test Package Name";

    private Preference mPreference;
    private PreferenceScreen mPreferenceScreen;
    private ApplicationPreferenceController mController;
    @Mock
    private ResolveInfo mResolveInfo;
    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context context = spy(RuntimeEnvironment.application);
        when(context.getPackageManager()).thenReturn(mPackageManager);
        when(mResolveInfo.loadLabel(mPackageManager)).thenReturn(PACKAGE_NAME);

        mPreferenceScreen = new PreferenceManager(context).createPreferenceScreen(context);
        mPreference = new Preference(context);
        mPreference.setKey(PREFERENCE_KEY);
        mPreferenceScreen.addPreference(mPreference);
        mController = new ApplicationPreferenceController(context, PREFERENCE_KEY,
                mock(FragmentController.class));
    }

    @Test
    public void testDisplayPreference_noResolveInfo_throwException() {
        assertThrows(IllegalStateException.class,
                () -> mController.displayPreference(mPreferenceScreen));
    }

    @Test
    public void testDisplayPreference_hasResolveInfo_setTitle() {
        mController.setResolveInfo(mResolveInfo);
        mController.displayPreference(mPreferenceScreen);
        assertThat(mPreference.getTitle()).isEqualTo(PACKAGE_NAME);
    }
}
