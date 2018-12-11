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

package com.android.car.settings.wifi;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.R;
import com.android.car.settings.common.CarUxRestrictionsHelper;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.wifi.details.WifiDetailFragment;
import com.android.settingslib.wifi.AccessPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a list of {@link AccessPoint} as a list of preference.
 */
public class AccessPointListPreferenceController extends WifiPreferenceControllerBase {
    private static final Logger LOG = new Logger(AccessPointListPreferenceController.class);
    private PreferenceGroup mPreferenceGroup;
    private List<AccessPoint> mAccessPoints = new ArrayList<>();
    private boolean mShowSavedApOnly;

    private final WifiManager.ActionListener mConnectionListener =
            new WifiManager.ActionListener() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onFailure(int reason) {
                    Toast.makeText(mContext,
                            R.string.wifi_failed_connect_message,
                            Toast.LENGTH_SHORT).show();
                }
            };

    public AccessPointListPreferenceController(
            @NonNull Context context,
            String preferenceKey,
            FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
    }

    @Override
    public void updateState(Preference preference) {
        refreshData();
    }

    @Override
    public int getAvailabilityStatus() {
        return WifiUtil.isWifiAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onAccessPointsChanged() {
        refreshData();
    }

    @Override
    public void onWifiStateChanged(int state) {
        // don't care
    }

    @Override
    public void onUxRestrictionsChanged(CarUxRestrictions restrictionInfo) {
        super.onUxRestrictionsChanged(restrictionInfo);
        mShowSavedApOnly = CarUxRestrictionsHelper.isNoSetup(restrictionInfo);
        refreshData();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreferenceGroup = (PreferenceGroup) screen.findPreference(getPreferenceKey());
        updatePreferenceList();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (preference instanceof AccessPointPreference) {
            AccessPoint accessPoint = ((AccessPointPreference) preference).getAccessPoint();
            // for new open unsecuried wifi network, connect to it right away
            if (accessPoint.getSecurity() == AccessPoint.SECURITY_NONE
                    && !accessPoint.isSaved() && !accessPoint.isActive()) {
                getCarWifiManager().connectToPublicWifi(accessPoint, mConnectionListener);
            } else if (accessPoint.isSaved()) {
                getFragmentController().launchFragment(WifiDetailFragment.getInstance(accessPoint));
            } else {
                getFragmentController().launchFragment(AddWifiFragment.getInstance(accessPoint));
            }
        } else {
            getFragmentController().launchFragment(AddWifiFragment.getInstance(null));
        }

        return true;
    }

    @VisibleForTesting
    void refreshData() {
        if (mCarWifiManager == null) {
            return;
        }
        mAccessPoints = mShowSavedApOnly
            ? getCarWifiManager().getSavedAccessPoints()
            : getCarWifiManager().getAllAccessPoints();
        LOG.d("showing accessPoints: " + mAccessPoints.size());
        updatePreferenceList();
    }

    private void updatePreferenceList() {
        mPreferenceGroup.setVisible(!mAccessPoints.isEmpty());
        mPreferenceGroup.removeAll();
        for (AccessPoint accessPoint : mAccessPoints) {
            LOG.d("Adding preference for " + WifiUtil.getKey(accessPoint));
            AccessPointPreference accessPointPreference = new AccessPointPreference(
                    mContext, accessPoint);
            mPreferenceGroup.addPreference(accessPointPreference);
        }
    }
}
