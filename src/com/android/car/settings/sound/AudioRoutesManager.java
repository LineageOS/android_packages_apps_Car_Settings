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
import static com.android.car.settings.sound.AudioRouteItem.Command.CHECK_CONDITIONS;
import static com.android.car.settings.sound.AudioRouteItem.Command.JOIN_BROADCAST;
import static com.android.car.settings.sound.AudioRouteItem.Command.LEAVE_BROADCAST;
import static com.android.car.settings.sound.AudioRouteItem.Command.RESET;
import static com.android.car.settings.sound.AudioRouteItem.Command.START_UNICAST;
import static com.android.car.settings.sound.AudioRouteItem.Command.STOP_UNICAST;
import static com.android.car.settings.sound.AudioRouteItem.State.BROADCAST_ACTIVE;
import static com.android.car.settings.sound.AudioRouteItem.State.BROADCAST_READY;
import static com.android.car.settings.sound.AudioRouteItem.State.CREATED;
import static com.android.car.settings.sound.AudioRouteItem.State.MULTICAST_ACTIVE;
import static com.android.car.settings.sound.AudioRouteItem.State.MULTICAST_READY_UNICAST_ACTIVE;
import static com.android.car.settings.sound.AudioRouteItem.State.MULTICAST_READY_UNICAST_READY;
import static com.android.car.settings.sound.AudioRouteItem.State.STARTING_BROADCAST;
import static com.android.car.settings.sound.AudioRouteItem.State.STARTING_UNICAST;
import static com.android.car.settings.sound.AudioRouteItem.State.UNICAST_ACTIVE;
import static com.android.car.settings.sound.AudioRouteItem.State.UNICAST_READY;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.car.media.AudioZoneConfigurationsChangeCallback;
import android.car.media.CarAudioManager;
import android.car.media.CarAudioZoneConfigInfo;
import android.car.media.CarVolumeGroupInfo;
import android.car.media.SwitchAudioZoneConfigCallback;
import android.content.Context;
import android.media.AudioDeviceAttributes;
import android.os.CountDownTimer;
import android.util.ArrayMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;

import com.android.car.settings.CarSettingsApplication;
import com.android.car.settings.R;
import com.android.car.settings.common.Logger;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * // TODO: rename to AudioOutputManager and allow registration of multiple callbacks to
 * CarAudioManager.
 * Manages the audio routes and volumes.
 */
public class AudioRoutesManager {
    private static final Logger LOG = new Logger(AudioRoutesManager.class);
    public static final int TIMEOUT_IN_MILLIS = 5_000;
    public static final int TIMEOUT_INTERVAL_IN_MILLIS = 1_000;
    public static final int DEBOUNCE_INTERVAL_IN_MILLIS = 1_000;
    private final Context mContext;
    private CarAudioManager mCarAudioManager = null;
    private final LocalBluetoothManager mBluetoothManager;
    private LocalBluetoothLeBroadcast mLeBroadcastProfile;
    private LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistantProfile;
    private int mAudioZone;
    private final int mUsage;
    private String mLastSwitchedAddress;
    private AudioRoutesUpdateListener mAudioRoutesUpdateListener;
    private Map<String, AudioRouteItem> mCachedAudioRoutes;
    private String mCachedActiveDeviceAddress;
    // The following four fields are used to manage the state transitions when requesting
    // audio route changes. mPendingActiveAddress and mPendingSelectAddress track the device
    // addresses for which setActive and switchAudioZoneToConfig, respectively, have been
    // requested. mCallbackDebouncer is used to group multiple audio zone configuration change
    // callbacks, and mTimeoutTimer is used to handle potential timeouts during these operations.
    private String mPendingActiveAddress = null;
    private String mPendingSelectAddress = null;
    private CountDownTimer mCallbackDebouncer = null;
    private CountDownTimer mTimeoutTimer = null;

    private record AudioRouteEvent(AudioRouteItem.Command command, AudioRouteItem audioRouteItem) {
    }

    /**
     * A listener for when the AudioRouteItems are updated.
     */
    public interface AudioRoutesUpdateListener {
        /** Called when a state of any AudioRouteItems is changed. */
        void onAudioRoutesUpdated(List<AudioRouteItem> audioRouteItems);
    }

    public AudioRoutesManager(Context context, int usage) {
        mContext = context;
        mCarAudioManager =
                ((CarSettingsApplication) mContext.getApplicationContext()).getCarAudioManager();
        mAudioZone = ((CarSettingsApplication) mContext.getApplicationContext()).getMyAudioZoneId();
        mBluetoothManager = LocalBluetoothManager.getInstance(context, /* onInitCallback= */ null);
        if (mBluetoothManager != null) {
            mLeBroadcastProfile =
                    mBluetoothManager.getProfileManager().getLeAudioBroadcastProfile();
            mLeBroadcastAssistantProfile =
                    mBluetoothManager.getProfileManager().getLeAudioBroadcastAssistantProfile();
        }
        mUsage = usage;
        mCachedAudioRoutes = new ArrayMap<>();
        if (isAudioRoutingEnabled()) {
            mCarAudioManager.clearAudioZoneConfigsCallback();
            mCarAudioManager.setAudioZoneConfigsChangeCallback(mContext.getMainExecutor(),
                    mAudioZoneConfigurationsChangeCallback);
        }
        updateAndNotifyAudioRouteItemsIfChanged();
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
        }
    }

    /** Is this address a valid BLE broadcast. */
    public boolean isLeBroadcast(@NonNull String address) {
        AudioRouteItem item = mCachedAudioRoutes.get(address);
        return item != null && item.getAudioRouteType() == TYPE_BLE_BROADCAST;
    }

    /** Sets the {@link AudioRoutesUpdateListener}. */
    public void setAudioRoutesUpdateListener(AudioRoutesUpdateListener listener) {
        LOG.d("[setAudioRoutesUpdateListener]");
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
        AudioRouteItem item = mCachedAudioRoutes.get(address);
        if (item == null) {
            LOG.d("[setUnicast] Cannot find an AudioRouteItem for " + address);
            return;
        }
        setUnicast(item);
    }

    /** Sets an AudioRouteItem to be the unicast target. **/
    public void setUnicast(AudioRouteItem item) {
        LOG.d("[setUnicast] " + item);
        requestAudioRouteEvent(new AudioRouteEvent(START_UNICAST, item));
    }

    private final SwitchAudioZoneConfigCallback mSwitchAudioZoneConfigCallback =
            (zoneConfig, isSuccessful) -> {
                LOG.d("[mSwitchAudioZoneConfigCallback] Audio zone switch to [%s] successful: %s"
                        .formatted(zoneConfigInfoToString(zoneConfig), isSuccessful));
                List<CarAudioZoneConfigInfo> configs = mCarAudioManager.getAudioZoneConfigInfos(
                        mAudioZone);
                LOG.d("[mSwitchAudioZoneConfigCallback] Audio zone configs: "
                        + zoneConfigInfosToString(configs));
                cancelTimer(mTimeoutTimer);
                cancelTimer(mCallbackDebouncer);
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

                    cancelTimer(mTimeoutTimer);
                    cancelTimer(mCallbackDebouncer);
                    mCallbackDebouncer = new CountDownTimer(DEBOUNCE_INTERVAL_IN_MILLIS,
                            DEBOUNCE_INTERVAL_IN_MILLIS) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                        }

                        @Override
                        public void onFinish() {
                            updateAndNotifyAudioRouteItemsIfChanged();
                            beginTimeout();
                        }
                    };
                    mCallbackDebouncer.start();
                }

                private void logConfigChange(List<CarAudioZoneConfigInfo> configs, int status) {
                    String statusName = switch (status) {
                        case CONFIG_STATUS_CHANGED -> "CONFIG_STATUS_CHANGED";
                        case CONFIG_STATUS_AUTO_SWITCHED -> "CONFIG_STATUS_AUTO_SWITCHED";
                        default -> "Status: " + status;
                    };
                    LOG.d("[onAudioZoneConfigurationsChanged]: (%s) %s".formatted(statusName,
                            zoneConfigInfosToString(configs)));
                }
            };

    private void beginTimeout() {
        if (mPendingActiveAddress != null || mPendingSelectAddress != null) {
            cancelTimer(mTimeoutTimer);
            mTimeoutTimer = new CountDownTimer(TIMEOUT_IN_MILLIS, TIMEOUT_INTERVAL_IN_MILLIS) {
                @Override
                public void onTick(long millisUntilFinished) {
                    LOG.d("[mTimeoutTimer] onTick: " + millisUntilFinished);
                }

                @Override
                public void onFinish() {
                    LOG.d("[mTimeoutTimer] timed out.");
                    cancelStartingUnicast();
                }
            };
            mTimeoutTimer.start();
        }
    }

    private void cancelTimer(CountDownTimer timer) {
        if (timer != null) {
            timer.cancel();
        }
    }

    private void cancelStartingUnicast() {
        mCachedAudioRoutes.values().stream()
                .filter(item ->
                        item.getState() == STARTING_UNICAST
                                && (Objects.equals(item.getAddress(), mPendingActiveAddress)
                                || Objects.equals(item.getAddress(),
                                mPendingSelectAddress)))
                .findFirst().ifPresent(item -> {
                    LOG.d("[cancelStartingUnicast] " + item);
                    requestAudioRouteEvent(new AudioRouteEvent(RESET, item));
                });
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
                .map(AudioDeviceAttributes::getName).collect(Collectors.toList());
        String status = Stream.<String>builder().add(config.isActive() ? "Active" : null).add(
                config.isSelected() ? "Selected" : null).add(
                config.isDefault() ? "Default" : null).build().filter(Objects::nonNull).collect(
                Collectors.joining(","));
        String deviceStr = !devices.isEmpty() ? String.join(",", devices) : "No device !!!";
        return config.getName() + " [" + status + "] : " + deviceStr;
    }

    @VisibleForTesting
    void updateAndNotifyAudioRouteItemsIfChanged() {
        LOG.d("[updateAndNotifyAudioRouteItemsIfChanged]");
        updateAndNotifyAudioRouteItems(/* forceNotify= */ false);
    }

    private void updateAndNotifyAudioRouteItems(boolean forceNotify) {
        Map<String, AudioRouteItem> newAudioRouteItems = createAudioRoutes();

        List<AudioRouteItem> selfDrivenStates = newAudioRouteItems.values().stream().filter(
                item -> item.getState().isSelfDrivenState()).toList();

        if (!selfDrivenStates.isEmpty()) {
            List<AudioRouteEvent> events = selfDrivenStates.stream().map(
                    item -> new AudioRouteEvent(CHECK_CONDITIONS, item)).toList();
            processAudioRouteEvents(events, newAudioRouteItems);
        } else {
            // If there is no self-driven state, that means there is no pending user action. In this
            // condition, we just change the selected audio route as the Car Service
            // indicates it.
            Optional<AudioRouteEvent> stopEvent = newAudioRouteItems.values().stream()
                    .filter(item -> !item.getAudioZoneConfigState().isSelected()
                            && item.getState().isActiveState())
                    .findFirst()
                    .map(item -> new AudioRouteEvent(RESET, item));

            Optional<AudioRouteEvent> startEvent = newAudioRouteItems.values().stream()
                    .filter(item -> item.getAudioZoneConfigState().isSelected()
                            && !item.getState().isActiveState())
                    .findFirst()
                    .map(item -> new AudioRouteEvent(START_UNICAST, item));

            List<AudioRouteEvent> events = Stream.of(stopEvent, startEvent).flatMap(
                    Optional::stream).toList();
            if (!events.isEmpty()) {
                processAudioRouteEvents(events, newAudioRouteItems);
            }
        }

        applyAndNotifyNewAudioRouteItems(newAudioRouteItems, forceNotify);
    }

    private void requestAudioRouteEvent(AudioRouteEvent event) {
        requestAudioRouteEvents(List.of(event));
    }

    private void requestAudioRouteEvents(List<AudioRouteEvent> events) {
        Map<String, AudioRouteItem> newAudioRouteItems = createAudioRoutes();

        processAudioRouteEvents(events, newAudioRouteItems);
        applyAndNotifyNewAudioRouteItems(newAudioRouteItems, true);
    }

    private void processAudioRouteEvents(List<AudioRouteEvent> events,
            Map<String, AudioRouteItem> newAudioRouteItems) {
        while (!events.isEmpty()) {
            LOG.d("[processAudioRouteEvents] " + events);
            events = handleEvents(events, newAudioRouteItems);
        }
    }

    private void applyAndNotifyNewAudioRouteItems(Map<String, AudioRouteItem> newAudioRouteItems,
            boolean forceNotify) {
        boolean updated = audioRouteItemsChanged(newAudioRouteItems);
        if (updated) {
            LOG.d("[applyAndNotifyNewAudioRouteItems] AudioRouteItems changed: "
                    + newAudioRouteItems);
        }

        boolean notify = forceNotify || updated;

        applyNewAudioRouteItems(newAudioRouteItems);

        if (notify && mAudioRoutesUpdateListener != null) {
            mAudioRoutesUpdateListener.onAudioRoutesUpdated(
                    mCachedAudioRoutes.values().stream().toList());
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
            AudioRouteItem audioRoute = event.audioRouteItem;
            AudioRouteItem.Command command = event.command;
            AudioRouteItem.State newState = audioRoute.getState();
            boolean requestNextEventWithNewState = false;


            boolean isBroadcastReady = audioRouteItems.values().stream().anyMatch(
                    item -> item.getAudioRouteType() == TYPE_BLE_BROADCAST
                            && item.getAudioZoneConfigState().isActive());
            boolean isBroadcasting = audioRouteItems.values().stream().anyMatch(
                    item -> item.getAudioRouteType() == TYPE_BLE_BROADCAST
                            && item.getAudioZoneConfigState().isSelected());
            boolean isLeSelected = audioRouteItems.values().stream().anyMatch(
                    item -> item.getAudioRouteType() == TYPE_BLE_HEADSET
                            && item.getAudioZoneConfigState().isSelected());

            BluetoothLeBroadcastMetadata broadcastMetadata = getCurrentBroadcast();

            switch (event.audioRouteItem.getState()) {
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
                        if (audioRoute.getAudioRouteType() == TYPE_BLE_HEADSET && isLeSelected) {
                            newState = MULTICAST_READY_UNICAST_READY;
                        } else {
                            newState = UNICAST_READY;
                        }
                    }
                    break;

                case UNICAST_READY:
                    if (command == RESET) {
                        newState = CREATED;
                        requestNextEventWithNewState = true;
                    } else if (command == START_UNICAST) {
                        if (audioRouteItems.values().stream().anyMatch(
                                item -> item.getState().isStartingState())) {
                            LOG.d("[handleEvents] <UNICAST_READY> DENIED START_UNICAST for "
                                    + audioRoute.getName());
                            break;
                        }
                        newState = STARTING_UNICAST;
                        requestNextEventWithNewState = true;
                    }
                    break;

                case STARTING_UNICAST:
                    if (command == RESET) {
                        newState = CREATED;
                        requestNextEventWithNewState = true;
                        mPendingActiveAddress = null;
                        mPendingSelectAddress = null;
                        cancelTimer(mTimeoutTimer);
                        Toast.makeText(mContext, mContext.getString(
                                R.string.audio_route_preference_connecting_failed,
                                audioRoute.getName()), Toast.LENGTH_SHORT).show();
                        break;
                    }

                    if (command != CHECK_CONDITIONS) break;

                    if (!audioRoute.getAudioZoneConfigState().isActive()) {
                        if (Objects.equals(mPendingActiveAddress, audioRoute.getAddress())) {
                            LOG.d("[handleEvents] <STARTING_UNICAST> setActive already requested "
                                    + "for " + audioRoute.getName());
                            break;
                        }
                        LOG.d("[handleEvents] <STARTING_UNICAST> Bluetooth setActive: "
                                + audioRoute.getName());
                        audioRoute.getBluetoothDevice().setActive();
                        mPendingActiveAddress = audioRoute.getAddress();
                        break;
                    }

                    mPendingActiveAddress = null;
                    cancelTimer(mTimeoutTimer);

                    if (!audioRoute.getAudioZoneConfigState().isSelected()) {
                        mLastSwitchedAddress = audioRoute.getAddress();
                        if (Objects.equals(mPendingSelectAddress,
                                audioRoute.getAddress())) {
                            LOG.d("[handleEvents] <STARTING_UNICAST> requestRouteSwitchInternal "
                                    + "already called " + "for " + audioRoute.getName());
                            break;
                        }
                        LOG.d("[handleEvents] <STARTING_UNICAST> requestRouteSwitchInternal for "
                                + audioRoute.getName());
                        requestRouteSwitchInternal(audioRoute);
                        mPendingSelectAddress = audioRoute.getAddress();
                        break;
                    }

                    mPendingSelectAddress = null;

                    // Unicast becomes active.
                    if (isBroadcastReady
                            && audioRoute.getBluetoothDeviceState().isConnectedLeAudio()) {
                        newState = MULTICAST_READY_UNICAST_ACTIVE;
                    } else {
                        newState = UNICAST_ACTIVE;
                    }

                    chainEvents.addAll(audioRouteItems.values().stream()
                            .filter(item -> item.getState() == MULTICAST_READY_UNICAST_ACTIVE)
                            .map(item -> new AudioRouteEvent(RESET, item))
                            .toList());
                    chainEvents.addAll(audioRouteItems.values().stream()
                            .filter(item -> item.getState() == UNICAST_ACTIVE)
                            .map(item -> new AudioRouteEvent(RESET, item))
                            .toList());
                    chainEvents.addAll(audioRouteItems.values().stream()
                            .filter(item -> item.getState() == UNICAST_READY)
                            .map(item -> new AudioRouteEvent(RESET, item))
                            .toList());
                    chainEvents.addAll(audioRouteItems.values().stream()
                            .filter(item -> item.getState() == MULTICAST_READY_UNICAST_READY)
                            .map(item -> new AudioRouteEvent(RESET, item))
                            .toList());
                    chainEvents.addAll(audioRouteItems.values().stream()
                            .filter(item -> item.getState() == BROADCAST_ACTIVE)
                            .map(item -> new AudioRouteEvent(RESET, item))
                            .toList());
                    // Stop broadcast if unicast starts.
                    chainEvents.addAll(audioRouteItems.values().stream()
                            .filter(item -> item.getState() == MULTICAST_ACTIVE)
                            .map(item -> new AudioRouteEvent(LEAVE_BROADCAST, item))
                            .toList());
                    break;

                case UNICAST_ACTIVE:
                    if (command == RESET) {
                        newState = CREATED;
                        requestNextEventWithNewState = true;
                    } else if (command == STOP_UNICAST) {
                        newState = UNICAST_READY;
                    } else if (command == JOIN_BROADCAST) {
                        newState = STARTING_BROADCAST;
                        requestNextEventWithNewState = true;
                    }
                    break;

                case MULTICAST_READY_UNICAST_ACTIVE:
                case STARTING_BROADCAST:
                case JOINING_BROADCAST:
                case MULTICAST_READY_UNICAST_READY:
                case MULTICAST_ACTIVE:
                case LEAVING_BROADCAST:
                case BROADCAST_READY:
                case BROADCAST_ACTIVE:
                    break;
            }
            if (requestNextEventWithNewState) {
                chainEvents.add(new AudioRouteEvent(CHECK_CONDITIONS, new AudioRouteItem.Builder(
                        audioRoute).setState(newState).build()));
            }

            if (newState != audioRoute.getState()) {
                LOG.d("[handleEvents] Transition %s : <%s> -> <%s> (%s)".formatted(
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

        return audioRoutes.values().stream().anyMatch(
                item -> item.getState() != mCachedAudioRoutes.get(item.getAddress()).getState());
    }


    private Map<String, AudioRouteItem> createAudioRoutes() {
        Map<String, AudioRouteItem> newAudioRoutes = new HashMap<>();
        List<CarAudioZoneConfigInfo> configs = mCarAudioManager.getAudioZoneConfigInfos(mAudioZone);

        // If multiple active configs for the same name exist, the selected one wins.
        Map<String, CarAudioZoneConfigInfo> activeConfigs = configs.stream().filter(
                CarAudioZoneConfigInfo::isActive).collect(
                Collectors.toMap(CarAudioZoneConfigInfo::getName, Function.identity(),
                        (a, b) -> b.isSelected() ? b : a));

        BluetoothLeBroadcastMetadata broadcastMetadata = getCurrentBroadcast();
        boolean isAudioSharingEnabled = isAudioSharingEnabled();
        AudioRouteItem.GlobalState globalState =
                new AudioRouteItem.GlobalState.Builder().setIsAudioSharingEnabled(
                        isAudioSharingEnabled).build();

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

            boolean isReceivingBroadcast = isReceivingBroadcast(device.getDevice(),
                    broadcastMetadata);

            AudioRouteItem.BluetoothDeviceState bluetoothDeviceState =
                    new AudioRouteItem.BluetoothDeviceState.Builder()
                            .setIsConnectedA2dp(device.isConnectedA2dpDevice())
                            .setIsConnectedLeAudio(device.isConnectedLeAudioDevice())
                            .setIsActiveA2dp(device.isActiveDevice(BluetoothProfile.A2DP))
                            .setIsActiveLeAudio(device.isActiveDevice(BluetoothProfile.LE_AUDIO))
                            .setIsReceivingBroadcast(isReceivingBroadcast)
                            .build();

            builder.setBluetoothDeviceState(bluetoothDeviceState);
            builder.setGlobalState(globalState);
            newAudioRoutes.put(device.getAddress(), builder.build());
        }
        return newAudioRoutes;
    }

    private void requestRouteSwitchInternal(AudioRouteItem switchedRoute) {
        if (mLastSwitchedAddress == null) {
            LOG.d("[requestRouteSwitchInternal] Failed to switch audio routing: "
                    + "mLastSwitchedAddress is null");
            return;
        }

        LOG.d("[requestRouteSwitchInternal] Trying to switch audio route to "
                + switchedRoute.getName());

        List<CarAudioZoneConfigInfo> configs = mCarAudioManager.getAudioZoneConfigInfos(mAudioZone);
        LOG.d("[requestRouteSwitchInternal] Current audio zone configs: " + zoneConfigInfosToString(
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
                        LOG.d("[requestRouteSwitchInternal] Audio route is already selected: "
                                + carAudioZoneConfigInfo.getName());
                        return;
                    }
                    try {
                        LOG.d("[requestRouteSwitchInternal] switchAudioZoneToConfig to "
                                + carAudioZoneConfigInfo.getName() + " for "
                                + switchedRoute.getName());
                        mCarAudioManager.switchAudioZoneToConfig(carAudioZoneConfigInfo,
                                ContextCompat.getMainExecutor(mContext),
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

    @VisibleForTesting
    Map<String, AudioRouteItem> getActiveRoutes() {
        return mCachedAudioRoutes;
    }
}
