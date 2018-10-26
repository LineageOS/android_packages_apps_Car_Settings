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
import android.content.pm.PackageInfo;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class VersionPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "application_details_version";
    private static final String TEST_VERSION_NAME = "9";

    private Preference mPreference;
    private PreferenceScreen mPreferenceScreen;
    private VersionPreferenceController mController;
    private PackageInfo mPackageInfo;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.application;

        mController = new VersionPreferenceController(context, PREFERENCE_KEY,
                mock(FragmentController.class));
        mPreferenceScreen = new PreferenceManager(context).createPreferenceScreen(context);
        mPreference = new Preference(context);
        mPreference.setKey(PREFERENCE_KEY);
        mPreferenceScreen.addPreference(mPreference);

        mPackageInfo = new PackageInfo();
        mPackageInfo.versionName = TEST_VERSION_NAME;
    }

    @Test
    public void testDisplayPreference_noPackageInfo_throwException() {
        assertThrows(IllegalStateException.class,
                () -> mController.displayPreference(mPreferenceScreen));
    }

    @Test
    public void testDisplayPreference_hasPackageInfo_setTitle() {
        mController.setPackageInfo(mPackageInfo);
        mController.displayPreference(mPreferenceScreen);
        assertThat(mPreference.getTitle()).isEqualTo(
                RuntimeEnvironment.application.getString(R.string.application_version_label,
                        TEST_VERSION_NAME));
    }
}
