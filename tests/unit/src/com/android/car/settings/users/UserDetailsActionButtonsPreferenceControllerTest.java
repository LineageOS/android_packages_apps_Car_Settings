/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static android.content.pm.UserInfo.FLAG_ADMIN;
import static android.content.pm.UserInfo.FLAG_INITIALIZED;

import static com.android.car.settings.common.ActionButtonsPreference.ActionButtons;
import static com.android.car.settings.users.UserDetailsActionButtonsPreferenceController.MAKE_ADMIN_DIALOG_TAG;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.ActionButtonInfo;
import com.android.car.settings.common.ActionButtonsPreference;
import com.android.car.settings.common.ConfirmationDialogFragment;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.testutils.TestLifecycleOwner;
import com.android.dx.mockito.inline.extended.ExtendedMockito;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;

@RunWith(AndroidJUnit4.class)
public class UserDetailsActionButtonsPreferenceControllerTest {
    private static final String TEST_USERNAME = "Test Username";

    private Context mContext = ApplicationProvider.getApplicationContext();
    private LifecycleOwner mLifecycleOwner;
    private ActionButtonsPreference mPreference;
    private CarUxRestrictions mCarUxRestrictions;
    private UserDetailsActionButtonsPreferenceController mPreferenceController;
    private MockitoSession mSession;

    @Mock
    private FragmentController mFragmentController;
    @Mock
    private UserHelper mMockUserHelper;
    @Mock
    private UserManager mMockUserManager;
    @Mock
    private RemoveUserHandler mRemoveUserHandler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = new TestLifecycleOwner();

        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();

        mPreference = new ActionButtonsPreference(mContext);
        mPreferenceController = new UserDetailsActionButtonsPreferenceController(mContext,
                /* preferenceKey= */ "key", mFragmentController, mCarUxRestrictions,
                mMockUserHelper, mMockUserManager, mRemoveUserHandler);
    }

    @After
    public void tearDown() {
        if (mSession != null) {
            mSession.finishMocking();
        }
    }

    @Test
    public void onCreate_isCurrentUser_renameButtonShown() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(getRenameButton().isVisible()).isTrue();
    }

    @Test
    public void onCreate_isNotCurrentUser_renameButtonHidden() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(getRenameButton().isVisible()).isFalse();
    }

    @Test
    public void onCreate_isAdminViewingNonAdmin_makeAdminButtonShown() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        when(mMockUserManager.isAdminUser()).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(getMakeAdminButton().isVisible()).isTrue();
    }

    @Test
    public void onCreate_isAdminViewingAdmin_makeAdminButtonHidden() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME,
                FLAG_INITIALIZED | FLAG_ADMIN);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        when(mMockUserManager.isAdminUser()).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(getMakeAdminButton().isVisible()).isFalse();
    }

    @Test
    public void onCreate_userIsRemovable_deleteButtonShown() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        when(mRemoveUserHandler.canRemoveUser(userInfo)).thenReturn(true);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(getDeleteButton().isVisible()).isTrue();
    }

    @Test
    public void onCreate_userIsNotRemovable_deleteButtonHidden() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        when(mRemoveUserHandler.canRemoveUser(userInfo)).thenReturn(false);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(getDeleteButton().isVisible()).isFalse();
    }

    @Test
    public void onCreate_hasPreviousMakeAdminDialog_dialogListenerSet() {
        ConfirmationDialogFragment dialog = new ConfirmationDialogFragment.Builder(
                mContext).build();
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        when(mMockUserManager.isAdminUser()).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        when(mFragmentController.findDialogByTag(
                MAKE_ADMIN_DIALOG_TAG)).thenReturn(dialog);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(dialog.getConfirmListener()).isNotNull();
    }

    @Test
    public void onCreate_hasPreviousDeleteDialog_dialogListenerSet() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        verify(mRemoveUserHandler).resetListeners();
    }

    @Test
    public void onRenameButtonClicked_launchEditUsernameFragment() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        getRenameButton().getOnClickListener().onClick(/* view= */ null);

        verify(mFragmentController).launchFragment(any(EditUsernameFragment.class));
    }

    @Test
    public void onMakeAdminButtonClicked_showsConfirmationDialog() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        when(mMockUserManager.isAdminUser()).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        getMakeAdminButton().getOnClickListener().onClick(/* view= */ null);

        verify(mFragmentController).showDialog(any(ConfirmationDialogFragment.class),
                eq(MAKE_ADMIN_DIALOG_TAG));
    }

    @Test
    public void onDeleteButtonClicked_showsConfirmationDialog() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        getDeleteButton().getOnClickListener().onClick(/* view= */ null);

        verify(mRemoveUserHandler).showConfirmRemoveUserDialog();
    }

    @Test
    public void onMakeAdminConfirmed_makeUserAdmin() {
        mSession = ExtendedMockito.mockitoSession().mockStatic(
                android.car.userlib.UserHelper.class).startMocking();

        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        when(mMockUserManager.isAdminUser()).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);

        mPreferenceController.onCreate(mLifecycleOwner);
        Bundle arguments = new Bundle();
        arguments.putParcelable(UsersDialogProvider.KEY_USER_TO_MAKE_ADMIN, userInfo);
        mPreferenceController.mMakeAdminConfirmListener.onConfirm(arguments);

        ExtendedMockito.verify(
                () -> android.car.userlib.UserHelper.grantAdminPermissions(mContext, userInfo));
    }

    @Test
    public void onMakeAdminConfirmed_goBack() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        when(mMockUserManager.isAdminUser()).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        Bundle arguments = new Bundle();
        arguments.putParcelable(UsersDialogProvider.KEY_USER_TO_MAKE_ADMIN, userInfo);
        mPreferenceController.mMakeAdminConfirmListener.onConfirm(arguments);

        verify(mFragmentController).goBack();
    }

    private ActionButtonInfo getRenameButton() {
        return mPreference.getButton(ActionButtons.BUTTON1);
    }

    private ActionButtonInfo getMakeAdminButton() {
        return mPreference.getButton(ActionButtons.BUTTON2);
    }

    private ActionButtonInfo getDeleteButton() {
        return mPreference.getButton(ActionButtons.BUTTON3);
    }
}
