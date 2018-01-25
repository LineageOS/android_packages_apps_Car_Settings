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
import android.util.Log;
import android.widget.Button;

import com.android.car.settings.R;
import com.android.car.settings.common.ListItemSettingsFragment;
import com.android.car.settings.users.UserDetailsSettingsFragment;
import com.android.settingslib.accounts.AuthenticatorHelper;

import java.util.ArrayList;
import java.util.List;

import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.TextListItem;

/**
 * Lists all Users available on this device.
 */
public class UserAndAccountSettingsFragment extends ListItemSettingsFragment
        implements AuthenticatorHelper.OnAccountsUpdateListener {
    private static final String TAG = "UserAndAccountSettings";
    private Context mContext;
    private UserManager mUserManager;
    private UserAndAccountItemProvider mItemProvider;

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
        mItemProvider = new UserAndAccountItemProvider(mContext, this, mUserManager);

        // Super class's onActivityCreated need to be called after mContext is initialized.
        // Because getLineItems is called in there.
        super.onActivityCreated(savedInstanceState);

        Button addUserBtn = (Button) getActivity().findViewById(R.id.action_button1);
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
    public void onAccountsUpdate(UserHandle userHandle) {
        mItemProvider.refreshItems();
        refreshList();
    }

    private void launchUserDetails(UserInfo userInfo) {
        mFragmentController.launchFragment(UserDetailsSettingsFragment.getInstance(userInfo));
    }

    private void launchAccountDetails(Account account, UserInfo userInfo) {
        mFragmentController.launchFragment(AccountDetailsFragment.newInstance(account, userInfo));
    }

    private void launchAccountChooser() {
        mFragmentController.launchFragment(ChooseAccountFragment.newInstance());
    }

    @Override
    public ListItemProvider getItemProvider() {
        return mItemProvider;
    }

    private static class UserAndAccountItemProvider extends ListItemProvider {
        private final List<ListItem> mItems = new ArrayList<>();
        private final Context mContext;
        private final UserAndAccountSettingsFragment mFragment;
        private final UserManager mUserManager;
        private AuthenticatorHelper mAuthenticatorHelper;

        UserAndAccountItemProvider(Context context, UserAndAccountSettingsFragment fragment,
                UserManager userManager) {
            mContext = context;
            mFragment = fragment;
            mUserManager = userManager;
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

        public void refreshItems() {
            mItems.clear();

            UserInfo currUserInfo = mUserManager.getUserInfo(ActivityManager.getCurrentUser());

            // Show current user
            mItems.add(createUserItem(currUserInfo,
                    mContext.getString(R.string.current_user_name, currUserInfo.name),
                    false /* withDividerHidden */));

            // Add "Account for $User" title for a list of accounts.
            mItems.add(createSubtitleItem(
                    mContext.getString(R.string.account_list_title, currUserInfo.name)));

            // Add an item for each account owned by the current user (1st and 3rd party accounts)
            mAuthenticatorHelper = new AuthenticatorHelper(mContext, currUserInfo.getUserHandle(),
                    mFragment);
            mAuthenticatorHelper.listenToAccountUpdates();
            String[] accountTypes = mAuthenticatorHelper.getEnabledAccountTypes();
            for (int i = 0; i < accountTypes.length; i++) {
                String accountType = accountTypes[i];
                Account[] accounts = AccountManager.get(mContext)
                        .getAccountsByTypeAsUser(accountType, currUserInfo.getUserHandle());
                for (Account account : accounts) {
                    mItems.add(createAccountItem(mAuthenticatorHelper, account, accountType,
                            currUserInfo));
                }
            }

            // Add "+ Add account" option
            mItems.add(createAddAccountItem());

            // Add "Other users" title item
            mItems.add(createSubtitleItem(mContext.getString(R.string.other_users_title)));

            // Display other users on the system
            List<UserInfo> infos = mUserManager.getUsers(true);
            for (UserInfo userInfo : infos) {
                if (userInfo.id != currUserInfo.id) {
                    mItems.add(createUserItem(
                            userInfo, userInfo.name, true /* withDividerHidden*/));
                }
            }
        }

        // Creates a line for a user, clicking on it leads to the user details page
        private ListItem createUserItem(UserInfo userInfo,
                String title, boolean withDividerHidden) {
            TextListItem item = new TextListItem(mContext);
            item.setPrimaryActionIcon(getUserIcon(userInfo), false /* useLargeIcon */);
            item.setTitle(title);
            item.setOnClickListener(view -> mFragment.launchUserDetails(userInfo));
            // Hiding the divider to group the items together visually. All of those without a
            // divider between them will be part of the same "group".
            item.setHideDivider(withDividerHidden);
            return item;
        }

        // Creates a subtitle line for visual separation in the list
        private ListItem createSubtitleItem(String title) {
            TextListItem item = new TextListItem(mContext);
            item.setPrimaryActionEmptyIcon();
            item.setTitle(title);
            item.addViewBinder(viewHolder ->
                    viewHolder.getTitle().setTextAppearance(R.style.SettingsListHeader));
            // Hiding the divider after subtitle, since subtitle is a header for a group of items.
            item.setHideDivider(true);
            return item;
        }

        // Creates a line for an account that belongs to a given user
        private ListItem createAccountItem(AuthenticatorHelper authHelper, Account account, String
                accountType, UserInfo userInfo) {
            TextListItem item = new TextListItem(mContext);
            item.setPrimaryActionIcon(authHelper.getDrawableForType(mContext, accountType),
                    false /* useLargeIcon */);
            item.setTitle(account.name);
            item.setOnClickListener(view -> mFragment.launchAccountDetails(account, userInfo));
            item.setHideDivider(true);
            return item;
        }

        // Creates a clickable "+ Add Account" line.
        // Clicking on it leads to the Add an Account page.
        private ListItem createAddAccountItem() {
            TextListItem item = new TextListItem(mContext);
            item.setPrimaryActionIcon(R.drawable.ic_add, false /* useLargeIcon */);
            item.setTitle(mContext.getString(R.string.add_account_title));
            item.setOnClickListener(
                    view -> mFragment.launchAccountChooser());
            return item;
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
    }
}
