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

import androidx.car.widget.TextListItem;

import com.android.car.settings.R;

/**
 * Class that represents a {@link TextListItem} for a user.
 */
class UserListItem extends TextListItem {
    private final Context mContext;
    private final UserInfo mUserInfo;
    private final boolean mIsCurrentUser;

    UserListItem(UserInfo userInfo, Context context,
            CarUserManagerHelper userManagerHelper) {
        super(context);
        mContext = context;
        mUserInfo = userInfo;
        mIsCurrentUser = userManagerHelper.isCurrentProcessUser(mUserInfo);

        Drawable icon = new UserIconProvider(userManagerHelper).getUserIcon(mUserInfo, mContext);
        setPrimaryActionIcon(icon, /* useLargeIcon= */ false);
        setTitle(getUserItemTitle(mUserInfo, mIsCurrentUser, mContext));
        setSummary();
    }

    public static String getUserItemTitle(UserInfo userInfo, boolean isCurrentUser,
            Context context) {
        return isCurrentUser ? context.getString(R.string.current_user_name, userInfo.name)
                : userInfo.name;
    }

    private void setSummary() {
        if (!mUserInfo.isInitialized()) {
            setBody(mContext.getString(R.string.user_summary_not_set_up));
        }
        if (mUserInfo.isAdmin()) {
            setBody(mIsCurrentUser ? mContext.getString(R.string.signed_in_admin_user)
                    : mContext.getString(R.string.user_admin));
        }
    }
}
