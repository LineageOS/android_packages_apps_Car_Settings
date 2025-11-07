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

import android.bluetooth.BluetoothCsipSetCoordinator;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.UserHandle;
import android.util.Log;

import com.android.car.settings.Flags;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/**
 * BluetoothPairingRequest is a receiver for any Bluetooth pairing request. It
 * checks if the Bluetooth Settings is currently visible and brings up the PIN, the passkey or a
 * confirmation entry dialog. Otherwise it starts the BluetoothPairingService which
 * starts a notification in the status bar that can be clicked to bring up the same dialog.
 */
public final class BluetoothPairingRequest extends BroadcastReceiver {
    private static final String TAG = "BluetoothPairingRequest";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
            Log.d(TAG, "Receive ACTION_PAIRING_REQUEST");
            if (device == null) {
                return;
            }

            if (Flags.newAudioRoutingUi()) {
                // Skips consent pairing dialog if the device is a member of the coordinated set
                // and is not bonding late.
                int pairingVariant = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT,
                        BluetoothDevice.ERROR);
                LocalBluetoothManager mBluetoothManager = BluetoothUtils.getLocalBtManager(context);
                if (pairingVariant == BluetoothDevice.PAIRING_VARIANT_CONSENT
                        && (mBluetoothManager.getCachedDeviceManager().isOngoingPairByCsip(device)
                        && !mBluetoothManager.getCachedDeviceManager().isLateBonding(device))) {
                    device.setPairingConfirmation(true);
                    return;
                }
            }

            // convert broadcast intent into activity intent (same action string)
            Intent pairingIntent = BluetoothPairingService.getPairingDialogIntent(context, intent);
            PowerManager powerManager = context.getSystemService(PowerManager.class);
            String deviceAddress = device != null ? device.getAddress() : null;
            String deviceName = device != null ? device.getName() : null;
            boolean shouldShowDialog = BluetoothUtils.shouldShowDialogInForeground(
                    context, deviceAddress, deviceName);
            if (powerManager.isInteractive() && shouldShowDialog) {
                // Since the screen is on and the BT-related activity is in the foreground,
                // just open the dialog
                context.startActivityAsUser(pairingIntent, UserHandle.CURRENT);
            } else {
                // Put up a notification that leads to the dialog
                intent.setClass(context, BluetoothPairingService.class);
                context.startServiceAsUser(intent, UserHandle.CURRENT);
            }

        } else if (Flags.newAudioRoutingUi()
                && BluetoothCsipSetCoordinator.ACTION_CSIS_SET_MEMBER_AVAILABLE.equals(action)) {
            Log.d(TAG, "Receive ACTION_CSIS_SET_MEMBER_AVAILABLE");
            if (device == null) {
                return;
            }
            final int groupId = intent.getIntExtra(BluetoothCsipSetCoordinator.EXTRA_CSIS_GROUP_ID,
                    BluetoothCsipSetCoordinator.GROUP_ID_INVALID);
            if (groupId == BluetoothCsipSetCoordinator.GROUP_ID_INVALID) {
                Log.d(TAG, "Invalid group ID");
                return;
            }
            LocalBluetoothManager btManager = BluetoothUtils.getLocalBtManager(context);
            if (btManager != null) {
                btManager.getCachedDeviceManager().pairDeviceByCsip(device, groupId);
            }
        }
    }
}
