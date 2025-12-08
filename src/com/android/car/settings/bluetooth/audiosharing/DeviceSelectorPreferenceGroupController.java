/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.car.settings.bluetooth.audiosharing;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.car.settings.CarSettingsApplication;
import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.ui.preference.CarUiTwoActionIconPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for selecting device to join the broadcast.
 */
public class DeviceSelectorPreferenceGroupController extends
        BaseAudioSharingPreferenceController<DeviceSelectorPreferenceGroup> {
    private final List<BluetoothDevice> mPendingDeviceAdds = new ArrayList<>();

    public DeviceSelectorPreferenceGroupController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        if (isBroadcastAvailable()) {
            mLeBroadcastProfile.registerServiceCallBack(getContext().getMainExecutor(),
                    new BaseLeBroadcastCallback() {
                        @Override
                        public void onBroadcastStarted(int reason, int broadcastId) {
                            addPendingDevices(getCurrentBroadcast());
                        }
                        @Override
                        public void onBroadcastMetadataChanged(int broadcastId,
                                @NonNull BluetoothLeBroadcastMetadata metadata) {
                            addPendingDevices(metadata);
                        }
                    });
            mLeBroadcastAssistantProfile.registerServiceCallBack(getContext().getMainExecutor(),
                    new BaseLeBroadcastAssistantCallback() {
                        @Override
                        public void onSourceAdded(@NonNull BluetoothDevice sink, int sourceId,
                                int reason) {
                            updatePreferenceList();
                        }

                        @Override
                        public void onSourceRemoved(@NonNull BluetoothDevice sink, int sourceId,
                                int reason) {
                            updatePreferenceList();
                            if (hasNoActiveSinkDevice(sourceId)) {
                                stopBroadcast();
                            }
                        }
                    });
        }
    }

    private void addPendingDevices(@Nullable BluetoothLeBroadcastMetadata metadata) {
        if (metadata == null || mPendingDeviceAdds.isEmpty()) {
            return;
        }
        for (BluetoothDevice device : mPendingDeviceAdds) {
            mLeBroadcastAssistantProfile.addSource(device, /* metadata= */ getCurrentBroadcast(),
                    /* isGroupOp= */ false);
        }
        mPendingDeviceAdds.clear();
    }

    private boolean hasNoActiveSinkDevice(int sourceId) {
        BluetoothLeBroadcastMetadata metadata = getCurrentBroadcast();
        for (BluetoothDevice device : mBtManager.getBluetoothAdapter().getBondedDevices()) {
            if (device != null && device.isConnected()) {
                BluetoothLeBroadcastMetadata srcMetadata =
                        mLeBroadcastAssistantProfile.getSourceMetadata(device, sourceId);
                if (metadata != null && srcMetadata != null
                        && srcMetadata.getSourceDevice().equals(metadata.getSourceDevice())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void updateState(DeviceSelectorPreferenceGroup preference) {
        super.updateState(preference);
        updatePreferenceList();
    }

    @Override
    protected void onBroadcastStartedInternal(int broadcastId) {
        updatePreferenceList();
    }

    @Override
    protected void onBroadcastStoppedInternal(int broadcastId) {
        updatePreferenceList();
    }

    private void updatePreferenceList() {
        getPreference().removeAll();
        if (!isBroadcastAvailable()) return;
        for (BluetoothDevice device : mBtManager.getBluetoothAdapter().getBondedDevices()) {
            if (device != null && device.isConnected() && mLeAudioProfile.isEnabled(device)) {
                getPreference().addPreference(createLeAudioDevicePreference(device));
            }
        }
    }

    private boolean isReceivingBroadcast(BluetoothDevice device) {
        BluetoothLeBroadcastMetadata metadata = getCurrentBroadcast();
        List<BluetoothLeBroadcastReceiveState> broadcastSources =
                mLeBroadcastAssistantProfile.getAllSources(device);
        if (metadata != null && !broadcastSources.isEmpty()) {
            for (BluetoothLeBroadcastReceiveState state : broadcastSources) {
                if (state.getSourceDevice().equals(metadata.getSourceDevice())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Preference createLeAudioDevicePreference(BluetoothDevice device) {
        CarUiTwoActionIconPreference preference = new CarUiTwoActionIconPreference(getContext());
        preference.setTitle(device.getName());
        preference.setOnPreferenceClickListener(pref -> {
            if (!isBroadcasting()) {
                mPendingDeviceAdds.add(device);
                startBroadcast();
            } else {
                mLeBroadcastAssistantProfile.addSource(device,
                        /* metadata= */ getCurrentBroadcast(), /* isGroupOp= */ false);
            }
            return true;
        });
        preference.setSecondaryActionIcon(R.drawable.ic_remove_circle);
        preference.setOnSecondaryActionClickListener(() -> {
            removeSourceIfDeviceListening(device);
        });

        if (isReceivingBroadcast(device)) {
            preference.setIcon(R.drawable.ic_audio_sharing);
            preference.setSecondaryActionVisible(true);
        } else {
            preference.setIcon(R.drawable.ic_headset);
            preference.setSecondaryActionVisible(false);
        }
        return preference;
    }

    private void removeSourceIfDeviceListening(BluetoothDevice device) {
        BluetoothLeBroadcastMetadata currentSource = getCurrentBroadcast();
        List<BluetoothLeBroadcastReceiveState> deviceSources =
                mLeBroadcastAssistantProfile.getAllSources(device);
        if (currentSource == null || deviceSources.isEmpty()) {
            return;
        }
        for (BluetoothLeBroadcastReceiveState source : deviceSources) {
            if (currentSource.getBroadcastId() == source.getBroadcastId()) {
                mLeBroadcastAssistantProfile.removeSource(device,
                        /* sourceId */ source.getSourceId());
            }
        }
    }

    private void startBroadcast() {
        mLeBroadcastProfile.stopLatestBroadcast();
        mLeBroadcastProfile.startBroadcast(CarSettingsApplication.CAR_SETTINGS_PACKAGE_NAME,
                /* language= */ null);
        // set the broadcast code to byte[0] to start a public broadcast
        mLeBroadcastProfile.setBroadcastCode(new byte[0]);
    }

    private void stopBroadcast() {
        mLeBroadcastProfile.stopLatestBroadcast();
    }

    @Override
    protected Class<DeviceSelectorPreferenceGroup> getPreferenceType() {
        return DeviceSelectorPreferenceGroup.class;
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        if (isUserEnabled() && isBroadcastAvailable()) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }
}
