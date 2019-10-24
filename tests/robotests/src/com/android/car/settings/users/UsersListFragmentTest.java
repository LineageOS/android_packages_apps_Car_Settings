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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.widget.Button;

import com.android.car.settings.R;
import com.android.car.settings.testutils.BaseTestActivity;
import com.android.car.settings.testutils.ShadowCarUserManagerHelper;
import com.android.car.settings.testutils.ShadowUserHelper;
import com.android.car.settings.testutils.ShadowUserIconProvider;
import com.android.car.settings.testutils.ShadowUserManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

/**
 * Tests for UserDetailsFragment.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowCarUserManagerHelper.class, ShadowUserIconProvider.class,
        ShadowUserHelper.class, ShadowUserManager.class})
public class UsersListFragmentTest {

    private Context mContext;
    private BaseTestActivity mTestActivity;
    private UsersListFragment mFragment;
    private Button mActionButton;

    @Mock
    private CarUserManagerHelper mCarUserManagerHelper;

    @Mock
    private UserHelper mUserHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowCarUserManagerHelper.setMockInstance(mCarUserManagerHelper);
        ShadowUserHelper.setInstance(mUserHelper);
        mContext = RuntimeEnvironment.application;
        mTestActivity = Robolectric.setupActivity(BaseTestActivity.class);
    }

    @After
    public void tearDown() {
        ShadowUserHelper.reset();
        ShadowCarUserManagerHelper.reset();
        ShadowUserManager.reset();
    }

    /* Test that onCreateNewUserConfirmed invokes a creation of a new non-admin. */
    @Test
    public void testOnCreateNewUserConfirmedInvokesCreateNewNonAdminUser() {
        createUsersListFragment(/* flags= */ 0);
        mFragment.mConfirmCreateNewUserListener.onConfirm(/* arguments= */ null);
        Robolectric.flushBackgroundThreadScheduler();
        verify(mCarUserManagerHelper)
                .createNewNonAdminUser(mContext.getString(R.string.user_new_user_name));
    }

    /* Test that if we're in demo user, click on the button starts exit out of the retail mode. */
    @Test
    public void testCallOnClick_demoUser_exitRetailMode() {
        createUsersListFragment(UserInfo.FLAG_DEMO);
        mActionButton.callOnClick();
        assertThat(isDialogShown(UsersListFragment.CONFIRM_EXIT_RETAIL_MODE_DIALOG_TAG)).isTrue();
    }

    /* Test that if the max num of users is reached, click on the button informs user of that. */
    @Test
    public void testCallOnClick_userLimitReached_showErrorDialog() {
        ShadowUserManager.setCanAddMoreUsers(false);
        createUsersListFragment(/* flags= */ 0);

        mActionButton.callOnClick();
        assertThat(isDialogShown(UsersListFragment.MAX_USERS_LIMIT_REACHED_DIALOG_TAG)).isTrue();
    }

    /* Test that if user can add other users, click on the button creates a dialog to confirm. */
    @Test
    public void testCallOnClick_showAddUserDialog() {
        createUsersListFragment(/* flags= */ 0);

        mActionButton.callOnClick();
        assertThat(isDialogShown(UsersListFragment.CONFIRM_CREATE_NEW_USER_DIALOG_TAG)).isTrue();
    }

    private void createUsersListFragment(int flags) {
        Shadows.shadowOf(UserManager.get(mContext)).addUser(UserHandle.myUserId(),
                "User Name", flags);
        UserInfo testUser = UserManager.get(mContext).getUserInfo(UserHandle.myUserId());
        mFragment = new UsersListFragment();
        when(mUserHelper.getCurrentProcessUserInfo()).thenReturn(testUser);
        when(mUserHelper.getAllSwitchableUsers()).thenReturn(new ArrayList<>());
        when(mCarUserManagerHelper.createNewNonAdminUser(any())).thenReturn(null);
        mTestActivity.launchFragment(mFragment);
        refreshButtons();
    }

    private void refreshButtons() {
        mActionButton = mTestActivity.findViewById(R.id.action_button1);
    }

    private boolean isDialogShown(String tag) {
        return mTestActivity.getSupportFragmentManager().findFragmentByTag(tag) != null;
    }
}
