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

package com.android.car.settings.profiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.XmlRes;

import com.android.car.settings.R;
import com.android.car.settings.accounts.AccountGroupPreferenceController;
import com.android.car.settings.accounts.AccountListPreferenceController;
import com.android.car.settings.accounts.AddAccountPreferenceController;
import com.android.car.settings.search.CarBaseSearchIndexProvider;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.search.SearchIndexable;

/**
 * Shows details for a user with the ability to remove user and edit current user.
 */
@SearchIndexable
public class ProfileDetailsFragment extends ProfileDetailsBaseFragment {

    private boolean mIsStarted;

    /** Creates instance of ProfileDetailsFragment. */
    public static ProfileDetailsFragment newInstance(int userId) {
        return (ProfileDetailsFragment) addUserIdToFragmentArguments(
                new ProfileDetailsFragment(), userId);
    }

    @VisibleForTesting
    final BroadcastReceiver mUserUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Update the user info value, as it may have changed.
            refreshUserInfo();
            if (mIsStarted) {
                // Update the text in the action bar when there is a user update.
                getToolbar().setTitle(getTitleText());
            }
        }
    };

    @Override
    @XmlRes
    protected int getPreferenceScreenResId() {
        return R.xml.user_details_fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(ProfileDetailsHeaderPreferenceController.class,
                R.string.pk_user_details_header).setUserInfo(getUserInfo());
        use(UserDetailsActionButtonsPreferenceController.class,
                R.string.pk_user_details_action_buttons).setUserInfo(getUserInfo());
        use(AccountGroupPreferenceController.class,
                R.string.pk_account_group).setUserInfo(getUserInfo());
        use(ProfileDetailsDeletePreferenceController.class,
                R.string.pk_profile_details_delete).setUserInfo(getUserInfo());

        // Accounts information
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerForUserEvents();
    }

    @Override
    public void onStart() {
        super.onStart();
        mIsStarted = true;
        getToolbar().setTitle(getTitleText());
    }

    @Override
    public void onStop() {
        mIsStarted = false;
        super.onStop();
    }

    @Override
    public void onDestroy() {
        unregisterForUserEvents();
        super.onDestroy();
    }

    @Override
    protected String getTitleText() {
        return getString(R.string.profiles_and_accounts_settings_title);
    }

    private void registerForUserEvents() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_INFO_CHANGED);
        getContext().registerReceiver(mUserUpdateReceiver, filter);
    }

    private void unregisterForUserEvents() {
        getContext().unregisterReceiver(mUserUpdateReceiver);
    }

    /**
     * Data provider for Settings Search.
     */
    public static final CarBaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new CarBaseSearchIndexProvider(R.xml.user_details_fragment,
                    Settings.ACTION_USER_SETTINGS);
}
