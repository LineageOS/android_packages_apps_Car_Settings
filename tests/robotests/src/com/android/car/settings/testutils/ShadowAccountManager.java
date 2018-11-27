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

package com.android.car.settings.testutils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.os.UserHandle;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(AccountManager.class)
public class ShadowAccountManager extends org.robolectric.shadows.ShadowAccountManager {
    @Implementation
    protected Account[] getAccountsByTypeAsUser(String type, UserHandle userHandle) {
        return getAccountsByType(type);
    }

    @Implementation
    protected AuthenticatorDescription[] getAuthenticatorTypesAsUser(int userId) {
        return getAuthenticatorTypes();
    }

    @Implementation
    protected Account[] getAccountsAsUser(int userId) {
        return getAccounts();
    }
}
