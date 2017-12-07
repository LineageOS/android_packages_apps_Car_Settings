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

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static android.content.Intent.EXTRA_USER;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.Activity;
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
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.util.Log;
import android.widget.TextView;

import com.android.car.list.SingleTextLineItem;
import com.android.car.list.SubtitleTextLineItem;
import com.android.car.list.TypedPagedListAdapter;
import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;
import com.android.car.settings.common.ListSettingsFragment;

import com.google.android.collect.Maps;

import com.android.car.settings.users.UserLineItem;
import com.android.internal.util.CharSequences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Activity asking a user to select an account to be set up.
 *
 * <p>An extra {@link UserHandle} can be specified in the intent as {@link EXTRA_USER},
 * if the user for which the action needs to be performed is different to the one the
 * Settings App will run in.
 */
public class ChooseAccountFragment extends ListSettingsFragment {
    private static final String TAG = "ChooseAccountFragment";

    private Context mContext;
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
        Bundle bundle = ListSettingsFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.choose_account_title);
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

        updateAuthDescriptions();
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public ArrayList<TypedPagedListAdapter.LineItem> getLineItems() {
        ArrayList<TypedPagedListAdapter.LineItem> items = new ArrayList<>();

        UserInfo currUserInfo = mUserManager.getUserInfo(ActivityManager.getCurrentUser());

        for (int i = 0; i < mProviderList.size(); i++) {
            items.add(new AccountTypeLineItem(
                    mContext,
                    currUserInfo,
                    mProviderList.get(i).name.toString(),
                    mProviderList.get(i).type));
        }
        return items;
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

            // Show all accounts type for now, before figure out why no authorities
            // for Facebook. See b/70339686 for details.
            mProviderList.add(new ProviderEntry(providerName, accountType));
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
                        .createPackageContextAsUser(desc.packageName, 0, mUserHandle);
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