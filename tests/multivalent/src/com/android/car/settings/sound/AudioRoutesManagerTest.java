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
import static android.media.AudioAttributes.USAGE_MEDIA;
import static android.media.AudioDeviceInfo.TYPE_BLE_BROADCAST;
import static android.media.AudioDeviceInfo.TYPE_BLE_HEADSET;
import static android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP;
import static android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothVolumeControl;
import android.car.media.CarAudioManager;
import android.car.media.CarAudioZoneConfigInfo;
import android.car.media.CarVolumeGroupInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;
import android.widget.Toast;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.CarSettingsApplication;
import com.android.car.settings.R;
import com.android.car.settings.bluetooth.audiosharing.BaseAudioSharingPreferenceController;
import com.android.dx.mockito.inline.extended.ExtendedMockito;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LeAudioProfile;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class AudioRoutesManagerTest {

    private static final int USAGE = USAGE_MEDIA;
    private static final int TEST_ZONE_ID = 0;
    private static final String AUDIO_DEVICE_NAME = "device audio";
    private static final String AUDIO_DEVICE_ADDRESS = "device_audio_address";
    private static final String DEVICE_AUDIO_ZONE_CONFIG_NAME = "zone_config_device";
    private static final String A2DP_AUDIO_ZONE_CONFIG_NAME = "zone_config_bluetooth_a2dp";
    private static final String LE_AUDIO_ZONE_CONFIG_NAME = "zone_config_bluetooth_le_audio";
    private static final String LE_BROADCAST_AUDIO_ZONE_CONFIG_NAME = "zone_config_le_broadcast";
    private static final String BT_A2DP_DEVICE_NAME = "bluetooth_a2dp";
    private static final String BT_A2DP_DEVICE_ADDRESS = "bluetooth_a2dp_address";
    private static final String BT_LE_AUDIO_DEVICE_NAME_1 = "bluetooth_le_audio_1";
    private static final String BT_LE_AUDIO_DEVICE_ADDRESS_1 = "bluetooth_le_audio_address_1";
    private static final String BT_LE_AUDIO_DEVICE_NAME_2 = "bluetooth_le_audio_2";
    private static final String BT_LE_AUDIO_DEVICE_ADDRESS_2 = "bluetooth_le_audio_address_2";
    private static final String BT_LE_AUDIO_DEVICE_NAME_3 = "bluetooth_le_audio_3";
    private static final String BT_LE_AUDIO_DEVICE_ADDRESS_3 = "bluetooth_le_audio_address_3";
    private static final String BT_LE_BROADCAST_DEVICE_NAME = "bluetooth_le_broadcast";
    private static final String BT_LE_BROADCAST_ADDRESS = "bluetooth_le_broadcast_address";
    private static final String CONFLICTING_ZONE_CONFIG_NAME = "conflicting_zone_config";
    private final Context mContext = spy(ApplicationProvider.getApplicationContext());
    private MockitoSession mSession;
    private AudioRoutesManager mAudioRoutesManager;
    @Mock
    private CarSettingsApplication mCarSettingsApplication;
    @Mock
    private CarAudioManager mCarAudioManager;
    @Mock
    private LocalBluetoothManager mBluetoothManager;
    @Mock
    private LocalBluetoothProfileManager mLocalBluetoothProfileManager;
    @Mock
    private LocalBluetoothLeBroadcast mLocalBluetoothLeBroadcast;
    @Mock
    private LocalBluetoothLeBroadcastAssistant mLocalBluetoothLeBroadcastAssistant;
    @Mock
    private VolumeControlProfile mVolumeControlProfile;
    @Mock
    private LeAudioProfile mLeAudioProfile;
    @Mock
    private CachedBluetoothDeviceManager mCachedBluetoothDeviceManager;
    @Mock
    private BluetoothLeBroadcastMetadata mBluetoothLeBroadcastMetadata;
    @Mock
    private BluetoothDevice mBroadcastBluetoothDevice;
    @Mock
    private BluetoothDevice mReceivingBroadcastBluetoothDevice1;
    @Mock
    private BluetoothDevice mReceivingBroadcastBluetoothDevice2;
    @Mock
    private BluetoothDevice mReceivingBroadcastBluetoothDevice3;
    @Mock
    private BluetoothLeBroadcastReceiveState mBluetoothLeBroadcastReceiveState;
    @Mock
    private AudioAttributes mAudioAttributes;
    @Mock
    private AudioDeviceInfo mAudioDeviceInfo;
    @Mock
    private AudioDeviceAttributes mAudioDeviceAttributes;
    @Mock
    private AudioRoutesManager.AudioRoutesUpdateListener mListener;
    @Mock
    private Toast mMockToast;
    private ArgumentCaptor<List<AudioRouteItem>> mAudioRoutesCaptor;
    private ArgumentCaptor<BluetoothVolumeControl.Callback> mVolumeControlCallbackCaptor;
    private ArgumentCaptor<CarAudioManager.CarVolumeCallback> mCarVolumeCallbackCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mAudioRoutesCaptor = ArgumentCaptor.forClass(List.class);
        mVolumeControlCallbackCaptor = ArgumentCaptor.forClass(
                BluetoothVolumeControl.Callback.class);
        mCarVolumeCallbackCaptor = ArgumentCaptor.forClass(CarAudioManager.CarVolumeCallback.class);
        initMocks();
    }

    @After
    public void tearDown() {
        mSession.finishMocking();
    }

    @Test
    public void getAudioRouteList_returnsDeviceAddresses() {
        CarAudioZoneConfigInfo deviceZoneConfig = createZoneConfig(
                /* name= */ DEVICE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ AUDIO_DEVICE_ADDRESS,
                /* deviceName= */ AUDIO_DEVICE_NAME,
                /* type= */ TYPE_BUILTIN_SPEAKER,
                /* isActive= */ true,
                /* isSelected= */ true);
        CarAudioZoneConfigInfo a2dpZoneConfig = createZoneConfig(
                /* name= */ A2DP_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_A2DP_DEVICE_ADDRESS,
                /* deviceName= */ BT_A2DP_DEVICE_NAME,
                /* type= */ TYPE_BLUETOOTH_A2DP,
                /* isActive= */ true,
                /* isSelected= */ false);
        CarAudioZoneConfigInfo leBroadcastZoneConfig = createZoneConfig(
                /* name= */ LE_BROADCAST_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_BROADCAST_ADDRESS,
                /* deviceName= */ BT_LE_BROADCAST_DEVICE_NAME,
                /* type= */ TYPE_BLE_BROADCAST,
                /* isActive= */ true,
                /* isSelected= */ false);
        // This one overlaps A2DP_AUDIO_ZONE_CONFIG_NAME
        CachedBluetoothDevice a2dpBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_A2DP_DEVICE_NAME,
                /* address= */ BT_A2DP_DEVICE_ADDRESS,
                /* device= */ null,
                /* isConnectedA2dp= */ true,
                /* isConnectedLeAudio= */false,
                /* isActiveA2dp= */ true,
                /* isActiveLeAudio= */ false);
        CachedBluetoothDevice leAudioBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* device= */ mReceivingBroadcastBluetoothDevice1,
                /* isConnectedA2dp= */ true,
                /* isConnectedLeAudio= */true,
                /* isActiveA2dp= */ true,
                /* isActiveLeAudio= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(deviceZoneConfig, a2dpZoneConfig, leBroadcastZoneConfig),
                /* cachedDevices= */ List.of(a2dpBluetoothDevice, leAudioBluetoothDevice)
        );

        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        List<String> audioRouteList = mAudioRoutesManager.getAudioRouteList();
        assertThat(audioRouteList).containsExactly(
                AUDIO_DEVICE_ADDRESS,
                BT_A2DP_DEVICE_ADDRESS,
                BT_LE_AUDIO_DEVICE_ADDRESS_1,
                BT_LE_BROADCAST_ADDRESS
        );
    }

    @Test
    public void getOutputAddress_returnsActiveDeviceAddress() {
        CarAudioZoneConfigInfo deviceZoneConfig = createZoneConfig(
                /* name= */ DEVICE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ AUDIO_DEVICE_ADDRESS,
                /* deviceName= */ AUDIO_DEVICE_NAME,
                /* type= */ TYPE_BUILTIN_SPEAKER,
                /* isActive= */ true,
                /* isSelected= */ true);
        CarAudioZoneConfigInfo a2dpZoneConfig = createZoneConfig(
                /* name= */ A2DP_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_A2DP_DEVICE_ADDRESS,
                /* deviceName= */ BT_A2DP_DEVICE_NAME,
                /* type= */ TYPE_BLUETOOTH_A2DP,
                /* isActive= */ true,
                /* isSelected= */ false);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(deviceZoneConfig, a2dpZoneConfig),
                /* cachedDevices= */ List.of()
        );

        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        assertThat(mAudioRoutesManager.getOutputAddress()).isEqualTo(AUDIO_DEVICE_ADDRESS);
    }

    @Test
    public void createAudioRoutes_returnsAudioRouteItems() {
        CarAudioZoneConfigInfo deviceZoneConfig = createZoneConfig(
                /* name= */ DEVICE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ AUDIO_DEVICE_ADDRESS,
                /* deviceName= */ AUDIO_DEVICE_NAME,
                /* type= */ TYPE_BUILTIN_SPEAKER,
                /* isActive= */ true,
                /* isSelected= */ true);
        CarAudioZoneConfigInfo a2dpZoneConfig = createZoneConfig(
                /* name= */ A2DP_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_A2DP_DEVICE_ADDRESS,
                /* deviceName= */ BT_A2DP_DEVICE_NAME,
                /* type= */ TYPE_BLUETOOTH_A2DP,
                /* isActive= */ true,
                /* isSelected= */ false);
        CarAudioZoneConfigInfo leBroadcastZoneConfig = createZoneConfig(
                /* name= */ LE_BROADCAST_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_BROADCAST_ADDRESS,
                /* deviceName= */ BT_LE_BROADCAST_DEVICE_NAME,
                /* type= */ TYPE_BLE_BROADCAST,
                /* isActive= */ true,
                /* isSelected= */ false);
        // This one overlaps A2DP_AUDIO_ZONE_CONFIG_NAME
        CachedBluetoothDevice a2dpBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_A2DP_DEVICE_NAME,
                /* address= */ BT_A2DP_DEVICE_ADDRESS,
                /* device= */ null,
                /* isConnectedA2dp= */ true,
                /* isConnectedLeAudio= */false,
                /* isActiveA2dp= */ true,
                /* isActiveLeAudio= */ false);
        CachedBluetoothDevice leAudioBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* device= */ mReceivingBroadcastBluetoothDevice1,
                /* isConnectedA2dp= */ true,
                /* isConnectedLeAudio= */ true,
                /* isActiveA2dp= */ true,
                /* isActiveLeAudio= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(deviceZoneConfig, a2dpZoneConfig, leBroadcastZoneConfig),
                /* cachedDevices= */ List.of(a2dpBluetoothDevice, leAudioBluetoothDevice)
        );
        setupLeBroadcast(BT_LE_BROADCAST_ADDRESS);
        setupBroadcastReceivingDevice(mReceivingBroadcastBluetoothDevice1);

        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        Map<String, AudioRouteItem> audioRouteItems = mAudioRoutesManager.getActiveRoutes();
        assertThat(audioRouteItems.size()).isEqualTo(4);

        // Audio device, active and selected.
        AudioRouteItem deviceAudioRoute = audioRouteItems.get(AUDIO_DEVICE_ADDRESS);
        assertThat(deviceAudioRoute.getAddress()).isEqualTo(AUDIO_DEVICE_ADDRESS);
        assertThat(deviceAudioRoute.getAudioRouteType()).isEqualTo(TYPE_BUILTIN_SPEAKER);
        assertThat(deviceAudioRoute.getBluetoothDevice()).isNull();
        assertThat(deviceAudioRoute.getAudioZoneConfigState().isActive()).isTrue();
        assertThat(deviceAudioRoute.getAudioZoneConfigState().isSelected()).isTrue();

        // A2DP BT device, active.
        AudioRouteItem a2dpAudioRoute = audioRouteItems.get(BT_A2DP_DEVICE_ADDRESS);
        assertThat(a2dpAudioRoute.getName()).isEqualTo(BT_A2DP_DEVICE_NAME);
        assertThat(a2dpAudioRoute.getAddress()).isEqualTo(BT_A2DP_DEVICE_ADDRESS);
        assertThat(a2dpAudioRoute.getAudioRouteType()).isEqualTo(TYPE_BLUETOOTH_A2DP);
        assertThat(a2dpAudioRoute.getBluetoothDevice()).isNotNull();
        assertThat(a2dpAudioRoute.getAudioZoneConfigState().isActive()).isTrue();
        assertThat(a2dpAudioRoute.getAudioZoneConfigState().isSelected()).isFalse();
        assertThat(a2dpAudioRoute.getBluetoothDeviceState().isActiveA2dp()).isTrue();
        assertThat(a2dpAudioRoute.getBluetoothDeviceState().isActiveLeAudio()).isFalse();
        assertThat(a2dpAudioRoute.getBluetoothDeviceState().isConnectedA2dp()).isTrue();
        assertThat(a2dpAudioRoute.getBluetoothDeviceState().isConnectedLeAudio()).isFalse();
        assertThat(a2dpAudioRoute.getBluetoothDeviceState().isReceivingBroadcast()).isFalse();
        assertThat(a2dpAudioRoute.getGlobalState().isAudioSharingEnabled()).isTrue();

        // LE Audio device, receiving broadcast.
        AudioRouteItem leAudioRoute = audioRouteItems.get(BT_LE_AUDIO_DEVICE_ADDRESS_1);
        assertThat(leAudioRoute.getName()).isEqualTo(BT_LE_AUDIO_DEVICE_NAME_1);
        assertThat(leAudioRoute.getAddress()).isEqualTo(BT_LE_AUDIO_DEVICE_ADDRESS_1);
        assertThat(leAudioRoute.getAudioRouteType()).isEqualTo(TYPE_BLE_HEADSET);
        assertThat(leAudioRoute.getBluetoothDevice()).isNotNull();
        assertThat(leAudioRoute.getAudioZoneConfigState().isActive()).isFalse();
        assertThat(leAudioRoute.getAudioZoneConfigState().isSelected()).isFalse();
        assertThat(leAudioRoute.getBluetoothDeviceState().isActiveA2dp()).isTrue();
        assertThat(leAudioRoute.getBluetoothDeviceState().isActiveLeAudio()).isTrue();
        assertThat(leAudioRoute.getBluetoothDeviceState().isConnectedA2dp()).isTrue();
        assertThat(leAudioRoute.getBluetoothDeviceState().isConnectedLeAudio()).isTrue();
        assertThat(leAudioRoute.getBluetoothDeviceState().isReceivingBroadcast()).isTrue();
        assertThat(leAudioRoute.getGlobalState().isAudioSharingEnabled()).isTrue();

        // Broadcast device.
        AudioRouteItem broadcastAudioRoute = audioRouteItems.get(BT_LE_BROADCAST_ADDRESS);
        assertThat(broadcastAudioRoute.getName()).isEqualTo(BT_LE_BROADCAST_DEVICE_NAME);
        assertThat(broadcastAudioRoute.getAddress()).isEqualTo(BT_LE_BROADCAST_ADDRESS);
        assertThat(broadcastAudioRoute.getAudioRouteType()).isEqualTo(TYPE_BLE_BROADCAST);
        assertThat(broadcastAudioRoute.getAudioZoneConfigState().isActive()).isTrue();
        assertThat(broadcastAudioRoute.getAudioZoneConfigState().isSelected()).isFalse();
    }

    @Test
    public void createAudioRoutes_usesCarAudioVolume_forSelectedDevice() {
        CarAudioZoneConfigInfo deviceZoneConfig = createZoneConfig(
                /* name= */ DEVICE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ AUDIO_DEVICE_ADDRESS,
                /* deviceName= */ AUDIO_DEVICE_NAME,
                /* type= */ TYPE_BUILTIN_SPEAKER,
                /* isActive= */ true,
                /* isSelected= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(deviceZoneConfig),
                /* cachedDevices= */ List.of()
        );
        when(mCarAudioManager.getVolumeGroupIdForUsage(TEST_ZONE_ID, USAGE)).thenReturn(1);
        when(mCarAudioManager.getGroupMaxVolume(1)).thenReturn(100);
        when(mCarAudioManager.getGroupMinVolume(1)).thenReturn(0);
        when(mCarAudioManager.getGroupVolume(1)).thenReturn(50);

        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);
        Map<String, AudioRouteItem> audioRouteItems = mAudioRoutesManager.getActiveRoutes();
        AudioRouteItem deviceAudioRoute = audioRouteItems.get(AUDIO_DEVICE_ADDRESS);

        assertThat(deviceAudioRoute.getVolumeState().getMaxVolume()).isEqualTo(100);
        assertThat(deviceAudioRoute.getVolumeState().getMinVolume()).isEqualTo(0);
        assertThat(deviceAudioRoute.getVolumeState().getCurrentVolume()).isEqualTo(50);
        assertThat(deviceAudioRoute.getVolumeState().useVolumeControlProfile()).isFalse();
    }

    @Test
    public void createAudioRoutes_usesCarAudioVolume_forNonBluetoothRoutes() {
        CarAudioZoneConfigInfo deviceZoneConfig = createZoneConfig(
                /* name= */ DEVICE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ AUDIO_DEVICE_ADDRESS,
                /* deviceName= */ AUDIO_DEVICE_NAME,
                /* type= */ TYPE_BUILTIN_SPEAKER,
                /* isActive= */ true,
                /* isSelected= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(deviceZoneConfig),
                /* cachedDevices= */ List.of()
        );
        when(mCarAudioManager.getVolumeGroupIdForUsage(TEST_ZONE_ID, USAGE)).thenReturn(1);
        when(mCarAudioManager.getGroupMaxVolume(1)).thenReturn(100);
        when(mCarAudioManager.getGroupMinVolume(1)).thenReturn(0);
        // Initial volume from CarAudioManager
        when(mCarAudioManager.getGroupVolume(1)).thenReturn(50);

        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);
        Map<String, AudioRouteItem> audioRouteItems = mAudioRoutesManager.getActiveRoutes();
        AudioRouteItem deviceAudioRoute = audioRouteItems.get(AUDIO_DEVICE_ADDRESS);

        assertThat(deviceAudioRoute.getVolumeState().getMaxVolume()).isEqualTo(100);
        assertThat(deviceAudioRoute.getVolumeState().getMinVolume()).isEqualTo(0);
        assertThat(deviceAudioRoute.getVolumeState().getCurrentVolume()).isEqualTo(50);
        assertThat(deviceAudioRoute.getVolumeState().useVolumeControlProfile()).isFalse();
    }

    @Test
    public void createAudioRoutes_usesCachedGroupVolume_forSelectedDevice() {
        CarAudioZoneConfigInfo a2dpZoneConfig = createZoneConfig(
                /* name= */ A2DP_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_A2DP_DEVICE_ADDRESS,
                /* deviceName= */ BT_A2DP_DEVICE_NAME,
                /* type= */ TYPE_BLUETOOTH_A2DP,
                /* isActive= */ true,
                /* isSelected= */ true);
        CachedBluetoothDevice a2dpBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_A2DP_DEVICE_NAME,
                /* address= */ BT_A2DP_DEVICE_ADDRESS,
                /* device= */ null,
                /* isConnectedA2dp= */ true,
                /* isConnectedLeAudio= */false,
                /* isActiveA2dp= */ true,
                /* isActiveLeAudio= */ false);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(a2dpZoneConfig),
                /* cachedDevices= */ List.of(a2dpBluetoothDevice)
        );
        when(mCarAudioManager.getVolumeGroupIdForUsage(TEST_ZONE_ID, USAGE)).thenReturn(1);
        when(mCarAudioManager.getGroupMaxVolume(1)).thenReturn(100);
        when(mCarAudioManager.getGroupMinVolume(1)).thenReturn(0);
        // Initial volume 60
        when(mCarAudioManager.getGroupVolume(1)).thenReturn(60);
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);
        // Capture callback to trigger volume change and populate cache
        verify(mCarAudioManager).registerCarVolumeCallback(mCarVolumeCallbackCaptor.capture());
        CarAudioManager.CarVolumeCallback callback = mCarVolumeCallbackCaptor.getValue();

        when(mCarAudioManager.getGroupVolume(1)).thenReturn(50);
        // Trigger callback. This should set cache to 50.
        callback.onGroupVolumeChanged(TEST_ZONE_ID, 1, 0);

        AudioRouteItem a2dpRoute = mAudioRoutesManager.getActiveRoutes().get(
                BT_A2DP_DEVICE_ADDRESS);
        assertThat(a2dpRoute.getVolumeState().getCurrentVolume()).isEqualTo(50);
    }

    @Test
    public void createAudioRoutes_usesCarAudioVolume_forLeAudioFallbackDevice() {
        CarAudioZoneConfigInfo deviceZoneConfig = createZoneConfig(
                /* name= */ DEVICE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ AUDIO_DEVICE_ADDRESS,
                /* deviceName= */ AUDIO_DEVICE_NAME,
                /* type= */ TYPE_BUILTIN_SPEAKER,
                /* isActive= */ true,
                /* isSelected= */ true);

        // Multicast LE Device is the unicast fallback => Use Car Audio volume.
        CarAudioZoneConfigInfo leAudioZoneConfig = createZoneConfig(
                /* name= */ LE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* deviceName= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* type= */ TYPE_BLE_HEADSET,
                /* isActive= */ true,
                /* isSelected= */ false);
        CachedBluetoothDevice leAudioBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* device= */ mReceivingBroadcastBluetoothDevice1,
                /* isConnectedA2dp= */ false,
                /* isConnectedLeAudio= */ true,
                /* isActiveA2dp= */ false,
                /* isActiveLeAudio= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(deviceZoneConfig, leAudioZoneConfig),
                /* cachedDevices= */ List.of(leAudioBluetoothDevice)
        );
        setupLeBroadcast(BT_LE_BROADCAST_ADDRESS);
        setupBroadcastReceivingDevice(mReceivingBroadcastBluetoothDevice1);

        // Fallback match for Device 1
        when(leAudioBluetoothDevice.getGroupId()).thenReturn(10);
        when(mLeAudioProfile.getBroadcastToUnicastFallbackGroup()).thenReturn(10);
        when(mVolumeControlProfile.getConnectionStatus(mReceivingBroadcastBluetoothDevice1))
                .thenReturn(BluetoothProfile.STATE_CONNECTED);

        when(mCarAudioManager.getVolumeGroupIdForUsage(TEST_ZONE_ID, USAGE)).thenReturn(1);
        when(mCarAudioManager.getGroupMaxVolume(1)).thenReturn(100);
        when(mCarAudioManager.getGroupMinVolume(1)).thenReturn(0);
        when(mCarAudioManager.getGroupVolume(1)).thenReturn(50);

        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);
        Map<String, AudioRouteItem> audioRouteItems = mAudioRoutesManager.getActiveRoutes();

        // Check Scenario (Fallback)
        AudioRouteItem leAudioRoute = audioRouteItems.get(BT_LE_AUDIO_DEVICE_ADDRESS_1);
        assertThat(leAudioRoute.getVolumeState().getMaxVolume()).isEqualTo(100);
        assertThat(leAudioRoute.getVolumeState().getMinVolume()).isEqualTo(0);
        assertThat(leAudioRoute.getVolumeState().getCurrentVolume()).isEqualTo(50);
        assertThat(leAudioRoute.getVolumeState().useVolumeControlProfile()).isFalse();
    }

    @Test
    public void createAudioRoutes_usesVcpVolume_forLeAudioDevice() {
        CarAudioZoneConfigInfo deviceZoneConfig = createZoneConfig(
                /* name= */ DEVICE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ AUDIO_DEVICE_ADDRESS,
                /* deviceName= */ AUDIO_DEVICE_NAME,
                /* type= */ TYPE_BUILTIN_SPEAKER,
                /* isActive= */ true,
                /* isSelected= */ true);
        CarAudioZoneConfigInfo leAudioZoneConfig = createZoneConfig(
                /* name= */ LE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* deviceName= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* type= */ TYPE_BLE_HEADSET,
                /* isActive= */ true,
                /* isSelected= */ false);
        // Multicast LE Device is NOT the unicast fallback => Use VCP.
        CachedBluetoothDevice leAudioBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME_2,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_2,
                /* device= */ mReceivingBroadcastBluetoothDevice2,
                /* isConnectedA2dp= */ false,
                /* isConnectedLeAudio= */ true,
                /* isActiveA2dp= */ false,
                /* isActiveLeAudio= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(deviceZoneConfig, leAudioZoneConfig),
                /* cachedDevices= */ List.of(leAudioBluetoothDevice)
        );
        setupLeBroadcast(BT_LE_BROADCAST_ADDRESS);
        setupBroadcastReceivingDevice(mReceivingBroadcastBluetoothDevice2);
        // No fallback match for Device, VCP supported.
        when(leAudioBluetoothDevice.getGroupId()).thenReturn(20);
        when(mLeAudioProfile.getBroadcastToUnicastFallbackGroup()).thenReturn(10);
        when(mVolumeControlProfile.getConnectionStatus(mReceivingBroadcastBluetoothDevice2))
                .thenReturn(BluetoothProfile.STATE_CONNECTED);
        when(mCarAudioManager.getVolumeGroupIdForUsage(TEST_ZONE_ID, USAGE)).thenReturn(1);
        when(mCarAudioManager.getGroupMaxVolume(1)).thenReturn(100);
        when(mCarAudioManager.getGroupMinVolume(1)).thenReturn(0);
        when(mCarAudioManager.getGroupVolume(1)).thenReturn(50);
        when(mReceivingBroadcastBluetoothDevice2.getAddress())
                .thenReturn(BT_LE_AUDIO_DEVICE_ADDRESS_2);

        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);
        // Trigger volume change via VCP callback
        verify(mVolumeControlProfile).registerCallback(any(),
                mVolumeControlCallbackCaptor.capture());
        BluetoothVolumeControl.Callback callback = mVolumeControlCallbackCaptor.getValue();
        // 191 scales to 75 in range 0-100 from 0-255
        callback.onDeviceVolumeChanged(mReceivingBroadcastBluetoothDevice2, 191);

        Map<String, AudioRouteItem> audioRouteItems = mAudioRoutesManager.getActiveRoutes();

        // Check Scenario (Non-fallback, VCP)
        AudioRouteItem leAudioRoute = audioRouteItems.get(BT_LE_AUDIO_DEVICE_ADDRESS_2);
        assertThat(leAudioRoute.getVolumeState().getMaxVolume()).isEqualTo(100);
        assertThat(leAudioRoute.getVolumeState().getMinVolume()).isEqualTo(0);
        assertThat(leAudioRoute.getVolumeState().getCurrentVolume()).isEqualTo(75);
        assertThat(leAudioRoute.getVolumeState().useVolumeControlProfile()).isTrue();
    }

    @Test
    public void createAudioRoutes_usesCarAudioVolume_forLeAudioNonVcpDevice() {
        CarAudioZoneConfigInfo deviceZoneConfig = createZoneConfig(
                /* name= */ DEVICE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ AUDIO_DEVICE_ADDRESS,
                /* deviceName= */ AUDIO_DEVICE_NAME,
                /* type= */ TYPE_BUILTIN_SPEAKER,
                /* isActive= */ true,
                /* isSelected= */ true);
        CarAudioZoneConfigInfo leAudioZoneConfig = createZoneConfig(
                /* name= */ LE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* deviceName= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* type= */ TYPE_BLE_HEADSET,
                /* isActive= */ true,
                /* isSelected= */ false);
        // Multicast LE Device is NOT the unicast fallback and VCP NOT supported => Use Car Audio.
        CachedBluetoothDevice leAudioBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME_3,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_3,
                /* device= */ mReceivingBroadcastBluetoothDevice3,
                /* isConnectedA2dp= */ false,
                /* isConnectedLeAudio= */ true,
                /* isActiveA2dp= */ false,
                /* isActiveLeAudio= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(deviceZoneConfig, leAudioZoneConfig),
                /* cachedDevices= */ List.of(leAudioBluetoothDevice)
        );
        setupLeBroadcast(BT_LE_BROADCAST_ADDRESS);
        setupBroadcastReceivingDevice(mReceivingBroadcastBluetoothDevice3);
        // No fallback match for Device, VCP NOT supported.
        when(leAudioBluetoothDevice.getGroupId()).thenReturn(30);
        when(mLeAudioProfile.getBroadcastToUnicastFallbackGroup()).thenReturn(10);
        when(mVolumeControlProfile.getConnectionStatus(mReceivingBroadcastBluetoothDevice3))
                .thenReturn(BluetoothProfile.STATE_DISCONNECTED);
        when(mCarAudioManager.getVolumeGroupIdForUsage(TEST_ZONE_ID, USAGE)).thenReturn(1);
        when(mCarAudioManager.getGroupMaxVolume(1)).thenReturn(100);
        when(mCarAudioManager.getGroupMinVolume(1)).thenReturn(0);
        when(mCarAudioManager.getGroupVolume(1)).thenReturn(50);

        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);
        Map<String, AudioRouteItem> audioRouteItems = mAudioRoutesManager.getActiveRoutes();

        // Check Scenario (Non-fallback, Non-VCP)
        AudioRouteItem leAudioRoute = audioRouteItems.get(BT_LE_AUDIO_DEVICE_ADDRESS_3);
        assertThat(leAudioRoute.getVolumeState().getMaxVolume()).isEqualTo(100);
        assertThat(leAudioRoute.getVolumeState().getMinVolume()).isEqualTo(0);
        assertThat(leAudioRoute.getVolumeState().getCurrentVolume()).isEqualTo(50);
        assertThat(leAudioRoute.getVolumeState().useVolumeControlProfile()).isFalse();
    }

    @Test
    public void createAudioRoutes_copiesPreviousState() {
        // Set up initial cached routes with a specific state
        AudioRouteItem.State initialState = AudioRouteItem.State.UNICAST_READY;
        AudioDeviceAttributes audioDeviceAttributes = mock(AudioDeviceAttributes.class);
        AudioRouteItem initialAudioRoute = new AudioRouteItem.Builder(audioDeviceAttributes)
                .setState(initialState)
                .build();
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);
        mAudioRoutesManager.getActiveRoutes().put(AUDIO_DEVICE_ADDRESS, initialAudioRoute);

        // Trigger update
        Map<String, AudioRouteItem> audioRouteItems = mAudioRoutesManager.getActiveRoutes();

        // Verify that the state is copied
        AudioRouteItem deviceAudioRoute = audioRouteItems.get(AUDIO_DEVICE_ADDRESS);
        assertThat(deviceAudioRoute.getState()).isEqualTo(initialState);
    }

    @Test
    public void createAudioRoutes_choosesSelectedConfigIfDuplicated() {
        CarAudioZoneConfigInfo unselectedConfig = createZoneConfig(
                /* name= */ DEVICE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ AUDIO_DEVICE_ADDRESS,
                /* deviceName= */ AUDIO_DEVICE_NAME,
                /* type= */ TYPE_BUILTIN_SPEAKER,
                /* isActive= */ true,
                /* isSelected= */ false);
        CarAudioZoneConfigInfo selectedConfig = createZoneConfig(
                /* name= */ DEVICE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ AUDIO_DEVICE_ADDRESS,
                /* deviceName= */ AUDIO_DEVICE_NAME,
                /* type= */ TYPE_BUILTIN_SPEAKER,
                /* isActive= */ true,
                /* isSelected= */ true);
        when(mCarAudioManager.getAudioZoneConfigInfos(TEST_ZONE_ID))
                .thenReturn(List.of(unselectedConfig, selectedConfig));
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        Map<String, AudioRouteItem> audioRouteItems = mAudioRoutesManager.getActiveRoutes();

        // Verify that the selected config wins
        AudioRouteItem deviceAudioRoute = audioRouteItems.get(AUDIO_DEVICE_ADDRESS);
        assertThat(deviceAudioRoute.getAudioZoneConfigState().isSelected()).isTrue();
    }

    @Test
    public void getDeviceName_returnsCorrectName() {
        CarAudioZoneConfigInfo deviceZoneConfig = createZoneConfig(
                /* name= */ DEVICE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ AUDIO_DEVICE_ADDRESS,
                /* deviceName= */ AUDIO_DEVICE_NAME,
                /* type= */ TYPE_BUILTIN_SPEAKER,
                /* isActive= */ true,
                /* isSelected= */ true);
        CarAudioZoneConfigInfo leBroadcastZoneConfig = createZoneConfig(
                /* name= */ LE_BROADCAST_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_BROADCAST_ADDRESS,
                /* deviceName= */ BT_LE_BROADCAST_DEVICE_NAME,
                /* type= */ TYPE_BLE_BROADCAST,
                /* isActive= */ true,
                /* isSelected= */ false);
        when(mCarAudioManager.getAudioZoneConfigInfos(TEST_ZONE_ID))
                .thenReturn(List.of(deviceZoneConfig, leBroadcastZoneConfig));
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        assertThat(mAudioRoutesManager.getDeviceName(AUDIO_DEVICE_ADDRESS)).isEqualTo(
                AUDIO_DEVICE_NAME);
        assertThat(mAudioRoutesManager.getDeviceName(BT_LE_BROADCAST_ADDRESS)).isEqualTo(
                "ble audio broadcast");
        assertThat(mAudioRoutesManager.getDeviceName("invalid_address")).isNull();
    }

    @Test
    public void getDeviceName_removesSpecialCharacters() {
        CarAudioZoneConfigInfo deviceZoneConfig = createZoneConfig(
                /* name= */ DEVICE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ AUDIO_DEVICE_ADDRESS,
                /* deviceName= */ "audio%device",
                /* type= */ TYPE_BUILTIN_SPEAKER,
                /* isActive= */ true,
                /* isSelected= */ false);
        when(mCarAudioManager.getAudioZoneConfigInfos(TEST_ZONE_ID))
                .thenReturn(List.of(deviceZoneConfig));
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        assertThat(mAudioRoutesManager.getDeviceName(AUDIO_DEVICE_ADDRESS)).isEqualTo(
                "audiodevice");
    }

    @Test
    public void isAudioRoutingEnabled_trueWhenFeatureEnabled() {
        when(mCarAudioManager.isAudioFeatureEnabled(AUDIO_FEATURE_DYNAMIC_ROUTING)).thenReturn(
                true);
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        assertThat(mAudioRoutesManager.isAudioRoutingEnabled()).isTrue();
    }

    @Test
    public void isAudioRoutingEnabled_falseWhenFeatureDisabled() {
        when(mCarAudioManager.isAudioFeatureEnabled(AUDIO_FEATURE_DYNAMIC_ROUTING)).thenReturn(
                false);
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        assertThat(mAudioRoutesManager.isAudioRoutingEnabled()).isFalse();
    }

    @Test
    public void leBroadcastMethods_returnCorrectly() {
        CarAudioZoneConfigInfo deviceZoneConfig = createZoneConfig(
                /* name= */ DEVICE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ AUDIO_DEVICE_ADDRESS,
                /* deviceName= */ AUDIO_DEVICE_NAME,
                /* type= */ TYPE_BUILTIN_SPEAKER,
                /* isActive= */ true,
                /* isSelected= */ true);
        CarAudioZoneConfigInfo leBroadcastZoneConfig = createZoneConfig(
                /* name= */ LE_BROADCAST_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_BROADCAST_ADDRESS,
                /* deviceName= */ BT_LE_BROADCAST_DEVICE_NAME,
                /* type= */ TYPE_BLE_BROADCAST,
                /* isActive= */ true,
                /* isSelected= */ false);
        when(mCarAudioManager.getAudioZoneConfigInfos(TEST_ZONE_ID))
                .thenReturn(List.of(deviceZoneConfig, leBroadcastZoneConfig));
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        assertThat(mAudioRoutesManager.isLeBroadcast(BT_LE_BROADCAST_ADDRESS)).isTrue();
        assertThat(mAudioRoutesManager.isLeBroadcast(AUDIO_DEVICE_ADDRESS)).isFalse();
    }

    @Test
    public void setUnicast_switchesAudioRoute() {
        CarAudioZoneConfigInfo deviceZoneConfig = createZoneConfig(
                /* name= */ DEVICE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ AUDIO_DEVICE_ADDRESS,
                /* deviceName= */ AUDIO_DEVICE_NAME,
                /* type= */ TYPE_BUILTIN_SPEAKER,
                /* isActive= */ true,
                /* isSelected= */ true);
        CarAudioZoneConfigInfo a2dpZoneConfig = createZoneConfig(
                /* name= */ A2DP_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_A2DP_DEVICE_ADDRESS,
                /* deviceName= */ BT_A2DP_DEVICE_NAME,
                /* type= */ TYPE_BLUETOOTH_A2DP,
                /* isActive= */ false,
                /* isSelected= */ false);
        // This one overlaps A2DP_AUDIO_ZONE_CONFIG_NAME
        CachedBluetoothDevice a2dpBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_A2DP_DEVICE_NAME,
                /* address= */ BT_A2DP_DEVICE_ADDRESS,
                /* device= */ null,
                /* isConnectedA2dp= */ true,
                /* isConnectedLeAudio= */false,
                /* isActiveA2dp= */ false,
                /* isActiveLeAudio= */ false);
        CachedBluetoothDevice leAudioBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* device= */ mReceivingBroadcastBluetoothDevice1,
                /* isConnectedA2dp= */ true,
                /* isConnectedLeAudio= */ true,
                /* isActiveA2dp= */ false,
                /* isActiveLeAudio= */ false);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(deviceZoneConfig, a2dpZoneConfig),
                /* cachedDevices= */ List.of(a2dpBluetoothDevice, leAudioBluetoothDevice)
        );

        // Initial state.
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);
        // State change notification 1.
        // AUDIO_DEVICE: UNICAST_ACTIVE
        // BT_A2DP_DEVICE: UNICAST_READY
        mAudioRoutesManager.setAudioRoutesUpdateListener(mListener);

        // setUnicast: AUDIO_DEVICE_ADDRESS -> BT_A2DP_DEVICE_ADDRESS
        // State change notification 2.
        // AUDIO_DEVICE: UNICAST_ACTIVE
        // BT_A2DP_DEVICE: STARTING_UNICAST
        mAudioRoutesManager.setUnicast(BT_A2DP_DEVICE_ADDRESS);

        // Simulate audio zone config for BT_A2DP_DEVICE is activated.
        when(a2dpZoneConfig.isActive()).thenReturn(true);
        // This does not invoke the callback because the state is still STARTING_UNICAST until
        // the BT device is selected.
        mAudioRoutesManager.updateAndNotifyAudioRouteItemsIfChanged();

        // Simulate audio zone config for BT_A2DP_DEVICE is selected.
        when(deviceZoneConfig.isSelected()).thenReturn(false);
        when(a2dpZoneConfig.isSelected()).thenReturn(true);
        // State change notification 3.
        // AUDIO_DEVICE: UNICAST_READY
        // BT_A2DP_DEVICE: UNICAST_ACTIVE
        mAudioRoutesManager.updateAndNotifyAudioRouteItemsIfChanged();

        // Verify the callback were invoked three times for the state changes.
        verify(mListener, times(3)).onAudioRoutesUpdated(mAudioRoutesCaptor.capture());
        List<List<AudioRouteItem>> allCapturedRoutes = mAudioRoutesCaptor.getAllValues();
        verifyAudioRoutesState(allCapturedRoutes.get(0),
                Map.of(AUDIO_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_ACTIVE,
                        BT_A2DP_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY,
                        BT_LE_AUDIO_DEVICE_ADDRESS_1, AudioRouteItem.State.UNICAST_READY));
        verifyAudioRoutesState(allCapturedRoutes.get(1),
                Map.of(AUDIO_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_ACTIVE,
                        BT_A2DP_DEVICE_ADDRESS, AudioRouteItem.State.STARTING_UNICAST,
                        BT_LE_AUDIO_DEVICE_ADDRESS_1, AudioRouteItem.State.UNICAST_READY));
        verifyAudioRoutesState(allCapturedRoutes.get(2),
                Map.of(AUDIO_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY,
                        BT_A2DP_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_ACTIVE,
                        BT_LE_AUDIO_DEVICE_ADDRESS_1, AudioRouteItem.State.UNICAST_READY));
        verify(mCarAudioManager).switchAudioZoneToConfig(eq(a2dpZoneConfig), any(), any());
    }

    @Test
    public void setUnicast_switchesWhatCarAudioSelected_insteadOfPreviousOne_ifFailed() {
        setAudioSharing(false);
        CarAudioZoneConfigInfo deviceZoneConfig = createZoneConfig(
                /* name= */ DEVICE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ AUDIO_DEVICE_ADDRESS,
                /* deviceName= */ AUDIO_DEVICE_NAME,
                /* type= */ TYPE_BUILTIN_SPEAKER,
                /* isActive= */ true,
                /* isSelected= */ false);
        CarAudioZoneConfigInfo a2dpZoneConfig = createZoneConfig(
                /* name= */ A2DP_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_A2DP_DEVICE_ADDRESS,
                /* deviceName= */ BT_A2DP_DEVICE_NAME,
                /* type= */ TYPE_BLUETOOTH_A2DP,
                /* isActive= */ true,
                /* isSelected= */ true);
        // This one overlaps A2DP_AUDIO_ZONE_CONFIG_NAME
        CachedBluetoothDevice a2dpBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_A2DP_DEVICE_NAME,
                /* address= */ BT_A2DP_DEVICE_ADDRESS,
                /* device= */ null,
                /* isConnectedA2dp= */ true,
                /* isConnectedLeAudio= */true,
                /* isActiveA2dp= */ true,
                /* isActiveLeAudio= */ true);
        CarAudioZoneConfigInfo leAudioZoneConfig = createZoneConfig(
                /* name= */ LE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* deviceName= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* type= */ TYPE_BLE_HEADSET,
                /* isActive= */ false,
                /* isSelected= */ false);
        // This one overlaps LE_AUDIO_ZONE_CONFIG_NAME
        CachedBluetoothDevice leAudioBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* device= */ null,
                /* isConnectedA2dp= */ true,
                /* isConnectedLeAudio= */ true,
                /* isActiveA2dp= */ false,
                /* isActiveLeAudio= */ false);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(deviceZoneConfig, a2dpZoneConfig, leAudioZoneConfig),
                /* cachedDevices= */ List.of(a2dpBluetoothDevice, leAudioBluetoothDevice)
        );

        // Initial state.
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);
        // State change notification 1.
        // AUDIO_DEVICE: UNICAST_READY
        // BT_A2DP_DEVICE: UNICAST_ACTIVE
        // BT_LE_AUDIO_DEVICE: UNICAST_READY
        mAudioRoutesManager.setAudioRoutesUpdateListener(mListener);

        // setUnicast: BT_A2DP_DEVICE -> BT_LE_AUDIO_DEVICE_ADDRESS_1
        // State change notification 2.
        // AUDIO_DEVICE: UNICAST_READY
        // BT_A2DP_DEVICE: UNICAST_ACTIVE
        // BT_LE_AUDIO_DEVICE: STARTING_UNICAST
        mAudioRoutesManager.setUnicast(BT_LE_AUDIO_DEVICE_ADDRESS_1);

        // cancelStartingUnicast (BT_LE_AUDIO_DEVICE_ADDRESS_1)
        // State change notification 3.
        // AUDIO_DEVICE: UNICAST_ACTIVE (Car Audio indicates this is the new selected audio route).
        // BT_A2DP_DEVICE: UNICAST_READY (Previous one but not selected)
        // BT_LE_AUDIO_DEVICE: UNICAST_READY
        when(deviceZoneConfig.isSelected()).thenReturn(true);
        when(a2dpZoneConfig.isSelected()).thenReturn(false);
        when(leAudioZoneConfig.isSelected()).thenReturn(false);
        mAudioRoutesManager.cancelStartingUnicast();

        assertShowingToast(mContext.getString(R.string.audio_route_preference_connecting_failed,
                BT_LE_AUDIO_DEVICE_NAME_1));
        // Verify the callback were invoked three times for the state changes.
        verify(mListener, times(3)).onAudioRoutesUpdated(mAudioRoutesCaptor.capture());
        List<List<AudioRouteItem>> allCapturedRoutes = mAudioRoutesCaptor.getAllValues();
        verifyAudioRoutesState(allCapturedRoutes.get(0),
                Map.of(AUDIO_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY,
                        BT_A2DP_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_ACTIVE,
                        BT_LE_AUDIO_DEVICE_ADDRESS_1, AudioRouteItem.State.UNICAST_READY));
        verifyAudioRoutesState(allCapturedRoutes.get(1),
                Map.of(AUDIO_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY,
                        BT_A2DP_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_ACTIVE,
                        BT_LE_AUDIO_DEVICE_ADDRESS_1, AudioRouteItem.State.STARTING_UNICAST));
        verifyAudioRoutesState(allCapturedRoutes.get(2),
                Map.of(AUDIO_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_ACTIVE,
                        BT_A2DP_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY,
                        BT_LE_AUDIO_DEVICE_ADDRESS_1, AudioRouteItem.State.UNICAST_READY));
    }

    @Test
    public void joinBroadcast_joinsBroadcast() {
        CarAudioZoneConfigInfo a2dpZoneConfig = createZoneConfig(
                /* name= */ A2DP_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_A2DP_DEVICE_ADDRESS,
                /* deviceName= */ BT_A2DP_DEVICE_NAME,
                /* type= */ TYPE_BLUETOOTH_A2DP,
                /* isActive= */ true,
                /* isSelected= */ false);
        CarAudioZoneConfigInfo leAudioZoneConfig1 = createZoneConfig(
                /* name= */ LE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* deviceName= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* type= */ TYPE_BLE_HEADSET,
                /* isActive= */ true,
                /* isSelected= */ true);
        CachedBluetoothDevice a2dpBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_A2DP_DEVICE_NAME,
                /* address= */ BT_A2DP_DEVICE_ADDRESS,
                /* device= */ null,
                /* isConnectedA2dp= */ true,
                /* isConnectedLeAudio= */false,
                /* isActiveA2dp= */ false,
                /* isActiveLeAudio= */ false);
        CarAudioZoneConfigInfo leBroadcastZoneConfig = createZoneConfig(
                /* name= */ LE_BROADCAST_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_BROADCAST_ADDRESS,
                /* deviceName= */ BT_LE_BROADCAST_DEVICE_NAME,
                /* type= */ TYPE_BLE_BROADCAST,
                /* isActive= */ true,
                /* isSelected= */ false);
        CachedBluetoothDevice leAudioBluetoothDevice1 = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* device= */ mReceivingBroadcastBluetoothDevice1,
                /* isConnectedA2dp= */ true,
                /* isConnectedLeAudio= */ true,
                /* isActiveA2dp= */ true,
                /* isActiveLeAudio= */ true);
        CachedBluetoothDevice leAudioBluetoothDevice2 = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME_2,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_2,
                /* device= */ mReceivingBroadcastBluetoothDevice2,
                /* isConnectedA2dp= */ true,
                /* isConnectedLeAudio= */ true,
                /* isActiveA2dp= */ true,
                /* isActiveLeAudio= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */List.of(a2dpZoneConfig, leAudioZoneConfig1, leBroadcastZoneConfig),
                /* cachedDevices= */
                List.of(a2dpBluetoothDevice, leAudioBluetoothDevice1, leAudioBluetoothDevice2)
        );
        setupLeBroadcast(BT_LE_BROADCAST_ADDRESS);

        // Initial state.
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);
        // State change notification 1.
        // BT_LE_AUDIO_DEVICE_1: UNICAST_ACTIVE
        // BT_LE_AUDIO_DEVICE_2: MULTICAST_READY_UNICAST_READY
        // BT_LE_BROADCAST: BROADCAST_READY
        mAudioRoutesManager.setAudioRoutesUpdateListener(mListener);

        // joinBroadcast: BT_LE_AUDIO_DEVICE_2
        // State change notification 2.
        // BT_LE_AUDIO_DEVICE_1: UNICAST_ACTIVE
        // BT_LE_AUDIO_DEVICE_2: STARTING_BROADCAST
        // BT_LE_BROADCAST: BROADCAST_READY
        mAudioRoutesManager.joinBroadcast(BT_LE_AUDIO_DEVICE_ADDRESS_2);

        // Simulate audio zone config for BT_LE_BROADCAST is selected.
        when(leBroadcastZoneConfig.isActive()).thenReturn(true);
        when(leBroadcastZoneConfig.isSelected()).thenReturn(true);
        // State change notification 3.
        // BT_LE_AUDIO_DEVICE_1: JOINING_BROADCAST
        // BT_LE_AUDIO_DEVICE_2: JOINING_BROADCAST
        // BT_LE_BROADCAST: BROADCAST_ACTIVE
        mAudioRoutesManager.updateAndNotifyAudioRouteItemsIfChanged();

        // Simulate BT_LE_AUDIO_DEVICE_1 joins the broadcast.
        setupBroadcastReceivingDevice(mReceivingBroadcastBluetoothDevice1);
        // State change notification 4.
        // BT_LE_AUDIO_DEVICE_1: MULTICAST_ACTIVE
        // BT_LE_AUDIO_DEVICE_2: JOINING_BROADCAST
        // BT_LE_BROADCAST: BROADCAST_ACTIVE
        mAudioRoutesManager.updateAndNotifyAudioRouteItemsIfChanged();

        // Simulate BT_LE_AUDIO_DEVICE_2 joins the broadcast.
        setupBroadcastReceivingDevice(mReceivingBroadcastBluetoothDevice2);
        // State change notification 5.
        // BT_LE_AUDIO_DEVICE_1: MULTICAST_ACTIVE
        // BT_LE_AUDIO_DEVICE_2: MULTICAST_ACTIVE
        // BT_LE_BROADCAST: BROADCAST_ACTIVE
        mAudioRoutesManager.updateAndNotifyAudioRouteItemsIfChanged();

        verify(mListener, times(5)).onAudioRoutesUpdated(mAudioRoutesCaptor.capture());
        List<List<AudioRouteItem>> allCapturedRoutes = mAudioRoutesCaptor.getAllValues();
        verifyAudioRoutesState(allCapturedRoutes.get(0),
                Map.of(BT_A2DP_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY,
                        BT_LE_AUDIO_DEVICE_ADDRESS_1, AudioRouteItem.State.UNICAST_ACTIVE,
                        BT_LE_AUDIO_DEVICE_ADDRESS_2,
                        AudioRouteItem.State.MULTICAST_READY_UNICAST_READY,
                        BT_LE_BROADCAST_ADDRESS, AudioRouteItem.State.BROADCAST_READY));
        verifyAudioRoutesState(allCapturedRoutes.get(1),
                Map.of(BT_A2DP_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY,
                        BT_LE_AUDIO_DEVICE_ADDRESS_1, AudioRouteItem.State.UNICAST_ACTIVE,
                        BT_LE_AUDIO_DEVICE_ADDRESS_2, AudioRouteItem.State.STARTING_BROADCAST,
                        BT_LE_BROADCAST_ADDRESS, AudioRouteItem.State.BROADCAST_READY));
        verifyAudioRoutesState(allCapturedRoutes.get(2),
                Map.of(BT_A2DP_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY,
                        BT_LE_AUDIO_DEVICE_ADDRESS_1, AudioRouteItem.State.JOINING_BROADCAST,
                        BT_LE_AUDIO_DEVICE_ADDRESS_2, AudioRouteItem.State.JOINING_BROADCAST,
                        BT_LE_BROADCAST_ADDRESS, AudioRouteItem.State.BROADCAST_ACTIVE));
        verifyAudioRoutesState(allCapturedRoutes.get(3),
                Map.of(BT_A2DP_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY,
                        BT_LE_AUDIO_DEVICE_ADDRESS_1, AudioRouteItem.State.MULTICAST_ACTIVE,
                        BT_LE_AUDIO_DEVICE_ADDRESS_2, AudioRouteItem.State.JOINING_BROADCAST,
                        BT_LE_BROADCAST_ADDRESS, AudioRouteItem.State.BROADCAST_ACTIVE));
        verifyAudioRoutesState(allCapturedRoutes.get(4),
                Map.of(BT_A2DP_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY,
                        BT_LE_AUDIO_DEVICE_ADDRESS_1, AudioRouteItem.State.MULTICAST_ACTIVE,
                        BT_LE_AUDIO_DEVICE_ADDRESS_2, AudioRouteItem.State.MULTICAST_ACTIVE,
                        BT_LE_BROADCAST_ADDRESS, AudioRouteItem.State.BROADCAST_ACTIVE));

        verify(mLocalBluetoothLeBroadcastAssistant).addSource(
                eq(mReceivingBroadcastBluetoothDevice1), any(), eq(true));
        verify(mLocalBluetoothLeBroadcastAssistant, times(2)).addSource(
                eq(mReceivingBroadcastBluetoothDevice2), any(), eq(true));
        verify(mCarAudioManager).switchAudioZoneToConfig(eq(leBroadcastZoneConfig), any(), any());
    }

    @Test
    public void leaveBroadcast_leavesBroadcast() {
        CarAudioZoneConfigInfo deviceZoneConfig = createZoneConfig(
                /* name= */ DEVICE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ AUDIO_DEVICE_ADDRESS,
                /* deviceName= */ AUDIO_DEVICE_NAME,
                /* type= */ TYPE_BUILTIN_SPEAKER,
                /* isActive= */ true,
                /* isSelected= */ false);
        CarAudioZoneConfigInfo leAudioZoneConfig1 = createZoneConfig(
                /* name= */ LE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* deviceName= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* type= */ TYPE_BLE_HEADSET,
                /* isActive= */ true,
                /* isSelected= */ false);
        CarAudioZoneConfigInfo leBroadcastZoneConfig = createZoneConfig(
                /* name= */ LE_BROADCAST_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_BROADCAST_ADDRESS,
                /* deviceName= */ BT_LE_BROADCAST_DEVICE_NAME,
                /* type= */ TYPE_BLE_BROADCAST,
                /* isActive= */ true,
                /* isSelected= */ true);
        CachedBluetoothDevice leAudioBluetoothDevice1 = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* device= */ mReceivingBroadcastBluetoothDevice1,
                /* isConnectedA2dp= */ true,
                /* isConnectedLeAudio= */ true,
                /* isActiveA2dp= */ true,
                /* isActiveLeAudio= */ true);
        CachedBluetoothDevice leAudioBluetoothDevice2 = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME_2,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_2,
                /* device= */ mReceivingBroadcastBluetoothDevice2,
                /* isConnectedA2dp= */ true,
                /* isConnectedLeAudio= */ true,
                /* isActiveA2dp= */ true,
                /* isActiveLeAudio= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */
                List.of(deviceZoneConfig, leAudioZoneConfig1, leBroadcastZoneConfig),
                /* cachedDevices= */ List.of(leAudioBluetoothDevice1, leAudioBluetoothDevice2)
        );
        setupLeBroadcast(BT_LE_BROADCAST_ADDRESS);
        setupBroadcastReceivingDevice(mReceivingBroadcastBluetoothDevice1);
        setupBroadcastReceivingDevice(mReceivingBroadcastBluetoothDevice2);

        // Initial state.
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);
        // State change notification 1.
        // AUDIO_DEVICE: UNICAST_READY
        // BT_LE_AUDIO_DEVICE_1: MULTICAST_ACTIVE
        // BT_LE_AUDIO_DEVICE_2: MULTICAST_ACTIVE
        // BT_LE_BROADCAST: BROADCAST_ACTIVE
        mAudioRoutesManager.setAudioRoutesUpdateListener(mListener);

        // leaveBroadcast: BT_LE_AUDIO_DEVICE_ADDRESS_1
        // State change notification 2.
        // AUDIO_DEVICE: UNICAST_READY
        // BT_LE_AUDIO_DEVICE_1: LEAVING_BROADCAST
        // BT_LE_AUDIO_DEVICE_2: MULTICAST_ACTIVE
        // BT_LE_BROADCAST: BROADCAST_ACTIVE
        mAudioRoutesManager.leaveBroadcast(BT_LE_AUDIO_DEVICE_ADDRESS_1);

        // Simulate BT_LE_AUDIO_DEVICE_1 leaves broadcast.
        clearBroadcastReceivingDevice(mReceivingBroadcastBluetoothDevice1);
        // State change notification 3.
        // AUDIO_DEVICE: UNICAST_READY
        // BT_LE_AUDIO_DEVICE_1: MULTICAST_READY_UNICAST_READY
        // BT_LE_AUDIO_DEVICE_2: MULTICAST_ACTIVE
        // BT_LE_BROADCAST: BROADCAST_ACTIVE
        mAudioRoutesManager.updateAndNotifyAudioRouteItemsIfChanged();

        // leaveBroadcast: BT_LE_AUDIO_DEVICE_ADDRESS_2
        // State change notification 4.
        // AUDIO_DEVICE: UNICAST_READY
        // BT_LE_AUDIO_DEVICE_1: MULTICAST_READY_UNICAST_READY
        // BT_LE_AUDIO_DEVICE_2: LEAVING_BROADCAST
        // BT_LE_BROADCAST: BROADCAST_ACTIVE
        mAudioRoutesManager.leaveBroadcast(BT_LE_AUDIO_DEVICE_ADDRESS_2);

        // Simulate BT_LE_AUDIO_DEVICE_2 leaves broadcast.
        clearBroadcastReceivingDevice(mReceivingBroadcastBluetoothDevice2);
        // State change notification 5.
        // AUDIO_DEVICE: UNICAST_READY
        // BT_LE_AUDIO_DEVICE_1: MULTICAST_READY_UNICAST_READY
        // BT_LE_AUDIO_DEVICE_2: MULTICAST_READY_UNICAST_READY
        // BT_LE_BROADCAST: BROADCAST_ACTIVE
        mAudioRoutesManager.updateAndNotifyAudioRouteItemsIfChanged();

        // Simulate BT_LE_BROADCAST closes broadcast.
        when(deviceZoneConfig.isSelected()).thenReturn(true);
        when(leBroadcastZoneConfig.isSelected()).thenReturn(false);
        // State change notification 6.
        // AUDIO_DEVICE: UNICAST_ACTIVE
        // BT_LE_AUDIO_DEVICE_1: UNICAST_READY
        // BT_LE_AUDIO_DEVICE_2: UNICAST_READY
        // BT_LE_BROADCAST: BROADCAST_READY
        mAudioRoutesManager.updateAndNotifyAudioRouteItemsIfChanged();

        verify(mListener, times(6)).onAudioRoutesUpdated(mAudioRoutesCaptor.capture());
        List<List<AudioRouteItem>> allCapturedRoutes = mAudioRoutesCaptor.getAllValues();
        verifyAudioRoutesState(allCapturedRoutes.get(0),
                Map.of(AUDIO_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY,
                        BT_LE_AUDIO_DEVICE_ADDRESS_1, AudioRouteItem.State.MULTICAST_ACTIVE,
                        BT_LE_AUDIO_DEVICE_ADDRESS_2, AudioRouteItem.State.MULTICAST_ACTIVE,
                        BT_LE_BROADCAST_ADDRESS, AudioRouteItem.State.BROADCAST_ACTIVE));
        verifyAudioRoutesState(allCapturedRoutes.get(1),
                Map.of(AUDIO_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY,
                        BT_LE_AUDIO_DEVICE_ADDRESS_1, AudioRouteItem.State.LEAVING_BROADCAST,
                        BT_LE_AUDIO_DEVICE_ADDRESS_2, AudioRouteItem.State.MULTICAST_ACTIVE,
                        BT_LE_BROADCAST_ADDRESS, AudioRouteItem.State.BROADCAST_ACTIVE));
        verifyAudioRoutesState(allCapturedRoutes.get(2),
                Map.of(AUDIO_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY,
                        BT_LE_AUDIO_DEVICE_ADDRESS_1,
                        AudioRouteItem.State.MULTICAST_READY_UNICAST_READY,
                        BT_LE_AUDIO_DEVICE_ADDRESS_2, AudioRouteItem.State.MULTICAST_ACTIVE,
                        BT_LE_BROADCAST_ADDRESS, AudioRouteItem.State.BROADCAST_ACTIVE));
        verifyAudioRoutesState(allCapturedRoutes.get(3),
                Map.of(AUDIO_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY,
                        BT_LE_AUDIO_DEVICE_ADDRESS_1,
                        AudioRouteItem.State.MULTICAST_READY_UNICAST_READY,
                        BT_LE_AUDIO_DEVICE_ADDRESS_2, AudioRouteItem.State.LEAVING_BROADCAST,
                        BT_LE_BROADCAST_ADDRESS, AudioRouteItem.State.BROADCAST_ACTIVE));
        verifyAudioRoutesState(allCapturedRoutes.get(4),
                Map.of(AUDIO_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY,
                        BT_LE_AUDIO_DEVICE_ADDRESS_1,
                        AudioRouteItem.State.MULTICAST_READY_UNICAST_READY,
                        BT_LE_AUDIO_DEVICE_ADDRESS_2,
                        AudioRouteItem.State.MULTICAST_READY_UNICAST_READY,
                        BT_LE_BROADCAST_ADDRESS, AudioRouteItem.State.BROADCAST_ACTIVE));
        verifyAudioRoutesState(allCapturedRoutes.get(5),
                Map.of(AUDIO_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_ACTIVE,
                        BT_LE_AUDIO_DEVICE_ADDRESS_1, AudioRouteItem.State.UNICAST_READY,
                        BT_LE_AUDIO_DEVICE_ADDRESS_2, AudioRouteItem.State.UNICAST_READY,
                        BT_LE_BROADCAST_ADDRESS, AudioRouteItem.State.BROADCAST_READY));
    }

    @Test
    public void tearDown_clearsCallback() {
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);
        mAudioRoutesManager.tearDown();

        /* Verifies that {@link CarAudioManager#clearAudioZoneConfigsCallback()} is called in
          the constructor and {@link #tearDown()}. */
        verify(mCarAudioManager, times(2)).clearAudioZoneConfigsCallback();
    }

    @Test
    public void setVolume_usesVolumeControlProfile_withScaledValue() {
        CarAudioZoneConfigInfo deviceZoneConfig = createZoneConfig(
                /* name= */ DEVICE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ AUDIO_DEVICE_ADDRESS,
                /* deviceName= */ AUDIO_DEVICE_NAME,
                /* type= */ TYPE_BUILTIN_SPEAKER,
                /* isActive= */ true,
                /* isSelected= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(deviceZoneConfig),
                /* cachedDevices= */ List.of()
        );
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);
        AudioRouteItem item = mAudioRoutesManager.getActiveRoutes().get(AUDIO_DEVICE_ADDRESS);
        AudioRouteItem.VolumeState volumeState = mock(AudioRouteItem.VolumeState.class);
        when(volumeState.useVolumeControlProfile()).thenReturn(true);
        BluetoothDevice bluetoothDevice = mock(BluetoothDevice.class);
        CachedBluetoothDevice cachedBluetoothDevice = mock(CachedBluetoothDevice.class);
        when(cachedBluetoothDevice.getDevice()).thenReturn(bluetoothDevice);
        item = new AudioRouteItem.Builder(mAudioDeviceAttributes)
                .setName(item.getName())
                .setState(item.getState())
                .setVolumeState(volumeState)
                .setBluetoothDevice(cachedBluetoothDevice)
                .setAudioZoneConfigState(item.getAudioZoneConfigState())
                .setBluetoothDeviceState(item.getBluetoothDeviceState())
                .setGlobalState(item.getGlobalState())
                .build();
        mAudioRoutesManager.getActiveRoutes().put(AUDIO_DEVICE_ADDRESS, item);
        when(mCarAudioManager.getVolumeGroupIdForUsage(TEST_ZONE_ID, USAGE)).thenReturn(1);
        when(mCarAudioManager.getGroupMaxVolume(1)).thenReturn(100);
        when(mCarAudioManager.getGroupMinVolume(1)).thenReturn(0);

        mAudioRoutesManager.setVolume(AUDIO_DEVICE_ADDRESS, 50);

        verify(mVolumeControlProfile).setDeviceVolume(eq(bluetoothDevice), eq(128), eq(true));
    }

    @Test
    public void setVolume_usesCarAudioManager() {
        CarAudioZoneConfigInfo deviceZoneConfig = createZoneConfig(
                /* name= */ DEVICE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ AUDIO_DEVICE_ADDRESS,
                /* deviceName= */ AUDIO_DEVICE_NAME,
                /* type= */ TYPE_BUILTIN_SPEAKER,
                /* isActive= */ true,
                /* isSelected= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(deviceZoneConfig),
                /* cachedDevices= */ List.of()
        );
        when(mCarAudioManager.getVolumeGroupIdForUsage(TEST_ZONE_ID, USAGE)).thenReturn(1);
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);
        AudioRouteItem item = mAudioRoutesManager.getActiveRoutes().get(AUDIO_DEVICE_ADDRESS);
        AudioRouteItem.VolumeState volumeState = mock(AudioRouteItem.VolumeState.class);
        when(volumeState.useVolumeControlProfile()).thenReturn(false);
        item = new AudioRouteItem.Builder(mAudioDeviceAttributes)
                .setName(item.getName())
                .setState(item.getState())
                .setVolumeState(volumeState)
                .setBluetoothDevice(item.getBluetoothDevice())
                .setAudioZoneConfigState(item.getAudioZoneConfigState())
                .setBluetoothDeviceState(item.getBluetoothDeviceState())
                .setGlobalState(item.getGlobalState())
                .build();
        mAudioRoutesManager.getActiveRoutes().put(AUDIO_DEVICE_ADDRESS, item);

        mAudioRoutesManager.setVolume(AUDIO_DEVICE_ADDRESS, 50);

        verify(mCarAudioManager).setGroupVolume(eq(TEST_ZONE_ID), eq(1), eq(50), anyInt());
    }

    @Test
    public void onDeviceVolumeChanged_updatesCacheAndNotifies() {
        CarAudioZoneConfigInfo leAudioZoneConfig = createZoneConfig(
                /* name= */ LE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* deviceName= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* type= */ TYPE_BLE_HEADSET,
                /* isActive= */ true,
                /* isSelected= */ false);
        CachedBluetoothDevice leAudioBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* device= */ mReceivingBroadcastBluetoothDevice1,
                /* isConnectedA2dp= */ false,
                /* isConnectedLeAudio= */ true,
                /* isActiveA2dp= */ false,
                /* isActiveLeAudio= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(leAudioZoneConfig),
                /* cachedDevices= */ List.of(leAudioBluetoothDevice)
        );
        setupLeBroadcast(BT_LE_BROADCAST_ADDRESS);
        setupBroadcastReceivingDevice(mReceivingBroadcastBluetoothDevice1);
        when(mReceivingBroadcastBluetoothDevice1.getAddress()).thenReturn(
                BT_LE_AUDIO_DEVICE_ADDRESS_1);
        setupVolumeControlConnectionStatus(mReceivingBroadcastBluetoothDevice1,
                BluetoothProfile.STATE_CONNECTED);
        when(leAudioBluetoothDevice.getGroupId()).thenReturn(10);
        when(mLeAudioProfile.getBroadcastToUnicastFallbackGroup()).thenReturn(20);
        when(mCarAudioManager.getVolumeGroupIdForUsage(TEST_ZONE_ID, USAGE)).thenReturn(1);
        when(mCarAudioManager.getGroupMaxVolume(1)).thenReturn(100);
        when(mCarAudioManager.getGroupMinVolume(1)).thenReturn(0);
        // Initial volume
        when(mCarAudioManager.getGroupVolume(1)).thenReturn(50);
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);
        mAudioRoutesManager.setAudioRoutesUpdateListener(mListener);
        // Capture the callback
        verify(mVolumeControlProfile).registerCallback(any(),
                mVolumeControlCallbackCaptor.capture());
        BluetoothVolumeControl.Callback callback = mVolumeControlCallbackCaptor.getValue();

        // Trigger volume change
        // Volume 200 (scaled from 0-255) to 0-100 -> ~78
        // Scale logic: (200 - 0) * (100 - 0) / (255 - 0) + 0 = 78.43 -> 78
        callback.onDeviceVolumeChanged(mReceivingBroadcastBluetoothDevice1, 200);

        verify(mListener, times(2)).onAudioRoutesUpdated(mAudioRoutesCaptor.capture());
        List<AudioRouteItem> routes = mAudioRoutesCaptor.getValue();
        AudioRouteItem item = routes.stream().filter(
                i -> i.getAddress().equals(BT_LE_AUDIO_DEVICE_ADDRESS_1)).findFirst().get();
        // The volume state in the item should be the SCALED volume.
        assertThat(item.getVolumeState().getMaxVolume()).isEqualTo(100);
        assertThat(item.getVolumeState().getMinVolume()).isEqualTo(0);
        assertThat(item.getVolumeState().getCurrentVolume()).isEqualTo(78);
    }

    @Test
    public void onGroupVolumeChanged_updatesCacheAndNotifies() {
        CarAudioZoneConfigInfo deviceZoneConfig = createZoneConfig(
                /* name= */ DEVICE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ AUDIO_DEVICE_ADDRESS,
                /* deviceName= */ AUDIO_DEVICE_NAME,
                /* type= */ TYPE_BUILTIN_SPEAKER,
                /* isActive= */ true,
                /* isSelected= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(deviceZoneConfig),
                /* cachedDevices= */ List.of()
        );
        when(mCarAudioManager.getVolumeGroupIdForUsage(TEST_ZONE_ID, USAGE)).thenReturn(1);
        when(mCarAudioManager.getGroupMaxVolume(1)).thenReturn(100);
        when(mCarAudioManager.getGroupMinVolume(1)).thenReturn(0);
        when(mCarAudioManager.getGroupVolume(1)).thenReturn(50);
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);
        mAudioRoutesManager.setAudioRoutesUpdateListener(mListener);
        // Capture the callback
        verify(mCarAudioManager).registerCarVolumeCallback(mCarVolumeCallbackCaptor.capture());
        CarAudioManager.CarVolumeCallback callback = mCarVolumeCallbackCaptor.getValue();

        // Trigger volume change
        when(mCarAudioManager.getGroupVolume(1)).thenReturn(60);
        callback.onGroupVolumeChanged(TEST_ZONE_ID, 1, 0);

        verify(mListener, times(2)).onAudioRoutesUpdated(mAudioRoutesCaptor.capture());
        List<AudioRouteItem> routes = mAudioRoutesCaptor.getValue();
        AudioRouteItem item = routes.stream().filter(
                i -> i.getAddress().equals(AUDIO_DEVICE_ADDRESS)).findFirst().get();
        assertThat(item.getVolumeState().getMaxVolume()).isEqualTo(100);
        assertThat(item.getVolumeState().getMinVolume()).isEqualTo(0);
        assertThat(item.getVolumeState().getCurrentVolume()).isEqualTo(60);
    }

    @Test
    public void testCreatedStateTransitions_toUnicastReady() {
        CarAudioZoneConfigInfo a2dpZoneConfig = createZoneConfig(
                /* name= */ A2DP_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_A2DP_DEVICE_ADDRESS,
                /* deviceName= */ BT_A2DP_DEVICE_NAME,
                /* type= */ TYPE_BLUETOOTH_A2DP,
                /* isActive= */ false,
                /* isSelected= */ false);
        CachedBluetoothDevice a2dpBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_A2DP_DEVICE_NAME,
                /* address= */ BT_A2DP_DEVICE_ADDRESS,
                /* device= */ null,
                /* isConnectedA2dp= */ true,
                /* isConnectedLeAudio= */ false,
                /* isActiveA2dp= */ false,
                /* isActiveLeAudio= */ false);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(a2dpZoneConfig),
                /* cachedDevices= */ List.of(a2dpBluetoothDevice)
        );
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        Map<String, AudioRouteItem> audioRouteItems = mAudioRoutesManager.getActiveRoutes();

        assertThat(audioRouteItems.get(BT_A2DP_DEVICE_ADDRESS).getState()).isEqualTo(
                AudioRouteItem.State.UNICAST_READY);
    }

    @Test
    public void testCreatedStateTransitions_toBroadcastReady() {
        CarAudioZoneConfigInfo leBroadcastZoneConfig = createZoneConfig(
                /* name= */ LE_BROADCAST_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_BROADCAST_ADDRESS,
                /* deviceName= */ BT_LE_BROADCAST_DEVICE_NAME,
                /* type= */ TYPE_BLE_BROADCAST,
                /* isActive= */ true,
                /* isSelected= */ false);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(leBroadcastZoneConfig),
                /* cachedDevices= */ List.of());
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        Map<String, AudioRouteItem> audioRouteItems = mAudioRoutesManager.getActiveRoutes();

        assertThat(audioRouteItems.get(BT_LE_BROADCAST_ADDRESS).getState()).isEqualTo(
                AudioRouteItem.State.BROADCAST_READY);
    }

    @Test
    public void testCreatedStateTransitions_toBroadcastActive() {
        CarAudioZoneConfigInfo leBroadcastZoneConfig = createZoneConfig(
                /* name= */ LE_BROADCAST_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_BROADCAST_ADDRESS,
                /* deviceName= */ BT_LE_BROADCAST_DEVICE_NAME,
                /* type= */ TYPE_BLE_BROADCAST,
                /* isActive= */ true,
                /* isSelected= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(leBroadcastZoneConfig),
                /* cachedDevices= */ List.of());
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        Map<String, AudioRouteItem> audioRouteItems = mAudioRoutesManager.getActiveRoutes();

        assertThat(audioRouteItems.get(BT_LE_BROADCAST_ADDRESS).getState()).isEqualTo(
                AudioRouteItem.State.BROADCAST_ACTIVE);
    }

    @Test
    public void testCreatedStateTransitions_toMulticastActive() {
        CarAudioZoneConfigInfo leAudioZoneConfig = createZoneConfig(
                /* name= */ LE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* deviceName= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* type= */ TYPE_BLE_HEADSET,
                /* isActive= */ true,
                /* isSelected= */ false);
        CachedBluetoothDevice leAudioBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* device= */ mReceivingBroadcastBluetoothDevice1,
                /* isConnectedA2dp= */ false,
                /* isConnectedLeAudio= */ true,
                /* isActiveA2dp= */ false,
                /* isActiveLeAudio= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(leAudioZoneConfig),
                /* cachedDevices= */ List.of(leAudioBluetoothDevice));
        setupLeBroadcast(BT_LE_BROADCAST_ADDRESS);
        setupBroadcastReceivingDevice(mReceivingBroadcastBluetoothDevice1);
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        Map<String, AudioRouteItem> audioRouteItems = mAudioRoutesManager.getActiveRoutes();

        assertThat(audioRouteItems.get(BT_LE_AUDIO_DEVICE_ADDRESS_1).getState()).isEqualTo(
                AudioRouteItem.State.MULTICAST_ACTIVE);
    }

    @Test
    public void testCreatedStateTransitions_toUnicastActive() {
        CarAudioZoneConfigInfo a2dpZoneConfig = createZoneConfig(
                /* name= */ A2DP_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_A2DP_DEVICE_ADDRESS,
                /* deviceName= */ BT_A2DP_DEVICE_NAME,
                /* type= */ TYPE_BLUETOOTH_A2DP,
                /* isActive= */ true,
                /* isSelected= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(a2dpZoneConfig),
                /* cachedDevices= */ List.of());
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        Map<String, AudioRouteItem> audioRouteItems = mAudioRoutesManager.getActiveRoutes();

        assertThat(audioRouteItems.get(BT_A2DP_DEVICE_ADDRESS).getState()).isEqualTo(
                AudioRouteItem.State.UNICAST_ACTIVE);
    }

    @Test
    public void testCreatedStateTransitions_toMulticastReadyUnicastActive() {
        CarAudioZoneConfigInfo leAudioZoneConfig = createZoneConfig(
                /* name= */ LE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* deviceName= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* type= */ TYPE_BLE_HEADSET,
                /* isActive= */ true,
                /* isSelected= */ true);
        CarAudioZoneConfigInfo leBroadcastZoneConfig = createZoneConfig(
                /* name= */ LE_BROADCAST_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_BROADCAST_ADDRESS,
                /* deviceName= */ BT_LE_BROADCAST_DEVICE_NAME,
                /* type= */ TYPE_BLE_BROADCAST,
                /* isActive= */ true,
                /* isSelected= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(leAudioZoneConfig, leBroadcastZoneConfig),
                /* cachedDevices= */ List.of());
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        Map<String, AudioRouteItem> audioRouteItems = mAudioRoutesManager.getActiveRoutes();

        assertThat(audioRouteItems.get(BT_LE_AUDIO_DEVICE_ADDRESS_1).getState()).isEqualTo(
                AudioRouteItem.State.MULTICAST_READY_UNICAST_ACTIVE);
    }

    @Test
    public void testStateTransitions_toMulticastReadyUnicastReady_whenAlreadyBroadcasting() {
        setAudioSharing(true);
        CarAudioZoneConfigInfo leAudioZoneConfig = createZoneConfig(
                /* name= */ LE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* deviceName= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* type= */ TYPE_BLE_HEADSET,
                /* isActive= */ false,
                /* isSelected= */ false);
        CachedBluetoothDevice leAudioBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* device= */ mReceivingBroadcastBluetoothDevice1,
                /* isConnectedA2dp= */ false,
                /* isConnectedLeAudio= */ true,
                /* isActiveA2dp= */ false,
                /* isActiveLeAudio= */ true);
        CarAudioZoneConfigInfo selectedLeAudioZoneConfig = createZoneConfig(
                /* name= */ LE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_2,
                /* deviceName= */ BT_LE_AUDIO_DEVICE_NAME_2,
                /* type= */ TYPE_BLE_HEADSET,
                /* isActive= */ true,
                /* isSelected= */ true);
        CachedBluetoothDevice selectedLeAudioBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME_2,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_2,
                /* device= */ mReceivingBroadcastBluetoothDevice2,
                /* isConnectedA2dp= */ false,
                /* isConnectedLeAudio= */ true,
                /* isActiveA2dp= */ false,
                /* isActiveLeAudio= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(leAudioZoneConfig, selectedLeAudioZoneConfig),
                /* cachedDevices= */
                List.of(leAudioBluetoothDevice, selectedLeAudioBluetoothDevice));
        setupLeBroadcast(BT_LE_BROADCAST_ADDRESS);
        clearBroadcastReceivingDevice(mReceivingBroadcastBluetoothDevice1);
        setupBroadcastReceivingDevice(mReceivingBroadcastBluetoothDevice2);

        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        Map<String, AudioRouteItem> audioRouteItems = mAudioRoutesManager.getActiveRoutes();

        assertThat(audioRouteItems.get(BT_LE_AUDIO_DEVICE_ADDRESS_1).getState()).isEqualTo(
                AudioRouteItem.State.MULTICAST_READY_UNICAST_READY);
        assertThat(audioRouteItems.get(BT_LE_AUDIO_DEVICE_ADDRESS_2).getState()).isEqualTo(
                AudioRouteItem.State.MULTICAST_ACTIVE);
    }

    @Test
    public void testStateTransitions_toMulticastReadyUnicastReady_whenAnyUnicastFound() {
        setAudioSharing(true);
        CarAudioZoneConfigInfo leAudioZoneConfig = createZoneConfig(
                /* name= */ LE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* deviceName= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* type= */ TYPE_BLE_HEADSET,
                /* isActive= */ false,
                /* isSelected= */ false);
        CachedBluetoothDevice leAudioBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME_1,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_1,
                /* device= */ mReceivingBroadcastBluetoothDevice1,
                /* isConnectedA2dp= */ false,
                /* isConnectedLeAudio= */ true,
                /* isActiveA2dp= */ false,
                /* isActiveLeAudio= */ true);
        CarAudioZoneConfigInfo selectedLeAudioZoneConfig = createZoneConfig(
                /* name= */ LE_AUDIO_ZONE_CONFIG_NAME,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_2,
                /* deviceName= */ BT_LE_AUDIO_DEVICE_NAME_2,
                /* type= */ TYPE_BLE_HEADSET,
                /* isActive= */ true,
                /* isSelected= */ true);
        CachedBluetoothDevice selectedLeAudioBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME_2,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS_2,
                /* device= */ mReceivingBroadcastBluetoothDevice2,
                /* isConnectedA2dp= */ true,
                /* isConnectedLeAudio= */ true,
                /* isActiveA2dp= */ true,
                /* isActiveLeAudio= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(leAudioZoneConfig, selectedLeAudioZoneConfig),
                /* cachedDevices= */
                List.of(leAudioBluetoothDevice, selectedLeAudioBluetoothDevice));

        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        Map<String, AudioRouteItem> audioRouteItems = mAudioRoutesManager.getActiveRoutes();

        assertThat(audioRouteItems.get(BT_LE_AUDIO_DEVICE_ADDRESS_1).getState()).isEqualTo(
                AudioRouteItem.State.MULTICAST_READY_UNICAST_READY);
        assertThat(audioRouteItems.get(BT_LE_AUDIO_DEVICE_ADDRESS_2).getState()).isEqualTo(
                AudioRouteItem.State.UNICAST_ACTIVE);
    }

    private void setupMockAudioRoutes(List<CarAudioZoneConfigInfo> zoneInfos,
            List<CachedBluetoothDevice> cachedDevices) {
        when(mCarAudioManager.getAudioZoneConfigInfos(TEST_ZONE_ID))
                .thenReturn(zoneInfos);
        when(mCachedBluetoothDeviceManager.getCachedDevicesCopy())
                .thenReturn(cachedDevices);
    }

    private void setupLeBroadcast(String leBroadcastAddress) {
        // LE Broadcast profile
        when(mLocalBluetoothLeBroadcast.getAllBroadcastMetadata()).thenReturn(
                List.of(mBluetoothLeBroadcastMetadata));
        when(mBluetoothLeBroadcastMetadata.getSourceDevice()).thenReturn(mBroadcastBluetoothDevice);
        when(mBroadcastBluetoothDevice.getAddress()).thenReturn(leBroadcastAddress);
        when(mLocalBluetoothLeBroadcastAssistant.getAllSources(any())).thenReturn(List.of());
    }

    private void setupBroadcastReceivingDevice(BluetoothDevice leDevice) {
        when(mLocalBluetoothLeBroadcastAssistant.getAllSources(leDevice)).thenReturn(
                List.of(mBluetoothLeBroadcastReceiveState));
        when(mBluetoothLeBroadcastReceiveState.getSourceDevice()).thenReturn(
                mBroadcastBluetoothDevice);
    }

    private void clearBroadcastReceivingDevice(BluetoothDevice leDevice) {
        when(mLocalBluetoothLeBroadcastAssistant.getAllSources(leDevice)).thenReturn(List.of());
    }

    private void setupVolumeControlConnectionStatus(BluetoothDevice device, int connectionStatus) {
        when(mVolumeControlProfile.getConnectionStatus(device)).thenReturn(connectionStatus);
    }

    private void verifyAudioRoutesState(List<AudioRouteItem> routes,
            Map<String, AudioRouteItem.State> expectedStates) {
        for (Map.Entry<String, AudioRouteItem.State> entry : expectedStates.entrySet()) {
            assertAudioRouteState(routes, entry.getKey(), entry.getValue());
        }
    }

    private void assertAudioRouteState(List<AudioRouteItem> routes, String address,
            AudioRouteItem.State expectedState) {
        if (routes == null) {
            return;
        }
        AudioRouteItem route = routes.stream()
                .filter(item -> item.getAddress().equals(address))
                .findFirst()
                .orElseThrow(
                        () -> new AssertionError("AudioRoute not found for address: " + address));
        assertThat(route.getState()).isEqualTo(expectedState);
    }

    private void initMocks() {
        mSession = ExtendedMockito.mockitoSession()
                .mockStatic(LocalBluetoothManager.class)
                .mockStatic(Toast.class)
                .strictness(Strictness.LENIENT)
                .startMocking();
        when(LocalBluetoothManager.getInstance(any(), any())).thenReturn(mBluetoothManager);
        when(mBluetoothManager.getProfileManager()).thenReturn(mLocalBluetoothProfileManager);
        when(mLocalBluetoothProfileManager.getLeAudioBroadcastProfile())
                .thenReturn(mLocalBluetoothLeBroadcast);
        when(mLocalBluetoothProfileManager.getLeAudioBroadcastAssistantProfile())
                .thenReturn(mLocalBluetoothLeBroadcastAssistant);
        when(mLocalBluetoothProfileManager.getVolumeControlProfile())
                .thenReturn(mVolumeControlProfile);
        when(mLocalBluetoothProfileManager.getLeAudioProfile())
                .thenReturn(mLeAudioProfile);
        when(mBluetoothManager.getCachedDeviceManager()).thenReturn(mCachedBluetoothDeviceManager);

        when(mContext.getApplicationContext()).thenReturn(mCarSettingsApplication);
        when(mCarSettingsApplication.getCarAudioManager()).thenReturn(mCarAudioManager);
        when(mCarSettingsApplication.getMyAudioZoneId()).thenReturn(TEST_ZONE_ID);
        when(mCarAudioManager.isAudioFeatureEnabled(AUDIO_FEATURE_DYNAMIC_ROUTING))
                .thenReturn(true);

        when(mAudioAttributes.getUsage()).thenReturn(USAGE);
        when(mCarAudioManager.getOutputDeviceForUsage(TEST_ZONE_ID, USAGE))
                .thenReturn(mAudioDeviceInfo);
        when(mAudioDeviceInfo.getAddress()).thenReturn(AUDIO_DEVICE_ADDRESS);

        when(mLocalBluetoothLeBroadcast.getAllBroadcastMetadata()).thenReturn(List.of());

        setAudioSharing(true);

        when(Toast.makeText(any(), anyString(), anyInt())).thenReturn(mMockToast);
    }

    private void setAudioSharing(boolean enable) {
        SharedPreferences sharedPrefs = mContext.getSharedPreferences(
                BaseAudioSharingPreferenceController.USER_ENABLE_AUDIO_SHARING_KEY,
                Context.MODE_PRIVATE);
        sharedPrefs.edit().putBoolean(
                BaseAudioSharingPreferenceController.USER_ENABLE_AUDIO_SHARING_KEY,
                enable).commit();
    }

    private CarAudioZoneConfigInfo createZoneConfig(String name, String address,
            String deviceName, int type, boolean isActive, boolean isSelected) {
        AudioDeviceAttributes mockAttributes = mock(AudioDeviceAttributes.class);
        when(mockAttributes.getAddress()).thenReturn(address);
        when(mockAttributes.getName()).thenReturn(deviceName);
        when(mockAttributes.getType()).thenReturn(type);

        CarVolumeGroupInfo mockVolumeInfo = mock(CarVolumeGroupInfo.class);
        when(mockVolumeInfo.getAudioAttributes())
                .thenReturn(new ArrayList<>(Collections.singleton(mAudioAttributes)));
        when(mockVolumeInfo.getAudioDeviceAttributes())
                .thenReturn(new ArrayList<>(Collections.singleton(mockAttributes)));

        CarAudioZoneConfigInfo mockZoneInfo = mock(CarAudioZoneConfigInfo.class);
        when(mockZoneInfo.getName()).thenReturn(name);
        when(mockZoneInfo.isActive()).thenReturn(isActive);
        when(mockZoneInfo.isSelected()).thenReturn(isSelected);
        when(mockZoneInfo.getZoneId()).thenReturn(TEST_ZONE_ID);
        when(mockZoneInfo.getConfigVolumeGroups())
                .thenReturn(new ArrayList<>(Collections.singleton(mockVolumeInfo)));
        return mockZoneInfo;
    }

    private CachedBluetoothDevice createCachedBluetoothDevice(String name, String address,
            BluetoothDevice device, boolean isConnectedA2dp, boolean isConnectedLeAudio,
            boolean isActiveA2dp, boolean isActiveLeAudio) {
        CachedBluetoothDevice mockDevice = mock(CachedBluetoothDevice.class);
        when(mockDevice.getName()).thenReturn(name);
        when(mockDevice.getAddress()).thenReturn(address);
        when(mockDevice.isConnectedA2dpDevice()).thenReturn(isConnectedA2dp);
        when(mockDevice.isConnectedLeAudioDevice()).thenReturn(isConnectedLeAudio);
        when(mockDevice.isActiveDevice(BluetoothProfile.A2DP)).thenReturn(isActiveA2dp);
        when(mockDevice.isActiveDevice(BluetoothProfile.LE_AUDIO)).thenReturn(isActiveLeAudio);
        if (device != null) {
            when(mockDevice.getDevice()).thenReturn(device);
        }
        return mockDevice;
    }

    private void assertShowingToast(String message) {
        ExtendedMockito.verify(
                () -> Toast.makeText(any(), eq(message), anyInt()));
        verify(mMockToast).show();
    }
}
