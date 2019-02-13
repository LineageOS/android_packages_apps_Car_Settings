/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.settings.system;

import static com.android.car.settings.common.PreferenceController.AVAILABLE;
import static com.android.car.settings.common.PreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.PreferenceControllerTestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

/** Unit test for {@link WifiMacAddressPreferenceController}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
public class WifiMacAddressPreferenceControllerTest {

    private static final String MAC_ADDRESS = "mac address";

    private Preference mPreference;
    private Context mContext;

    @Mock
    private WifiManager mMockWifiManager;
    @Mock
    private WifiInfo mMockWifiInfo;
    @Mock
    private Context mMockContext;
    private PreferenceControllerTestHelper<WifiMacAddressPreferenceController>
            mControllerHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        // Construct controller.
        mPreference = new Preference(mContext);
        mControllerHelper = new PreferenceControllerTestHelper<>(mContext,
            WifiMacAddressPreferenceController.class, mPreference);

        when(mMockContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mMockWifiManager);
        when(mMockWifiManager.getConnectionInfo()).thenReturn(mMockWifiInfo);
        mControllerHelper.getController().init(mMockContext);
    }

    @Test
    public void getAvailabilityStatus_wifiAvailable_available() {
        Shadows.shadowOf(mContext.getPackageManager()).setSystemFeature(
                PackageManager.FEATURE_WIFI, /* supported= */ true);

        assertThat(mControllerHelper.getController().getAvailabilityStatus())
                .isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_wifiNotAvailable_unsupportedOnDevice() {
        Shadows.shadowOf(mContext.getPackageManager()).setSystemFeature(
                PackageManager.FEATURE_WIFI, /* supported= */ false);

        assertThat(mControllerHelper.getController().getAvailabilityStatus())
                .isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getSummary_shouldHaveMacAddress() {
        Shadows.shadowOf(mContext.getPackageManager()).setSystemFeature(
                PackageManager.FEATURE_WIFI, true);

        when(mMockWifiInfo.getMacAddress()).thenReturn(MAC_ADDRESS);

        mControllerHelper.markState(Lifecycle.State.CREATED);
        mControllerHelper.getController().refreshUi();

        assertThat(mPreference.getSummary()).isEqualTo(MAC_ADDRESS);
    }
}
