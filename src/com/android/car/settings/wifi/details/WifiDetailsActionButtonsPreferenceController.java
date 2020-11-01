/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.car.settings.wifi.details;

import static com.android.car.settings.common.ActionButtonsPreference.ActionButtons;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.android.car.settings.R;
import com.android.car.settings.common.ActionButtonsPreference;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.wifi.WifiUtil;

/**
 * Shows Wifi details action buttons (forget and connect).
 */
public class WifiDetailsActionButtonsPreferenceController
        extends WifiDetailsBasePreferenceController<ActionButtonsPreference> {

    private class ActionFailListener implements WifiManager.ActionListener {
        @StringRes
        private final int mMessageResId;

        ActionFailListener(@StringRes int messageResId) {
            mMessageResId = messageResId;
        }

        @Override
        public void onSuccess() {
        }

        @Override
        public void onFailure(int reason) {
            Toast.makeText(getContext(), mMessageResId, Toast.LENGTH_SHORT).show();
        }
    }

    private final WifiManager mWifiManager;

    public WifiDetailsActionButtonsPreferenceController(Context context,
            String preferenceKey, FragmentController fragmentController,
            CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mWifiManager = context.getSystemService(WifiManager.class);
    }

    @Override
    protected Class<ActionButtonsPreference> getPreferenceType() {
        return ActionButtonsPreference.class;
    }

    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();
        getPreference()
                .getButton(ActionButtons.BUTTON1)
                .setText(R.string.forget)
                .setIcon(R.drawable.ic_delete)
                .setVisible(canForgetNetwork())
                .setOnClickListener(v -> {
                    WifiUtil.forget(getContext(), getAccessPoint());
                    getFragmentController().goBack();
                });
        getPreference()
                .getButton(ActionButtons.BUTTON2)
                .setText(R.string.wifi_setup_connect)
                .setIcon(R.drawable.ic_settings_wifi)
                .setVisible(needConnect())
                .setOnClickListener(v -> {
                    mWifiManager.connect(getAccessPoint().getConfig(),
                            new ActionFailListener(R.string.wifi_failed_connect_message));
                    getFragmentController().goBack();
                });
    }

    @Override
    protected void updateState(ActionButtonsPreference preference) {
        preference.getButton(ActionButtons.BUTTON1).setVisible(canForgetNetwork());
        preference.getButton(ActionButtons.BUTTON2).setVisible(needConnect());
    }

    @Override
    protected int getAvailabilityStatus() {
        if (!WifiUtil.isWifiAvailable(getContext())) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    private boolean needConnect() {
        if (getWifiInfoProvider().getNetwork() == null
                || getWifiInfoProvider().getNetworkInfo() == null
                || getWifiInfoProvider().getWifiInfo() == null) {
            return false;
        }
        return getAccessPoint().isSaved() && !getAccessPoint().isActive();
    }

    private boolean canForgetNetwork() {
        if (getWifiInfoProvider().getNetwork() == null
                || getWifiInfoProvider().getNetworkInfo() == null
                || getWifiInfoProvider().getWifiInfo() == null) {
            return false;
        }
        return (getWifiInfoProvider().getWifiInfo() != null
                && getWifiInfoProvider().getWifiInfo().isEphemeral()) || canModifyNetwork();
    }

    private boolean canModifyNetwork() {
        WifiConfiguration wifiConfig = getWifiInfoProvider().getNetworkConfiguration();
        return wifiConfig != null && !WifiUtil.isNetworkLockedDown(getContext(), wifiConfig);
    }
}
