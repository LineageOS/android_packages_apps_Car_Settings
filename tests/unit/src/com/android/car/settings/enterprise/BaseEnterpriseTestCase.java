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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.admin.DeviceAdminInfo;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
abstract class BaseEnterpriseTestCase {

    @Rule
    public final MockitoRule mockitorule = MockitoJUnit.rule();

    protected final Context mRealContext = ApplicationProvider.getApplicationContext();
    protected final Context mSpiedContext = spy(mRealContext);

    protected final ComponentName mDefaultAdmin =
            new ComponentName(mSpiedContext, DefaultDeviceAdminReceiver.class);
    protected final ComponentName mFancyAdmin =
            new ComponentName(mSpiedContext, FancyDeviceAdminReceiver.class);

    protected DeviceAdminInfo mDefaultDeviceAdminInfo;
    protected DeviceAdminInfo mFancyDeviceAdminInfo;

    @Mock
    private DevicePolicyManager mDpm;

    @Before
    public final void setFixtures() throws Exception {
        when(mSpiedContext.getSystemService(DevicePolicyManager.class)).thenReturn(mDpm);

        PackageManager pm = mRealContext.getPackageManager();
        ActivityInfo defaultInfo = pm.getReceiverInfo(mDefaultAdmin, PackageManager.GET_META_DATA);
        mDefaultDeviceAdminInfo = new DeviceAdminInfo(mRealContext, defaultInfo);

        ActivityInfo fancyInfo = pm.getReceiverInfo(mFancyAdmin, PackageManager.GET_META_DATA);
        mFancyDeviceAdminInfo = new DeviceAdminInfo(mRealContext, fancyInfo);
    }

    protected final void mockProfileOwner() {
        mockIsAdminActive();
        when(mDpm.getProfileOwner()).thenReturn(mDefaultAdmin);
    }

    protected final void mockDeviceOwner() {
        mockIsAdminActive();
        when(mDpm.getDeviceOwnerComponentOnCallingUser()).thenReturn(mDefaultAdmin);
        when(mDpm.getDeviceOwnerComponentOnAnyUser()).thenReturn(mDefaultAdmin);
    }

    protected final void mockFinancialDevice() {
        when(mDpm.isDeviceManaged()).thenReturn(true);
        when(mDpm.getDeviceOwnerType(mDefaultAdmin))
                .thenReturn(DevicePolicyManager.DEVICE_OWNER_TYPE_FINANCED);
    }

    protected final void mockIsAdminActive() {
        when(mDpm.isAdminActive(mDefaultAdmin)).thenReturn(true);
    }

    protected final void mockActiveAdmin(ComponentName admin) {
        when(mDpm.getActiveAdmins()).thenReturn(Arrays.asList(admin));
    }

    protected void mockGetLongSupportMessageForUser(CharSequence message) {
        when(mDpm.getLongSupportMessageForUser(eq(mDefaultAdmin), anyInt())).thenReturn(message);
    }
}
