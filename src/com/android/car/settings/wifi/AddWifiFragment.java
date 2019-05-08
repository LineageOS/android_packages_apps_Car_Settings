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

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;

import androidx.annotation.LayoutRes;
import androidx.annotation.XmlRes;

import com.android.car.settings.R;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.SettingsFragment;

/**
 * Adds a wifi network, the network can be public or private. If ADD_NETWORK_MODE is not specified
 * in the intent, then it needs to contain AccessPoint information, which is be use that to
 * render UI, e.g. show SSID etc.
 */
public class AddWifiFragment extends SettingsFragment {
    private static final Logger LOG = new Logger(AddWifiFragment.class);

    private WifiManager mWifiManager;
    private Button mAddWifiButton;

    private int mSelectedPosition = AccessPointSecurity.SECURITY_NONE_POSITION;

    @Override
    @XmlRes
    protected int getPreferenceScreenResId() {
        return R.xml.add_wifi_fragment;
    }

    @Override
    @LayoutRes
    protected int getActionBarLayoutId() {
        return R.layout.action_bar_with_button;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mWifiManager = getContext().getSystemService(WifiManager.class);

        use(NetworkNamePreferenceController.class, R.string.pk_add_wifi_network_name)
                .setTextChangeListener(newName -> {
                    if (mAddWifiButton != null) {
                        mAddWifiButton.setEnabled(!TextUtils.isEmpty(newName));
                    }
                });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAddWifiButton = getActivity().findViewById(R.id.action_button1);
        mAddWifiButton.setText(R.string.wifi_setup_connect);
        mAddWifiButton.setEnabled(false);

        // This only needs to handle hidden networks.
        mAddWifiButton.setOnClickListener(v -> {
            // TODO: revisit to remove references to controllers here.
            NetworkNamePreferenceController useController = use(
                    NetworkNamePreferenceController.class, R.string.pk_add_wifi_network_name);
            NetworkSecurityGroupPreferenceController controller = use(
                    NetworkSecurityGroupPreferenceController.class,
                    R.string.pk_add_wifi_security_group);
            int netId = WifiUtil.connectToAccessPoint(getContext(), useController.getNetworkName(),
                    controller.getSelectedSecurityType(),
                    controller.getPasswordText(), /* hidden= */ true);

            LOG.d("connected to netId: " + netId);
            if (netId != WifiUtil.INVALID_NET_ID) {
                goBack();
            }
        });
    }
}
