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

package com.android.car.settingslib.applications;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import android.app.admin.DevicePolicyManager;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.Process;
import android.os.UserHandle;

import androidx.test.runner.AndroidJUnit4;

import com.android.car.settings.testutils.ShadowSmsApplication;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowRoleManager;

import java.util.Set;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowSmsApplication.class})
public class ApplicationFeatureProviderImplTest {

    private static final String TEST_PKG = "com.test.package";

    private Context mContext;
    private PackageManager mPm;
    private IPackageManager mPms;
    private DevicePolicyManager mDpm;
    private ApplicationFeatureProviderImpl mProvider;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPm = mock(PackageManager.class);
        mPms = mock(IPackageManager.class);
        mDpm = mock(DevicePolicyManager.class);
        mProvider = new ApplicationFeatureProviderImpl(mContext, mPm, mPms, mDpm);
    }

    @After
    public void tearDown() {
        ShadowSmsApplication.reset();
    }

    @Test
    public void getKeepEnabledPackages_containsDialerRoleHolder() {
        ShadowRoleManager.addRoleHolder(RoleManager.ROLE_DIALER, TEST_PKG, Process.myUserHandle());

        Set<String> keepEnabledPackages = mProvider.getKeepEnabledPackages();

        assertThat(keepEnabledPackages).contains(TEST_PKG);
    }
}
