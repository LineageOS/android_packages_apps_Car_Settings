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

package com.android.car.settings.users;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;

/**
 * Controller that displays the preference for letting the user delete the current profile
 */
public class ProfileDetailsDeletePreferenceController
        extends UserDetailsBasePreferenceController<Preference> {

    private static final Logger LOG = new Logger(ProfileDetailsDeletePreferenceController.class);

    private UserHelper mProfileHelper;

    private final RemoveUserHandler mRemoveUserHandler;

    public ProfileDetailsDeletePreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        this(context, preferenceKey, fragmentController, uxRestrictions,
                UserHelper.getInstance(context),
                new RemoveUserHandler(context, UserHelper.getInstance(context),
                        UserManager.get(context), fragmentController));
    }

    @VisibleForTesting
    ProfileDetailsDeletePreferenceController(Context context,
            String preferenceKey, FragmentController fragmentController,
            CarUxRestrictions uxRestrictions, UserHelper userHelper,
            RemoveUserHandler removeUserHandler) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mProfileHelper = userHelper;
        mRemoveUserHandler = removeUserHandler;
    }

    @Override
    protected Class<Preference> getPreferenceType() {
        return Preference.class;
    }

    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();
        mRemoveUserHandler.resetListeners();
    }

    @Override
    public void setUserInfo(UserInfo userInfo) {
        super.setUserInfo(userInfo);
        mRemoveUserHandler.setUserInfo(userInfo);
    }

    @Override
    protected void updateState(Preference preference) {
        preference.setVisible(mRemoveUserHandler.canRemoveUser(getUserInfo())
                && mProfileHelper.isCurrentProcessUser(getUserInfo()));
    }

    @Override
    public boolean handlePreferenceClicked(Preference preference) {
        mRemoveUserHandler.showConfirmRemoveUserDialog();
        return true;
    }
}
