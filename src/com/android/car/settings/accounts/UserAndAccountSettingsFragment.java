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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.TextView;

import com.android.car.settings.R;
import com.android.car.settings.common.ListItemSettingsFragment;
import com.android.car.settings.users.UserDetailsSettingsFragment;

import com.android.settingslib.accounts.AuthenticatorHelper;

import java.util.ArrayList;
import java.util.List;

import androidx.car.widget.ListItem;

/**
 * Lists all Users available on this device.
 */
public class UserAndAccountSettingsFragment extends ListItemSettingsFragment
        implements AuthenticatorHelper.OnAccountsUpdateListener {
    private static final String TAG = "UserAndAccountSettings";
    private Context mContext;
    private UserManager mUserManager;
    private AuthenticatorHelper mAuthenticatorHelper;

    public static UserAndAccountSettingsFragment newInstance() {
        UserAndAccountSettingsFragment
                userAndAccountSettingsFragment = new UserAndAccountSettingsFragment();
        Bundle bundle = ListItemSettingsFragment.getBundle();
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
    public List<ListItem> getListItems() {
        ArrayList<ListItem> items = new ArrayList<>();

        UserInfo currUserInfo = mUserManager.getUserInfo(ActivityManager.getCurrentUser());

        // Show current user
        items.add(createUserItem(currUserInfo,
                getString(R.string.current_user_name, currUserInfo.name),
                false /* withDividerHidden */));

        // Add "Account for $User" title for a list of accounts.
        items.add(createSubtitleItem(getString(R.string.account_list_title, currUserInfo.name)));

        // Add an item for each account owned by the current user (1st and 3rd party accounts)
        mAuthenticatorHelper = new AuthenticatorHelper(mContext, currUserInfo.getUserHandle(),
                this);
        mAuthenticatorHelper.listenToAccountUpdates();
        String[] accountTypes = mAuthenticatorHelper.getEnabledAccountTypes();
        AccountHelper accountHelper = new AccountHelper(mContext, currUserInfo.getUserHandle());
        for (int i = 0; i < accountTypes.length; i++) {
            String accountType = accountTypes[i];
            Account[] accounts = AccountManager.get(mContext)
                    .getAccountsByTypeAsUser(accountType, currUserInfo.getUserHandle());
            for (Account account : accounts) {
                items.add(createAccountItem(accountHelper, account, accountType, currUserInfo));
            }
        }

        // Add "+ Add account" option
        items.add(createAddAccountItem());

        // Add "Other users" title item
        items.add(createSubtitleItem(getString(R.string.other_users_title)));

        // Display other users on the system
        List<UserInfo> infos = mUserManager.getUsers(true);
        for (UserInfo userInfo : infos) {
            if (userInfo.id != currUserInfo.id) {
                items.add(createUserItem(userInfo, userInfo.name, true /* withDividerHidden*/));
            }
        }
        return items;
    }

    // Creates a line for a user, clicking on it leads to the user details page
    private ListItem createUserItem(UserInfo userInfo, String title, boolean withDividerHidden) {
        ListItem.Builder listItem =  new ListItem.Builder(mContext)
                .withPrimaryActionIcon(getUserIcon(userInfo), false /* useLargeIcon */)
                .withTitle(title)
                .withOnClickListener(view -> mFragmentController.launchFragment(
                        UserDetailsSettingsFragment.getInstance(userInfo)));

        if (withDividerHidden) {
            // Hiding the divider to group the items together visually. All of those without a
            // divider between them will be part of the same "group".
            listItem.withDividerHidden();
        }
        return listItem.build();
    }

    // Creates a subtitle line for visual separation in the list
    private ListItem createSubtitleItem(String title) {
        return new ListItem.Builder(mContext)
                .withPrimaryActionEmptyIcon()
                .withTitle(title)
                .withViewBinder(viewHolder ->
                        viewHolder.getTitle().setTextAppearance(R.style.SettingsListHeader))
                .withDividerHidden() // Hiding the divider after subtitle, since subtitle is a
                // header for a group of items.
                .build();
    }

    // Creates a line for an account that belongs to a given user
    private ListItem createAccountItem(AccountHelper accountHelper, Account account, String
            accountType, UserInfo userInfo) {
        return new ListItem.Builder(mContext)
                .withPrimaryActionIcon(accountHelper.getDrawableForType(mContext, accountType),
                        false /* useLargeIcon */)
                .withTitle(account.name)
                .withOnClickListener(view -> mFragmentController.launchFragment(
                        AccountDetailsFragment.newInstance(account, userInfo)))
                .withDividerHidden()
                .build();
    }

    // Creates a clickable "+ Add Account" line. Clicking on it leads to the Add an Account page.
    private ListItem createAddAccountItem() {
        return new ListItem.Builder(mContext)
                .withPrimaryActionIcon(R.drawable.ic_add, false /* useLargeIcon */)
                .withTitle(getString(R.string.add_account_title))
                .withOnClickListener(view -> mFragmentController.launchFragment(
                        ChooseAccountFragment.newInstance()))
                .build();
    }

    private Drawable getUserIcon(UserInfo userInfo) {
        Bitmap picture = mUserManager.getUserIcon(userInfo.id);

        if (picture != null) {
            int avatarSize = mContext.getResources()
                    .getDimensionPixelSize(R.dimen.car_primary_icon_size);
            picture = Bitmap.createScaledBitmap(
                    picture, avatarSize, avatarSize, true /* filter */);
            return new BitmapDrawable(mContext.getResources(), picture);
        }
        return mContext.getDrawable(R.drawable.ic_user);
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
