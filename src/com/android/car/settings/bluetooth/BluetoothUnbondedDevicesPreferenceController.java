/*
 * Copyright 2018 The Android Open Source Project
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

import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.car.drivingstate.CarUxRestrictions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.preference.PreferenceGroup;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

/**
 * Displays a list of unbonded (unpaired) Bluetooth devices. This controller also sets the
 * Bluetooth adapter to discovery mode and begins scanning for discoverable devices for as long as
 * the preference group is shown. Clicking on a device will start the pairing process. Discovery
 * and scanning are halted while a device is pairing. Users with the {@link
 * DISALLOW_CONFIG_BLUETOOTH} restriction cannot pair devices.
 */
public class BluetoothUnbondedDevicesPreferenceController extends
        BluetoothDevicesGroupPreferenceController {

    private static final Logger LOG = new Logger(
            BluetoothUnbondedDevicesPreferenceController.class);

    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final AlwaysDiscoverable mAlwaysDiscoverable;
    private boolean mIsScanningEnabled;

    public BluetoothUnbondedDevicesPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mAlwaysDiscoverable = new AlwaysDiscoverable(context, mBluetoothAdapter);
    }

    @Override
    protected BluetoothDeviceFilter.Filter getDeviceFilter() {
        return BluetoothDeviceFilter.UNBONDED_DEVICE_FILTER;
    }

    @Override
    protected void onDeviceClicked(CachedBluetoothDevice cachedDevice) {
        LOG.d("onDeviceClicked: " + cachedDevice);
        disableScanning();
        if (cachedDevice.startPairing()) {
            LOG.d("startPairing");
            // Indicate that this client (vehicle) would like access to contacts (PBAP) and messages
            // (MAP) if there is a server which permits it (usually a phone).
            cachedDevice.getDevice().setPhonebookAccessPermission(BluetoothDevice.ACCESS_ALLOWED);
            cachedDevice.getDevice().setMessageAccessPermission(BluetoothDevice.ACCESS_ALLOWED);
        } else {
            BluetoothUtils.showError(getContext(), cachedDevice.getName(),
                    R.string.bluetooth_pairing_error_message);
            refreshUi();
        }
    }

    @Override
    protected int getAvailabilityStatus() {
        int availabilityStatus = super.getAvailabilityStatus();
        if (availabilityStatus == AVAILABLE
                && getCarUserManagerHelper().isCurrentProcessUserHasRestriction(
                DISALLOW_CONFIG_BLUETOOTH)) {
            return DISABLED_FOR_USER;
        }
        return availabilityStatus;
    }

    @Override
    protected void onStopInternal() {
        super.onStopInternal();
        disableScanning();
        getBluetoothManager().getCachedDeviceManager().clearNonBondedDevices();
        getPreferenceMap().clear();
        getPreference().removeAll();
    }

    @Override
    protected void updateState(PreferenceGroup preferenceGroup) {
        super.updateState(preferenceGroup);
        if (shouldEnableScanning()) {
            enableScanning();
        } else {
            disableScanning();
        }
    }

    private boolean shouldEnableScanning() {
        for (CachedBluetoothDevice device : getPreferenceMap().keySet()) {
            if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
                return false;
            }
        }
        return true;
    }

    /**
     * Starts scanning for devices which will be displayed in the group for a user to select.
     * Calls are idempotent.
     */
    private void enableScanning() {
        mIsScanningEnabled = true;
        if (!mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.startDiscovery();
        }
        mAlwaysDiscoverable.start();
        getPreference().setEnabled(true);
    }

    /** Stops scanning for devices and disables interaction. Calls are idempotent. */
    private void disableScanning() {
        mIsScanningEnabled = false;
        getPreference().setEnabled(false);
        mAlwaysDiscoverable.stop();
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    @Override
    public void onScanningStateChanged(boolean started) {
        LOG.d("onScanningStateChanged started: " + started + " mIsScanningEnabled: "
                + mIsScanningEnabled);
        if (!started && mIsScanningEnabled) {
            enableScanning();
        }
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        LOG.d("onDeviceBondStateChanged device: " + cachedDevice + " state: " + bondState);
        refreshUi();
    }

    /**
     * Helper class to keep the {@link BluetoothAdapter} in discoverable mode indefinitely. By
     * default, setting the scan mode to BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE will
     * timeout, but for pairing, we want to keep the device discoverable as long as the page is
     * scanning.
     */
    private static final class AlwaysDiscoverable extends BroadcastReceiver {

        private final Context mContext;
        private final BluetoothAdapter mAdapter;
        private final IntentFilter mIntentFilter = new IntentFilter(
                BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);

        private boolean mStarted;

        AlwaysDiscoverable(Context context, BluetoothAdapter adapter) {
            mContext = context;
            mAdapter = adapter;
        }

        /**
         * Sets the adapter scan mode to
         * {@link BluetoothAdapter#SCAN_MODE_CONNECTABLE_DISCOVERABLE}. {@link #start()} calls
         * should have a matching calls to {@link #stop()} when discover mode is no longer needed.
         */
        void start() {
            if (mStarted) {
                return;
            }
            mContext.registerReceiver(this, mIntentFilter);
            mStarted = true;
            setDiscoverable();
        }

        void stop() {
            if (!mStarted) {
                return;
            }
            mContext.unregisterReceiver(this);
            mStarted = false;
            mAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            setDiscoverable();
        }

        private void setDiscoverable() {
            if (mAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                mAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
            }
        }
    }
}
