/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.car.settings.datetime;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.time.Capabilities;
import android.app.time.TimeCapabilities;
import android.app.time.TimeCapabilitiesAndConfig;
import android.app.time.TimeConfiguration;
import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilities;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.car.drivingstate.CarUxRestrictions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.Flags;
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
public class TimeZonePickerPreferenceControllerTest {

    private Context mContext;
    private LifecycleOwner mLifecycleOwner;
    private Preference mPreference;
    private TimeZonePickerPreferenceController mController;

    @Mock
    private FragmentController mFragmentController;
    @Mock
    private TimeManager mTimeManager;
    @Mock
    private TimeCapabilities mTimeCapabilities;
    @Mock
    private TimeCapabilitiesAndConfig mTimeCapabilitiesAndConfig;
    @Mock
    private TimeConfiguration mTimeConfiguration;
    @Mock
    private TimeZoneCapabilities mTimeZoneCapabilities;
    @Mock
    private TimeZoneCapabilitiesAndConfig mTimeZoneCapabilitiesAndConfig;
    @Mock
    private TimeZoneConfiguration mTimeZoneConfiguration;

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Before
    public void setUp() {
        mLifecycleOwner = new TestLifecycleOwner();
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        mPreference = new SwitchPreference(mContext);

        CarUxRestrictions carUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();
        when(mContext.getSystemService(TimeManager.class)).thenReturn(mTimeManager);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(mTimeCapabilitiesAndConfig);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig())
                .thenReturn(mTimeZoneCapabilitiesAndConfig);
        when(mTimeCapabilitiesAndConfig.getCapabilities()).thenReturn(mTimeCapabilities);
        when(mTimeCapabilitiesAndConfig.getConfiguration()).thenReturn(mTimeConfiguration);
        when(mTimeZoneCapabilitiesAndConfig.getCapabilities()).thenReturn(mTimeZoneCapabilities);
        when(mTimeZoneCapabilitiesAndConfig.getConfiguration()).thenReturn(mTimeZoneConfiguration);
        mController = new TimeZonePickerPreferenceController(mContext,
                /* preferenceKey= */ "key", mFragmentController, carUxRestrictions);
        PreferenceControllerTestUtil.assignPreference(mController, mPreference);

        mController.onCreate(mLifecycleOwner);
    }

    @RequiresFlagsDisabled(Flags.FLAG_UPDATE_DATE_AND_TIME_PAGE)
    @Test
    public void testRefreshUi_disabled_automaticTZProviderFlagDisabled() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AUTO_TIME_ZONE, 1);
        mController.refreshUi();
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @RequiresFlagsDisabled(Flags.FLAG_UPDATE_DATE_AND_TIME_PAGE)
    @Test
    public void testRefreshUi_enabled_automaticTZProviderFlagDisabled() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AUTO_TIME_ZONE, 0);
        mController.refreshUi();
        assertThat(mPreference.isEnabled()).isTrue();
    }

    @RequiresFlagsDisabled(Flags.FLAG_UPDATE_DATE_AND_TIME_PAGE)
    @Test
    public void testRefreshUi_fromBroadcastReceiver_disabled_automaticTZProviderFlagDisabled() {
        mController.onStart(mLifecycleOwner);

        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AUTO_TIME_ZONE, 1);
        ArgumentCaptor<BroadcastReceiver> broadcastReceiverArgumentCaptor = ArgumentCaptor.forClass(
                BroadcastReceiver.class);
        verify(mContext).registerReceiver(broadcastReceiverArgumentCaptor.capture(), any(),
                eq(Context.RECEIVER_NOT_EXPORTED));
        broadcastReceiverArgumentCaptor.getValue().onReceive(mContext,
                new Intent(Intent.ACTION_TIME_CHANGED));
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @RequiresFlagsDisabled(Flags.FLAG_UPDATE_DATE_AND_TIME_PAGE)
    @Test
    public void testRefreshUi_fromBroadcastReceiver_enabled_automaticTZProviderFlagDisabled() {
        mController.onStart(mLifecycleOwner);

        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AUTO_TIME_ZONE, 0);
        ArgumentCaptor<BroadcastReceiver> broadcastReceiverArgumentCaptor = ArgumentCaptor.forClass(
                BroadcastReceiver.class);
        verify(mContext).registerReceiver(broadcastReceiverArgumentCaptor.capture(), any(),
                eq(Context.RECEIVER_NOT_EXPORTED));
        broadcastReceiverArgumentCaptor.getValue().onReceive(mContext,
                new Intent(Intent.ACTION_TIME_CHANGED));
        assertThat(mPreference.isEnabled()).isTrue();
    }

    @RequiresFlagsEnabled(Flags.FLAG_UPDATE_DATE_AND_TIME_PAGE)
    @Test
    public void testRefreshUi_disabled() {
        mockIsAutoTimeAndTimeZoneDetectionEnabled(true);
        mController.refreshUi();
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @RequiresFlagsEnabled(Flags.FLAG_UPDATE_DATE_AND_TIME_PAGE)
    @Test
    public void testRefreshUi_enabled() {
        mockIsAutoTimeAndTimeZoneDetectionEnabled(false);
        mController.refreshUi();
        assertThat(mPreference.isEnabled()).isTrue();
    }

    @RequiresFlagsEnabled(Flags.FLAG_UPDATE_DATE_AND_TIME_PAGE)
    @Test
    public void testRefreshUi_fromBroadcastReceiver_disabled() {
        mController.onStart(mLifecycleOwner);

        mockIsAutoTimeAndTimeZoneDetectionEnabled(true);
        ArgumentCaptor<BroadcastReceiver> broadcastReceiverArgumentCaptor = ArgumentCaptor.forClass(
                BroadcastReceiver.class);
        verify(mContext).registerReceiver(broadcastReceiverArgumentCaptor.capture(), any(),
                eq(Context.RECEIVER_NOT_EXPORTED));
        broadcastReceiverArgumentCaptor.getValue().onReceive(mContext,
                new Intent(Intent.ACTION_TIME_CHANGED));
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @RequiresFlagsEnabled(Flags.FLAG_UPDATE_DATE_AND_TIME_PAGE)
    @Test
    public void testRefreshUi_fromBroadcastReceiver_enabled() {
        mController.onStart(mLifecycleOwner);

        mockIsAutoTimeAndTimeZoneDetectionEnabled(false);
        ArgumentCaptor<BroadcastReceiver> broadcastReceiverArgumentCaptor = ArgumentCaptor.forClass(
                BroadcastReceiver.class);
        verify(mContext).registerReceiver(broadcastReceiverArgumentCaptor.capture(), any(),
                eq(Context.RECEIVER_NOT_EXPORTED));
        broadcastReceiverArgumentCaptor.getValue().onReceive(mContext,
                new Intent(Intent.ACTION_TIME_CHANGED));
        assertThat(mPreference.isEnabled()).isTrue();
    }

    private void mockIsAutoTimeAndTimeZoneDetectionEnabled(boolean isEnabled) {
        when(mTimeCapabilities.getConfigureAutoDetectionEnabledCapability())
                .thenReturn(isEnabled ? Capabilities.CAPABILITY_POSSESSED
                        : Capabilities.CAPABILITY_NOT_SUPPORTED);
        when(mTimeZoneCapabilities.getConfigureAutoDetectionEnabledCapability())
                .thenReturn(isEnabled ? Capabilities.CAPABILITY_POSSESSED
                        : Capabilities.CAPABILITY_NOT_SUPPORTED);
        when(mTimeConfiguration.hasIsAutoDetectionEnabled()).thenReturn(isEnabled);
        when(mTimeConfiguration.isAutoDetectionEnabled()).thenReturn(isEnabled);
        when(mTimeZoneConfiguration.hasIsAutoDetectionEnabled()).thenReturn(isEnabled);
        when(mTimeZoneConfiguration.isAutoDetectionEnabled()).thenReturn(isEnabled);
    }
}
