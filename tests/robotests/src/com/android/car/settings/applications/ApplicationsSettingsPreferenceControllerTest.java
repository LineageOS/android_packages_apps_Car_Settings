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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
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

import java.util.ArrayList;
import java.util.List;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class ApplicationsSettingsPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "applications_settings_screen";
    private static final String APP_NAME_1 = "Some Application";
    private static final String APP_NAME_2 = "Other Application";

    private PreferenceScreen mPreferenceScreen;
    private ApplicationsSettingsPreferenceController mController;
    @Mock
    private FragmentController mFragmentController;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ResolveInfo mResolveInfo1;
    @Mock
    private ResolveInfo mResolveInfo2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context context = spy(RuntimeEnvironment.application);
        when(context.getPackageManager()).thenReturn(mPackageManager);

        mPreferenceScreen = new PreferenceManager(context).createPreferenceScreen(context);
        mPreferenceScreen.setKey(PREFERENCE_KEY);
        mController = new ApplicationsSettingsPreferenceController(context, PREFERENCE_KEY,
                mFragmentController);

        when(mResolveInfo1.loadLabel(any(PackageManager.class))).thenReturn(APP_NAME_1);
        when(mResolveInfo2.loadLabel(any(PackageManager.class))).thenReturn(APP_NAME_2);

        List<ResolveInfo> testList = new ArrayList<>();
        testList.add(mResolveInfo1);
        testList.add(mResolveInfo2);

        // Cannot use specific intent because it doesn't have a proper "equals" method.
        when(mPackageManager.queryIntentActivities(any(Intent.class),
                eq(PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
                        | PackageManager.MATCH_DISABLED_COMPONENTS))).thenReturn(testList);
    }

    @Test
    public void displayPreference_hasElements() {
        mController.displayPreference(mPreferenceScreen);
        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    public void displayPreference_orderIsCorrect() {
        mController.displayPreference(mPreferenceScreen);
        List<String> computedOrder = new ArrayList<>();
        for (int i = 0; i < mPreferenceScreen.getPreferenceCount(); i++) {
            computedOrder.add(mPreferenceScreen.getPreference(i).getTitle().toString());
        }

        assertThat(computedOrder).containsExactly(APP_NAME_2, APP_NAME_1);
    }

    @Test
    public void displayPreference_preferenceClick() {
        mController.displayPreference(mPreferenceScreen);
        Preference preference = mPreferenceScreen.getPreference(0);
        preference.performClick();
        verify(mFragmentController).launchFragment(any(ApplicationDetailsFragment.class));
    }

    @Test
    public void displayPreference_multipleCalls() {
        mController.displayPreference(mPreferenceScreen);
        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(2);

        // Second call shouldn't add more items to the list.
        mController.displayPreference(mPreferenceScreen);
        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(2);
    }
}
