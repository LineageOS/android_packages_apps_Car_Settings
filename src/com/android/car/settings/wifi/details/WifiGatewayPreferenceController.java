/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context;
import android.net.LinkProperties;
import android.net.Network;
import android.net.RouteInfo;

import com.android.car.settings.common.FragmentController;

/**
 * Shows info about Wifi Gateway info.
 */
public class WifiGatewayPreferenceController extends ActiveWifiDetailPreferenceControllerBase {

    public WifiGatewayPreferenceController(
            Context context, String preferenceKey, FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
    }

    @Override
    public void onLinkPropertiesChanged(Network network, LinkProperties lp) {
        super.onLinkPropertiesChanged(network, lp);
        updateIfAvailable();
    }

    @Override
    protected void updateInfo() {
        String gateway = null;
        for (RouteInfo routeInfo : mWifiInfoProvider.getLinkProperties().getRoutes()) {
            if (routeInfo.isIPv4Default() && routeInfo.hasGateway()) {
                gateway = routeInfo.getGateway().getHostAddress();
                break;
            }
        }

        if (gateway == null) {
            mWifiDetailPreference.setVisible(false);
        } else {
            mWifiDetailPreference.setDetailText(gateway);
            mWifiDetailPreference.setVisible(true);
        }
    }
}
