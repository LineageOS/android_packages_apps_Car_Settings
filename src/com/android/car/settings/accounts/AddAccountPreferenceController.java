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

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.NoSetupPreferenceController;
import com.android.settingslib.accounts.AuthenticatorHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Controller for showing the user the list of accounts they can add.
 *
 * <p>Largely derived from {@link com.android.settings.accounts.ChooseAccountActivity}
 */
public class AddAccountPreferenceController extends NoSetupPreferenceController implements
        AuthenticatorHelper.OnAccountsUpdateListener {
    private final UserHandle mUserHandle;
    private final AuthenticatorHelper mAuthenticatorHelper;
    private ArrayList<String> mAuthorities;
    private HashSet<String> mAccountTypesFilter;
    private AddAccountListener mListener;
    private PreferenceScreen mPreferenceScreen;
    private ArrayMap<String, AuthenticatorDescriptionPreference> mPreferences = new ArrayMap<>();

    public AddAccountPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
        mUserHandle = new CarUserManagerHelper(context).getCurrentProcessUserInfo().getUserHandle();

        mAuthenticatorHelper = new AuthenticatorHelper(context, mUserHandle, this);
    }

    /** Sets the authorities that the user has. */
    public void setAuthorities(ArrayList<String> authorities) {
        mAuthorities = authorities;
    }

    /** Sets the filter for accounts that should be shown. */
    public void setAccountTypesFilter(HashSet<String> accountTypesFilter) {
        mAccountTypesFilter = accountTypesFilter;
    }

    /** Sets the AddAccountListener. */
    public void setListener(AddAccountListener listener) {
        mListener = listener;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreferenceScreen = screen;
        forceUpdateAccountsCategory(mPreferenceScreen);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        // When a preference is clicked, start up the flow for adding an account
        if (preference instanceof AuthenticatorDescriptionPreference) {
            AuthenticatorDescriptionPreference pref =
                    (AuthenticatorDescriptionPreference) preference;
            return onAddAccount(pref.getAccountType());
        }
        return false;
    }

    /**
     * Registers the account update callback.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mAuthenticatorHelper.listenToAccountUpdates();
    }

    /**
     * Unregisters the account update callback.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mAuthenticatorHelper.stopListeningToAccountUpdates();
    }

    @Override
    public void onAccountsUpdate(UserHandle userHandle) {
        // Only force a refresh if accounts have changed for the current user.
        if (userHandle.equals(mUserHandle)) {
            forceUpdateAccountsCategory(mPreferenceScreen);
        }
    }

    /** Forces a refresh of the authenticator description preferences. */
    @VisibleForTesting
    void forceUpdateAccountsCategory(PreferenceScreen screen) {
        // Fake an account update so that the account types are updated
        mAuthenticatorHelper.onReceive(mContext, /* intent= */ null);

        Map<String, AuthenticatorDescriptionPreference> preferencesToRemove =
                new ArrayMap<>(mPreferences);
        List<AuthenticatorDescriptionPreference> preferences =
                getAuthenticatorDescriptionPreferences(preferencesToRemove);
        // Add all preferences that aren't already shown
        for (int i = 0; i < preferences.size(); i++) {
            AuthenticatorDescriptionPreference preference = preferences.get(i);
            preference.setOrder(i);
            String key = preference.getKey();
            if (!mPreferences.containsKey(key)) {
                screen.addPreference(preference);
                mPreferences.put(key, preference);
            }
        }

        // Remove all preferences that should no longer be shown
        for (String key : preferencesToRemove.keySet()) {
            screen.removePreference(mPreferences.get(key));
            mPreferences.remove(key);
        }
    }

    /**
     * Returns a list of preferences corresponding to the account types the user can add.
     *
     * <p> Derived from
     * {@link com.android.settings.accounts.ChooseAccountActivity#onAuthDescriptionsUpdated}
     *
     * @param preferencesToRemove the current preferences shown; will contain the preferences that
     *                            need to be removed from the screen after method execution
     */
    private List<AuthenticatorDescriptionPreference> getAuthenticatorDescriptionPreferences(
            Map<String, AuthenticatorDescriptionPreference> preferencesToRemove) {
        AuthenticatorDescription[] authenticatorDescriptions = AccountManager.get(
                mContext).getAuthenticatorTypesAsUser(
                mUserHandle.getIdentifier());

        ArrayList<AuthenticatorDescriptionPreference> authenticatorDescriptionPreferences =
                new ArrayList<>();
        // Create list of account providers to show on page.
        for (AuthenticatorDescription authenticatorDescription : authenticatorDescriptions) {
            String accountType = authenticatorDescription.type;
            CharSequence label = mAuthenticatorHelper.getLabelForType(mContext, accountType);
            Drawable icon = mAuthenticatorHelper.getDrawableForType(mContext, accountType);

            List<String> accountAuthorities =
                    mAuthenticatorHelper.getAuthoritiesForAccountType(accountType);

            // If there are specific authorities required, we need to check whether they are
            // included in the account type.
            boolean authorized =
                    (mAuthorities == null || mAuthorities.isEmpty() || accountAuthorities == null
                            || !Collections.disjoint(accountAuthorities, mAuthorities));

            // If there is an account type filter, make sure this account type is included.
            authorized = authorized && (mAccountTypesFilter == null
                    || !mAccountTypesFilter.contains(accountType));

            // If authorized, add a preference for the provider to the list and remove it from
            // preferencesToRemove.
            if (authorized) {
                AuthenticatorDescriptionPreference preference = preferencesToRemove.remove(
                        accountType);
                if (preference != null) {
                    authenticatorDescriptionPreferences.add(preference);
                    continue;
                }
                authenticatorDescriptionPreferences.add(
                        new AuthenticatorDescriptionPreference(mContext, accountType, label, icon));
            }
        }

        Collections.sort(authenticatorDescriptionPreferences, Comparator.comparing(
                (AuthenticatorDescriptionPreference a) -> a.getTitle().toString()));

        return authenticatorDescriptionPreferences;
    }

    /** Defers to the listener to handle adding the account. */
    private boolean onAddAccount(String accountType) {
        if (mListener != null) {
            mListener.addAccount(accountType);
            return true;
        }
        return false;
    }

    /** Handles adding accounts. */
    interface AddAccountListener {
        /** Handles adding an account. */
        void addAccount(String accountType);
    }

    private static class AuthenticatorDescriptionPreference extends Preference {
        private final String mType;

        AuthenticatorDescriptionPreference(Context context, String accountType, CharSequence label,
                Drawable icon) {
            super(context);
            mType = accountType;

            setKey(accountType);
            setTitle(label);
            setIcon(icon);
        }

        private String getAccountType() {
            return mType;
        }
    }
}
