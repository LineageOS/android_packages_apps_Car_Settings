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

import static android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION;
import static android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN;
import static android.car.test.mocks.AndroidMockitoHelper.syncCallOnMainThread;

import static com.android.car.settings.enterprise.DeviceAdminAddActivity.EXTRA_DEVICE_ADMIN_PACKAGE_NAME;

import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.android.car.settings.R;
import com.android.car.ui.toolbar.ToolbarController;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public final class DeviceAdminAddFragmentTest extends BaseEnterpriseTestCase {

    private static final String EXPLANATION = "To get to the other side";

    private final Context mRealContext = ApplicationProvider.getApplicationContext();

    private DeviceAdminAddFragment mRealFragment;
    private DeviceAdminAddFragment mSpiedFragment;

    @Mock
    private ToolbarController mToolbarController;

    @Mock
    private FragmentActivity mActivity;

    @Mock
    private DeviceAdminAddHeaderPreferenceController mHeaderController;
    @Mock
    private DeviceAdminAddExplanationPreferenceController mExplanationController;
    @Mock
    private DeviceAdminAddSupportPreferenceController mSupportController;
    @Mock
    private DeviceAdminAddWarningPreferenceController mWarningController;
    @Mock
    private DeviceAdminAddPoliciesPreferenceController mPoliciesController;
    @Mock
    private DeviceAdminAddCancelPreferenceController mCancelController;
    @Mock
    private DeviceAdminAddActionPreferenceController mActionController;

    @Before
    public void createFragments() throws Exception {
        mRealFragment = syncCallOnMainThread(() -> new DeviceAdminAddFragment());
        mSpiedFragment = spy(mRealFragment);

        when(mExplanationController.setDeviceAdmin(any())).thenReturn(mExplanationController);

        // Note: Must use doReturn (instead of when..doReturn() below because it's a spy
        doReturn(mActivity).when(mSpiedFragment).requireActivity();
        doReturn(mHeaderController).when(mSpiedFragment)
                .use(eq(DeviceAdminAddHeaderPreferenceController.class), anyInt());
        doReturn(mExplanationController).when(mSpiedFragment)
                .use(eq(DeviceAdminAddExplanationPreferenceController.class), anyInt());
        doReturn(mSupportController).when(mSpiedFragment)
                .use(eq(DeviceAdminAddSupportPreferenceController.class), anyInt());
        doReturn(mWarningController).when(mSpiedFragment)
                .use(eq(DeviceAdminAddWarningPreferenceController.class), anyInt());
        doReturn(mPoliciesController).when(mSpiedFragment)
                .use(eq(DeviceAdminAddPoliciesPreferenceController.class), anyInt());
        doReturn(mCancelController).when(mSpiedFragment)
                .use(eq(DeviceAdminAddCancelPreferenceController.class), anyInt());
        doReturn(mActionController).when(mSpiedFragment)
                .use(eq(DeviceAdminAddActionPreferenceController.class), anyInt());
    }

    @Test
    public void testGetPreferenceScreenResId() {
        int resId = mRealFragment.getPreferenceScreenResId();

        XmlResourceParser parser = mRealContext.getResources().getXml(resId);
        assertWithMessage("xml with id%s", resId).that(parser).isNotNull();
    }

    @Test
    public void testSetupToolbar_noIntent() {
        mSpiedFragment.setToolbarTitle(mToolbarController);

        verify(mToolbarController, never()).setTitle(anyInt());
    }

    @Test
    public void testSetupToolbar_notFromActionAddDeviceAdmin() {
        mockActivityIntent(new Intent());

        mSpiedFragment.setToolbarTitle(mToolbarController);

        verify(mToolbarController, never()).setTitle(anyInt());
    }

    @Test
    public void testSetupToolbar_fromActionAddDeviceAdmin() {
        mockActivityIntent(new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN));

        mSpiedFragment.setToolbarTitle(mToolbarController);

        verify(mToolbarController).setTitle(R.string.add_device_admin_msg);
    }

    @Test
    public void testAttach_noIntent() {
        mRealFragment.onAttach(mSpiedContext, mActivity);

        verifyActivityFinished();
        verifyControllersNeverUsed();
    }

    @Test
    public void testAttach_noExtra() {
        mockActivityIntent(new Intent());

        mRealFragment.onAttach(mSpiedContext, mActivity);

        verifyActivityFinished();
        verifyControllersNeverUsed();
    }

    @Test
    public void testAttach_adminExtra_adminNotFound() {
        mockActivityIntent(new Intent()
                .putExtra(EXTRA_DEVICE_ADMIN, new ComponentName("Guy", "Incognito")));

        mRealFragment.onAttach(mSpiedContext, mActivity);

        verifyActivityFinished();
        verifyControllersNeverUsed();
    }

    @Test
    public void testAttach_adminExtra_ok() {
        mockActivityIntent(new Intent()
                .putExtra(EXTRA_DEVICE_ADMIN, mDefaultAdmin)
                .putExtra(EXTRA_ADD_EXPLANATION, EXPLANATION));

        mSpiedFragment.onAttach(mSpiedContext, mActivity);

        verifyActivityNeverFinished();
        verifyControllersUsed();
    }

    @Test
    public void testAttach_packageExtra_adminNotFound() {
        mockActivityIntent(new Intent().putExtra(EXTRA_DEVICE_ADMIN_PACKAGE_NAME, "D'OH!"));

        mRealFragment.onAttach(mSpiedContext, mActivity);

        verifyActivityFinished();
        verifyControllersNeverUsed();
    }

    @Test
    public void testAttach_packageExtra_ok() {
        mockActivityIntent(new Intent()
                .putExtra(EXTRA_DEVICE_ADMIN_PACKAGE_NAME, mPackageName)
                .putExtra(EXTRA_ADD_EXPLANATION, EXPLANATION));
        mockActiveAdmin(mDefaultAdmin);

        mSpiedFragment.onAttach(mSpiedContext, mActivity);

        verifyActivityNeverFinished();
        verifyControllersUsed();
    }

    private void mockActivityIntent(Intent intent) {
        when(mActivity.getIntent()).thenReturn(intent);
    }

    private void verifyActivityFinished() {
        verify(mActivity).finish();
    }

    private void verifyActivityNeverFinished() {
        verify(mActivity, never()).finish();
    }

    private void verifyControllersNeverUsed() {
        verify(mSpiedFragment, never()).use(any(), anyInt());
    }

    private void verifyControllersUsed() {
        verify(mSpiedFragment).use(DeviceAdminAddHeaderPreferenceController.class,
                R.string.pk_device_admin_add_header);
        verifySetDeviceAdmin(mHeaderController);

        verify(mSpiedFragment).use(DeviceAdminAddExplanationPreferenceController.class,
                R.string.pk_device_admin_add_explanation);
        verifySetDeviceAdmin(mExplanationController);
        verify(mExplanationController).setExplanation(EXPLANATION);

        verify(mSpiedFragment).use(DeviceAdminAddSupportPreferenceController.class,
                R.string.pk_device_admin_add_support);
        verifySetDeviceAdmin(mSupportController);

        verify(mSpiedFragment).use(DeviceAdminAddWarningPreferenceController.class,
                R.string.pk_device_admin_add_warning);
        verifySetDeviceAdmin(mWarningController);

        verify(mSpiedFragment).use(DeviceAdminAddPoliciesPreferenceController.class,
                R.string.pk_device_admin_add_policies);
        verifySetDeviceAdmin(mPoliciesController);

        verify(mSpiedFragment).use(DeviceAdminAddActionPreferenceController.class,
                R.string.pk_device_admin_add_action);
        verifySetDeviceAdmin(mActionController);

        verify(mSpiedFragment).use(DeviceAdminAddCancelPreferenceController.class,
                R.string.pk_device_admin_add_cancel);
        verifySetDeviceAdmin(mCancelController);
    }

    private void verifySetDeviceAdmin(BaseDeviceAdminAddPreferenceController<?> controller) {
        verify(controller).setDeviceAdmin(argThat(info->info.getComponent().equals(mDefaultAdmin)));
    }
}
