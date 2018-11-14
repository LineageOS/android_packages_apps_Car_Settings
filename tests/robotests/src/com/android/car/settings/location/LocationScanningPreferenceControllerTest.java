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

import static com.android.car.settings.common.BasePreferenceController.AVAILABLE;
import static com.android.car.settings.common.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.content.pm.PackageManager;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class LocationScanningPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "location_scanning";

    private ShadowPackageManager mShadowPackageManager;
    private LocationScanningPreferenceController mController;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.application;
        mController = new LocationScanningPreferenceController(context, PREFERENCE_KEY,
                mock(FragmentController.class));
        mShadowPackageManager = Shadows.shadowOf(context.getPackageManager());
    }

    @Test
    public void getAvailabilityStatus_available_wifiOrBluetooth() {
        mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_WIFI, /* supported= */ true);
        mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE,
                /* supported= */ false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);

        mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_WIFI, /* supported= */ false);
        mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE,
                /* supported= */ true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_notAvailable_noWifiNoBluetooth() {
        mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_WIFI, /* supported= */ false);
        mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE,
                /* supported= */ false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }
}
