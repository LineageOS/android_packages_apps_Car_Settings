/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.car.settings.sound;

import static android.car.media.CarAudioManager.AUDIO_FEATURE_DYNAMIC_ROUTING;
import static android.car.media.CarAudioManager.CONFIG_STATUS_AUTO_SWITCHED;
import static android.car.media.CarAudioManager.CONFIG_STATUS_CHANGED;
import static android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP;
import static android.media.AudioManager.DEVICE_OUT_BLE_BROADCAST;

import android.bluetooth.BluetoothProfile;
import android.car.media.AudioZoneConfigurationsChangeCallback;
import android.car.media.CarAudioManager;
import android.car.media.CarAudioZoneConfigInfo;
import android.car.media.CarVolumeGroupInfo;
import android.car.media.SwitchAudioZoneConfigCallback;
import android.content.Context;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;
import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;

import com.android.car.settings.CarSettingsApplication;
import com.android.car.settings.common.Logger;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * // TODO: rename to AudioOutputManager and allow registration of multiple callbacks to
 * CarAudioManager.
 * Manages the audio routes and volumes.
 */
public class AudioRoutesManager {
    private static final Logger LOG = new Logger(AudioRoutesManager.class);
    private final Context mContext;
    private final CarAudioManager mCarAudioManager;
    private final LocalBluetoothManager mBluetoothManager;
    private final int mAudioZone;
    private final int mUsage;
    private String mLastSwitchedAddress;
    private AudioZoneConfigUpdateListener mListener;
    private Map<String, AudioRouteItem> mCachedActiveRoutes;
    private String mCachedActiveDeviceAddress;

    /**
     * A listener for when the AudioZoneConfig is updated.
     */
    public interface AudioZoneConfigUpdateListener {
        /** Call back delegate for AudioZoneConfigurationsChangeCallback */
        void onAudioZoneConfigUpdated(boolean routesChanged);
        /** Call back delegate for SwitchAudioZoneConfigCallback */
        void onAudioZoneSwitch();
        /** Call back for when a switch is requested within this class */
        void onSwitchRequested(String address, boolean success);
    }

    private final AudioZoneConfigurationsChangeCallback mAudioZoneConfigurationsChangeCallback =
            new AudioZoneConfigurationsChangeCallback() {
                @Override
                public void onAudioZoneConfigurationsChanged(
                        @NonNull List<CarAudioZoneConfigInfo> configs, int status) {
                    List<CarAudioZoneConfigInfo> relevantConfigs = configs.stream()
                            .filter(info -> info.getZoneId() == mAudioZone)
                            .toList();
                    if (!relevantConfigs.isEmpty()) {
                        logConfigChange(relevantConfigs, status);
                        if (status == CONFIG_STATUS_CHANGED) {
                            if (!isOutputAddress(mLastSwitchedAddress)) {
                                requestRouteSwitchInternal();
                            }
                        }
                        boolean routesChanged = activeRoutesChanged(
                                mCachedActiveRoutes.keySet().stream().toList(), relevantConfigs);
                        if (status == CONFIG_STATUS_AUTO_SWITCHED || routesChanged) {
                            updateActiveRoutes();
                        }
                        if (mListener != null) {
                            mListener.onAudioZoneConfigUpdated(routesChanged);
                        }
                    }
                }

                private boolean activeRoutesChanged(List<String> currentlyActive,
                        List<CarAudioZoneConfigInfo> changedConfigs) {
                    // changedConfigs is incremental, it does not compare the same set of
                    // active routes so a set comparison is not possible
                    List<String> changedToActive = getUsageMatchedAttributes(changedConfigs,
                            /* isActive= */ true).stream()
                            .map(AudioDeviceAttributes::getAddress)
                            .toList();
                    List<String> changedToInactive = getUsageMatchedAttributes(
                            changedConfigs, /* isActive= */ false).stream()
                            .map(AudioDeviceAttributes::getAddress)
                            .toList();
                    // if active routes became active
                    for (String route : currentlyActive) {
                        if (changedToInactive.contains(route)) {
                            return true;
                        }
                    }
                    // any inactive route became active
                    for (String route : changedToActive) {
                        if (!currentlyActive.contains(route)) {
                            return true;
                        }
                    }
                    return false;
                }

                private void logConfigChange(List<CarAudioZoneConfigInfo> configs, int status) {
                    String statusName = switch (status) {
                        case CONFIG_STATUS_CHANGED -> "CONFIG_STATUS_CHANGED";
                        case CONFIG_STATUS_AUTO_SWITCHED -> "CONFIG_STATUS_AUTO_SWITCHED";
                        default -> "Status: " + status;
                    };
                    LOG.d("onAudioZoneConfigurationsChanged: %s (%s)".formatted(
                            zoneConfigInfosToString(configs),
                            statusName));
                }
            };

    private String zoneConfigInfosToString(List<CarAudioZoneConfigInfo> configs) {
        return String.join(" | ", configs.stream()
                .map(this::zoneConfigInfoToString)
                .collect(Collectors.joining(" | ")));
    }

    private String zoneConfigInfoToString(CarAudioZoneConfigInfo config) {
        List<String> devices = config.getConfigVolumeGroups().stream()
                .filter(volumeGroup -> volumeGroup.getAudioAttributes().stream()
                        .anyMatch(audioAttr -> audioAttr.getUsage() == mUsage))
                .flatMap(
                        volumeGroup -> volumeGroup.getAudioDeviceAttributes().stream())
                .map(AudioDeviceAttributes::getName)
                .collect(Collectors.toList());
        String status = Stream.<String>builder()
                .add(config.isActive() ? "Active" : null)
                .add(config.isSelected() ? "Selected" : null)
                .add(config.isDefault() ? "Default" : null)
                .build()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.joining(","));
        return config.getName() + " [" + status + "] : " + String.join(",", devices);
    }

    private final SwitchAudioZoneConfigCallback mSwitchAudioZoneConfigCallback =
            (zoneConfig, isSuccessful) -> {
                LOG.d("Audio zone switch to [%s] successful: %s"
                        .formatted(zoneConfigInfoToString(zoneConfig), isSuccessful));
                if (isSuccessful) {
                    updateActiveRoutes();
                    if (mListener != null) {
                        mListener.onAudioZoneSwitch();
                    }
                } else {
                    LOG.d("Switch audio zone failed.");
                }
            };

    public AudioRoutesManager(Context context, int usage) {
        mContext = context;
        mCarAudioManager = ((CarSettingsApplication) mContext.getApplicationContext())
                .getCarAudioManager();
        mAudioZone = ((CarSettingsApplication) mContext.getApplicationContext()).getMyAudioZoneId();
        mBluetoothManager = LocalBluetoothManager.getInstance(context, /* onInitCallback= */ null);
        mUsage = usage;
        mCachedActiveRoutes = new ArrayMap<>();
        if (isAudioRoutingEnabled()) {
            mCarAudioManager.clearAudioZoneConfigsCallback();
            mCarAudioManager.setAudioZoneConfigsChangeCallback(mContext.getMainExecutor(),
                    mAudioZoneConfigurationsChangeCallback);
            updateActiveRoutes();
        }
    }

    /** Returns the AudioRouteItem at the given address */
    public AudioRouteItem getRouteItem(String address) {
        updateActiveRoutes();
        return mCachedActiveRoutes.get(address);
    }

    private void updateActiveRoutes() {
        mCachedActiveRoutes.clear();
        // register for all active car audio zones
        List<CarAudioZoneConfigInfo> activeAudioZones = mCarAudioManager
                .getAudioZoneConfigInfos(mAudioZone).stream()
                .toList();
        for (AudioDeviceAttributes attr : getUsageMatchedAttributes(activeAudioZones,
                /* isActive=*/ true)) {
            mCachedActiveRoutes.put(attr.getAddress(), new AudioRouteItem(attr));
        }
        // register for all active connected A2DP device
        List<CachedBluetoothDevice> bluetoothDevices = mBluetoothManager.getCachedDeviceManager()
                .getCachedDevicesCopy().stream()
                .filter(device -> device.isConnectedA2dpDevice()
                        || device.isConnectedLeAudioDevice())
                .toList();
        for (CachedBluetoothDevice device : bluetoothDevices) {
            if (device.isActiveDevice(BluetoothProfile.A2DP)) {
                AudioRouteItem item = mCachedActiveRoutes.computeIfAbsent(device.getAddress(),
                        address -> new AudioRouteItem(device));
                item.setBluetoothDevice(device);
                item.setAudioRouteType(TYPE_BLUETOOTH_A2DP);
            }
        }
        // update active device device address
        AudioDeviceInfo info = mCarAudioManager.getOutputDeviceForUsage(mAudioZone, mUsage);
        mCachedActiveDeviceAddress = info == null ? null : info.getAddress();
    }

    private List<AudioDeviceAttributes> getUsageMatchedAttributes(
            List<CarAudioZoneConfigInfo> audioZoneConfigInfos, boolean isActive) {
        return audioZoneConfigInfos.stream()
                .filter(info -> (info.isActive() == isActive))
                .flatMap(audioZone -> audioZone.getConfigVolumeGroups().stream())
                .filter(volumeGroup -> volumeGroup.getAudioAttributes().stream()
                        .anyMatch(audioAttr -> audioAttr.getUsage() == mUsage))
                .flatMap(volumeGroup -> volumeGroup.getAudioDeviceAttributes().stream())
                .collect(Collectors.toList());
    }

    /**
     * Sets the {@link AudioZoneConfigUpdateListener}.
     */
    public void setListener(AudioZoneConfigUpdateListener listener) {
        mListener = listener;
    }

    /**.
     * @return currently updated audio route list.
     */
    public List<String> getAudioRouteList() {
        return mCachedActiveRoutes.keySet().stream().toList();
    }

    /** True the display name for users at this address, filtering for allowed characters */
    public String getDeviceName(String address) {
        if (mCachedActiveRoutes.containsKey(address)) {
            if (isLeBroadcast(address)) {
                return "ble audio broadcast";
            }
            // remove special character '%' to prevent run time error during string formatting
            return mCachedActiveRoutes.get(address).getName()
                    .replace("%", "");
        }
        return null;
    }

    @VisibleForTesting
    Map<String, AudioRouteItem> getActiveRoutes() {
        return mCachedActiveRoutes;
    }

    @VisibleForTesting
    CarAudioManager getCarAudioManager() {
        return mCarAudioManager;
    }

    @Nullable
    public String getOutputAddress() {
        return mCachedActiveDeviceAddress;
    }

    /** True if the address is current cached output, false otherwise  */
    public boolean isOutputAddress(@Nullable String address) {
        return address != null && address.equals(mCachedActiveDeviceAddress);
    }

    /** True if audio routing feature is enabled, false otherwise  */
    public boolean isAudioRoutingEnabled() {
        if (mCarAudioManager != null
                && mCarAudioManager.isAudioFeatureEnabled(AUDIO_FEATURE_DYNAMIC_ROUTING)) {
            return true;
        }
        return false;
    }

    /** Clears the callback so other classes in CarSettings can register it later */
    public void tearDown() {
        if (mCarAudioManager != null) {
            mCarAudioManager.clearAudioZoneConfigsCallback();
        }
    }

    /** Is the output address a valid BLE broadcast */
    public boolean isOutputLeBroadcast() {
        return isLeBroadcast(mCachedActiveDeviceAddress);
    }

    /** Is this address a valid BLE broadcast */
    public boolean isLeBroadcast(@NonNull String address) {
        AudioRouteItem item = mCachedActiveRoutes.get(address);
        return item != null && item.getAudioDeviceAttributes() != null
                && item.getAudioDeviceAttributes().getInternalType() == DEVICE_OUT_BLE_BROADCAST;
    }

    /**
     * @return the currently connected LE Broadcast address, or {@code null} if it the current audio
     * route list does not contain a valid LE channel.
     */
    @Nullable
    public String getBroadcastAddress() {
        return getAudioRouteList().stream().filter(this::isLeBroadcast).findFirst().orElse(null);
    }

    /**
     * Update to a new audio destination of the provided address.
     */
    public void requestRouteSwitch(String address) {
        AudioRouteItem audioRouteItem = mCachedActiveRoutes.get(address);
        if (audioRouteItem == null) {
            return;
        }
        LOG.d("Updating the current targeted address as %s".formatted(getDeviceName(address)));
        mLastSwitchedAddress = address;
        if (audioRouteItem.getAudioRouteType() == TYPE_BLUETOOTH_A2DP) {
            CachedBluetoothDevice bluetoothDevice = audioRouteItem.getBluetoothDevice();
            if (bluetoothDevice.isActiveDevice(BluetoothProfile.A2DP)) {
                requestRouteSwitchInternal();
            } else {
                // set bluetooth device as active and wait for its audio zone to become active
                bluetoothDevice.setActive();
            }
        } else {
            requestRouteSwitchInternal();
        }
    }

    private void requestRouteSwitchInternal() {
        if (mLastSwitchedAddress == null) {
            LOG.d("Failed to switch audio routing: mLastSwitchedAddress is null");
            mListener.onSwitchRequested(mLastSwitchedAddress, /* success= */ false);
            return;
        }
        AudioRouteItem switchedRoute = mCachedActiveRoutes.get(mLastSwitchedAddress);
        if (switchedRoute == null) {
            LOG.d("Cannot find an AudioRouteItem for " + mLastSwitchedAddress);
            mListener.onSwitchRequested(mLastSwitchedAddress, /* success= */ false);
            return;
        }

        LOG.d("Trying to switch audio route to " + switchedRoute.getAddress());

        List<CarAudioZoneConfigInfo> configs =
                mCarAudioManager.getAudioZoneConfigInfos(mAudioZone);
        LOG.d(zoneConfigInfosToString(configs));

        for (CarAudioZoneConfigInfo carAudioZoneConfigInfo : configs) {
            for (CarVolumeGroupInfo carVolumeGroupInfo :
                    carAudioZoneConfigInfo.getConfigVolumeGroups()) {
                boolean hasCorrectUsage = carVolumeGroupInfo.getAudioAttributes().stream().anyMatch(
                        audioAttribute -> audioAttribute.getUsage() == mUsage);
                boolean hasCorrectAddress =
                        carVolumeGroupInfo.getAudioDeviceAttributes().stream().anyMatch(
                                deviceAttribute -> switchedRoute.getAddress().equals(
                                        deviceAttribute.getAddress()));

                if (hasCorrectUsage && hasCorrectAddress && carAudioZoneConfigInfo.isActive()) {
                    try {
                        if (mListener != null) {
                            mListener.onSwitchRequested(mLastSwitchedAddress, /* success= */ true);
                        }
                        LOG.d("Found audio route to " + mLastSwitchedAddress);
                        mCarAudioManager.switchAudioZoneToConfig(carAudioZoneConfigInfo,
                                ContextCompat.getMainExecutor(mContext),
                                mSwitchAudioZoneConfigCallback);
                    } catch (IllegalStateException e) {
                        LOG.e("IllegalStateException occurred during audio zone switching: " + e);
                        continue;
                    }
                    return;
                }
            }
        }
        if (mListener != null) {
            LOG.d("Failed to switch audio routing to " + mLastSwitchedAddress);
            mListener.onSwitchRequested(mLastSwitchedAddress, /* success= */ false);
        }
    }
}
