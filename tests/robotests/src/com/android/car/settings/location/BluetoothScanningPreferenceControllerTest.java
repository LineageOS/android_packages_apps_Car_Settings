/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.car.settings.location;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.SwitchPreference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class BluetoothScanningPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "location_scanning_bluetooth";

    private SwitchPreference mPreference;
    private ContentResolver mContentResolver;
    private BluetoothScanningPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context context = RuntimeEnvironment.application;
        mContentResolver = context.getContentResolver();
        mController = new BluetoothScanningPreferenceController(context, PREFERENCE_KEY,
                mock(FragmentController.class));
        mPreference = new SwitchPreference(context);
        mPreference.setKey(PREFERENCE_KEY);
    }

    @Test
    public void updateState_bluetoothScanningEnabled_shouldCheckPreference() {
        mPreference.setChecked(false);
        Settings.Global.putInt(mContentResolver, Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 1);
        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_bluetoothScanningDisabled_shouldUncheckPreference() {
        mPreference.setChecked(true);
        Settings.Global.putInt(mContentResolver, Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 0);
        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_preferenceChecked_shouldEnableBluetoothScanning() {
        mPreference.setChecked(true);
        mController.handlePreferenceTreeClick(mPreference);

        assertThat(Settings.Global.getInt(mContentResolver,
                Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 0)).isEqualTo(1);
    }

    @Test
    public void handlePreferenceTreeClick_preferenceUnchecked_shouldDisableBluetoothScanning() {
        mPreference.setChecked(false);
        mController.handlePreferenceTreeClick(mPreference);

        assertThat(Settings.Global.getInt(mContentResolver,
                Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 1)).isEqualTo(0);
    }
}
