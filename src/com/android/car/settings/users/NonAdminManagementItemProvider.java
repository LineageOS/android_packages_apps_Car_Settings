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

import androidx.car.widget.ActionListItem;

import com.android.car.settings.R;

/**
 * Provides list items for screen that manages non-admin privileges.
 */
public class NonAdminManagementItemProvider  extends AbstractRefreshableListItemProvider {
    private final AssignAdminListener mAssignAdminListener;
    private final UserIconProvider mUserIconProvider;
    private final int mUserId;

    NonAdminManagementItemProvider(int userId, Context context,
            AssignAdminListener assignAdminListener, CarUserManagerHelper userManagerHelper) {
        super(context);
        mUserIconProvider = new UserIconProvider(userManagerHelper);
        mAssignAdminListener = assignAdminListener;
        mUserId = userId;
        populateItems();
    }

    /**
     * Creates an item for assigning admin privileges to a non-admin user and adds it to the list.
     */
    @Override
    protected void populateItems() {
        UserInfo userInfo = UserUtils.getUserInfo(mContext, mUserId);

        ActionListItem item = new ActionListItem(mContext);
        item.setPrimaryActionIcon(mUserIconProvider.getUserIcon(userInfo, mContext),
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item.setTitle(mContext.getString(R.string.grant_admin_privileges));
        item.setAction(mContext.getString(R.string.assign_admin_privileges),
                /* showDivider= */ false,
                v -> mAssignAdminListener.onAssignAdminClicked());

        mItems.add(item);
    }

    /**
     * Interface for registering clicks on assigning admin privileges button.
     */
    interface AssignAdminListener {
        /**
         * Invoked when edit button is clicked.
         */
        void onAssignAdminClicked();
    }
}

