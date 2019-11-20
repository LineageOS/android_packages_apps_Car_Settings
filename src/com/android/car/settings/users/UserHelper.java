/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.annotation.Nullable;
import android.app.ActivityManager;
import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import com.android.car.settings.R;
import com.android.internal.annotations.VisibleForTesting;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class for providing basic user logic that applies across the Settings app for Cars.
 */
public class UserHelper {
    private static final String TAG = "UserHelper";
    private static UserHelper sInstance;

    private final UserManager mUserManager;
    private final Resources mResources;
    private final CarUserManagerHelper mCarUserManagerHelper;
    private final String mDefaultAdminName;

    /**
     * Returns an instance of UserHelper.
     */
    public static UserHelper getInstance(Context context) {
        if (sInstance == null) {
            Context appContext = context.getApplicationContext();
            Resources resources = appContext.getResources();
            sInstance = new UserHelper(UserManager.get(appContext), resources,
                    resources.getString(com.android.internal.R.string.owner_name),
                    new CarUserManagerHelper(appContext));
        }
        return sInstance;
    }

    @VisibleForTesting
    UserHelper(UserManager userManager, Resources resources, String defaultAdminName,
            CarUserManagerHelper carUserManagerHelper) {
        mUserManager = userManager;
        mResources = resources;
        mCarUserManagerHelper = carUserManagerHelper;
        mDefaultAdminName = defaultAdminName;
    }

    /**
     * Tries to remove the user that's passed in. System user cannot be removed.
     * If the user to be removed is user currently running the process,
     * it switches to the guest user first, and then removes the user.
     * If the user being removed is the last admin user, this will create a new admin user.
     *
     * @param userInfo User to be removed
     * @return {@code true} if user is successfully removed, {@code false} otherwise.
     */
    public boolean removeUser(UserInfo userInfo) {
        if (userInfo.id == UserHandle.USER_SYSTEM) {
            Log.w(TAG, "User " + userInfo.id + " is system user, could not be removed.");
            return false;
        }

        // Try to create a new admin before deleting the current one.
        if (userInfo.isAdmin() && getAllAdminUsers().size() <= 1) {
            return removeLastAdmin(userInfo);
        }

        if (!mUserManager.isAdminUser() && !isCurrentProcessUser(userInfo)) {
            // If the caller is non-admin, they can only delete themselves.
            Log.e(TAG, "Non-admins cannot remove other users.");
            return false;
        }

        // If the ID being removed is the current foreground user, we need to handle switching to
        // another user first
        if (userInfo.id == ActivityManager.getCurrentUser()) {
            if (mUserManager.getUserSwitchability() != UserManager.SWITCHABILITY_STATUS_OK) {
                // If we can't switch to a different user, we can't exit this one and therefore
                // can't delete it.
                Log.w(TAG, "User switching is not allowed. Current user cannot be deleted");
                return false;
            }
            mCarUserManagerHelper.startGuestSession(mResources.getString(R.string.user_guest));
        }

        return mUserManager.removeUser(userInfo.id);
    }

    private boolean removeLastAdmin(UserInfo userInfo) {
        if (Log.isLoggable(TAG, Log.INFO)) {
            Log.i(TAG, "User " + userInfo.id
                    + " is the last admin user on device. Creating a new admin.");
        }

        UserInfo newAdmin = createNewAdminUser(mDefaultAdminName);
        if (newAdmin == null) {
            Log.w(TAG, "Couldn't create another admin, cannot delete current user.");
            return false;
        }

        mCarUserManagerHelper.switchToUserId(newAdmin.id);
        return mUserManager.removeUser(userInfo.id);
    }


    /**
     * Creates a new user on the system, the created user would be granted admin role.
     * Only admins can create other admins.
     *
     * @param userName Name to give to the newly created user.
     * @return Newly created admin user, null if failed to create a user.
     */
    @Nullable
    private UserInfo createNewAdminUser(String userName) {
        if (!(mUserManager.isAdminUser() || mUserManager.isSystemUser())) {
            // Only Admins or System user can create other privileged users.
            Log.e(TAG, "Only admin users and system user can create other admins.");
            return null;
        }

        UserInfo user = mUserManager.createUser(userName, UserInfo.FLAG_ADMIN);
        if (user == null) {
            // Couldn't create user, most likely because there are too many.
            Log.w(TAG, "can't create admin user.");
            return null;
        }
        new UserIconProvider().assignDefaultIcon(mUserManager, mResources, user);

        return user;
    }

    /**
     * Checks if the current process user can modify accounts. Demo and Guest users cannot modify
     * accounts even if the DISALLOW_MODIFY_ACCOUNTS restriction is not applied.
     */
    public boolean canCurrentProcessModifyAccounts() {
        return !mUserManager.hasUserRestriction(UserManager.DISALLOW_MODIFY_ACCOUNTS)
                && !mUserManager.isDemoUser()
                && !mUserManager.isGuestUser();
    }

    /**
     * Returns a list of {@code UserInfo} representing all users that can be brought to the
     * foreground.
     */
    public List<UserInfo> getAllUsers() {
        return getAllLivingUsers(/* filter= */ null);
    }

    /**
     * Returns a list of {@code UserInfo} representing all users that can be swapped with the
     * current user into the foreground.
     */
    public List<UserInfo> getAllSwitchableUsers() {
        final int foregroundUserId = ActivityManager.getCurrentUser();
        return getAllLivingUsers(userInfo -> userInfo.id != foregroundUserId);
    }

    /**
     * Returns a list of {@code UserInfo} representing all users that are non-ephemeral and are
     * valid to have in the foreground.
     */
    public List<UserInfo> getAllPersistentUsers() {
        return getAllLivingUsers(userInfo -> !userInfo.isEphemeral());
    }

    /**
     * Returns a list of {@code UserInfo} representing all admin users and are
     * valid to have in the foreground.
     */
    public List<UserInfo> getAllAdminUsers() {
        return getAllLivingUsers(UserInfo::isAdmin);
    }

    /**
     * Gets all users that are not dying.  This method will handle
     * {@link UserManager#isHeadlessSystemUserMode} and ensure the system user is not
     * part of the return list when the flag is on.
     * @param filter Optional filter to apply to the list of users.  Pass null to skip.
     * @return An optionally filtered list containing all living users
     */
    private List<UserInfo> getAllLivingUsers(@Nullable Predicate<? super UserInfo> filter) {
        Stream<UserInfo> filteredListStream =
                mUserManager.getUsers(/* excludeDying= */ true).stream();

        if (filter != null) {
            filteredListStream = filteredListStream.filter(filter);
        }

        if (UserManager.isHeadlessSystemUserMode()) {
            filteredListStream =
                    filteredListStream.filter(userInfo -> userInfo.id != UserHandle.USER_SYSTEM);
        }
        return filteredListStream.collect(Collectors.toList());
    }

    /**
     * Checks whether passed in user is the user that's running the current process.
     *
     * @param userInfo User to check.
     * @return {@code true} if user running the process, {@code false} otherwise.
     */
    public boolean isCurrentProcessUser(UserInfo userInfo) {
        return UserHandle.myUserId() == userInfo.id;
    }

    /**
     * Gets UserInfo for the user running the caller process.
     *
     * <p>Differentiation between foreground user and current process user is relevant for
     * multi-user deployments.
     *
     * <p>Some multi-user aware components (like SystemUI) needs to run a singleton component
     * in system user. Current process user is always the same for that component, even when
     * the foreground user changes.
     *
     * @return {@link UserInfo} for the user running the current process.
     */
    public UserInfo getCurrentProcessUserInfo() {
        return mUserManager.getUserInfo(UserHandle.myUserId());
    }
}
