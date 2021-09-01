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

import android.annotation.NonNull;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.TelephonyNetworkSpecifier;
import android.os.UserManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * Base controller for mobile preferences that registers and detects for networks
 *
 *  @param <T> The upper bound on the type of {@link Preference} on which the controller
 *            expects to operate.
 */
public abstract class MobileNetworkBasePreferenceController<T extends Preference> extends
        PreferenceController<T> implements SubscriptionsChangeListener.SubscriptionsChangeAction {

    protected final SubscriptionManager mSubscriptionManager;
    protected TelephonyManager mTelephonyManager;

    private final UserManager mUserManager;
    private final SubscriptionsChangeListener mChangeListener;
    private final ConnectivityManager mConnectivityManager;

    @VisibleForTesting
    List<Integer> mSubIds = new ArrayList<>();

    @VisibleForTesting
    final ConnectivityManager.NetworkCallback mNetworkCallback =
            new ConnectivityManager.NetworkCallback(){
                @Override
                public void onAvailable(@NonNull Network network) {
                    NetworkCapabilities networkCapabilities = mConnectivityManager
                            .getNetworkCapabilities(network);
                    NetworkSpecifier networkSpecifier = networkCapabilities.getNetworkSpecifier();
                    if (networkSpecifier instanceof TelephonyNetworkSpecifier) {
                        int subId = ((TelephonyNetworkSpecifier) networkSpecifier)
                                .getSubscriptionId();

                        if (mSubIds.contains(subId)) {
                            return;
                        }
                        if (mSubIds.isEmpty()) {
                            // The first network is the main one
                            mTelephonyManager = mTelephonyManager.createForSubscriptionId(subId);
                        }
                        mSubIds.add(subId);
                        refreshUi();
                    }
                }
            };

    public MobileNetworkBasePreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mUserManager = UserManager.get(context);
        mChangeListener = new SubscriptionsChangeListener(context, /* action= */ this);
        mConnectivityManager = context.getSystemService(ConnectivityManager.class);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
    }

    @Override
    protected int getAvailabilityStatus() {
        if (!NetworkUtils.hasMobileNetwork(mConnectivityManager)) {
            return UNSUPPORTED_ON_DEVICE;
        }

        boolean isNotAdmin = !mUserManager.isAdminUser();
        boolean hasRestriction =
                mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS);
        if (isNotAdmin || hasRestriction) {
            return DISABLED_FOR_PROFILE;
        }

        return AVAILABLE;
    }

    @Override
    protected void onCreateInternal() {
        mConnectivityManager.registerNetworkCallback(getNetworkRequest(), mNetworkCallback);
    }

    @Override
    protected void onStartInternal() {
        mChangeListener.start();
    }

    @Override
    protected void onStopInternal() {
        mChangeListener.stop();
    }

    @Override
    protected void onDestroyInternal() {
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
    }

    @Override
    public void onSubscriptionsChanged() {
        refreshUi();
    }

    /** Returns network request to listen for */
    protected abstract NetworkRequest getNetworkRequest();
}
