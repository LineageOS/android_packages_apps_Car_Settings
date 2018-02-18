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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.design.widget.TextInputEditText;
import android.view.View;
import android.widget.Button;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.TestConfig;
import com.android.car.settings.testutils.ShadowActivityManager;
import com.android.car.settings.testutils.TestAppCompatActivity;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {ShadowActivityManager.class})
@Ignore
/**
 * Tests for EditUsernameFragment.
 */
public class EditUsernameFragmentTest {
    private TestAppCompatActivity mTestActivity;

    @Mock
    private UserManager mUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mTestActivity = Robolectric.buildActivity(TestAppCompatActivity.class)
                .create()
                .start()
                .resume()
                .get();

        mTestActivity.setUserManager(mUserManager);
    }

    /**
     * Tests that user name of the profile in question is displayed in the TextInputEditTest field.
     */
    @Test
    public void testUserNameDisplayedInDetails() {
        int userId = 123;
        int differentUserId = 345;
        createEditUsernameFragment(userId, differentUserId);

        TextInputEditText userNameEditText =
                (TextInputEditText) mTestActivity.findViewById(R.id.user_name_text_edit);
        assertThat(userNameEditText.getText().toString()).isEqualTo("test_name");
    }

    /**
     * Tests that non-owner users can edit their own name.
     */
    @Test
    public void testIfNotOwnerCanEditThemselves() {
        int userId = 123;
        createEditUsernameFragment(userId, userId); // User editing their own profile.

        TextInputEditText userNameEditText =
                (TextInputEditText) mTestActivity.findViewById(R.id.user_name_text_edit);
        assertThat(userNameEditText.isEnabled()).isTrue();
    }

    /**
     * Tests that non-owner users cannot edit the names of other users.
     */
    @Test
    public void testIfNotOwnerCanNotEditOthers() {
        int userId = 123;
        int differentUserId = 345;
        // Non-owner trying to edit someone else's profile.
        createEditUsernameFragment(userId, differentUserId);

        TextInputEditText userNameEditText =
                (TextInputEditText) mTestActivity.findViewById(R.id.user_name_text_edit);
        assertThat(userNameEditText.isEnabled()).isFalse();
    }

    /**
     * Tests that owner user can edit everyone's name.
     */
    @Test
    public void testIfOwnerCanEditOthers() {
        int differentUserId = 345;
        // Owner editing someone else's profile. UserHandle.USER_SYSTEM is the owner id.
        createEditUsernameFragment(UserHandle.USER_SYSTEM, differentUserId);

        TextInputEditText userNameEditText =
                (TextInputEditText) mTestActivity.findViewById(R.id.user_name_text_edit);
        assertThat(userNameEditText.isEnabled()).isTrue();
    }

    /**
     * Tests that ok and cancel button are hidden by default.
     */
    @Test
    public void testEditButtonsHiddenByDefault() {
        int differentUserId = 345;
        // Owner editing someone else's profile. UserHandle.USER_SYSTEM is the owner id.
        createEditUsernameFragment(UserHandle.USER_SYSTEM, differentUserId);

        Button okButton = (Button) mTestActivity.findViewById(R.id.ok_button);
        assertThat(okButton.getVisibility()).isEqualTo(View.GONE);

        Button cancelButton = (Button) mTestActivity.findViewById(R.id.cancel_button);
        assertThat(cancelButton.getVisibility()).isEqualTo(View.GONE);
    }

    /**
     * Tests that ok and cancel button appear after the user starts editing the name.
     */
    @Test
    public void testEditButtonsAppearOnFocus() {
        int differentUserId = 345;
        // Owner editing someone else's profile. UserHandle.USER_SYSTEM is the owner id.
        createEditUsernameFragment(UserHandle.USER_SYSTEM, differentUserId);

        TextInputEditText userNameEditText =
                (TextInputEditText) mTestActivity.findViewById(R.id.user_name_text_edit);
        Button cancelButton = (Button) mTestActivity.findViewById(R.id.cancel_button);
        Button okButton = (Button) mTestActivity.findViewById(R.id.ok_button);

        // Buttons appear when text edit is in focus.
        userNameEditText.requestFocus();
        assertThat(okButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(cancelButton.getVisibility()).isEqualTo(View.VISIBLE);
    }

    /**
     * Tests that clicking OK saves the new name for the user.
     */
    @Test
    public void testClickingOkSavesNewUserName() {
        int differentUserId = 345;
        // Owner editing someone else's profile. UserHandle.USER_SYSTEM is the owner id.
        createEditUsernameFragment(UserHandle.USER_SYSTEM, differentUserId);
        TextInputEditText userNameEditText =
                (TextInputEditText) mTestActivity.findViewById(R.id.user_name_text_edit);
        Button okButton = (Button) mTestActivity.findViewById(R.id.ok_button);

        userNameEditText.requestFocus();
        userNameEditText.setText("new_user_name");
        okButton.callOnClick();

        verify(mUserManager).setUserName(differentUserId, "new_user_name");
    }

    /**
     * Tests that clicking Cancel brings us back to the previous fragment in activity.
     */
    @Test
    public void testClickingCancelInvokesGoingBack() {
        int differentUserId = 345;
        // Owner editing someone else's profile. UserHandle.USER_SYSTEM is the owner id.
        createEditUsernameFragment(UserHandle.USER_SYSTEM, differentUserId);
        TextInputEditText userNameEditText =
                (TextInputEditText) mTestActivity.findViewById(R.id.user_name_text_edit);
        Button cancelButton = (Button) mTestActivity.findViewById(R.id.cancel_button);

        userNameEditText.requestFocus();
        userNameEditText.setText("new_user_name");

        mTestActivity.clearOnBackPressedFlag();
        cancelButton.callOnClick();

        // Back called.
        assertThat(mTestActivity.getOnBackPressedFlag()).isTrue();

        // New user name is not saved.
        verify(mUserManager, never()).setUserName(differentUserId, "new_user_name");
    }

    /**
     * Tests that if the DISALLOW_REMOVE_USER restriction is on, the removeUserButton is hidden
     */
    @Test
    public void testDisallowRemoveUsersPermissionHidesRemoveUserButton() {
        int userId = 123;
        int differentUserId = 345;
        doReturn(true).when(mUserManager)
                .hasUserRestriction(UserManager.DISALLOW_REMOVE_USER);

        createEditUsernameFragment(userId, differentUserId);

        Button removeUserButton = (Button) mTestActivity.findViewById(R.id.action_button2);

        assertThat(removeUserButton.getVisibility()).isEqualTo(View.GONE);
    }

    /**
     * Tests that if the DISALLOW_USER_SWITCH restriction is on, the switchUserButton is hidden
     */
    @Test
    public void testDisallowSwitchUsersPermissionHidesSwitchUserButton() {
        int userId = 123;
        int differentUserId = 345;
        doReturn(true).when(mUserManager)
                .hasUserRestriction(UserManager.DISALLOW_USER_SWITCH);

        createEditUsernameFragment(userId, differentUserId);

        Button switchUserButton = (Button) mTestActivity.findViewById(R.id.action_button1);

        assertThat(switchUserButton.getVisibility()).isEqualTo(View.GONE);
    }

    private void createEditUsernameFragment(int currentUserId, int detailsUserId) {
        UserInfo testUser = new UserInfo(detailsUserId /* id */, "test_name", 0 /* flags */);
        doReturn(testUser).when(mUserManager).getUserInfo(detailsUserId);


        if (currentUserId == UserHandle.USER_SYSTEM) {
            doReturn(true).when(mUserManager).isSystemUser();
        }

        UserInfo currentUser = new UserInfo(currentUserId, "current_user", 0 /* flags */);
        doReturn(currentUser).when(mUserManager).getUserInfo(UserHandle.myUserId());

        EditUsernameFragment fragment = EditUsernameFragment.getInstance(testUser);
        mTestActivity.createFragment(fragment);
    }
}
