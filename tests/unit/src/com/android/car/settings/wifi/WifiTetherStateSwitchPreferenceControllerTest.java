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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.TetheringManager;
import android.net.wifi.WifiManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.SwitchPreference;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.ColoredSwitchPreference;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.Executor;

@RunWith(AndroidJUnit4.class)
public class WifiTetherStateSwitchPreferenceControllerTest {
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private Context mContext = ApplicationProvider.getApplicationContext();
    private SwitchPreference mSwitchPreference;
    private WifiTetherStateSwitchPreferenceController mPreferenceController;
    private CarUxRestrictions mCarUxRestrictions;

    @Mock
    private CarWifiManager mCarWifiManager;
    @Mock
    private TetheringManager mTetheringManager;
    @Mock
    private FragmentController mFragmentController;
    @Mock
    private androidx.lifecycle.Lifecycle mMockLifecycle;

    @Before
    @UiThreadTest
    public void setUp() {
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        MockitoAnnotations.initMocks(this);

        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();

        mSwitchPreference = new ColoredSwitchPreference(mContext);
        when(mFragmentController.getSettingsLifecycle()).thenReturn(mMockLifecycle);
        mPreferenceController = new WifiTetherStateSwitchPreferenceController(mContext,
                /* preferenceKey= */ "key", mFragmentController, mCarUxRestrictions);
        mPreferenceController.setCarWifiManager(mCarWifiManager);
        mPreferenceController.setTetheringManager(mTetheringManager);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mSwitchPreference);
    }

    @Test
    public void onStart_tetherStateOn_shouldReturnSwitchStateOn() {
        when(mCarWifiManager.isWifiApEnabled()).thenReturn(true);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void onStart_tetherStateOff_shouldReturnSwitchStateOff() {
        when(mCarWifiManager.isWifiApEnabled()).thenReturn(false);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void onSwitchOn_shouldAttemptTetherOn() {
        when(mCarWifiManager.isWifiApEnabled()).thenReturn(false);
        mPreferenceController.onCreate(mLifecycleOwner);

        mSwitchPreference.performClick();

        verify(mTetheringManager).startTethering(
                eq(ConnectivityManager.TETHERING_WIFI),
                any(Executor.class), any(TetheringManager.StartTetheringCallback.class)
        );
    }

    @Test
    public void onSwitchOff_shouldAttemptTetherOff() {
        when(mCarWifiManager.isWifiApEnabled()).thenReturn(true);
        mPreferenceController.onCreate(mLifecycleOwner);

        mSwitchPreference.performClick();

        verify(mTetheringManager).stopTethering(ConnectivityManager.TETHERING_WIFI);
    }

    @Test
    public void onTetherEnabling_shouldReturnSwitchStateDisabled() {
        when(mCarWifiManager.isWifiApEnabled()).thenReturn(false);
        mPreferenceController.onCreate(mLifecycleOwner);

        mPreferenceController.handleWifiApStateChanged(WifiManager.WIFI_AP_STATE_ENABLING);

        assertThat(mSwitchPreference.isEnabled()).isFalse();
    }

    @Test
    public void onTetherEnabled_shouldReturnSwitchStateEnabledAndOn() {
        when(mCarWifiManager.isWifiApEnabled()).thenReturn(false);
        mPreferenceController.onCreate(mLifecycleOwner);

        when(mCarWifiManager.isWifiApEnabled()).thenReturn(true);
        mPreferenceController.handleWifiApStateChanged(WifiManager.WIFI_AP_STATE_ENABLED);

        assertThat(mSwitchPreference.isEnabled()).isTrue();
        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void onTetherDisabled_shouldReturnSwitchStateEnabledAndOff() {
        when(mCarWifiManager.isWifiApEnabled()).thenReturn(true);
        mPreferenceController.onCreate(mLifecycleOwner);

        when(mCarWifiManager.isWifiApEnabled()).thenReturn(false);
        mPreferenceController.handleWifiApStateChanged(WifiManager.WIFI_AP_STATE_DISABLED);

        assertThat(mSwitchPreference.isEnabled()).isTrue();
        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void onEnableTetherFailed_shouldReturnSwitchStateEnabledAndOff() {
        when(mCarWifiManager.isWifiApEnabled()).thenReturn(false);
        mPreferenceController.onCreate(mLifecycleOwner);

        mPreferenceController.handleWifiApStateChanged(WifiManager.WIFI_AP_STATE_ENABLING);
        mPreferenceController.handleWifiApStateChanged(WifiManager.WIFI_AP_STATE_FAILED);

        assertThat(mSwitchPreference.isEnabled()).isTrue();
        assertThat(mSwitchPreference.isChecked()).isFalse();
    }
}
