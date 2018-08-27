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

package com.android.car.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;
import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.ListItemProvider.ListProvider;
import androidx.car.widget.TextListItem;

import com.android.car.settings.R;
import com.android.car.settings.common.EditTextListItem;
import com.android.car.settings.common.ListItemSettingsFragment;
import com.android.car.settings.common.Logger;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;

import java.util.ArrayList;

/**
 * Shows details about a bluetooth device, including actions related to the device,
 * e.g. forget etc. The intent should include information about the device, use that to
 * render UI, e.g. show name etc.
 */
public class BluetoothDetailFragment extends ListItemSettingsFragment implements
        BluetoothProfileListItem.DataChangedListener {
    private static final Logger LOG = new Logger(BluetoothDetailFragment.class);

    public static final String EXTRA_BT_DEVICE = "extra_bt_device";

    private BluetoothDevice mDevice;
    private CachedBluetoothDevice mCachedDevice;

    private CachedBluetoothDeviceManager mDeviceManager;
    private LocalBluetoothManager mLocalManager;
    private EditTextListItem mInputListItem;
    private Button mOkButton;

    public static BluetoothDetailFragment getInstance(BluetoothDevice btDevice) {
        BluetoothDetailFragment bluetoothDetailFragment = new BluetoothDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_BT_DEVICE, btDevice);
        bluetoothDetailFragment.setArguments(bundle);
        return bluetoothDetailFragment;
    }

    @Override
    @LayoutRes
    protected int getActionBarLayoutId() {
        return R.layout.action_bar_with_button;
    }

    @Override
    @StringRes
    protected int getTitleId() {
        return R.string.bluetooth_settings;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDevice = getArguments().getParcelable(EXTRA_BT_DEVICE);
        mLocalManager =
                LocalBluetoothManager.getInstance(getContext(), /* onInitCallback= */ null);
        if (mLocalManager == null) {
            LOG.e("Bluetooth is not supported on this device");
            return;
        }
        mDeviceManager = mLocalManager.getCachedDeviceManager();
        mCachedDevice = mDeviceManager.findDevice(mDevice);
        if (mCachedDevice == null) {
            mCachedDevice = mDeviceManager.addDevice(mDevice);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (mDevice == null) {
            LOG.w("No bluetooth device set.");
            return;
        }
        super.onActivityCreated(savedInstanceState);

        setupForgetButton();
        setupOkButton();
    }

    @Override
    public void onDataChanged() {
        refreshList();
    }

    @Override
    public ListItemProvider getItemProvider() {
        return new ListProvider(getListItems());
    }

    private ArrayList<ListItem> getListItems() {
        ArrayList<ListItem> listItems = new ArrayList<>();
        mInputListItem = new EditTextListItem(
                getContext().getString(R.string.bluetooth_preference_paired_dialog_name_label),
                mCachedDevice.getName());
        mInputListItem.setTextType(EditTextListItem.TextType.TEXT);
        listItems.add(mInputListItem);
        TextListItem profileHeader = new TextListItem(getContext());
        profileHeader.setTitle(getContext().getString(
                R.string.bluetooth_device_advanced_profile_header_title));
        listItems.add(profileHeader);
        addProfileListItems(listItems);
        return listItems;
    }

    private void addProfileListItems(ArrayList<ListItem> listItems) {
        for (LocalBluetoothProfile profile : mCachedDevice.getConnectableProfiles()) {
            listItems.add(new BluetoothProfileListItem(
                    getContext(), profile, mCachedDevice, this));
        }
    }

    private void setupForgetButton() {
        Button fortgetButton = getActivity().findViewById(R.id.action_button2);
        fortgetButton.setVisibility(View.VISIBLE);
        fortgetButton.setText(R.string.forget);
        fortgetButton.setOnClickListener(v -> {
            mCachedDevice.unpair();
            getFragmentController().goBack();
        });
    }

    private void setupOkButton() {
        mOkButton = getActivity().findViewById(R.id.action_button1);
        mOkButton.setText(android.R.string.ok);
        mOkButton.setOnClickListener(v -> {
            if (!mInputListItem.getInput().equals(mCachedDevice.getName())) {
                mCachedDevice.setName(mInputListItem.getInput());
            }
            getFragmentController().goBack();
        });
    }
}
