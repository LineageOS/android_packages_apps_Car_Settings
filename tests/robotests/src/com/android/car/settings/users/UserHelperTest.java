/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.car.settings.testutils.ShadowActivityManager;
import com.android.car.settings.testutils.ShadowUserManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowActivityManager.class, ShadowUserManager.class})
public class UserHelperTest {

    private UserHelper mUserHelper;

    @Mock
    private UserManager mMockUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mUserHelper = new UserHelper(mMockUserManager);

        when(mMockUserManager.hasUserRestriction(UserManager.DISALLOW_MODIFY_ACCOUNTS))
                .thenReturn(false);
        when(mMockUserManager.isDemoUser()).thenReturn(false);
        when(mMockUserManager.isGuestUser()).thenReturn(false);

        ShadowUserManager.setIsHeadlessSystemUserMode(true);
        ShadowActivityManager.setCurrentUser(10);
    }

    @After
    public void tearDown() {
        ShadowActivityManager.reset();
        ShadowUserManager.reset();
    }

    @Test
    public void canCurrentProcessModifyAccounts_baseline_returnsTrue() {
        assertThat(mUserHelper.canCurrentProcessModifyAccounts()).isTrue();
    }

    @Test
    public void canCurrentProcessModifyAccounts_hasDisallowModifyAccounts_returnsFalse() {
        when(mMockUserManager.hasUserRestriction(UserManager.DISALLOW_MODIFY_ACCOUNTS))
                .thenReturn(true);
        assertThat(mUserHelper.canCurrentProcessModifyAccounts()).isFalse();
    }

    @Test
    public void canCurrentProcessModifyAccounts_isDemoUser_returnsFalse() {
        when(mMockUserManager.isDemoUser()).thenReturn(true);
        assertThat(mUserHelper.canCurrentProcessModifyAccounts()).isFalse();
    }

    @Test
    public void canCurrentProcessModifyAccounts_isGuestUser_returnsFalse() {
        when(mMockUserManager.isGuestUser()).thenReturn(true);
        assertThat(mUserHelper.canCurrentProcessModifyAccounts()).isFalse();
    }

    @Test
    public void testGetAllSwitchableUsers() {
        // Add system user
        UserInfo systemUser = createUserInfoForId(UserHandle.USER_SYSTEM);

        // Create two non-foreground users.
        int fgUserId = ActivityManager.getCurrentUser();
        UserInfo fgUser = createUserInfoForId(fgUserId);
        UserInfo user1 = createUserInfoForId(fgUserId + 1);
        UserInfo user2 = createUserInfoForId(fgUserId + 2);

        mockGetUsers(systemUser, fgUser, user1, user2);

        // Should return all non-foreground users.
        assertThat(mUserHelper.getAllSwitchableUsers()).containsExactly(user1, user2);
    }

    @Test
    public void testGetAllSwitchableUsers_notHeadless() {
        ShadowUserManager.setIsHeadlessSystemUserMode(false);

        // Add system user
        UserInfo systemUser = createUserInfoForId(UserHandle.USER_SYSTEM);

        // Create two non-foreground users.
        int fgUserId = ActivityManager.getCurrentUser();
        UserInfo fgUser = createUserInfoForId(fgUserId);
        UserInfo user1 = createUserInfoForId(fgUserId + 1);
        UserInfo user2 = createUserInfoForId(fgUserId + 2);

        mockGetUsers(systemUser, fgUser, user1, user2);

        // Should return all non-foreground users.
        assertThat(mUserHelper.getAllSwitchableUsers()).containsExactly(systemUser, user1, user2);
    }

    @Test
    public void testGetAllPersistentUsers() {
        // Add system user
        UserInfo systemUser = createUserInfoForId(UserHandle.USER_SYSTEM);

        // Create two non-ephemeral users.
        int fgUserId = ActivityManager.getCurrentUser();
        UserInfo user1 = createUserInfoForId(fgUserId);
        UserInfo user2 = createUserInfoForId(fgUserId + 1);
        // Create two ephemeral users.
        UserInfo user3 = new UserInfo(
                fgUserId + 2, /* name = */ "user3", UserInfo.FLAG_EPHEMERAL);
        UserInfo user4 = new UserInfo(
                fgUserId + 3, /* name = */ "user4", UserInfo.FLAG_EPHEMERAL);

        mockGetUsers(systemUser, user1, user2, user3, user4);

        // Should return all non-ephemeral users.
        assertThat(mUserHelper.getAllPersistentUsers()).containsExactly(user1, user2);
    }

    @Test
    public void testGetAllPersistentUsers_notHeadless() {
        ShadowUserManager.setIsHeadlessSystemUserMode(false);

        // Add system user
        UserInfo systemUser = createUserInfoForId(UserHandle.USER_SYSTEM);

        // Create two non-ephemeral users.
        int fgUserId = ActivityManager.getCurrentUser();
        UserInfo user1 = createUserInfoForId(fgUserId);
        UserInfo user2 = createUserInfoForId(fgUserId + 1);
        // Create two ephemeral users.
        UserInfo user3 = new UserInfo(
                fgUserId + 2, /* name = */ "user3", UserInfo.FLAG_EPHEMERAL);
        UserInfo user4 = new UserInfo(
                fgUserId + 3, /* name = */ "user4", UserInfo.FLAG_EPHEMERAL);

        mockGetUsers(systemUser, user1, user2, user3, user4);

        // Should return all non-ephemeral users.
        assertThat(mUserHelper.getAllPersistentUsers()).containsExactly(systemUser, user1, user2);
    }

    private UserInfo createUserInfoForId(int id) {
        UserInfo userInfo = new UserInfo();
        userInfo.id = id;
        return userInfo;
    }

    private void mockGetUsers(UserInfo... users) {
        List<UserInfo> testUsers = new ArrayList<>(Arrays.asList(users));
        when(mMockUserManager.getUsers()).thenReturn(testUsers);
        when(mMockUserManager.getUsers(true)).thenReturn(testUsers);
        when(mMockUserManager.getUsers(false)).thenReturn(testUsers);
    }
}
