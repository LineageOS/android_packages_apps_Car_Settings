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
import android.content.pm.UserInfo;
import android.os.UserManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@Implements(UserManager.class)
public class ShadowUserManager extends org.robolectric.shadows.ShadowUserManager {
    private static UserManager sInstance;

    public static void setInstance(UserManager manager) {
        sInstance = manager;
    }

    @Implementation
    protected UserInfo getUserInfo(@UserIdInt int userHandle) {
        return sInstance.getUserInfo(userHandle);
    }

    @Resetter
    public static void reset() {
        sInstance = null;
    }
}
