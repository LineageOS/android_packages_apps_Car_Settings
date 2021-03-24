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

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;

import com.android.car.settings.common.ConfirmationDialogFragment;
import com.android.car.settings.common.ErrorDialog;
import com.android.car.settings.common.FragmentController;

/**
 * Consolidates user removal logic into one handler so we can have consistent logic across various
 * parts of the Settings app.
 */
public class RemoveUserHandler {
    @VisibleForTesting
    static final String REMOVE_USER_DIALOG_TAG = "RemoveUserDialogFragment";

    private final Context mContext;
    private final UserHelper mUserHelper;
    private final UserManager mUserManager;
    private final FragmentController mFragmentController;

    private UserInfo mUserInfo;

    @VisibleForTesting
    ConfirmationDialogFragment.ConfirmListener mRemoveConfirmListener;

    public RemoveUserHandler(Context context, UserHelper userHelper, UserManager userManager,
            FragmentController fragmentController) {
        mContext = context;
        mUserHelper = userHelper;
        mUserManager = userManager;
        mFragmentController = fragmentController;
    }

    /**
     * Sets the user info to be handled for removal
     * @param userInfo UserInfo of the user to remove
     */
    public void setUserInfo(UserInfo userInfo) {
        mUserInfo = userInfo;

        mRemoveConfirmListener = arguments -> {
            String userType = arguments.getString(UsersDialogProvider.KEY_USER_TYPE);
            if (userType.equals(UsersDialogProvider.LAST_ADMIN)) {
                mFragmentController.launchFragment(
                        ChooseNewAdminFragment.newInstance(userInfo));
            } else {
                int removeUserResult = mUserHelper.removeUser(mContext, mUserInfo);
                if (removeUserResult == UserHelper.REMOVE_USER_RESULT_SUCCESS) {
                    mFragmentController.goBack();
                } else {
                    // If failed, need to show error dialog for users.
                    mFragmentController.showDialog(
                            ErrorDialog.newInstance(
                                    mUserHelper.getErrorMessageForUserResult(removeUserResult)),
                            null);
                }
            }
        };
    }

    /**
     * Resets listeners as they can get unregistered with certain configuration changes.
     */
    public void resetListeners() {
        ConfirmationDialogFragment removeUserDialog =
                (ConfirmationDialogFragment) mFragmentController.findDialogByTag(
                        REMOVE_USER_DIALOG_TAG);

        ConfirmationDialogFragment.resetListeners(
                removeUserDialog,
                mRemoveConfirmListener,
                /* rejectListener= */ null,
                /* neutralListener= */ null);
    }

    /**
     * Checks to see if the current active user can delete the requested user.
     * @param userInfo UserInfo of the user to delete
     * @return True if the user can be deleted by the current active user. False otherwise.
     */
    public boolean canRemoveUser(UserInfo userInfo) {
        return !mUserManager.hasUserRestriction(UserManager.DISALLOW_REMOVE_USER)
                && userInfo.id != UserHandle.USER_SYSTEM
                && !mUserManager.isDemoUser();
    }

    /**
     * Show the remove user confirmation dialog. This will handle edge cases such as removing
     * the last user or removing the last admin user.
     */
    public void showConfirmRemoveUserDialog() {
        boolean isLastUser = mUserHelper.getAllPersistentUsers().size() == 1;
        boolean isLastAdmin = mUserInfo.isAdmin()
                && mUserHelper.getAllAdminUsers().size() == 1;

        ConfirmationDialogFragment dialogFragment;

        if (isLastUser) {
            dialogFragment = UsersDialogProvider.getConfirmRemoveLastUserDialogFragment(
                    mContext, mRemoveConfirmListener, /* rejectListener= */ null);
        } else if (isLastAdmin) {
            dialogFragment = UsersDialogProvider.getConfirmRemoveLastAdminDialogFragment(
                    mContext, mRemoveConfirmListener, /* rejectListener= */ null);
        } else {
            dialogFragment = UsersDialogProvider.getConfirmRemoveUserDialogFragment(mContext,
                    mRemoveConfirmListener, /* rejectListener= */ null);
        }
        mFragmentController.showDialog(dialogFragment, REMOVE_USER_DIALOG_TAG);
    }

}
