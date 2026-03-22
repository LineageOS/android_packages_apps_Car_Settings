/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.car.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.wifitrackerlib.WifiEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class WifiUtilTest {

    @Mock
    private WifiManager mMockWifiManager;
    @Mock
    private WifiEntry mMockWifiEntry;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test copied from {@link com.android.settings.wifi.dpp.WifiNetworkConfigTest}
     */
    @Test
    public void getWifiShareQrCode_returnsCorrectEscapedString() {
        String ssid = "Pixel:_ABCD;";
        String password = "\\012345678,";
        int networkId = 1;

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + ssid + "\"";
        config.networkId = networkId;
        config.hiddenSSID = false;
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

        WifiConfiguration privilegedConfig = new WifiConfiguration();
        privilegedConfig.networkId = networkId;
        privilegedConfig.preSharedKey = "\"" + password + "\"";

        when(mMockWifiEntry.getWifiConfiguration()).thenReturn(config);
        when(mMockWifiManager.getPrivilegedConfiguredNetworks()).thenReturn(
                Arrays.asList(privilegedConfig));

        String qrCode = WifiUtil.getWifiShareQrCode(mMockWifiManager, mMockWifiEntry);

        assertThat(qrCode).isEqualTo("WIFI:S:Pixel\\:_ABCD\\;;T:WPA;P:\\\\012345678\\,;H:false;;");
    }
}
