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

import static android.net.wifi.SoftApConfiguration.BAND_2GHZ;
import static android.net.wifi.SoftApConfiguration.BAND_5GHZ;

import static com.android.car.settings.wifi.WifiTetherApBandPreferenceController.BAND_2GHZ_5GHZ;
import static com.android.car.settings.wifi.WifiTetherApBandPreferenceController.DUAL_BANDS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.SparseIntArray;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.ListPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.Flags;
import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.testutils.TestLifecycleOwner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class WifiTetherApBandPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private Context mContext = ApplicationProvider.getApplicationContext();
    private ListPreference mPreference;
    private LifecycleOwner mLifecycleOwner;
    private WifiTetherApBandPreferenceController mPreferenceController;
    private CarUxRestrictions mCarUxRestrictions;

    @Mock
    private FragmentController mFragmentController;
    @Mock
    private Lifecycle mMockLifecycle;
    @Mock
    private CarWifiManager mCarWifiManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = new TestLifecycleOwner();
        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();
        when(mFragmentController.getSettingsLifecycle()).thenReturn(mMockLifecycle);
        mPreference = new ListPreference(mContext);
        when(mCarWifiManager.getCountryCode()).thenReturn("1");
        mPreferenceController = new WifiTetherApBandPreferenceController(mContext,
                /* preferenceKey= */ "key", mFragmentController, mCarUxRestrictions,
                mCarWifiManager);

        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
    }

    @Test
    public void onStart_5GhzBandNotSupported_defaultTo2Ghz() {
        when(mCarWifiManager.is5GhzBandSupported()).thenReturn(false);
        when(mCarWifiManager.getSoftApConfig()).thenReturn(
                new SoftApConfiguration.Builder().setBand(BAND_5GHZ).build());

        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.wifi_ap_choose_2G));
    }

    @Test
    @EnableFlags(Flags.FLAG_HOTSPOT_UI_SPEED_UPDATE)
    public void onStart_dualBandNotSupported_defaultTo2Ghz() {
        when(mCarWifiManager.is5GhzBandSupported()).thenReturn(true);
        when(mCarWifiManager.isDualBandSupported()).thenReturn(false);
        when(mCarWifiManager.getSoftApConfig()).thenReturn(
                new SoftApConfiguration.Builder().setBands(DUAL_BANDS).build());

        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.wifi_ap_choose_2G));
        assertThat(mPreference.getValue()).isEqualTo(
                Integer.toString(SoftApConfiguration.BAND_2GHZ));
    }

    @Test
    public void onStart_wifiConfigApBandSetTo5Ghz_valueIsSetTo5Ghz() {
        when(mCarWifiManager.is5GhzBandSupported()).thenReturn(true);
        when(mCarWifiManager.getSoftApConfig()).thenReturn(
                new SoftApConfiguration.Builder().setBand(BAND_5GHZ).build());

        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.wifi_ap_prefer_5G));
        assertThat(mPreference.getValue()).isEqualTo(
                Integer.toString(SoftApConfiguration.BAND_5GHZ));
    }

    @Test
    @EnableFlags(Flags.FLAG_HOTSPOT_UI_SPEED_UPDATE)
    public void onStart_wifiConfigDualApBand_valueIsSetToDualBand() {
        when(mCarWifiManager.is5GhzBandSupported()).thenReturn(true);
        when(mCarWifiManager.isDualBandSupported()).thenReturn(true);
        when(mCarWifiManager.getSoftApConfig()).thenReturn(
                new SoftApConfiguration.Builder().setBands(DUAL_BANDS).build());

        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.wifi_ap_2G_5G));
        assertThat(mPreference.getValue()).isEqualTo(
                Integer.toString(BAND_2GHZ_5GHZ));
    }

    @Test
    public void onStart_wifiConfigApBandSetTo2Ghz_valueIsSetTo2Ghz() {
        when(mCarWifiManager.is5GhzBandSupported()).thenReturn(true);
        when(mCarWifiManager.getSoftApConfig()).thenReturn(
                new SoftApConfiguration.Builder().setBand(BAND_2GHZ).build());

        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        assertThat(mPreference.getValue()).isEqualTo(
                Integer.toString(SoftApConfiguration.BAND_2GHZ));
    }

    @Test
    public void onPreferenceChangedTo5Ghz_updatesApBandConfigTo5Ghz() {
        when(mCarWifiManager.is5GhzBandSupported()).thenReturn(true);
        when(mCarWifiManager.getSoftApConfig()).thenReturn(
                new SoftApConfiguration.Builder().setBand(BAND_2GHZ).build());

        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
        mPreferenceController.handlePreferenceChanged(mPreference,
                Integer.toString(SoftApConfiguration.BAND_5GHZ));

        SoftApConfiguration actualConfig = getSoftApConfig();
        assertThat(getBandFromConfig(actualConfig)).isEqualTo(BAND_2GHZ_5GHZ);
        assertThat(actualConfig.getChannels().size()).isEqualTo(1);
    }

    @Test
    public void onPreferenceChangedTo2Ghz_updatesApBandConfigTo2Ghz() {
        when(mCarWifiManager.is5GhzBandSupported()).thenReturn(true);
        when(mCarWifiManager.getSoftApConfig()).thenReturn(
                new SoftApConfiguration.Builder().setBand(BAND_5GHZ).build());

        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
        mPreferenceController.handlePreferenceChanged(mPreference, Integer.toString(BAND_2GHZ));

        SoftApConfiguration actualConfig = getSoftApConfig();
        assertThat(getBandFromConfig(actualConfig)).isEqualTo(BAND_2GHZ);
        assertThat(actualConfig.getChannels().size()).isEqualTo(1);
    }

    @Test
    @EnableFlags(Flags.FLAG_HOTSPOT_UI_SPEED_UPDATE)
    public void onPreferenceChangedToDualBand_updatesApBandConfigToDualBand() {
        when(mCarWifiManager.is5GhzBandSupported()).thenReturn(true);
        when(mCarWifiManager.isDualBandSupported()).thenReturn(true);
        when(mCarWifiManager.getSoftApConfig()).thenReturn(
                new SoftApConfiguration.Builder().setBand(BAND_2GHZ).build());

        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
        mPreferenceController.handlePreferenceChanged(mPreference,
                Integer.toString(BAND_2GHZ_5GHZ));

        SoftApConfiguration actualConfig = getSoftApConfig();
        assertThat(getBandFromConfig(actualConfig)).isEqualTo(BAND_2GHZ_5GHZ);
        assertThat(actualConfig.getChannels().size()).isEqualTo(2);
    }

    private SoftApConfiguration getSoftApConfig() {
        ArgumentCaptor<SoftApConfiguration> captor = ArgumentCaptor.forClass(
                SoftApConfiguration.class);
        verify(mCarWifiManager).setSoftApConfig(captor.capture());
        return captor.getValue();
    }

    private int getBandFromConfig(SoftApConfiguration config) {
        int band = 0;

        SparseIntArray channels = config.getChannels();
        for (int i = 0; i < channels.size(); i++) {
            band |= channels.keyAt(i);
        }

        return band;
    }
}
