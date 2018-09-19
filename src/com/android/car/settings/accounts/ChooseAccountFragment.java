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
 * limitations under the License.
 */

package com.android.car.settings.accounts;

import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.annotation.StringRes;
import androidx.car.widget.ListItemProvider;

import com.android.car.settings.R;
import com.android.car.settings.common.ListItemSettingsFragment;
import com.android.car.settings.common.Logger;
import com.android.settingslib.accounts.AuthenticatorHelper;

/**
 * Activity asking a user to select an account to be set up.
 *
 * <p>An extra {@link UserHandle} can be specified in the intent as {@link EXTRA_USER},
 * if the user for which the action needs to be performed is different to the one the
 * Settings App will run in.
 */
public class ChooseAccountFragment extends ListItemSettingsFragment
        implements AuthenticatorHelper.OnAccountsUpdateListener {
    private static final Logger LOG = new Logger(ChooseAccountFragment.class);
    private static final int ADD_ACCOUNT_REQUEST_CODE = 1001;

    private ChooseAccountItemProvider mItemProvider;

    @Override
    @StringRes
    protected int getTitleId() {
        return R.string.add_an_account;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mItemProvider = new ChooseAccountItemProvider(getContext(), this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAccountsUpdate(UserHandle userHandle) {
        LOG.v("Accounts changed, refreshing the account list.");
        mItemProvider.refreshItems();
        refreshList();
    }

    @Override
    public ListItemProvider getItemProvider() {
        return mItemProvider;
    }

    /** Starts the activity that handles adding an account. */
    void onAddAccount(String accountType) {
        Intent intent = new Intent(getContext(), AddAccountActivity.class);
        intent.putExtra(AddAccountActivity.EXTRA_SELECTED_ACCOUNT, accountType);
        startActivityForResult(intent, ADD_ACCOUNT_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != ADD_ACCOUNT_REQUEST_CODE) {
            LOG.d("Unidentified activity returned a result! Ignoring the result.");
            return;
        }
        // Done with adding the account, so go back.
        getFragmentController().goBack();
    }
}
