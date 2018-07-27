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

package com.android.car.settings.home;

import android.content.Context;
import android.net.wifi.WifiManager;

import androidx.car.widget.TextListItem;

import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;
import com.android.car.settings.common.ListController;
import com.android.car.settings.wifi.CarWifiManager;
import com.android.car.settings.wifi.WifiSettingsFragment;
import com.android.car.settings.wifi.WifiUtil;


/**
 * Represents the wifi list item on settings home page.
 */
public class WifiListItem extends TextListItem {
    private final CarWifiManager mCarWifiManager;
    private final ListController mListController;

    public WifiListItem(
            Context context,
            CarWifiManager carWifiManager,
            BaseFragment.FragmentController fragmentController,
            ListController listController) {
        super(context);
        mListController = listController;
        mCarWifiManager = carWifiManager;
        setTitle(context.getString(R.string.wifi_settings));
        setBody(context.getString(R.string.wifi_settings_summary));
        setSupplementalIcon(R.drawable.ic_chevron_right, /* showDivider= */ false);
        setPrimaryActionIcon(WifiUtil.getIconRes(mCarWifiManager.getWifiState()),
                /* useLargeIcon= */ false);
        setSwitch(mCarWifiManager.isWifiEnabled(), /* showDivider= */ false,
                (button, isChecked) -> mCarWifiManager.setWifiEnabled(!isChecked));
        setOnClickListener(v -> fragmentController
                .launchFragment(WifiSettingsFragment.newInstance()));
    }

    /**
     * Updates this list item to reflect the given Wifi state.
     *
     * @param state one of {@link WifiManager#WIFI_STATE_DISABLED},
     *         {@link WifiManager#WIFI_STATE_ENABLED}, {@link WifiManager#WIFI_STATE_DISABLING},
     *         {@link WifiManager#WIFI_STATE_ENABLING}, {@link WifiManager#WIFI_STATE_UNKNOWN}
     */
    public void onWifiStateChanged(int state) {
        setPrimaryActionIcon(WifiUtil.getIconRes(state), /* useLargeIcon= */ false);
        setSwitchState(WifiUtil.isWifiOn(state));
        mListController.refreshList();
    }

}
