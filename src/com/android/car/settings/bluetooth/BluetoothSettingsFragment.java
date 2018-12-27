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

import static android.os.UserManager.DISALLOW_BLUETOOTH;

import android.bluetooth.BluetoothAdapter;
import android.car.userlib.CarUserManagerHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Switch;

import androidx.annotation.LayoutRes;
import androidx.annotation.XmlRes;

import com.android.car.settings.R;
import com.android.car.settings.common.SettingsFragment;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/**
 * Main page for Bluetooth settings. It manages the power switch for the Bluetooth adapter. It also
 * displays paired devices and the entry point for device pairing.
 */
public class BluetoothSettingsFragment extends SettingsFragment {

    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final IntentFilter mIntentFilter = new IntentFilter(
            BluetoothAdapter.ACTION_STATE_CHANGED);
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            handleStateChanged(state);
        }
    };

    private CarUserManagerHelper mCarUserManagerHelper;
    private LocalBluetoothManager mLocalBluetoothManager;
    private Switch mBluetoothSwitch;

    @Override
    @XmlRes
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_settings_fragment;
    }

    @Override
    @LayoutRes
    protected int getActionBarLayoutId() {
        return R.layout.action_bar_with_toggle;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCarUserManagerHelper = new CarUserManagerHelper(context);
        mLocalBluetoothManager = BluetoothUtils.getLocalBtManager(context);
        if (mLocalBluetoothManager == null) {
            goBack();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mBluetoothSwitch = requireActivity().findViewById(R.id.toggle_switch);
        mBluetoothSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    mBluetoothSwitch.setEnabled(false);
                    if (isChecked) {
                        mBluetoothAdapter.enable();
                    } else {
                        mBluetoothAdapter.disable();
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        requireContext().registerReceiver(mReceiver, mIntentFilter);
        mLocalBluetoothManager.setForegroundActivity(requireActivity());
        handleStateChanged(mBluetoothAdapter.getState());
    }

    @Override
    public void onStop() {
        super.onStop();
        requireContext().unregisterReceiver(mReceiver);
        mLocalBluetoothManager.setForegroundActivity(null);
    }

    private boolean isUserRestricted() {
        return mCarUserManagerHelper.isCurrentProcessUserHasRestriction(DISALLOW_BLUETOOTH);
    }

    private void handleStateChanged(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_TURNING_ON:
                mBluetoothSwitch.setChecked(true);
                mBluetoothSwitch.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_ON:
                mBluetoothSwitch.setChecked(true);
                mBluetoothSwitch.setEnabled(!isUserRestricted());
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                mBluetoothSwitch.setChecked(false);
                mBluetoothSwitch.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_OFF:
            default:
                mBluetoothSwitch.setChecked(false);
                mBluetoothSwitch.setEnabled(!isUserRestricted());
        }
    }
}
