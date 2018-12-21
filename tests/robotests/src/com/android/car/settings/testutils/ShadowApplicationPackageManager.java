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

import android.annotation.UserIdInt;
import android.app.ApplicationPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/** Shadow of ApplicationPackageManager that allows the getting of content providers per user. */
@Implements(value = ApplicationPackageManager.class)
public class ShadowApplicationPackageManager extends
        org.robolectric.shadows.ShadowApplicationPackageManager {

    @Implementation
    public Drawable getUserBadgedIcon(Drawable icon, UserHandle user) {
        return icon;
    }

    @Override
    @Implementation
    public ProviderInfo resolveContentProviderAsUser(String name, int flags,
            @UserIdInt int userId) {
        return resolveContentProvider(name, flags);
    }

    @Implementation
    public int getPackageUidAsUser(String packageName, int flags, int userId)
            throws PackageManager.NameNotFoundException {
        return 0;
    }
}
