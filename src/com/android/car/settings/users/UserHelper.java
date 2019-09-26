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

import android.content.Context;
import android.os.UserManager;

import com.android.internal.annotations.VisibleForTesting;

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
}
