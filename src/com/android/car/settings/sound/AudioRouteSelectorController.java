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

import static android.bluetooth.BluetoothAdapter.STATE_ON;

import static com.android.car.settings.bluetooth.audiosharing.BaseAudioSharingPreferenceController.isUserAudioSharingEnabled;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.car.settings.CarSettingsApplication;
import com.android.car.settings.Flags;
import com.android.car.settings.R;
import com.android.car.settings.bluetooth.audiosharing.BaseLeBroadcastAssistantCallback;
import com.android.car.settings.bluetooth.audiosharing.BaseLeBroadcastCallback;
import com.android.car.settings.common.CollapsibleSeekbarPreference;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.PreferenceController;
import com.android.car.settings.sound.audiorouting.AudioRoutePreferenceGroup;
import com.android.settingslib.bluetooth.LeAudioProfile;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controls the audio destination selection.
 */
public class AudioRouteSelectorController extends PreferenceController<AudioRoutePreferenceGroup>
        implements CollapsibleSeekbarPreference.CollapsibleSeekbarUpdateListener {
    private static final Logger LOG = new Logger(AudioRouteSelectorController.class);
    private AudioRoutesManager mRouteManager;
    private LocalBluetoothManager mBtManager;
    private LeAudioProfile mLeAudioProfile;
    private LocalBluetoothLeBroadcast mLeBroadcastProfile;
    private LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistantProfile;
    // Devices that are waiting for a broadcast session to finish starting-up to then be added to
    // the active broad cast session.
    private final List<BluetoothDevice> mPendingDeviceAdds = new ArrayList<>();
    // Device preferences that user have previously clicked on, indicating last user input
    // TODO: (b/451455063) These variable are used for tracking previous user actions in some states
    //  and we should manage them more clearly using state machine enum for debugging and testing.
    //  For example, mConnectingBroadcastDevices should only contain a single value if
    //  state is STATE_BLE_STARTING. Otherwise, we can just directly add it to the active session.
    private final Set<String> mConnectingUnicastDevices = new HashSet<>();
    private final Set<String> mConnectingBroadcastDevices = new HashSet<>();
    private final Set<String> mDisconnectingBroadcastDevices = new HashSet<>();

    /**
     * TODO:(b/451455063) Use "state machine" variable mCurrentState, it should be used in the
     *  callbacks as well as in the UI to help with debugging and program logic.
     */
    AudioRoutesManager.AudioZoneConfigUpdateListener mRouteManagerCallback =
            new AudioRoutesManager.AudioZoneConfigUpdateListener() {
                @Override
                public void onAudioZoneConfigUpdated(boolean routesUpdated) {
                    if (isAudioSharingEnabled()) {
                        String address = mRouteManager.getBroadcastAddress();
                        if (address != null && !mPendingDeviceAdds.isEmpty()) {
                            // there are devices waiting to be added to the BLE broadcast
                            if (address.equals(mRouteManager.getOutputAddress())) {
                                // the new active audio zone is LE Broadcast
                                addPendingDevices(getCurrentBroadcast());
                            } else {
                                // When LeBroadcastProfile.startBroadcast is called, an audio route
                                // for BLE will be created, then trigger onAudioZoneConfigUpdated.
                                mRouteManager.requestRouteSwitch(address);
                            }
                        }
                        if (isOutputDeviceSingularlyReceivingBroadcast()) {
                            stopBroadcast();
                        }
                    }
                    if (routesUpdated) {
                        updatePreferenceOptions();
                    } else {
                        updateChildPreferences();
                    }
                }

                @Override
                public void onAudioZoneSwitch() {
                    if (isAudioSharingEnabled()) {
                        if (mRouteManager.isOutputLeBroadcast()) {
                            // the new active audio zone is LE Broadcast
                            if (!mPendingDeviceAdds.isEmpty()) {
                                // and there are devices waiting to be added to the BLE broadcast.
                                addPendingDevices(getCurrentBroadcast());
                            }
                        }
                        if (isOutputDeviceSingularlyReceivingBroadcast()) {
                            stopBroadcast();
                        }
                    }
                    updateChildPreferences();
                }

                @Override
                public void onSwitchRequested(String address, boolean success) {
                    LOG.d("Audio route switch to [%s] executed with response %s"
                            .formatted(mRouteManager.getDeviceName(address), success));
                    if (!success && mConnectingUnicastDevices.contains(address)) {
                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(() -> {
                            // TODO(b/451453306): use a count down latch to handle retries when
                            //  route switching fails for the first few tries (e.g when route is not
                            //  available yet.
                            //  We should also add a state for when a recent switch request has
                            //  failed, with a text saying "Could not connect to device."
                            LOG.d("Retry to audio route switch: " + address);
                            mRouteManager.requestRouteSwitch(address);
                        }, /* delayMillis= */ 200);
                    }
                }
            };

    BaseLeBroadcastCallback mLeBroadcastCallback = new BaseLeBroadcastCallback() {
        @Override
        public void onBroadcastStarted(int reason, int broadcastId) {
            LOG.d("ble broadcast started");
            String address = mRouteManager.getBroadcastAddress();
            if (!mRouteManager.isOutputLeBroadcast()) {
                LOG.d("broadcast started but active route is not broadcast, "
                        + "requesting switch to audio route to broadcast");
                mRouteManager.requestRouteSwitch(address);
            }
        }
    };

    BaseLeBroadcastAssistantCallback mBaseLeBroadcastAssistantCallback =
            new BaseLeBroadcastAssistantCallback() {
                @Override
                public void onSourceAdded(@NonNull BluetoothDevice sink, int sourceId,
                        int reason) {
                    LOG.i("source added to broadcast" + sink.getName());
                    updateState(getPreference());
                }

                @Override
                public void onSourceRemoved(@NonNull BluetoothDevice sink, int sourceId,
                        int reason) {
                    LOG.d("source removed from broadcast: " + sink.getName());
                    if (isBroadcasting() && hasNoActiveSinkDevice(sourceId)) {
                        // all devices have exited the broadcast, stopping broadcast and let
                        // the active device be actively switched to default output
                        stopBroadcast();
                    }
                    updateState(getPreference());
                }
            };

    public AudioRouteSelectorController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mRouteManager = new AudioRoutesManager(context,
                context.getResources().getInteger(R.integer.audio_route_selector_usage));
        mRouteManager.setListener(mRouteManagerCallback);
        mBtManager = LocalBluetoothManager.getInstance(context, /* onInitCallback= */ null);
        if (mBtManager != null) {
            mLeAudioProfile = mBtManager.getProfileManager().getLeAudioProfile();
            mLeBroadcastProfile = mBtManager.getProfileManager().getLeAudioBroadcastProfile();
            mLeBroadcastAssistantProfile = mBtManager.getProfileManager()
                    .getLeAudioBroadcastAssistantProfile();
            if (mLeBroadcastProfile != null && mLeBroadcastAssistantProfile != null) {
                mLeBroadcastProfile.registerServiceCallBack(context.getMainExecutor(),
                        mLeBroadcastCallback);
                mLeBroadcastAssistantProfile.registerServiceCallBack(getContext().getMainExecutor(),
                        mBaseLeBroadcastAssistantCallback);
            }
        }
    }

    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();
        updatePreferenceOptions();
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        if (Flags.newAudioRoutingUi()) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    private void updatePreferenceOptions() {
        if (getAvailabilityStatus() == CONDITIONALLY_UNAVAILABLE
                || !mRouteManager.isAudioRoutingEnabled()) {
            return;
        }
        LOG.d("Updating preferences options based on updated audio routes");
        getPreference().removeAll();
        for (String address: mRouteManager.getAudioRouteList()) {
            if (mRouteManager.isLeBroadcast(address)) {
                // LE audio broadcast address does not have an associated audio device
                // and show not be directly shown to user
                continue;
            }
            AudioRouteItem item = mRouteManager.getRouteItem(address);
            if (item != null) {
                AudioRoutePreference pref = new AudioRoutePreference(getContext());
                pref.setListener(this);
                pref.setKey(item.getAddress());
                pref.setTitle(mRouteManager.getDeviceName(item.getAddress()));
                if (isAudioSharingEnabled() && isLeAudioBluetoothDevice(item.getAddress())) {
                    // enable multi-select only on BLE devices and by default, show the multi-select
                    // action button unless hidden later on in the state machine
                    pref.setMultiSelectAvailable(true);
                    pref.setShowActionButton(true);
                }
                getPreference().addPreference(pref);
            }
        }
        updateChildPreferences();
    }

    private boolean isLeAudioBluetoothDevice(@NonNull String address) {
        AudioRouteItem item = mRouteManager.getRouteItem(address);
        return item != null && item.getBluetoothDevice() != null
                && mLeAudioProfile != null
                && mLeAudioProfile.isEnabled(item.getBluetoothDevice().getDevice());
    }

    @Override
    protected void updateState(AudioRoutePreferenceGroup preference) {
        super.updateState(preference);
        updateChildPreferences();
    }

    @Override
    protected void onDestroyInternal() {
        mRouteManager.tearDown();
    }

    private void addConnectingUnicastDevice(String key) {
        mConnectingUnicastDevices.clear();
        mConnectingUnicastDevices.add(key);
    }

    private void addConnectingBroadcastDevice(String key) {
        mConnectingBroadcastDevices.clear();
        mConnectingBroadcastDevices.add(key);
    }

    private void addDisconnectingBroadcastDevice(String key) {
        mDisconnectingBroadcastDevices.clear();
        mDisconnectingBroadcastDevices.add(key);
    }

    private void addPendingDevices(@Nullable BluetoothLeBroadcastMetadata metadata) {
        if (metadata == null || mPendingDeviceAdds.isEmpty()) {
            return;
        }
        for (BluetoothDevice device : mPendingDeviceAdds) {
            LOG.d("Request adding pending device to broadcast: " + device.getName());
            mLeBroadcastAssistantProfile.addSource(device, /* metadata= */ metadata,
                    /* isGroupOp= */ true);
        }
        mPendingDeviceAdds.clear();
    }

    @Nullable
    private AudioRoutePreference getChildPreference(@NonNull String key) {
        for (AudioRoutePreference pref : getChildPreferences()) {
            if (key.equals(pref.getKey())) return pref;
        }
        return null;
    }

    @NonNull
    private List<AudioRoutePreference> getChildPreferences() {
        List<AudioRoutePreference> children = new ArrayList<>();
        for (int i = 0; i < getPreference().getPreferenceCount(); i++) {
            children.add((AudioRoutePreference) getPreference().getPreference(i));
        }
        return children;
    }

    /**
     * TODO(b/451455063) Handle the state machine case where a user makes multiple clicks on the UI
     *  when a previous connection is pending. After implementing the state machine, we can make
     *  this method ignore certain user actions during some states. Otherwise, we may see a flicker
     *  in the UI and audio output jumping between several devices.
     */
    @Override
    public void onSelected(String key, int currentState) {
        if (mRouteManager.isOutputAddress(key) || getChildPreference(key) == null
                && !isReceivingBroadcast(key, getCurrentBroadcast())) {
            // no-op, should already be shown as the unicast active
        } else {
            mRouteManager.requestRouteSwitch(key);
            addConnectingUnicastDevice(key);
            if (isBroadcasting()) {
                stopBroadcast();
            }
        }
        updateChildPreferences();
    }

    /**
     * TODO(b/451455063) Handle the state machine case where a user makes multiple clicks on the UI
     *  when a previous connection is pending. After implementing the state machine, we can make
     *  this method ignore certain user actions during some states. Otherwise, we may see a flicker
     *  in the UI and audio output jumping between several devices.
     */
    @Override
    public void onMultiSelected(String key, int currentState) {
        if (!isAudioSharingEnabled() || getChildPreference(key) == null
                || !isLeAudioBluetoothDevice(key)) {
            updateChildPreferences();
            return;
        }
        BluetoothDevice device = mBtManager.getBluetoothAdapter().getRemoteDevice(key);
        String deviceName = device.getName();
        if (!isBroadcasting()) {
            // start a broadcast and queue the adding of device until an audio route is available.
            LOG.d("Starting broadcast and adding device as pending: " + deviceName);
            startBroadcast();
            addConnectingBroadcastDevice(key);
            mPendingDeviceAdds.add(device);
        } else if (!isReceivingBroadcast(device.getAddress(), getCurrentBroadcast())) {
            // if broadcast is in session but device is connected, add the device as a sink
            LOG.d("Adding selected device to broadcast: %s" + deviceName);
            addConnectingBroadcastDevice(key);
            mLeBroadcastAssistantProfile.addSource(device, /* metadata= */ getCurrentBroadcast(),
                    /* isGroupOp= */ true);
        } else {
            // user is trying to remove the device from listening session
            if (getReceivingDevices(getCurrentBroadcast()).size() == 1) {
                // request route switch first before disconnecting
                LOG.d("Requesting route switch and disconnecting from broadcast: " + deviceName);
                addDisconnectingBroadcastDevice(key);
                mRouteManager.requestRouteSwitch(key);
            } else {
                // disconnect directly
                LOG.d("Removing device from broadcast: %s" + deviceName);
                addDisconnectingBroadcastDevice(key);
                removeSourceIfDeviceListening(device);
            }
        }
        updateChildPreferences();
    }

    /**
     * Updates the child preferences according to state machine state.
     * TODO(b/451448848) Update program logic to show join broadcast only on preferences
     *  that are not the active device AND has BLE capability.
     */
    private void updateChildPreferences() {
        String activeAddress = mRouteManager.getOutputAddress();
        mConnectingUnicastDevices.remove(activeAddress);
        LOG.d("Output audio device: %s".formatted(mRouteManager.getDeviceName(activeAddress)));
        for (AudioRoutePreference pref : getChildPreferences()) {
            String address = pref.getKey();
            if (address.equals(activeAddress) && (mConnectingBroadcastDevices.isEmpty()
                    || mConnectingBroadcastDevices.contains(address))) {
                // If the active address it not in the connecting broadcast device list, and the
                // list is not empty, then it must be an intermediary state where the device output
                // has been changed into default.
                // BT active -> [BT inactive, auto switch to main] -> BLE active
                // Let it fall through and set as inactive to avoid flickering during transition
                pref.setUnicastActive(isLeAudioBluetoothDevice(address));
                mDisconnectingBroadcastDevices.remove(address);
            } else if (isAudioSharingEnabled() && isLeAudioBluetoothDevice(address)) {
                // Multi-casting UI states for BLE enabled devices.
                if (isReceivingBroadcast(address, getCurrentBroadcast())) {
                    // Case 1: Device is receiving BLE broadcast
                    mConnectingBroadcastDevices.remove(address);
                    pref.setMulticastActive();
                } else if (activeAddress != null && activeAddress.equals(
                        mRouteManager.getBroadcastAddress())) {
                    // Case 2: Device is not currently receiving, but BLE is the active output
                    pref.setMulticastInactive();
                } else {
                    // fall through: no active BLE, set device as unicast inactive
                    pref.setUnicastInactive();
                    mDisconnectingBroadcastDevices.remove(address);
                }
            } else {
                // fall through: by default, set all other device as unicast inactive
                pref.setUnicastInactive();
                mDisconnectingBroadcastDevices.remove(address);
            }
            // Add summary string indicating last user input in the UI
            if (mConnectingUnicastDevices.contains(address)) {
                pref.setSummary(R.string.audio_route_preference_connecting_device);
            } else if (mConnectingBroadcastDevices.contains(address)) {
                pref.setSummary(R.string.audio_route_preference_connecting_broadcast);
            } else if (mDisconnectingBroadcastDevices.contains(address)) {
                pref.setSummary(R.string.audio_route_preference_leaving_broadcast);
            }
        }
    }

    private void startBroadcast() {
        mLeBroadcastProfile.stopLatestBroadcast();
        mLeBroadcastProfile.startBroadcast(CarSettingsApplication.CAR_SETTINGS_PACKAGE_NAME,
                /* language= */ null);
        mLeBroadcastProfile.setBroadcastCode(new byte[0]);
    }

    private void stopBroadcast() {
        LOG.d("Stopping current BLE broadcast");
        mLeBroadcastProfile.stopLatestBroadcast();
    }

    /**
     * TODO (b/451450273) consider moving the BLE related methods into a static method elsewhere.
     *  Mutating method should NOT check if BLE profiles are non-null, because they should not have
     *  been called in the first place and should have been guarded by
     *  {@link #isAudioSharingEnabled()} in this class.
     *
     * Return {@code true} if there is only one device receiving broadcast, and that device is
     * also the active device in the non BLE audio stream.
     * This condition means that all other BLE devices have exited the broadcast, and the
     * state machine has moved the active device from BLE to Unicast, but there is still a broadcast
     * session in the background.
     * The state machine should handle clean-up appropriately if this method returns true.
     */
    private boolean isOutputDeviceSingularlyReceivingBroadcast() {
        List<BluetoothDevice> devices = getReceivingDevices(getCurrentBroadcast());
        return devices.size() == 1 && devices.getFirst().getAddress().equals(
                mRouteManager.getOutputAddress());
    }

    /**
     * Returns true if the broadcast with sourceId does not have any actively listening device.
     */
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

    @NonNull
    private List<BluetoothDevice> getReceivingDevices(BluetoothLeBroadcastMetadata metadata) {
        return mLeBroadcastAssistantProfile.getAllConnectedDevices().stream().filter(
                device -> isReceivingBroadcast(device, metadata)).toList();
    }

    private boolean isReceivingBroadcast(BluetoothDevice device,
            BluetoothLeBroadcastMetadata metadata) {
        if (metadata == null || device == null) {
            return false;
        }
        for (BluetoothLeBroadcastReceiveState state :
                mLeBroadcastAssistantProfile.getAllSources(device)) {
            if (state.getSourceDevice().equals(metadata.getSourceDevice())) {
                return true;
            }
        }
        return false;
    }

    private boolean isReceivingBroadcast(String address,
            BluetoothLeBroadcastMetadata metadata) {
        AudioRouteItem item = mRouteManager.getRouteItem(address);
        if (item == null || item.getBluetoothDevice() == null) {
            return false;
        }
        return isReceivingBroadcast(item.getBluetoothDevice().getDevice(), metadata);
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

    @Nullable
    private BluetoothLeBroadcastMetadata getCurrentBroadcast() {
        List<BluetoothLeBroadcastMetadata> metadata = mLeBroadcastProfile.getAllBroadcastMetadata();
        if (metadata.isEmpty()) {
            return null;
        }
        return metadata.getFirst();
    }

    /**
     * @return {@code true} if bluetooth is turned on.
     */
    public boolean isBluetoothStateOn() {
        return mBtManager != null && mBtManager.getBluetoothAdapter() != null
                && mBtManager.getBluetoothAdapter().getBluetoothState() == STATE_ON;
    }

    /**
     * @return {@code true} if all bluetooth profiles necessary for LE bluetooth broadcast state
     * management in Settings is available, {@code false} otherwise.
     */
    public boolean isBroadcastAvailable() {
        return isBluetoothStateOn() && mBtManager != null && mLeAudioProfile != null
                && mLeBroadcastProfile != null && mLeBroadcastAssistantProfile != null;

    }

    /**
     * @return {@code true} if there is an active broadcast session, {@code false} otherwise.
     */
    public boolean isBroadcasting() {
        return isBroadcastAvailable() && !mLeBroadcastProfile.getAllBroadcastMetadata().isEmpty();
    }

    private boolean isAudioSharingEnabled() {
        return isUserAudioSharingEnabled(getContext()) && mLeBroadcastCallback != null
                && mLeBroadcastAssistantProfile != null;
    }

    @Override
    protected Class<AudioRoutePreferenceGroup> getPreferenceType() {
        return AudioRoutePreferenceGroup.class;
    }

    @VisibleForTesting
    void setAudioRoutesManager(AudioRoutesManager audioRoutesManager) {
        mRouteManager = audioRoutesManager;
    }
}
