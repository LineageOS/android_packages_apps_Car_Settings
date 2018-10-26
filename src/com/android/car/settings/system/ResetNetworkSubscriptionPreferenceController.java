/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.settings.system;

import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.NoSetupPreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller which determines if a network selection option is visible. On devices with multiple
 * network subscriptions, a user may select the network to reset.
 */
public class ResetNetworkSubscriptionPreferenceController extends
        NoSetupPreferenceController implements Preference.OnPreferenceChangeListener {

    private final SubscriptionManager mSubscriptionManager;
    private ListPreference mListPreference;

    public ResetNetworkSubscriptionPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
        mSubscriptionManager = (SubscriptionManager) context.getSystemService(
                Context.TELEPHONY_SUBSCRIPTION_SERVICE);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mListPreference = (ListPreference) screen.findPreference(getPreferenceKey());

        List<SubscriptionInfo> subscriptions = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptions == null || subscriptions.isEmpty()) {
            // No subscriptions to reset.
            mListPreference.setValue(String.valueOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID));
            mListPreference.setVisible(false);
            return;
        }
        if (subscriptions.size() == 1) {
            // Only one subscription, so nothing else to select. Use it and hide the preference.
            mListPreference.setValue(String.valueOf(subscriptions.get(0).getSubscriptionId()));
            mListPreference.setVisible(false);
            return;
        }

        int defaultSubscriptionId = getDefaultSubscriptionId();
        int selectedIndex = 0;
        int size = subscriptions.size();
        List<String> subscriptionNames = new ArrayList<>(size);
        List<String> subscriptionIds = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            SubscriptionInfo subscription = subscriptions.get(i);
            int subscriptionId = subscription.getSubscriptionId();
            if (subscriptionId == defaultSubscriptionId) {
                // Set the default as the first selected value.
                selectedIndex = i;
            }
            subscriptionNames.add(getSubscriptionName(subscription));
            subscriptionIds.add(String.valueOf(subscriptionId));
        }

        mListPreference.setEntries(toCharSequenceArray(subscriptionNames));
        mListPreference.setEntryValues(toCharSequenceArray(subscriptionIds));
        mListPreference.setTitle(subscriptionNames.get(selectedIndex));
        mListPreference.setValueIndex(selectedIndex);
    }

    /**
     * Returns the default subscription id in the order of data, voice, sms, system subscription.
     */
    private int getDefaultSubscriptionId() {
        int defaultSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (!SubscriptionManager.isUsableSubIdValue(defaultSubscriptionId)) {
            defaultSubscriptionId = SubscriptionManager.getDefaultVoiceSubscriptionId();
        }
        if (!SubscriptionManager.isUsableSubIdValue(defaultSubscriptionId)) {
            defaultSubscriptionId = SubscriptionManager.getDefaultSmsSubscriptionId();
        }
        if (!SubscriptionManager.isUsableSubIdValue(defaultSubscriptionId)) {
            defaultSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
        }
        return defaultSubscriptionId;
    }

    /**
     * Returns the subscription display name falling back to the number, the carrier, and then
     * network id codes.
     */
    private String getSubscriptionName(SubscriptionInfo subscription) {
        String name = subscription.getDisplayName().toString();
        if (TextUtils.isEmpty(name)) {
            name = subscription.getNumber();
        }
        if (TextUtils.isEmpty(name)) {
            name = subscription.getCarrierName().toString();
        }
        if (TextUtils.isEmpty(name)) {
            name = mContext.getString(R.string.reset_network_fallback_subscription_name,
                    subscription.getMcc(), subscription.getMnc(), subscription.getSimSlotIndex(),
                    subscription.getSubscriptionId());
        }
        return name;
    }

    private CharSequence[] toCharSequenceArray(List<String> list) {
        CharSequence[] array = new CharSequence[list.size()];
        list.toArray(array);
        return array;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String subscriptionIdStr = (String) newValue;
        int index = mListPreference.findIndexOfValue(subscriptionIdStr);
        CharSequence subscriptionName = mListPreference.getEntries()[index];
        mListPreference.setTitle(subscriptionName);
        return true;
    }
}
