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

package com.android.car.settings.users;

import static android.content.pm.UserInfo.FLAG_INITIALIZED;

import static com.android.car.settings.users.RemoveUserHandler.REMOVE_USER_DIALOG_TAG;
import static com.android.car.settings.users.UsersDialogProvider.ANY_USER;
import static com.android.car.settings.users.UsersDialogProvider.KEY_USER_TYPE;
import static com.android.car.settings.users.UsersDialogProvider.LAST_ADMIN;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.ConfirmationDialogFragment;
import com.android.car.settings.common.FragmentController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class RemoveUserHandlerTest {
    private static final String TEST_USERNAME = "Test Username";

    private Context mContext = ApplicationProvider.getApplicationContext();
    private RemoveUserHandler mRemoveUserHandler;

    @Mock
    private FragmentController mMockFragmentController;
    @Mock
    private UserHelper mMockUserHelper;
    @Mock
    private UserManager mMockUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mRemoveUserHandler = new RemoveUserHandler(
                mContext, mMockUserHelper, mMockUserManager, mMockFragmentController);
    }

    @Test
    public void userNotRestricted_canRemoveUser() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        mRemoveUserHandler.setUserInfo(userInfo);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        assertThat(mRemoveUserHandler.canRemoveUser(userInfo)).isTrue();
    }

    @Test
    public void userRestricted_cannotRemoveUser() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        mRemoveUserHandler.setUserInfo(userInfo);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        when(mMockUserManager.hasUserRestriction(UserManager.DISALLOW_REMOVE_USER))
                .thenReturn(true);

        assertThat(mRemoveUserHandler.canRemoveUser(userInfo)).isFalse();
    }

    @Test
    public void viewingSystemUser_cannotRemoveUser() {
        UserInfo userInfo = new UserInfo(/* id= */ 0, TEST_USERNAME, FLAG_INITIALIZED);
        mRemoveUserHandler.setUserInfo(userInfo);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        assertThat(mRemoveUserHandler.canRemoveUser(userInfo)).isFalse();
    }

    @Test
    public void isDemoUser_cannotRemoveUser() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        mRemoveUserHandler.setUserInfo(userInfo);
        when(mMockUserManager.isDemoUser()).thenReturn(true);
        assertThat(mRemoveUserHandler.canRemoveUser(userInfo)).isFalse();
    }

    @Test
    public void hasPreviousDeleteDialog_dialogListenerSet() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        mRemoveUserHandler.setUserInfo(userInfo);
        ConfirmationDialogFragment dialog = new ConfirmationDialogFragment.Builder(
                mContext).build();
        when(mMockFragmentController.findDialogByTag(REMOVE_USER_DIALOG_TAG)).thenReturn(dialog);

        mRemoveUserHandler.resetListeners();

        assertThat(dialog.getConfirmListener()).isNotNull();
    }

    @Test
    public void showConfirmRemoveUserDialog_showsConfirmationDialog() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        mRemoveUserHandler.setUserInfo(userInfo);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);

        mRemoveUserHandler.showConfirmRemoveUserDialog();

        verify(mMockFragmentController).showDialog(any(ConfirmationDialogFragment.class),
                eq(REMOVE_USER_DIALOG_TAG));
    }

    @Test
    public void onDeleteConfirmed_removeUser() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        mRemoveUserHandler.setUserInfo(userInfo);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);

        Bundle arguments = new Bundle();
        arguments.putString(KEY_USER_TYPE, ANY_USER);
        mRemoveUserHandler.mRemoveConfirmListener.onConfirm(arguments);

        verify(mMockUserHelper).removeUser(mContext, userInfo);
    }

    @Test
    @UiThreadTest
    public void onDeleteConfirmed_lastAdmin_launchChooseNewAdminFragment() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        mRemoveUserHandler.setUserInfo(userInfo);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);

        Bundle arguments = new Bundle();
        arguments.putString(KEY_USER_TYPE, LAST_ADMIN);
        mRemoveUserHandler.mRemoveConfirmListener.onConfirm(arguments);

        verify(mMockFragmentController).launchFragment(any(ChooseNewAdminFragment.class));
    }
}
