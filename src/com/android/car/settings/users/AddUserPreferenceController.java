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

import android.car.Car;
import android.car.drivingstate.CarUxRestrictions;
import android.car.user.CarUserManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.car.settings.R;
import com.android.car.settings.common.ConfirmationDialogFragment;
import com.android.car.settings.common.ErrorDialog;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;
import com.android.internal.annotations.VisibleForTesting;

/**
 * Business Logic for preference either adds a new user or exits retail mode, depending on the
 * current user.
 */
public class AddUserPreferenceController extends PreferenceController<Preference> implements
        AddNewUserTask.AddNewUserListener{
    private static final String FACTORY_RESET_PACKAGE_NAME = "android";
    private static final String FACTORY_RESET_REASON = "ExitRetailModeConfirmed";
    @VisibleForTesting
    static final String CONFIRM_EXIT_RETAIL_MODE_DIALOG_TAG =
            "com.android.car.settings.users.ConfirmExitRetailModeDialog";
    @VisibleForTesting
    static final String CONFIRM_CREATE_NEW_USER_DIALOG_TAG =
            "com.android.car.settings.users.ConfirmCreateNewUserDialog";
    @VisibleForTesting
    static final String MAX_USERS_LIMIT_REACHED_DIALOG_TAG =
            "com.android.car.settings.users.MaxUsersLimitReachedDialog";

    private Car mCar;
    private UserManager mUserManager;
    private CarUserManager mCarUserManager;

    @VisibleForTesting
    AsyncTask mAddNewUserTask;
    /** Indicates that a task is running. */
    private boolean mIsBusy;

    @VisibleForTesting
    final ConfirmationDialogFragment.ConfirmListener mConfirmCreateNewUserListener = arguments -> {
        mAddNewUserTask = new AddNewUserTask(getContext(),
                mCarUserManager, /* addNewUserListener= */ this).execute(
                getContext().getString(R.string.user_new_user_name));
        mIsBusy = true;
        refreshUi();
    };

    /**
     * Will perform a factory reset. Copied from
     * {@link com.android.settings.MainClearConfirm#doMainClear()}
     */
    private final ConfirmationDialogFragment.ConfirmListener mConfirmExitRetailModeListener =
            arguments -> {
                Intent intent = new Intent(Intent.ACTION_FACTORY_RESET);
                intent.setPackage(FACTORY_RESET_PACKAGE_NAME);
                intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                intent.putExtra(Intent.EXTRA_REASON, FACTORY_RESET_REASON);
                intent.putExtra(Intent.EXTRA_WIPE_EXTERNAL_STORAGE, true);
                intent.putExtra(Intent.EXTRA_WIPE_ESIMS, true);
                getContext().sendBroadcast(intent);
                // Intent handling is asynchronous -- assume it will happen soon.
            };

    public AddUserPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController,
            CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mCar = Car.createCar(context);
        mCarUserManager = (CarUserManager) mCar.getCarManager(Car.CAR_USER_SERVICE);
        mUserManager = UserManager.get(context);
    }

    @Override
    protected Class<Preference> getPreferenceType() {
        return Preference.class;
    }

    @Override
    protected void onCreateInternal() {
        ConfirmationDialogFragment.resetListeners(
                (ConfirmationDialogFragment) getFragmentController().findDialogByTag(
                        CONFIRM_CREATE_NEW_USER_DIALOG_TAG),
                mConfirmCreateNewUserListener,
                /* rejectListener= */ null,
                /* neutralListener= */ null);
        ConfirmationDialogFragment.resetListeners(
                (ConfirmationDialogFragment) getFragmentController().findDialogByTag(
                        CONFIRM_EXIT_RETAIL_MODE_DIALOG_TAG),
                mConfirmExitRetailModeListener,
                /* rejectListener= */ null,
                /* neutralListener= */ null);

        if (mUserManager.isDemoUser()) {
            getPreference().setTitle(R.string.exit_retail_button_text);
        } else {
            getPreference().setTitle(R.string.user_add_user_menu);
            getPreference().setIcon(R.drawable.ic_add);
        }
    }

    @Override
    protected void onStopInternal() {
        getFragmentController().showProgressBar(false);
    }

    @Override
    protected void onDestroyInternal() {
        if (mAddNewUserTask != null) {
            mAddNewUserTask.cancel(/* mayInterruptIfRunning= */ false);
        }
        if (mCar != null) {
            mCar.disconnect();
        }
    }

    @Override
    public void onUserAddedSuccess() {
        mIsBusy = false;
        refreshUi();
    }

    @Override
    public void onUserAddedFailure() {
        mIsBusy = false;
        refreshUi();
        // Display failure dialog.
        getFragmentController().showDialog(
                ErrorDialog.newInstance(R.string.add_user_error_title), /* tag= */ null);
    }

    @Override
    protected void updateState(Preference preference) {
        preference.setEnabled(!mIsBusy);
        getFragmentController().showProgressBar(mIsBusy);
    }

    @Override
    protected boolean handlePreferenceClicked(Preference preference) {
        // If the user is a demo user, show a dialog asking if they want to exit retail/demo mode.
        if (mUserManager.isDemoUser()) {
            ConfirmationDialogFragment dialogFragment =
                    UsersDialogProvider.getConfirmExitRetailModeDialogFragment(getContext(),
                            mConfirmExitRetailModeListener, null);

            getFragmentController().showDialog(dialogFragment, CONFIRM_EXIT_RETAIL_MODE_DIALOG_TAG);
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
        if (canCurrentProcessAddUsers()) {
            ConfirmationDialogFragment dialogFragment =
                    UsersDialogProvider.getConfirmCreateNewUserDialogFragment(getContext(),
                            mConfirmCreateNewUserListener, null);

            getFragmentController().showDialog(dialogFragment, CONFIRM_CREATE_NEW_USER_DIALOG_TAG);
            return true;
        }
        return false;
    }

    @Override
    protected int getAvailabilityStatus() {
        if (mUserManager.isDemoUser() || canCurrentProcessAddUsers()) {
            return AVAILABLE;
        }
        return DISABLED_FOR_USER;
    }

    @VisibleForTesting
    void setUserManager(UserManager userManager) {
        mUserManager = userManager;
    }

    @VisibleForTesting
    void setCarUserManager(CarUserManager carUserManager) {
        mCarUserManager = carUserManager;
    }

    private boolean canCurrentProcessAddUsers() {
        return !mUserManager.hasUserRestriction(UserManager.DISALLOW_ADD_USER);
    }
}
