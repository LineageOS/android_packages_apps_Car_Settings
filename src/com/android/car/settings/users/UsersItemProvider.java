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

import android.car.user.CarUserManagerHelper;
import android.content.Context;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;

import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.TextListItem;

import com.android.car.settings.R;

import java.util.List;

/**
 * Implementation of {@link ListItemProvider} for {@link UsersListFragment}.
 * Creates items that represent users on the system.
 */
class UsersItemProvider extends AbstractRefreshableListItemProvider  {
    private final UserClickListener mUserClickListener;
    private final CarUserManagerHelper mCarUserManagerHelper;
    private final UserIconProvider mUserIconProvider;

    UsersItemProvider(Context context, UserClickListener userClickListener,
            CarUserManagerHelper userManagerHelper) {
        super(context);
        mCarUserManagerHelper = userManagerHelper;
        mUserIconProvider = new UserIconProvider(mCarUserManagerHelper);
        mUserClickListener = userClickListener;
        populateItems();
    }

    /**
     * Clears and recreates the list of items.
     */
    @Override
    public void populateItems() {
        UserInfo currUserInfo = mCarUserManagerHelper.getCurrentProcessUserInfo();

        // Show current user
        mItems.add(createUserItem(currUserInfo));

        // Display other users on the system
        List<UserInfo> infos = mCarUserManagerHelper.getAllSwitchableUsers();
        for (UserInfo userInfo : infos) {
            if (!userInfo.isGuest()) { // Do not show guest users.
                mItems.add(createUserItem(userInfo));
            }
        }

        // Display guest session option.
        mItems.add(createGuestItem());
    }

    // Creates a line for a user, clicking on it leads to the user details page.
    private ListItem createUserItem(UserInfo userInfo) {
        UserListItem item = new UserListItem(userInfo, mContext, mCarUserManagerHelper);

        item.setOnClickListener(view -> mUserClickListener.onUserClicked(userInfo));
        item.setSupplementalIcon(R.drawable.ic_chevron_right, false);
        return item;
    }

    // Creates a line for a guest session.
    private ListItem createGuestItem() {
        Drawable icon = mUserIconProvider.getDefaultGuestIcon(mContext);

        TextListItem item = new TextListItem(mContext);
        item.setPrimaryActionIcon(icon, /* useLargeIcon= */ false);
        item.setTitle(mContext.getString(R.string.user_guest));

        item.setOnClickListener(view -> mUserClickListener.onGuestClicked());
        item.setSupplementalIcon(R.drawable.ic_chevron_right, false);
        return item;
    }

    /**
     * Interface for registering clicks on users.
     */
    interface UserClickListener {
        /**
         * Invoked when user is clicked.
         *
         * @param userInfo User for which the click is registered.
         */
        void onUserClicked(UserInfo userInfo);

        /**
         * Invoked when guest is clicked.
         */
        void onGuestClicked();
    }
}
