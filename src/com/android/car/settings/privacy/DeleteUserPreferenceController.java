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

package com.android.car.settings.privacy;

import android.app.ActivityManager;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;
import com.android.car.settings.users.RemoveUserHandler;
import com.android.car.settings.users.UserHelper;
import com.android.car.settings.users.UserUtils;
import com.android.internal.annotations.VisibleForTesting;

/** Business logic for preference that deletes the current user profile. */
public class DeleteUserPreferenceController extends PreferenceController<Preference> {

    private final UserInfo mUserInfo;
    private final RemoveUserHandler mRemoveUserHandler;

    public DeleteUserPreferenceController(Context context,
            String preferenceKey, FragmentController fragmentController,
            CarUxRestrictions uxRestrictions) {
        this(context, preferenceKey, fragmentController, uxRestrictions,
                new RemoveUserHandler(context, UserHelper.getInstance(context),
                        UserManager.get(context), fragmentController));
    }

    @VisibleForTesting
    DeleteUserPreferenceController(Context context,
            String preferenceKey, FragmentController fragmentController,
            CarUxRestrictions uxRestrictions, RemoveUserHandler removeUserHandler) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mRemoveUserHandler = removeUserHandler;
        mUserInfo = UserUtils.getUserInfo(getContext(), ActivityManager.getCurrentUser());
        mRemoveUserHandler.setUserInfo(mUserInfo);
    }

    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();
        mRemoveUserHandler.resetListeners();
    }

    @Override
    protected Class<Preference> getPreferenceType() {
        return Preference.class;
    }

    @Override
    protected boolean handlePreferenceClicked(Preference preference) {
        mRemoveUserHandler.showConfirmRemoveUserDialog();
        return true;
    }

    @AvailabilityStatus
    protected int getAvailabilityStatus() {
        return mRemoveUserHandler.canRemoveUser(mUserInfo) ? AVAILABLE : AVAILABLE_FOR_VIEWING;
    }
}
