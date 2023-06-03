/*
 * Copyright 2018 The Android Open Source Project
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

import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceGroup;

import com.android.car.settings.R;
import com.android.car.settings.common.CarUxRestrictionsHelper;
import com.android.car.settings.common.DrawableButtonActionItem;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.MultiActionPreference;
import com.android.car.settings.common.ToggleButtonActionItem;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;

import java.util.Set;

/**
 * Displays a list of bonded (paired) Bluetooth devices. Clicking on a device launch the device
 * details page. Additional buttons to will connect/disconnect from the device, toggle phone calls,
 * and toggle media audio.
 *
 * <p>
 * Moreover, these buttons' availability and enable/disable status are controlled by UX restriction
 * and user restriction. Specifically,
 * <ul>
 * <li>{@code BLUETOOTH_BUTTON}: always available and enabled.
 * <li>{@code PHONE_BUTTON}: available when the device has {@code BluetoothProfile.HEADSET_CLIENT}
 * and {@code CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP} is not set. Disabled but clickable when
 * {@link UserManager.DISALLOW_CONFIG_BLUETOOTH} is set.
 * <li>{@code MEDIA_BUTTON}: available when the device has {@code BluetoothProfile.A2DP_SINK} and
 * {@code CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP} is not set; Disabled but clickable when
 * {@link UserManager.DISALLOW_CONFIG_BLUETOOTH} is set.
 * </ul>
 *
 * <p>
 * Note: when button is disabled, it will still be shown as available. When the button is disabled
 * because of {@link UserManager.DISALLOW_CONFIG_BLUETOOTH} is set by DevicePolicyManager, click
 * on the button will show action disabled by admin dialog.
 *
 * <p>
 * Device detail page will not be launched when UX retriction is set. It can still be launched
 * when there is {@link UserManager.DISALLOW_CONFIG_BLUETOOTH} restriction. However, individual
 * profile's toggle switch will be disabled - when clicked, shows action disabled by admin dialog.
 */
public class BluetoothBondedDevicesPreferenceController extends
        BluetoothDevicesGroupPreferenceController implements
        BluetoothDevicePreference.UpdateToggleButtonListener {

    private static final MultiActionPreference.ActionItemGroupOne CONNECT_BLUETOOTH_BUTTON =
            MultiActionPreference.ActionItemGroupOne.ACTION_ITEM1;
    private static final MultiActionPreference.ActionItemGroupTwo PHONE_BUTTON =
            MultiActionPreference.ActionItemGroupTwo.ACTION_ITEM1;
    private static final MultiActionPreference.ActionItemGroupTwo MEDIA_BUTTON =
            MultiActionPreference.ActionItemGroupTwo.ACTION_ITEM2;

    private final BluetoothDeviceFilter.Filter mBondedDeviceTypeFilter =
            new BondedDeviceTypeFilter();
    private boolean mShowDeviceDetails = true;
    private boolean mHasUxRestriction;

    public BluetoothBondedDevicesPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @VisibleForTesting
    BluetoothBondedDevicesPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions,
            LocalBluetoothManager localBluetoothManager, UserManager userManager) {
        super(context, preferenceKey, fragmentController, uxRestrictions, localBluetoothManager,
                userManager);
    }

    @Override
    protected BluetoothDeviceFilter.Filter getDeviceFilter() {
        return mBondedDeviceTypeFilter;
    }

    @Override
    protected BluetoothDevicePreference createDevicePreference(CachedBluetoothDevice cachedDevice) {
        BluetoothDevicePreference pref = super.createDevicePreference(cachedDevice);
        pref.setToggleButtonUpdateListener(this);
        mHasUxRestriction = hasNoSetupUxRestriction();
        setButtonsCheckedAndListeners(pref);

        ToggleButtonActionItem phoneItem = pref.getGroupTwoActionItem(PHONE_BUTTON);
        phoneItem.setContentDescription(getContext(),
                R.string.bluetooth_bonded_phone_toggle_content_description);

        ToggleButtonActionItem mediaItem = pref.getGroupTwoActionItem(MEDIA_BUTTON);
        mediaItem.setContentDescription(getContext(),
                R.string.bluetooth_bonded_media_toggle_content_description);
        return pref;
    }

    @Override
    protected void onDeviceClicked(CachedBluetoothDevice cachedDevice) {
        if (mShowDeviceDetails) {
            getFragmentController().launchFragment(
                    BluetoothDeviceDetailsFragment.newInstance(cachedDevice));
        }
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        refreshUi();
    }

    @Override
    protected void updateState(PreferenceGroup preferenceGroup) {
        super.updateState(preferenceGroup);
        updateActionAvailability(preferenceGroup);
    }

    @Override
    public void updateToggleButtonState(BluetoothDevicePreference preference) {
        updateActionAvailability(preference);
    }

    private void updateActionAvailability(PreferenceGroup group) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            BluetoothDevicePreference preference =
                    (BluetoothDevicePreference) group.getPreference(i);
            updateActionAvailability(preference);
        }
    }

    private void updateActionAvailability(BluetoothDevicePreference preference) {
        mHasUxRestriction = hasNoSetupUxRestriction();
        if (!mHasUxRestriction) {
            setButtonsCheckedAndListeners(preference);
        } else {
            updatePhoneActionItemAvailability(preference, /* isAvailable= */ false);
            updateMediaActionItemAvailability(preference, /* isAvailable= */ false);
        }
        mShowDeviceDetails = !mHasUxRestriction;
    }

    private void setButtonsCheckedAndListeners(BluetoothDevicePreference preference) {
        CachedBluetoothDevice cachedDevice = preference.getCachedDevice();

        // If device is currently attempting to connect/disconnect, disable further actions
        if (cachedDevice.isBusy()) {
            disableAllActionItems(preference);
            // There is a case where on creation the cached device will try to automatically connect
            // but does not report itself as busy yet. This ensures that the bluetooth button state
            // is correct (should be checked in either connecting or disconnecting states).

            return;
        }

        LocalBluetoothProfile phoneProfile = null;
        LocalBluetoothProfile mediaProfile = null;
        for (LocalBluetoothProfile profile : cachedDevice.getProfiles()) {
            if (profile.getProfileId() == BluetoothProfile.HEADSET_CLIENT) {
                phoneProfile = profile;
            } else if (profile.getProfileId() == BluetoothProfile.A2DP_SINK) {
                mediaProfile = profile;
            }
        }
        LocalBluetoothProfile finalPhoneProfile = phoneProfile;
        LocalBluetoothProfile finalMediaProfile = mediaProfile;

        boolean isConnected = cachedDevice.isConnected();
        showConnectButton(/* show= */ !isConnected, preference);

        // Setup up bluetooth button
        updateBluetoothActionItemAvailability(preference);
        DrawableButtonActionItem bluetoothItem = preference.getGroupOneActionItem(
                CONNECT_BLUETOOTH_BUTTON);
        bluetoothItem.setOnClickListener(
                () -> {
                    if (cachedDevice.isBusy()) {
                        return;
                    }
                    // If trying to connect and both phone and media are disabled, connecting will
                    // always fail. In this case force both profiles on.
                    if (finalPhoneProfile != null && finalMediaProfile != null
                            && !finalPhoneProfile.isEnabled(cachedDevice.getDevice())
                            && !finalMediaProfile.isEnabled(cachedDevice.getDevice())) {
                        finalPhoneProfile.setEnabled(cachedDevice.getDevice(), true);
                        finalMediaProfile.setEnabled(cachedDevice.getDevice(), true);
                    }

                    bluetoothItem.showLoadingAnimation(true);
                    bluetoothItem.setText(null);
                    cachedDevice.connect();
                });

        if (phoneProfile == null || !isConnected || mHasUxRestriction) {
            // Disable phone button
            updatePhoneActionItemAvailability(preference, /* isAvailable= */ false);
        } else {
            // Enable phone button
            ToggleButtonActionItem phoneItem = preference.getGroupTwoActionItem(PHONE_BUTTON);

            boolean phoneEnabled = phoneProfile.isEnabled(cachedDevice.getDevice());
            phoneItem.setChecked(phoneEnabled);

            updatePhoneActionItemAvailability(preference, /* isAvailable= */ true);

            if (hasDisallowConfigRestriction()) {
                phoneItem.setOnClickWhileDisabledListener(p -> BluetoothUtils
                        .onClickWhileDisabled(getContext(), getFragmentController()));
            }
            phoneItem.setOnClickListener(isChecked -> {
                finalPhoneProfile.setEnabled(cachedDevice.getDevice(), isChecked);
                phoneItem.setDrawable(getContext(), isChecked ? R.drawable.ic_bluetooth_phone
                        : R.drawable.ic_bluetooth_phone_unavailable);
            });
        }

        if (mediaProfile == null || !isConnected || mHasUxRestriction) {
            // Disable media button
            updateMediaActionItemAvailability(preference, /* isAvailable= */ false);
        } else {
            // Enable media button
            ToggleButtonActionItem mediaItem = preference.getGroupTwoActionItem(MEDIA_BUTTON);
            boolean mediaEnabled = mediaProfile.isEnabled(cachedDevice.getDevice());
            mediaItem.setChecked(mediaEnabled);

            updateMediaActionItemAvailability(preference, /* isAvailable= */ true);

            if (hasDisallowConfigRestriction()) {
                mediaItem.setOnClickWhileDisabledListener(p -> BluetoothUtils
                        .onClickWhileDisabled(getContext(), getFragmentController()));
            }
            mediaItem.setOnClickListener(isChecked -> {
                finalMediaProfile.setEnabled(cachedDevice.getDevice(), isChecked);
                mediaItem.setDrawable(getContext(), isChecked ? R.drawable.ic_bluetooth_media
                        : R.drawable.ic_bluetooth_media_unavailable);
            });
        }
    }

    private void updateBluetoothActionItemAvailability(BluetoothDevicePreference preference) {
        // Run on main thread because recyclerview may still be computing layout
        getContext().getMainExecutor().execute(() -> {
            DrawableButtonActionItem button = preference.getGroupOneActionItem(
                    CONNECT_BLUETOOTH_BUTTON);
            button.setText(getContext().getString(R.string.connect));
            button.showLoadingAnimation(false);
            button.setEnabled(true);
            button.setDrawable(getContext(), R.drawable.ic_bluetooth_connect_button);
        });
    }

    private void updatePhoneActionItemAvailability(BluetoothDevicePreference preference,
            boolean isAvailable) {
        // Run on main thread because recyclerview may still be computing layout
        getContext().getMainExecutor().execute(() -> {
            ToggleButtonActionItem phoneItem = preference.getGroupTwoActionItem(PHONE_BUTTON);
            phoneItem.setEnabled(isAvailable && !hasDisallowConfigRestriction());
            if (isAvailable) {
                phoneItem.setDrawable(getContext(), phoneItem.isChecked()
                        ? R.drawable.ic_bluetooth_phone
                        : R.drawable.ic_bluetooth_phone_unavailable);
            } else {
                phoneItem.setDrawable(getContext(), R.drawable.ic_bluetooth_phone_unavailable);
            }
            phoneItem.setRestricted(!isAvailable && mHasUxRestriction);
        });
    }

    private void updateMediaActionItemAvailability(BluetoothDevicePreference preference,
            boolean isAvailable) {
        // Run on main thread because recyclerview may still be computing layout
        getContext().getMainExecutor().execute(() -> {
            ToggleButtonActionItem mediaItem = preference.getGroupTwoActionItem(MEDIA_BUTTON);
            mediaItem.setEnabled(isAvailable && !hasDisallowConfigRestriction());
            if (isAvailable) {
                mediaItem.setDrawable(getContext(),
                        mediaItem.isChecked() ? R.drawable.ic_bluetooth_media
                                : R.drawable.ic_bluetooth_media_unavailable);
            } else {
                mediaItem.setDrawable(getContext(), R.drawable.ic_bluetooth_media_unavailable);
            }
            mediaItem.setRestricted(!isAvailable && mHasUxRestriction);
        });
    }

    private void disableAllActionItems(BluetoothDevicePreference preference) {
        // Run on main thread because recyclerview may still be computing layout
        getContext().getMainExecutor().execute(() -> {
            preference.getGroupOneActionItem(CONNECT_BLUETOOTH_BUTTON).setEnabled(false);
            preference.getGroupTwoActionItem(PHONE_BUTTON).setEnabled(false);
            preference.getGroupTwoActionItem(MEDIA_BUTTON).setEnabled(false);
        });
    }

    private boolean hasDisallowConfigRestriction() {
        return getUserManager().hasUserRestriction(DISALLOW_CONFIG_BLUETOOTH);
    }

    private boolean hasNoSetupUxRestriction() {
        return CarUxRestrictionsHelper.isNoSetup(getUxRestrictions());
    }

    /**
     * Whether to show bluetooth connect button and hide profile buttons or the other way around as
     * we have both of them in one layout.
     */
    private void showConnectButton(boolean show, BluetoothDevicePreference preference) {
        getContext().getMainExecutor().execute(() -> {
            if (show) {
                preference.getGroupOneActionItem(CONNECT_BLUETOOTH_BUTTON).setVisible(true);
                preference.getGroupTwoActionItem(PHONE_BUTTON).setVisible(false);
                preference.getGroupTwoActionItem(MEDIA_BUTTON).setVisible(false);
            } else {
                preference.getGroupOneActionItem(CONNECT_BLUETOOTH_BUTTON).setVisible(false);
                preference.getGroupTwoActionItem(PHONE_BUTTON).setVisible(true);
                preference.getGroupTwoActionItem(MEDIA_BUTTON).setVisible(true);
            }
        });
    }

    @Override
    protected boolean shouldShowChevronIcon() {
        return true;
    }

    @Override
    protected boolean shouldShowIcon() {
        return false;
    }

    /** Filter that matches only bonded devices with specific device types. */
    //TODO(b/198339129): Use BluetoothDeviceFilter.BONDED_DEVICE_FILTER
    private class BondedDeviceTypeFilter implements BluetoothDeviceFilter.Filter {
        @Override
        public boolean matches(BluetoothDevice device) {
            Set<BluetoothDevice> bondedDevices = mBluetoothManager.getBluetoothAdapter()
                    .getBondedDevices();
            return bondedDevices != null && bondedDevices.contains(device);
        }
    }
}
