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

package com.android.car.settings.wifi.details;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;
import com.android.settingslib.wifi.AccessPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class WifiMacAddressPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "somePreferenceKey";
    private static final String MAC_ADDRESS = "mac address";

    private PreferenceScreen mPreferenceScreen;
    private WifiDetailPreference mPreference;

    @Mock
    private AccessPoint mMockAccessPoint;
    @Mock
    private WifiInfoProvider mMockWifiInfoProvider;
    @Mock
    private NetworkInfo mMockNetworkInfo;
    @Mock
    private WifiInfo mMockWifiInfo;
    @Mock
    private FragmentController mMockFragmentController;

    private Context mContext;
    private WifiMacAddressPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        ShadowPackageManager pm = shadowOf(mContext.getPackageManager());
        pm.setSystemFeature(PackageManager.FEATURE_WIFI, true);
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreference = new WifiDetailPreference(mContext);
        mPreference.setKey(PREFERENCE_KEY);
        mPreferenceScreen.addPreference(mPreference);
        when(mMockWifiInfoProvider.getWifiInfo()).thenReturn(mMockWifiInfo);
        when(mMockAccessPoint.isActive()).thenReturn(true);

        mController = newController();
    }

    private WifiMacAddressPreferenceController newController() {
        return (WifiMacAddressPreferenceController) new WifiMacAddressPreferenceController(
                mContext, PREFERENCE_KEY, mMockFragmentController).init(
                        mMockAccessPoint, mMockWifiInfoProvider);
    }

    @Test
    public void onWifiChanged_shouldHaveDetailTextSet() {
        mController.displayPreference(mPreferenceScreen);
        when(mMockWifiInfo.getMacAddress()).thenReturn(MAC_ADDRESS);
        mController.onWifiChanged(mMockNetworkInfo, mMockWifiInfo);

        assertThat(mPreference.getDetailText()).isEqualTo(MAC_ADDRESS);
    }
}
