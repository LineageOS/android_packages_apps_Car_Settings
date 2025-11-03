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

// TODO: b/451450273 - Replace with Java record or AutoBuilder.
/** A class to encapsulate audio route information. */
public class AudioRouteItem {
    private final String mName;
    private final String mAddress;
    private final @AudioDeviceInfo.AudioDeviceType int mAudioDeviceType;
    private final AudioZoneConfigState mAudioZoneConfigState;
    private final BluetoothDeviceState mBluetoothDeviceState;
    // TODO: b/451450273 - Remove mBluetoothDevice;
    @Nullable private final CachedBluetoothDevice mBluetoothDevice;

    /** A data class to encapsulate the state of an audio zone configuration. */
    public static final class AudioZoneConfigState {
        public final boolean mIsActive;
        public final boolean mIsSelected;
        public final boolean mIsBroadcasting;

        private AudioZoneConfigState(Builder builder) {
            this.mIsActive = builder.mIsActive;
            this.mIsSelected = builder.mIsSelected;
            this.mIsBroadcasting = builder.mIsBroadcasting;
        }

        @Override
        public String toString() {
            return "AudioZoneConfigState{mIsActive=%b, mIsSelected=%b, mIsBroadcasting=%b}"
                    .formatted(mIsActive, mIsSelected, mIsBroadcasting);
        }

        /** Builder for {@link AudioZoneConfigState}. */
        public static class Builder {
            private boolean mIsActive = false;
            private boolean mIsSelected = false;
            private boolean mIsBroadcasting = false;

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

            /** Sets whether the device is broadcasting. */
            public Builder setIsBroadcasting(boolean isBroadcasting) {
                mIsBroadcasting = isBroadcasting;
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
        public final boolean mIsActiveA2dp;
        public final boolean mIsActiveLeAudio;
        public final boolean mIsConnectedA2dp;
        public final boolean mIsConnectedLeAudio;
        public final boolean mIsReceivingBroadcast;

        private BluetoothDeviceState(Builder builder) {
            this.mIsActiveA2dp = builder.mIsActiveA2dp;
            this.mIsActiveLeAudio = builder.mIsActiveLeAudio;
            this.mIsConnectedA2dp = builder.mIsConnectedA2dp;
            this.mIsConnectedLeAudio = builder.mIsConnectedLeAudio;
            this.mIsReceivingBroadcast = builder.mIsReceivingBroadcast;
        }

        @Override
        public String toString() {
            return ("BluetoothDeviceState{mIsActiveA2dp=%b, mIsActiveLeAudio=%b, "
                    + "mIsConnectedA2dp=%b, mIsConnectedLeAudio=%b, mIsReceivingBroadcast=%b}")
                    .formatted(
                            mIsActiveA2dp,
                            mIsActiveLeAudio,
                            mIsConnectedA2dp,
                            mIsConnectedLeAudio,
                            mIsReceivingBroadcast);
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

    private AudioRouteItem(Builder builder) {
        mName = builder.mName;
        mAddress = builder.mAddress;
        mAudioDeviceType = builder.mAudioDeviceType;
        mBluetoothDevice = builder.mBluetoothDevice;
        mAudioZoneConfigState = builder.mAudioZoneConfigState;
        mBluetoothDeviceState = builder.mBluetoothDeviceState;
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

    @Override
    public String toString() {
        return ("AudioRouteItem{mName='%s', mAddress='%s', mAudioDeviceType=%d, "
                + "mAudioZoneConfigState=%s, mBluetoothDeviceState=%s}")
                .formatted(
                        mName,
                        mAddress,
                        mAudioDeviceType,
                        mAudioZoneConfigState,
                        mBluetoothDeviceState);
    }

    public static class Builder {
        private String mName;
        private String mAddress;
        private @AudioDeviceInfo.AudioDeviceType int mAudioDeviceType;
        @Nullable private CachedBluetoothDevice mBluetoothDevice;
        private AudioZoneConfigState mAudioZoneConfigState =
                new AudioZoneConfigState.Builder().build();
        private BluetoothDeviceState mBluetoothDeviceState =
                new BluetoothDeviceState.Builder().build();

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

        /** Builds the {@link AudioRouteItem} instance. */
        public AudioRouteItem build() {
            return new AudioRouteItem(this);
        }
    }
}
