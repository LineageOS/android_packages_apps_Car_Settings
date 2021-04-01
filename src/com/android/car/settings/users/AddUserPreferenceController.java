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

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.car.settings.R;
import com.android.car.settings.common.ConfirmationDialogFragment;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;
import com.android.internal.annotations.VisibleForTesting;

/**
 * Business Logic for preference either adds a new user or exits retail mode, depending on the
 * current user.
 */
public class AddUserPreferenceController extends PreferenceController<Preference> {

    @VisibleForTesting
    static final String MAX_USERS_LIMIT_REACHED_DIALOG_TAG =
            "com.android.car.settings.users.MaxUsersLimitReachedDialog";

    private UserManager mUserManager;
    private DemoUserDialogHandler mDemoUserDialogHandler;
    private AddProfileHandler mAddProfileHandler;

    public AddUserPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController,
            CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mUserManager = UserManager.get(context);
        mDemoUserDialogHandler = new DemoUserDialogHandler(context, fragmentController);
        mAddProfileHandler = new AddProfileHandler(context, fragmentController, this);
    }

    @Override
    protected Class<Preference> getPreferenceType() {
        return Preference.class;
    }

    @Override
    protected void onCreateInternal() {
        mDemoUserDialogHandler.onCreateInternal();
        mAddProfileHandler.onCreateInternal();

        if (mUserManager.isDemoUser()) {
            getPreference().setTitle(R.string.exit_retail_button_text);
        } else {
            getPreference().setTitle(R.string.add_profile_text);
            getPreference().setIcon(R.drawable.ic_add);
        }
    }

    @Override
    protected void onStopInternal() {
        mAddProfileHandler.onStopInternal();
    }

    @Override
    protected void onDestroyInternal() {
        mAddProfileHandler.onDestroyInternal();
    }

    @Override
    protected void updateState(Preference preference) {
        mAddProfileHandler.updateState(preference);
    }

    @Override
    protected boolean handlePreferenceClicked(Preference preference) {
        // If the user is a demo user, show a dialog asking if they want to exit retail/demo mode.
        if (mUserManager.isDemoUser()) {
            mDemoUserDialogHandler.showExitRetailDialog();
            return true;
        }

        // If no more users can be added because the maximum allowed number is reached, let the user
        // know.
        if (!mUserManager.canAddMoreUsers()) {
            ConfirmationDialogFragment dialogFragment =
                    UsersDialogProvider.getMaxUsersLimitReachedDialogFragment(getContext(),
                            UserHelper.getInstance(getContext()).getMaxSupportedRealUsers());

            getFragmentController().showDialog(dialogFragment, MAX_USERS_LIMIT_REACHED_DIALOG_TAG);
            return true;
        }

        // Only add the add user button if the current user is allowed to add a user.
        if (mAddProfileHandler.canAddUser(mUserManager)) {
            mAddProfileHandler.showAddProfileDialog();
            return true;
        }
        return false;
    }

    @Override
    protected int getAvailabilityStatus() {
        if (mUserManager.isDemoUser() || mAddProfileHandler.canAddUser(mUserManager)) {
            return AVAILABLE;
        }
        return DISABLED_FOR_USER;
    }

    @VisibleForTesting
    void setUserManager(UserManager userManager) {
        mUserManager = userManager;
    }
}
