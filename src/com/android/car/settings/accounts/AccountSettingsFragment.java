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
import com.android.car.settings.common.SettingsFragment;
import com.android.car.settings.search.CarBaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/**
 * Lists the user's accounts and any related options.
 */
@SearchIndexable
public class AccountSettingsFragment extends SettingsFragment {

    @Override
    @XmlRes
    protected int getPreferenceScreenResId() {
        return R.xml.account_settings_fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Intent activityIntent = requireActivity().getIntent();
        String[] authorities = activityIntent.getStringArrayExtra(Settings.EXTRA_AUTHORITIES);
        String[] accountTypes = activityIntent.getStringArrayExtra(Settings.EXTRA_ACCOUNT_TYPES);
        if (authorities != null || accountTypes != null) {
            use(AccountListPreferenceController.class, R.string.pk_account_list)
                    .setAuthorities(authorities);
            use(AddAccountPreferenceController.class, R.string.pk_account_settings_add)
                    .setAuthorities(authorities).setAccountTypes(accountTypes);
        }
    }

    /**
     * Data provider for Settings Search.
     */
    public static final CarBaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new CarBaseSearchIndexProvider(R.xml.account_settings_fragment,
                    Settings.ACTION_SYNC_SETTINGS);
}
