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

import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceGroup;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.settingslib.wifi.AccessPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class NetworkSecurityGroupPreferenceControllerTest {

    private Context mContext;
    private PreferenceGroup mPreferenceGroup;
    private PreferenceControllerTestHelper<NetworkSecurityGroupPreferenceController>
            mPreferenceControllerHelper;
    private NetworkSecurityGroupPreferenceController mController;
    @Mock
    private AccessPoint mAccessPoint;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreferenceGroup = new LogicalPreferenceGroup(mContext);
        mPreferenceControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                NetworkSecurityGroupPreferenceController.class, mPreferenceGroup);
        mController = mPreferenceControllerHelper.getController();
    }

    @Test
    public void testOnCreate_hasElements() {
        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    public void testRefreshUi_hasAccessPoint_securityTypePreferenceNotVisible() {
        mController.setAccessPoint(mAccessPoint);
        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        ListPreference securityTypePreference =
                (ListPreference) mPreferenceGroup.findPreference(
                        mContext.getString(R.string.pk_add_wifi_security));
        assertThat(securityTypePreference.isVisible()).isFalse();
    }

    @Test
    public void testRefreshUi_hasAccessPoint_passwordTextPreferenceIsVisible() {
        mController.setAccessPoint(mAccessPoint);
        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        EditTextPreference passwordTextPreference =
                (EditTextPreference) mPreferenceGroup.findPreference(
                        mContext.getString(R.string.pk_add_wifi_password));
        assertThat(passwordTextPreference.isVisible()).isTrue();
    }

    @Test
    public void testRefreshUi_noAccessPoint_securityTypePreferenceIsVisible() {
        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        ListPreference securityTypePreference =
                (ListPreference) mPreferenceGroup.findPreference(
                        mContext.getString(R.string.pk_add_wifi_security));
        assertThat(securityTypePreference.isVisible()).isTrue();
    }

    @Test
    public void testRefreshUi_noAccessPoint_noSecurity_passwordTextPreferenceNotVisible() {
        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        ListPreference securityTypePreference =
                (ListPreference) mPreferenceGroup.findPreference(
                        mContext.getString(R.string.pk_add_wifi_security));
        securityTypePreference.callChangeListener(Integer.toString(AccessPoint.SECURITY_NONE));
        EditTextPreference passwordTextPreference =
                (EditTextPreference) mPreferenceGroup.findPreference(
                        mContext.getString(R.string.pk_add_wifi_password));
        assertThat(passwordTextPreference.isVisible()).isFalse();
    }

    @Test
    public void testRefreshUi_noAccessPoint_wepSecurity_passwordTextPreferenceIsVisible() {
        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        ListPreference securityTypePreference =
                (ListPreference) mPreferenceGroup.findPreference(
                        mContext.getString(R.string.pk_add_wifi_security));
        securityTypePreference.callChangeListener(Integer.toString(AccessPoint.SECURITY_WEP));
        EditTextPreference passwordTextPreference =
                (EditTextPreference) mPreferenceGroup.findPreference(
                        mContext.getString(R.string.pk_add_wifi_password));
        assertThat(passwordTextPreference.isVisible()).isTrue();
    }

    @Test
    public void testRefreshUi_noAccessPoint_pskSecurity_passwordTextPreferenceIsVisible() {
        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        ListPreference securityTypePreference =
                (ListPreference) mPreferenceGroup.findPreference(
                        mContext.getString(R.string.pk_add_wifi_security));
        securityTypePreference.callChangeListener(Integer.toString(AccessPoint.SECURITY_PSK));
        EditTextPreference passwordTextPreference =
                (EditTextPreference) mPreferenceGroup.findPreference(
                        mContext.getString(R.string.pk_add_wifi_password));
        assertThat(passwordTextPreference.isVisible()).isTrue();
    }

    @Test
    public void testRefreshUi_noAccessPoint_eapSecurity_passwordTextPreferenceIsVisible() {
        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        ListPreference securityTypePreference =
                (ListPreference) mPreferenceGroup.findPreference(
                        mContext.getString(R.string.pk_add_wifi_security));
        securityTypePreference.callChangeListener(Integer.toString(AccessPoint.SECURITY_EAP));
        EditTextPreference passwordTextPreference =
                (EditTextPreference) mPreferenceGroup.findPreference(
                        mContext.getString(R.string.pk_add_wifi_password));
        assertThat(passwordTextPreference.isVisible()).isTrue();
    }

    @Test
    public void testRefreshUi_passwordEmpty_showsDefaultText() {
        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        EditTextPreference passwordTextPreference =
                (EditTextPreference) mPreferenceGroup.findPreference(
                        mContext.getString(R.string.pk_add_wifi_password));
        passwordTextPreference.callChangeListener("");
        assertThat(passwordTextPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.default_password_summary));
    }

    @Test
    public void testRefreshUi_passwordSet_showsStars() {
        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        EditTextPreference passwordTextPreference =
                (EditTextPreference) mPreferenceGroup.findPreference(
                        mContext.getString(R.string.pk_add_wifi_password));
        passwordTextPreference.callChangeListener("test password");
        // 13 stars for 13 characters of the password.
        assertThat(passwordTextPreference.getSummary()).isEqualTo("*************");
    }
}
