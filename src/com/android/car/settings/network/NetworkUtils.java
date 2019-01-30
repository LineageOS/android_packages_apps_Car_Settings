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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionPlan;
import android.text.BidiFormatter;
import android.text.format.Formatter;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.internal.util.CollectionUtils;

import java.util.List;

/** Provides helpful utilities surrounding network related tasks. */
public class NetworkUtils {

    @VisibleForTesting
    static final long PETA = 1000000000000000L;

    private NetworkUtils() {
    }

    /** Returns {@code true} if device has a mobile network. */
    public static boolean hasMobileNetwork(ConnectivityManager connectivityManager) {
        Network[] networks = connectivityManager.getAllNetworks();
        for (Network network : networks) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Format byte value to readable string using IEC units.
     */
    public static CharSequence bytesToIecUnits(Context context, long byteValue) {
        final Formatter.BytesResult res = Formatter.formatBytes(context.getResources(), byteValue,
                Formatter.FLAG_IEC_UNITS);
        return BidiFormatter.getInstance().unicodeWrap(context.getString(
                com.android.internal.R.string.fileSizeSuffix, res.value, res.units));
    }

    /**
     * Returns the primary subscription plan. Returns {@code null} if {@link SubscriptionPlan}
     * doesn't exist for a given subscriptionId or if the first {@link SubscriptionPlan} has
     * invalid properties.
     */
    @Nullable
    public static SubscriptionPlan getPrimaryPlan(SubscriptionManager subManager,
            int subscriptionId) {
        List<SubscriptionPlan> plans = subManager.getSubscriptionPlans(subscriptionId);
        if (CollectionUtils.isEmpty(plans)) {
            return null;
        }
        // First plan in the list is the primary plan
        SubscriptionPlan plan = plans.get(0);
        return plan.getDataLimitBytes() > 0
                && saneSize(plan.getDataUsageBytes())
                && plan.getCycleRule() != null ? plan : null;
    }

    private static boolean saneSize(long value) {
        return value >= 0L && value < PETA;
    }
}
