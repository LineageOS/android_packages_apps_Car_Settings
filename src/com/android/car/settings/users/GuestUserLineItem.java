/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.annotation.NonNull;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.RemoteException;
import android.os.UserManager;
import android.util.Log;
import android.widget.ImageView;

import com.android.car.settings.R;
import com.android.car.settings.common.IconTextLineItem;

/**
 * Represents a guest user in settings page.
 */
public class GuestUserLineItem extends IconTextLineItem {
    private static final String TAG = "GuestUserLineItem";
    private final Context mContext;
    private final UserManager mUserManager;

    public GuestUserLineItem(@NonNull Context context, UserManager userManager) {
        super(context.getString(R.string.user_guest));
        mContext = context;
        mUserManager = userManager;
    }

    @Override
    public void onClick() {
        UserInfo guest = mUserManager.createGuest(
                mContext, mContext.getString(R.string.user_guest));
        if (guest == null) {
            // Couldn't create user, most likely because there are too many, but we haven't
            // been able to reload the list yet.
            Log.w(TAG, "can't create user.");
            return;
        }
        try {
            ActivityManager.getService().switchUser(guest.id);
        } catch (RemoteException e) {
            Log.e(TAG, "Couldn't switch user.", e);
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isExpandable() {
        return true;
    }

    @Override
    public CharSequence getDesc() {
        return null;
    }

    @Override
    public void setIcon(ImageView iconView) {
        iconView.setImageDrawable(mContext.getDrawable(R.drawable.ic_guest_user));
    }
}
