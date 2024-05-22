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

import static com.android.car.datasubscription.DataSubscription.DATA_SUBSCRIPTION_ACTION;

import android.annotation.SuppressLint;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.annotation.CallSuper;
import androidx.annotation.VisibleForTesting;

import com.android.car.datasubscription.DataSubscription;
import com.android.car.settings.R;
import com.android.car.settings.common.ColoredTwoActionSwitchPreference;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;
import com.android.settingslib.utils.StringUtil;

import java.util.List;

/** Controls the preference for accessing mobile network settings. */
public class MobileNetworkEntryPreferenceController extends
        PreferenceController<ColoredTwoActionSwitchPreference> implements
        SubscriptionsChangeListener.SubscriptionsChangeAction,
        DataSubscription.DataSubscriptionChangeListener {
    private final UserManager mUserManager;
    private final SubscriptionsChangeListener mChangeListener;
    private final SubscriptionManager mSubscriptionManager;
    private final ConnectivityManager mConnectivityManager;
    private final TelephonyManager mTelephonyManager;
    private final int mSubscriptionId;
    private final ContentObserver mMobileDataChangeObserver = new ContentObserver(
            new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            refreshUi();
        }
    };
    private DataSubscription mSubscription;

    @SuppressLint("MissingPermission")
    public MobileNetworkEntryPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mUserManager = UserManager.get(context);
        mChangeListener = new SubscriptionsChangeListener(context, /* action= */ this);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mConnectivityManager = context.getSystemService(ConnectivityManager.class);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (isDataSubscriptionFlagEnable()) {
            mSubscription = new DataSubscription(context);
        }
    }

    @Override
    protected Class<ColoredTwoActionSwitchPreference> getPreferenceType() {
        return ColoredTwoActionSwitchPreference.class;
    }

    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();
        getPreference().setOnSecondaryActionClickListener(this::onSecondaryActionClick);
    }

    @Override
    protected void updateState(ColoredTwoActionSwitchPreference preference) {
        List<SubscriptionInfo> subs = SubscriptionUtils.getAvailableSubscriptions(
                mSubscriptionManager, mTelephonyManager);
        preference.setEnabled(getAvailabilityStatus() == AVAILABLE);
        preference.setSummary(getSummary(subs));
        preference.setActionText(getActionText());
        getPreference().setSecondaryActionChecked(mTelephonyManager.isDataEnabled());
    }

    @Override
    protected void onStartInternal() {
        mChangeListener.start();
        if (mSubscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            getContext().getContentResolver().registerContentObserver(getObservableUri(
                    mSubscriptionId), /* notifyForDescendants= */ false, mMobileDataChangeObserver);
        }
        if (mSubscription != null) {
            mSubscription.addDataSubscriptionListener(this);
        }
    }

    @Override
    protected void onStopInternal() {
        mChangeListener.stop();
        if (mSubscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            getContext().getContentResolver().unregisterContentObserver(mMobileDataChangeObserver);
        }
        if (mSubscription != null) {
            mSubscription.removeDataSubscriptionListener();
        }
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        if (!NetworkUtils.hasMobileNetwork(mConnectivityManager)
                && !NetworkUtils.hasSim(mTelephonyManager)) {
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
    protected boolean handlePreferenceClicked(ColoredTwoActionSwitchPreference preference) {
        if (isDataSubscriptionFlagEnable()
                && mSubscription.isDataSubscriptionInactiveOrTrial()) {
            Intent dataSubscriptionIntent = new Intent(DATA_SUBSCRIPTION_ACTION);
            dataSubscriptionIntent.setPackage(getContext().getString(
                    R.string.connectivity_flow_app));
            dataSubscriptionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(dataSubscriptionIntent);
            return true;
        }
        List<SubscriptionInfo> subs = SubscriptionUtils.getAvailableSubscriptions(
                mSubscriptionManager, mTelephonyManager);
        if (subs.isEmpty()) {
            return true;
        } else if (subs.size() == 1) {
            getFragmentController().launchFragment(
                    MobileNetworkFragment.newInstance(subs.get(0).getSubscriptionId()));
        } else {
            getFragmentController().launchFragment(new MobileNetworkListFragment());
        }
        return true;
    }

    @Override
    protected boolean handlePreferenceChanged(ColoredTwoActionSwitchPreference preference,
            Object newValue) {
        List<SubscriptionInfo> subs = SubscriptionUtils.getAvailableSubscriptions(
                mSubscriptionManager, mTelephonyManager);
        preference.setSummary(getSummary(subs));
        preference.setActionText(getActionText());
        return true;
    }

    @Override
    @CallSuper
    public void onSubscriptionsChanged() {
        refreshUi();
    }

    private CharSequence getSummary(List<SubscriptionInfo> subs) {
        if (!mTelephonyManager.isDataEnabled()) {
            return getContext().getString(R.string.mobile_network_state_off);
        }
        if (isDataSubscriptionFlagEnable()
                && mSubscription.isDataSubscriptionInactiveOrTrial()) {
            return getContext().getString(R.string.connectivity_inactive_prompt);
        }
        int count = subs.size();
        if (subs.isEmpty()) {
            return null;
        } else if (count == 1) {
            return subs.get(0).getDisplayName();
        } else {
            return StringUtil.getIcuPluralsString(getContext(), count,
                    R.string.mobile_network_summary_count);
        }
    }

    private CharSequence getActionText() {
        if (!mTelephonyManager.isDataEnabled()) {
            return null;
        }
        if (isDataSubscriptionFlagEnable()
                && mSubscription.isDataSubscriptionInactiveOrTrial()
                && !getUxRestrictions().isRequiresDistractionOptimization()) {
            getPreference().setIsWarning(true);
            return getContext().getString(R.string.connectivity_inactive_action_text);
        }
        return null;
    }

    private Uri getObservableUri(int subId) {
        Uri uri = Settings.Global.getUriFor(Settings.Global.MOBILE_DATA);
        if (mTelephonyManager.getSimCount() != 1) {
            uri = Settings.Global.getUriFor(Settings.Global.MOBILE_DATA + subId);
        }
        return uri;
    }

    @VisibleForTesting
    void onSecondaryActionClick(boolean isChecked) {
        mTelephonyManager.setDataEnabled(isChecked);
        handlePreferenceChanged(getPreference(), isChecked);
    }

    @VisibleForTesting
    void setSubscription(DataSubscription subscription) {
        mSubscription = subscription;
    }

    private boolean isDataSubscriptionFlagEnable() {
        return com.android.car.datasubscription.Flags.dataSubscriptionPopUp();
    }

    @Override
    public void onChange(int value) {
        refreshUi();
    }
}
