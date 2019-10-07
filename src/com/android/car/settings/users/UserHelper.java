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
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.internal.annotations.VisibleForTesting;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class for providing basic user logic that applies across the Settings app for Cars.
 */
public class UserHelper {

    private static UserHelper sInstance;

    private final UserManager mUserManager;

    /**
     * Returns an instance of UserHelper.
     */
    public static UserHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new UserHelper(UserManager.get(context.getApplicationContext()));
        }
        return sInstance;
    }

    @VisibleForTesting
    UserHelper(UserManager userManager) {
        mUserManager = userManager;
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
}
