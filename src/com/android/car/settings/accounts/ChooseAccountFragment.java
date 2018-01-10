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

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import com.android.car.settings.R;
import com.android.car.settings.common.ListItemSettingsFragment;

import com.google.android.collect.Maps;

import com.android.internal.util.CharSequences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;

import androidx.car.widget.ListItem;

/**
 * Activity asking a user to select an account to be set up.
 *
 * <p>An extra {@link UserHandle} can be specified in the intent as {@link EXTRA_USER},
 * if the user for which the action needs to be performed is different to the one the
 * Settings App will run in.
 */
public class ChooseAccountFragment extends ListItemSettingsFragment {
    private static final String TAG = "ChooseAccountFragment";

    private Context mContext;
    private String[] mAuthorities;
    private HashSet<String> mAccountTypesFilter;
    private final ArrayList<ProviderEntry> mProviderList = new ArrayList<ProviderEntry>();
    private AuthenticatorDescription[] mAuthDescs;
    private HashMap<String, ArrayList<String>> mAccountTypeToAuthorities;
    private Map<String, AuthenticatorDescription> mTypeToAuthDescription
            = new HashMap<String, AuthenticatorDescription>();

    // The UserHandle of the user we are choosing an account for
    private UserHandle mUserHandle;
    private UserManager mUserManager;

    private static class ProviderEntry implements Comparable<ProviderEntry> {
        private final CharSequence name;
        private final String type;
        ProviderEntry(CharSequence providerName, String accountType) {
            name = providerName;
            type = accountType;
        }

        @Override
        public int compareTo(ProviderEntry another) {
            if (name == null) {
                return -1;
            }
            if (another.name == null) {
                return 1;
            }
            return CharSequences.compareToIgnoreCase(name, another.name);
        }
    }

    public static ChooseAccountFragment newInstance() {
        ChooseAccountFragment
                chooseAccountFragment = new ChooseAccountFragment();
        Bundle bundle = ListItemSettingsFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.add_an_account);
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_with_button);
        chooseAccountFragment.setArguments(bundle);
        return chooseAccountFragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mContext = getContext();
        mUserManager =
                (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        mUserHandle = mUserManager.getUserInfo(ActivityManager.getCurrentUser()).getUserHandle();
        mAuthorities = getActivity().getIntent().getStringArrayExtra(
                AccountHelper.AUTHORITIES_FILTER_KEY);

        if (mAccountTypesFilter == null) {
            mAccountTypesFilter = new HashSet<String>();
            mAccountTypesFilter.add(AccountHelper.ACCOUNT_TYPE_BLUETOOTH);
            mAccountTypesFilter.add(AccountHelper.ACCOUNT_TYPE_PHONE);
            mAccountTypesFilter.add(AccountHelper.ACCOUNT_TYPE_SIM);
        }

        updateAuthDescriptions();
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public List<ListItem> getListItems() {
        List<ListItem> items = new ArrayList<>();

        UserInfo currUserInfo = mUserManager.getUserInfo(ActivityManager.getCurrentUser());
        AccountHelper accountHelper = new AccountHelper(mContext, currUserInfo.getUserHandle());

        for (int i = 0; i < mProviderList.size(); i++) {
            String accountType = mProviderList.get(i).type;
            Drawable icon = accountHelper.getDrawableForType(mContext, accountType);
            items.add(new ListItem.Builder(mContext)
                    .withPrimaryActionIcon(icon, false /* useLargeIcon */)
                    .withTitle(mProviderList.get(i).name.toString())
                    .withOnClickListener(v -> onItemSelected(accountType))
                    .build());
        }
        return items;
    }

    // Starts a AddAccountActivity for the accountType that was clicked on.
    private void onItemSelected(String accountType) {
        Intent intent = new Intent(mContext, AddAccountActivity.class);
        intent.putExtra(AddAccountActivity.EXTRA_SELECTED_ACCOUNT, accountType);
        mContext.startActivity(intent);
    }

    /**
     * Updates provider icons. Subclasses should call this in onCreate()
     * and update any UI that depends on AuthenticatorDescriptions in onAuthDescriptionsUpdated().
     */
    private void updateAuthDescriptions() {
        mAuthDescs = AccountManager.get(getContext()).getAuthenticatorTypesAsUser(
                mUserHandle.getIdentifier());
        for (int i = 0; i < mAuthDescs.length; i++) {
            mTypeToAuthDescription.put(mAuthDescs[i].type, mAuthDescs[i]);
        }
        onAuthDescriptionsUpdated();
    }

    private void onAuthDescriptionsUpdated() {
        // Create list of providers to show on page.
        for (int i = 0; i < mAuthDescs.length; i++) {
            String accountType = mAuthDescs[i].type;
            CharSequence providerName = getLabelForType(accountType);

            // Get the account authorities implemented by the account type.
            ArrayList<String> accountAuths = getAuthoritiesForAccountType(accountType);
            boolean addAccountType = true;
            // If there are specific authorities required, we need to check whether it's
            // included in the account type.
            if (mAuthorities != null && mAuthorities.length > 0 && accountAuths != null) {
                addAccountType = false;
                for (int k = 0; k < mAuthorities.length; k++) {
                    if (accountAuths.contains(mAuthorities[k])) {
                        addAccountType = true;
                        break;
                    }
                }
            }
            // If account type is in the account type filter list, don't show it.
            if (addAccountType && mAccountTypesFilter != null
                    && mAccountTypesFilter.contains(accountType)) {
                addAccountType = false;
            }
            if (addAccountType) {
                mProviderList.add(new ProviderEntry(providerName, accountType));
            }
        }
    }

    public ArrayList<String> getAuthoritiesForAccountType(String type) {
        if (mAccountTypeToAuthorities == null) {
            mAccountTypeToAuthorities = Maps.newHashMap();
            SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypesAsUser(
                    mUserHandle.getIdentifier());
            for (int i = 0, n = syncAdapters.length; i < n; i++) {
                final SyncAdapterType adapterType = syncAdapters[i];
                ArrayList<String> authorities =
                        mAccountTypeToAuthorities.get(adapterType.accountType);
                if (authorities == null) {
                    authorities = new ArrayList<String>();
                    mAccountTypeToAuthorities.put(adapterType.accountType, authorities);
                }
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.d(TAG, "added authority " + adapterType.authority + " to accountType "
                            + adapterType.accountType);
                }
                authorities.add(adapterType.authority);
            }
        }
        return mAccountTypeToAuthorities.get(type);
    }

    /**
     * Gets the label associated with a particular account type. If none found, return null.
     * @param accountType the type of account
     * @return a CharSequence for the label or null if one cannot be found.
     */
    protected CharSequence getLabelForType(final String accountType) {
        CharSequence label = null;
        if (mTypeToAuthDescription.containsKey(accountType)) {
            try {
                AuthenticatorDescription desc = mTypeToAuthDescription.get(accountType);
                Context authContext = getActivity()
                        .createPackageContextAsUser(desc.packageName, 0 /* flags */, mUserHandle);
                label = authContext.getResources().getText(desc.labelId);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "No label name for account type " + accountType);
            } catch (Resources.NotFoundException e) {
                Log.w(TAG, "No label resource for account type " + accountType);
            }
        }
        return label;
    }
}
