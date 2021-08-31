/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.car.settings.enterprise;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DeviceAdminStringProviderImplTest {
    private DeviceAdminStringProviderImpl mDeviceAdminStringProvider;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mDeviceAdminStringProvider = new DeviceAdminStringProviderImpl(mContext);
    }

    @Test
    public void testDefaultDisabledByPolicyTitle() {
        assertEquals(mDeviceAdminStringProvider.getDefaultDisabledByPolicyTitle(),
                mContext.getString(R.string.disabled_by_policy_title));
    }

    @Test
    public void testDisallowAdjustVolumeTitle() {
        assertEquals(mDeviceAdminStringProvider.getDisallowAdjustVolumeTitle(),
                mContext.getString(R.string.disabled_by_policy_title_adjust_volume));
    }

    @Test
    public void testDisallowOutgoingCallsTitle() {
        assertEquals(mDeviceAdminStringProvider.getDisallowOutgoingCallsTitle(),
                mContext.getString(R.string.disabled_by_policy_title_outgoing_calls));
    }

    @Test
    public void testDisallowSmsTitle() {
        assertEquals(mDeviceAdminStringProvider.getDisallowSmsTitle(),
                mContext.getString(R.string.disabled_by_policy_title_sms));
    }

    @Test
    public void testDisableCameraTitle() {
        assertEquals(mDeviceAdminStringProvider.getDisableCameraTitle(),
                mContext.getString(R.string.disabled_by_policy_title_camera));
    }

    @Test
    public void testDisableScreenCaptureTitle() {
        assertEquals(mDeviceAdminStringProvider.getDisableScreenCaptureTitle(),
                mContext.getString(R.string.disabled_by_policy_title_screen_capture));
    }

    @Test
    public void testSuspendPackagesTitle() {
        assertEquals(mDeviceAdminStringProvider.getSuspendPackagesTitle(),
                mContext.getString(R.string.disabled_by_policy_title_suspend_packages));
    }

    @Test
    public void testDefaultDisabledByPolicyContent() {
        assertEquals(mDeviceAdminStringProvider.getDefaultDisabledByPolicyContent(),
                mContext.getString(R.string.default_admin_support_msg));
    }

    @Test
    public void testLearnMoreHelpPageUrl() {
        assertEquals(mDeviceAdminStringProvider.getLearnMoreHelpPageUrl(),
                mContext.getString(R.string.help_url_action_disabled_by_it_admin));
    }

    @Test
    public void testDisabledByPolicyTitleForFinancedDevice() {
        assertEquals(mDeviceAdminStringProvider.getDisabledByPolicyTitleForFinancedDevice(),
                mContext.getString(R.string.disabled_by_policy_title_financed_device));
    }

    @Test
    public void testDisabledBiometricsParentConsentTitle() {
        assertEquals(mDeviceAdminStringProvider.getDisabledBiometricsParentConsentTitle(),
                mContext.getString(R.string.disabled_by_policy_title_biometric_parental_consent));
    }

    @Test
    public void testDisabledBiometricsParentConsentContent() {
        assertEquals(mDeviceAdminStringProvider.getDisabledBiometricsParentConsentContent(),
                mContext.getString(R.string.disabled_by_policy_content_biometric_parental_consent));
    }
}
