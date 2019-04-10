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

import android.annotation.Nullable;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothDevice;
import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.drivingstate.CarUxRestrictions;
import android.car.trust.CarTrustAgentEnrollmentManager;
import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.PreferenceController;
import com.android.internal.widget.LockPatternUtils;

/**
 * Business logic when user click on add trusted device, a new screen will be shown and user can add
 * a trusted device using CarTrustAgentEnrollmentManager, confirm pairing code dialog base on that.
 * TODO(wentingzhai): test the enrollment process when CarTrustAgentEnrollment Service is done.
 */
public class AddTrustedDevicePreferenceController extends PreferenceController<Preference> {

    private static final Logger LOG = new Logger(AddTrustedDevicePreferenceController.class);
    private final Car mCar;
    private final CarUserManagerHelper mCarUserManagerHelper;
    private final LockPatternUtils mLockPatternUtils;
    private BluetoothDevice mBluetoothDevice;
    @Nullable
    private CarTrustAgentEnrollmentManager mCarTrustAgentEnrollmentManager;

    private final CarTrustAgentEnrollmentManager.CarTrustAgentEnrollmentCallback
            mCarTrustAgentEnrollmentCallback =
            new CarTrustAgentEnrollmentManager.CarTrustAgentEnrollmentCallback() {

                @Override
                public void onEnrollmentHandshakeFailure(BluetoothDevice device, int errorCode) {
                    LOG.e("Trust agent service time out");
                }

                @Override
                public void onAuthStringAvailable(BluetoothDevice device, String authString) {
                    ConfirmPairingCodeDialog dialog = ConfirmPairingCodeDialog.newInstance(
                            device.getName(), authString);
                    dialog.setConfirmPairingCodeListener(mConfirmParingCodeListener);
                    getFragmentController().showDialog(dialog, ConfirmPairingCodeDialog.TAG);
                }

                @Override
                public void onEscrowTokenAdded(long handle) {
                    // User need to enter the correct authentication of the car to activate the
                    // added token.
                    getContext().startActivity(new Intent(getContext(), CheckLockActivity.class));
                }

                @Override
                public void onEscrowTokenRemoved(long handle) {
                }

                @Override
                public void onEscrowTokenActiveStateChanged(long handle, boolean active) {
                    if (active) {
                        // TODO(b/124052887) to show the local device name of the device
                        Toast.makeText(getContext(), getContext().getString(
                                R.string.trusted_device_success_enrollment_toast,
                                mBluetoothDevice.getAddress()), Toast.LENGTH_LONG).show();
                    } else {
                        LOG.d(handle + " has been deactivated");
                    }
                    try {
                        mCarTrustAgentEnrollmentManager.stopEnrollmentAdvertising();
                        mCarTrustAgentEnrollmentManager.setBleCallback(null);
                        mCarTrustAgentEnrollmentManager.setEnrollmentCallback(null);
                    } catch (CarNotConnectedException e) {
                        LOG.e(e.getMessage(), e);
                    }
                    getFragmentController().goBack();
                }
            };

    private final CarTrustAgentEnrollmentManager.CarTrustAgentBleCallback
            mCarTrustAgentBleCallback =
            new CarTrustAgentEnrollmentManager.CarTrustAgentBleCallback() {
                @Override
                public void onBleEnrollmentDeviceConnected(BluetoothDevice device) {
                    mBluetoothDevice = device;
                    try {
                        mCarTrustAgentEnrollmentManager.initiateEnrollmentHandshake(
                                mBluetoothDevice);
                    } catch (CarNotConnectedException e) {
                        LOG.e(e.getMessage());
                    }
                }

                @Override
                public void onBleEnrollmentDeviceDisconnected(BluetoothDevice device) {
                    LOG.d("Bluetooth device " + device.getName() + "has been disconnected");
                    mBluetoothDevice = null;
                }

                @Override
                public void onEnrollmentAdvertisingStarted() {
                    LOG.d("Advertising started successfully");
                }

                @Override
                public void onEnrollmentAdvertisingFailed(int errorCode) {
                    getFragmentController().goBack();
                }
            };

    @VisibleForTesting
    final ConfirmPairingCodeDialog.ConfirmPairingCodeListener mConfirmParingCodeListener =
            new ConfirmPairingCodeDialog.ConfirmPairingCodeListener() {
                public void onConfirmPairingCode() {
                    try {
                        mCarTrustAgentEnrollmentManager.enrollmentHandshakeAccepted(
                                mBluetoothDevice);
                    } catch (CarNotConnectedException e) {
                        LOG.e(e.getMessage(), e);
                    }
                }

                public void onDialogCancelled() {
                    getFragmentController().goBack();
                }
            };

    public AddTrustedDevicePreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mCar = Car.createCar(context);
        mCarUserManagerHelper = new CarUserManagerHelper(context);
        mLockPatternUtils = new LockPatternUtils(context);
        try {
            mCarTrustAgentEnrollmentManager = (CarTrustAgentEnrollmentManager) mCar.getCarManager(
                    Car.CAR_TRUST_AGENT_ENROLLMENT_SERVICE);
        } catch (CarNotConnectedException e) {
            LOG.e(e.getMessage(), e);
        }
    }

    @Override
    protected void checkInitialized() {
        if (mCarTrustAgentEnrollmentManager == null) {
            throw new IllegalStateException("mCarTrustAgentEnrollmentManager is null.");
        }
    }

    @Override
    protected void updateState(Preference preference) {
        preference.setEnabled(hasPassword());
    }

    private boolean hasPassword() {
        return mLockPatternUtils.getKeyguardStoredPasswordQuality(
                mCarUserManagerHelper.getCurrentProcessUserId())
                != DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
    }

    @Override
    protected Class<Preference> getPreferenceType() {
        return Preference.class;
    }

    @Override
    public boolean handlePreferenceClicked(Preference preference) {
        try {
            mCarTrustAgentEnrollmentManager.startEnrollmentAdvertising();
        } catch (CarNotConnectedException e) {
            LOG.e(e.getMessage(), e);
        }
        // return false to make sure AddTrustedDeviceProgressFragment will show up.
        return false;
    }

    @Override
    protected void onStartInternal() {
        try {
            mCarTrustAgentEnrollmentManager.setEnrollmentCallback(mCarTrustAgentEnrollmentCallback);
            mCarTrustAgentEnrollmentManager.setBleCallback(mCarTrustAgentBleCallback);
        } catch (CarNotConnectedException e) {
            LOG.e(e.getMessage(), e);
        }
        ConfirmPairingCodeDialog pairingCodeDialog =
                (ConfirmPairingCodeDialog) getFragmentController().findDialogByTag(
                        ConfirmPairingCodeDialog.TAG);
        if (pairingCodeDialog != null) {
            pairingCodeDialog.setConfirmPairingCodeListener(mConfirmParingCodeListener);
        }
    }
}
