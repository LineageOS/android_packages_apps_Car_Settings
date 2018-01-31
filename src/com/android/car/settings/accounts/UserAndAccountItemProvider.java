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

package com.android.car.settings.accounts;

import android.accounts.Account;
import android.content.Context;
import android.content.pm.UserInfo;

import com.android.car.settings.R;
import com.android.car.settings.users.UserManagerHelper;

import java.util.ArrayList;
import java.util.List;

import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.TextListItem;

/**
 * Implementation of {@link ListItemProvider} for {@link UserAndAccountSettingsFragment}.
 * Creates items that represent the current user, current user's accounts and other users.
 */
class UserAndAccountItemProvider extends ListItemProvider {
    private final List<ListItem> mItems = new ArrayList<>();
    private final Context mContext;
    private final UserAndAccountClickListener mItemClickListener;
    private final UserManagerHelper mUserManagerHelper;
    private final AccountManagerHelper mAccountManagerHelper;

    /**
     * Interface for registering clicks on user or account items.
     */
    interface UserAndAccountClickListener {
        /**
         * Invoked when user is clicked.
         *
         * @param userInfo User for which the click is registered.
         */
        void onUserClicked(UserInfo userInfo);

        /**
         * Invoked when a specific account is clicked on.
         *
         * @param account Account for which to display details.
         * @param userInfo User who's the owner of the account.
         */
        void onAccountClicked(Account account, UserInfo userInfo);

        /**
         * Invoked when add account button is clicked.
         */
        void onAddAccountClicked();
    }

    UserAndAccountItemProvider(Context context, UserAndAccountClickListener itemClickListener,
            UserManagerHelper userManagerHelper, AccountManagerHelper accountManagerHelper) {
        mContext = context;
        mItemClickListener = itemClickListener;
        mUserManagerHelper = userManagerHelper;
        mAccountManagerHelper = accountManagerHelper;
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
        // WithDividerHidden = false will hide the divider to group the items together visually.
        // All of those without a divider between them will be part of the same "group".
        mItems.add(createUserItem(currUserInfo,
                mContext.getString(R.string.current_user_name, currUserInfo.name),
                false /* withDividerHidden */));

        // Add "Account for $User" title for a list of accounts.
        mItems.add(createSubtitleItem(
                mContext.getString(R.string.account_list_title, currUserInfo.name)));

        // Add an item for each account owned by the current user (1st and 3rd party accounts)
        for (Account account : mAccountManagerHelper.getAccountsForCurrentUser()) {
            mItems.add(createAccountItem(account, account.type, currUserInfo));
        }

        // Add "+ Add account" option
        mItems.add(createAddAccountItem());

        // Add "Other users" title item
        mItems.add(createSubtitleItem(mContext.getString(R.string.other_users_title)));

        // Display other users on the system
        List<UserInfo> infos = mUserManagerHelper.getOtherUsers();
        for (UserInfo userInfo : infos) {
            mItems.add(createUserItem(
                    userInfo, userInfo.name, true /* withDividerHidden*/));
        }
    }

    // Creates a line for a user, clicking on it leads to the user details page
    private ListItem createUserItem(UserInfo userInfo,
            String title, boolean withDividerHidden) {
        TextListItem item = new TextListItem(mContext);
        item.setPrimaryActionIcon(mUserManagerHelper.getUserIcon(userInfo),
                false /* useLargeIcon */);
        item.setTitle(title);
        item.setOnClickListener(view -> mItemClickListener.onUserClicked(userInfo));
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
    private ListItem createAccountItem(Account account, String accountType,
            UserInfo userInfo) {
        TextListItem item = new TextListItem(mContext);
        item.setPrimaryActionIcon(mAccountManagerHelper.getDrawableForType(accountType),
                false /* useLargeIcon */);
        item.setTitle(account.name);
        item.setOnClickListener(view -> mItemClickListener.onAccountClicked(account, userInfo));
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
                view -> mItemClickListener.onAddAccountClicked());
        return item;
    }
}