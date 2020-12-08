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

package com.android.car.settings.wifi.details;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;

import com.android.car.settings.R;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.SettingsFragment;
import com.android.settingslib.wifi.AccessPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows details about a wifi network, including actions related to the network,
 * e.g. ignore, disconnect, etc. The intent should include information about
 * access point, use that to render UI, e.g. show SSID etc.
 */
public class WifiDetailsFragment extends SettingsFragment {
    private static final String EXTRA_AP_STATE = "extra_ap_state";
    private static final Logger LOG = new Logger(WifiDetailsFragment.class);

    private WifiManager mWifiManager;
    private AccessPoint mAccessPoint;
    private List<WifiDetailsBasePreferenceController> mControllers = new ArrayList<>();

    private WifiInfoProvider mWifiInfoProvider;

    /**
     * Gets an instance of this class.
     */
    public static WifiDetailsFragment getInstance(AccessPoint accessPoint) {
        WifiDetailsFragment wifiDetailsFragment = new WifiDetailsFragment();
        Bundle bundle = new Bundle();
        Bundle accessPointState = new Bundle();
        accessPoint.saveWifiState(accessPointState);
        bundle.putBundle(EXTRA_AP_STATE, accessPointState);
        wifiDetailsFragment.setArguments(bundle);
        return wifiDetailsFragment;
    }

    @Override
    @XmlRes
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_detail_fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mAccessPoint = new AccessPoint(getContext(), getArguments().getBundle(EXTRA_AP_STATE));
        mWifiManager = context.getSystemService(WifiManager.class);
        LOG.d("Creating WifiInfoProvider for " + mAccessPoint);
        if (mWifiInfoProvider == null) {
            mWifiInfoProvider = new WifiInfoProvider(getContext(), mAccessPoint);
        }
        getLifecycle().addObserver(mWifiInfoProvider);

        LOG.d("Creating WifiInfoProvider.Listeners.");
        mControllers.add(use(
                WifiDetailsHeaderPreferenceController.class, R.string.pk_wifi_details_header)
                .init(mAccessPoint, mWifiInfoProvider));
        mControllers.add(use(
                WifiDetailsActionButtonsPreferenceController.class,
                R.string.pk_wifi_details_action_buttons)
                .init(mAccessPoint, mWifiInfoProvider));
        mControllers.add(use(
                WifiSignalStrengthPreferenceController.class, R.string.pk_wifi_signal_strength)
                .init(mAccessPoint, mWifiInfoProvider));
        mControllers.add(use(WifiFrequencyPreferenceController.class, R.string.pk_wifi_frequency)
                .init(mAccessPoint, mWifiInfoProvider));
        mControllers.add(use(WifiSecurityPreferenceController.class, R.string.pk_wifi_security)
                .init(mAccessPoint, mWifiInfoProvider));
        mControllers.add(use(WifiMacAddressPreferenceController.class, R.string.pk_wifi_mac_address)
                .init(mAccessPoint, mWifiInfoProvider));
        mControllers.add(use(WifiIpAddressPreferenceController.class, R.string.pk_wifi_ip).init(
                mAccessPoint, mWifiInfoProvider));
        mControllers.add(use(WifiGatewayPreferenceController.class, R.string.pk_wifi_gateway).init(
                mAccessPoint, mWifiInfoProvider));
        mControllers.add(use(WifiSubnetPreferenceController.class, R.string.pk_wifi_subnet_mask)
                .init(mAccessPoint, mWifiInfoProvider));
        mControllers.add(use(WifiDnsPreferenceController.class, R.string.pk_wifi_dns).init(
                mAccessPoint, mWifiInfoProvider));
        mControllers.add(use(WifiLinkSpeedPreferenceController.class, R.string.pk_wifi_link_speed)
                .init(mAccessPoint, mWifiInfoProvider));
        mControllers.add(use(WifiIpv6AddressPreferenceController.class, R.string.pk_wifi_ipv6).init(
                mAccessPoint, mWifiInfoProvider));
        LOG.d("Done init.");
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        enableRotaryScroll();
    }

    public void onDetach() {
        super.onDetach();
        getLifecycle().removeObserver(mWifiInfoProvider);
    }
}
