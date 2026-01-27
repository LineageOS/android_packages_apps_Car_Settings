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
import static android.media.AudioDeviceInfo.TYPE_BLE_BROADCAST;
import static android.media.AudioDeviceInfo.TYPE_BLE_HEADSET;

import static com.android.car.settings.bluetooth.audiosharing.BaseAudioSharingPreferenceController.isUserAudioSharingEnabled;
import static com.android.car.settings.sound.AudioRouteItem.Command.CANCEL_STARTING_UNICAST;
import static com.android.car.settings.sound.AudioRouteItem.Command.CHECK_CONDITIONS;
import static com.android.car.settings.sound.AudioRouteItem.Command.JOIN_BROADCAST;
import static com.android.car.settings.sound.AudioRouteItem.Command.LEAVE_BROADCAST;
import static com.android.car.settings.sound.AudioRouteItem.Command.RESET;
import static com.android.car.settings.sound.AudioRouteItem.Command.START_UNICAST;
import static com.android.car.settings.sound.AudioRouteItem.Command.STOP_UNICAST;
import static com.android.car.settings.sound.AudioRouteItem.State.BROADCAST_ACTIVE;
import static com.android.car.settings.sound.AudioRouteItem.State.BROADCAST_READY;
import static com.android.car.settings.sound.AudioRouteItem.State.CREATED;
import static com.android.car.settings.sound.AudioRouteItem.State.JOINING_BROADCAST;
import static com.android.car.settings.sound.AudioRouteItem.State.LEAVING_BROADCAST;
import static com.android.car.settings.sound.AudioRouteItem.State.MULTICAST_ACTIVE;
import static com.android.car.settings.sound.AudioRouteItem.State.MULTICAST_READY_UNICAST_ACTIVE;
import static com.android.car.settings.sound.AudioRouteItem.State.MULTICAST_READY_UNICAST_READY;
import static com.android.car.settings.sound.AudioRouteItem.State.STARTING_BROADCAST;
import static com.android.car.settings.sound.AudioRouteItem.State.STARTING_UNICAST;
import static com.android.car.settings.sound.AudioRouteItem.State.UNICAST_ACTIVE;
import static com.android.car.settings.sound.AudioRouteItem.State.UNICAST_READY;

import static java.util.stream.Collectors.toList;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothVolumeControl;
import android.car.media.AudioZoneConfigurationsChangeCallback;
import android.car.media.CarAudioManager;
import android.car.media.CarAudioZoneConfigInfo;
import android.car.media.CarVolumeGroupInfo;
import android.car.media.SwitchAudioZoneConfigCallback;
import android.content.Context;
import android.media.AudioDeviceAttributes;
import android.util.ArrayMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;

import com.android.car.settings.CarSettingsApplication;
import com.android.car.settings.R;
import com.android.car.settings.bluetooth.audiosharing.BaseLeBroadcastAssistantCallback;
import com.android.car.settings.common.Logger;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LeAudioProfile;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * // TODO: rename to AudioOutputManager and allow registration of multiple callbacks to
 * CarAudioManager.
 * Manages the audio routes and volumes.
 */
public class AudioRoutesManager {
    private static final Logger LOG = new Logger(AudioRoutesManager.class);
    public static final int TIMEOUT_IN_SECS = 5;
    public static final int DEBOUNCE_INTERVAL_IN_MILLIS = 1_000;
    public static final int MIN_LE_AUDIO_VOLUME = 0;
    public static final int MAX_LE_AUDIO_VOLUME = 255;
    private final Context mContext;
    private final ScheduledExecutorService mExecutor;
    private CarAudioManager mCarAudioManager = null;
    private final LocalBluetoothManager mBluetoothManager;
    private LocalBluetoothLeBroadcast mLeBroadcastProfile;
    private LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistantProfile;
    private VolumeControlProfile mVolumeControlProfile;
    private LeAudioProfile mLeAudioProfile;
    private int mAudioZone;
    private final int mUsage;
    private AudioRoutesUpdateListener mAudioRoutesUpdateListener;
    private volatile Map<String, AudioRouteItem> mCachedAudioRoutes;
    private String mCachedActiveDeviceAddress;
    // The following four fields are used to manage the state transitions when requesting
    // audio route changes. mPendingActiveAddress and mPendingSelectAddress track the device
    // addresses for which setActive and switchAudioZoneToConfig, respectively, have been
    // requested. mCallbackDebouncer is used to group multiple audio zone configuration change
    // callbacks, and mTimeoutTimer is used to handle potential timeouts during these operations.
    private String mPendingActiveAddress = null;
    private String mPendingSelectAddress = null;
    private ScheduledFuture<?> mCallbackDebouncer = null;
    private ScheduledFuture<?> mActiveTimeout = null;
    private ScheduledFuture<?> mSelectTimeout = null;
    // Volume is scaled with Car Audio's min / max volumes.
    private final Map<String, Integer> mAudioVolumeCache = new ConcurrentHashMap<>();
    private boolean mUseTimeout = true;

    private record AudioRouteEvent(String address, AudioRouteItem.Command command,
                                   AudioRouteItem.State state) {
        AudioRouteEvent(String address, AudioRouteItem.Command command) {
            this(address, command, AudioRouteItem.State.UNSPECIFIED);
        }

        @Override
        public String toString() {
            return "AudioRouteEvent[address=%s, command=%s, state=%s]".formatted(
                    AudioRouteItem.redact(address), command, state);
        }
    }

    /**
     * A listener for when the AudioRouteItems are updated.
     */
    public interface AudioRoutesUpdateListener {
        /** Called when a state of any AudioRouteItems is changed. */
        void onAudioRoutesUpdated(List<AudioRouteItem> audioRouteItems);
    }

    public AudioRoutesManager(Context context, int usage) {
        this(context, usage, Executors.newSingleThreadScheduledExecutor());
    }

    @VisibleForTesting
    public AudioRoutesManager(Context context, int usage, ScheduledExecutorService executor) {
        mContext = context;
        mExecutor = executor;
        mCarAudioManager =
                ((CarSettingsApplication) mContext.getApplicationContext()).getCarAudioManager();
        mAudioZone = ((CarSettingsApplication) mContext.getApplicationContext()).getMyAudioZoneId();
        mBluetoothManager = LocalBluetoothManager.getInstance(context, /* onInitCallback= */ null);
        if (mBluetoothManager != null) {
            mLeBroadcastProfile =
                    mBluetoothManager.getProfileManager().getLeAudioBroadcastProfile();
            mLeBroadcastAssistantProfile =
                    mBluetoothManager.getProfileManager().getLeAudioBroadcastAssistantProfile();
            mVolumeControlProfile = mBluetoothManager.getProfileManager().getVolumeControlProfile();
            mLeAudioProfile = mBluetoothManager.getProfileManager().getLeAudioProfile();
        }
        mUsage = usage;
        mCachedAudioRoutes = new ArrayMap<>();
        if (isAudioRoutingEnabled()) {
            if (mBluetoothManager != null) {
                mBluetoothManager.getEventManager().registerCallback(mBluetoothCallback);
            }
            if (mLeBroadcastProfile != null && mLeBroadcastAssistantProfile != null) {
                mLeBroadcastAssistantProfile.registerServiceCallBack(mExecutor,
                        mBaseLeBroadcastAssistantCallback);
            }
            if (mVolumeControlProfile != null) {
                mVolumeControlProfile.registerCallback(mExecutor,
                        mVolumeControlCallback);
            }
            mCarAudioManager.clearAudioZoneConfigsCallback();
            mCarAudioManager.setAudioZoneConfigsChangeCallback(mExecutor,
                    mAudioZoneConfigurationsChangeCallback);
            mCarAudioManager.registerCarVolumeCallback(mCarVolumeCallback);
        }
        updateAndNotifyAudioRouteItemsIfChanged();
    }

    /** Sets whether to use timeout for audio route operations. */
    @VisibleForTesting
    void setUseTimeout(boolean useTimeout) {
        mUseTimeout = useTimeout;
    }

    /** Gets the current output address. */
    @Nullable
    public String getOutputAddress() {
        return mCachedActiveDeviceAddress;
    }

    /** True if audio routing feature is enabled, false otherwise. */
    public boolean isAudioRoutingEnabled() {
        if (mCarAudioManager != null && mCarAudioManager.isAudioFeatureEnabled(
                AUDIO_FEATURE_DYNAMIC_ROUTING)) {
            return true;
        }
        return false;
    }

    /** Clears the callback so other classes in CarSettings can register it later. */
    public void tearDown() {
        if (mCarAudioManager != null) {
            mCarAudioManager.clearAudioZoneConfigsCallback();
            mCarAudioManager.unregisterCarVolumeCallback(mCarVolumeCallback);
        }
        if (mVolumeControlProfile != null) {
            mVolumeControlProfile.unregisterCallback(mVolumeControlCallback);
        }
        if (mLeBroadcastAssistantProfile != null) {
            mLeBroadcastAssistantProfile.unregisterServiceCallBack(
                    mBaseLeBroadcastAssistantCallback);
        }
        if (mBluetoothManager != null) {
            mBluetoothManager.getEventManager().unregisterCallback(mBluetoothCallback);
        }
        mExecutor.shutdown();
    }

    @VisibleForTesting
    public Map<String, AudioRouteItem> getActiveRoutes() {
        return mCachedAudioRoutes;
    }

    /** Is this address a valid BLE broadcast. */
    public boolean isLeBroadcast(@NonNull String address) {
        AudioRouteItem item = mCachedAudioRoutes.get(address);
        return item != null && item.getAudioRouteType() == TYPE_BLE_BROADCAST;
    }

    /** Sets the {@link AudioRoutesUpdateListener}. */
    public void setAudioRoutesUpdateListener(AudioRoutesUpdateListener listener) {
        LOG.i("[setAudioRoutesUpdateListener]");
        mAudioRoutesUpdateListener = listener;
        updateAndNotifyAudioRouteItems(/* forceNotify= */ true);
    }

    /** Gets currently updated audio route list. */
    public List<String> getAudioRouteList() {
        return mCachedAudioRoutes.keySet().stream().toList();
    }

    /** True the display name for users at this address, filtering for allowed characters */
    public String getDeviceName(String address) {
        if (mCachedAudioRoutes.containsKey(address)) {
            if (isLeBroadcast(address)) {
                return "ble audio broadcast";
            }
            // remove special character '%' to prevent run time error during string formatting
            return mCachedAudioRoutes.get(address).getName().replace("%", "");
        }
        return null;
    }

    /** Sets a device to be the unicast target. **/
    public void setUnicast(String address) {
        LOG.i("[setUnicast] " + AudioRouteItem.redact(address));
        requestAudioRouteEvent(new AudioRouteEvent(address, START_UNICAST));
    }

    /** Joins a broadcast session for the given address. */
    public void joinBroadcast(String joiningAddress) {
        LOG.i("[joinBroadcast] " + AudioRouteItem.redact(joiningAddress));
        requestAudioRouteEvent(new AudioRouteEvent(joiningAddress, JOIN_BROADCAST));
    }

    /** Leaves a broadcast session for the given address. */
    public void leaveBroadcast(String leavingAddress) {
        LOG.i("[leaveBroadcast] " + AudioRouteItem.redact(leavingAddress));
        requestAudioRouteEvent(new AudioRouteEvent(leavingAddress, LEAVE_BROADCAST));
    }

    public void setVolume(String address, int volume) {
        mExecutor.execute(() -> {
            AudioRouteItem item = mCachedAudioRoutes.get(address);
            if (item == null) {
                LOG.i("[setVolume] AudioRoute not found: " + AudioRouteItem.redact(address));
                return;
            }
            int groupId = mCarAudioManager.getVolumeGroupIdForUsage(mAudioZone, mUsage);

            if (item.getVolumeState().useVolumeControlProfile()) {
                if (mVolumeControlProfile == null) {
                    LOG.i("[setVolume] VolumeControlProfile not available");
                    return;
                }
                int scaledVolume = scaleVolume(volume,
                        mCarAudioManager.getGroupMinVolume(groupId),
                        mCarAudioManager.getGroupMaxVolume(groupId),
                        MIN_LE_AUDIO_VOLUME,
                        MAX_LE_AUDIO_VOLUME);
                mVolumeControlProfile.setDeviceVolume(item.getBluetoothDevice().getDevice(),
                        scaledVolume, /* isGroupOp= */ true);
                LOG.i("[setVolume] setDeviceVolume: %s %s".formatted(item.getAnonymizedAddress(),
                        scaledVolume));
            } else {
                mCarAudioManager.setGroupVolume(mAudioZone, groupId, volume, /* flags= */ 0);
                LOG.i("[setVolume] setGroupVolume: %s %s".formatted(item.getAnonymizedAddress(),
                        volume));
            }
        });
    }

    private final BluetoothVolumeControl.Callback mVolumeControlCallback =
            new BluetoothVolumeControl.Callback() {
                @Override
                public void onDeviceVolumeChanged(@NonNull BluetoothDevice device, int volume) {
                    if (!mCachedAudioRoutes.keySet().contains(device.getAddress())) {
                        LOG.i("[onDeviceVolumeChanged] unknown address: "
                                + device.getAnonymizedAddress());
                        return;
                    }
                    int groupId = mCarAudioManager.getVolumeGroupIdForUsage(mAudioZone, mUsage);
                    Integer cachedVolume = mAudioVolumeCache.get(device.getAddress());
                    int scaledVolume = scaleVolume(volume,
                            MIN_LE_AUDIO_VOLUME,
                            MAX_LE_AUDIO_VOLUME,
                            mCarAudioManager.getGroupMinVolume(groupId),
                            mCarAudioManager.getGroupMaxVolume(groupId));
                    if (cachedVolume != null && cachedVolume == scaledVolume) {
                        LOG.i(("[BluetoothVolumeControl.Callback#onDeviceVolumeChanged] Volume "
                                + "for device %s is unchanged. Skipping update.")
                                .formatted(device.getAnonymizedAddress()));
                        return;
                    }
                    LOG.i(("[BluetoothVolumeControl.Callback#onDeviceVolumeChanged] device: %s, "
                            + "originalVolume: %s, scaledVolume: %s")
                            .formatted(device.getAnonymizedAddress(), volume, scaledVolume));
                    mAudioVolumeCache.put(device.getAddress(), scaledVolume);
                    updateAndNotifyAudioRouteItemsIfChanged();
                }
            };

    private final CarAudioManager.CarVolumeCallback mCarVolumeCallback =
            new CarAudioManager.CarVolumeCallback() {
                @Override
                public void onGroupVolumeChanged(int zoneId, int groupId, int flags) {
                    mExecutor.execute(() -> {
                        if (zoneId != mAudioZone) {
                            return;
                        }
                        if (mCarAudioManager.getVolumeGroupIdForUsage(mAudioZone, mUsage)
                                != groupId) {
                            return;
                        }

                        mCachedAudioRoutes.entrySet().stream()
                                .filter(item -> item.getValue().getState() == UNICAST_ACTIVE)
                                .map(Map.Entry::getKey)
                                .findFirst()
                                .ifPresent(address -> {
                                    int volume = mCarAudioManager.getGroupVolume(groupId);
                                    Integer cachedVolume = mAudioVolumeCache.get(address);
                                    if (cachedVolume != null && cachedVolume == volume) {
                                        LOG.i(("[CarVolumeCallback#onGroupVolumeChanged] Volume "
                                                + "for device %s is unchanged. Skipping update.")
                                                .formatted(address));
                                        return;
                                    }
                                    LOG.i(("[CarVolumeCallback#onGroupVolumeChanged] device: %s, "
                                            + "volume: %s").formatted(address, volume));
                                    mAudioVolumeCache.put(address, volume);
                                    updateAndNotifyAudioRouteItemsIfChanged();
                                });
                    });
                }

                @Override
                public void onGroupMuteChanged(int zoneId, int groupId, int flags) {
                    // Optionally handle mute changes
                }

                @Override
                public void onMasterMuteChanged(int zoneId, int flags) {
                    // Optionally handle master mute changes
                }
            };

    private final BaseLeBroadcastAssistantCallback mBaseLeBroadcastAssistantCallback =
            new BaseLeBroadcastAssistantCallback() {
                @Override
                public void onSourceAdded(@NonNull BluetoothDevice sink, int sourceId, int reason) {
                    LOG.i(("[BaseLeBroadcastAssistantCallback#onSourceAdded] %s <%s>, sourceId: "
                            + "%s, reason: %s")
                            .formatted(sink.getName(), sink.getAddress(), sourceId, reason));
                    updateAndNotifyAudioRouteItemsIfChanged();
                }

                @Override
                public void onSourceRemoved(@NonNull BluetoothDevice sink, int sourceId,
                        int reason) {
                    LOG.i(("[BaseLeBroadcastAssistantCallback#onSourceRemoved] %s <%s>, sourceId: "
                            + "%s, reason: %s")
                            .formatted(sink.getName(), sink.getAddress(), sourceId, reason));
                    updateAndNotifyAudioRouteItemsIfChanged();
                }
            };

    private final BluetoothCallback mBluetoothCallback = new BluetoothCallback() {
        @Override
        public void onDeviceAdded(@NonNull CachedBluetoothDevice cachedDevice) {
            LOG.i("[BluetoothCallback#onDeviceAdded] device: "
                    + cachedDevice.getDevice().getAnonymizedAddress());
            updateAndNotifyAudioRouteItemsIfChanged();
        }

        @Override
        public void onDeviceDeleted(@NonNull CachedBluetoothDevice cachedDevice) {
            LOG.i("[BluetoothCallback#onDeviceDeleted] device: "
                    + cachedDevice.getDevice().getAnonymizedAddress());
            updateAndNotifyAudioRouteItemsIfChanged();
        }

        @Override
        public void onConnectionStateChanged(@Nullable CachedBluetoothDevice cachedDevice,
                int state) {
            LOG.i("[BluetoothCallback#onConnectionStateChanged] device: %s, state: %s".formatted(
                    cachedDevice == null ? "null" : cachedDevice.getDevice().getAnonymizedAddress(),
                    state));
            updateAndNotifyAudioRouteItemsIfChanged();
        }

        @Override
        public void onProfileConnectionStateChanged(@NonNull CachedBluetoothDevice cachedDevice,
                int state, int profileId) {
            LOG.i(("[BluetoothCallback#onProfileConnectionStateChanged] device: %s, state: %s, "
                    + "profileId: %s").formatted(cachedDevice.getDevice().getAnonymizedAddress(),
                    state, profileId));
            updateAndNotifyAudioRouteItemsIfChanged();
        }
    };


    private final SwitchAudioZoneConfigCallback mSwitchAudioZoneConfigCallback =
            (zoneConfig, isSuccessful) -> {
                LOG.i("[SwitchAudioZoneConfigCallback] Audio zone switch to [%s] successful: %s"
                        .formatted(zoneConfigInfoToString(zoneConfig), isSuccessful));
                List<CarAudioZoneConfigInfo> configs = mCarAudioManager.getAudioZoneConfigInfos(
                        mAudioZone);
                LOG.i("[SwitchAudioZoneConfigCallback] Audio zone configs: "
                        + zoneConfigInfosToString(configs));
                cancelActiveTimeout();
                cancelSelectTimeout();
                cancelTimeout(mCallbackDebouncer);
                updateAndNotifyAudioRouteItemsIfChanged();
            };

    private final AudioZoneConfigurationsChangeCallback mAudioZoneConfigurationsChangeCallback =
            new AudioZoneConfigurationsChangeCallback() {
                @Override
                public void onAudioZoneConfigurationsChanged(
                        @NonNull List<CarAudioZoneConfigInfo> configs, int status) {
                    List<CarAudioZoneConfigInfo> relevantConfigs = configs.stream().filter(
                            info -> info.getZoneId() == mAudioZone).toList();
                    logConfigChange(relevantConfigs, status);
                    cancelActiveTimeout();
                    cancelSelectTimeout();
                    mCallbackDebouncer = mExecutor.schedule(() -> {
                        cancelTimeout(mCallbackDebouncer);
                        updateAndNotifyAudioRouteItemsIfChanged();
                        restartActiveTimeout();
                        restartSelectTimeout();
                    }, DEBOUNCE_INTERVAL_IN_MILLIS, TimeUnit.MILLISECONDS);
                }

                private void logConfigChange(List<CarAudioZoneConfigInfo> configs, int status) {
                    String statusName = switch (status) {
                        case CONFIG_STATUS_CHANGED -> "CONFIG_STATUS_CHANGED";
                        case CONFIG_STATUS_AUTO_SWITCHED -> "CONFIG_STATUS_AUTO_SWITCHED";
                        default -> "Status: " + status;
                    };
                    LOG.i("[AudioZoneConfigurationsChangeCallback]: (%s) %s"
                            .formatted(statusName, zoneConfigInfosToString(configs)));
                }
            };

    private void restartActiveTimeout() {
        mActiveTimeout = scheduleTimeout(mActiveTimeout, mPendingActiveAddress, () -> {
            LOG.i("[mActiveTimeoutFuture] Timed out waiting for active: "
                    + AudioRouteItem.redact(mPendingActiveAddress));
            cancelStartingUnicast();
        });
    }

    private void restartSelectTimeout() {
        mSelectTimeout = scheduleTimeout(mSelectTimeout, mPendingSelectAddress, () -> {
            LOG.i("[mSelectTimeoutFuture] Timed out waiting for select: "
                    + AudioRouteItem.redact(mPendingSelectAddress));
            cancelStartingUnicast();
        });
    }

    private ScheduledFuture<?> scheduleTimeout(ScheduledFuture<?> currentFuture,
            Object triggerAddress, @NonNull Runnable task) {
        cancelTimeout(currentFuture);
        if (!mUseTimeout || triggerAddress == null) {
            return null;
        }
        return mExecutor.schedule(task, TIMEOUT_IN_SECS, TimeUnit.SECONDS);
    }

    private void cancelActiveTimeout() {
        cancelTimeout(mActiveTimeout);
    }

    private void cancelSelectTimeout() {
        cancelTimeout(mSelectTimeout);
    }

    private void cancelTimeout(ScheduledFuture<?> timer) {
        if (timer != null) {
            boolean cancelled = timer.cancel(/* mayInterruptIfRunning= */ false);
        }
    }

    @VisibleForTesting
    void cancelStartingUnicast() {
        Stream<AudioRouteEvent> cancelStream = mCachedAudioRoutes.values().stream()
                .filter(item -> item.getState() == STARTING_UNICAST
                        && (Objects.equals(item.getAddress(), mPendingActiveAddress)
                        || Objects.equals(item.getAddress(), mPendingSelectAddress)))
                .findFirst()
                .stream()
                .map(item -> new AudioRouteEvent(item.getAddress(), CANCEL_STARTING_UNICAST));

        Stream<AudioRouteEvent> resetStream = mCachedAudioRoutes.values().stream()
                .map(item -> new AudioRouteEvent(item.getAddress(), RESET));

        requestAudioRouteEvents(Stream.concat(cancelStream, resetStream).toList());
    }

    private String zoneConfigInfosToString(List<CarAudioZoneConfigInfo> configs) {
        return String.join(" | ", configs.stream().map(this::zoneConfigInfoToString).collect(
                Collectors.joining(" | ")));
    }

    private String zoneConfigInfoToString(CarAudioZoneConfigInfo config) {
        List<String> devices = config.getConfigVolumeGroups().stream()
                .filter(volumeGroup -> volumeGroup.getAudioAttributes().stream()
                        .anyMatch(audioAttr -> audioAttr.getUsage() == mUsage))
                .flatMap(volumeGroup -> volumeGroup.getAudioDeviceAttributes().stream())
                .map(attr -> "%s <%s>"
                        .formatted(attr.getName(), AudioRouteItem.redact(attr.getAddress())))
                .collect(toList());
        String status = Stream.<String>builder().add(config.isActive() ? "Active" : null).add(
                config.isSelected() ? "Selected" : null).add(
                config.isDefault() ? "Default" : null).build().filter(Objects::nonNull).collect(
                Collectors.joining(","));
        String deviceStr = !devices.isEmpty() ? String.join(",", devices) : "No device !!!";
        return config.getName() + " [" + status + "] : " + deviceStr;
    }

    @VisibleForTesting
    void updateAndNotifyAudioRouteItemsIfChanged() {
        LOG.i("[updateAndNotifyAudioRouteItemsIfChanged]");
        updateAndNotifyAudioRouteItems(/* forceNotify= */ false);
    }

    private void updateAndNotifyAudioRouteItems(boolean forceNotify) {
        mExecutor.execute(() -> {
            Map<String, AudioRouteItem> newAudioRouteItems = createAudioRoutes();

            List<AudioRouteItem> selfDrivenStates = newAudioRouteItems.values().stream().filter(
                    item -> item.getState().isSelfDrivenState()).toList();

            if (!selfDrivenStates.isEmpty()) {
                List<AudioRouteEvent> events = selfDrivenStates.stream().map(
                        item -> new AudioRouteEvent(item.getAddress(), CHECK_CONDITIONS)).toList();
                processAudioRouteEvents(events, newAudioRouteItems);
            } else {
                // If not in a self-driven state (no pending user action), mirror the Car Audio.
                List<AudioRouteEvent> events = newAudioRouteItems.values().stream().map(
                        item -> new AudioRouteEvent(item.getAddress(), RESET)).toList();
                processAudioRouteEvents(events, newAudioRouteItems);
            }

            applyAndNotifyNewAudioRouteItems(newAudioRouteItems, forceNotify);
        });
    }

    private void requestAudioRouteEvent(AudioRouteEvent event) {
        requestAudioRouteEvents(List.of(event));
    }

    private void requestAudioRouteEvents(List<AudioRouteEvent> events) {
        mExecutor.execute(() -> {
            Map<String, AudioRouteItem> newAudioRouteItems = createAudioRoutes();

            processAudioRouteEvents(events, newAudioRouteItems);
            applyAndNotifyNewAudioRouteItems(newAudioRouteItems, true);
        });
    }

    private void processAudioRouteEvents(List<AudioRouteEvent> events,
            Map<String, AudioRouteItem> newAudioRouteItems) {
        while (!events.isEmpty()) {
            LOG.i("[processAudioRouteEvents] " + events);
            events = handleEvents(events, newAudioRouteItems);
        }
    }

    private void applyAndNotifyNewAudioRouteItems(Map<String, AudioRouteItem> newAudioRouteItems,
            boolean forceNotify) {
        boolean updated = audioRouteItemsChanged(newAudioRouteItems);
        if (updated) {
            LOG.i("[applyAndNotifyNewAudioRouteItems] AudioRouteItems changed: "
                    + newAudioRouteItems.entrySet().stream().collect(
                    Collectors.toMap(e -> AudioRouteItem.redact(e.getKey()), Map.Entry::getValue)));
        }

        boolean notify = forceNotify || updated;

        // Apply new routes immediately as they might be read by other background tasks
        applyNewAudioRouteItems(newAudioRouteItems);

        if (notify && mAudioRoutesUpdateListener != null) {
            ContextCompat.getMainExecutor(mContext).execute(() -> {
                if (mAudioRoutesUpdateListener != null) {
                    mAudioRoutesUpdateListener.onAudioRoutesUpdated(
                            mCachedAudioRoutes.values().stream().toList());
                }
            });
        }
    }

    private void applyNewAudioRouteItems(Map<String, AudioRouteItem> newAudioRouteItems) {
        mCachedAudioRoutes = newAudioRouteItems;
        mCachedAudioRoutes.values().stream()
                .filter(item -> item.getAudioZoneConfigState().isSelected()).findFirst()
                .map(AudioRouteItem::getAddress)
                .ifPresentOrElse(
                        address -> mCachedActiveDeviceAddress = address,
                        // There is a situation, an audio route is selected but no device is
                        // assigned.
                        () -> LOG.e("[applyNewAudioRouteItems] Invalid state: No audio route is "
                                + "selected"));
    }

    private List<AudioRouteEvent> handleEvents(List<AudioRouteEvent> events,
            Map<String, AudioRouteItem> audioRouteItems) {
        Iterator<AudioRouteEvent> iterator = events.iterator();
        List<AudioRouteEvent> chainEvents = new ArrayList<>();

        while (iterator.hasNext()) {
            AudioRouteEvent event = iterator.next();
            AudioRouteItem.Command command = event.command;
            AudioRouteItem audioRoute = audioRouteItems.get(event.address());
            if (audioRoute == null) {
                LOG.i("[handleEvents] AudioRoute not found: "
                        + AudioRouteItem.redact(event.address()));
                continue;
            }

            AudioRouteItem.State newState;
            if (event.state == AudioRouteItem.State.UNSPECIFIED) {
                newState = audioRoute.getState();
            } else {
                newState = event.state;
            }

            boolean addNewCheckEventWithNewState = false;

            boolean isBroadcastReady = audioRouteItems.values().stream().anyMatch(
                    item -> item.getAudioRouteType() == TYPE_BLE_BROADCAST
                            && item.getAudioZoneConfigState().isActive());
            boolean isBroadcasting = audioRouteItems.values().stream().anyMatch(
                    item -> item.getAudioRouteType() == TYPE_BLE_BROADCAST
                            && item.getAudioZoneConfigState().isSelected());
            boolean isAnyBleUnicasting = audioRouteItems.values().stream().anyMatch(
                    item -> item.getAudioRouteType() == TYPE_BLE_HEADSET
                            && item.getAudioZoneConfigState().isSelected());
            boolean isAnyReceivingBroadcast = audioRouteItems.values().stream().anyMatch(
                    item -> item.getBluetoothDeviceState().isReceivingBroadcast());

            BluetoothLeBroadcastMetadata broadcastMetadata = getCurrentBroadcast();

            switch (audioRoute.getState()) {
                case CREATED:
                    if (command != CHECK_CONDITIONS) {
                        break;
                    }
                    if (audioRoute.getAudioRouteType() == TYPE_BLE_BROADCAST) {
                        if (audioRoute.getAudioZoneConfigState().isActive()) {
                            newState = BROADCAST_READY;
                        }
                        if (audioRoute.getAudioZoneConfigState().isSelected()) {
                            newState = BROADCAST_ACTIVE;
                        }
                    } else if (audioRoute.getBluetoothDeviceState().isReceivingBroadcast()) {
                        newState = MULTICAST_ACTIVE;
                    } else if (audioRoute.getAudioZoneConfigState().isSelected()) {
                        if (audioRoute.getAudioRouteType() == TYPE_BLE_HEADSET && isBroadcasting) {
                            newState = MULTICAST_READY_UNICAST_ACTIVE;
                        } else {
                            newState = UNICAST_ACTIVE;
                        }
                    } else {
                        if (audioRoute.getGlobalState().isAudioSharingEnabled()
                                && audioRoute.getAudioRouteType() == TYPE_BLE_HEADSET
                                && (isAnyBleUnicasting || isAnyReceivingBroadcast)) {
                            newState = MULTICAST_READY_UNICAST_READY;
                        } else {
                            newState = UNICAST_READY;
                        }
                    }
                    break;

                case UNICAST_READY:
                    if (command == RESET) {
                        newState = CREATED;
                        addNewCheckEventWithNewState = true;
                    } else if (command == START_UNICAST) {
                        if (audioRouteItems.values().stream().anyMatch(
                                item -> item.getState().isStartingState())) {
                            LOG.i("[handleEvents] <UNICAST_READY> DENIED START_UNICAST for "
                                    + audioRoute.getName());
                            break;
                        }
                        newState = STARTING_UNICAST;
                        addNewCheckEventWithNewState = true;
                    }
                    break;

                case STARTING_UNICAST:
                    if (command == CANCEL_STARTING_UNICAST) {
                        newState = CREATED;
                        addNewCheckEventWithNewState = true;
                        mPendingActiveAddress = null;
                        mPendingSelectAddress = null;
                        cancelActiveTimeout();
                        cancelSelectTimeout();
                        ContextCompat.getMainExecutor(mContext).execute(() -> {
                            Toast.makeText(mContext, mContext.getString(
                                    R.string.audio_route_preference_connecting_failed,
                                    audioRoute.getName()), Toast.LENGTH_SHORT).show();
                        });
                        break;
                    }

                    if (command != CHECK_CONDITIONS) break;

                    if (!audioRoute.getAudioZoneConfigState().isActive()) {
                        if (Objects.equals(mPendingActiveAddress, audioRoute.getAddress())) {
                            LOG.i("[handleEvents] <STARTING_UNICAST> setActive already requested "
                                    + "for " + audioRoute.getName());
                            break;
                        }
                        LOG.i("[handleEvents] <STARTING_UNICAST> Bluetooth setActive: "
                                + audioRoute.getName());
                        audioRoute.getBluetoothDevice().setActive();
                        mPendingActiveAddress = audioRoute.getAddress();
                        restartActiveTimeout();
                        break;
                    }

                    mPendingActiveAddress = null;
                    cancelActiveTimeout();

                    if (!audioRoute.getAudioZoneConfigState().isSelected()) {
                        if (Objects.equals(mPendingSelectAddress, audioRoute.getAddress())) {
                            LOG.i("[handleEvents] <STARTING_UNICAST> requestRouteSwitchInternal "
                                    + "already called " + "for " + audioRoute.getName());
                            break;
                        }
                        LOG.i("[handleEvents] <STARTING_UNICAST> requestRouteSwitchInternal for "
                                + audioRoute.getName());
                        requestRouteSwitchInternal(audioRoute);
                        mPendingSelectAddress = audioRoute.getAddress();
                        restartSelectTimeout();
                        break;
                    }

                    mPendingSelectAddress = null;
                    cancelSelectTimeout();

                    // Unicast becomes active.
                    if (isBroadcastReady
                            && audioRoute.getBluetoothDeviceState().isConnectedLeAudio()) {
                        newState = MULTICAST_READY_UNICAST_ACTIVE;
                    } else {
                        newState = UNICAST_ACTIVE;
                    }

                    chainEvents.addAll(audioRouteItems.values().stream()
                            .filter(item -> item.getState() != MULTICAST_ACTIVE)
                            .map(item -> new AudioRouteEvent(item.getAddress(), RESET))
                            .toList());
                    // Stop broadcast if unicast starts.
                    chainEvents.addAll(audioRouteItems.values().stream()
                            .filter(item -> item.getState() == MULTICAST_ACTIVE)
                            .map(item -> new AudioRouteEvent(item.getAddress(), LEAVE_BROADCAST))
                            .toList());
                    break;

                case UNICAST_ACTIVE:
                    if (command == RESET) {
                        newState = CREATED;
                        addNewCheckEventWithNewState = true;
                    } else if (command == STOP_UNICAST) {
                        newState = UNICAST_READY;
                    } else if (command == JOIN_BROADCAST) {
                        newState = STARTING_BROADCAST;
                        addNewCheckEventWithNewState = true;
                    }
                    break;

                case MULTICAST_READY_UNICAST_ACTIVE:
                    if (command == RESET) {
                        newState = CREATED;
                        addNewCheckEventWithNewState = true;
                    } else if (command == STOP_UNICAST) {
                        newState = MULTICAST_READY_UNICAST_READY;
                    } else if (command == JOIN_BROADCAST) {
                        newState = STARTING_BROADCAST;
                        addNewCheckEventWithNewState = true;
                    }
                    break;

                case STARTING_BROADCAST:
                    if (command != CHECK_CONDITIONS) break;

                    if (!audioRoute.isBluetoothAudioRoute()) {
                        LOG.i("[handleEvents] <STARTING_BROADCAST> Not Bluetooth device: "
                                + audioRoute.getName());
                        break;
                    }

                    if (!isBroadcastReady) {
                        LOG.i("[handleEvents] <STARTING_BROADCAST> Starting broadcast...");
                        startBroadcast();
                        break;
                    }

                    if (!isBroadcasting) {
                        Optional<AudioRouteItem> changeRoute =
                                audioRouteItems.values().stream().filter(
                                        item -> item.getAudioRouteType()
                                                == TYPE_BLE_BROADCAST).findFirst();

                        if (changeRoute.isPresent()) {
                            LOG.i("[handleEvents] <STARTING_BROADCAST> requestRouteSwitchInternal"
                                    + " for " + changeRoute.get().getAnonymizedAddress());
                            requestRouteSwitchInternal(changeRoute.get());
                        }
                    } else {
                        LOG.i("[handleEvents] <STARTING_BROADCAST> Broadcasting...");
                        chainEvents.addAll(audioRouteItems.values().stream()
                                .filter(item -> item.getState() == UNICAST_ACTIVE
                                        && item.getBluetoothDevice().isConnectedLeAudioDevice())
                                .map(item -> new AudioRouteEvent(item.getAddress(), JOIN_BROADCAST))
                                .toList());
                        chainEvents.addAll(audioRouteItems.values().stream()
                                .filter(item -> item.getState() == BROADCAST_READY)
                                .map(item -> new AudioRouteEvent(item.getAddress(), RESET))
                                .toList());
                        newState = JOINING_BROADCAST;
                        addNewCheckEventWithNewState = true;
                    }
                    break;

                case JOINING_BROADCAST:
                    if (command != CHECK_CONDITIONS) break;

                    if (!isBroadcasting) {
                        LOG.i("[startBroadcast] Broadcasting stopped...");
                        break;
                    }

                    if (!isReceivingBroadcast(audioRoute.getBluetoothDevice().getDevice(),
                            broadcastMetadata)) {
                        LOG.i("[handleEvents] <JOINING_BROADCAST> addSource %s to broadcast"
                                .formatted(audioRoute.getName()));
                        mLeBroadcastAssistantProfile.addSource(
                                audioRoute.getBluetoothDevice().getDevice(),
                                broadcastMetadata, /* isGroupOp= */ true);

                    } else {
                        LOG.i("[handleEvents] <JOINING_BROADCAST> JOINED");
                        newState = MULTICAST_ACTIVE;

                        chainEvents.addAll(audioRouteItems.values().stream()
                                .filter(item -> item.getState() == UNICAST_ACTIVE)
                                .map(item -> new AudioRouteEvent(item.getAddress(), RESET))
                                .toList());
                        chainEvents.addAll(audioRouteItems.values().stream()
                                .filter(item -> item.getState() == MULTICAST_READY_UNICAST_ACTIVE)
                                .map(item -> new AudioRouteEvent(item.getAddress(), RESET))
                                .toList());
                    }
                    break;

                case MULTICAST_READY_UNICAST_READY:
                    if (command == RESET) {
                        newState = CREATED;
                        addNewCheckEventWithNewState = true;
                    } else if (command == JOIN_BROADCAST) {
                        newState = STARTING_BROADCAST;
                        addNewCheckEventWithNewState = true;
                    } else if (command == START_UNICAST) {
                        chainEvents.addAll(audioRouteItems.values().stream()
                                .filter(item -> item.getState() == MULTICAST_ACTIVE)
                                .map(item -> new AudioRouteEvent(item.getAddress(),
                                        LEAVE_BROADCAST))
                                .toList());
                        newState = STARTING_UNICAST;
                        addNewCheckEventWithNewState = true;
                    }
                    break;

                case MULTICAST_ACTIVE:
                    if (command == RESET) {
                        newState = CREATED;
                        addNewCheckEventWithNewState = true;
                    } else if (command == LEAVE_BROADCAST) {
                        newState = LEAVING_BROADCAST;
                        addNewCheckEventWithNewState = true;
                    }
                    break;

                case LEAVING_BROADCAST:
                    if (command != CHECK_CONDITIONS) break;

                    List<BluetoothLeBroadcastReceiveState> deviceSources =
                            mLeBroadcastAssistantProfile.getAllSources(
                                    audioRoute.getBluetoothDevice().getDevice());

                    for (BluetoothLeBroadcastReceiveState source : deviceSources) {
                        if (broadcastMetadata.getBroadcastId() == source.getBroadcastId()) {
                            mLeBroadcastAssistantProfile.removeSource(
                                    audioRoute.getBluetoothDevice().getDevice(),
                                    /* sourceId */ source.getSourceId());
                            LOG.i("[handleEvents] <LEAVING_BROADCAST> Leaving broadcast: "
                                    + audioRoute.getName());
                        }
                    }

                    if (!audioRoute.getBluetoothDeviceState().isReceivingBroadcast()) {
                        LOG.i("[handleEvents] <LEAVING_BROADCAST> Left broadcast: "
                                + audioRoute.getName());
                        if (!isAnyReceivingBroadcast) {
                            LOG.i("[handleEvents] <LEAVING_BROADCAST> All left. Stopping "
                                    + "broadcast");
                            mLeBroadcastProfile.stopLatestBroadcast();
                        }

                        newState = MULTICAST_READY_UNICAST_READY;
                    }
                    break;

                case BROADCAST_READY:
                    if (command == RESET) {
                        newState = CREATED;
                        addNewCheckEventWithNewState = true;
                    }
                    break;

                case BROADCAST_ACTIVE:
                    if (command == RESET) {
                        newState = CREATED;
                        addNewCheckEventWithNewState = true;
                    }
                    break;
            }

            if (addNewCheckEventWithNewState) {
                chainEvents.add(
                        new AudioRouteEvent(audioRoute.getAddress(), CHECK_CONDITIONS, newState));
            }

            if (newState != audioRoute.getState()) {
                LOG.i("[handleEvents] Transition %s : <%s> -> <%s> (%s)".formatted(
                        audioRoute.getName(), audioRoute.getState(), newState, command));
            }

            audioRouteItems.put(audioRoute.getAddress(), new AudioRouteItem.Builder(
                    audioRoute).setState(newState).build());
        }
        return chainEvents;
    }

    private boolean audioRouteItemsChanged(Map<String, AudioRouteItem> audioRoutes) {
        if (!audioRoutes.keySet().equals(mCachedAudioRoutes.keySet())) {
            return true;
        }

        return audioRoutes.values().stream().anyMatch(item -> {
            AudioRouteItem cachedItem = mCachedAudioRoutes.get(item.getAddress());
            if (item.getState() != cachedItem.getState()) {
                return true;
            }
            // Compare VolumeState only if both are non-null
            AudioRouteItem.VolumeState currentVolumeState = item.getVolumeState();
            AudioRouteItem.VolumeState cachedVolumeState = cachedItem.getVolumeState();
            if (currentVolumeState != null && cachedVolumeState != null) {
                return currentVolumeState.getCurrentVolume()
                        != cachedVolumeState.getCurrentVolume();
            } else {
                // If one is null and the other isn't, they are different
                return currentVolumeState != cachedVolumeState;
            }
        });
    }


    private Map<String, AudioRouteItem> createAudioRoutes() {
        Map<String, AudioRouteItem> newAudioRoutes = new HashMap<>();
        List<CarAudioZoneConfigInfo> configs = mCarAudioManager.getAudioZoneConfigInfos(mAudioZone);
        LOG.i("[createAudioRoutes] CarAudioZoneConfigInfo: " + zoneConfigInfosToString(configs));
        LOG.i("[createAudioRoutes] mAudioVolumeMap: " + mAudioVolumeCache.entrySet().stream()
                .collect(Collectors.toMap(e ->
                        AudioRouteItem.redact(e.getKey()), Map.Entry::getValue)));

        Map<String, CarAudioZoneConfigInfo> activeConfigs =
                removeDuplicateZoneConfigsWithSameAddress(configs);

        boolean isAudioSharingEnabled = isAudioSharingEnabled();
        AudioRouteItem.GlobalState globalState =
                new AudioRouteItem.GlobalState.Builder().setIsAudioSharingEnabled(
                        isAudioSharingEnabled).build();

        int groupId = mCarAudioManager.getVolumeGroupIdForUsage(mAudioZone, mUsage);
        int zoneMaxVolume = mCarAudioManager.getGroupMaxVolume(groupId);
        int zoneMinVolume = mCarAudioManager.getGroupMinVolume(groupId);
        int zoneCurrentVolume = mCarAudioManager.getGroupVolume(groupId);
        LOG.i("[createAudioRoutes] zoneCurrentVolume: " + zoneCurrentVolume);

        AudioRouteItem.VolumeState zoneVolumeState = new AudioRouteItem.VolumeState.Builder()
                .setMaxVolume(zoneMaxVolume)
                .setMinVolume(zoneMinVolume)
                .setCurrentVolume(zoneCurrentVolume)
                .build();

        // Register for all active car audio zone configs.
        for (CarAudioZoneConfigInfo config : activeConfigs.values()) {
            for (CarVolumeGroupInfo volumeGroup : config.getConfigVolumeGroups()) {
                boolean hasCorrectUsage = volumeGroup.getAudioAttributes().stream().anyMatch(
                        audioAttribute -> audioAttribute.getUsage() == mUsage);
                if (!hasCorrectUsage) continue;

                for (AudioDeviceAttributes attr : volumeGroup.getAudioDeviceAttributes()) {
                    AudioRouteItem.Builder builder = new AudioRouteItem.Builder(attr);
                    if (mCachedAudioRoutes.containsKey(attr.getAddress())) {
                        // Copy the previous state if existing.
                        AudioRouteItem cachedItem = mCachedAudioRoutes.get(attr.getAddress());
                        builder.setState(cachedItem.getState());
                    }
                    builder.setAudioZoneConfigState(
                            new AudioRouteItem.AudioZoneConfigState.Builder()
                                    .setIsActive(config.isActive())
                                    .setIsSelected(config.isSelected())
                                    .build());
                    builder.setGlobalState(globalState);
                    if (config.isSelected()) {
                        builder.setVolumeState(zoneVolumeState);
                    }
                    newAudioRoutes.put(attr.getAddress(), builder.build());
                }
            }
        }

        Set<String> activeConfigDevices = newAudioRoutes.keySet();

        // Register for all connected Bluetooth devices.
        List<CachedBluetoothDevice> bluetoothDevices =
                mBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy().stream().filter(
                        device -> device.isConnectedA2dpDevice()
                                || device.isConnectedLeAudioDevice()).toList();
        for (CachedBluetoothDevice device : bluetoothDevices) {
            AudioRouteItem.Builder builder = new AudioRouteItem.Builder(device);
            if (mCachedAudioRoutes.containsKey(device.getAddress())) {
                // Copy the previous state if existing.
                AudioRouteItem cachedItem = mCachedAudioRoutes.get(device.getAddress());
                builder.setState(cachedItem.getState());
            }

            AudioRouteItem.AudioZoneConfigState audioZoneConfigState = activeConfigDevices.contains(
                    device.getAddress())
                    ? newAudioRoutes.get(device.getAddress()).getAudioZoneConfigState()
                    : new AudioRouteItem.AudioZoneConfigState.Builder().build();
            builder.setAudioZoneConfigState(audioZoneConfigState);

            boolean isReceivingBroadcast = isAudioSharingEnabled() && isReceivingBroadcast(
                    device.getDevice(), getCurrentBroadcast());

            AudioRouteItem.BluetoothDeviceState bluetoothDeviceState =
                    new AudioRouteItem.BluetoothDeviceState.Builder()
                            .setIsConnectedA2dp(device.isConnectedA2dpDevice())
                            .setIsConnectedLeAudio(device.isConnectedLeAudioDevice())
                            .setIsActiveA2dp(device.isActiveDevice(BluetoothProfile.A2DP))
                            .setIsActiveLeAudio(device.isActiveDevice(BluetoothProfile.LE_AUDIO))
                            .setIsReceivingBroadcast(isReceivingBroadcast)
                            .build();

            AudioRouteItem.VolumeState.Builder volumeStateBuilder = zoneVolumeState.toBuilder();
            int cachedVolume = mAudioVolumeCache.getOrDefault(device.getAddress(), -1);
            if (cachedVolume != -1) {
                volumeStateBuilder.setCurrentVolume(cachedVolume);
            }

            if (mLeAudioProfile.getBroadcastToUnicastFallbackGroup() == device.getGroupId()
                    || audioZoneConfigState.isSelected()) {
                volumeStateBuilder.setUseVolumeControlProfile(false);
            } else {
                volumeStateBuilder.setUseVolumeControlProfile(
                        isVolumeControlSupported(device.getDevice()));
            }

            builder.setVolumeState(volumeStateBuilder.build());
            builder.setBluetoothDeviceState(bluetoothDeviceState);
            builder.setGlobalState(globalState);
            newAudioRoutes.put(device.getAddress(), builder.build());
        }
        LOG.i("[createAudioRoutes] newAudioRoutes: " + newAudioRoutes.entrySet().stream().collect(
                Collectors.toMap(e -> AudioRouteItem.redact(e.getKey()), Map.Entry::getValue)));
        return newAudioRoutes;
    }

    @NonNull
    private Map<String, CarAudioZoneConfigInfo> removeDuplicateZoneConfigsWithSameAddress(
            List<CarAudioZoneConfigInfo> configs) {
        return configs.stream()
                .filter(CarAudioZoneConfigInfo::isActive)
                .flatMap(config -> config.getConfigVolumeGroups().stream()
                        .filter(group -> group.getAudioAttributes().stream()
                                .anyMatch(attr -> attr.getUsage() == mUsage))
                        .flatMap(group -> group.getAudioDeviceAttributes().stream())
                        .map(device -> Map.entry(device.getAddress(), config)))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,   // Key: Device Address
                        Map.Entry::getValue, // Value: CarAudioZoneConfigInfo
                        // If address exists in multiple configs, prioritize the selected one.
                        (a, b) -> b.isSelected() ? b : a));
    }

    private void requestRouteSwitchInternal(AudioRouteItem switchedRoute) {
        LOG.i("[requestRouteSwitchInternal] Trying to switch audio route to "
                + switchedRoute.getName());

        List<CarAudioZoneConfigInfo> configs = mCarAudioManager.getAudioZoneConfigInfos(mAudioZone);
        LOG.i("[requestRouteSwitchInternal] Current audio zone configs: " + zoneConfigInfosToString(
                configs));

        for (CarAudioZoneConfigInfo carAudioZoneConfigInfo : configs) {
            for (CarVolumeGroupInfo carVolumeGroupInfo :
                    carAudioZoneConfigInfo.getConfigVolumeGroups()) {
                boolean hasCorrectUsage = carVolumeGroupInfo.getAudioAttributes().stream().anyMatch(
                        audioAttribute -> audioAttribute.getUsage() == mUsage);
                // b/453538644 - Note that we are not checking device type, but just pick one
                // matching address.
                boolean hasCorrectAddress =
                        carVolumeGroupInfo.getAudioDeviceAttributes().stream().anyMatch(
                                deviceAttribute -> switchedRoute.getAddress().equals(
                                        deviceAttribute.getAddress()));

                if (hasCorrectUsage && hasCorrectAddress && carAudioZoneConfigInfo.isActive()) {
                    if (carAudioZoneConfigInfo.isSelected()) {
                        LOG.i("[requestRouteSwitchInternal] Audio route is already selected: "
                                + carAudioZoneConfigInfo.getName());
                        return;
                    }
                    try {
                        LOG.i("[requestRouteSwitchInternal] switchAudioZoneToConfig to %s for %s"
                                .formatted(carAudioZoneConfigInfo.getName(),
                                        switchedRoute.getName()));
                        mCarAudioManager.switchAudioZoneToConfig(carAudioZoneConfigInfo,
                                mExecutor,
                                mSwitchAudioZoneConfigCallback);
                    } catch (IllegalStateException e) {
                        LOG.e("[requestRouteSwitchInternal] IllegalStateException occurred during"
                                + " audio zone switching: " + e);
                        continue;
                    }
                    return;
                }
            }
        }
    }

    private BluetoothLeBroadcastMetadata getCurrentBroadcast() {
        // do not check broadcast profiles before accessing broadcast metadata, as an NPE would
        // indicate this method was called somewhere where BLE profile check should have been done
        // prior to getting current broadcast
        List<BluetoothLeBroadcastMetadata> metadata = mLeBroadcastProfile.getAllBroadcastMetadata();
        if (metadata.isEmpty()) {
            return null;
        }
        return metadata.getFirst();
    }

    private boolean isReceivingBroadcast(BluetoothDevice device,
            BluetoothLeBroadcastMetadata broadcastMetadata) {
        if (broadcastMetadata == null || device == null) {
            return false;
        }
        for (BluetoothLeBroadcastReceiveState state : mLeBroadcastAssistantProfile.getAllSources(
                device)) {
            if (state.getSourceDevice().equals(broadcastMetadata.getSourceDevice())) {
                return true;
            }
        }
        return false;
    }

    private boolean isAudioSharingEnabled() {
        return isUserAudioSharingEnabled(mContext) && mLeBroadcastAssistantProfile != null;
    }

    private void startBroadcast() {
        mLeBroadcastProfile.stopLatestBroadcast();
        mLeBroadcastProfile.startBroadcast(CarSettingsApplication.CAR_SETTINGS_PACKAGE_NAME,
                /* language= */ null);
        mLeBroadcastProfile.setBroadcastCode(new byte[0]);
    }

    private boolean isVolumeControlSupported(BluetoothDevice device) {
        if (mVolumeControlProfile == null) {
            return false;
        }
        return mVolumeControlProfile.getConnectionStatus(device)
                == BluetoothProfile.STATE_CONNECTED;
    }

    private static int scaleVolume(int value, int inMin, int inMax, int outMin, int outMax) {
        if (inMax == inMin) {
            return outMin;
        }
        return (int) Math.round(
                (double) (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin);
    }
}
