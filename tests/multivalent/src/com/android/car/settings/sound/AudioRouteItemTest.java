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

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mDefaultAudioZoneConfigState = new AudioRouteItem.AudioZoneConfigState.Builder().build();
        mDefaultBluetoothDeviceState = new AudioRouteItem.BluetoothDeviceState.Builder().build();
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
                        .build();

        assertThat(audioRouteItem.getName()).isEqualTo(name);
        assertThat(audioRouteItem.getAddress()).isEqualTo(address);
        assertThat(audioRouteItem.getAudioRouteType()).isEqualTo(audioDeviceType);
        assertThat(audioRouteItem.getBluetoothDevice()).isNull();
        assertThat(audioRouteItem.getAudioZoneConfigState())
                .isEqualTo(mDefaultAudioZoneConfigState);
        assertThat(audioRouteItem.getBluetoothDeviceState())
                .isEqualTo(mDefaultBluetoothDeviceState);
    }

    @Test
    public void init_withCachedBluetoothA2dpDevice() {
        String name = "bluetooth";
        String address = "bluetoothAddress";
        when(mCachedBluetoothDevice.getName()).thenReturn(name);
        when(mCachedBluetoothDevice.getAddress()).thenReturn(address);
        when(mCachedBluetoothDevice.isConnectedA2dpDevice()).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(false);

        AudioRouteItem audioRouteItem =
                new AudioRouteItem.Builder(mCachedBluetoothDevice)
                        .setAudioZoneConfigState(mDefaultAudioZoneConfigState)
                        .setBluetoothDeviceState(mDefaultBluetoothDeviceState)
                        .build();

        assertThat(audioRouteItem.getName()).isEqualTo(name);
        assertThat(audioRouteItem.getAddress()).isEqualTo(address);
        assertThat(audioRouteItem.getAudioRouteType()).isEqualTo(TYPE_BLUETOOTH_A2DP);
        assertThat(audioRouteItem.getBluetoothDevice()).isEqualTo(mCachedBluetoothDevice);
        assertThat(audioRouteItem.getAudioZoneConfigState())
                .isEqualTo(mDefaultAudioZoneConfigState);
        assertThat(audioRouteItem.getBluetoothDeviceState())
                .isEqualTo(mDefaultBluetoothDeviceState);
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

        AudioRouteItem audioRouteItem =
                new AudioRouteItem.Builder(mCachedBluetoothDevice)
                        .setAudioZoneConfigState(mDefaultAudioZoneConfigState)
                        .setBluetoothDeviceState(mDefaultBluetoothDeviceState)
                        .build();

        assertThat(audioRouteItem.getName()).isEqualTo(name);
        assertThat(audioRouteItem.getAddress()).isEqualTo(address);
        assertThat(audioRouteItem.getAudioRouteType()).isEqualTo(TYPE_BLE_HEADSET);
        assertThat(audioRouteItem.getBluetoothDevice()).isEqualTo(mCachedBluetoothDevice);
        assertThat(audioRouteItem.getAudioZoneConfigState())
                .isEqualTo(mDefaultAudioZoneConfigState);
        assertThat(audioRouteItem.getBluetoothDeviceState())
                .isEqualTo(mDefaultBluetoothDeviceState);
    }

    @Test
    public void testAudioZoneConfigStateBuilder() {
        boolean isActive = true;
        boolean isSelected = false;
        boolean isBroadcasting = true;

        AudioRouteItem.AudioZoneConfigState state =
                new AudioRouteItem.AudioZoneConfigState.Builder()
                        .setIsActive(isActive)
                        .setIsSelected(isSelected)
                        .setIsBroadcasting(isBroadcasting)
                        .build();

        assertThat(state.mIsActive).isEqualTo(isActive);
        assertThat(state.mIsSelected).isEqualTo(isSelected);
        assertThat(state.mIsBroadcasting).isEqualTo(isBroadcasting);
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

        assertThat(state.mIsActiveA2dp).isEqualTo(isActiveA2dp);
        assertThat(state.mIsActiveLeAudio).isEqualTo(isActiveLeAudio);
        assertThat(state.mIsConnectedA2dp).isEqualTo(isConnectedA2dp);
        assertThat(state.mIsConnectedLeAudio).isEqualTo(isConnectedLeAudio);
        assertThat(state.mIsReceivingBroadcast).isEqualTo(isReceivingBroadcast);
    }
}
