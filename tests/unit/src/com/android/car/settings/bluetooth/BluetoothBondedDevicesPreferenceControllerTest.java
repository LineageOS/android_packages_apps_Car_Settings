/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.car.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.settings.common.MultiActionPreference;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.common.ToggleButtonActionItem;
import com.android.car.settings.testutils.BluetoothTestUtils;
import com.android.car.settings.testutils.TestLifecycleOwner;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
// TODO(b/215784744) add tests for onClick behavior when the buttons are restricted or disabled.
public class BluetoothBondedDevicesPreferenceControllerTest {
    private static final String TEST_RESTRICTION = UserManager.DISALLOW_CONFIG_BLUETOOTH;

    private LifecycleOwner mLifecycleOwner;
    private Context mContext = ApplicationProvider.getApplicationContext();
    private PreferenceGroup mPreferenceGroup;
    private BluetoothBondedDevicesPreferenceController mPreferenceController;
    private CarUxRestrictions mCarUxRestrictions;
    private LocalBluetoothProfile mPhoneProfile;
    private LocalBluetoothProfile mMediaProfile;
    private LocalBluetoothManager mLocalBluetoothManager;
    private Collection<CachedBluetoothDevice> mCachedDevices;

    @Mock
    private FragmentController mFragmentController;
    @Mock
    private CachedBluetoothDevice mBondedCachedDevice;
    @Mock
    private BluetoothDevice mBondedDevice;
    @Mock
    private UserManager mUserManager;
    @Mock
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock
    private LocalBluetoothAdapter mLocalBluetoothAdapter;

    @Before
    @UiThreadTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // Ensure bluetooth is available and enabled.
        assumeTrue(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH));
        BluetoothTestUtils.setBluetoothState(mContext, /* enable= */ true);

        Context mSpiedContext = spy(mContext);
        mLifecycleOwner = new TestLifecycleOwner();
        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();

        when(mBondedDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);
        when(mBondedCachedDevice.getDevice()).thenReturn(mBondedDevice);

        mPhoneProfile =
                new BluetoothTestUtils.TestLocalBluetoothProfile(BluetoothProfile.HEADSET_CLIENT);
        mMediaProfile =
                new BluetoothTestUtils.TestLocalBluetoothProfile(BluetoothProfile.A2DP_SINK);
        when(mBondedCachedDevice.getProfiles()).thenReturn(List.of(mPhoneProfile, mMediaProfile));

        BluetoothDevice unbondedDevice = mock(BluetoothDevice.class);
        when(unbondedDevice.getBondState()).thenReturn(BluetoothDevice.BOND_NONE);
        CachedBluetoothDevice unbondedCachedDevice = mock(CachedBluetoothDevice.class);
        when(unbondedCachedDevice.getDevice()).thenReturn(unbondedDevice);

        mCachedDevices = Arrays.asList(mBondedCachedDevice, unbondedCachedDevice);

        mLocalBluetoothManager = spy(BluetoothUtils.getLocalBtManager(mSpiedContext));
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(mCachedDevices);
        when(mLocalBluetoothManager.getBluetoothAdapter()).thenReturn(mLocalBluetoothAdapter);
        when(mLocalBluetoothAdapter.getBondedDevices()).thenReturn(Set.of(mBondedDevice));

        PreferenceManager preferenceManager = new PreferenceManager(mSpiedContext);
        PreferenceScreen screen = preferenceManager.createPreferenceScreen(mSpiedContext);
        mPreferenceGroup = new LogicalPreferenceGroup(mSpiedContext);
        screen.addPreference(mPreferenceGroup);
        mPreferenceController = new BluetoothBondedDevicesPreferenceController(mSpiedContext,
                /* preferenceKey= */ "key", mFragmentController, mCarUxRestrictions,
                mLocalBluetoothManager, mUserManager);

        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreferenceGroup);
    }

    @Test
    public void showsOnlyBondedDevices() {
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(1);
        BluetoothDevicePreference devicePreference =
                (BluetoothDevicePreference) mPreferenceGroup.getPreference(0);
        assertThat(devicePreference.getCachedDevice()).isEqualTo(mBondedCachedDevice);
    }

    @Test
    public void onDeviceBondStateChanged_refreshesUi() {
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(1);

        // Unbond the only bonded device.
        when(mBondedCachedDevice.getBondState()).thenReturn(BluetoothDevice.BOND_NONE);
        when(mBondedDevice.getBondState()).thenReturn(BluetoothDevice.BOND_NONE);
        when(mLocalBluetoothAdapter.getBondedDevices()).thenReturn(Set.of());
        mPreferenceController.onDeviceBondStateChanged(mBondedCachedDevice,
                BluetoothDevice.BOND_NONE);

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    @UiThreadTest
    public void onDeviceClicked_connected_launchesDeviceDetailsFragment() {
        when(mBondedCachedDevice.isConnected()).thenReturn(true);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
        BluetoothDevicePreference devicePreference =
                (BluetoothDevicePreference) mPreferenceGroup.getPreference(0);

        devicePreference.performClick();

        verify(mFragmentController).launchFragment(
                any(BluetoothDeviceDetailsFragment.class));
    }

    @Test
    public void bluetoothButtonClicked_connected_disconnectsFromDevice() {
        when(mBondedCachedDevice.isConnected()).thenReturn(true);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
        BluetoothDevicePreference devicePreference =
                (BluetoothDevicePreference) mPreferenceGroup.getPreference(0);

        ToggleButtonActionItem bluetoothButton = devicePreference.getActionItem(
                MultiActionPreference.ActionItem.ACTION_ITEM1);
        assertThat(bluetoothButton.isVisible()).isTrue();
        bluetoothButton.onClick();

        verify(mBondedCachedDevice).disconnect();
    }

    @Test
    public void bluetoothButtonClicked_notConnected_connectsToDevice() {
        when(mBondedCachedDevice.isConnected()).thenReturn(false);
        when(mUserManager.hasUserRestriction(TEST_RESTRICTION)).thenReturn(false);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
        BluetoothDevicePreference devicePreference =
                (BluetoothDevicePreference) mPreferenceGroup.getPreference(0);

        ToggleButtonActionItem bluetoothButton = devicePreference.getActionItem(
                MultiActionPreference.ActionItem.ACTION_ITEM1);
        assertThat(bluetoothButton.isEnabled()).isTrue();
        bluetoothButton.onClick();

        verify(mBondedCachedDevice).connect();
    }

    @Test
    public void phoneButtonClicked_phoneProfile_enabled() {
        when(mBondedCachedDevice.isConnected()).thenReturn(true);
        when(mUserManager.hasUserRestriction(TEST_RESTRICTION)).thenReturn(false);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
        BluetoothDevicePreference devicePreference =
                (BluetoothDevicePreference) mPreferenceGroup.getPreference(0);

        ToggleButtonActionItem phoneButton = devicePreference.getActionItem(
                MultiActionPreference.ActionItem.ACTION_ITEM2);
        assertThat(phoneButton.isEnabled()).isTrue();
        assertThat(mPhoneProfile.isEnabled(mBondedDevice)).isFalse();
        phoneButton.onClick();

        assertThat(mPhoneProfile.isEnabled(mBondedDevice)).isTrue();
    }

    @Test
    public void mediaButtonClicked_mediaProfile_enabled() {
        when(mBondedCachedDevice.isConnected()).thenReturn(true);
        when(mUserManager.hasUserRestriction(TEST_RESTRICTION)).thenReturn(false);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
        BluetoothDevicePreference devicePreference =
                (BluetoothDevicePreference) mPreferenceGroup.getPreference(0);

        ToggleButtonActionItem mediaButton = devicePreference.getActionItem(
                MultiActionPreference.ActionItem.ACTION_ITEM3);
        mediaButton.onClick();

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(mMediaProfile.isEnabled(mBondedDevice)).isTrue();
    }

    @Test
    public void actionButtons_disallowConfigBluetooth_bluetoothActionStaysDisabled() {
        when(mBondedCachedDevice.isConnected()).thenReturn(true);
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_BLUETOOTH))
                .thenReturn(true);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        BluetoothDevicePreference devicePreference =
                (BluetoothDevicePreference) mPreferenceGroup.getPreference(0);

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(devicePreference.getActionItem(
                MultiActionPreference.ActionItem.ACTION_ITEM1).isEnabled()).isTrue();
        assertThat(devicePreference.getActionItem(
                MultiActionPreference.ActionItem.ACTION_ITEM2).isEnabled()).isFalse();
        assertThat(devicePreference.getActionItem(
                MultiActionPreference.ActionItem.ACTION_ITEM3).isEnabled()).isFalse();
    }

    @Test
    public void onUxRestrictionsChanged_hasRestrictions_actionButtonDisabled() {
        when(mBondedCachedDevice.isConnected()).thenReturn(true);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
        BluetoothDevicePreference devicePreference =
                (BluetoothDevicePreference) mPreferenceGroup.getPreference(0);

        CarUxRestrictions restrictions = new CarUxRestrictions.Builder(
                true, CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP, 0).build();
        mPreferenceController.onUxRestrictionsChanged(restrictions);

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(devicePreference.getActionItem(
                MultiActionPreference.ActionItem.ACTION_ITEM1).isEnabled()).isTrue();
        assertThat(devicePreference.getActionItem(
                MultiActionPreference.ActionItem.ACTION_ITEM2).isEnabled()).isFalse();
        assertThat(devicePreference.getActionItem(
                MultiActionPreference.ActionItem.ACTION_ITEM3).isEnabled()).isFalse();
    }

    @Test
    public void onUxRestrictionsChanged_restrictionToggled_actionButtonsEnabled() {
        when(mBondedCachedDevice.isConnected()).thenReturn(true);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
        BluetoothDevicePreference devicePreference =
                (BluetoothDevicePreference) mPreferenceGroup.getPreference(0);

        CarUxRestrictions restrictions = new CarUxRestrictions.Builder(
                true, CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP, 0).build();
        mPreferenceController.onUxRestrictionsChanged(restrictions);

        CarUxRestrictions noRestrictions = new CarUxRestrictions.Builder(
                true, CarUxRestrictions.UX_RESTRICTIONS_BASELINE, 0).build();
        mPreferenceController.onUxRestrictionsChanged(noRestrictions);

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(devicePreference.getActionItem(
                MultiActionPreference.ActionItem.ACTION_ITEM1).isEnabled()).isTrue();
        assertThat(devicePreference.getActionItem(
                MultiActionPreference.ActionItem.ACTION_ITEM2).isEnabled()).isTrue();
        assertThat(devicePreference.getActionItem(
                MultiActionPreference.ActionItem.ACTION_ITEM3).isEnabled()).isTrue();
    }
}
