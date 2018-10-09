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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.settingslib.wifi.AccessPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class WifiSignalStrengthPreferenceControllerTest {

    private static final int LEVEL = 1;
    private static final String PREFERENCE_KEY = "somePreferenceKey";

    private PreferenceScreen mPreferenceScreen;

    @Mock
    private AccessPoint mMockAccessPoint;
    @Mock
    private WifiInfoProvider mMockWifiInfoProvider;
    @Mock
    private NetworkInfo mMockNetworkInfo;
    @Mock
    private WifiInfo mMockWifiInfo;
    @Mock
    private WifiDetailPreference mMockPreference;
    @Mock
    private FragmentController mMockFragmentController;

    private Context mContext;
    private WifiSignalStrengthPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        when(mMockPreference.getKey()).thenReturn(PREFERENCE_KEY);
        mPreferenceScreen.addPreference(mMockPreference);
        when(mMockAccessPoint.getLevel()).thenReturn(LEVEL);

        mController = newController();
    }

    private WifiSignalStrengthPreferenceController newController() {
        return (WifiSignalStrengthPreferenceController) new WifiSignalStrengthPreferenceController(
                mContext, PREFERENCE_KEY, mMockFragmentController).init(
                        mMockAccessPoint, mMockWifiInfoProvider);
    }

    private void displayAndStart() {
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onWifiChanged_shouldHaveDetailTextSet() {
        String expectedStrength =
                mContext.getResources().getStringArray(R.array.wifi_signals)[LEVEL];

        displayAndStart();
        mController.onWifiChanged(mMockNetworkInfo, mMockWifiInfo);

        // wifiInfoProvider will do call back twice, once on start once onWifiChanged
        verify(mMockPreference, times(2)).setDetailText(expectedStrength);
    }
}
