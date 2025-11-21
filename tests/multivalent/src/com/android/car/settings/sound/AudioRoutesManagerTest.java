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
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.car.media.CarAudioManager;
import android.car.media.CarAudioZoneConfigInfo;
import android.car.media.CarVolumeGroupInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.CarSettingsApplication;
import com.android.car.settings.bluetooth.audiosharing.BaseAudioSharingPreferenceController;
import com.android.dx.mockito.inline.extended.ExtendedMockito;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
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
    private static final String LE_BROADCAST_AUDIO_ZONE_CONFIG_NAME = "zone_config_le_broadcast";
    private static final String BT_A2DP_DEVICE_NAME = "bluetooth_a2dp";
    private static final String BT_A2DP_DEVICE_ADDRESS = "bluetooth_a2dp_address";
    private static final String BT_LE_AUDIO_DEVICE_NAME = "bluetooth_le_audio";
    private static final String BT_LE_AUDIO_DEVICE_ADDRESS = "bluetooth_le_audio_address";
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
    private CachedBluetoothDeviceManager mCachedBluetoothDeviceManager;
    @Mock
    private BluetoothLeBroadcastMetadata mBluetoothLeBroadcastMetadata;
    @Mock
    private BluetoothDevice mBroadcastBluetoothDevice;
    @Mock
    private BluetoothDevice mReceivingBroadcastBluetoothDevice;
    @Mock
    private BluetoothLeBroadcastReceiveState mBluetoothLeBroadcastReceiveState;
    @Mock
    private AudioAttributes mAudioAttributes;
    @Mock
    private AudioDeviceInfo mAudioDeviceInfo;
    @Mock
    private AudioRoutesManager.AudioRoutesUpdateListener mListener;
    private ArgumentCaptor<List<AudioRouteItem>> mAudioRoutesCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mAudioRoutesCaptor = ArgumentCaptor.forClass(List.class);
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
                /* isConnectedAudio= */false,
                /* isActiveA2dp= */ true,
                /* isActiveLeAudio= */ false);
        CachedBluetoothDevice leAudioBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS,
                /* device= */ mReceivingBroadcastBluetoothDevice,
                /* isConnectedA2dp= */ true,
                /* isConnectedAudio= */true,
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
                BT_LE_AUDIO_DEVICE_ADDRESS,
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
    public void getActiveRoutes_returnsAudioRouteItems() {
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
                /* isConnectedAudio= */false,
                /* isActiveA2dp= */ true,
                /* isActiveLeAudio= */ false);
        CachedBluetoothDevice leAudioBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS,
                /* device= */ mReceivingBroadcastBluetoothDevice,
                /* isConnectedA2dp= */ true,
                /* isConnectedAudio= */ true,
                /* isActiveA2dp= */ true,
                /* isActiveLeAudio= */ true);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(deviceZoneConfig, a2dpZoneConfig, leBroadcastZoneConfig),
                /* cachedDevices= */ List.of(a2dpBluetoothDevice, leAudioBluetoothDevice)
        );
        setupMockLeBroadcast(BT_LE_BROADCAST_ADDRESS);

        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        Map<String, AudioRouteItem> audioRouteItems = mAudioRoutesManager.getActiveRoutes();
        assertThat(audioRouteItems.size()).isEqualTo(4);

        // Audio device, active and selected.
        AudioRouteItem deviceAudioRoute = audioRouteItems.get(AUDIO_DEVICE_ADDRESS);
        assertThat(deviceAudioRoute).isNotNull();
        assertThat(deviceAudioRoute.getName()).isEqualTo(AUDIO_DEVICE_NAME);
        assertThat(deviceAudioRoute.getAddress()).isEqualTo(AUDIO_DEVICE_ADDRESS);
        assertThat(deviceAudioRoute.getAudioRouteType()).isEqualTo(TYPE_BUILTIN_SPEAKER);
        assertThat(deviceAudioRoute.getBluetoothDevice()).isNull();
        assertThat(deviceAudioRoute.getAudioZoneConfigState().isActive()).isTrue();
        assertThat(deviceAudioRoute.getAudioZoneConfigState().isSelected()).isTrue();

        // A2DP BT device, active.
        AudioRouteItem a2dpAudioRoute = audioRouteItems.get(BT_A2DP_DEVICE_ADDRESS);
        assertThat(a2dpAudioRoute).isNotNull();
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
        AudioRouteItem leAudioRoute = audioRouteItems.get(BT_LE_AUDIO_DEVICE_ADDRESS);
        assertThat(leAudioRoute).isNotNull();
        assertThat(leAudioRoute.getName()).isEqualTo(BT_LE_AUDIO_DEVICE_NAME);
        assertThat(leAudioRoute.getAddress()).isEqualTo(BT_LE_AUDIO_DEVICE_ADDRESS);
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
        assertThat(broadcastAudioRoute).isNotNull();
        assertThat(broadcastAudioRoute.getName()).isEqualTo(BT_LE_BROADCAST_DEVICE_NAME);
        assertThat(broadcastAudioRoute.getAddress()).isEqualTo(BT_LE_BROADCAST_ADDRESS);
        assertThat(broadcastAudioRoute.getAudioRouteType()).isEqualTo(TYPE_BLE_BROADCAST);
        assertThat(broadcastAudioRoute.getAudioZoneConfigState().isActive()).isTrue();
        assertThat(broadcastAudioRoute.getAudioZoneConfigState().isSelected()).isFalse();
        assertThat(broadcastAudioRoute.getGlobalState().isAudioSharingEnabled()).isTrue();
    }

    @Test
    public void getActiveRoutes_copiesPreviousState() {
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
        assertThat(deviceAudioRoute).isNotNull();
        assertThat(deviceAudioRoute.getState()).isEqualTo(initialState);
    }

    @Test
    public void getActiveRoutes_selectedConfigWins() {
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
        assertThat(deviceAudioRoute).isNotNull();
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
                /* isConnectedAudio= */false,
                /* isActiveA2dp= */ false,
                /* isActiveLeAudio= */ false);
        CachedBluetoothDevice leAudioBluetoothDevice = createCachedBluetoothDevice(
                /* name= */ BT_LE_AUDIO_DEVICE_NAME,
                /* address= */ BT_LE_AUDIO_DEVICE_ADDRESS,
                /* device= */ mReceivingBroadcastBluetoothDevice,
                /* isConnectedA2dp= */ true,
                /* isConnectedAudio= */ true,
                /* isActiveA2dp= */ false,
                /* isActiveLeAudio= */ false);
        setupMockAudioRoutes(
                /* zoneInfos= */ List.of(deviceZoneConfig, a2dpZoneConfig),
                /* cachedDevices= */ List.of(a2dpBluetoothDevice, leAudioBluetoothDevice)
        );

        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);
        mAudioRoutesManager.setAudioRoutesUpdateListener(mListener);
        // setUnicast: AUDIO_DEVICE_ADDRESS -> BT_A2DP_DEVICE_ADDRESS
        mAudioRoutesManager.setUnicast(BT_A2DP_DEVICE_ADDRESS);
        // Simulate Car Audio callback forBT_A2DP_DEVICE_ADDRESS to be active.
        when(a2dpZoneConfig.isActive()).thenReturn(true);
        mAudioRoutesManager.updateAndNotifyAudioRouteItemsIfChanged();
        // Simulate Car Audio callback for BT_A2DP_DEVICE_ADDRESS to be selected.
        when(deviceZoneConfig.isSelected()).thenReturn(false);
        when(a2dpZoneConfig.isSelected()).thenReturn(true);
        mAudioRoutesManager.updateAndNotifyAudioRouteItemsIfChanged();

        verify(mListener, times(3)).onAudioRoutesUpdated(mAudioRoutesCaptor.capture());
        List<List<AudioRouteItem>> allCapturedRoutes = mAudioRoutesCaptor.getAllValues();
        verifyAudioRoutesState(allCapturedRoutes.get(0),
                Map.of(AUDIO_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_ACTIVE,
                        BT_A2DP_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY,
                        BT_LE_AUDIO_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY));
        verifyAudioRoutesState(allCapturedRoutes.get(1),
                Map.of(AUDIO_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_ACTIVE,
                        BT_A2DP_DEVICE_ADDRESS, AudioRouteItem.State.STARTING_UNICAST,
                        BT_LE_AUDIO_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY));
        verifyAudioRoutesState(allCapturedRoutes.get(2),
                Map.of(AUDIO_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY,
                        BT_A2DP_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_ACTIVE,
                        BT_LE_AUDIO_DEVICE_ADDRESS, AudioRouteItem.State.UNICAST_READY));
        verify(mCarAudioManager).switchAudioZoneToConfig(eq(a2dpZoneConfig), any(), any());
    }

    @Test
    public void tearDown_clearsCallback() {
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);
        mAudioRoutesManager.tearDown();

        /* Verifies that {@link CarAudioManager#clearAudioZoneConfigsCallback()} is called in
          the constructor and {@link #tearDown()}. */
        verify(mCarAudioManager, times(2)).clearAudioZoneConfigsCallback();
    }

    private void setupMockAudioRoutes(List<CarAudioZoneConfigInfo> zoneInfos,
            List<CachedBluetoothDevice> cachedDevices) {
        when(mCarAudioManager.getAudioZoneConfigInfos(TEST_ZONE_ID))
                .thenReturn(zoneInfos);
        when(mCachedBluetoothDeviceManager.getCachedDevicesCopy())
                .thenReturn(cachedDevices);
    }

    private void setupMockLeBroadcast(String leBroadcastAddress) {
        // LE Broadcast profile
        when(mLocalBluetoothLeBroadcast.getAllBroadcastMetadata()).thenReturn(
                List.of(mBluetoothLeBroadcastMetadata));
        when(mBluetoothLeBroadcastMetadata.getSourceDevice()).thenReturn(mBroadcastBluetoothDevice);
        when(mBroadcastBluetoothDevice.getAddress()).thenReturn(leBroadcastAddress);

        when(mLocalBluetoothLeBroadcastAssistant.getAllSources(
                mReceivingBroadcastBluetoothDevice)).thenReturn(
                List.of(mBluetoothLeBroadcastReceiveState));
        when(mBluetoothLeBroadcastReceiveState.getSourceDevice()).thenReturn(
                mBroadcastBluetoothDevice);
    }

    private void verifyAudioRoutesState(List<AudioRouteItem> routes,
            Map<String, AudioRouteItem.State> expectedStates) {
        for (Map.Entry<String, AudioRouteItem.State> entry : expectedStates.entrySet()) {
            assertAudioRouteState(routes, entry.getKey(), entry.getValue());
        }
    }

    private void assertAudioRouteState(List<AudioRouteItem> routes, String address,
            AudioRouteItem.State expectedState) {
        AudioRouteItem route = routes.stream()
                .filter(item -> item.getAddress().equals(address))
                .findFirst()
                .orElse(null);
        assertThat(route).isNotNull();
        assertThat(route.getState()).isEqualTo(expectedState);
    }

    private void initMocks() {
        mSession = ExtendedMockito.mockitoSession().mockStatic(
                LocalBluetoothManager.class, withSettings().lenient()).startMocking();
        when(LocalBluetoothManager.getInstance(any(), any())).thenReturn(mBluetoothManager);
        when(mBluetoothManager.getProfileManager()).thenReturn(mLocalBluetoothProfileManager);
        when(mLocalBluetoothProfileManager.getLeAudioBroadcastProfile())
                .thenReturn(mLocalBluetoothLeBroadcast);
        when(mLocalBluetoothProfileManager.getLeAudioBroadcastAssistantProfile())
                .thenReturn(mLocalBluetoothLeBroadcastAssistant);
        when(mLocalBluetoothProfileManager.getVolumeControlProfile())
                .thenReturn(mVolumeControlProfile);
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

        SharedPreferences sharedPrefs = mContext.getSharedPreferences(
                BaseAudioSharingPreferenceController.USER_ENABLE_AUDIO_SHARING_KEY,
                Context.MODE_PRIVATE);
        sharedPrefs.edit().putBoolean(
                BaseAudioSharingPreferenceController.USER_ENABLE_AUDIO_SHARING_KEY, true).commit();
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
}
