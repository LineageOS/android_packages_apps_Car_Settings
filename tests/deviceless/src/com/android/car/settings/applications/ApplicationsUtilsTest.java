/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.car.settings.applications;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.pm.UserInfo;
import android.platform.test.flag.junit.SetFlagsRule;
import android.telecom.TelecomManager;

import androidx.test.runner.AndroidJUnit4;

import com.android.car.settings.profiles.ProfileHelper;
import com.android.car.settings.testutils.ShadowSmsApplication;
import com.android.car.settings.testutils.ShadowTelecomDependencies;
import com.android.car.settings.testutils.ShadowUserHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowTelecomManager;

import java.util.Collections;

/** Unit test for {@link ApplicationsUtils}. */
@RunWith(AndroidJUnit4.class)
@Config(
        shadows = {
            ShadowUserHelper.class,
            ShadowTelecomDependencies.class
        })
public class ApplicationsUtilsTest {

    private static final String PACKAGE_NAME = "com.android.car.settings.test";

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setUp() {
        mSetFlagsRule.enableFlags(android.telecom.flags.Flags.FLAG_TELECOM_MAINLINE_API);
    }

    @After
    public void tearDown() {
        ShadowSmsApplication.reset();
        ShadowUserHelper.reset();
    }

    @Test
    public void isKeepEnabledPackage_defaultDialerApplication_returnsTrue() {
        TelecomManager telecomManager = RuntimeEnvironment.application.getSystemService(
                TelecomManager.class);
        ShadowTelecomManager shadowTelecomManager = Shadow.extract(telecomManager);
        shadowTelecomManager.setDefaultDialer(PACKAGE_NAME);

        assertThat(ApplicationsUtils.isKeepEnabledPackage(RuntimeEnvironment.application,
                PACKAGE_NAME)).isTrue();
    }

    @Test
    public void isKeepEnabledPackage_defaultSmsApplication_returnsTrue() {
        ShadowSmsApplication.setDefaultSmsApplication(new ComponentName(PACKAGE_NAME, "cls"));

        assertThat(ApplicationsUtils.isKeepEnabledPackage(RuntimeEnvironment.application,
                PACKAGE_NAME)).isTrue();
    }

    @Test
    public void isKeepEnabledPackage_returnsFalse() {
        assertThat(ApplicationsUtils.isKeepEnabledPackage(RuntimeEnvironment.application,
                PACKAGE_NAME)).isFalse();
    }

    @Test
    public void isProfileOrDeviceOwner_profileOwner_returnsTrue() {
        UserInfo userInfo = new UserInfo();
        userInfo.id = 123;
        DevicePolicyManager dpm = mock(DevicePolicyManager.class);
        ProfileHelper profileHelper = mock(ProfileHelper.class);
        ShadowUserHelper.setInstance(profileHelper);
        when(profileHelper.getAllProfiles()).thenReturn(Collections.singletonList(userInfo));
        when(dpm.getProfileOwnerAsUser(userInfo.id)).thenReturn(
                new ComponentName(PACKAGE_NAME, "cls"));

        assertThat(ApplicationsUtils.isProfileOrDeviceOwner(PACKAGE_NAME, dpm, profileHelper))
                .isTrue();
    }

    @Test
    public void isProfileOrDeviceOwner_deviceOwner_returnsTrue() {
        DevicePolicyManager dpm = mock(DevicePolicyManager.class);
        when(dpm.isDeviceOwnerAppOnAnyUser(PACKAGE_NAME)).thenReturn(true);

        assertThat(ApplicationsUtils.isProfileOrDeviceOwner(PACKAGE_NAME, dpm,
                mock(ProfileHelper.class))).isTrue();
    }

    @Test
    public void isProfileOrDeviceOwner_returnsFalse() {
        assertThat(ApplicationsUtils.isProfileOrDeviceOwner(PACKAGE_NAME,
                mock(DevicePolicyManager.class), mock(ProfileHelper.class))).isFalse();
    }

}
