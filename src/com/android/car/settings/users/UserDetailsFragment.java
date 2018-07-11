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

import android.car.user.CarUserManagerHelper;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.car.widget.ListItemProvider;

import com.android.car.settings.R;
import com.android.car.settings.common.ListItemSettingsFragment;

/**
 * Shows details for a user with the ability to remove user and edit current user.
 */
public class UserDetailsFragment extends ListItemSettingsFragment implements
        ConfirmRemoveUserDialog.ConfirmRemoveUserListener,
        UserDetailsItemProvider.EditUserListener,
        CarUserManagerHelper.OnUsersUpdateListener,
        NonAdminManagementItemProvider.AssignAdminListener,
        ConfirmAssignAdminPrivilegesDialog.ConfirmAssignAdminListener {
    public static final String EXTRA_USER_ID = "extra_user_id";
    private static final String ERROR_DIALOG_TAG = "RemoveUserErrorDialogTag";
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static final String CONFIRM_REMOVE_DIALOG_TAG = "ConfirmRemoveUserDialog";
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static final String CONFIRM_ASSIGN_ADMIN_DIALOG_TAG = "ConfirmAssignAdminDialog";

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    CarUserManagerHelper mCarUserManagerHelper;
    private AbstractRefreshableListItemProvider mItemProvider;
    private int mUserId;
    private UserInfo mUserInfo;

    /**
     * Creates instance of UserDetailsFragment.
     */
    public static UserDetailsFragment newInstance(int userId) {
        UserDetailsFragment userDetailsFragment = new UserDetailsFragment();
        Bundle bundle = ListItemSettingsFragment.getBundle();
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_with_button);
        bundle.putInt(EXTRA_TITLE_ID, R.string.user_details_title);
        bundle.putInt(EXTRA_USER_ID, userId);
        userDetailsFragment.setArguments(bundle);
        return userDetailsFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserId = getArguments().getInt(EXTRA_USER_ID);

        if (savedInstanceState != null) {
            ConfirmRemoveUserDialog confirmRemoveUserDialog = (ConfirmRemoveUserDialog)
                    getFragmentManager().findFragmentByTag(CONFIRM_REMOVE_DIALOG_TAG);
            if (confirmRemoveUserDialog != null) {
                confirmRemoveUserDialog.setConfirmRemoveUserListener(this);
            }

            ConfirmAssignAdminPrivilegesDialog confirmAssignAdminDialog =
                    (ConfirmAssignAdminPrivilegesDialog) getFragmentManager()
                            .findFragmentByTag(CONFIRM_ASSIGN_ADMIN_DIALOG_TAG);
            if (confirmAssignAdminDialog != null) {
                confirmAssignAdminDialog.setConfirmAssignAdminListener(this);
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mCarUserManagerHelper = new CarUserManagerHelper(getContext());
        mUserInfo = UserUtils.getUserInfo(getContext(), mUserId);
        mItemProvider = getUserDetailsItemProvider();

        mCarUserManagerHelper.registerOnUsersUpdateListener(this);

        // Needs to be called after creation of item provider.
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
    public void onAssignAdminClicked() {
        ConfirmAssignAdminPrivilegesDialog dialog = new ConfirmAssignAdminPrivilegesDialog();
        dialog.setConfirmAssignAdminListener(this);
        dialog.show(getFragmentManager(), CONFIRM_ASSIGN_ADMIN_DIALOG_TAG);
    }

    @Override
    public void onAssignAdminConfirmed() {
        mCarUserManagerHelper.assignAdminPrivileges(mUserInfo);
        getActivity().onBackPressed();
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
    public void onRemoveUserConfirmed() {
        removeUser();
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
        if (mCarUserManagerHelper.isCurrentProcessAdminUser() && !mUserInfo.isAdmin()) {
            // Admins should be able to manage non-admins and upgrade their privileges.
            return new NonAdminManagementItemProvider(mUserId, getContext(), this,
                    mCarUserManagerHelper);
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
        titleView.setText(UserListItem.getUserItemTitle(mUserInfo,
                mCarUserManagerHelper.isCurrentProcessUser(mUserInfo), getContext()));
    }

    private void removeUser() {
        if (mCarUserManagerHelper.removeUser(
                mUserInfo, getContext().getString(R.string.user_guest))) {
            getActivity().onBackPressed();
        } else {
            // If failed, need to show error dialog for users.
            RemoveUserErrorDialog removeUserErrorDialog = new RemoveUserErrorDialog();
            removeUserErrorDialog.show(getFragmentManager(), ERROR_DIALOG_TAG);
        }
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
        removeUserBtn.setOnClickListener(v -> {
            ConfirmRemoveUserDialog dialog = new ConfirmRemoveUserDialog();
            dialog.setConfirmRemoveUserListener(this);
            dialog.show(getFragmentManager(), CONFIRM_REMOVE_DIALOG_TAG);
        });
    }
}
