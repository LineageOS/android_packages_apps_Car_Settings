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

package com.android.car.settings.network;

import android.car.drivingstate.CarUxRestrictions;
import android.car.userlib.CarUserManagerHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;

/** Controls the preference for accessing mobile network settings. */
public class MobileNetworkEntryPreferenceController extends PreferenceController<Preference> {

    private final CarUserManagerHelper mCarUserManagerHelper;
    private final TelephonyManager mTelephonyManager;
    private final ConnectivityManager mConnectivityManager;
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            refreshUi();
        }
    };
    private final BroadcastReceiver mAirplaneModeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshUi();
        }
    };
    private final IntentFilter mIntentFilter = new IntentFilter(
            Intent.ACTION_AIRPLANE_MODE_CHANGED);

    public MobileNetworkEntryPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mCarUserManagerHelper = new CarUserManagerHelper(context);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mConnectivityManager = getContext().getSystemService(ConnectivityManager.class);
    }

    @Override
    protected Class<Preference> getPreferenceType() {
        return Preference.class;
    }

    @Override
    protected void onStartInternal() {
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        getContext().registerReceiver(mAirplaneModeChangedReceiver, mIntentFilter);
    }

    @Override
    protected void onStopInternal() {
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        getContext().unregisterReceiver(mAirplaneModeChangedReceiver);
    }

    @Override
    protected int getAvailabilityStatus() {
        if (!hasMobileNetwork()) {
            return UNSUPPORTED_ON_DEVICE;
        }

        boolean isNotAdmin = !mCarUserManagerHelper.getCurrentProcessUserInfo().isAdmin();
        boolean hasRestriction = mCarUserManagerHelper.isCurrentProcessUserHasRestriction(
                UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS);
        if (isNotAdmin || hasRestriction) {
            return DISABLED_FOR_USER;
        }
        return AVAILABLE;
    }

    @Override
    protected void updateState(Preference preference) {
        preference.setSummary(mTelephonyManager.getNetworkOperatorName());
        preference.setEnabled(isAirplaneModeOff());
    }

    private boolean isAirplaneModeOff() {
        return Settings.Global.getInt(getContext().getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) == 0;
    }

    private boolean hasMobileNetwork() {
        Network[] networks = mConnectivityManager.getAllNetworks();
        for (Network network : networks) {
            NetworkCapabilities capabilities = mConnectivityManager.getNetworkCapabilities(network);
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return true;
            }
        }
        return false;
    }
}
