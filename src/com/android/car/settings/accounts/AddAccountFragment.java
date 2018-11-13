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
 * limitations under the License.
 */

package com.android.car.settings.accounts;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.annotation.XmlRes;

import com.android.car.settings.R;
import com.android.car.settings.common.BasePreferenceFragment;
import com.android.car.settings.common.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Lists accounts the user can add.
 */
public class AddAccountFragment extends BasePreferenceFragment implements
        AddAccountPreferenceController.AddAccountListener {
    private static final int ADD_ACCOUNT_REQUEST_CODE = 1001;
    private static final Logger LOG = new Logger(AddAccountFragment.class);

    @Override
    @XmlRes
    protected int getPreferenceScreenResId() {
        return R.xml.add_account_fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        String[] authorities = requireActivity().getIntent().getStringArrayExtra(
                Settings.EXTRA_AUTHORITIES);
        if (authorities != null) {
            use(AddAccountPreferenceController.class, R.string.pk_add_account)
                    .setAuthorities(new ArrayList<>(Arrays.asList(authorities)));
        }

        String[] accountTypesForFilter = requireActivity().getIntent().getStringArrayExtra(
                Settings.EXTRA_ACCOUNT_TYPES);
        if (accountTypesForFilter != null) {
            use(AddAccountPreferenceController.class, R.string.pk_add_account)
                    .setAccountTypesFilter(new HashSet<>(Arrays.asList(accountTypesForFilter)));
        }

        use(AddAccountPreferenceController.class, R.string.pk_add_account)
                .setListener(this);
    }

    @Override
    public void addAccount(String accountType) {
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
