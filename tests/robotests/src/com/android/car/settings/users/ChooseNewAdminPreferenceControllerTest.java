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

package com.android.car.settings.users;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertThrows;

import android.content.Context;
import android.content.pm.UserInfo;

import androidx.lifecycle.Lifecycle;

import com.android.car.settings.common.ConfirmationDialogFragment;
import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.car.settings.testutils.ShadowUserHelper;
import com.android.car.settings.testutils.ShadowUserIconProvider;
import com.android.car.settings.testutils.ShadowUserManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserIconProvider.class,
        ShadowUserManager.class, ShadowUserHelper.class})
public class ChooseNewAdminPreferenceControllerTest {

    private static final UserInfo TEST_ADMIN_USER = new UserInfo(/* id= */ 10,
            "TEST_USER_NAME", /* flags= */ 0);
    private static final UserInfo TEST_OTHER_USER = new UserInfo(/* id= */ 11,
            "TEST_OTHER_NAME", /* flags= */ 0);

    private Context mContext;
    private PreferenceControllerTestHelper<ChooseNewAdminPreferenceController> mControllerHelper;
    private ChooseNewAdminPreferenceController mController;
    private ConfirmationDialogFragment mDialog;

    @Mock
    private UserHelper mUserHelper;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        MockitoAnnotations.initMocks(this);
        ShadowUserHelper.setInstance(mUserHelper);
        mControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                ChooseNewAdminPreferenceController.class);
        mController = mControllerHelper.getController();
        mController.setAdminInfo(TEST_ADMIN_USER);
        mControllerHelper.setPreference(new LogicalPreferenceGroup(mContext));
        mDialog = new ConfirmationDialogFragment.Builder(mContext).build();
    }

    @After
    public void tearDown() {
        ShadowUserManager.reset();
    }

    @Test
    public void testOnCreate_hasPreviousDialog_dialogListenerSet() {
        when(mControllerHelper.getMockFragmentController().findDialogByTag(
                ConfirmationDialogFragment.TAG)).thenReturn(mDialog);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        assertThat(mDialog.getConfirmListener()).isNotNull();
    }

    @Test
    public void testCheckInitialized_noAdminInfoSet_throwsError() {
        assertThrows(IllegalStateException.class,
                () -> new PreferenceControllerTestHelper<>(mContext,
                        ChooseNewAdminPreferenceController.class,
                        new LogicalPreferenceGroup(mContext)));
    }

    @Test
    public void testUserClicked_opensDialog() {
        mController.userClicked(/* userToMakeAdmin= */ TEST_OTHER_USER);

        verify(mControllerHelper.getMockFragmentController()).showDialog(
                any(),
                eq(ConfirmationDialogFragment.TAG));
    }

    @Test
    @Ignore("b/172513940")
    public void testAssignNewAdminAndRemoveOldAdmin_grantAdminCalled() {
        mController.assignNewAdminAndRemoveOldAdmin(TEST_OTHER_USER);

        // verify(mCarUserManagerHelper).grantAdminPermissions(TEST_OTHER_USER);
    }

    @Test
    public void testAssignNewAdminAndRemoveOldAdmin_removeUserCalled() {
        mController.assignNewAdminAndRemoveOldAdmin(TEST_OTHER_USER);

        verify(mUserHelper).removeUser(any(), eq(TEST_ADMIN_USER));
    }

    @Test
    public void testAssignNewAdminAndRemoveOldAdmin_success_noErrorDialog() {
        when(mUserHelper.removeUser(any(), eq(TEST_ADMIN_USER)))
                .thenReturn(UserHelper.REMOVE_USER_RESULT_SUCCESS);

        mController.assignNewAdminAndRemoveOldAdmin(TEST_OTHER_USER);

        verify(mControllerHelper.getMockFragmentController(), never()).showDialog(any(), any());
    }

    @Test
    public void testAssignNewAdminAndRemoveOldAdmin_failure_errorDialog() {
        when(mUserHelper.removeUser(any(), eq(TEST_ADMIN_USER)))
                .thenReturn(UserHelper.REMOVE_USER_RESULT_FAILED);

        mController.assignNewAdminAndRemoveOldAdmin(TEST_OTHER_USER);

        verify(mControllerHelper.getMockFragmentController()).showDialog(any(), any());
    }
}
