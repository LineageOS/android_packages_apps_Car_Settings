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
import android.net.LinkProperties;
import android.net.Network;
import android.net.RouteInfo;

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

import java.net.InetAddress;
import java.util.Arrays;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class WifiGatewayPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "somePreferenceKey";
    private static final String GATE_WAY = "gateway";

    private PreferenceScreen mPreferenceScreen;
    private WifiDetailPreference mPreference;

    @Mock
    private AccessPoint mMockAccessPoint;
    @Mock
    private WifiInfoProvider mMockWifiInfoProvider;
    @Mock
    private Network mMockNetwork;
    @Mock
    private LinkProperties mMockLinkProperties;
    @Mock
    private RouteInfo mMockRouteInfo;
    @Mock
    private InetAddress mMockInetAddress;
    @Mock
    private FragmentController mMockFragmentController;

    private Context mContext;
    private WifiGatewayPreferenceController mController;

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
        when(mMockWifiInfoProvider.getLinkProperties()).thenReturn(mMockLinkProperties);
    }

    @Test
    public void onWifiChanged_shouldHaveDetailTextSet() {
        when(mMockAccessPoint.isActive()).thenReturn(true);
        mController = (WifiGatewayPreferenceController) new WifiGatewayPreferenceController(
                mContext, PREFERENCE_KEY, mMockFragmentController).init(
                mMockAccessPoint, mMockWifiInfoProvider);

        mController.displayPreference(mPreferenceScreen);
        when(mMockLinkProperties.getRoutes()).thenReturn(Arrays.asList(mMockRouteInfo));
        when(mMockRouteInfo.isIPv4Default()).thenReturn(true);
        when(mMockRouteInfo.hasGateway()).thenReturn(true);
        when(mMockRouteInfo.getGateway()).thenReturn(mMockInetAddress);
        when(mMockInetAddress.getHostAddress()).thenReturn(GATE_WAY);
        mController.onLinkPropertiesChanged(mMockNetwork, mMockLinkProperties);

        assertThat(mPreference.getDetailText()).isEqualTo(GATE_WAY);
    }

    @Test
    public void onWifiChanged_isNotActive_noUpdate() {
        when(mMockAccessPoint.isActive()).thenReturn(false);
        mController = (WifiGatewayPreferenceController) new WifiGatewayPreferenceController(
                mContext, PREFERENCE_KEY, mMockFragmentController).init(
                mMockAccessPoint, mMockWifiInfoProvider);

        mController.displayPreference(mPreferenceScreen);
        mController.onLinkPropertiesChanged(mMockNetwork, mMockLinkProperties);

        assertThat(mPreference.getDetailText()).isNull();
    }
}
