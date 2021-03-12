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

import static com.android.car.settings.common.ActionButtonsPreference.ActionButtons;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;

import com.android.car.settings.R;
import com.android.car.settings.common.ActionButtonInfo;
import com.android.car.settings.common.ActionButtonsPreference;
import com.android.car.settings.common.ConfirmationDialogFragment;
import com.android.car.settings.common.FragmentController;

/**
 * Displays the action buttons for user details.
 *
 * <p>The actions shown depends on the current and selected user.
 * <ol>
 * <li>Rename: shown if selected user is the current user
 * <li>Make admin: shown if current user is an admin and the selected user is not
 * <li>Delete: shown if the current user is allowed to remove users and is not a demo user
 * </ol>
 */
public final class UserDetailsActionButtonsPreferenceController
        extends UserDetailsBasePreferenceController<ActionButtonsPreference> {

    @VisibleForTesting
    static final String MAKE_ADMIN_DIALOG_TAG = "MakeAdminDialogFragment";

    private final UserHelper mUserHelper;
    private final UserManager mUserManager;

    @VisibleForTesting
    final ConfirmationDialogFragment.ConfirmListener mMakeAdminConfirmListener =
            arguments -> {
                UserInfo userToMakeAdmin =
                        (UserInfo) arguments.get(UsersDialogProvider.KEY_USER_TO_MAKE_ADMIN);
                android.car.userlib.UserHelper.grantAdminPermissions(getContext(), userToMakeAdmin);
                getFragmentController().goBack();
            };

    private final RemoveUserHandler mRemoveUserHandler;

    public UserDetailsActionButtonsPreferenceController(Context context,
            String preferenceKey, FragmentController fragmentController,
            CarUxRestrictions uxRestrictions) {
        this(context, preferenceKey, fragmentController, uxRestrictions,
                UserHelper.getInstance(context), UserManager.get(context),
                new RemoveUserHandler(context, UserHelper.getInstance(context),
                        UserManager.get(context), fragmentController));
    }

    @VisibleForTesting
    UserDetailsActionButtonsPreferenceController(Context context,
            String preferenceKey, FragmentController fragmentController,
            CarUxRestrictions uxRestrictions, UserHelper userHelper, UserManager userManager,
            RemoveUserHandler removeUserHandler) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mUserHelper = userHelper;
        mUserManager = userManager;
        mRemoveUserHandler = removeUserHandler;
    }

    @Override
    protected Class<ActionButtonsPreference> getPreferenceType() {
        return ActionButtonsPreference.class;
    }

    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();

        ConfirmationDialogFragment makeAdminDialog =
                (ConfirmationDialogFragment) getFragmentController().findDialogByTag(
                        MAKE_ADMIN_DIALOG_TAG);
        ConfirmationDialogFragment.resetListeners(
                makeAdminDialog,
                mMakeAdminConfirmListener,
                /* rejectListener= */ null,
                /* neutralListener= */ null);

        mRemoveUserHandler.resetListeners();

        ActionButtonInfo renameButton = getPreference().getButton(ActionButtons.BUTTON1);
        ActionButtonInfo makeAdminButton = getPreference().getButton(ActionButtons.BUTTON2);
        ActionButtonInfo manageProfilesButton = getPreference().getButton(ActionButtons.BUTTON3);
        ActionButtonInfo deleteButton = getPreference().getButton(ActionButtons.BUTTON4);

        renameButton
                .setText(R.string.bluetooth_rename_button)
                .setIcon(R.drawable.ic_edit)
                .setVisible(mUserHelper.isCurrentProcessUser(getUserInfo()))
                .setOnClickListener(v -> getFragmentController().launchFragment(
                        EditUsernameFragment.newInstance(getUserInfo())));

        makeAdminButton
                .setText(R.string.grant_admin_permissions_button_text)
                .setIcon(R.drawable.ic_person)
                .setVisible(UserUtils.isAdminViewingNonAdmin(UserManager.get(getContext()),
                        getUserInfo()))
                .setOnClickListener(v -> showConfirmMakeAdminDialog());

        manageProfilesButton
                .setText(R.string.manage_other_profiles_button_text)
                .setIcon(R.drawable.ic_people)
                .setVisible(mUserManager.isAdminUser()
                        && mUserHelper.isCurrentProcessUser(getUserInfo()))
                .setOnClickListener(v -> getFragmentController().launchFragment(
                        new UsersListFragment()));

        // Do not show delete button if the current user can't remove the selected user
        deleteButton
                .setText(R.string.delete_button)
                .setIcon(R.drawable.ic_delete)
                .setVisible(mRemoveUserHandler.canRemoveUser(getUserInfo()))
                .setOnClickListener(v -> mRemoveUserHandler.showConfirmRemoveUserDialog());
    }

    @Override
    public void setUserInfo(UserInfo userInfo) {
        super.setUserInfo(userInfo);
        mRemoveUserHandler.setUserInfo(userInfo);
    }

    private void showConfirmMakeAdminDialog() {
        ConfirmationDialogFragment dialogFragment =
                UsersDialogProvider.getConfirmGrantAdminDialogFragment(getContext(),
                        mMakeAdminConfirmListener, /* rejectListener= */ null, getUserInfo());

        getFragmentController().showDialog(dialogFragment, MAKE_ADMIN_DIALOG_TAG);
    }
}
