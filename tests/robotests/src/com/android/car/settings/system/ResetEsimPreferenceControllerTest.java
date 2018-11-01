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

package com.android.car.settings.system;

import static com.android.car.settings.common.BasePreferenceController.AVAILABLE;
import static com.android.car.settings.common.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.provider.Settings;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowEuiccManager;

/** Unit test for {@link ResetEsimPreferenceController}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
public class ResetEsimPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "preference_key";

    private Context mContext;
    private ShadowEuiccManager mShadowEuiccManager;
    private ResetEsimPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mShadowEuiccManager = Shadow.extract(mContext.getSystemService(Context.EUICC_SERVICE));
        mController = new ResetEsimPreferenceController(mContext, PREFERENCE_KEY,
                mock(FragmentController.class));
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.EUICC_PROVISIONED, 0);
    }

    @Test
    public void getAvailabilityStatus_disabledEuiccManager_unsupportedOnDevice() {
        mShadowEuiccManager.setIsEnabled(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_euiccNotProvisioned_unsupportedOnDevice() {
        mShadowEuiccManager.setIsEnabled(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_euiccNotProvisioned_developer_available() {
        mShadowEuiccManager.setIsEnabled(true);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_euiccProvisioned_available() {
        mShadowEuiccManager.setIsEnabled(true);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.EUICC_PROVISIONED, 1);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }
}
