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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.pm.IPackageManager;
import android.os.UserHandle;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(AndroidJUnit4.class)
public final class DeviceAdminAppsPreferenceControllerTest
        extends BasePreferenceControllerTestCase {

    private static final UserHandle USER_HANDLE = new UserHandle(/* id= */ 100);

    @Mock
    private IPackageManager mIPackageManager;

    @Mock
    private PreferenceGroup mPreferenceGroup;

    private DeviceAdminAppsPreferenceController mController;

    @Before
    @UiThreadTest
    public void setUp() throws Exception {
        setFixtures();

        mController = new DeviceAdminAppsPreferenceController(mSpiedContext, mPreferenceKey,
                mFragmentController, mUxRestrictions, mIPackageManager);
        mController.setDeviceAdmin(mDefaultDeviceAdminInfo);

        when(mIPackageManager.getReceiverInfo(eq(mDefaultAdmin), anyInt(), anyInt()))
                .thenReturn(mDefaultActivityInfo);
        when(mIPackageManager.getReceiverInfo(eq(mFancyAdmin), anyInt(), anyInt()))
                .thenReturn(mFancyActivityInfo);
    }

    @Test
    public void testUpdateState_emptyProfiles() {
        mockGetUserProfiles();

        mController.updateState(mPreferenceGroup);

        verifyPreferenceCount(0);
    }

    @Test
    public void testUpdateState_noActiveAdminApps() {
        mockGetUserProfiles(USER_HANDLE);
        mockActiveAdmins();

        mController.updateState(mPreferenceGroup);

        verifyPreferenceCount(0);
    }

    @Test
    @UiThreadTest
    public void testUpdateState_singleActiveAdminApp() {
        mockGetUserProfiles(USER_HANDLE);
        mockActiveAdmins(mDefaultAdmin);

        mController.updateState(mPreferenceGroup);

        verifyPreferenceCount(1);
    }

    @Test
    @UiThreadTest
    public void testUpdateState_multipleActiveAdminApps() {
        mockGetUserProfiles(USER_HANDLE);
        mockActiveAdmins(mDefaultAdmin, mFancyAdmin);

        mController.updateState(mPreferenceGroup);

        verifyPreferenceCount(2);
    }

    private void verifyPreferenceCount(int preferenceCount) {
        verify(mPreferenceGroup).removeAll();
        verify(mPreferenceGroup, times(preferenceCount)).addPreference(any(Preference.class));
    }
}
