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
import android.car.drivingstate.CarUxRestrictions;
import android.car.trust.CarTrustAgentEnrollmentManager;
import android.car.trust.TrustedDeviceInfo;
import android.content.Context;
import android.os.UserHandle;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.car.settings.R;
import com.android.car.settings.common.ConfirmationDialogFragment;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;
import com.android.internal.widget.LockPatternUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Business logic of trusted device list page
 */
public class TrustedDeviceListPreferenceController extends PreferenceController<PreferenceGroup> {
    @VisibleForTesting
    static final String KEY_HANDLE = "handle";
    private final LockPatternUtils mLockPatternUtils;
    private final Car mCar;
    @Nullable
    private CarTrustAgentEnrollmentManager mCarTrustAgentEnrollmentManager;
    private final CarTrustAgentEnrollmentManager.CarTrustAgentEnrollmentCallback
            mCarTrustAgentEnrollmentCallback =
            new CarTrustAgentEnrollmentManager.CarTrustAgentEnrollmentCallback() {

                @Override
                public void onEnrollmentHandshakeFailure(BluetoothDevice device, int errorCode) {
                }

                @Override
                public void onAuthStringAvailable(BluetoothDevice device, String authString) {
                }

                @Override
                public void onEscrowTokenAdded(long handle) {
                }

                @Override
                public void onEscrowTokenRemoved(long handle) {
                    refreshUi();
                }

                @Override
                public void onEscrowTokenActiveStateChanged(long handle, boolean active) {
                    if (active) {
                        refreshUi();
                    }
                }
            };

    @VisibleForTesting
    final ConfirmationDialogFragment.ConfirmListener mConfirmListener = arguments -> {
        long handle = arguments.getLong(KEY_HANDLE);
        mCarTrustAgentEnrollmentManager.removeEscrowToken(handle,
                UserHandle.myUserId());
    };

    public TrustedDeviceListPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mLockPatternUtils = new LockPatternUtils(context);
        mCar = Car.createCar(context);
        mCarTrustAgentEnrollmentManager = (CarTrustAgentEnrollmentManager) mCar.getCarManager(
                Car.CAR_TRUST_AGENT_ENROLLMENT_SERVICE);

    }

    @Override
    protected void checkInitialized() {
        if (mCarTrustAgentEnrollmentManager == null) {
            throw new IllegalStateException("mCarTrustAgentEnrollmentManager is null.");
        }
    }

    @Override
    protected Class<PreferenceGroup> getPreferenceType() {
        return PreferenceGroup.class;
    }


    @Override
    protected void updateState(PreferenceGroup preferenceGroup) {
        if (!hasPassword()) {
            preferenceGroup.removeAll();
            preferenceGroup.addPreference(createAuthenticationReminderPreference());
            return;
        }
        List<Preference> updatedList = createTrustDevicePreferenceList();
        if (!isEqual(preferenceGroup, updatedList)) {
            preferenceGroup.removeAll();
            for (Preference trustedDevice : updatedList) {
                preferenceGroup.addPreference(trustedDevice);
            }
        }
        preferenceGroup.setVisible(preferenceGroup.getPreferenceCount() > 0);
    }

    private boolean hasPassword() {
        return mLockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId())
                != DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
    }

    @Override
    protected void onCreateInternal() {
        ConfirmationDialogFragment dialog =
                (ConfirmationDialogFragment) getFragmentController().findDialogByTag(
                        ConfirmationDialogFragment.TAG);
        ConfirmationDialogFragment.resetListeners(
                dialog,
                mConfirmListener,
                /* rejectListener= */ null,
                /* neutralListener= */ null);
    }

    @Override
    protected void onStartInternal() {
        mCarTrustAgentEnrollmentManager.setEnrollmentCallback(mCarTrustAgentEnrollmentCallback);

    }

    @Override
    protected void onStopInternal() {
        mCarTrustAgentEnrollmentManager.setEnrollmentCallback(null);
    }

    /**
     * Method to compare two lists of preferences, used only by updateState method.
     *
     * @param preferenceGroup   current preference group
     * @param trustedDeviceList updated preference list
     * @return {@code true} when two lists are the same
     */
    private boolean isEqual(PreferenceGroup preferenceGroup, List<Preference> trustedDeviceList) {
        if (preferenceGroup.getPreferenceCount() != trustedDeviceList.size()) {
            return false;
        }
        for (Preference p : trustedDeviceList) {
            if (preferenceGroup.findPreference(p.getKey()) == null) {
                return false;
            }
        }
        return true;
    }

    private List<Preference> createTrustDevicePreferenceList() {
        List<Preference> trustedDevicesList = new ArrayList<>();
        List<TrustedDeviceInfo> devices =
                mCarTrustAgentEnrollmentManager.getEnrolledDeviceInfoForUser(UserHandle.myUserId());
        for (TrustedDeviceInfo deviceInfo : devices) {
            trustedDevicesList.add(
                    createTrustedDevicePreference(deviceInfo.getName(), deviceInfo.getHandle()));
        }
        return trustedDevicesList;
    }

    private Preference createTrustedDevicePreference(String deviceName, long handle) {
        Preference preference = new Preference(getContext());
        preference.setIcon(R.drawable.ic_settings_bluetooth);
        preference.setTitle(deviceName);
        preference.setKey(String.valueOf(handle));
        preference.setOnPreferenceClickListener((Preference pref) -> {
            getFragmentController().showDialog(
                    getConfirmRemoveDeviceDialogFragment(deviceName, handle),
                    ConfirmationDialogFragment.TAG);
            return true;
        });
        return preference;
    }

    private Preference createAuthenticationReminderPreference() {
        Preference preference = new Preference(getContext());
        preference.setSummary(R.string.trusted_device_set_authentication_reminder);
        return preference;
    }

    private ConfirmationDialogFragment getConfirmRemoveDeviceDialogFragment(
            String deviceName, long handle) {
        return new ConfirmationDialogFragment.Builder(getContext())
                .setTitle(deviceName)
                .setMessage(getContext().getString(
                        R.string.remove_device_message, deviceName, deviceName))
                .setPositiveButton(R.string.trusted_device_remove_button, mConfirmListener)
                .setNegativeButton(
                        R.string.trusted_device_done_button, /* mRejectListener= */ null)
                .addArgumentLong(KEY_HANDLE, handle)
                .build();
    }
}
