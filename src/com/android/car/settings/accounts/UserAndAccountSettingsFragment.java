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
package com.android.car.settings.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.TextView;

import com.android.car.list.SubtitleTextLineItem;
import com.android.car.list.TypedPagedListAdapter;
import com.android.car.settings.R;
import com.android.car.settings.common.ListSettingsFragment;
import com.android.car.settings.users.UserLineItem;

import com.android.settingslib.accounts.AuthenticatorHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists all Users available on this device.
 */
public class UserAndAccountSettingsFragment extends ListSettingsFragment
        implements AuthenticatorHelper.OnAccountsUpdateListener {
    private static final String TAG = "UserAndAccountSettings";
    private Context mContext;
    private UserManager mUserManager;
    private AuthenticatorHelper mAuthenticatorHelper;

    public static UserAndAccountSettingsFragment newInstance() {
        UserAndAccountSettingsFragment
                userAndAccountSettingsFragment = new UserAndAccountSettingsFragment();
        Bundle bundle = ListSettingsFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.user_and_account_settings_title);
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_with_button);
        userAndAccountSettingsFragment.setArguments(bundle);
        return userAndAccountSettingsFragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mContext = getContext();
        mUserManager =
                (UserManager) mContext.getSystemService(Context.USER_SERVICE);

        // Super class's onActivityCreated need to be called after mContext is initialized.
        // Because getLineItems is called in there.
        super.onActivityCreated(savedInstanceState);

        TextView addUserBtn = (TextView) getActivity().findViewById(R.id.action_button1);
        addUserBtn.setText(R.string.user_add_user_menu);
        addUserBtn.setOnClickListener(v -> {
            UserInfo user = mUserManager.createUser(
                    mContext.getString(R.string.user_new_user_name), 0 /* flags */);
            if (user == null) {
                // Couldn't create user, most likely because there are too many, but we haven't
                // been able to reload the list yet.
                Log.w(TAG, "can't create user.");
                return;
            }
            try {
                ActivityManager.getService().switchUser(user.id);
            } catch (RemoteException e) {
                Log.e(TAG, "Couldn't switch user.", e);
            }
        });
    }

    @Override
    public ArrayList<TypedPagedListAdapter.LineItem> getLineItems() {
        ArrayList<TypedPagedListAdapter.LineItem> items = new ArrayList<>();

        UserInfo currUserInfo = mUserManager.getUserInfo(ActivityManager.getCurrentUser());

        // Show current user and list of accounts owned by current user.
        items.add(new UserLineItem(
                mContext,
                currUserInfo,
                mUserManager,
                mFragmentController));

        // Add "Account for $User" title for a list of accounts.
        items.add(new SubtitleTextLineItem(
                getString(R.string.account_list_title, currUserInfo.name)));

        mAuthenticatorHelper =
                new AuthenticatorHelper(mContext, currUserInfo.getUserHandle(), this);
        mAuthenticatorHelper.listenToAccountUpdates();

        String[] accountTypes = mAuthenticatorHelper.getEnabledAccountTypes();
        for (int i = 0; i < accountTypes.length; i++) {
            String accountType = accountTypes[i];
            Account[] accounts = AccountManager.get(mContext)
                    .getAccountsByTypeAsUser(accountType, currUserInfo.getUserHandle());
            for (Account account : accounts) {
                items.add(new AccountLineItem(
                        mContext,
                        currUserInfo,
                        account,
                        mFragmentController));
            }
        }
        items.add(new AddAccountLineItem(
                getString(R.string.add_account_title),
                R.drawable.ic_add,
                mContext,
                mFragmentController));

        items.add(new SubtitleTextLineItem(getString(R.string.other_users_title)));

        List<UserInfo> infos = mUserManager.getUsers(true);
        for (UserInfo userInfo : infos) {
            if (userInfo.id != currUserInfo.id) {
                items.add(new UserLineItem(
                        mContext,
                        userInfo,
                        mUserManager,
                        mFragmentController));
            }
        }
        return items;
    }

    @Override
    public void onAccountsUpdate(UserHandle userHandle) {
        Fragment fragment = getFragmentManager().findFragmentByTag(TAG);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.detach(fragment);
        transaction.attach(fragment);
        transaction.commit();
    }
}
