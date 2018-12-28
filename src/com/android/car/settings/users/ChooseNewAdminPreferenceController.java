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

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.pm.UserInfo;

import androidx.annotation.VisibleForTesting;

import com.android.car.settings.R;
import com.android.car.settings.common.ErrorDialog;
import com.android.car.settings.common.FragmentController;

/**
 * Business logic for when the last admin is about to be removed from the device and a new
 * administrator needs to be chosen.
 */
public class ChooseNewAdminPreferenceController extends UsersBasePreferenceController {

    private final ConfirmGrantAdminPermissionsDialog.ConfirmGrantAdminListener mGrantAdminListener =
            userToMakeAdmin -> {
                assignNewAdminAndRemoveOldAdmin(userToMakeAdmin);
                getFragmentController().goBack();
            };

    private UserInfo mAdminInfo;

    public ChooseNewAdminPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        getPreferenceProvider().setIncludeCurrentUser(false);
        getPreferenceProvider().setIncludeGuest(false);
    }

    /** Setter for the user info of the admin we're deleting. */
    public void setAdminInfo(UserInfo adminInfo) {
        mAdminInfo = adminInfo;
    }

    @Override
    protected void checkInitialized() {
        if (mAdminInfo == null) {
            throw new IllegalStateException("Admin info should be set by this point");
        }
    }

    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();
        ConfirmGrantAdminPermissionsDialog dialog =
                (ConfirmGrantAdminPermissionsDialog) getFragmentController().findDialogByTag(
                        ConfirmGrantAdminPermissionsDialog.TAG);
        if (dialog != null) {
            dialog.setConfirmGrantAdminListener(mGrantAdminListener);
        }
    }

    @Override
    protected void userClicked(UserInfo userToMakeAdmin) {
        ConfirmGrantAdminPermissionsDialog dialog = new ConfirmGrantAdminPermissionsDialog();
        dialog.setUserToMakeAdmin(userToMakeAdmin);
        dialog.setConfirmGrantAdminListener(mGrantAdminListener);
        getFragmentController().showDialog(dialog, ConfirmGrantAdminPermissionsDialog.TAG);
    }

    @VisibleForTesting
    void assignNewAdminAndRemoveOldAdmin(UserInfo userToMakeAdmin) {
        getCarUserManagerHelper().grantAdminPermissions(userToMakeAdmin);
        removeOldAdmin();
    }

    private void removeOldAdmin() {
        if (!getCarUserManagerHelper().removeUser(mAdminInfo,
                getContext().getString(R.string.user_guest))) {
            // If failed, need to show error dialog for users.
            getFragmentController().showDialog(
                    ErrorDialog.newInstance(R.string.delete_user_error_title), /* tag= */ null);
        }
    }
}
