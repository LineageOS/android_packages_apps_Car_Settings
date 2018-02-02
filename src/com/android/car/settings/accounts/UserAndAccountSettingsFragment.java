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
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.android.car.settings.R;
import com.android.car.settings.common.ListItemSettingsFragment;
import com.android.car.settings.users.ConfirmCreateNewUserDialog;
import com.android.car.settings.users.UserDetailsSettingsFragment;
import com.android.car.settings.users.UserManagerHelper;
import com.android.settingslib.accounts.AuthenticatorHelper;

import androidx.car.widget.ListItemProvider;

/**
 * Lists all Users available on this device.
 */
public class UserAndAccountSettingsFragment extends ListItemSettingsFragment
        implements AuthenticatorHelper.OnAccountsUpdateListener,
        UserManagerHelper.OnUsersUpdateListener,
        UserAndAccountItemProvider.UserAndAccountClickListener,
        ConfirmCreateNewUserDialog.ConfirmCreateNewUserListener {
    private static final String TAG = "UserAndAccountSettings";

    private Context mContext;
    private UserAndAccountItemProvider mItemProvider;
    private AccountManagerHelper mAccountManagerHelper;
    private UserManagerHelper mUserManagerHelper;

    private ProgressBar mProgressBar;
    private Button mAddUserButton;

    private AsyncTask mAddNewUserTask;

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

        mAccountManagerHelper = new AccountManagerHelper(mContext, this);
        mAccountManagerHelper.startListeningToAccountUpdates();

        mUserManagerHelper = new UserManagerHelper(mContext);
        mItemProvider = new UserAndAccountItemProvider(mContext, this,
                mUserManagerHelper, mAccountManagerHelper);

        // Register to receive changes to the users.
        mUserManagerHelper.registerOnUsersUpdateListener(this);

        // Super class's onActivityCreated need to be called after mContext is initialized.
        // Because getLineItems is called in there.
        super.onActivityCreated(savedInstanceState);

        mProgressBar = getActivity().findViewById(R.id.progress_bar);

        mAddUserButton = (Button) getActivity().findViewById(R.id.action_button1);
        mAddUserButton.setText(R.string.user_add_user_menu);
        mAddUserButton.setOnClickListener(v -> {
            ConfirmCreateNewUserDialog dialog =
                    new ConfirmCreateNewUserDialog();
            dialog.setConfirmCreateNewUserListener(this);
            dialog.show(this);
        });
    }

    @Override
    public void onCreateNewUserConfirmed() {
        mAddNewUserTask = new AddNewUserTask().execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mAddNewUserTask != null) {
            mAddNewUserTask.cancel(false /* mayInterruptIfRunning */);
        }

        mUserManagerHelper.unregisterOnUsersUpdateListener();
        mAccountManagerHelper.stopListeningToAccountUpdates();
    }

    @Override
    public void onAccountsUpdate(UserHandle userHandle) {
        refreshListItems();
    }

    @Override
    public void onUsersUpdate() {
        refreshListItems();
    }

    private void refreshListItems() {
        mItemProvider.refreshItems();
        refreshList();
    }

    @Override
    public void onUserClicked(UserInfo userInfo) {
        mFragmentController.launchFragment(UserDetailsSettingsFragment.getInstance(userInfo));
    }

    @Override
    public void onAccountClicked(Account account, UserInfo userInfo) {
        mFragmentController.launchFragment(AccountDetailsFragment.newInstance(account, userInfo));
    }

    public void onAddAccountClicked() {
        mFragmentController.launchFragment(ChooseAccountFragment.newInstance());
    }

    @Override
    public ListItemProvider getItemProvider() {
        return mItemProvider;
    }

    private class AddNewUserTask extends AsyncTask<Void, Void, UserInfo> {
        @Override
        protected UserInfo doInBackground(Void... params) {
            return mUserManagerHelper.createNewUser();
        }

        @Override
        protected void onPreExecute() {
            mAddUserButton.setEnabled(false);
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(UserInfo user) {
            mAddUserButton.setEnabled(true);
            mProgressBar.setVisibility(View.GONE);
            if (user != null) {
                mUserManagerHelper.switchToUser(user);
            }
        }
    }
}
