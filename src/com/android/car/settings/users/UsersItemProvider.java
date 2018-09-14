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
    private final boolean mIncludeCurrentUser;
    private final boolean mIncludeGuest;
    private final boolean mIncludeSuplementalIcon;

    private UsersItemProvider(Context context, CarUserManagerHelper userManagerHelper,
            Builder builder) {
        super(context);
        mCarUserManagerHelper = userManagerHelper;
        mUserIconProvider = new UserIconProvider(mCarUserManagerHelper);

        mUserClickListener = builder.mUserClickListener;
        mIncludeCurrentUser = builder.mIncludeCurrentUser;
        mIncludeGuest = builder.mIncludeGuest;
        mIncludeSuplementalIcon = builder.mIncludeSuplementalIcon;

        populateItems();
    }

    /**
     * Recreates the list of items.
     */
    @Override
    public void populateItems() {
        UserInfo currUserInfo = mCarUserManagerHelper.getCurrentProcessUserInfo();

        // Show current user
        if (mIncludeCurrentUser) {
            mItems.add(createUserItem(currUserInfo));
        }

        // Display other users on the system
        List<UserInfo> infos = mCarUserManagerHelper.getAllSwitchableUsers();
        for (UserInfo userInfo : infos) {
            if (!userInfo.isGuest()) { // Do not show guest users.
                mItems.add(createUserItem(userInfo));
            }
        }

        // Display guest session option.
        if (mIncludeGuest) {
            mItems.add(createGuestItem());
        }
    }

    // Creates a list item for a user, clicking on it leads to the user details page.
    private ListItem createUserItem(UserInfo userInfo) {
        UserListItem item = new UserListItem(userInfo, mContext, mCarUserManagerHelper);

        if (mUserClickListener != null) {
            item.setOnClickListener(view -> mUserClickListener.onUserClicked(userInfo));
        }

        if (mIncludeSuplementalIcon) {
            item.setSupplementalIcon(R.drawable.ic_chevron_right, false);
        }
        return item;
    }

    // Creates a list item for a guest session.
    private ListItem createGuestItem() {
        Drawable icon = mUserIconProvider.getDefaultGuestIcon(mContext);

        TextListItem item = new TextListItem(mContext);
        item.setPrimaryActionIcon(icon, TextListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item.setTitle(mContext.getString(R.string.user_guest));
        return item;
    }

    public static final class Builder {
        private final Context mContext;
        private final CarUserManagerHelper mUserManagerHelper;

        private UserClickListener mUserClickListener;
        private boolean mIncludeCurrentUser = true;
        private boolean mIncludeGuest = true;
        private boolean mIncludeSuplementalIcon;

        /**
         * Builder for constructing an instance of {@link UsersItemProvider}
         */
        Builder(Context context, CarUserManagerHelper userManagerHelper) {
            mContext = context;
            mUserManagerHelper = userManagerHelper;
        }

        /**
         * Setter for {@link UserClickListener} to be invoked when any item returned by the provider
         * is clicked.
         */
        public Builder setOnUserClickListener(UserClickListener listener) {
            mUserClickListener = listener;
            return this;
        }

        /**
         * If set to {@code true}, current user will be in the list of users returned by the
         * provider, otherwise it will not. Default is {@code true}.
         */
        public Builder setIncludeCurrentUser(boolean include) {
            mIncludeCurrentUser = include;
            return this;
        }

        /**
         * If set to {@code true}, guest user will be in the list of users returned by the
         * provider, otherwise it will not. Default is {@code true}.
         */
        public Builder setIncludeGuest(boolean include) {
            mIncludeGuest = include;
            return this;
        }

        /**
         * If set to {@code true}, supplemental icon will be set on all items returned by the
         * provider, otherwise it will not. Default is {@code false}.
         */
        public Builder setIncludeSupplementalIcon(boolean include) {
            mIncludeSuplementalIcon = include;
            return this;
        }

        /**
         * Returns an instance of {@link UsersItemProvider} constructed from the {@link Builder}
         * parameters.
         */
        public UsersItemProvider create() {
            return new UsersItemProvider(mContext, mUserManagerHelper, /* builder= */ this);
        }
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
    }
}
