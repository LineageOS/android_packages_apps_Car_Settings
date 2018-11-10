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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertThrows;

import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.content.pm.UserInfo;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.common.ErrorDialog;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.testutils.ShadowCarUserManagerHelper;
import com.android.car.settings.testutils.ShadowUserIconProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowCarUserManagerHelper.class, ShadowUserIconProvider.class})
public class ChooseNewAdminPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "choose_new_admin";
    private static final UserInfo TEST_ADMIN_USER = new UserInfo(/* id= */ 10,
            "TEST_USER_NAME", /* flags= */ 0);
    private static final UserInfo TEST_OTHER_USER = new UserInfo(/* id= */ 11,
            "TEST_OTHER_NAME", /* flags= */ 0);

    private Context mContext;
    private ChooseNewAdminPreferenceController mController;
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private CarUserManagerHelper mCarUserManagerHelper;
    @Mock
    private FragmentController mFragmentController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        MockitoAnnotations.initMocks(this);
        ShadowCarUserManagerHelper.setMockInstance(mCarUserManagerHelper);
        mController = new ChooseNewAdminPreferenceController(mContext, PREFERENCE_KEY,
                mFragmentController);
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreferenceScreen.setKey(PREFERENCE_KEY);
    }

    @After
    public void tearDown() {
        ShadowCarUserManagerHelper.reset();
    }

    @Test
    public void testOnCreate_noAdminInfoSet_throwsError() {
        assertThrows(IllegalStateException.class, () -> mController.onCreate());
    }

    @Test
    public void testUserClicked_opensDialog() {
        mController.setAdminInfo(TEST_ADMIN_USER);
        mController.userClicked(/* userToMakeAdmin= */ TEST_OTHER_USER);
        verify(mFragmentController).launchFragment(any(ConfirmGrantAdminPermissionsDialog.class));
    }

    @Test
    public void testAssignNewAdminAndRemoveOldAdmin_grantAdminCalled() {
        mController.setAdminInfo(TEST_ADMIN_USER);
        mController.assignNewAdminAndRemoveOldAdmin(TEST_OTHER_USER);
        verify(mCarUserManagerHelper).grantAdminPermissions(TEST_OTHER_USER);
    }

    @Test
    public void testAssignNewAdminAndRemoveOldAdmin_removeUserCalled() {
        mController.setAdminInfo(TEST_ADMIN_USER);
        mController.assignNewAdminAndRemoveOldAdmin(TEST_OTHER_USER);
        verify(mCarUserManagerHelper).removeUser(eq(TEST_ADMIN_USER), anyString());
    }

    @Test
    public void testAssignNewAdminAndRemoveOldAdmin_success_noErrorDialog() {
        mController.setAdminInfo(TEST_ADMIN_USER);
        doReturn(true).when(mCarUserManagerHelper).removeUser(TEST_ADMIN_USER,
                mContext.getString(R.string.user_guest));
        mController.assignNewAdminAndRemoveOldAdmin(TEST_OTHER_USER);
        verify(mFragmentController, never()).launchFragment(any(ErrorDialog.class));
    }

    @Test
    public void testAssignNewAdminAndRemoveOldAdmin_failure_errorDialog() {
        mController.setAdminInfo(TEST_ADMIN_USER);
        doReturn(false).when(mCarUserManagerHelper).removeUser(TEST_ADMIN_USER,
                mContext.getString(R.string.user_guest));
        mController.assignNewAdminAndRemoveOldAdmin(TEST_OTHER_USER);
        verify(mFragmentController).launchFragment(any(ErrorDialog.class));
    }
}
