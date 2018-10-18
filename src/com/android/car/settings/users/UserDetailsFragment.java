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

import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.car.widget.ListItemProvider;

import com.android.car.settings.R;
import com.android.car.settings.common.ErrorDialog;
import com.android.car.settings.common.ListItemSettingsFragment;
import com.android.car.settings.users.ConfirmRemoveUserDialog.ConfirmRemoveUserListener;

/**
 * Shows details for a user with the ability to remove user and edit current user.
 */
public class UserDetailsFragment extends ListItemSettingsFragment implements
        UserDetailsItemProvider.EditUserListener,
        CarUserManagerHelper.OnUsersUpdateListener,
        NonAdminManagementItemProvider.UserRestrictionsListener,
        NonAdminManagementItemProvider.UserRestrictionsProvider {
    @VisibleForTesting
    static final String CONFIRM_GRANT_ADMIN_DIALOG_TAG = "ConfirmGrantAdminDialog";
    @VisibleForTesting
    static final String CONFIRM_REMOVE_USER_DIALOG_TAG = "ConfirmRemoveUserDialog";
    @VisibleForTesting
    static final String CONFIRM_REMOVE_LAST_ADMIN_DIALOG_TAG = "ConfirmRemoveLastAdminDialog";


    @VisibleForTesting
    CarUserManagerHelper mCarUserManagerHelper;
    private AbstractRefreshableListItemProvider mItemProvider;
    private int mUserId;
    private UserInfo mUserInfo;

    /**
     * Creates instance of UserDetailsFragment.
     */
    public static UserDetailsFragment newInstance(int userId) {
        UserDetailsFragment userDetailsFragment = new UserDetailsFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(Intent.EXTRA_USER_ID, userId);
        userDetailsFragment.setArguments(bundle);
        return userDetailsFragment;
    }

    @Override
    @LayoutRes
    protected int getActionBarLayoutId() {
        return R.layout.action_bar_with_button;
    }

    @Override
    @StringRes
    protected int getTitleId() {
        return R.string.user_details_admin_title;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserId = getArguments().getInt(Intent.EXTRA_USER_ID);

        if (savedInstanceState != null) {
            reattachListenerToRemoveUserDialog(CONFIRM_REMOVE_LAST_ADMIN_DIALOG_TAG,
                    this::launchChooseNewAdminFragment);

            reattachListenerToRemoveUserDialog(CONFIRM_REMOVE_USER_DIALOG_TAG, this::removeUser);

            reattachListenerToGrantAdminDialog(CONFIRM_GRANT_ADMIN_DIALOG_TAG, this::grantAdmin);
        }

        mCarUserManagerHelper = new CarUserManagerHelper(getContext());
        mUserInfo = UserUtils.getUserInfo(getContext(), mUserId);
        mItemProvider = getUserDetailsItemProvider();

        mCarUserManagerHelper.registerOnUsersUpdateListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Override title.
        refreshFragmentTitle();

        showRemoveUserButton();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCarUserManagerHelper.unregisterOnUsersUpdateListener(this);
    }

    @Override
    public void onGrantAdminPermission() {
        ConfirmGrantAdminPermissionsDialog dialog = new ConfirmGrantAdminPermissionsDialog();
        dialog.setConfirmGrantAdminListener(this::grantAdmin);
        dialog.show(getFragmentManager(), CONFIRM_GRANT_ADMIN_DIALOG_TAG);
    }

    @VisibleForTesting
    void grantAdmin() {
        mCarUserManagerHelper.grantAdminPermissions(mUserInfo);
        getActivity().onBackPressed();
    }

    @Override
    public boolean hasCreateUserPermission() {
        return !mCarUserManagerHelper.hasUserRestriction(
                UserManager.DISALLOW_ADD_USER, mUserInfo);
    }

    @Override
    public boolean hasOutgoingCallsPermission() {
        return !mCarUserManagerHelper.hasUserRestriction(
                UserManager.DISALLOW_OUTGOING_CALLS, mUserInfo);
    }

    @Override
    public boolean hasSmsMessagingPermission() {
        return !mCarUserManagerHelper.hasUserRestriction(
                UserManager.DISALLOW_SMS, mUserInfo);
    }

    @Override
    public boolean hasInstallAppsPermission() {
        return !mCarUserManagerHelper.hasUserRestriction(
                UserManager.DISALLOW_INSTALL_APPS, mUserInfo);
    }

    @Override
    public boolean hasUninstallAppsPermission() {
        return !mCarUserManagerHelper.hasUserRestriction(
                UserManager.DISALLOW_UNINSTALL_APPS, mUserInfo);
    }

    @Override
    public void onCreateUserPermissionChanged(boolean granted) {
        /*
         * If the permission is granted, the DISALLOW_ADD_USER restriction should be removed and
         * vice versa.
         */
        mCarUserManagerHelper.setUserRestriction(
                mUserInfo, UserManager.DISALLOW_ADD_USER, !granted);
    }

    @Override
    public void onOutgoingCallsPermissionChanged(boolean granted) {
        /*
         * If the permission is granted, the DISALLOW_OUTGOING_CALLS restriction should be removed
         * and vice versa.
         */
        mCarUserManagerHelper.setUserRestriction(
                mUserInfo, UserManager.DISALLOW_OUTGOING_CALLS, !granted);
    }

    @Override
    public void onSmsMessagingPermissionChanged(boolean granted) {
        /*
         * If the permission is granted, the DISALLOW_SMS restriction should be removed
         * and vice versa.
         */
        mCarUserManagerHelper.setUserRestriction(
                mUserInfo, UserManager.DISALLOW_SMS, !granted);
    }

    @Override
    public void onInstallAppsPermissionChanged(boolean granted) {
        /*
         * If the permission is granted, the DISALLOW_INSTALL_APPS restriction should be removed
         * and vice versa.
         */
        mCarUserManagerHelper.setUserRestriction(
                mUserInfo, UserManager.DISALLOW_INSTALL_APPS, !granted);
    }

    @Override
    public void onUninstallAppsPermissionChanged(boolean granted) {
        /*
         * If the permission is granted, the DISALLOW_UNINSTALL_APPS restriction should be removed
         * and vice versa.
         */
        mCarUserManagerHelper.setUserRestriction(
                mUserInfo, UserManager.DISALLOW_UNINSTALL_APPS, !granted);
    }

    @Override
    public void onUsersUpdate() {
        // Refresh UserInfo, since it might have changed.
        mUserInfo = getUserInfo(mUserId);

        // Because UserInfo might have changed, we should refresh the content that depends on it.
        refreshFragmentTitle();

        // Refresh the item provider, and then the list.
        mItemProvider.refreshItems();
        refreshList();
    }

    @Override
    public void onEditUserClicked(UserInfo userInfo) {
        getFragmentController().launchFragment(EditUsernameFragment.newInstance(userInfo));
    }

    @Override
    public ListItemProvider getItemProvider() {
        return mItemProvider;
    }

    private AbstractRefreshableListItemProvider getUserDetailsItemProvider() {
        if (isAdminViewingNonAdmin()) {
            // Admins should be able to manage non-admins and upgrade their permissions.
            return new NonAdminManagementItemProvider(getContext(),
                    /* userRestrictionsListener= */ this, /* userRestrictionsProvider= */this,
                    new UserIconProvider(mCarUserManagerHelper).getUserIcon(mUserInfo,
                            getContext()));
        }
        // Admins seeing other admins, and non-admins seeing themselves, should have a simpler view.
        return new UserDetailsItemProvider(mUserId, getContext(),
                /* editUserListener= */ this, mCarUserManagerHelper);
    }

    private UserInfo getUserInfo(int userId) {
        UserManager userManager = (UserManager) getContext().getSystemService(Context.USER_SERVICE);
        return userManager.getUserInfo(userId);
    }

    private void refreshFragmentTitle() {
        TextView titleView = getActivity().findViewById(R.id.title);
        String userName = UserListItem.getUserItemTitle(mUserInfo,
                mCarUserManagerHelper.isCurrentProcessUser(mUserInfo), getContext());
        if (isAdminViewingNonAdmin()) {
            titleView.setText(getContext().getString(R.string.user_details_admin_title, userName));
            return;
        }
        titleView.setText(userName);
    }

    /**
     * Returns whether or not the current user is an admin and whether the user info they are
     * viewing is of a non-admin.
     */
    private boolean isAdminViewingNonAdmin() {
        return mCarUserManagerHelper.isCurrentProcessAdminUser() && !mUserInfo.isAdmin();
    }

    @VisibleForTesting
    void removeUser() {
        if (mCarUserManagerHelper.removeUser(
                mUserInfo, getContext().getString(R.string.user_guest))) {
            getActivity().onBackPressed();
        } else {
            // If failed, need to show error dialog for users.
            ErrorDialog.show(this, R.string.delete_user_error_title);
        }
    }

    private void launchChooseNewAdminFragment() {
        getFragmentController().launchFragment(ChooseNewAdminFragment.newInstance(mUserInfo));
    }

    private void showRemoveUserButton() {
        Button removeUserBtn = (Button) getActivity().findViewById(R.id.action_button1);
        // If the current user is not allowed to remove users, the user trying to be removed
        // cannot be removed, or the current user is a demo user, do not show delete button.
        if (!mCarUserManagerHelper.canCurrentProcessRemoveUsers()
                || !mCarUserManagerHelper.canUserBeRemoved(mUserInfo)
                || mCarUserManagerHelper.isCurrentProcessDemoUser()) {
            removeUserBtn.setVisibility(View.GONE);
            return;
        }
        removeUserBtn.setVisibility(View.VISIBLE);
        removeUserBtn.setText(R.string.delete_button);
        removeUserBtn.setOnClickListener(v -> showConfirmRemoveUserDialog());
    }

    private void showConfirmRemoveUserDialog() {
        boolean isLastUser = mCarUserManagerHelper.getAllPersistentUsers().size() == 1;
        boolean isLastAdmin = mUserInfo.isAdmin()
                && mCarUserManagerHelper.getAllAdminUsers().size() == 1;

        ConfirmRemoveUserDialog dialog;
        String tag;
        if (isLastUser) {
            dialog = ConfirmRemoveUserDialog.createForLastUser(this::removeUser);
            tag = CONFIRM_REMOVE_USER_DIALOG_TAG;
        } else if (isLastAdmin) {
            dialog = ConfirmRemoveUserDialog.createForLastAdmin(this::launchChooseNewAdminFragment);
            tag = CONFIRM_REMOVE_LAST_ADMIN_DIALOG_TAG;
        } else {
            dialog = ConfirmRemoveUserDialog.createDefault(this::removeUser);
            tag = CONFIRM_REMOVE_USER_DIALOG_TAG;
        }
        dialog.show(getFragmentManager(), tag);
    }

    private void reattachListenerToRemoveUserDialog(String tag,
            ConfirmRemoveUserListener listener) {
        ConfirmRemoveUserDialog confirmRemoveLastAdminDialog = (ConfirmRemoveUserDialog)
                getFragmentManager().findFragmentByTag(tag);
        if (confirmRemoveLastAdminDialog != null) {
            confirmRemoveLastAdminDialog.setConfirmRemoveUserListener(listener);
        }
    }

    private void reattachListenerToGrantAdminDialog(String tag,
            ConfirmGrantAdminPermissionsDialog.ConfirmGrantAdminListener listener) {
        ConfirmGrantAdminPermissionsDialog confirmGrantAdminDialog =
                (ConfirmGrantAdminPermissionsDialog) getFragmentManager().findFragmentByTag(tag);
        if (confirmGrantAdminDialog != null) {
            confirmGrantAdminDialog.setConfirmGrantAdminListener(listener);
        }
    }
}
