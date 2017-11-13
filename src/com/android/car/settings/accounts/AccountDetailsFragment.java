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
import android.os.Bundle;
import android.widget.TextView;

import com.android.car.list.SingleTextLineItem;
import com.android.car.list.TypedPagedListAdapter;
import com.android.car.settings.R;
import com.android.car.settings.common.ListSettingsFragment;

import java.util.ArrayList;

/**
 * Shows account details, and delete account option.
 */
public class AccountDetailsFragment extends ListSettingsFragment {
    private static final String TAG = "AccountDetailsFragment";

    public static final String EXTRA_ACCOUNT_INFO = "extra_account_info";

    private Account mAccount;
    private AccountManager mAccountManager;

    public static AccountDetailsFragment getInstance(Account account) {
        AccountDetailsFragment
                accountDetailsFragment = new AccountDetailsFragment();
        Bundle bundle = ListSettingsFragment.getBundle();
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_with_button);
        bundle.putInt(EXTRA_TITLE_ID, R.string.account_details_title);
        bundle.putParcelable(EXTRA_ACCOUNT_INFO, account);
        accountDetailsFragment.setArguments(bundle);
        return accountDetailsFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAccount = getArguments().getParcelable(EXTRA_ACCOUNT_INFO);
    }

    @Override
    public ArrayList<TypedPagedListAdapter.LineItem> getLineItems() {
        ArrayList<TypedPagedListAdapter.LineItem> lineItems = new ArrayList<>();
        lineItems.add(new SingleTextLineItem(mAccount.name));
        return lineItems;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAccountManager = AccountManager.get(getActivity());
        TextView removeAccountBtn = getActivity().findViewById(R.id.action_button1);
        removeAccountBtn.setText(R.string.delete_button);
        removeAccountBtn.setOnClickListener(v -> removeAccount());
    }

    private void removeAccount() {
        // TODO Switch to removeAccountAsUser
        if (mAccountManager.removeAccountExplicitly(mAccount)) {
            getActivity().onBackPressed();
        }
    }
}
