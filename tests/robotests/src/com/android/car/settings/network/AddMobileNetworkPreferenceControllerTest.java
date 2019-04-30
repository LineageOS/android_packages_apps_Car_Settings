/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.settings.network;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.pm.ResolveInfo;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.PreferenceControllerTestHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class AddMobileNetworkPreferenceControllerTest {

    private Context mContext;
    private Preference mPreference;
    private PreferenceControllerTestHelper<AddMobileNetworkPreferenceController> mControllerHelper;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new Preference(mContext);
        mControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                AddMobileNetworkPreferenceController.class, mPreference);
    }

    @After
    public void tearDown() {
        ShadowPackageManager.reset();
    }

    @Test
    public void onCreate_intentResolves_isVisible() {
        getShadowPackageManager().addResolveInfoForIntent(
                AddMobileNetworkPreferenceController.ADD_NETWORK_INTENT, new ResolveInfo());
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void onCreate_intentFailsToResolve_isNotVisible() {
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        assertThat(mPreference.isVisible()).isTrue();
    }

    private ShadowPackageManager getShadowPackageManager() {
        return Shadow.extract(mContext.getPackageManager());
    }
}
