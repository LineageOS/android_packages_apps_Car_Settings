/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.android.car.settings.common.ActionButtonsPreference.ActionButtons;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;

import androidx.lifecycle.LifecycleOwner;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.ActionButtonsPreference;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.wifi.AccessPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class WifiDetailsActionButtonsPreferenceControllerTest {

    private Context mContext = ApplicationProvider.getApplicationContext();
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private ActionButtonsPreference mActionButtonsPreference;
    private WifiDetailsActionButtonsPreferenceController mPreferenceController;
    private CarUxRestrictions mCarUxRestrictions;

    @Mock
    private FragmentController mFragmentController;
    @Mock
    private AccessPoint mMockAccessPoint;
    @Mock
    private WifiInfoProvider mMockWifiInfoProvider;
    @Mock
    private Network mMockNetwork;
    @Mock
    private NetworkInfo mMockNetworkInfo;
    @Mock
    private WifiInfo mMockWifiInfo;

    @Before
    @UiThreadTest
    public void setUp() {
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        MockitoAnnotations.initMocks(this);

        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();

        mActionButtonsPreference = new ActionButtonsPreference(mContext);
        mPreferenceController = new WifiDetailsActionButtonsPreferenceController(mContext,
                /* preferenceKey= */ "key", mFragmentController, mCarUxRestrictions);
        mPreferenceController.init(mMockAccessPoint, mMockWifiInfoProvider);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController,
                mActionButtonsPreference);

        when(mMockWifiInfoProvider.getNetwork()).thenReturn(mMockNetwork);
        when(mMockWifiInfoProvider.getNetworkInfo()).thenReturn(mMockNetworkInfo);
        when(mMockWifiInfoProvider.getWifiInfo()).thenReturn(mMockWifiInfo);
    }

    @Test
    public void updateState_connectNotNeeded_connectButtonHidden() {
        when(mMockAccessPoint.isSaved()).thenReturn(true);
        when(mMockAccessPoint.isActive()).thenReturn(true);

        mPreferenceController.onCreate(mLifecycleOwner);
        assertThat(mActionButtonsPreference.getButton(ActionButtons.BUTTON2).isVisible()).isFalse();
    }

    @Test
    public void updateState_needConnect_connectButtonShown() {
        when(mMockAccessPoint.isSaved()).thenReturn(true);
        when(mMockAccessPoint.isActive()).thenReturn(false);

        mPreferenceController.onCreate(mLifecycleOwner);
        assertThat(mActionButtonsPreference.getButton(ActionButtons.BUTTON2).isVisible()).isTrue();
    }

    @Test
    public void updateState_canForget_forgetButtonShown() {
        when(mMockAccessPoint.isSaved()).thenReturn(true);
        when(mMockAccessPoint.isActive()).thenReturn(true);
        when(mMockWifiInfo.isEphemeral()).thenReturn(true);

        mPreferenceController.onCreate(mLifecycleOwner);
        assertThat(mActionButtonsPreference.getButton(ActionButtons.BUTTON1).isVisible()).isTrue();
    }

    @Test
    public void updateState_canNotForget_forgetButtonHidden() {
        when(mMockAccessPoint.isSaved()).thenReturn(true);
        when(mMockAccessPoint.isActive()).thenReturn(true);
        when(mMockWifiInfo.isEphemeral()).thenReturn(false);

        mPreferenceController.onCreate(mLifecycleOwner);
        assertThat(mActionButtonsPreference.getButton(ActionButtons.BUTTON1).isVisible()).isFalse();
    }
}
