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

package com.android.car.settings.location;

import static com.android.car.settings.common.PreferenceController.AVAILABLE;
import static com.android.car.settings.common.PreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.testutils.TestLifecycleOwner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class LocationStateListenerBasePreferenceControllerTest {
    private LifecycleOwner mLifecycleOwner;
    private Context mContext = spy(ApplicationProvider.getApplicationContext());
    private Preference mPreference;
    private LocationStateListenerBasePreferenceController mPreferenceController;
    private CarUxRestrictions mCarUxRestrictions;

    @Mock
    private FragmentController mFragmentController;

    @Mock
    private LocationManager mLocationManager;

    @Before
    public void setUp() {
        mLifecycleOwner = new TestLifecycleOwner();
        MockitoAnnotations.initMocks(this);

        when(mContext.getSystemService(LocationManager.class)).thenReturn(mLocationManager);

        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();
        mPreference = new Preference(mContext);
    }

    @Test
    public void onAdasIntentReceived_refreshUi() {
        when(mLocationManager.isAdasGnssLocationEnabled()).thenReturn(false);
        mPreferenceController = new LocationStateListenerBasePreferenceController(mContext,
                /* preferenceKey= */ "key", mFragmentController, mCarUxRestrictions) {
            @Override
            protected Class<Preference> getPreferenceType() {
                return Preference.class;
            }
            @Override
            protected int getDefaultAvailabilityStatus() {
                return getLocationManager().isAdasGnssLocationEnabled()
                        ? AVAILABLE
                        : CONDITIONALLY_UNAVAILABLE;
            }
        };
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.addDefaultBypassLocationStateListener();
        mPreferenceController.onStart(mLifecycleOwner);

        PreferenceControllerTestUtil.assertAvailability(
                mPreferenceController.getAvailabilityStatus(), CONDITIONALLY_UNAVAILABLE);

        ArgumentCaptor<BroadcastReceiver> broadcastReceiverArgumentCaptor = ArgumentCaptor.forClass(
                BroadcastReceiver.class);
        ArgumentCaptor<IntentFilter> intentFilterCaptor = ArgumentCaptor.forClass(
                IntentFilter.class);
        verify(mContext, times(1))
                .registerReceiver(broadcastReceiverArgumentCaptor.capture(),
                        intentFilterCaptor.capture(), eq(Context.RECEIVER_NOT_EXPORTED));

        List<IntentFilter> actions = intentFilterCaptor.getAllValues();
        assertThat(actions.get(0).hasAction(LocationManager.ACTION_ADAS_GNSS_ENABLED_CHANGED))
                .isTrue();

        when(mLocationManager.isAdasGnssLocationEnabled()).thenReturn(true);
        broadcastReceiverArgumentCaptor.getValue().onReceive(mContext,
                new Intent(LocationManager.ACTION_ADAS_GNSS_ENABLED_CHANGED));

        PreferenceControllerTestUtil.assertAvailability(
                mPreferenceController.getAvailabilityStatus(), AVAILABLE);
    }

    @Test
    public void onLocationIntentReceived_refreshUi() {
        when(mLocationManager.isLocationEnabled()).thenReturn(false);
        mPreferenceController = new LocationStateListenerBasePreferenceController(mContext,
                /* preferenceKey= */ "key", mFragmentController, mCarUxRestrictions) {
            @Override
            protected Class<Preference> getPreferenceType() {
                return Preference.class;
            }
            @Override
            protected int getDefaultAvailabilityStatus() {
                return getLocationManager().isLocationEnabled()
                        ? AVAILABLE
                        : CONDITIONALLY_UNAVAILABLE;
            }
        };
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.addDefaultMainLocationStateListener();
        mPreferenceController.onStart(mLifecycleOwner);

        PreferenceControllerTestUtil.assertAvailability(
                mPreferenceController.getAvailabilityStatus(), CONDITIONALLY_UNAVAILABLE);

        ArgumentCaptor<BroadcastReceiver> broadcastReceiverArgumentCaptor = ArgumentCaptor.forClass(
                BroadcastReceiver.class);
        ArgumentCaptor<IntentFilter> intentFilterCaptor = ArgumentCaptor.forClass(
                IntentFilter.class);

        verify(mContext, times(1))
                .registerReceiver(broadcastReceiverArgumentCaptor.capture(),
                        intentFilterCaptor.capture(), eq(Context.RECEIVER_NOT_EXPORTED));

        List<IntentFilter> actions = intentFilterCaptor.getAllValues();
        assertThat(actions.get(0).hasAction(LocationManager.MODE_CHANGED_ACTION)).isTrue();

        when(mLocationManager.isLocationEnabled()).thenReturn(true);
        broadcastReceiverArgumentCaptor.getValue().onReceive(mContext,
                new Intent(LocationManager.MODE_CHANGED_ACTION));

        PreferenceControllerTestUtil.assertAvailability(
                mPreferenceController.getAvailabilityStatus(), AVAILABLE);
    }

    @Test
    public void onPowerPolicyChange_refreshUi() {
        mPreferenceController = new LocationStateListenerBasePreferenceController(mContext,
                /* preferenceKey= */ "key", mFragmentController, mCarUxRestrictions) {
            @Override
            protected Class<Preference> getPreferenceType() {
                return Preference.class;
            }
            @Override
            protected int getDefaultAvailabilityStatus() {
                return getIsPowerPolicyOn()
                        ? AVAILABLE
                        : CONDITIONALLY_UNAVAILABLE;
            }
        };
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.addDefaultPowerPolicyListener();
        mPreferenceController.onStart(mLifecycleOwner);

        PreferenceControllerTestUtil.assertAvailability(
                mPreferenceController.getAvailabilityStatus(), AVAILABLE);

        mPreferenceController.mPowerPolicyListener.getPolicyChangeHandler()
                .handlePolicyChange(/* isOn= */ false);

        PreferenceControllerTestUtil.assertAvailability(
                mPreferenceController.getAvailabilityStatus(), CONDITIONALLY_UNAVAILABLE);
    }
}
