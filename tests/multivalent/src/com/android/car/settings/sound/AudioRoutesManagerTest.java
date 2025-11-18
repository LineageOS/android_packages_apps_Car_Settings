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
    private static final String AUDIO_DEVICE_NAME = "audio";
    private static final String AUDIO_DEVICE_ADDRESS = "audio_address";
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
    private CachedBluetoothDevice mA2dpBluetoothDevice;
    @Mock
    private CachedBluetoothDevice mLeAudioBluetoothDevice;
    @Mock
    private AudioDeviceAttributes mAudioDeviceAttributes1;
    @Mock
    private CarAudioZoneConfigInfo mDeviceZoneConfig;
    @Mock
    private CarVolumeGroupInfo mCarVolumeGroupInfo1;
    @Mock
    private CarAudioZoneConfigInfo mA2dpZoneConfig;
    @Mock
    private CarVolumeGroupInfo mCarVolumeGroupInfo2;
    @Mock
    private AudioDeviceAttributes mAudioDeviceAttributes2;
    @Mock
    private CarAudioZoneConfigInfo mLeBroadcastZoneConfig;
    @Mock
    private CarVolumeGroupInfo mCarVolumeGroupInfo3;
    @Mock
    private AudioDeviceAttributes mLeBroadcastDevice;
    @Mock
    private AudioAttributes mAudioAttributes;
    @Mock
    private AudioDeviceInfo mAudioDeviceInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        initMocks();
    }

    @After
    public void tearDown() {
        mSession.finishMocking();
    }

    @Test
    public void getAudioRouteList_returnsDeviceAddresses() {
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        List<String> audioRouteList = mAudioRoutesManager.getAudioRouteList();

        assertThat(audioRouteList).containsExactly(AUDIO_DEVICE_ADDRESS, BT_A2DP_DEVICE_ADDRESS,
                BT_LE_AUDIO_DEVICE_ADDRESS, BT_LE_BROADCAST_ADDRESS);
    }

    @Test
    public void getOutputAddress_returnsActiveDeviceAddress() {
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        assertThat(mAudioRoutesManager.getOutputAddress()).isEqualTo(AUDIO_DEVICE_ADDRESS);
    }

    @Test
    public void getActiveRoutes_returnsAudioRouteItems() {
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
        AudioRouteItem initialAudioRoute = new AudioRouteItem.Builder(mAudioDeviceAttributes1)
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
        // Mock two CarAudioZoneConfigInfo objects with the same name but different selected states
        CarAudioZoneConfigInfo unselectedConfig = mock(CarAudioZoneConfigInfo.class);
        when(unselectedConfig.getName()).thenReturn(DEVICE_AUDIO_ZONE_CONFIG_NAME);
        when(unselectedConfig.isActive()).thenReturn(true);
        when(unselectedConfig.isSelected()).thenReturn(false);
        when(unselectedConfig.getZoneId()).thenReturn(TEST_ZONE_ID);
        when(unselectedConfig.getConfigVolumeGroups())
                .thenReturn(new ArrayList<>(Collections.singleton(mCarVolumeGroupInfo1)));

        CarAudioZoneConfigInfo selectedConfig = mock(CarAudioZoneConfigInfo.class);
        when(selectedConfig.getName()).thenReturn(DEVICE_AUDIO_ZONE_CONFIG_NAME);
        when(selectedConfig.isActive()).thenReturn(true);
        when(selectedConfig.isSelected()).thenReturn(true);
        when(selectedConfig.getZoneId()).thenReturn(TEST_ZONE_ID);
        when(selectedConfig.getConfigVolumeGroups())
                .thenReturn(new ArrayList<>(Collections.singleton(mCarVolumeGroupInfo1)));

        // Mock CarAudioManager to return both configs
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
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        assertThat(mAudioRoutesManager.getDeviceName(AUDIO_DEVICE_ADDRESS)).isEqualTo(
                AUDIO_DEVICE_NAME);
        assertThat(mAudioRoutesManager.getDeviceName(BT_LE_BROADCAST_ADDRESS)).isEqualTo(
                "ble audio broadcast");
        assertThat(mAudioRoutesManager.getDeviceName("invalid_address")).isNull();
    }

    @Test
    public void getDeviceName_removesSpecialCharacters() {
        when(mAudioDeviceAttributes1.getName()).thenReturn("audio%device");
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
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);

        assertThat(mAudioRoutesManager.isLeBroadcast(BT_LE_BROADCAST_ADDRESS)).isTrue();
        assertThat(mAudioRoutesManager.isLeBroadcast(AUDIO_DEVICE_ADDRESS)).isFalse();
    }

    @Test
    public void tearDown_clearsCallback() {
        mAudioRoutesManager = new AudioRoutesManager(mContext, USAGE);
        mAudioRoutesManager.tearDown();

        /* Verifies that {@link CarAudioManager#clearAudioZoneConfigsCallback()} is called in
          the constructor and {@link #tearDown()}. */
        verify(mCarAudioManager, times(2)).clearAudioZoneConfigsCallback();
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
        when(mCachedBluetoothDeviceManager.getCachedDevicesCopy())
                .thenReturn(List.of(mA2dpBluetoothDevice, mLeAudioBluetoothDevice));

        when(mContext.getApplicationContext()).thenReturn(mCarSettingsApplication);
        when(mCarSettingsApplication.getCarAudioManager()).thenReturn(mCarAudioManager);
        when(mCarSettingsApplication.getMyAudioZoneId()).thenReturn(TEST_ZONE_ID);
        when(mCarAudioManager.isAudioFeatureEnabled(AUDIO_FEATURE_DYNAMIC_ROUTING))
                .thenReturn(true);
        when(mCarAudioManager.getAudioZoneConfigInfos(TEST_ZONE_ID))
                .thenReturn(List.of(mDeviceZoneConfig, mA2dpZoneConfig,
                        mLeBroadcastZoneConfig));

        // Audio Route 1. CarAudioZoneConfigInfo : Audio device
        when(mDeviceZoneConfig.getName()).thenReturn(DEVICE_AUDIO_ZONE_CONFIG_NAME);
        when(mDeviceZoneConfig.isActive()).thenReturn(true);
        when(mDeviceZoneConfig.isSelected()).thenReturn(true);
        when(mDeviceZoneConfig.getConfigVolumeGroups())
                .thenReturn(new ArrayList<>(Collections.singleton(mCarVolumeGroupInfo1)));
        when(mCarVolumeGroupInfo1.getAudioAttributes())
                .thenReturn(new ArrayList<>(Collections.singleton(mAudioAttributes)));
        when(mCarVolumeGroupInfo1.getAudioDeviceAttributes())
                .thenReturn(new ArrayList<>(Collections.singleton(mAudioDeviceAttributes1)));
        when(mAudioDeviceAttributes1.getAddress()).thenReturn(AUDIO_DEVICE_ADDRESS);
        when(mAudioDeviceAttributes1.getName()).thenReturn(AUDIO_DEVICE_NAME);
        when(mAudioDeviceAttributes1.getType()).thenReturn(TYPE_BUILTIN_SPEAKER);

        // Audio Route 2. CarAudioZoneConfigInfo: Bluetooth 1 (A2DP only)
        when(mA2dpZoneConfig.getName()).thenReturn(A2DP_AUDIO_ZONE_CONFIG_NAME);
        when(mA2dpZoneConfig.isActive()).thenReturn(true);
        when(mA2dpZoneConfig.isSelected()).thenReturn(false);
        when(mA2dpZoneConfig.getConfigVolumeGroups())
                .thenReturn(new ArrayList<>(Collections.singleton(mCarVolumeGroupInfo2)));
        when(mCarVolumeGroupInfo2.getAudioAttributes())
                .thenReturn(new ArrayList<>(Collections.singleton(mAudioAttributes)));
        when(mCarVolumeGroupInfo2.getAudioDeviceAttributes())
                .thenReturn(new ArrayList<>(Collections.singleton(mAudioDeviceAttributes2)));
        when(mAudioDeviceAttributes2.getAddress()).thenReturn(BT_A2DP_DEVICE_ADDRESS);
        when(mAudioDeviceAttributes2.getName()).thenReturn(BT_A2DP_DEVICE_NAME);
        when(mAudioDeviceAttributes2.getType()).thenReturn(TYPE_BLUETOOTH_A2DP);

        // Audio Route 3. CarAudioZoneConfigInfo: LE Broadcast
        when(mLeBroadcastZoneConfig.getName()).thenReturn(LE_BROADCAST_AUDIO_ZONE_CONFIG_NAME);
        when(mLeBroadcastZoneConfig.isActive()).thenReturn(true);
        when(mLeBroadcastZoneConfig.isSelected()).thenReturn(false);
        when(mLeBroadcastZoneConfig.getConfigVolumeGroups())
                .thenReturn(new ArrayList<>(Collections.singleton(mCarVolumeGroupInfo3)));
        when(mCarVolumeGroupInfo3.getAudioAttributes())
                .thenReturn(new ArrayList<>(Collections.singleton(mAudioAttributes)));
        when(mCarVolumeGroupInfo3.getAudioDeviceAttributes())
                .thenReturn(new ArrayList<>(Collections.singleton(mLeBroadcastDevice)));
        when(mLeBroadcastDevice.getAddress()).thenReturn(BT_LE_BROADCAST_ADDRESS);
        when(mLeBroadcastDevice.getName()).thenReturn(BT_LE_BROADCAST_DEVICE_NAME);
        when(mLeBroadcastDevice.getType()).thenReturn(TYPE_BLE_BROADCAST);

        // Audio Route 2. Bluetooth 1 (A2DP only). This overlaps CarAudioZoneConfigInfo
        when(mA2dpBluetoothDevice.getName()).thenReturn(BT_A2DP_DEVICE_NAME);
        when(mA2dpBluetoothDevice.getAddress()).thenReturn(BT_A2DP_DEVICE_ADDRESS);
        when(mA2dpBluetoothDevice.isConnectedA2dpDevice()).thenReturn(true);
        when(mA2dpBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(false);
        when(mA2dpBluetoothDevice.isActiveDevice(BluetoothProfile.A2DP)).thenReturn(true);
        when(mA2dpBluetoothDevice.isActiveDevice(BluetoothProfile.LE_AUDIO)).thenReturn(false);

        // Audio Route 4. Bluetooth 2 (A2DP + BLE). This is not a part of CarAudioZoneConfigInfo.
        when(mLeAudioBluetoothDevice.getName()).thenReturn(BT_LE_AUDIO_DEVICE_NAME);
        when(mLeAudioBluetoothDevice.getAddress()).thenReturn(BT_LE_AUDIO_DEVICE_ADDRESS);
        when(mLeAudioBluetoothDevice.isConnectedA2dpDevice()).thenReturn(true);
        when(mLeAudioBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(true);
        when(mLeAudioBluetoothDevice.isActiveDevice(BluetoothProfile.A2DP)).thenReturn(true);
        when(mLeAudioBluetoothDevice.isActiveDevice(BluetoothProfile.LE_AUDIO)).thenReturn(true);
        when(mLeAudioBluetoothDevice.getDevice()).thenReturn(mReceivingBroadcastBluetoothDevice);

        // LE Broadcast profile
        when(mLocalBluetoothLeBroadcast.getAllBroadcastMetadata()).thenReturn(
                List.of(mBluetoothLeBroadcastMetadata));
        when(mBluetoothLeBroadcastMetadata.getSourceDevice()).thenReturn(mBroadcastBluetoothDevice);
        when(mBroadcastBluetoothDevice.getAddress()).thenReturn(BT_LE_BROADCAST_ADDRESS);

        // LE Broadcast Assistant profile
        when(mLocalBluetoothLeBroadcastAssistant.getAllSources(
                mReceivingBroadcastBluetoothDevice)).thenReturn(
                List.of(mBluetoothLeBroadcastReceiveState));
        when(mBluetoothLeBroadcastReceiveState.getSourceDevice()).thenReturn(
                mBroadcastBluetoothDevice);

        when(mAudioAttributes.getUsage()).thenReturn(USAGE);
        when(mCarAudioManager.getOutputDeviceForUsage(TEST_ZONE_ID, USAGE))
                .thenReturn(mAudioDeviceInfo);
        when(mAudioDeviceInfo.getAddress()).thenReturn(AUDIO_DEVICE_ADDRESS);

        SharedPreferences sharedPrefs = mContext.getSharedPreferences(
                BaseAudioSharingPreferenceController.USER_ENABLE_AUDIO_SHARING_KEY,
                Context.MODE_PRIVATE);
        sharedPrefs.edit().putBoolean(
                BaseAudioSharingPreferenceController.USER_ENABLE_AUDIO_SHARING_KEY, true).commit();
    }
}
