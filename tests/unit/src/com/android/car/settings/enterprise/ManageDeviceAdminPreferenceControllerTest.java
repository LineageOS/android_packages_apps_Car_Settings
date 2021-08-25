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

import static com.android.car.settings.common.PreferenceController.AVAILABLE;
import static com.android.car.settings.common.PreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.car.settings.common.PreferenceController.UNSUPPORTED_ON_DEVICE;

import android.content.ComponentName;
import android.content.pm.UserInfo;

import androidx.preference.Preference;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.R;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class ManageDeviceAdminPreferenceControllerTest
        extends BasePreferenceControllerTestCase {

    private static final UserInfo USER_PROFILE =
            new UserInfo(/* id= */ 100, /* name= */ "testUser", /* flags= */ 0);

    private Preference mPreference;

    @Test
    public void testUpdateState_emptyProfiles() {
        ManageDeviceAdminPreferenceController controller = newControllerWithFeatureEnabled();
        mockGetProfiles();

        controller.updateState(mPreference);

        assertPreferenceSummary(mPreference,
                mRealContext.getString(R.string.number_of_device_admins_none));
    }

    @Test
    public void testUpdateState_noActiveAdminApps() {
        ManageDeviceAdminPreferenceController controller = newControllerWithFeatureEnabled();
        mockGetProfiles(USER_PROFILE);
        mockActiveAdmins();

        controller.updateState(mPreference);

        assertPreferenceSummary(mPreference,
                mRealContext.getString(R.string.number_of_device_admins_none));
    }

    @Test
    public void testUpdateState_singleActiveAdminApp() {
        ManageDeviceAdminPreferenceController controller = newControllerWithFeatureEnabled();
        mockGetProfiles(USER_PROFILE);
        mockActiveAdmins(ComponentName.createRelative("packageName", "className"));

        controller.updateState(mPreference);

        assertPreferenceSummary(mPreference,
                mRealContext.getResources().getQuantityString(
                        R.plurals.number_of_device_admins, 1, 1));
    }

    @Test
    public void testUpdateState_multipleActiveAdminApps() {
        ManageDeviceAdminPreferenceController controller = newControllerWithFeatureEnabled();
        mockGetProfiles(USER_PROFILE);
        mockActiveAdmins(ComponentName.createRelative("packageName1", "className1"),
                ComponentName.createRelative("packageName2", "className2"));

        controller.updateState(mPreference);

        assertPreferenceSummary(mPreference,
                mRealContext.getResources().getQuantityString(
                        R.plurals.number_of_device_admins, 2, 2));
    }

    @Test
    public void testGetAvailabilityStatus_noDeviceAdmin() {
        ManageDeviceAdminPreferenceController controller = newControllerWithFeatureDisabled();

        assertAvailability(controller.getAvailabilityStatus(), CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void testGetAvailabilityStatus_deviceAdminDisabled() {
        ManageDeviceAdminPreferenceController controller = newControllerWithFeatureDisabled();
        controller.setDeviceAdmin(mDefaultDeviceAdminInfo);

        assertAvailability(controller.getAvailabilityStatus(), UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void testGetAvailabilityStatus_deviceAdminEnabled() {
        ManageDeviceAdminPreferenceController controller = newControllerWithFeatureEnabled();

        assertAvailability(controller.getAvailabilityStatus(), AVAILABLE);
    }

    private ManageDeviceAdminPreferenceController newControllerWithFeatureDisabled() {
        mockNoDeviceAdminFeature();
        return newController();
    }

    private ManageDeviceAdminPreferenceController newControllerWithFeatureEnabled() {
        mockHasDeviceAdminFeature();
        ManageDeviceAdminPreferenceController controller = newController();
        controller.setDeviceAdmin(mDefaultDeviceAdminInfo);
        return controller;
    }

    private ManageDeviceAdminPreferenceController newController() {
        mPreference = new Preference(mSpiedContext);
        return new ManageDeviceAdminPreferenceController(
                mSpiedContext, mPreferenceKey, mFragmentController, mUxRestrictions);
    }
}
