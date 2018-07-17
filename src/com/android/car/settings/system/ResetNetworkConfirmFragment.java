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

package com.android.car.settings.system;

import static java.util.Objects.requireNonNull;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.TextListItem;

import com.android.car.settings.R;
import com.android.car.settings.common.ListItemSettingsFragment;

import java.util.Collections;
import java.util.List;

/**
 * Final warning presented to user to confirm restoring network settings to the factory default.
 * If a user confirms, all settings are reset for connectivity, Wi-Fi, and Bluetooth.
 */
public class ResetNetworkConfirmFragment extends ListItemSettingsFragment {

    /**
     * Creates new instance of {@link ResetNetworkConfirmFragment}.
     */
    public static ResetNetworkConfirmFragment newInstance() {
        ResetNetworkConfirmFragment fragment = new ResetNetworkConfirmFragment();
        Bundle bundle = ListItemSettingsFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.reset_network_confirm_title);
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_with_button);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Button resetSettingsButton = requireNonNull(getActivity()).findViewById(
                R.id.action_button1);
        resetSettingsButton.setText(
                getContext().getString(R.string.reset_network_confirm_button_text));
        resetSettingsButton.setOnClickListener(v -> resetNetwork());
    }

    @Override
    public ListItemProvider getItemProvider() {
        return new ListItemProvider.ListProvider(getListItems());
    }

    private List<ListItem> getListItems() {
        Context context = requireContext();
        TextListItem item = new TextListItem(context);
        item.setBody(context.getString(R.string.reset_network_confirm_desc), /* asPrimary= */ true);
        item.setHideDivider(true);
        return Collections.singletonList(item);
    }

    private void resetNetwork() {
        if (ActivityManager.isUserAMonkey()) {
            return;
        }

        Context context = requireNonNull(getActivity()).getApplicationContext();

        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            connectivityManager.factoryReset();
        }

        WifiManager wifiManager = (WifiManager)
                context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            wifiManager.factoryReset();
        }

        BluetoothManager btManager = (BluetoothManager)
                context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (btManager != null) {
            BluetoothAdapter btAdapter = btManager.getAdapter();
            if (btAdapter != null) {
                btAdapter.factoryReset();
            }
        }

        Toast.makeText(requireContext(), R.string.reset_network_complete_toast,
                Toast.LENGTH_SHORT).show();
    }
}
