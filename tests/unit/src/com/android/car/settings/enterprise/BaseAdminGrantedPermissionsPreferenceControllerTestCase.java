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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.preference.Preference;

import com.android.car.settings.R;
import com.android.car.settings.applications.SyncApplicationFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

abstract class BaseAdminGrantedPermissionsPreferenceControllerTestCase
        <C extends BaseAdminGrantedPermissionsPreferenceController> extends
        BasePreferenceControllerTestCase {

    private C mController;

    protected final String[] mPermissions;

    @Mock
    private Preference mPreference;

    @Mock
    protected SyncApplicationFeatureProvider mSyncApplicationFeatureProvider;

    BaseAdminGrantedPermissionsPreferenceControllerTestCase(String... permissions) {
        mPermissions = permissions;
    }

    @Before
    public void setController() {
        mController = newController(mSyncApplicationFeatureProvider);
    }

    protected abstract C newController(SyncApplicationFeatureProvider provider);

    @Test
    public void testGetAvailabilityStatus_noFeature() {
        mockNoDeviceAdminFeature();
        // Cannot use mController because it check for feature on constructor
        C controller = newController(mSyncApplicationFeatureProvider);

        assertAvailability(controller.getAvailabilityStatus(), UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void testGetAvailabilityStatus_noPermissionsGranted() {
        mockGetNumberOfAppsWithAdminGrantedPermissions(mPermissions, 0);

        assertAvailability(mController.getAvailabilityStatus(), CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void testGetAvailabilityStatus_permissionsGranted() {
        mockGetNumberOfAppsWithAdminGrantedPermissions(mPermissions, 42);

        assertAvailability(mController.getAvailabilityStatus(), AVAILABLE);
    }

    @Test
    public void testGetAvailabilityStatus_cachesResult() {
        mockGetNumberOfAppsWithAdminGrantedPermissions(mPermissions, 42);

        mController.getAvailabilityStatus();
        mController.getAvailabilityStatus();

        verifyGetNumberOfAppsWithAdminGrantedPermissionsCalledOnce(mPermissions);
    }

    @Test
    public void testUpdateState_set() {
        mockGetNumberOfAppsWithAdminGrantedPermissions(mPermissions, 42);

        mController.updateState(mPreference);

        String expectedSummary = mRealContext.getResources().getQuantityString(
                R.plurals.enterprise_privacy_number_packages_lower_bound, 42, 42);
        verifyPreferenceTitleNeverSet(mPreference);
        verifyPreferenceSummarySet(mPreference, expectedSummary);
        verifyPreferenceIconNeverSet(mPreference);
    }

    @Test
    public void testUpdateState_cachesResult() {
        mockGetNumberOfAppsWithAdminGrantedPermissions(mPermissions, 42);

        mController.updateState(mPreference);
        mController.updateState(mPreference);

        verifyGetNumberOfAppsWithAdminGrantedPermissionsCalledOnce(mPermissions);
    }

    protected void mockGetNumberOfAppsWithAdminGrantedPermissions(String[] permissions, int num) {
        when(mSyncApplicationFeatureProvider
                .getNumberOfAppsWithAdminGrantedPermissions(permissions)).thenReturn(num);
    }

    protected void verifyGetNumberOfAppsWithAdminGrantedPermissionsCalledOnce(
            String[] permissions) {
        verify(mSyncApplicationFeatureProvider)
                .getNumberOfAppsWithAdminGrantedPermissions(permissions);
    }
}
