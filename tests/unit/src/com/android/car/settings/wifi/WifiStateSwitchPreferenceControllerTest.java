/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.net.wifi.WifiManager;

import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.ColoredSwitchPreference;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class WifiStateSwitchPreferenceControllerTest {
    private Context mContext = ApplicationProvider.getApplicationContext();
    private SwitchPreference mSwitchPreference;
    private WifiStateSwitchPreferenceController mPreferenceController;
    private CarUxRestrictions mCarUxRestrictions;
    private CarWifiManager mCarWifiManager;

    @Mock
    private FragmentController mFragmentController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mCarWifiManager = new CarWifiManager(mContext);

        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();

        mSwitchPreference = new ColoredSwitchPreference(mContext);
        mPreferenceController = new WifiStateSwitchPreferenceController(mContext,
                /* preferenceKey= */ "key", mFragmentController, mCarUxRestrictions);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mSwitchPreference);
    }

    @Test
    public void onWifiStateChanged_disabled_setsSwitchUnchecked() {
        initializePreference(/* enabled= */ true);
        mPreferenceController.onWifiStateChanged(WifiManager.WIFI_STATE_DISABLED);

        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void onWifiStateChanged_enabled_setsSwitchChecked() {
        initializePreference(/* enabled= */ false);
        mPreferenceController.onWifiStateChanged(WifiManager.WIFI_STATE_ENABLED);

        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void onWifiStateChanged_enabling_setsSwitchChecked() {
        initializePreference(/* enabled= */ false);
        mPreferenceController.onWifiStateChanged(WifiManager.WIFI_STATE_ENABLING);

        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    private void initializePreference(boolean enabled) {
        mCarWifiManager.setWifiEnabled(enabled);
        mSwitchPreference.setChecked(enabled);
    }
}
