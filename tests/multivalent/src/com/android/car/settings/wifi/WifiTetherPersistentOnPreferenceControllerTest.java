/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static android.car.settings.CarSettings.Global.ENABLE_PERSISTENT_TETHERING;

import static com.android.car.settings.common.PreferenceController.AVAILABLE;
import static com.android.car.settings.common.PreferenceController.DISABLED_FOR_PROFILE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.car.wifi.CarWifiManager;
import android.content.Context;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.CarSettingsApplication;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.testutils.TestLifecycleOwner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class WifiTetherPersistentOnPreferenceControllerTest {
    private Context mContext = spy(ApplicationProvider.getApplicationContext());
    private SwitchPreference mPreference;
    private LifecycleOwner mLifecycleOwner;
    private WifiTetherPersistentOnPreferenceController mPreferenceController;
    private CarUxRestrictions mCarUxRestrictions;

    @Mock
    private FragmentController mFragmentController;
    @Mock
    private CarSettingsApplication mCarSettingsApplication;
    @Mock
    private CarWifiManager mCarWifiManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = new TestLifecycleOwner();

        when(mContext.getApplicationContext()).thenReturn(mCarSettingsApplication);
        when(mCarSettingsApplication.getCarWifiManager()).thenReturn(mCarWifiManager);

        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();
        mPreferenceController = new WifiTetherPersistentOnPreferenceController(mContext,
                /* preferenceKey= */ "key", mFragmentController, mCarUxRestrictions);
        mPreference = new SwitchPreference(mContext);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
    }

    @Test
    public void getDefaultAvailabilityStatus_noAvailableOnDevice() {
        when(mCarWifiManager.canControlPersistTetheringSettings()).thenReturn(false);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        PreferenceControllerTestUtil.assertAvailability(
                mPreferenceController.getAvailabilityStatus(), DISABLED_FOR_PROFILE);
    }

    @Test
    public void testDefaultAvailabilityStatus_availableOnDevice() {
        when(mCarWifiManager.canControlPersistTetheringSettings()).thenReturn(true);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        PreferenceControllerTestUtil.assertAvailability(
                mPreferenceController.getAvailabilityStatus(), AVAILABLE);
    }

    @Test
    public void testUpdateState_stateChecked() {
        when(mCarWifiManager.canControlPersistTetheringSettings()).thenReturn(true);
        Settings.Global.putString(mContext.getContentResolver(), ENABLE_PERSISTENT_TETHERING,
                WifiTetherPersistentOnPreferenceController.ENABLED);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void testUpdateState_stateNotChecked() {
        when(mCarWifiManager.canControlPersistTetheringSettings()).thenReturn(true);
        Settings.Global.putString(mContext.getContentResolver(), ENABLE_PERSISTENT_TETHERING,
                WifiTetherPersistentOnPreferenceController.DISABLED);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void testHandlePreferenceChanged_enableUWB() {
        when(mCarWifiManager.canControlPersistTetheringSettings()).thenReturn(true);
        Settings.Global.putString(mContext.getContentResolver(), ENABLE_PERSISTENT_TETHERING,
                WifiTetherPersistentOnPreferenceController.DISABLED);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
        mPreferenceController.handlePreferenceChanged(mPreference, true);

        assertThat(Settings.Global.getString(mContext.getContentResolver(),
                ENABLE_PERSISTENT_TETHERING)).isEqualTo(
                WifiTetherPersistentOnPreferenceController.ENABLED);
    }
}
