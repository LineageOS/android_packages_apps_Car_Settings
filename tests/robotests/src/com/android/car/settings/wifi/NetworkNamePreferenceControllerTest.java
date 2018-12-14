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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.preference.EditTextPreference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.settingslib.wifi.AccessPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class NetworkNamePreferenceControllerTest {

    private static final String TEST_SSID = "test_ssid";

    private Context mContext;
    private EditTextPreference mEditTextPreference;
    private PreferenceControllerTestHelper<NetworkNamePreferenceController>
            mPreferenceControllerHelper;
    private NetworkNamePreferenceController mController;
    @Mock
    private AccessPoint mAccessPoint;
    @Mock
    private NetworkNamePreferenceController.NetworkNameChangeListener mNetworkNameChangeListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mEditTextPreference = new EditTextPreference(mContext);
        mPreferenceControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                NetworkNamePreferenceController.class, mEditTextPreference);
        mController = mPreferenceControllerHelper.getController();
        mController.setTextChangeListener(mNetworkNameChangeListener);
        when(mAccessPoint.getSsid()).thenReturn(TEST_SSID);
    }

    @Test
    public void testOnStartInternal_hasAccessPoint_textIsSet() {
        mController.setAccessPoint(mAccessPoint);
        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);
        assertThat(mEditTextPreference.getText()).isEqualTo(TEST_SSID);
    }

    @Test
    public void testOnStartInternal_noAccessPoint_textIsNotSet() {
        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);
        assertThat(mEditTextPreference.getText()).isNull();
    }

    @Test
    public void testOnStartInternal_hasAccessPoint_isNotSelectable() {
        mController.setAccessPoint(mAccessPoint);
        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);
        assertThat(mEditTextPreference.isSelectable()).isFalse();
    }

    @Test
    public void testOnStartInternal_noAccessPoint_isSelectable() {
        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);
        assertThat(mEditTextPreference.isSelectable()).isTrue();
    }

    @Test
    public void testHandlePreferenceChanged_newTextIsSet() {
        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mEditTextPreference.setText("Old value");
        mEditTextPreference.callChangeListener("New value");
        assertThat(mEditTextPreference.getSummary()).isEqualTo("New value");
    }

    @Test
    public void testHandlePreferenceChanged_listenerIsCalled() {
        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mEditTextPreference.callChangeListener("New value");
        verify(mNetworkNameChangeListener).onNetworkNameChanged("New value");
    }
}
