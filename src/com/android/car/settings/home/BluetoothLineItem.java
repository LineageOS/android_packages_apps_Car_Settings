/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.car.settings.home;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.car.widget.TextListItem;

import com.android.car.settings.R;
import com.android.car.settings.bluetooth.BluetoothSettingsFragment;
import com.android.car.settings.common.BaseFragment;
import com.android.car.settings.common.Logger;


/**
 * Represents the Bluetooth line item on settings home page.
 */
public class BluetoothLineItem extends TextListItem {
    private static final Logger LOG = new Logger(BluetoothLineItem.class);
    private BluetoothAdapter mBluetoothAdapter;
    private BaseFragment.FragmentController mFragmentController;

    public BluetoothLineItem(Context context, BaseFragment.FragmentController fragmentController) {
        super(context);
        setTitle(context.getString(R.string.bluetooth_settings));
        setBody(context.getString(R.string.bluetooth_settings_summary));
        setSupplementalIcon(R.drawable.ic_chevron_right, /* showDivider= */ false);
        mFragmentController = fragmentController;
        mBluetoothAdapter =
                ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE))
                        .getAdapter();
        boolean enabled = isBluetoothEnabled();
        setPrimaryActionIcon(getIconRes(enabled), /* useLargeIcon= */ false);
        setSwitch(enabled, /* showDivider= */ false, (button, isChecked) -> {
            if (isChecked) {
                mBluetoothAdapter.enable();
            } else {
                mBluetoothAdapter.disable();
            }
        });
        if (isBluetoothAvailable()) {
            setOnClickListener(v -> fragmentController.launchFragment(
                    BluetoothSettingsFragment.getInstance()));
        }
    }

    private boolean isBluetoothEnabled() {
        boolean enabled = isBluetoothAvailable() && mBluetoothAdapter.isEnabled();
        LOG.d("BluetoothEnabled: " + enabled);
        return enabled;
    }

    public void onBluetoothStateChanged(boolean enabled) {
        setPrimaryActionIcon(getIconRes(enabled), /* useLargeIcon= */ false);
        setSwitchState(enabled);
    }

    private boolean isBluetoothAvailable() {
        return mBluetoothAdapter != null;
    }

    @DrawableRes
    private int getIconRes(boolean enabled) {
        return enabled
                ? R.drawable.ic_settings_bluetooth : R.drawable.ic_settings_bluetooth_disabled;
    }
}
