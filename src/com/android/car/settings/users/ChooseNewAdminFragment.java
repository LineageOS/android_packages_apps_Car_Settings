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
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.car.widget.ListItemProvider;

import com.android.car.settings.R;
import com.android.car.settings.common.ErrorDialog;
import com.android.car.settings.common.ListItemSettingsFragment;

import static java.util.Objects.requireNonNull;

/**
 * This screen appears after the last admin on the device tries to delete themselves. (but only if
 * there are other users on the device)
 *
 * <p> It lets the Admin see a list of non-Admins on the device and choose a user from the list to
 * upgrade to Admin.
 *
 * <p> After new admin has been selected and upgraded, the old Admin is removed.
 */
public class ChooseNewAdminFragment extends ListItemSettingsFragment
        implements CarUserManagerHelper.OnUsersUpdateListener,
        UsersItemProvider.UserClickListener {
    private static final String CONFIRM_GRANT_ADMIN_DIALOG_TAG = "ConfirmGrantAdminDialog";

    private UsersItemProvider mItemProvider;
    private CarUserManagerHelper mCarUserManagerHelper;
    private UserInfo mAdminInfo;

    /**
     * Creates a new instance of {@link ChooseNewAdminFragment} that enables the last remaining
     * admin to choose a new Admin from a list of Non-Admins.
     *
     * @param adminInfo Admin that will get removed after new admin has been designated.
     */
    public static ChooseNewAdminFragment newInstance(UserInfo adminInfo) {
        ChooseNewAdminFragment usersListFragment = new ChooseNewAdminFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(Intent.EXTRA_USER, adminInfo);
        usersListFragment.setArguments(bundle);
        return usersListFragment;
    }

    @Override
    @LayoutRes
    protected int getActionBarLayoutId() {
        return R.layout.action_bar_with_button;
    }

    @Override
    @StringRes
    protected int getTitleId() {
        return R.string.choose_new_admin_label;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdminInfo = (UserInfo) requireNonNull(getArguments()).getParcelable(Intent.EXTRA_USER);

        mCarUserManagerHelper = new CarUserManagerHelper(getContext());
        mItemProvider = new UsersItemProvider.Builder(getContext(), mCarUserManagerHelper)
                .setOnUserClickListener(this)
                .setIncludeCurrentUser(false)
                .setIncludeGuest(false)
                .create();


        // Register to receive changes to the users.
        mCarUserManagerHelper.registerOnUsersUpdateListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Button cancelBtn = (Button) getActivity().findViewById(R.id.action_button1);
        cancelBtn.setVisibility(View.VISIBLE);
        cancelBtn.setText(R.string.cancel);
        cancelBtn.setOnClickListener(v -> getActivity().onBackPressed());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mCarUserManagerHelper.unregisterOnUsersUpdateListener(this);
    }

    @Override
    public void onUsersUpdate() {
        mItemProvider.refreshItems();
        refreshList();
    }

    @Override
    public void onUserClicked(final UserInfo userToMakeAdmin) {
        ConfirmGrantAdminPermissionsDialog dialog = new ConfirmGrantAdminPermissionsDialog();
        dialog.setConfirmGrantAdminListener(
                () -> assignNewAdminAndRemoveOldAdmin(userToMakeAdmin));
        dialog.show(getFragmentManager(), CONFIRM_GRANT_ADMIN_DIALOG_TAG);
    }

    @VisibleForTesting
    void assignNewAdminAndRemoveOldAdmin(UserInfo userToMakeAdmin) {
        mCarUserManagerHelper.grantAdminPermissions(userToMakeAdmin);

        requireActivity().onBackPressed();
        removeOldAdmin();
    }

    private void removeOldAdmin() {
        if (!mCarUserManagerHelper.removeUser(
                mAdminInfo, getContext().getString(R.string.user_guest))) {
            // If failed, need to show error dialog for users.
            ErrorDialog.show(this, R.string.delete_user_error_title);
        }
    }

    @Override
    public ListItemProvider getItemProvider() {
        return mItemProvider;
    }
}
