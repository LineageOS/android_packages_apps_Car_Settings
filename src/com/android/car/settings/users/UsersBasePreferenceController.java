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
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.NoSetupPreferenceController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Shared business logic between {@link UsersListFragment} and {@link ChooseNewAdminFragment}. */
public abstract class UsersBasePreferenceController extends NoSetupPreferenceController implements
        LifecycleObserver {

    /** Update screen when users list is updated. */
    private final CarUserManagerHelper.OnUsersUpdateListener mOnUsersUpdateListener =
            () -> refreshUsers();

    /** Click listener to open the User Detail Fragment for given preference. */
    private final UsersPreferenceProvider.UserClickListener mUserClickListener =
            userInfo -> userClicked(userInfo);

    private UsersPreferenceProvider mPreferenceProvider;
    private CarUserManagerHelper mCarUserManagerHelper;
    private List<Preference> mUsersToDisplay;
    private PreferenceScreen mPreferenceScreen;
    private boolean mShouldRefreshUsers = false;

    public UsersBasePreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
        mUsersToDisplay = new ArrayList<>();
        mCarUserManagerHelper = new CarUserManagerHelper(mContext);
        mPreferenceProvider = new UsersPreferenceProvider(mContext, mCarUserManagerHelper,
                mUserClickListener);
    }

    /**
     * Ensure that helper is set by the time onCreate is called. Register a listener to refresh
     * screen on updates.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {
        mCarUserManagerHelper.registerOnUsersUpdateListener(mOnUsersUpdateListener);
    }

    /** Unregister listener to refresh screen on updates. */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        mCarUserManagerHelper.unregisterOnUsersUpdateListener(mOnUsersUpdateListener);
    }

    /**
     * Refresh the screen on start (updateState is called onStart()). This is required since
     * granting admin permissions does not trigger an update to the car user manager helper
     * listener.
     */
    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        refreshUsers();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceScreen = screen;

        // Having a bit that checks for whether the users is invalidated is helpful to prevent us
        // from redrawing the screen more than necessary.
        if (mUsersToDisplay.isEmpty() || mShouldRefreshUsers) {
            List<Preference> newUsers = mPreferenceProvider.createUserList();

            // Even after invalidation, only refresh if the set of preferences is different.
            if (userListsAreDifferent(mUsersToDisplay, newUsers)) {
                mUsersToDisplay = newUsers;
                mPreferenceScreen.removeAll();

                for (Preference preference : mUsersToDisplay) {
                    mPreferenceScreen.addPreference(preference);
                }
            }

            mShouldRefreshUsers = false;
        }
    }

    /** Gets the car user manager helper. */
    protected CarUserManagerHelper getCarUserManagerHelper() {
        return mCarUserManagerHelper;
    }

    /** Handles the user click on a preference for a certain user. */
    protected abstract void userClicked(UserInfo userInfo);


    /** Gets the preference provider to set additional arguments if necessary. */
    protected UsersPreferenceProvider getPreferenceProvider() {
        return mPreferenceProvider;
    }

    private void refreshUsers() {
        mShouldRefreshUsers = true;
        displayPreference(mPreferenceScreen);
    }

    private boolean userListsAreDifferent(List<Preference> currentList,
            List<Preference> newList) {
        if (currentList.size() != newList.size()) {
            return true;
        }

        for (int i = 0; i < currentList.size(); i++) {
            // Cannot use "compareTo" on preference, since it uses the order attribute to compare.
            if (preferencesAreDifferent(currentList.get(i), newList.get(i))) {
                return true;
            }
        }

        return false;
    }

    private boolean preferencesAreDifferent(Preference lhs, Preference rhs) {
        return !Objects.equals(lhs.getTitle(), rhs.getTitle())
                || !Objects.equals(lhs.getSummary(), rhs.getSummary());
    }
}
