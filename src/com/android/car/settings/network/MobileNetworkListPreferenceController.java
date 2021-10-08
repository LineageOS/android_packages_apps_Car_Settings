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
import android.telephony.SubscriptionManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;

import java.util.List;

/** Business logic to populate the list of available mobile networks. */
public class MobileNetworkListPreferenceController extends
        MobileNetworkBasePreferenceController<PreferenceGroup> {

    private final SubscriptionManager mSubscriptionManager;

    public MobileNetworkListPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);

        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
    }

    @Override
    protected Class<PreferenceGroup> getPreferenceType() {
        return PreferenceGroup.class;
    }

    @Override
    protected void updateState(PreferenceGroup preferenceGroup) {
        preferenceGroup.removeAll();

        List<SubscriptionInfo> totalSubs = SubscriptionUtils
                .getAvailableSubscriptions(mSubscriptionManager, mTelephonyManager);

        for (SubscriptionInfo info : totalSubs) {
            if (mSubIds.contains(info.getSubscriptionId())) {
                preferenceGroup.addPreference(createPreference(info));
            }
        }
    }

    @Override
    protected NetworkRequest getNetworkRequest() {
        return new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();
    }

    private Preference createPreference(SubscriptionInfo info) {
        Preference preference = new Preference(getContext());
        preference.setTitle(info.getDisplayName());
        preference.setKey(Integer.toString(info.getSubscriptionId()));

        boolean isEsim = info.isEmbedded();
        if (mSubscriptionManager.isActiveSubscriptionId(info.getSubscriptionId())) {
            preference.setSummary(isEsim ? R.string.mobile_network_active_esim
                    : R.string.mobile_network_active_sim);
        } else {
            preference.setSummary(isEsim ? R.string.mobile_network_inactive_esim
                    : R.string.mobile_network_inactive_sim);
        }

        preference.setOnPreferenceClickListener(pref -> {
            MobileNetworkFragment fragment = MobileNetworkFragment.newInstance(
                    info.getSubscriptionId());
            getFragmentController().launchFragment(fragment);
            return true;
        });

        return preference;
    }
}
