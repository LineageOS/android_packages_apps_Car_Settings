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

package com.android.car.settings.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import com.android.settingslib.accounts.AuthenticatorHelper;

/**
 * Helper class for managing accounts that belong to a single user.
 */
public class AccountManagerHelper {
    private final Context mContext;
    private final UserHandle mCurrentUserHandle;
    private final AuthenticatorHelper mAuthenticatorHelper;
    private final AccountManager mAccountManager;

    public AccountManagerHelper(Context context,
            AuthenticatorHelper.OnAccountsUpdateListener listener) {
        mContext = context;
        mAccountManager = AccountManager.get(context);
        mCurrentUserHandle = new CarUserManagerHelper(
                context).getCurrentProcessUserInfo().getUserHandle();

        // Listen to account updates for this user.
        mAuthenticatorHelper = new AuthenticatorHelper(context, mCurrentUserHandle, listener);
    }

    /**
     * Starts listening to account updates. Every registered listener should be unregistered.
     */
    public void startListeningToAccountUpdates() {
        mAuthenticatorHelper.listenToAccountUpdates();
    }

    /**
     * Stops listening to account updates. Should be called on cleanup/destroy.
     */
    public void stopListeningToAccountUpdates() {
        mAuthenticatorHelper.stopListeningToAccountUpdates();
    }

    /**
     * Returns whether the given account is in the list of accounts for the current user.
     * Useful for checking whether an account has been deleted.
     *
     * @param account Account which existence we're checking for.
     * @return {@code true} if it exists, {@code false} if it doesn't.
     */
    public boolean accountExists(Account account) {
        if (account == null) {
            return false;
        }

        Account[] accounts = mAccountManager.getAccountsByTypeAsUser(account.type,
                mCurrentUserHandle);
        for (Account other : accounts) {
            if (other.equals(account)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Wrapper for {@code AuthenticatorHelper.getDrawableForType}.
     * Gets an icon associated with a particular account type.
     *
     * @param accountType the type of account
     * @return a drawable for the icon
     */
    public Drawable getDrawableForType(final String accountType) {
        return mAuthenticatorHelper.getDrawableForType(mContext, accountType);
    }

    /**
     * Wrapper for {@code AuthenticatorHelper.getLabelForType}.
     * Gets the label associated with a particular account type. If none found, return {@code null}.
     *
     * @param accountType the type of account
     * @return a CharSequence for the label or null if one cannot be found.
     */
    public CharSequence getLabelForType(final String accountType) {
        return mAuthenticatorHelper.getLabelForType(mContext, accountType);
    }
}
