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
package com.android.car.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.android.car.settings.common.BaseFragment;
import com.android.car.settings.R;
import com.android.car.settings.common.TypedPagedListAdapter;
import com.android.car.view.PagedListView;

import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.android.settingslib.bluetooth.MapProfile;
import com.android.settingslib.bluetooth.PanProfile;
import com.android.settingslib.bluetooth.PbapServerProfile;

import java.util.ArrayList;

/**
 * Shows details about a bluetooth device, including actions related to the device,
 * e.g. forget etc. The intent should include information about the device, use that to
 * render UI, e.g. show name etc.
 */
public class BluetoothDetailFragment extends BaseFragment implements
        BluetoothProfileLineItem.DataChangedListener {
    private static final String TAG = "BluetoothDetailFragment";

    public static final String EXTRA_BT_DEVICE = "extra_bt_device";

    private BluetoothDevice mDevice;
    private CachedBluetoothDevice mCachedDevice;

    private PagedListView mListView;
    private TypedPagedListAdapter mPagedListAdapter;
    private CachedBluetoothDeviceManager mDeviceManager;
    private LocalBluetoothManager mLocalManager;
    private EditText mNameView;
    private TextView mOkButton;

    public static BluetoothDetailFragment getInstance(BluetoothDevice btDevice) {
        BluetoothDetailFragment bluetoothDetailFragment = new BluetoothDetailFragment();
        Bundle bundle = BaseFragment.getBundle();
        bundle.putParcelable(EXTRA_BT_DEVICE, btDevice);
        bundle.putInt(EXTRA_TITLE_ID, R.string.bluetooth_settings);
        bundle.putInt(EXTRA_LAYOUT, R.layout.bluetooth_details);
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_with_button);
        bluetoothDetailFragment.setArguments(bundle);
        return bluetoothDetailFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDevice = getArguments().getParcelable(EXTRA_BT_DEVICE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView = (PagedListView) getView().findViewById(R.id.list);
        mListView.setDarkMode();

        if (mDevice == null) {
            Log.w(TAG, "No bluetooth device set.");
            return;
        }

        mLocalManager = LocalBluetoothManager.getInstance(getContext(), null /* listener */);
        if (mLocalManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            return;
        }
        mDeviceManager = mLocalManager.getCachedDeviceManager();
        mCachedDevice = mDeviceManager.findDevice(mDevice);
        if (mCachedDevice == null) {
            mCachedDevice = mDeviceManager.addDevice(
                    mLocalManager.getBluetoothAdapter(),
                    mLocalManager.getProfileManager(),
                    mDevice);
        }

        mNameView = (EditText) getView().findViewById(R.id.bt_name);
        mNameView.setText(mCachedDevice.getName());
        setupForgetButton();
        setupOkButton();

        mPagedListAdapter = new TypedPagedListAdapter(getContext(), getProfileLineItems());
        mListView.setAdapter(mPagedListAdapter);
    }

    @Override
    public void onDataChanged() {
        mPagedListAdapter.notifyDataSetChanged();
    }

    private ArrayList<TypedPagedListAdapter.LineItem> getProfileLineItems() {
        ArrayList<TypedPagedListAdapter.LineItem> lineItems = new ArrayList<>();
        for (LocalBluetoothProfile profile : mCachedDevice.getConnectableProfiles()) {
            lineItems.add(new BluetoothProfileLineItem(
                    getContext(), profile, mCachedDevice, this));
        }

        int pbapPermission = mCachedDevice.getPhonebookPermissionChoice();
        // Only provide PBAP cabability if the client device has requested PBAP.
        if (pbapPermission != CachedBluetoothDevice.ACCESS_UNKNOWN) {
            PbapServerProfile psp = mLocalManager.getProfileManager().getPbapProfile();
            lineItems.add(new BluetoothProfileLineItem(
                    getContext(), psp, mCachedDevice, this));
        }

        int mapPermission = mCachedDevice.getMessagePermissionChoice();
        if (mapPermission != CachedBluetoothDevice.ACCESS_UNKNOWN) {
            MapProfile mapProfile = mLocalManager.getProfileManager().getMapProfile();
            lineItems.add(new BluetoothProfileLineItem(
                    getContext(), mapProfile, mCachedDevice, this));
        }
        return lineItems;
    }

    private void setupForgetButton() {
        TextView fortgetButton = (TextView) getActivity().findViewById(R.id.action_button2);
        fortgetButton.setVisibility(View.VISIBLE);
        fortgetButton.setText(R.string.forget);
        fortgetButton.setOnClickListener(v -> {
            mCachedDevice.unpair();
            mFragmentController.goBack();
        });
    }

    private void setupOkButton() {
        mOkButton = (TextView) getActivity().findViewById(R.id.action_button1);
        mOkButton.setText(R.string.okay);
        // before the text gets changed, always set it in a disabled state.
        mOkButton.setEnabled(false);
        mNameView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // don't care
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // dont' care
            }

            @Override
            public void afterTextChanged(Editable s) {
                mOkButton.setEnabled(!s.toString().equals(mCachedDevice.getName()));
            }
        });
        mOkButton.setOnClickListener(v -> {
            mCachedDevice.setName(mNameView.getText().toString());
            mFragmentController.goBack();
        });
    }
}
