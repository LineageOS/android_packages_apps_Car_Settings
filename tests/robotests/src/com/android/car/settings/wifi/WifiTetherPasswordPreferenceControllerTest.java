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

package com.android.car.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.text.InputType;

import androidx.lifecycle.Lifecycle;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.car.settings.common.ValidatedEditTextPreference;
import com.android.car.settings.testutils.ShadowCarWifiManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowCarWifiManager.class})
public class WifiTetherPasswordPreferenceControllerTest {

    private static final String TEST_PASSWORD = "TEST_PASSWORD";

    private Context mContext;
    private ValidatedEditTextPreference mPreference;
    private PreferenceControllerTestHelper<WifiTetherPasswordPreferenceController>
            mControllerHelper;
    private CarWifiManager mCarWifiManager;

    @Before
    public void setup() {
        mContext = RuntimeEnvironment.application;
        mCarWifiManager = new CarWifiManager(mContext);
        mPreference = new ValidatedEditTextPreference(mContext);
        mControllerHelper =
                new PreferenceControllerTestHelper<WifiTetherPasswordPreferenceController>(mContext,
                        WifiTetherPasswordPreferenceController.class, mPreference);
    }

    @After
    public void tearDown() {
        ShadowCarWifiManager.reset();
    }

    @Test
    public void onStart_wifiConfigHasPassword_setsSummary() {
        WifiConfiguration config = new WifiConfiguration();
        config.preSharedKey = TEST_PASSWORD;
        getShadowCarWifiManager().setWifiApConfig(config);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);

        assertThat(mPreference.getSummary()).isEqualTo(TEST_PASSWORD);
    }

    @Test
    public void onStart_wifiConfigHasPassword_obscuresSummary() {
        WifiConfiguration config = new WifiConfiguration();
        config.preSharedKey = TEST_PASSWORD;
        getShadowCarWifiManager().setWifiApConfig(config);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);

        assertThat(mPreference.getSummaryInputType())
                .isEqualTo((InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
    }

    @Test
    public void onStart_wifiConfigHasNoPassword_doesNotObscureSummary() {
        WifiConfiguration config = new WifiConfiguration();
        config.preSharedKey = null;
        getShadowCarWifiManager().setWifiApConfig(config);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);

        assertThat(mPreference.getSummaryInputType()).isEqualTo(InputType.TYPE_CLASS_TEXT);
    }

    private ShadowCarWifiManager getShadowCarWifiManager() {
        return Shadow.extract(mCarWifiManager);
    }
}
