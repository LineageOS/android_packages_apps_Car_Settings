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

package com.android.car.settings.network;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.telephony.SubscriptionManager;

import androidx.annotation.VisibleForTesting;

import com.android.car.settings.R;
import com.android.car.settings.common.ConfirmationDialogFragment;
import com.android.car.settings.common.FragmentController;
import com.android.car.ui.preference.CarUiSwitchPreference;

/** Controls logic of toggling OEM network */
public class OemNetworkPreferenceController extends
        MobileNetworkBasePreferenceController<CarUiSwitchPreference> {

    @VisibleForTesting
    final ConfirmationDialogFragment.ConfirmListener mConfirmDisableListener =
            arguments -> {
                getPreference().setChecked(false);
                NetworkUtils.setMobileDataEnabled(getContext(), getSubId(), /* enabled= */ false,
                        /* disableOtherSubscriptions= */  false);
            };

    public OemNetworkPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<CarUiSwitchPreference> getPreferenceType() {
        return CarUiSwitchPreference.class;
    }

    @Override
    protected void onStartInternal() {
        super.onStartInternal();
        ConfirmationDialogFragment.resetListeners(
                (ConfirmationDialogFragment) getFragmentController().findDialogByTag(
                        ConfirmationDialogFragment.TAG),
                mConfirmDisableListener,
                /* rejectListener= */ null,
                /* neutralListener= */ null);
    }

    @Override
    protected void updateState(CarUiSwitchPreference preference) {
        if (getSubId() != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            getPreference().setVisible(true);
            getPreference().setSummary(mTelephonyManager.getNetworkOperatorName());
        } else {
            getPreference().setVisible(false);
        }
    }

    @Override
    protected boolean handlePreferenceChanged(CarUiSwitchPreference preference, Object newValue) {
        boolean enabled = (Boolean) newValue;
        if (enabled) {
            NetworkUtils.setMobileDataEnabled(getContext(), getSubId(), /* enabled= */ true,
                    /* disableOtherSubscriptions= */ false);
            return true;
        } else {
            getFragmentController().showDialog(getDisableDialog(), ConfirmationDialogFragment.TAG);
            return false;
        }
    }

    @Override
    protected NetworkRequest getNetworkRequest() {
        return new NetworkRequest.Builder()
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_OEM_PAID)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();
    }

    private int getSubId() {
        if (mSubIds.isEmpty()) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }

        return mSubIds.get(0);
    }

    private ConfirmationDialogFragment getDisableDialog() {
        return new ConfirmationDialogFragment.Builder(getContext())
                .setMessage(R.string.network_and_internet_oem_network_dialog_description)
                .setPositiveButton(R.string.network_and_internet_oem_network_dialog_confirm_label,
                        mConfirmDisableListener)
                .setNegativeButton(R.string.cancel, /* rejectListener= */ null)
                .build();
    }
}
