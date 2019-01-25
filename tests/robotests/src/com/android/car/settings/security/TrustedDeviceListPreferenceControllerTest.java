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

package com.android.car.settings.security;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.trust.CarTrustAgentEnrollmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.UserHandle;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.car.settings.testutils.ShadowCar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Unit tests for {@link TrustedDeviceListPreferenceController}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowCar.class})
public class TrustedDeviceListPreferenceControllerTest {
    private Context mContext;
    private PreferenceControllerTestHelper<TrustedDeviceListPreferenceController>
            mPreferenceControllerHelper;
    @Mock
    private CarTrustAgentEnrollmentManager mMockCarTrustAgentEnrollmentManager;
    private PreferenceGroup mPreferenceGroup;
    private SharedPreferences mPrefs;
    private TrustedDeviceListPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        ShadowCar.setCarManager(Car.CAR_TRUST_AGENT_ENROLLMENT_SERVICE,
                mMockCarTrustAgentEnrollmentManager);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor preferencesEditor = mPrefs.edit();
        preferencesEditor.putString("1", "device1");
        preferencesEditor.putString("2", "device2");
        preferencesEditor.putString("3", "device3");
        preferencesEditor.apply();
        mPreferenceGroup = new LogicalPreferenceGroup(mContext);
        mPreferenceControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                TrustedDeviceListPreferenceController.class, mPreferenceGroup);
        mController = mPreferenceControllerHelper.getController();
        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);
    }

    @After
    public void tearDown() {
        ShadowCar.reset();
    }

    @Test
    public void onDeviceRemoved_refreshUi() throws CarNotConnectedException {
        List<Integer> handle = new ArrayList<>(Arrays.asList(1, 2, 3));
        when(mMockCarTrustAgentEnrollmentManager.getEnrollmentHandlesForUser(
                UserHandle.myUserId())).thenReturn(handle);

        mController.refreshUi();

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(3);

        ArgumentCaptor<CarTrustAgentEnrollmentManager.CarTrustAgentEnrollmentCallback> callBack =
                ArgumentCaptor.forClass(
                        CarTrustAgentEnrollmentManager.CarTrustAgentEnrollmentCallback.class);
        verify(mMockCarTrustAgentEnrollmentManager).setEnrollmentCallback(callBack.capture());

        callBack.getValue().onTrustRevoked(handle.get(0), true);

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    public void onDeviceAdded_refreshUi() throws CarNotConnectedException {
        List<Integer> handle = new ArrayList<>(Arrays.asList(1, 2));
        when(mMockCarTrustAgentEnrollmentManager.getEnrollmentHandlesForUser(
                UserHandle.myUserId())).thenReturn(handle);

        mController.refreshUi();

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(2);

        List<Integer> updatedHandle = new ArrayList<>(Arrays.asList(1, 2, 3));
        ArgumentCaptor<CarTrustAgentEnrollmentManager.CarTrustAgentEnrollmentCallback> callBack =
                ArgumentCaptor.forClass(
                        CarTrustAgentEnrollmentManager.CarTrustAgentEnrollmentCallback.class);
        when(mMockCarTrustAgentEnrollmentManager.getEnrollmentHandlesForUser(
                UserHandle.myUserId())).thenReturn(updatedHandle);
        verify(mMockCarTrustAgentEnrollmentManager).setEnrollmentCallback(callBack.capture());

        callBack.getValue().onEscrowTokenActiveStateChanged(updatedHandle.get(0), true);

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(3);
    }

    @Test
    public void refreshUi_noDevices_hidesGroup() throws CarNotConnectedException {
        List<Integer> updatedHandle = new ArrayList<>();
        when(mMockCarTrustAgentEnrollmentManager.getEnrollmentHandlesForUser(
                UserHandle.myUserId())).thenReturn(updatedHandle);

        mController.refreshUi();

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(0);
        assertThat(mPreferenceGroup.isVisible()).isFalse();
    }

    @Test
    public void refreshUi_devices_showsGroup() throws CarNotConnectedException {
        List<Integer> updatedHandle = new ArrayList<>(Arrays.asList(1));
        when(mMockCarTrustAgentEnrollmentManager.getEnrollmentHandlesForUser(
                UserHandle.myUserId())).thenReturn(updatedHandle);

        mController.refreshUi();

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(1);
        assertThat(mPreferenceGroup.isVisible()).isTrue();
    }

    @Test
    public void onPreferenceClicked_showDialog() throws CarNotConnectedException {
        List<Integer> handle = new ArrayList<>(Arrays.asList(1, 2));
        when(mMockCarTrustAgentEnrollmentManager.getEnrollmentHandlesForUser(
                UserHandle.myUserId())).thenReturn(handle);
        mController.refreshUi();
        Preference p = mPreferenceGroup.getPreference(0);

        p.performClick();

        verify(mPreferenceControllerHelper.getMockFragmentController()).showDialog(
                any(ConfirmRemoveDeviceDialog.class), anyString());
    }

    @Test
    public void onRemoveDeviceDialogConfirmed_revokeTrust() throws CarNotConnectedException {
        mController.mConfirmRemoveDeviceListener.onConfirmRemoveDevice(1);

        verify(mMockCarTrustAgentEnrollmentManager).revokeTrust(1);
    }

    @Test
    public void onTrustRevoked_removeHandleFromSharedPreference() throws CarNotConnectedException {
        ArgumentCaptor<CarTrustAgentEnrollmentManager.CarTrustAgentEnrollmentCallback> callBack =
                ArgumentCaptor.forClass(
                        CarTrustAgentEnrollmentManager.CarTrustAgentEnrollmentCallback.class);
        verify(mMockCarTrustAgentEnrollmentManager).setEnrollmentCallback(callBack.capture());

        callBack.getValue().onTrustRevoked(1, true);

        assertThat(mPrefs.getString("1", null)).isNull();
    }
}
