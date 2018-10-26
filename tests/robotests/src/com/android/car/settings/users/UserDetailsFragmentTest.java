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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.testutils.BaseTestActivity;
import com.android.car.settings.testutils.ShadowCarUserManagerHelper;
import com.android.car.settings.testutils.ShadowTextListItem;
import com.android.car.settings.testutils.ShadowUserIconProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;

/**
 * Tests for UserDetailsFragment.
 */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowUserIconProvider.class, ShadowTextListItem.class,
        ShadowCarUserManagerHelper.class})
public class UserDetailsFragmentTest {
    private BaseTestActivity mTestActivity;
    private UserDetailsFragment mUserDetailsFragment;

    @Mock
    private CarUserManagerHelper mCarUserManagerHelper;
    @Mock
    private UserManager mUserManager;

    private Button mRemoveUserButton;

    @Before
    public void setUpTestActivity() {
        MockitoAnnotations.initMocks(this);
        ShadowCarUserManagerHelper.setMockInstance(mCarUserManagerHelper);

        ShadowApplication shadowApp = ShadowApplication.getInstance();
        shadowApp.setSystemService(Context.USER_SERVICE, mUserManager);

        mTestActivity = Robolectric.buildActivity(BaseTestActivity.class)
                .setup()
                .get();
    }

    @After
    public void tearDown() {
        ShadowCarUserManagerHelper.reset();
    }

    @Test
    public void testFragmentTitle_adminViewingNonAdmin() {
        doReturn(true).when(mCarUserManagerHelper).isCurrentProcessAdminUser();
        String userName = "Test User";
        UserInfo testUser = new UserInfo(/* id= */ 10, userName, /* flags= */ 0);
        createUserDetailsFragment(testUser);

        TextView titleView = mTestActivity.findViewById(R.id.title);
        assertThat(titleView.getText())
                .isEqualTo(application.getString(R.string.user_details_admin_title, userName));
    }

    @Test
    public void testFragmentTitle_adminViewingAdmin() {
        doReturn(true).when(mCarUserManagerHelper).isCurrentProcessAdminUser();
        String userName = "Test User";
        UserInfo testUser = new UserInfo(/* id= */ 10, userName, /* flags= */ UserInfo.FLAG_ADMIN);
        createUserDetailsFragment(testUser);

        TextView titleView = mTestActivity.findViewById(R.id.title);
        assertThat(titleView.getText()).isEqualTo(userName);
    }

    @Test
    public void testFragmentTitle_nonAdmin() {
        doReturn(false).when(mCarUserManagerHelper).isCurrentProcessAdminUser();
        String userName = "Test User";
        UserInfo testUser = new UserInfo(/* id= */ 10, userName, /* flags= */ 0);
        createUserDetailsFragment(testUser);

        TextView titleView = mTestActivity.findViewById(R.id.title);
        assertThat(titleView.getText()).isEqualTo(userName);
    }

    /**
     * Tests that if the current user cannot remove other users, the removeUserButton is hidden.
     */
    @Test
    public void testRemoveUserButtonHiddenWhenCurrentUserCannotRemoveUsers() {
        doReturn(true).when(mCarUserManagerHelper).canCurrentProcessRemoveUsers();
        doReturn(true).when(mCarUserManagerHelper).canUserBeRemoved(any());
        doReturn(false).when(mCarUserManagerHelper).isCurrentProcessDemoUser();
        createUserDetailsFragment();

        assertThat(mRemoveUserButton.getVisibility()).isEqualTo(View.VISIBLE);

        doReturn(false).when(mCarUserManagerHelper).canCurrentProcessRemoveUsers();
        refreshFragment();

        assertThat(mRemoveUserButton.getVisibility()).isEqualTo(View.GONE);
    }

    /**
     * Tests that if the user cannot be removed, the remove button is hidden.
     */
    @Test
    public void testRemoveUserButtonHiddenIfUserCannotBeRemoved() {
        doReturn(true).when(mCarUserManagerHelper).canCurrentProcessRemoveUsers();
        doReturn(true).when(mCarUserManagerHelper).canUserBeRemoved(any());
        doReturn(false).when(mCarUserManagerHelper).isCurrentProcessDemoUser();
        createUserDetailsFragment();

        assertThat(mRemoveUserButton.getVisibility()).isEqualTo(View.VISIBLE);

        doReturn(false).when(mCarUserManagerHelper).canUserBeRemoved(any());
        refreshFragment();

        assertThat(mRemoveUserButton.getVisibility()).isEqualTo(View.GONE);
    }

    /**
     * Tests that demo user cannot remove other users.
     */
    @Test
    public void testRemoveUserButtonHiddenForDemoUser() {
        doReturn(true).when(mCarUserManagerHelper).canCurrentProcessRemoveUsers();
        doReturn(true).when(mCarUserManagerHelper).canUserBeRemoved(any());
        doReturn(false).when(mCarUserManagerHelper).isCurrentProcessDemoUser();
        createUserDetailsFragment();

        assertThat(mRemoveUserButton.getVisibility()).isEqualTo(View.VISIBLE);

        doReturn(true).when(mCarUserManagerHelper).isCurrentProcessDemoUser();
        refreshFragment();

        assertThat(mRemoveUserButton.getVisibility()).isEqualTo(View.GONE);
    }

    /* Test that clicking remove user button creates a confirm user removal dialog. */
    @Test
    public void testRemoveUserButtonClick() {
        doReturn(true).when(mCarUserManagerHelper).canCurrentProcessRemoveUsers();
        doReturn(true).when(mCarUserManagerHelper).canUserBeRemoved(any());
        doReturn(false).when(mCarUserManagerHelper).isCurrentProcessDemoUser();
        createUserDetailsFragment();

        assertThat(mRemoveUserButton.getVisibility()).isEqualTo(View.VISIBLE);
        mRemoveUserButton.callOnClick();

        assertThat(mUserDetailsFragment.getFragmentManager().findFragmentByTag(
                UserDetailsFragment.CONFIRM_REMOVE_USER_DIALOG_TAG)).isNotNull();
    }

    /* Test that edit icon does not show up for non-current users. */
    @Test
    public void testEditIconNotAvailableForNonCurrentUser() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Test User", /* flags= */ 0);
        doReturn(false).when(mCarUserManagerHelper).isCurrentProcessUser(testUser);
        createUserDetailsFragment(testUser);

        // Edit not visible for non-current user.
        assertThat(getUserItem().getSupplementalIconDrawableId()).isEqualTo(0);
    }

    /* Test that edit becomes available for current user. (Each user can edit themselves) */
    @Test
    public void testEditIconAvailableForCurrentUser() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Test User", /* flags= */ 0);
        doReturn(true).when(mCarUserManagerHelper).isCurrentProcessUser(testUser);
        createUserDetailsFragment(testUser);

        // Edit icon available for current user.
        assertThat(getUserItem().getSupplementalIconDrawableId())
                .isEqualTo(R.drawable.ic_mode_edit);
    }

    /* Test that clicking edit launches EditUsernameFragment. */
    @Test
    @Ignore
    public void testEditIconInvokesEditUsernameFragment() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Test User", /* flags= */ 0);
        doReturn(true).when(mCarUserManagerHelper).isCurrentProcessUser(testUser);
        createUserDetailsFragment(testUser);

        // Invoke clicking on edit icon.
        getUserItem().getSupplementalIconOnClickListener().onClick(null);

        // Verify EditUsernameFragment is launched.
        assertThat((EditUsernameFragment) mUserDetailsFragment.getFragmentManager()
                .findFragmentById(R.id.fragment_container)).isNotNull();
    }

    /* /* Test that successful user removal invokes back press. */
    @Test
    public void testBackButtonPressedWhenRemoveUserSuccessful() {
        doReturn(true).when(mCarUserManagerHelper).removeUser(any(), any());
        createUserDetailsFragment();
        mUserDetailsFragment.removeUser();

        assertThat(mTestActivity.getOnBackPressedFlag()).isTrue();
    }

    /* Test that removeUser invokes user removal. */
    @Test
    public void testRemoveUserInvokesRemoveUser() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Test User", /* flags= */ 0);
        createUserDetailsFragment(testUser);
        mUserDetailsFragment.removeUser();

        verify(mCarUserManagerHelper).removeUser(
                testUser, application.getString(R.string.user_guest));
    }

    /* Test that the fragment title refreshes after user name has been updated. */
    @Test
    public void testOnUsersUpdateRefreshesTitle() {
        int userId = 10;

        String oldUserName = "Test User";
        UserInfo testUser = new UserInfo(userId, oldUserName, /* flags= */ 0);
        createUserDetailsFragment(testUser);
        TextView titleView = mTestActivity.findViewById(R.id.title);

        assertThat(titleView.getText()).isEqualTo(oldUserName);

        String newUserName = "New Test User";
        UserInfo changedUser = new UserInfo(userId, newUserName, /* flags= */ 0);
        // Update the UserInfo for this user ID.
        doReturn(changedUser).when(mUserManager).getUserInfo(userId);

        // Trigger the refresh.
        mUserDetailsFragment.onUsersUpdate();

        // Title should reflect the name on the new UserInfo.
        assertThat(titleView.getText()).isEqualTo(newUserName);
    }

    /* Test that the fragment refreshes after user name has been updated. */
    @Test
    public void testOnUsersUpdateRefreshesUserItem() {
        int userId = 10;

        String oldUserName = "Test User";
        UserInfo testUser = new UserInfo(userId, oldUserName, /* flags= */ 0);
        createUserDetailsFragment(testUser);

        assertThat(getUserItem().getTitle()).isEqualTo(oldUserName);

        String newUserName = "New Test User";
        UserInfo changedUser = new UserInfo(userId, newUserName, /* flags= */ 0);
        // Update the UserInfo for this user ID.
        doReturn(changedUser).when(mUserManager).getUserInfo(userId);

        // Trigger the refresh.
        mUserDetailsFragment.onUsersUpdate();

        // Title should reflect the name on the new UserInfo.
        assertThat(getUserItem().getTitle()).isEqualTo(newUserName);
    }

    @Test
    public void testOnDestroyUnregistersListener() {
        createUserDetailsFragment();

        verify(mCarUserManagerHelper).registerOnUsersUpdateListener(mUserDetailsFragment);

        mUserDetailsFragment.onDestroy();

        verify(mCarUserManagerHelper).unregisterOnUsersUpdateListener(mUserDetailsFragment);
    }

    @Test
    public void testNonAdminManagementItemProviderCreated() {
        UserInfo nonAdmin = new UserInfo(/* id= */ 10, "Non admin", /* flags= */ 0);
        doReturn(true).when(mCarUserManagerHelper).isCurrentProcessAdminUser();

        createUserDetailsFragment(nonAdmin);

        assertThat(mUserDetailsFragment.getItemProvider())
                .isInstanceOf(NonAdminManagementItemProvider.class);
    }

    /* Test that trying to grant admin permissions creates a confirm grant admin dialog. */
    @Ignore // Failing with IllegalStateException in android.graphics.text.MeasuredText.Builder
    @Test
    public void testOnGrantAdminPermission_confirmDialogShown() {
        createUserDetailsFragment();

        mUserDetailsFragment.onGrantAdminPermission();

        assertThat(mUserDetailsFragment.getFragmentManager().findFragmentByTag(
                UserDetailsFragment.CONFIRM_GRANT_ADMIN_DIALOG_TAG)).isNotNull();
    }

    @Test
    public void testGrantAdmin() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Non admin", /* flags= */ 0);
        createUserDetailsFragment(testUser);

        mUserDetailsFragment.grantAdmin();

        verify(mCarUserManagerHelper).grantAdminPermissions(testUser);
    }

    @Test
    public void testHasCreateUserPermission_cannotCreateUsers_shouldBeFalse() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Non admin", /* flags= */ 0);
        doReturn(true).when(mCarUserManagerHelper).hasUserRestriction(
                eq(UserManager.DISALLOW_ADD_USER), eq(testUser));
        createUserDetailsFragment(testUser);

        assertThat(mUserDetailsFragment.hasCreateUserPermission()).isFalse();
    }

    @Test
    public void testHasCreateUserPermission_canCreateUsers_shouldBeTrue() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Non admin", /* flags= */ 0);
        doReturn(false).when(mCarUserManagerHelper).hasUserRestriction(
                eq(UserManager.DISALLOW_ADD_USER), eq(testUser));
        createUserDetailsFragment(testUser);

        assertThat(mUserDetailsFragment.hasCreateUserPermission()).isTrue();
    }


    @Test
    public void testOnCreateUserPermissionChanged() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Non admin", /* flags= */ 0);
        createUserDetailsFragment(testUser);

        mUserDetailsFragment.onCreateUserPermissionChanged(/* enabled= */ true);

        verify(mCarUserManagerHelper).setUserRestriction(
                eq(testUser), eq(UserManager.DISALLOW_ADD_USER), eq(false));
    }

    @Test
    public void testHasOutgoingCallsPermission_cannotMakeOutgoingCalls_shouldBeFalse() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Non admin", /* flags= */ 0);
        doReturn(true).when(mCarUserManagerHelper).hasUserRestriction(
                eq(UserManager.DISALLOW_OUTGOING_CALLS), eq(testUser));
        createUserDetailsFragment(testUser);

        assertThat(mUserDetailsFragment.hasOutgoingCallsPermission()).isFalse();
    }

    @Test
    public void testHasOutgoingCallsPermission_cantMakeOutgoingCalls_shouldBeTrue() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Non admin", /* flags= */ 0);
        doReturn(false).when(mCarUserManagerHelper).hasUserRestriction(
                eq(UserManager.DISALLOW_OUTGOING_CALLS), eq(testUser));
        createUserDetailsFragment(testUser);

        assertThat(mUserDetailsFragment.hasOutgoingCallsPermission()).isTrue();
    }


    @Test
    public void testOnGrantOutgoingCallsPermission() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Non admin", /* flags= */ 0);
        createUserDetailsFragment(testUser);

        mUserDetailsFragment.onOutgoingCallsPermissionChanged(/* enabled= */ true);

        verify(mCarUserManagerHelper).setUserRestriction(
                eq(testUser), eq(UserManager.DISALLOW_OUTGOING_CALLS), eq(false));
    }

    @Test
    public void testHasMessagingPermission_cannotMessage_shouldBeFalse() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Non admin", /* flags= */ 0);
        doReturn(true).when(mCarUserManagerHelper).hasUserRestriction(
                eq(UserManager.DISALLOW_SMS), eq(testUser));
        createUserDetailsFragment(testUser);

        assertThat(mUserDetailsFragment.hasSmsMessagingPermission()).isFalse();
    }

    @Test
    public void testHasMessagingPermission_canMessage_shouldBeTrue() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Non admin", /* flags= */ 0);
        doReturn(false).when(mCarUserManagerHelper).hasUserRestriction(
                eq(UserManager.DISALLOW_SMS), eq(testUser));
        createUserDetailsFragment(testUser);

        assertThat(mUserDetailsFragment.hasSmsMessagingPermission()).isTrue();
    }

    @Test
    public void testOnGrantMessagingPermission() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Non admin", /* flags= */ 0);
        createUserDetailsFragment(testUser);

        mUserDetailsFragment.onSmsMessagingPermissionChanged(/* enabled= */ true);

        verify(mCarUserManagerHelper).setUserRestriction(
                eq(testUser), eq(UserManager.DISALLOW_SMS), eq(false));
    }

    @Test
    public void testHasInstallAppsPermission_cannotInstallApps_shouldBeFalse() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Non admin", /* flags= */ 0);
        doReturn(true).when(mCarUserManagerHelper).hasUserRestriction(
                eq(UserManager.DISALLOW_INSTALL_APPS), eq(testUser));
        createUserDetailsFragment(testUser);

        assertThat(mUserDetailsFragment.hasInstallAppsPermission()).isFalse();
    }

    @Test
    public void testHasInstallAppsPermission_canInstallApps_shouldBeTrue() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Non admin", /* flags= */ 0);
        doReturn(false).when(mCarUserManagerHelper).hasUserRestriction(
                eq(UserManager.DISALLOW_INSTALL_APPS), eq(testUser));
        createUserDetailsFragment(testUser);

        assertThat(mUserDetailsFragment.hasInstallAppsPermission()).isTrue();
    }

    @Test
    public void testOnGrantInstallAppsPermission() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Non admin", /* flags= */ 0);
        createUserDetailsFragment(testUser);

        mUserDetailsFragment.onInstallAppsPermissionChanged(/* enabled= */ true);

        verify(mCarUserManagerHelper).setUserRestriction(
                eq(testUser), eq(UserManager.DISALLOW_INSTALL_APPS), eq(false));
    }

    @Test
    public void testHasUninstallAppsPermission_cannotUninstallApps_shouldBeFalse() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Non admin", /* flags= */ 0);
        doReturn(true).when(mCarUserManagerHelper).hasUserRestriction(
                eq(UserManager.DISALLOW_UNINSTALL_APPS), eq(testUser));
        createUserDetailsFragment(testUser);

        assertThat(mUserDetailsFragment.hasUninstallAppsPermission()).isFalse();
    }

    @Test
    public void testHasUninstallAppsPermission_canUninstallApps_shouldBeTrue() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Non admin", /* flags= */ 0);
        doReturn(false).when(mCarUserManagerHelper).hasUserRestriction(
                eq(UserManager.DISALLOW_UNINSTALL_APPS), eq(testUser));
        createUserDetailsFragment(testUser);

        assertThat(mUserDetailsFragment.hasUninstallAppsPermission()).isTrue();
    }

    @Test
    public void testOnGrantUninstallAppsPermission() {
        UserInfo testUser = new UserInfo(/* id= */ 10, "Non admin", /* flags= */ 0);
        createUserDetailsFragment(testUser);

        mUserDetailsFragment.onUninstallAppsPermissionChanged(/* enabled= */ true);

        verify(mCarUserManagerHelper).setUserRestriction(
                eq(testUser), eq(UserManager.DISALLOW_UNINSTALL_APPS), eq(false));
    }

    private void createUserDetailsFragment(UserInfo userInfo) {
        UserInfo testUser = userInfo == null ? new UserInfo() : userInfo;

        mUserDetailsFragment = UserDetailsFragment.newInstance(testUser.id);
        doReturn(testUser).when(mUserManager).getUserInfo(testUser.id);
        mTestActivity.launchFragment(mUserDetailsFragment);

        refreshButtons();
    }

    private void createUserDetailsFragment() {
        createUserDetailsFragment(null);
    }

    private void refreshFragment() {
        mTestActivity.reattachFragment(mUserDetailsFragment);

        refreshButtons();
    }

    private void refreshButtons() {
        // Get buttons;
        mRemoveUserButton = (Button) mTestActivity.findViewById(R.id.action_button1);
    }

    private ShadowTextListItem getUserItem() {
        return Shadow.extract(mUserDetailsFragment.getItemProvider().get(0));
    }
}
