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
import android.content.Context;
import android.content.pm.UserInfo;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.NoSetupPreferenceController;

/** Common setup of all preference controllers related to user details. */
public class BaseUserDetailsPreferenceController extends NoSetupPreferenceController implements
        LifecycleObserver {

    private final CarUserManagerHelper mCarUserManagerHelper;
    private UserInfo mUserInfo;

    public BaseUserDetailsPreferenceController(Context context,
            String preferenceKey,
            FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
        mCarUserManagerHelper = new CarUserManagerHelper(mContext);
    }

    /** Checks that user info has been set. */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {
        if (mUserInfo == null) {
            throw new IllegalStateException("UserInfo should be non-null by this point");
        }
    }

    /** Sets the user info for which this preference controller operates. */
    public void setUserInfo(UserInfo userInfo) {
        mUserInfo = userInfo;
    }

    /** Gets the current user info. */
    protected UserInfo getUserInfo() {
        return mUserInfo;
    }

    /** Refreshes the user info, since it might have changed. */
    protected void refreshUserInfo() {
        mUserInfo = UserUtils.getUserInfo(mContext, mUserInfo.id);
    }

    /** Gets the car user manager helper. */
    protected CarUserManagerHelper getCarUserManagerHelper() {
        return mCarUserManagerHelper;
    }
}
