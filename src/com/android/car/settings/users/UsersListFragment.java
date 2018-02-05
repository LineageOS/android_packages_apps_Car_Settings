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

package com.android.car.settings.users;

import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.android.car.settings.R;
import com.android.car.settings.accounts.UserDetailsFragment;
import com.android.car.settings.common.ListItemSettingsFragment;

import androidx.car.widget.ListItemProvider;

/**
 * Lists all Users available on this device.
 */
public class UsersListFragment extends ListItemSettingsFragment
        implements UserManagerHelper.OnUsersUpdateListener,
        UsersItemProvider.UserClickListener,
        ConfirmCreateNewUserDialog.ConfirmCreateNewUserListener {
    private static final String TAG = "UsersListFragment";

    private UsersItemProvider mItemProvider;
    private UserManagerHelper mUserManagerHelper;

    private ProgressBar mProgressBar;
    private Button mAddUserButton;

    private AsyncTask mAddNewUserTask;

    public static UsersListFragment newInstance() {
        UsersListFragment usersListFragment = new UsersListFragment();
        Bundle bundle = ListItemSettingsFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.user_and_account_settings_title);
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_with_button);
        usersListFragment.setArguments(bundle);
        return usersListFragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mUserManagerHelper = new UserManagerHelper(getContext());
        mItemProvider =
                new UsersItemProvider(getContext(), this, mUserManagerHelper);

        // Register to receive changes to the users.
        mUserManagerHelper.registerOnUsersUpdateListener(this);

        // Super class's onActivityCreated need to be called after mContext is initialized.
        // Because getLineItems is called in there.
        super.onActivityCreated(savedInstanceState);

        mProgressBar = getActivity().findViewById(R.id.progress_bar);

        // Only add the add user button if the current user is allowed to add a user.
        if(mUserManagerHelper.canAddUsers()) {
            mAddUserButton = (Button) getActivity().findViewById(R.id.action_button1);
            mAddUserButton.setText(R.string.user_add_user_menu);
            mAddUserButton.setOnClickListener(v -> {
                ConfirmCreateNewUserDialog dialog =
                        new ConfirmCreateNewUserDialog();
                dialog.setConfirmCreateNewUserListener(this);
                dialog.show(this);
            });
        }
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
    }

    @Override
    public void onUsersUpdate() {
        refreshListItems();
    }

    @Override
    public void onUserClicked(UserInfo userInfo) {
        if (mUserManagerHelper.userIsCurrentUser(userInfo)) {
            // Is it's current user, launch fragment that displays their accounts.
            mFragmentController.launchFragment(UserDetailsFragment.newInstance());
        } else {
            mFragmentController.launchFragment(EditUsernameFragment.getInstance(userInfo));
        }
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

    private void refreshListItems() {
        mItemProvider.refreshItems();
        refreshList();
    }
}
