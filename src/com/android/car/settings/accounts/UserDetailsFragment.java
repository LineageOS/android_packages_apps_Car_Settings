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
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.View;
import android.widget.Button;

import androidx.car.widget.ListItemProvider;

import com.android.car.settings.R;
import com.android.car.settings.common.ListItemSettingsFragment;
import com.android.car.settings.users.EditUsernameFragment;
import com.android.settingslib.accounts.AuthenticatorHelper;
import com.android.settingslib.users.UserManagerHelper;

/**
 * Shows current user and the accounts that belong to the user.
 */
public class UserDetailsFragment extends ListItemSettingsFragment
        implements AuthenticatorHelper.OnAccountsUpdateListener,
        UserManagerHelper.OnUsersUpdateListener,
        UserAndAccountItemProvider.UserAndAccountClickListener {
    private static final String TAG = "UserAndAccountSettings";

    private UserAndAccountItemProvider mItemProvider;
    private AccountManagerHelper mAccountManagerHelper;
    private UserManagerHelper mUserManagerHelper;

    private Button mAddAccountButton;

    public static UserDetailsFragment newInstance() {
        UserDetailsFragment userDetailsFragment = new UserDetailsFragment();
        Bundle bundle = ListItemSettingsFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.user_settings_title);
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_with_button);
        userDetailsFragment.setArguments(bundle);
        return userDetailsFragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mAccountManagerHelper = new AccountManagerHelper(getContext(), this);
        mAccountManagerHelper.startListeningToAccountUpdates();

        mUserManagerHelper = new UserManagerHelper(getContext());
        mItemProvider = new UserAndAccountItemProvider(getContext(), this,
                mUserManagerHelper, mAccountManagerHelper);

        // Register to receive changes to the users.
        mUserManagerHelper.registerOnUsersUpdateListener(this);

        // Super class's onActivityCreated need to be called after mContext is initialized.
        // Because getLineItems is called in there.
        super.onActivityCreated(savedInstanceState);

        mAddAccountButton = (Button) getActivity().findViewById(R.id.action_button1);
        if (mUserManagerHelper.currentProcessCanModifyAccounts()) {
            mAddAccountButton.setText(R.string.user_add_account_menu);
            mAddAccountButton.setOnClickListener(v -> onAddAccountClicked());
        } else {
            mAddAccountButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mUserManagerHelper.unregisterOnUsersUpdateListener();
        mAccountManagerHelper.stopListeningToAccountUpdates();

        // The action button may be hidden at some point, so make it visible again
        mAddAccountButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAccountsUpdate(UserHandle userHandle) {
        refreshListItems();
    }

    @Override
    public void onUsersUpdate() {
        refreshListItems();
    }

    @Override
    public void onUserClicked(UserInfo userInfo) {
        getFragmentController().launchFragment(EditUsernameFragment.getInstance(userInfo));
    }

    @Override
    public void onAccountClicked(Account account, UserInfo userInfo) {
        getFragmentController().launchFragment(
                AccountDetailsFragment.newInstance(account, userInfo));
    }

    @Override
    public ListItemProvider getItemProvider() {
        return mItemProvider;
    }

    private void refreshListItems() {
        mItemProvider.refreshItems();
        refreshList();
    }

    private void onAddAccountClicked() {
        getFragmentController().launchFragment(ChooseAccountFragment.newInstance());
    }
}
