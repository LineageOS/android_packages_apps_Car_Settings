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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.trust.CarTrustAgentEnrollmentManager;
import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
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


/** Unit tests for {@link AddTrustedDevicePreferenceController}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowCar.class})
public class AddTrustedDevicePreferenceControllerTest {
    private static final String ADDRESS = "00:11:22:33:AA:BB";
    private Context mContext;
    private PreferenceControllerTestHelper<AddTrustedDevicePreferenceController>
            mPreferenceControllerHelper;
    @Mock
    private CarTrustAgentEnrollmentManager mMockCarTrustAgentEnrollmentManager;
    private Preference mPreference;
    private AddTrustedDevicePreferenceController mController;
    private BluetoothDevice mBluetoothDevice;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        ShadowCar.setCarManager(Car.CAR_TRUST_AGENT_ENROLLMENT_SERVICE,
                mMockCarTrustAgentEnrollmentManager);
        mPreference = new Preference(mContext);
        mPreferenceControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                AddTrustedDevicePreferenceController.class, mPreference);
        mController = mPreferenceControllerHelper.getController();
        mPreferenceControllerHelper.markState(Lifecycle.State.STARTED);
        mBluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(ADDRESS);
    }

    @After
    public void tearDown() {
        ShadowCar.reset();
    }

    @Test
    public void onPreferenceClicked_startAdvertising() throws CarNotConnectedException {
        mPreference.performClick();
        verify(mMockCarTrustAgentEnrollmentManager).startEnrollmentAdvertising();
    }

    @Test
    public void onAuthStringAvailable_showDialog() throws CarNotConnectedException {
        ArgumentCaptor<CarTrustAgentEnrollmentManager.CarTrustAgentEnrollmentCallback>
                enrollmentCallBack =
                ArgumentCaptor.forClass(
                        CarTrustAgentEnrollmentManager.CarTrustAgentEnrollmentCallback.class);
        verify(mMockCarTrustAgentEnrollmentManager).setEnrollmentCallback(
                enrollmentCallBack.capture());
        ArgumentCaptor<CarTrustAgentEnrollmentManager.CarTrustAgentBleCallback>
                bleCallBack =
                ArgumentCaptor.forClass(
                        CarTrustAgentEnrollmentManager.CarTrustAgentBleCallback.class);
        verify(mMockCarTrustAgentEnrollmentManager).setBleCallback(bleCallBack.capture());

        bleCallBack.getValue().onBleEnrollmentDeviceConnected(mBluetoothDevice);
        enrollmentCallBack.getValue().onAuthStringAvailable(mBluetoothDevice, "123");

        verify(mPreferenceControllerHelper.getMockFragmentController()).showDialog(
                any(ConfirmPairingCodeDialog.class), anyString());
    }

    @Test
    public void onEscrowTokenActiveStateChanged_returnToListFragment()
            throws CarNotConnectedException {
        ArgumentCaptor<CarTrustAgentEnrollmentManager.CarTrustAgentEnrollmentCallback> callBack =
                ArgumentCaptor.forClass(
                        CarTrustAgentEnrollmentManager.CarTrustAgentEnrollmentCallback.class);
        verify(mMockCarTrustAgentEnrollmentManager).setEnrollmentCallback(callBack.capture());

        ArgumentCaptor<CarTrustAgentEnrollmentManager.CarTrustAgentBleCallback>
                bleCallBack = ArgumentCaptor.forClass(
                CarTrustAgentEnrollmentManager.CarTrustAgentBleCallback.class);
        verify(mMockCarTrustAgentEnrollmentManager).setBleCallback(bleCallBack.capture());

        bleCallBack.getValue().onBleEnrollmentDeviceConnected(mBluetoothDevice);
        callBack.getValue().onEscrowTokenActiveStateChanged(123, true);

        verify(mPreferenceControllerHelper.getMockFragmentController()).goBack();
    }

    @Test
    public void onEnrollmentAdvertisingFailed_returnToListFragment()
            throws CarNotConnectedException {
        ArgumentCaptor<CarTrustAgentEnrollmentManager.CarTrustAgentBleCallback> callBack =
                ArgumentCaptor.forClass(
                        CarTrustAgentEnrollmentManager.CarTrustAgentBleCallback.class);
        verify(mMockCarTrustAgentEnrollmentManager).setBleCallback(callBack.capture());

        callBack.getValue().onEnrollmentAdvertisingFailed(12);

        verify(mPreferenceControllerHelper.getMockFragmentController()).goBack();
    }

    @Test
    public void onBluetoothDeviceConnected_initiateHandshake() throws CarNotConnectedException {
        ArgumentCaptor<CarTrustAgentEnrollmentManager.CarTrustAgentBleCallback>
                bleCallBack = ArgumentCaptor.forClass(
                CarTrustAgentEnrollmentManager.CarTrustAgentBleCallback.class);
        verify(mMockCarTrustAgentEnrollmentManager).setBleCallback(bleCallBack.capture());

        bleCallBack.getValue().onBleEnrollmentDeviceConnected(mBluetoothDevice);

        verify(mMockCarTrustAgentEnrollmentManager).initiateEnrollmentHandshake(
                any(BluetoothDevice.class));
    }

    @Test
    public void onPairingCodeDialogConfirmed_initiateHandshake()
            throws CarNotConnectedException {
        mController.mConfirmParingCodeListener.onConfirmPairingCode();
        verify(mMockCarTrustAgentEnrollmentManager).enrollmentHandshakeAccepted();

    }

    @Test
    public void onPairingCodeDialogCancelled_returnToListFragment() {
        mController.mConfirmParingCodeListener.onDialogCancelled();
        verify(mPreferenceControllerHelper.getMockFragmentController()).goBack();
    }
}
