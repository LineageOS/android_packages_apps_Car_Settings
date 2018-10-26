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
import static org.testng.Assert.assertThrows;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class PermissionsPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "application_details_permissions";
    private static final String PACKAGE_NAME = "Test Package Name";

    private Context mContext;
    private Preference mPreference;
    private PreferenceScreen mPreferenceScreen;
    private PermissionsPreferenceController mController;
    private ResolveInfo mResolveInfo;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;

        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreference = new Preference(mContext);
        mPreference.setKey(PREFERENCE_KEY);
        mPreferenceScreen.addPreference(mPreference);
        mController = new PermissionsPreferenceController(mContext, PREFERENCE_KEY,
                mock(FragmentController.class));

        mResolveInfo = new ResolveInfo();
        mResolveInfo.resolvePackageName = PACKAGE_NAME;
        mResolveInfo.activityInfo = new ActivityInfo();
        mResolveInfo.activityInfo.packageName = PACKAGE_NAME;
    }

    @Test
    public void testOnCreate_noResolveInfo_throwException() {
        assertThrows(IllegalStateException.class, () -> mController.onCreate());
    }

    @Test
    public void testHandlePreferenceTreeClick_wrongPreference_noAction() {
        // Setup so the controller knows about the preference. Otherwise handlePreferenceTreeClick
        // doesn't know which preference to compare the input against.
        mController.setResolveInfo(mResolveInfo);
        mController.displayPreference(mPreferenceScreen);

        assertThat(mController.handlePreferenceTreeClick(new Preference(mContext))).isFalse();
    }

    @Test
    public void testHandlePreferenceTreeClick_navigateToNextActivity() {
        // Setup so the controller knows about the preference.
        mController.setResolveInfo(mResolveInfo);
        mController.displayPreference(mPreferenceScreen);

        assertThat(mController.handlePreferenceTreeClick(mPreference)).isTrue();

        Intent actual = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(actual.getAction()).isEqualTo(Intent.ACTION_MANAGE_APP_PERMISSIONS);
        assertThat(actual.getStringExtra(Intent.EXTRA_PACKAGE_NAME)).isEqualTo(PACKAGE_NAME);
    }
}
