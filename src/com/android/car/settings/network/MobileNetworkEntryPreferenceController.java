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
import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.telephony.SubscriptionInfo;

import androidx.preference.Preference;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.settingslib.utils.StringUtil;

import java.util.List;

/** Controls the preference for accessing mobile network settings. */
public class MobileNetworkEntryPreferenceController extends
        MobileNetworkBasePreferenceController<Preference> {

    public MobileNetworkEntryPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<Preference> getPreferenceType() {
        return Preference.class;
    }

    @Override
    protected void updateState(Preference preference) {
        List<SubscriptionInfo> totalSubs = SubscriptionUtils.getAvailableSubscriptions(
                mSubscriptionManager, mTelephonyManager);

        if (totalSubs.isEmpty()) {
            // If there are no available networks, disable
            preference.setEnabled(false);
            preference.setSummary(null);
        } else if (mSubIds.isEmpty()) {
            // If the only available networks are oem, hide
            preference.setVisible(false);
        } else {
            // If there are available non-oem networks, show
            preference.setVisible(true);
            preference.setEnabled(true);
            preference.setSummary(getSummary());
        }
    }

    @Override
    protected boolean handlePreferenceClicked(Preference preference) {
        if (mSubIds.isEmpty()) {
            return true;
        }

        if (mSubIds.size() == 1) {
            getFragmentController().launchFragment(
                    MobileNetworkFragment.newInstance(mSubIds.get(0)));
        } else {
            getFragmentController().launchFragment(new MobileNetworkListFragment());
        }
        return true;
    }

    @Override
    protected NetworkRequest getNetworkRequest() {
        return new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();
    }

    private CharSequence getSummary() {
        if (mSubIds.isEmpty()) {
            return null;
        } else if (mSubIds.size() == 1) {
            return mTelephonyManager.getNetworkOperatorName();
        } else {
            return StringUtil.getIcuPluralsString(getContext(), mSubIds.size(),
                    R.string.mobile_network_summary_count);
        }
    }
}
