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

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.SyncAdapterType;
import android.os.Bundle;
import android.os.UserHandle;

import java.util.HashSet;
import java.util.Set;

/** Helper that provides utility methods for account syncing. */
class AccountSyncHelper {
    private AccountSyncHelper() {
    }

    /** Returns the syncable sync adapters available for an account. */
    static Set<SyncAdapterType> getSyncableSyncAdaptersForAccount(Account account,
            UserHandle userHandle) {
        Set<SyncAdapterType> adapters = new HashSet<>();

        SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypesAsUser(
                userHandle.getIdentifier());
        for (int i = 0; i < syncAdapters.length; i++) {
            SyncAdapterType syncAdapter = syncAdapters[i];
            String authority = syncAdapter.authority;

            // If the sync adapter is not for this account type, don't include it
            if (!syncAdapter.accountType.equals(account.type)) {
                continue;
            }

            boolean isSyncable = ContentResolver.getIsSyncableAsUser(account, authority,
                    userHandle.getIdentifier()) > 0;
            // If the adapter is not syncable, don't include it
            if (!isSyncable) {
                continue;
            }

            adapters.add(syncAdapter);
        }

        return adapters;
    }

    /**
     * Requests a sync if it is allowed.
     *
     * <p>Derived from
     * {@link com.android.settings.accounts.AccountSyncSettings#requestOrCancelSync}.
     */
    static void requestSyncIfAllowed(Account account, String authority, int userId) {
        if (!syncIsAllowed(account, authority, userId)) {
            return;
        }

        Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSyncAsUser(account, authority, userId, extras);
    }

    private static boolean syncIsAllowed(Account account, String authority, int userId) {
        boolean oneTimeSyncMode = !ContentResolver.getMasterSyncAutomaticallyAsUser(userId);
        boolean syncEnabled = ContentResolver.getSyncAutomaticallyAsUser(account, authority,
                userId);
        return oneTimeSyncMode || syncEnabled;
    }
}
