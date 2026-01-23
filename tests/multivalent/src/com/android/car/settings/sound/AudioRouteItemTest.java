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
import static android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.media.AudioDeviceAttributes;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class AudioRouteItemTest {

    @Mock
    private AudioDeviceAttributes mAudioDeviceAttributes;
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;

    private AudioRouteItem.AudioZoneConfigState mDefaultAudioZoneConfigState;
    private AudioRouteItem.BluetoothDeviceState mDefaultBluetoothDeviceState;
    private AudioRouteItem.VolumeState mDefaultVolumeState;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mDefaultAudioZoneConfigState = new AudioRouteItem.AudioZoneConfigState.Builder().build();
        mDefaultBluetoothDeviceState = new AudioRouteItem.BluetoothDeviceState.Builder().build();
        mDefaultVolumeState = new AudioRouteItem.VolumeState.Builder().build();
    }

    @Test
    public void init_withAudioDeviceAttributes() {
        String name = "audio";
        String address = "audioAddress";
        int audioDeviceType = TYPE_BUILTIN_SPEAKER;
        when(mAudioDeviceAttributes.getName()).thenReturn(name);
        when(mAudioDeviceAttributes.getAddress()).thenReturn(address);
        when(mAudioDeviceAttributes.getType()).thenReturn(audioDeviceType);

        AudioRouteItem audioRouteItem =
                new AudioRouteItem.Builder(mAudioDeviceAttributes)
                        .setAudioZoneConfigState(mDefaultAudioZoneConfigState)
                        .setBluetoothDeviceState(mDefaultBluetoothDeviceState)
                        .setVolumeState(mDefaultVolumeState)
                        .build();

        assertThat(audioRouteItem.getName()).isEqualTo(name);
        assertThat(audioRouteItem.getAddress()).isEqualTo(address);
        assertThat(audioRouteItem.getAudioRouteType()).isEqualTo(audioDeviceType);
        assertThat(audioRouteItem.getBluetoothDevice()).isNull();
        assertThat(audioRouteItem.getAudioZoneConfigState())
                .isEqualTo(mDefaultAudioZoneConfigState);
        assertThat(audioRouteItem.getBluetoothDeviceState())
                .isEqualTo(mDefaultBluetoothDeviceState);
        assertThat(audioRouteItem.getState()).isEqualTo(AudioRouteItem.State.CREATED);
        assertThat(audioRouteItem.getVolumeState()).isEqualTo(mDefaultVolumeState);
    }

    @Test
    public void init_withCachedBluetoothA2dpDevice() {
        String name = "bluetooth";
        String address = "bluetoothAddress";
        when(mCachedBluetoothDevice.getName()).thenReturn(name);
        when(mCachedBluetoothDevice.getAddress()).thenReturn(address);
        when(mCachedBluetoothDevice.isConnectedA2dpDevice()).thenReturn(true);
        when(mCachedBluetoothDevice.isActiveDevice(BluetoothProfile.LE_AUDIO)).thenReturn(false);
        BluetoothDevice mockDevice = org.mockito.Mockito.mock(
                android.bluetooth.BluetoothDevice.class);
        when(mCachedBluetoothDevice.getDevice()).thenReturn(mockDevice);

        AudioRouteItem audioRouteItem =
                new AudioRouteItem.Builder(mCachedBluetoothDevice)
                        .setAudioZoneConfigState(mDefaultAudioZoneConfigState)
                        .setBluetoothDeviceState(mDefaultBluetoothDeviceState)
                        .setVolumeState(mDefaultVolumeState)
                        .build();

        assertThat(audioRouteItem.getName()).isEqualTo(name);
        assertThat(audioRouteItem.getAddress()).isEqualTo(address);
        assertThat(audioRouteItem.getAudioRouteType()).isEqualTo(TYPE_BLUETOOTH_A2DP);
        assertThat(audioRouteItem.getBluetoothDevice()).isEqualTo(mCachedBluetoothDevice);
        assertThat(audioRouteItem.getAudioZoneConfigState())
                .isEqualTo(mDefaultAudioZoneConfigState);
        assertThat(audioRouteItem.getBluetoothDeviceState())
                .isEqualTo(mDefaultBluetoothDeviceState);
        assertThat(audioRouteItem.getVolumeState()).isEqualTo(mDefaultVolumeState);
    }

    @Test
    public void init_withCachedBluetoothBleDevice() {
        String name = "bluetooth";
        String address = "bluetoothAddress";
        when(mCachedBluetoothDevice.getName()).thenReturn(name);
        when(mCachedBluetoothDevice.getAddress()).thenReturn(address);
        // If both profiles are available, BLE is preferred.
        when(mCachedBluetoothDevice.isConnectedA2dpDevice()).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(true);
        BluetoothDevice mockDevice = org.mockito.Mockito.mock(
                android.bluetooth.BluetoothDevice.class);
        when(mCachedBluetoothDevice.getDevice()).thenReturn(mockDevice);

        AudioRouteItem audioRouteItem =
                new AudioRouteItem.Builder(mCachedBluetoothDevice)
                        .setAudioZoneConfigState(mDefaultAudioZoneConfigState)
                        .setBluetoothDeviceState(mDefaultBluetoothDeviceState)
                        .setVolumeState(mDefaultVolumeState)
                        .build();

        assertThat(audioRouteItem.getName()).isEqualTo(name);
        assertThat(audioRouteItem.getAddress()).isEqualTo(address);
        assertThat(audioRouteItem.getAudioRouteType()).isEqualTo(TYPE_BLE_HEADSET);
        assertThat(audioRouteItem.getBluetoothDevice()).isEqualTo(mCachedBluetoothDevice);
        assertThat(audioRouteItem.getAudioZoneConfigState())
                .isEqualTo(mDefaultAudioZoneConfigState);
        assertThat(audioRouteItem.getBluetoothDeviceState())
                .isEqualTo(mDefaultBluetoothDeviceState);
        assertThat(audioRouteItem.getVolumeState()).isEqualTo(mDefaultVolumeState);
    }

    @Test
    public void testAudioZoneConfigStateBuilder() {
        boolean isActive = true;
        boolean isSelected = false;

        AudioRouteItem.AudioZoneConfigState state =
                new AudioRouteItem.AudioZoneConfigState.Builder()
                        .setIsActive(isActive)
                        .setIsSelected(isSelected)
                        .build();

        assertThat(state.isActive()).isEqualTo(isActive);
        assertThat(state.isSelected()).isEqualTo(isSelected);
    }

    @Test
    public void testBluetoothDeviceStateBuilder() {
        boolean isActiveA2dp = true;
        boolean isActiveLeAudio = false;
        boolean isConnectedA2dp = true;
        boolean isConnectedLeAudio = false;
        boolean isReceivingBroadcast = true;

        AudioRouteItem.BluetoothDeviceState state =
                new AudioRouteItem.BluetoothDeviceState.Builder()
                        .setIsActiveA2dp(isActiveA2dp)
                        .setIsActiveLeAudio(isActiveLeAudio)
                        .setIsConnectedA2dp(isConnectedA2dp)
                        .setIsConnectedLeAudio(isConnectedLeAudio)
                        .setIsReceivingBroadcast(isReceivingBroadcast)
                        .build();

        assertThat(state.isActiveA2dp()).isEqualTo(isActiveA2dp);
        assertThat(state.isActiveLeAudio()).isEqualTo(isActiveLeAudio);
        assertThat(state.isConnectedA2dp()).isEqualTo(isConnectedA2dp);
        assertThat(state.isConnectedLeAudio()).isEqualTo(isConnectedLeAudio);
        assertThat(state.isReceivingBroadcast()).isEqualTo(isReceivingBroadcast);
    }

    @Test
    public void testToBuilder_createsNewObjectWithSameValues() {
        String name = "originalName";
        String address = "originalAddress";
        int audioDeviceType = TYPE_BLUETOOTH_A2DP;
        AudioRouteItem.State state = AudioRouteItem.State.UNICAST_ACTIVE;

        AudioRouteItem.AudioZoneConfigState audioZoneConfigState =
                new AudioRouteItem.AudioZoneConfigState.Builder()
                        .setIsActive(true)
                        .setIsSelected(true)
                        .build();
        AudioRouteItem.BluetoothDeviceState bluetoothDeviceState =
                new AudioRouteItem.BluetoothDeviceState.Builder()
                        .setIsActiveA2dp(true)
                        .setIsConnectedA2dp(true)
                        .build();

        AudioRouteItem originalItem =
                new AudioRouteItem.Builder(mAudioDeviceAttributes)
                        .setName(name)
                        .setAddress(address)
                        .setAudioDeviceType(audioDeviceType)
                        .setBluetoothDevice(mCachedBluetoothDevice)
                        .setAudioZoneConfigState(audioZoneConfigState)
                        .setBluetoothDeviceState(bluetoothDeviceState)
                        .setState(state)
                        .build();

        AudioRouteItem newItem = originalItem.toBuilder().build();

        assertThat(newItem.getName()).isEqualTo(originalItem.getName());
        assertThat(newItem.getAddress()).isEqualTo(originalItem.getAddress());
        assertThat(newItem.getAudioRouteType()).isEqualTo(originalItem.getAudioRouteType());
        assertThat(newItem.getBluetoothDevice()).isEqualTo(originalItem.getBluetoothDevice());
        assertThat(newItem.getAudioZoneConfigState())
                .isEqualTo(originalItem.getAudioZoneConfigState());
        assertThat(newItem.getBluetoothDeviceState())
                .isEqualTo(originalItem.getBluetoothDeviceState());
        assertThat(newItem.getState()).isEqualTo(originalItem.getState());
    }

    @Test
    public void testIsBluetoothAudioRoute() {
        when(mAudioDeviceAttributes.getType()).thenReturn(TYPE_BLUETOOTH_A2DP);
        AudioRouteItem bluetoothA2dpItem = new AudioRouteItem.Builder(mAudioDeviceAttributes)
                .build();
        assertThat(bluetoothA2dpItem.isBluetoothAudioRoute()).isTrue();

        when(mAudioDeviceAttributes.getType()).thenReturn(TYPE_BLE_HEADSET);
        AudioRouteItem bleHeadsetItem = new AudioRouteItem.Builder(mAudioDeviceAttributes).build();
        assertThat(bleHeadsetItem.isBluetoothAudioRoute()).isTrue();

        when(mAudioDeviceAttributes.getType()).thenReturn(TYPE_BUILTIN_SPEAKER);
        AudioRouteItem speakerItem = new AudioRouteItem.Builder(mAudioDeviceAttributes).build();
        assertThat(speakerItem.isBluetoothAudioRoute()).isFalse();
    }

    @Test
    public void testVolumeStateBuilder() {
        int min = 1;
        int max = 10;
        int current = 5;
        boolean useProfile = true;
        AudioRouteItem.VolumeState state = new AudioRouteItem.VolumeState.Builder()
                .setMinVolume(min)
                .setMaxVolume(max)
                .setCurrentVolume(current)
                .setUseVolumeControlProfile(useProfile)
                .build();
        assertThat(state.getMinVolume()).isEqualTo(min);
        assertThat(state.getMaxVolume()).isEqualTo(max);
        assertThat(state.getCurrentVolume()).isEqualTo(current);
        assertThat(state.useVolumeControlProfile()).isEqualTo(useProfile);
    }

    @Test
    public void testVolumeStateToBuilderCopiesAndModifiesCorrectly() {
        int originalMin = 1;
        int originalMax = 10;
        int originalCurrent = 5;
        boolean originalUseProfile = true;

        AudioRouteItem.VolumeState originalState = new AudioRouteItem.VolumeState.Builder()
                .setMinVolume(originalMin)
                .setMaxVolume(originalMax)
                .setCurrentVolume(originalCurrent)
                .setUseVolumeControlProfile(originalUseProfile)
                .build();

        // Create a new builder from the original state
        AudioRouteItem.VolumeState.Builder builder = originalState.toBuilder();

        // Modify values in the new builder
        int newMin = 2;
        int newMax = 20;
        int newCurrent = 8;
        boolean newUseProfile = false;
        builder.setMinVolume(newMin)
                .setMaxVolume(newMax)
                .setCurrentVolume(newCurrent)
                .setUseVolumeControlProfile(newUseProfile);

        AudioRouteItem.VolumeState newState = builder.build();

        // Verify the new state has the modified values
        assertThat(newState.getMinVolume()).isEqualTo(newMin);
        assertThat(newState.getMaxVolume()).isEqualTo(newMax);
        assertThat(newState.getCurrentVolume()).isEqualTo(newCurrent);
        assertThat(newState.useVolumeControlProfile()).isEqualTo(newUseProfile);

        // Verify the original state remains unchanged
        assertThat(originalState.getMinVolume()).isEqualTo(originalMin);
        assertThat(originalState.getMaxVolume()).isEqualTo(originalMax);
        assertThat(originalState.getCurrentVolume()).isEqualTo(originalCurrent);
        assertThat(originalState.useVolumeControlProfile()).isEqualTo(originalUseProfile);
    }

    @Test
    public void testGlobalStateBuilder() {
        boolean enabled = true;
        AudioRouteItem.GlobalState state = new AudioRouteItem.GlobalState.Builder()
                .setIsAudioSharingEnabled(enabled)
                .build();
        assertThat(state.isAudioSharingEnabled()).isEqualTo(enabled);
    }

    @Test
    public void state_getValue_returnsCorrectIntegerValue() {
        // Verifies that each state enum returns the correct integer value.
        assertThat(AudioRouteItem.State.UNSPECIFIED.getValue()).isEqualTo(0);
        assertThat(AudioRouteItem.State.CREATED.getValue()).isEqualTo(1);
        assertThat(AudioRouteItem.State.UNICAST_READY.getValue()).isEqualTo(2);
        assertThat(AudioRouteItem.State.UNICAST_ACTIVE.getValue()).isEqualTo(3);
        assertThat(AudioRouteItem.State.MULTICAST_READY_UNICAST_READY.getValue()).isEqualTo(4);
        assertThat(AudioRouteItem.State.MULTICAST_READY_UNICAST_ACTIVE.getValue()).isEqualTo(5);
        assertThat(AudioRouteItem.State.MULTICAST_ACTIVE.getValue()).isEqualTo(6);
        assertThat(AudioRouteItem.State.STARTING_UNICAST.getValue()).isEqualTo(7);
        assertThat(AudioRouteItem.State.JOINING_BROADCAST.getValue()).isEqualTo(8);
        assertThat(AudioRouteItem.State.STARTING_BROADCAST.getValue()).isEqualTo(9);
        assertThat(AudioRouteItem.State.LEAVING_BROADCAST.getValue()).isEqualTo(10);
        assertThat(AudioRouteItem.State.BROADCAST_READY.getValue()).isEqualTo(11);
        assertThat(AudioRouteItem.State.BROADCAST_ACTIVE.getValue()).isEqualTo(12);
    }

    @Test
    public void testState_isActiveState() {
        assertThat(AudioRouteItem.State.UNSPECIFIED.isActiveState()).isFalse();
        assertThat(AudioRouteItem.State.CREATED.isActiveState()).isFalse();
        assertThat(AudioRouteItem.State.UNICAST_READY.isActiveState()).isFalse();
        assertThat(AudioRouteItem.State.UNICAST_ACTIVE.isActiveState()).isTrue();
        assertThat(AudioRouteItem.State.MULTICAST_READY_UNICAST_READY.isActiveState()).isFalse();
        assertThat(AudioRouteItem.State.MULTICAST_READY_UNICAST_ACTIVE.isActiveState()).isTrue();
        assertThat(AudioRouteItem.State.MULTICAST_ACTIVE.isActiveState()).isFalse();
        assertThat(AudioRouteItem.State.STARTING_UNICAST.isActiveState()).isFalse();
        assertThat(AudioRouteItem.State.JOINING_BROADCAST.isActiveState()).isFalse();
        assertThat(AudioRouteItem.State.STARTING_BROADCAST.isActiveState()).isFalse();
        assertThat(AudioRouteItem.State.LEAVING_BROADCAST.isActiveState()).isFalse();
        assertThat(AudioRouteItem.State.BROADCAST_READY.isActiveState()).isFalse();
        assertThat(AudioRouteItem.State.BROADCAST_ACTIVE.isActiveState()).isTrue();
    }

    @Test
    public void testState_isStartingState() {
        assertThat(AudioRouteItem.State.UNSPECIFIED.isStartingState()).isFalse();
        assertThat(AudioRouteItem.State.STARTING_UNICAST.isStartingState()).isTrue();
        assertThat(AudioRouteItem.State.JOINING_BROADCAST.isStartingState()).isTrue();
        assertThat(AudioRouteItem.State.CREATED.isStartingState()).isFalse();
        assertThat(AudioRouteItem.State.UNICAST_ACTIVE.isStartingState()).isFalse();
        assertThat(AudioRouteItem.State.BROADCAST_ACTIVE.isStartingState()).isFalse();
    }

    @Test
    public void testState_isSelfDrivenState() {
        assertThat(AudioRouteItem.State.UNSPECIFIED.isSelfDrivenState()).isFalse();
        assertThat(AudioRouteItem.State.CREATED.isSelfDrivenState()).isTrue();
        assertThat(AudioRouteItem.State.STARTING_UNICAST.isSelfDrivenState()).isTrue();
        assertThat(AudioRouteItem.State.STARTING_BROADCAST.isSelfDrivenState()).isTrue();
        assertThat(AudioRouteItem.State.JOINING_BROADCAST.isSelfDrivenState()).isTrue();
        assertThat(AudioRouteItem.State.LEAVING_BROADCAST.isSelfDrivenState()).isTrue();
        assertThat(AudioRouteItem.State.UNICAST_READY.isSelfDrivenState()).isFalse();
        assertThat(AudioRouteItem.State.UNICAST_ACTIVE.isSelfDrivenState()).isFalse();
        assertThat(AudioRouteItem.State.BROADCAST_ACTIVE.isSelfDrivenState()).isFalse();
    }

    @Test
    public void testState_isBroadcastState() {
        assertThat(AudioRouteItem.State.UNSPECIFIED.isBroadcastState()).isFalse();
        assertThat(AudioRouteItem.State.CREATED.isBroadcastState()).isFalse();
        assertThat(AudioRouteItem.State.UNICAST_READY.isBroadcastState()).isFalse();
        assertThat(AudioRouteItem.State.UNICAST_ACTIVE.isBroadcastState()).isFalse();
        assertThat(AudioRouteItem.State.MULTICAST_READY_UNICAST_READY.isBroadcastState()).isFalse();
        assertThat(
                AudioRouteItem.State.MULTICAST_READY_UNICAST_ACTIVE.isBroadcastState()).isFalse();
        assertThat(AudioRouteItem.State.MULTICAST_ACTIVE.isBroadcastState()).isFalse();
        assertThat(AudioRouteItem.State.STARTING_UNICAST.isBroadcastState()).isFalse();
        assertThat(AudioRouteItem.State.JOINING_BROADCAST.isBroadcastState()).isFalse();
        assertThat(AudioRouteItem.State.STARTING_BROADCAST.isBroadcastState()).isFalse();
        assertThat(AudioRouteItem.State.LEAVING_BROADCAST.isBroadcastState()).isFalse();
        assertThat(AudioRouteItem.State.BROADCAST_READY.isBroadcastState()).isTrue();
        assertThat(AudioRouteItem.State.BROADCAST_ACTIVE.isBroadcastState()).isTrue();
    }
}
