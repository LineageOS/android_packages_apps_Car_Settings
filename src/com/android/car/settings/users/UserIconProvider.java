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

import android.content.Context;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.android.car.settings.R;
import com.android.settingslib.users.UserManagerHelper;

/**
 * Simple class for providing icons for users in Settings.
 */
public final class UserIconProvider {

    private UserIconProvider() {}

    /**
     * Gets the icon for the given user to use in settings.
     * If icon has not been assigned to this user, it defaults to a generic user icon.
     *
     * @param userInfo User for which the icon is requested.
     * @param userManagerHelper Helper wrapper class for user management.
     *
     * @return Drawable representing the icon for the user.
     */
    public static Drawable getUserIcon(UserInfo userInfo,
            UserManagerHelper userManagerHelper, Context context) {
        Bitmap icon = userManagerHelper.getUserIcon(userInfo);
        if (icon == null) {
            // Return default user icon.
            return context.getDrawable(R.drawable.ic_user);
        }
        return userManagerHelper.scaleUserIcon(icon, context.getResources()
                .getDimensionPixelSize(R.dimen.car_primary_icon_size));
    }
}
