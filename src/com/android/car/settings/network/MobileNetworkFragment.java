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
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.XmlRes;

import com.android.car.settings.R;
import com.android.car.settings.common.SettingsFragment;
import com.android.car.settings.datausage.DataUsagePreferenceController;
import com.android.car.settings.datausage.DataUsageSummaryPreferenceController;
import com.android.car.settings.datausage.DataWarningAndLimitPreferenceController;
import com.android.car.settings.search.CarBaseSearchIndexProvider;
import com.android.car.ui.toolbar.ToolbarController;
import com.android.internal.telephony.flags.Flags;
import com.android.internal.util.CollectionUtils;
import com.android.settingslib.search.SearchIndexable;

import com.google.android.collect.Lists;

import java.util.Arrays;
import java.util.List;

/** Mobile network settings homepage. */
@SearchIndexable
public class MobileNetworkFragment extends SettingsFragment implements
        MobileNetworkUpdateManager.MobileNetworkUpdateListener {

    @VisibleForTesting
    static final String ARG_NETWORK_SUB_ID = "network_sub_id";

    private SubscriptionManager mSubscriptionManager;
    private MobileNetworkUpdateManager mMobileNetworkUpdateManager;
    private CharSequence mTitle;

    /**
     * Creates a new instance of the {@link MobileNetworkFragment}, which shows settings related to
     * the given {@code subId}.
     */
    public static MobileNetworkFragment newInstance(int subId) {
        MobileNetworkFragment fragment = new MobileNetworkFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_NETWORK_SUB_ID, subId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @XmlRes
    protected int getPreferenceScreenResId() {
        return R.xml.mobile_network_fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mSubscriptionManager = getSubscriptionManager(context);

        int subId = getArguments() != null
                ? getArguments().getInt(ARG_NETWORK_SUB_ID, MobileNetworkUpdateManager.SUB_ID_NULL)
                : MobileNetworkUpdateManager.SUB_ID_NULL;
        mMobileNetworkUpdateManager = getMobileNetworkUpdateManager(context, subId);
        getLifecycle().addObserver(mMobileNetworkUpdateManager);

        List<MobileNetworkUpdateManager.MobileNetworkUpdateListener> listeners =
                Lists.newArrayList(
                        this,
                        use(MobileDataTogglePreferenceController.class,
                                R.string.pk_mobile_data_toggle),
                        use(RoamingPreferenceController.class, R.string.pk_mobile_roaming_toggle));
        for (MobileNetworkUpdateManager.MobileNetworkUpdateListener listener : listeners) {
            mMobileNetworkUpdateManager.registerListener(listener);
        }

        List<NetworkBasePreferenceController> preferenceControllers =
                Arrays.asList(
                        use(DataUsageSummaryPreferenceController.class,
                                R.string.pk_data_usage_summary),
                        use(MobileDataTogglePreferenceController.class,
                                R.string.pk_mobile_data_toggle),
                        use(RoamingPreferenceController.class, R.string.pk_mobile_roaming_toggle),
                        use(DataUsagePreferenceController.class, R.string.pk_app_data_usage),
                        use(DataWarningAndLimitPreferenceController.class,
                                R.string.pk_data_warning_and_limit));

        for (NetworkBasePreferenceController preferenceController :
                preferenceControllers) {
            preferenceController.setFields(subId);
        }
    }

    @Override
    protected void setupToolbar(@NonNull ToolbarController toolbar) {
        super.setupToolbar(toolbar);

        if (mTitle != null) {
            toolbar.setTitle(mTitle);
        }
    }

    @Override
    public void onMobileNetworkUpdated(int subId) {
        SubscriptionInfo info = null;

        if (subId != MobileNetworkUpdateManager.SUB_ID_NULL) {
            for (SubscriptionInfo subscriptionInfo :
                    mSubscriptionManager.getSelectableSubscriptionInfoList()) {
                if (subscriptionInfo.getSubscriptionId() == subId) {
                    info = subscriptionInfo;
                }
            }
        }

        if (info == null && !CollectionUtils.isEmpty(
                mSubscriptionManager.getActiveSubscriptionInfoList())) {
            info = mSubscriptionManager.getActiveSubscriptionInfoList().get(0);
        }

        if (info != null) {
            // It is possible for this to be called before the activity is fully created. If so,
            // cache the value so that it can be constructed when setupToolbar is called.
            mTitle = info.getDisplayName();
            if (getToolbar() != null) {
                getToolbar().setTitle(mTitle);
            }
        }
    }

    @VisibleForTesting
    SubscriptionManager getSubscriptionManager(Context context) {
        SubscriptionManager sm = context.getSystemService(SubscriptionManager.class);
        if (Flags.workProfileApiSplit()) {
            sm = sm.createForAllUserProfiles();
        }
        return sm;
    }

    @VisibleForTesting
    MobileNetworkUpdateManager getMobileNetworkUpdateManager(Context context, int subId) {
        return new MobileNetworkUpdateManager(context, subId);
    }

    public static final CarBaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new CarBaseSearchIndexProvider(R.xml.mobile_network_fragment,
                    Settings.ACTION_NETWORK_OPERATOR_SETTINGS) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return NetworkUtils.hasMobileNetwork(
                            context.getSystemService(ConnectivityManager.class));
                }
            };
}
