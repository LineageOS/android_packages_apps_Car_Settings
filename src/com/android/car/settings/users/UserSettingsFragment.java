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

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;

import com.android.car.settings.R;
import com.android.car.settings.common.ListSettingsFragment;
import com.android.car.settings.common.TypedPagedListAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists all Users available on this device.
 */
public class UserSettingsFragment extends ListSettingsFragment {
    public static UserSettingsFragment getInstance() {
        UserSettingsFragment
                userSettingsFragment = new UserSettingsFragment();
        Bundle bundle = ListSettingsFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.user_settings_title);
        userSettingsFragment.setArguments(bundle);
        return userSettingsFragment;
    }

    @Override
    public ArrayList<TypedPagedListAdapter.LineItem> getLineItems() {
        Context context = getContext();
        UserManager mUserManager =
                (UserManager) context.getSystemService(Context.USER_SERVICE);
        List<UserInfo> infos = mUserManager.getUsers(true);
        ArrayList<TypedPagedListAdapter.LineItem> items = new ArrayList<>();
        for (UserInfo userInfo : infos) {
            items.add(new UserLineItem(context, userInfo, mUserManager, mFragmentController));
        }
        items.add(new AddUserLineItem(context, mUserManager));
        items.add(new GuestUserLineItem(context, mUserManager));
        return items;
    }
}
