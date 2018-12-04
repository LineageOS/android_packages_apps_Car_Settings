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

package com.android.car.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;

import androidx.preference.PreferenceCategory;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class AccessPointListPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "somePreferenceKey";
    private static final int SIGNAL_LEVEL = 1;

    @Mock
    private FragmentController mMockFragmentController;
    @Mock
    private AccessPoint mMockAccessPoint;
    @Mock
    private CarWifiManager mMockCarWifiManager;

    private PreferenceScreen mPreferenceScreen;
    private PreferenceCategory mPreferenceCategory;
    private Context mContext;
    private AccessPointListPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreferenceCategory = new PreferenceCategory(mContext);
        mPreferenceCategory.setKey(PREFERENCE_KEY);
        mPreferenceScreen.addPreference(mPreferenceCategory);
        mController = new AccessPointListPreferenceController(
                mContext, PREFERENCE_KEY, mMockFragmentController);
        mController.mCarWifiManager = mMockCarWifiManager;

        when(mMockAccessPoint.getSecurity()).thenReturn(AccessPoint.SECURITY_NONE);
        when(mMockAccessPoint.getLevel()).thenReturn(SIGNAL_LEVEL);
    }

    @Test
    public void updateAccessPoints_emptyList_notVisible() {
        mController.displayPreference(mPreferenceScreen);
        when(mMockCarWifiManager.getAllAccessPoints()).thenReturn(new ArrayList<>());
        mController.refreshData();

        assertThat(mPreferenceCategory.isVisible()).isEqualTo(false);
    }

    @Test
    public void updateAccessPoints_notEmpty_visible() {
        mController.displayPreference(mPreferenceScreen);
        List<AccessPoint> accessPointList = Arrays.asList(mMockAccessPoint);
        when(mMockCarWifiManager.getAllAccessPoints()).thenReturn(accessPointList);
        mController.refreshData();

        assertThat(mPreferenceCategory.isVisible()).isEqualTo(true);
    }

    @Test
    public void updateAccessPoints_notEmpty_listCount() {
        mController.displayPreference(mPreferenceScreen);
        List<AccessPoint> accessPointList = Arrays.asList(mMockAccessPoint);
        when(mMockCarWifiManager.getAllAccessPoints()).thenReturn(accessPointList);
        mController.refreshData();

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(accessPointList.size());
    }

    @Test
    public void onUxRestrictionsChanged_switchToSavedApOnly() {
        mController.displayPreference(mPreferenceScreen);
        List<AccessPoint> allAccessPointList = Arrays.asList(mMockAccessPoint, mMockAccessPoint);
        when(mMockCarWifiManager.getAllAccessPoints()).thenReturn(allAccessPointList);
        List<AccessPoint> savedAccessPointList = Arrays.asList(mMockAccessPoint);
        when(mMockCarWifiManager.getSavedAccessPoints()).thenReturn(savedAccessPointList);
        mController.refreshData();

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(allAccessPointList.size());

        CarUxRestrictions noSetupRestrictions = new CarUxRestrictions.Builder(
                true, CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP, 0).build();
        mController.onUxRestrictionsChanged(noSetupRestrictions);
        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(savedAccessPointList.size());
    }
}
