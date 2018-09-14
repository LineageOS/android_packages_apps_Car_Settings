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

import static org.mockito.Mockito.doReturn;
import static org.robolectric.RuntimeEnvironment.application;

import android.car.userlib.CarUserManagerHelper;
import android.content.pm.UserInfo;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.testutils.ShadowTextListItem;
import com.android.car.settings.testutils.ShadowUserIconProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

/**
 * Tests for UserListItemGenerator.
 */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = { ShadowUserIconProvider.class, ShadowTextListItem.class })
public class UserListItemTest {
    private UserListItem mUserItem;

    @Mock
    private CarUserManagerHelper mCarUserManagerHelper;

    @Before
    public void setUpMocks() {
        MockitoAnnotations.initMocks(this);
    }

    /* Test that user title for current user indicates that the user is currently signed in. */
    @Test
    public void testItemTitleForCurrentUser() {
        String testName = "Test User";
        UserInfo testUser = new UserInfo(/* id= */ 10, testName, /* flags= */ 0);
        doReturn(true).when(mCarUserManagerHelper).isCurrentProcessUser(testUser);
        createUserListItem(testUser);

        assertThat(getUserItem().getTitle()).isEqualTo(
                application.getString(R.string.current_user_name, testName));
    }

    /* Test that user title for non-current user is just user's name. */
    @Test
    public void testItemTitleForNonCurrentUser() {
        String testName = "Test User";
        UserInfo testUser = new UserInfo(/* id= */ 10, testName, /* flags= */ 0);
        doReturn(false).when(mCarUserManagerHelper).isCurrentProcessUser(testUser);
        createUserListItem(testUser);

        assertThat(getUserItem().getTitle()).isEqualTo(testName);
    }

    /* Test that summary for currently logged in Admin indicated that the admin is signed in. */
    @Test
    public void testItemSummaryForCurrentAdmin() {
        UserInfo initializedAdmin = new UserInfo(/* id= */ 10, "Test User",
                UserInfo.FLAG_ADMIN | UserInfo.FLAG_INITIALIZED);
        doReturn(true).when(mCarUserManagerHelper).isCurrentProcessUser(initializedAdmin);
        createUserListItem(initializedAdmin);

        assertThat(getUserItem().getBody()).isEqualTo(
                application.getString(R.string.signed_in_admin_user));
    }

    /* Test that summary for admin that's not currently logged in just states that user is admin. */
    @Test
    public void testItemSummaryForOtherAdmin() {
        UserInfo initializedAdmin = new UserInfo(/* id= */ 10, "Test User",
                UserInfo.FLAG_ADMIN | UserInfo.FLAG_INITIALIZED);
        doReturn(false).when(mCarUserManagerHelper).isCurrentProcessUser(initializedAdmin);
        createUserListItem(initializedAdmin);

        assertThat(getUserItem().getBody()).isEqualTo(application.getString(R.string.user_admin));
    }

    /* Test there is no summary for non-current, non-admin user. */
    @Test
    public void testItemSummaryForNonCurrentUser() {
        UserInfo initializedUser =
                new UserInfo(/* id= */ 10, "Test User", UserInfo.FLAG_INITIALIZED);
        doReturn(false).when(mCarUserManagerHelper).isCurrentProcessUser(initializedUser);
        createUserListItem(initializedUser);

        assertThat(getUserItem().getBody()).isNull();
    }

    /* Test that summary for user that is not initialized indicates that user is not set up. */
    @Test
    public void testSummaryForNonInitializedUser() {
        UserInfo nonInitializedUser = new UserInfo(/* id= */ 10, "Test User", /* flags= */ 0);
        createUserListItem(nonInitializedUser);

        assertThat(getUserItem().getBody())
                .isEqualTo(application.getString(R.string.user_summary_not_set_up));
    }

    private void createUserListItem(UserInfo userInfo) {
        mUserItem = new UserListItem(userInfo,
                RuntimeEnvironment.application.getApplicationContext(), mCarUserManagerHelper);
    }

    private ShadowTextListItem getUserItem() {
        return Shadow.extract(mUserItem);
    }
}
