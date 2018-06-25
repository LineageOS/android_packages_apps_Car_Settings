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
package com.android.car.settings.wifi;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemProvider;

import com.android.car.settings.R;
import com.android.car.settings.common.EditTextListItem;
import com.android.car.settings.common.ListItemSettingsFragment;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.MutableListItemProvider;
import com.android.car.settings.common.PasswordListItem;
import com.android.car.settings.common.SpinnerListItem;
import com.android.settingslib.wifi.AccessPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Adds a wifi network, the network can be public or private. If ADD_NETWORK_MODE is not specified
 * in the intent, then it needs to contain AccessPoint information, which is be use that to
 * render UI, e.g. show SSID etc.
 */
public class AddWifiFragment extends ListItemSettingsFragment implements
        AdapterView.OnItemSelectedListener {
    public static final String EXTRA_AP_STATE = "extra_ap_state";

    private static final Logger LOG = new Logger(AddWifiFragment.class);
    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9A-F]+$");
    private static final int INVALID_NET_ID = -1;
    @Nullable
    private AccessPoint mAccessPoint;
    @Nullable
    private SpinnerListItem<AccessPointSecurity> mSpinnerLineItem;
    private WifiManager mWifiManager;
    private Button mAddWifiButton;
    private final WifiManager.ActionListener mConnectionListener =
            new WifiManager.ActionListener() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onFailure(int reason) {
                    Toast.makeText(getContext(),
                            R.string.wifi_failed_connect_message,
                            Toast.LENGTH_SHORT).show();
                }
            };
    private EditTextListItem mWifiNameInput;
    private EditTextListItem mWifiPasswordInput;
    private MutableListItemProvider mItemProvider;

    private int mSelectedPosition = AccessPointSecurity.SECURITY_NONE_POSITION;

    public static AddWifiFragment getInstance(AccessPoint accessPoint) {
        AddWifiFragment addWifiFragment = new AddWifiFragment();
        Bundle bundle = ListItemSettingsFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.wifi_setup_add_network);
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_with_button);
        Bundle accessPointState = new Bundle();
        if (accessPoint != null) {
            accessPoint.saveWifiState(accessPointState);
            bundle.putBundle(EXTRA_AP_STATE, accessPointState);
        }
        addWifiFragment.setArguments(bundle);
        return addWifiFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().keySet().contains(EXTRA_AP_STATE)) {
            mAccessPoint = new AccessPoint(getContext(), getArguments().getBundle(EXTRA_AP_STATE));
        }
        mWifiManager = getContext().getSystemService(WifiManager.class);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAddWifiButton = getActivity().findViewById(R.id.action_button1);
        mAddWifiButton.setText(R.string.wifi_setup_connect);
        mAddWifiButton.setOnClickListener(v -> {
            int netId = connectToAccessPoint();
            LOG.d("connected to netId: " + netId);
            if (netId != INVALID_NET_ID) {
                getFragmentController().goBack();
            }
        });
        mAddWifiButton.setEnabled(mAccessPoint != null);
    }

    @Override
    public ListItemProvider getItemProvider() {
        mItemProvider = new MutableListItemProvider(getLineItems());
        return mItemProvider;
    }

    private ArrayList<ListItem> getLineItems() {
        ArrayList<ListItem> lineItems = new ArrayList<>();
        if (mAccessPoint != null) {
            mWifiNameInput = new EditTextListItem(
                    getContext().getString(R.string.wifi_ssid), mAccessPoint.getSsid().toString());
            mWifiNameInput.setTextType(EditTextListItem.TextType.NONE);
        } else {
            mWifiNameInput = new EditTextListItem(
                    getContext().getString(R.string.wifi_ssid));
            mWifiNameInput.setTextType(EditTextListItem.TextType.TEXT);
            mWifiNameInput.setTextChangeListener(s ->
                    mAddWifiButton.setEnabled(!TextUtils.isEmpty(s)));
        }
        lineItems.add(mWifiNameInput);

        if (mAccessPoint == null) {
            List<AccessPointSecurity> securities =
                    AccessPointSecurity.getSecurityTypes(getContext());
            mSpinnerLineItem = new SpinnerListItem<>(
                    getContext(),
                    this,
                    securities,
                    getContext().getText(R.string.wifi_security),
                    mSelectedPosition);
            lineItems.add(mSpinnerLineItem);
        }

        if (mAccessPoint != null
                || mSelectedPosition != AccessPointSecurity.SECURITY_NONE_POSITION) {
            mWifiPasswordInput = new PasswordListItem(
                    getContext().getString(R.string.wifi_password));
            lineItems.add(mWifiPasswordInput);
        }
        return lineItems;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position == mSelectedPosition) {
            return;
        }
        mSelectedPosition = position;
        mItemProvider.setItems(getLineItems());
        refreshList();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    /**
     * Returns netId. -1 if connection fails.
     */
    private int connectToAccessPoint() {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", getSsId());
        wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        int security;
        if (mAccessPoint == null) {
            security = mSpinnerLineItem.getItem(mSelectedPosition).getSecurityType();
            wifiConfig.hiddenSSID = true;
        } else {
            security = mAccessPoint.getSecurity();
        }
        switch (security) {
            case AccessPoint.SECURITY_NONE:
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                wifiConfig.allowedAuthAlgorithms.clear();
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                break;
            case AccessPoint.SECURITY_WEP:
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                String password = mWifiPasswordInput.getInput();
                wifiConfig.wepKeys[0] = isHexString(password) ? password
                        : "\"" + password + "\"";
                wifiConfig.wepTxKeyIndex = 0;
                break;
            case AccessPoint.SECURITY_PSK:
            case AccessPoint.SECURITY_EAP:
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                wifiConfig.preSharedKey = String.format(
                        "\"%s\"", mWifiPasswordInput.getInput());
                break;
            default:
                LOG.w("invalid security type: " + security);
                break;
        }
        int netId = mWifiManager.addNetwork(wifiConfig);
        // this only means wifiManager failed writing the new wifiConfig to db, doesn't mean
        // the network exists/is valid
        if (netId == INVALID_NET_ID) {
            Toast.makeText(getContext(),
                    R.string.wifi_failed_connect_message,
                    Toast.LENGTH_SHORT).show();
        } else {
            mWifiManager.enableNetwork(netId, true);
        }
        return netId;
    }

    private boolean isHexString(String password) {
        return HEX_PATTERN.matcher(password).matches();
    }

    // TODO: handle null case, show warning message etc.
    private String getSsId() {
        if (mAccessPoint == null) {
            return mWifiNameInput.getInput();
        } else {
            return mAccessPoint.getSsid().toString();
        }
    }
}
