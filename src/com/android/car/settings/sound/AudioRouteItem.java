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

import static android.media.AudioDeviceInfo.TYPE_BLE_HEADSET;
import static android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP;
import static android.media.AudioDeviceInfo.TYPE_UNKNOWN;

import android.annotation.SuppressLint;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;

import androidx.annotation.Nullable;

import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: b/451450273 - Replace with Java record or AutoBuilder.
/**
 * A class to encapsulate audio route information.
 *
 * <p>This class holds all the relevant information about an audio route, including its name,
 * address, type, and current state. It also contains nested classes to represent the state of
 * various components related to the audio route, such as the audio zone configuration, the
 * bluetooth device, and the global state.
 *
 * <p>The class provides a {@link Builder} to facilitate the creation of {@link AudioRouteItem}
 * instances.
 */
public class AudioRouteItem {

    /** An enum to represent the state of an audio route. */
    public enum State {
        /** The state is not specified. */
        UNSPECIFIED(0),
        /** The audio route has been created. */
        CREATED(1),
        /** The audio route is ready to be selected for unicast. */
        UNICAST_READY(2),
        /** The audio route is selected for unicast. */
        UNICAST_ACTIVE(3),
        /** The audio route is ready to be selected for unicast or multicast. */
        MULTICAST_READY_UNICAST_READY(4),
        /**
         * The audio route is selected for unicast while ready to be selected (switched) for
         * multicast.
         */
        MULTICAST_READY_UNICAST_ACTIVE(5),
        /** The audio route is selected for multicast (LE Audio sharing). */
        MULTICAST_ACTIVE(6),
        /** The audio route is in the process of starting unicast streaming. */
        STARTING_UNICAST(7),
        /** The audio route is in the process of joining a broadcast. */
        JOINING_BROADCAST(8),
        /** The audio route is in the process of starting a broadcast. */
        STARTING_BROADCAST(9),
        /** The audio route is in the process of leaving a broadcast. */
        LEAVING_BROADCAST(10),
        /** The audio route is ready for broadcast (multicast) streaming. */
        BROADCAST_READY(11),
        /** The audio route is actively broadcast (multicast) streaming. */
        BROADCAST_ACTIVE(12);


        private final int mValue;

        State(int value) {
            this.mValue = value;
        }

        /** Gets the integer value of the state. */
        public int getValue() {
            return mValue;
        }

        /** Checks if the state is a self-driven state. */
        public boolean isSelfDrivenState() {
            return switch (this) {
                case CREATED, STARTING_UNICAST, STARTING_BROADCAST, JOINING_BROADCAST,
                     LEAVING_BROADCAST -> true;
                default -> false;
            };
        }

        /** Checks if the state is an active state. */
        public boolean isActiveState() {
            return switch (this) {
                case UNICAST_ACTIVE, MULTICAST_READY_UNICAST_ACTIVE, BROADCAST_ACTIVE -> true;
                default -> false;
            };
        }

        /** Checks if the state is a starting state. */
        public boolean isStartingState() {
            return switch (this) {
                case STARTING_UNICAST, JOINING_BROADCAST -> true;
                default -> false;
            };
        }

        /** Checks if the state is a broadcast state. */
        public boolean isBroadcastState() {
            return switch (this) {
                case BROADCAST_READY, BROADCAST_ACTIVE -> true;
                default -> false;
            };
        }
    }

    /** An enum to represent the commands that can trigger a state transition. */
    public enum Command {
        /** Command to check the conditions for self-driven states. */
        CHECK_CONDITIONS,
        /** Command to start unicast streaming. */
        START_UNICAST,
        /** Command to stop unicast streaming. */
        STOP_UNICAST,
        /** Command to join a broadcast. */
        JOIN_BROADCAST,
        /** Command to leave a broadcast. */
        LEAVE_BROADCAST,
        /** Command to reset the state. */
        RESET,
        /** Command to cancel STARTING_UNICAST state. */
        CANCEL_STARTING_UNICAST,
    }

    /** A data class to encapsulate the volume state of an audio route. */
    public static final class VolumeState {
        private final int mMinVolume;
        private final int mMaxVolume;
        private final int mCurrentVolume;
        private final boolean mUseVolumeControlProfile;

        private VolumeState(Builder builder) {
            mMinVolume = builder.mMinVolume;
            mMaxVolume = builder.mMaxVolume;
            mCurrentVolume = builder.mCurrentVolume;
            mUseVolumeControlProfile = builder.mUseVolumeControlProfile;
        }

        /** Gets the minimum volume. */
        public int getMinVolume() {
            return mMinVolume;
        }

        /** Gets the maximum volume. */
        public int getMaxVolume() {
            return mMaxVolume;
        }

        /** Gets the current volume. */
        public int getCurrentVolume() {
            return mCurrentVolume;
        }

        /** Checks if the volume control profile should be used. */
        public boolean useVolumeControlProfile() {
            return mUseVolumeControlProfile;
        }

        @Override
        public String toString() {
            return "(Min: %d, Max: %d, Current: %d, UseVolumeControlProfile: %b)".formatted(
                    mMinVolume, mMaxVolume, mCurrentVolume, mUseVolumeControlProfile);
        }

        /** Creates a new {@link Builder} from this {@link VolumeState}. */
        public Builder toBuilder() {
            return new Builder(this);
        }

        /** Builder for {@link VolumeState}. */
        public static class Builder {
            private int mMinVolume = 0;
            private int mMaxVolume = 0;
            private int mCurrentVolume = 0;
            private boolean mUseVolumeControlProfile = false;

            public Builder() {}

            /** Constructor for {@link VolumeState.Builder} from a {@link VolumeState}. */
            public Builder(VolumeState volumeState) {
                mMinVolume = volumeState.getMinVolume();
                mMaxVolume = volumeState.getMaxVolume();
                mCurrentVolume = volumeState.getCurrentVolume();
                mUseVolumeControlProfile = volumeState.useVolumeControlProfile();
            }

            /** Sets the minimum volume. */
            public Builder setMinVolume(int minVolume) {
                mMinVolume = minVolume;
                return this;
            }

            /** Sets the maximum volume. */
            public Builder setMaxVolume(int maxVolume) {
                mMaxVolume = maxVolume;
                return this;
            }

            /** Sets the current volume. */
            public Builder setCurrentVolume(int currentVolume) {
                mCurrentVolume = currentVolume;
                return this;
            }

            /** Sets whether to use the volume control profile. */
            public Builder setUseVolumeControlProfile(boolean useVolumeControlProfile) {
                mUseVolumeControlProfile = useVolumeControlProfile;
                return this;
            }

            /** Builds the {@link VolumeState} instance. */
            public VolumeState build() {
                return new VolumeState(this);
            }
        }
    }

    private final String mName;
    private final String mAddress;
    private final @AudioDeviceInfo.AudioDeviceType int mAudioDeviceType;
    private final AudioZoneConfigState mAudioZoneConfigState;
    private final BluetoothDeviceState mBluetoothDeviceState;
    private final GlobalState mGlobalState;
    private final State mState;
    private final VolumeState mVolumeState;
    // TODO: b/451450273 - Remove mBluetoothDevice;
    @Nullable private final CachedBluetoothDevice mBluetoothDevice;

    /** A data class to encapsulate the state of an audio zone configuration. */
    public static final class AudioZoneConfigState {
        private final boolean mIsActive;
        private final boolean mIsSelected;

        private AudioZoneConfigState(Builder builder) {
            this.mIsActive = builder.mIsActive;
            this.mIsSelected = builder.mIsSelected;
        }

        /** Checks if the audio zone is active. */
        public boolean isActive() {
            return mIsActive;
        }

        /** Checks if the audio zone is selected. */
        public boolean isSelected() {
            return mIsSelected;
        }

        @Override
        public String toString() {
            String status = Stream.<String>builder()
                    .add(isActive() ? "Active" : null)
                    .add(isSelected() ? "Selected" : null)
                    .build()
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(","));
            return "(" + status + ")";
        }

        /** Builder for {@link AudioZoneConfigState}. */
        public static class Builder {
            private boolean mIsActive = false;
            private boolean mIsSelected = false;

            /** Sets whether the audio zone is active. */
            public Builder setIsActive(boolean isActive) {
                mIsActive = isActive;
                return this;
            }

            /** Sets whether the audio zone is selected. */
            public Builder setIsSelected(boolean isSelected) {
                mIsSelected = isSelected;
                return this;
            }

            /** Builds the {@link AudioZoneConfigState} instance. */
            public AudioZoneConfigState build() {
                return new AudioZoneConfigState(this);
            }
        }
    }

    /** A data class to encapsulate the state of a bluetooth device. */
    public static final class BluetoothDeviceState {
        private final boolean mIsActiveA2dp;
        private final boolean mIsActiveLeAudio;
        private final boolean mIsConnectedA2dp;
        private final boolean mIsConnectedLeAudio;
        private final boolean mIsReceivingBroadcast;

        private BluetoothDeviceState(Builder builder) {
            this.mIsActiveA2dp = builder.mIsActiveA2dp;
            this.mIsActiveLeAudio = builder.mIsActiveLeAudio;
            this.mIsConnectedA2dp = builder.mIsConnectedA2dp;
            this.mIsConnectedLeAudio = builder.mIsConnectedLeAudio;
            this.mIsReceivingBroadcast = builder.mIsReceivingBroadcast;
        }

        /** Checks if the A2DP profile is active. */
        public boolean isActiveA2dp() {
            return mIsActiveA2dp;
        }

        /** Checks if the LE Audio profile is active. */
        public boolean isActiveLeAudio() {
            return mIsActiveLeAudio;
        }

        /** Checks if the A2DP profile is connected. */
        public boolean isConnectedA2dp() {
            return mIsConnectedA2dp;
        }

        /** Checks if the LE Audio profile is connected. */
        public boolean isConnectedLeAudio() {
            return mIsConnectedLeAudio;
        }

        /** Checks if the device is receiving the broadcast. */
        public boolean isReceivingBroadcast() {
            return mIsReceivingBroadcast;
        }

        @Override
        public String toString() {
            String status = Stream.<String>builder()
                    .add(isConnectedA2dp() ? "A2DP Connected" : null)
                    .add(isActiveA2dp() ? "A2DP Active" : null)
                    .add(isConnectedLeAudio() ? "LE Connected" : null)
                    .add(isActiveLeAudio() ? "LE Active" : null)
                    .add(isReceivingBroadcast() ? "Receiving Broadcast" : null)
                    .build()
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(","));
            return "(" + status + ")";
        }

        /** Builder for {@link BluetoothDeviceState}. */
        public static class Builder {
            private boolean mIsActiveA2dp = false;
            private boolean mIsActiveLeAudio = false;
            private boolean mIsConnectedA2dp = false;
            private boolean mIsConnectedLeAudio = false;
            private boolean mIsReceivingBroadcast = false;

            /** Sets whether the A2DP profile is active. */
            public Builder setIsActiveA2dp(boolean isActiveA2dp) {
                mIsActiveA2dp = isActiveA2dp;
                return this;
            }

            /** Sets whether the LE Audio profile is active. */
            public Builder setIsActiveLeAudio(boolean isActiveLeAudio) {
                mIsActiveLeAudio = isActiveLeAudio;
                return this;
            }

            /** Sets whether the A2DP profile is connected. */
            public Builder setIsConnectedA2dp(boolean isConnectedA2dp) {
                mIsConnectedA2dp = isConnectedA2dp;
                return this;
            }

            /** Sets whether the LE Audio profile is connected. */
            public Builder setIsConnectedLeAudio(boolean isConnectedLeAudio) {
                mIsConnectedLeAudio = isConnectedLeAudio;
                return this;
            }

            /** Sets whether the device is receiving the broadcast. */
            public Builder setIsReceivingBroadcast(boolean isReceivingBroadcast) {
                mIsReceivingBroadcast = isReceivingBroadcast;
                return this;
            }

            /** Builds the {@link BluetoothDeviceState} instance. */
            public BluetoothDeviceState build() {
                return new BluetoothDeviceState(this);
            }
        }
    }

    // TODO: b/451450273 - Rename to GlobalStates.
    /** A data class to encapsulate the global state. */
    public static final class GlobalState {
        private final boolean mIsAudioSharingEnabled;

        private GlobalState(Builder builder) {
            this.mIsAudioSharingEnabled = builder.mIsAudioSharingEnabled;
        }

        /** Checks if audio sharing is enabled. */
        public boolean isAudioSharingEnabled() {
            return mIsAudioSharingEnabled;
        }

        @Override
        public String toString() {
            String status = Stream.<String>builder()
                    .add(isAudioSharingEnabled() ? "Audio-sharing" : null)
                    .build()
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(","));
            return "(" + status + ")";
        }

        /** Builder for {@link GlobalState}. */
        public static class Builder {
            private boolean mIsAudioSharingEnabled = false;

            /** Sets whether audio sharing is enabled. */
            public Builder setIsAudioSharingEnabled(boolean isAudioSharingEnabled) {
                mIsAudioSharingEnabled = isAudioSharingEnabled;
                return this;
            }

            /** Builds the {@link GlobalState} instance. */
            public GlobalState build() {
                return new GlobalState(this);
            }
        }
    }

    private AudioRouteItem(Builder builder) {
        mName = builder.mName;
        mAddress = builder.mAddress;
        mAudioDeviceType = builder.mAudioDeviceType;
        mBluetoothDevice = builder.mBluetoothDevice;
        mAudioZoneConfigState = builder.mAudioZoneConfigState;
        mBluetoothDeviceState = builder.mBluetoothDeviceState;
        mGlobalState = builder.mGlobalState;
        mState = builder.mState;
        mVolumeState = builder.mVolumeState;
    }

    /** Creates a new {@link Builder} from this {@link AudioRouteItem}. */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /** Gets the name of the audio route. */
    public String getName() {
        return mName;
    }

    /** Gets the address of the audio route. */
    public String getAddress() {
        return mAddress;
    }

    /** Gets the audio device type of the route. */
    public @AudioDeviceInfo.AudioDeviceType int getAudioRouteType() {
        return mAudioDeviceType;
    }

    /** Gets the cached bluetooth device associated with this audio route. */
    @Nullable
    public CachedBluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }

    /** Gets the state of the audio zone configuration. */
    public AudioZoneConfigState getAudioZoneConfigState() {
        return mAudioZoneConfigState;
    }

    /** Gets the state of the bluetooth device. */
    public BluetoothDeviceState getBluetoothDeviceState() {
        return mBluetoothDeviceState;
    }

    /** Gets the global state. */
    public GlobalState getGlobalState() {
        return mGlobalState;
    }

    /** Gets the state of the audio route. */
    public State getState() {
        return mState;
    }

    /** Gets the volume state of the audio route. */
    public VolumeState getVolumeState() {
        return mVolumeState;
    }

    /** Checks if the audio route is a Bluetooth audio route. */
    public boolean isBluetoothAudioRoute() {
        return mAudioDeviceType == TYPE_BLE_HEADSET || mAudioDeviceType == TYPE_BLUETOOTH_A2DP;
    }


    @Override
    public String toString() {
        return ("%s (%s) <%s> Type=%d, "
                + "AudioZoneConfigState=%s, BluetoothDeviceState=%s, GlobalState=%s, "
                + "VolumeState=%s}")
                .formatted(
                        mName,
                        mAddress,
                        mState,
                        mAudioDeviceType,
                        mAudioZoneConfigState,
                        mBluetoothDeviceState,
                        mGlobalState,
                        mVolumeState
                );
    }

    /** Builder for {@link AudioRouteItem}. */
    public static class Builder {
        private String mName;
        private String mAddress;
        private @AudioDeviceInfo.AudioDeviceType int mAudioDeviceType;
        @Nullable private CachedBluetoothDevice mBluetoothDevice;
        private AudioZoneConfigState mAudioZoneConfigState =
                new AudioZoneConfigState.Builder().build();
        private BluetoothDeviceState mBluetoothDeviceState =
                new BluetoothDeviceState.Builder().build();
        private GlobalState mGlobalState = new GlobalState.Builder().build();
        private State mState = State.CREATED;
        private VolumeState mVolumeState = new VolumeState.Builder().build();

        /** Constructor for {@link AudioRouteItem.Builder} from a {@link CachedBluetoothDevice}. */
        @SuppressLint("MissingPermission")
        public Builder(CachedBluetoothDevice bluetoothDevice) {
            mName = bluetoothDevice.getName();
            mAddress = bluetoothDevice.getAddress();
            mAudioDeviceType = getBluetoothAudioType(bluetoothDevice);

            mBluetoothDevice = bluetoothDevice;
        }

        private int getBluetoothAudioType(CachedBluetoothDevice bluetoothDevice) {
            if (bluetoothDevice.isConnectedLeAudioDevice()) {
                return TYPE_BLE_HEADSET;
            } else if (bluetoothDevice.isConnectedA2dpDevice()) {
                return TYPE_BLUETOOTH_A2DP;
            }
            return TYPE_UNKNOWN;
        }

        /** Constructor for {@link AudioRouteItem.Builder} from a {@link AudioDeviceAttributes}. */
        public Builder(AudioDeviceAttributes audioDeviceAttributes) {
            mName = audioDeviceAttributes.getName();
            mAddress = audioDeviceAttributes.getAddress();
            mAudioDeviceType = audioDeviceAttributes.getType();
        }

        /** Constructor for {@link AudioRouteItem.Builder} from a {@link AudioRouteItem}. */
        public Builder(AudioRouteItem audioRouteItem) {
            mName = audioRouteItem.getName();
            mAddress = audioRouteItem.getAddress();
            mAudioDeviceType = audioRouteItem.getAudioRouteType();
            mBluetoothDevice = audioRouteItem.getBluetoothDevice();
            mAudioZoneConfigState = audioRouteItem.getAudioZoneConfigState();
            mBluetoothDeviceState = audioRouteItem.getBluetoothDeviceState();
            mGlobalState = audioRouteItem.getGlobalState();
            mState = audioRouteItem.getState();
            mVolumeState = audioRouteItem.getVolumeState();
        }

        /** Sets the name of the audio route. */
        public Builder setName(String name) {
            mName = name;
            return this;
        }

        /** Sets the address of the audio route. */
        public Builder setAddress(String address) {
            mAddress = address;
            return this;
        }

        /** Sets the audio device type. */
        public Builder setAudioDeviceType(@AudioDeviceInfo.AudioDeviceType int audioDeviceType) {
            mAudioDeviceType = audioDeviceType;
            return this;
        }

        /** Sets the cached bluetooth device. */
        public Builder setBluetoothDevice(@Nullable CachedBluetoothDevice bluetoothDevice) {
            mBluetoothDevice = bluetoothDevice;
            return this;
        }

        /** Sets the audio zone configuration state. */
        public Builder setAudioZoneConfigState(AudioZoneConfigState audioZoneConfigState) {
            mAudioZoneConfigState = audioZoneConfigState;
            return this;
        }

        /** Sets the bluetooth device state. */
        public Builder setBluetoothDeviceState(BluetoothDeviceState bluetoothDeviceState) {
            mBluetoothDeviceState = bluetoothDeviceState;
            return this;
        }

        /** Sets the global state. */
        public Builder setGlobalState(GlobalState globalState) {
            mGlobalState = globalState;
            return this;
        }

        /** Sets the audio route state. */
        public Builder setState(State state) {
            mState = state;
            return this;
        }

        /** Sets the volume state. */
        public Builder setVolumeState(VolumeState volumeState) {
            mVolumeState = volumeState;
            return this;
        }

        /** Builds the {@link AudioRouteItem} instance. */
        public AudioRouteItem build() {
            return new AudioRouteItem(this);
        }
    }
}
