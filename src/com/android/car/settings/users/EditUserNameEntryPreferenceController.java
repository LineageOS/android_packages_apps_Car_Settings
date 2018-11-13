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
import android.graphics.drawable.Drawable;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.R;
import com.android.car.settings.common.ButtonPreference;
import com.android.car.settings.common.FragmentController;

/** Business logic for the preference which opens the EditUserNameFragment. */
public class EditUserNameEntryPreferenceController extends
        BaseUserDetailsPreferenceController implements LifecycleObserver {

    private PreferenceScreen mPreferenceScreen;

    public EditUserNameEntryPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
    }

    private final CarUserManagerHelper.OnUsersUpdateListener mOnUsersUpdateListener = () -> {
        refreshUserInfo();
        displayPreference(mPreferenceScreen);
    };

    /** Registers a listener which updates the displayed user name when a user is modified. */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {
        getCarUserManagerHelper().registerOnUsersUpdateListener(mOnUsersUpdateListener);
    }

    /** Unregisters a listener which updates the displayed user name when a user is modified. */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        getCarUserManagerHelper().unregisterOnUsersUpdateListener(mOnUsersUpdateListener);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceScreen = screen;

        ButtonPreference preference = (ButtonPreference) screen.findPreference(getPreferenceKey());
        preference.setOnButtonClickListener(pref -> {
            getFragmentController().launchFragment(EditUsernameFragment.newInstance(getUserInfo()));
        });

        Drawable icon = new UserIconProvider(getCarUserManagerHelper()).getUserIcon(getUserInfo(),
                mContext);
        preference.setIcon(icon);
        preference.setTitle(
                UserUtils.getUserDisplayName(mContext, getCarUserManagerHelper(), getUserInfo()));

        if (!getCarUserManagerHelper().isCurrentProcessUser(getUserInfo())) {
            preference.showButton(false);
        }
    }

    @Override
    public CharSequence getSummary() {
        if (!getUserInfo().isInitialized()) {
            return mContext.getString(R.string.user_summary_not_set_up);
        }
        if (getUserInfo().isAdmin()) {
            return getCarUserManagerHelper().isCurrentProcessUser(getUserInfo())
                    ? mContext.getString(R.string.signed_in_admin_user)
                    : mContext.getString(R.string.user_admin);
        }
        return super.getSummary();
    }
}
