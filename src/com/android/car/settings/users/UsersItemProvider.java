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
 * limitations under the License
 */

package com.android.car.settings.users;

import android.content.Context;
import android.content.pm.UserInfo;

import com.android.car.settings.R;

import java.util.ArrayList;
import java.util.List;

import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.TextListItem;

/**
 * Implementation of {@link ListItemProvider} for {@link UsersListFragment}.
 * Creates items that represent users on the system.
 */
class UsersItemProvider extends ListItemProvider {
    private final List<ListItem> mItems = new ArrayList<>();
    private final Context mContext;
    private final UserClickListener mUserClickListener;
    private final UserManagerHelper mUserManagerHelper;

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

    UsersItemProvider(Context context, UserClickListener userClickListener,
            UserManagerHelper userManagerHelper) {
        mContext = context;
        mUserClickListener = userClickListener;
        mUserManagerHelper = userManagerHelper;
        refreshItems();
    }

    @Override
    public ListItem get(int position) {
        return mItems.get(position);
    }

    @Override
    public int size() {
        return mItems.size();
    }

    /**
     * Clears and recreates the list of items.
     */
    public void refreshItems() {
        mItems.clear();

        UserInfo currUserInfo = mUserManagerHelper.getCurrentUserInfo();

        // Show current user
        mItems.add(createUserItem(currUserInfo,
                mContext.getString(R.string.current_user_name, currUserInfo.name)));

        // Display other users on the system
        List<UserInfo> infos = mUserManagerHelper.getOtherUsers();
        for (UserInfo userInfo : infos) {
            mItems.add(createUserItem(userInfo, userInfo.name));
        }
    }

    // Creates a line for a user, clicking on it leads to the user details page
    private ListItem createUserItem(UserInfo userInfo, String title) {
        TextListItem item = new TextListItem(mContext);
        item.setPrimaryActionIcon(mUserManagerHelper.getUserIcon(userInfo),
                false /* useLargeIcon */);
        item.setTitle(title);

        if (!mUserManagerHelper.isInitialized(userInfo)) {
            // Indicate that the user has not been initialized yet.
            item.setBody(mContext.getString(R.string.user_summary_not_set_up));
        }

        item.setOnClickListener(view -> mUserClickListener.onUserClicked(userInfo));
        item.setSupplementalIcon(R.drawable.ic_chevron_right, false);
        return item;
    }
}