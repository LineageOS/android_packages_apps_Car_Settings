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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.StringRes;
import android.annotation.UserIdInt;
import android.app.ActivityManager;
import android.car.Car;
import android.car.user.CarUserManager;
import android.car.user.OperationResult;
import android.car.user.UserCreationResult;
import android.car.user.UserRemovalResult;
import android.car.user.UserSwitchResult;
import android.car.util.concurrent.AsyncFuture;
import android.content.Context;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.UserHandle;
import android.os.UserManager;
import android.sysprop.CarProperties;
import android.util.Log;

import com.android.car.settings.R;
import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class for providing basic user logic that applies across the Settings app for Cars.
 */
public class UserHelper {
    private static final String TAG = "UserHelper";
    private static final int TIMEOUT_MS = CarProperties.user_hal_timeout().orElse(5_000) + 500;
    private static UserHelper sInstance;

    private final UserManager mUserManager;
    private final CarUserManager mCarUserManager;
    private final Resources mResources;
    private final String mDefaultAdminName;
    private final String mDefaultGuestName;

    /**
     * Result code for when a user was successfully marked for removal and the device switched to a
     * different user.
     */
    public static final int REMOVE_USER_RESULT_SUCCESS = 0;

    /**
     * Result code for when there was a failure removing a user.
     */
    public static final int REMOVE_USER_RESULT_FAILED = 1;

    /**
     * Result code when the user was successfully marked for removal, but the switch to a new user
     * failed. In this case the user marked for removal is set as ephemeral and will be removed
     * on the next user switch or reboot.
     */
    public static final int REMOVE_USER_RESULT_SWITCH_FAILED = 2;

    /**
     * Possible return values for {@link #removeUser(int)}, which attempts to remove a user and
     * switch to a new one. Note that this IntDef is distinct from {@link UserRemovalResult}, which
     * is only a result code for the user removal operation.
     */
    @IntDef(prefix = {"REMOVE_USER_RESULT"}, value = {
            REMOVE_USER_RESULT_SUCCESS,
            REMOVE_USER_RESULT_FAILED,
            REMOVE_USER_RESULT_SWITCH_FAILED,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RemoveUserResult {
    }

    /**
     * Returns an instance of UserHelper.
     */
    public static UserHelper getInstance(Context context) {
        if (sInstance == null) {
            Context appContext = context.getApplicationContext();
            Resources resources = appContext.getResources();
            sInstance = new UserHelper(UserManager.get(appContext), resources,
                    resources.getString(com.android.internal.R.string.owner_name),
                    resources.getString(R.string.user_guest),
                    getCarUserManager(appContext));
        }
        return sInstance;
    }

    @VisibleForTesting
    UserHelper(UserManager userManager, Resources resources, String defaultAdminName,
            String defaultGuestName, CarUserManager carUserManager) {
        mUserManager = userManager;
        mResources = resources;
        mDefaultAdminName = defaultAdminName;
        mDefaultGuestName = defaultGuestName;
        mCarUserManager = carUserManager;
    }

    private static CarUserManager getCarUserManager(@NonNull Context context) {
        Car car = Car.createCar(context);
        CarUserManager carUserManager = (CarUserManager) car.getCarManager(Car.CAR_USER_SERVICE);
        return carUserManager;
    }

    /**
     * Tries to remove the user that's passed in. System user cannot be removed.
     * If the user to be removed is user currently running the process, it switches to the guest
     * user first, and then removes the user.
     * If the user being removed is the last admin user, this will create a new admin user.
     *
     * @param context  An application context
     * @param userInfo User to be removed
     * @return {@link RemoveUserResult} indicating the result status for user removal and switching
     */
    @RemoveUserResult
    public int removeUser(Context context, UserInfo userInfo) {
        if (userInfo.id == UserHandle.USER_SYSTEM) {
            Log.w(TAG, "User " + userInfo.id + " is system user, could not be removed.");
            return REMOVE_USER_RESULT_FAILED;
        }

        // Try to create a new admin before deleting the current one.
        if (userInfo.isAdmin() && getAllAdminUsers().size() <= 1) {
            return replaceLastAdmin(userInfo);
        }

        if (!mUserManager.isAdminUser() && !isCurrentProcessUser(userInfo)) {
            // If the caller is non-admin, they can only delete themselves.
            Log.e(TAG, "Non-admins cannot remove other users.");
            return REMOVE_USER_RESULT_FAILED;
        }

        if (userInfo.id == ActivityManager.getCurrentUser()) {
            return removeThisUserAndSwitchToGuest(context, userInfo);
        }

        return removeUser(userInfo.id);
    }

    /**
     * If the ID being removed is the current foreground user, we need to handle switching to
     * a new or existing guest.
     */
    @RemoveUserResult
    private int removeThisUserAndSwitchToGuest(Context context, UserInfo userInfo) {
        if (mUserManager.getUserSwitchability() != UserManager.SWITCHABILITY_STATUS_OK) {
            // If we can't switch to a different user, we can't exit this one and therefore
            // can't delete it.
            Log.w(TAG, "User switching is not allowed. Current user cannot be deleted");
            return REMOVE_USER_RESULT_FAILED;
        }
        UserInfo guestUser = createNewOrFindExistingGuest(context);
        if (guestUser == null) {
            Log.e(TAG, "Could not create a Guest user.");
            return REMOVE_USER_RESULT_FAILED;
        }

        // since the user is still current, this will set it as ephemeral
        int result = removeUser(userInfo.id);
        if (result != REMOVE_USER_RESULT_SUCCESS) {
            return result;
        }

        if (!switchUser(guestUser.id)) {
            return REMOVE_USER_RESULT_SWITCH_FAILED;
        }

        return REMOVE_USER_RESULT_SUCCESS;
    }

    @RemoveUserResult
    private int removeUser(@UserIdInt int userId) {
        UserRemovalResult result = mCarUserManager.removeUser(userId);
        if (Log.isLoggable(TAG, Log.INFO)) {
            Log.i(TAG, "Remove user result: " + result);
        }
        if (result.isSuccess()) {
            return REMOVE_USER_RESULT_SUCCESS;
        } else {
            Log.w(TAG, "Failed to remove user " + userId + ": " + result);
            return REMOVE_USER_RESULT_FAILED;
        }
    }

    private boolean switchUser(@UserIdInt int userId) {
        UserSwitchResult result = getResult("switch", mCarUserManager.switchUser(userId));
        return result != null && result.isSuccess();
    }

    /**
     * Returns the {@link StringRes} that corresponds to a {@link RemoveUserResult} result code.
     */
    @StringRes
    public int getErrorMessageForUserResult(@RemoveUserResult int result) {
        if (result == REMOVE_USER_RESULT_SWITCH_FAILED) {
            return R.string.delete_user_error_set_ephemeral_title;
        }

        return R.string.delete_user_error_title;
    }

    /**
     * Gets the result of an async operation.
     *
     * @param operation name of the operation, to be logged in case of error
     * @param future    future holding the operation result.
     * @return result of the operation or {@code null} if it failed or timed out.
     */
    @Nullable
    private static <T extends OperationResult> T getResult(String operation,
            AsyncFuture<T> future) {
        T result = null;
        try {
            result = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.w(TAG, "Interrupted waiting to " + operation + " user", e);
            return null;
        } catch (ExecutionException | TimeoutException e) {
            Log.w(TAG, "Exception waiting to " + operation + " user", e);
            return null;
        }
        if (result == null) {
            Log.w(TAG, "Time out (" + TIMEOUT_MS + " ms) trying to " + operation + " user");
            return null;
        }
        if (!result.isSuccess()) {
            Log.w(TAG, "Failed to " + operation + " user: " + result);
            return null;
        }
        return result;
    }

    @RemoveUserResult
    private int replaceLastAdmin(UserInfo userInfo) {
        if (Log.isLoggable(TAG, Log.INFO)) {
            Log.i(TAG, "User " + userInfo.id
                    + " is the last admin user on device. Creating a new admin.");
        }

        UserInfo newAdmin = createNewAdminUser(mDefaultAdminName);
        if (newAdmin == null) {
            Log.w(TAG, "Couldn't create another admin, cannot delete current user.");
            return REMOVE_USER_RESULT_FAILED;
        }

        int removeUserResult = removeUser(userInfo.id);
        if (removeUserResult != REMOVE_USER_RESULT_SUCCESS) {
            return removeUserResult;
        }

        if (switchUser(newAdmin.id)) {
            return REMOVE_USER_RESULT_SUCCESS;
        } else {
            return REMOVE_USER_RESULT_SWITCH_FAILED;
        }
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
        UserCreationResult result = getResult("create admin",
                mCarUserManager.createUser(userName, UserInfo.FLAG_ADMIN));
        if (result == null) return null;
        UserInfo user = result.getUser();

        new UserIconProvider().assignDefaultIcon(mUserManager, mResources, user);
        return user;
    }

    /**
     * Creates and returns a new guest user or returns the existing one.
     * Returns null if it fails to create a new guest.
     *
     * @param context an application context
     * @return The UserInfo representing the Guest, or null if it failed
     */
    @Nullable
    public UserInfo createNewOrFindExistingGuest(Context context) {
        // createGuest() will return null if a guest already exists.
        UserCreationResult result = getResult("create guest",
                mCarUserManager.createGuest(mDefaultGuestName));
        UserInfo newGuest = result == null ? null : result.getUser();

        if (newGuest != null) {
            new UserIconProvider().assignDefaultIcon(mUserManager, mResources, newGuest);
            return newGuest;
        }

        return mUserManager.findCurrentGuestUser();
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
    public List<UserInfo> getAllLivingUsers(@Nullable Predicate<? super UserInfo> filter) {
        Stream<UserInfo> filteredListStream = mUserManager.getAliveUsers().stream();

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

    /**
     * Maximum number of users allowed on the device. This includes real users, managed profiles
     * and restricted users, but excludes guests.
     *
     * <p> It excludes system user in headless system user model.
     *
     * @return Maximum number of users that can be present on the device.
     */
    private int getMaxSupportedUsers() {
        int maxSupportedUsers = UserManager.getMaxSupportedUsers();
        if (UserManager.isHeadlessSystemUserMode()) {
            maxSupportedUsers -= 1;
        }
        return maxSupportedUsers;
    }

    private int getManagedProfilesCount() {
        List<UserInfo> users = getAllUsers();

        // Count all users that are managed profiles of another user.
        int managedProfilesCount = 0;
        for (UserInfo user : users) {
            if (user.isManagedProfile()) {
                managedProfilesCount++;
            }
        }
        return managedProfilesCount;
    }

    /**
     * Get the maximum number of real (non-guest, non-managed profile) users that can be created on
     * the device. This is a dynamic value and it decreases with the increase of the number of
     * managed profiles on the device.
     *
     * <p> It excludes system user in headless system user model.
     *
     * @return Maximum number of real users that can be created.
     */
    public int getMaxSupportedRealUsers() {
        return getMaxSupportedUsers() - getManagedProfilesCount();
    }
}
