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

import android.content.ContentResolver;
import android.content.SyncAdapterType;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Derived from {@link com.android.settings.testutils.shadow.ShadowContentResolver}
 *
 * <p>Needed for many account-related tests because the default ShadowContentResolver does not
 * include an implementation of getSyncAdapterTypesAsUser, which is used by {@link
 * com.android.settingslib.accounts.AuthenticatorHelper#buildAccountTypeToAuthoritiesMap}.
 */
@Implements(ContentResolver.class)
public class ShadowContentResolver extends org.robolectric.shadows.ShadowContentResolver {

    private static SyncAdapterType[] sSyncAdapterTypes = new SyncAdapterType[0];

    @Implementation
    public static SyncAdapterType[] getSyncAdapterTypesAsUser(int userId) {
        return sSyncAdapterTypes;
    }

    public static void setSyncAdapterTypes(SyncAdapterType[] syncAdapterTypes) {
        sSyncAdapterTypes = syncAdapterTypes;
    }

    public static void reset() {
        org.robolectric.shadows.ShadowContentResolver.reset();
        sSyncAdapterTypes = new SyncAdapterType[0];
    }
}
