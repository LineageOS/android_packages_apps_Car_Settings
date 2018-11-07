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
import android.annotation.UserIdInt;
import android.content.ContentResolver;
import android.content.SyncAdapterType;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.HashMap;
import java.util.Map;

/**
 * Derived from {@link com.android.settings.testutils.shadow.ShadowContentResolver}
 *
 * <p>Needed for many account-related tests because the default ShadowContentResolver does not
 * include an implementation of getSyncAdapterTypesAsUser, which is used by {@link
 * com.android.settingslib.accounts.AuthenticatorHelper#buildAccountTypeToAuthoritiesMap}.
 */
@Implements(ContentResolver.class)
public class ShadowContentResolver {
    private static final int SYNCABLE = 1;

    private static SyncAdapterType[] sSyncAdapterTypes = new SyncAdapterType[0];
    private static Map<String, Integer> sSyncable = new HashMap<>();
    private static Map<String, Boolean> sSyncAutomatically = new HashMap<>();
    private static Map<Integer, Boolean> sMasterSyncAutomatically = new HashMap<>();

    @Implementation
    public static SyncAdapterType[] getSyncAdapterTypesAsUser(int userId) {
        return sSyncAdapterTypes;
    }

    @Implementation
    public static int getIsSyncableAsUser(Account account, String authority, int userId) {
        return sSyncable.getOrDefault(authority, SYNCABLE);
    }

    @Implementation
    public static boolean getSyncAutomaticallyAsUser(Account account, String authority,
            int userId) {
        return sSyncAutomatically.getOrDefault(authority, true);
    }

    @Implementation
    public static boolean getMasterSyncAutomaticallyAsUser(int userId) {
        return sMasterSyncAutomatically.getOrDefault(userId, true);
    }

    public static void setSyncAdapterTypes(SyncAdapterType[] syncAdapterTypes) {
        sSyncAdapterTypes = syncAdapterTypes;
    }

    @Implementation
    public static void setIsSyncable(Account account, String authority, int syncable) {
        sSyncable.put(authority, syncable);
    }

    @Implementation
    public static void setSyncAutomaticallyAsUser(Account account, String authority, boolean sync,
            @UserIdInt int userId) {
        sSyncAutomatically.put(authority, sync);
    }

    @Implementation
    public static void setMasterSyncAutomaticallyAsUser(boolean sync, @UserIdInt int userId) {
        sMasterSyncAutomatically.put(userId, sync);
    }

    public static void reset() {
        sSyncable.clear();
        sSyncAutomatically.clear();
        sMasterSyncAutomatically.clear();
        sSyncAdapterTypes = new SyncAdapterType[0];
    }
}
