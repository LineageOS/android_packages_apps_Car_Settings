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
import android.provider.Settings;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowSettings;
import org.robolectric.util.ReflectionHelpers;

@Implements(Settings.Secure.class)
public class ShadowSecureSettings extends ShadowSettings.ShadowSecure {

    @Implementation
    protected static int getIntForUser(ContentResolver resolver, String name, int def,
            int userHandle) {
        if (Settings.Secure.LOCATION_MODE.equals(name)) {
            // Map from to underlying location provider storage API to location mode
            return Shadow.directlyOn(
                    Settings.Secure.class,
                    "getLocationModeForUser",
                    ReflectionHelpers.ClassParameter.from(ContentResolver.class, resolver),
                    ReflectionHelpers.ClassParameter.from(int.class, userHandle));
        }

        return Shadow.directlyOn(
                Settings.Secure.class,
                "getIntForUser",
                ReflectionHelpers.ClassParameter.from(ContentResolver.class, resolver),
                ReflectionHelpers.ClassParameter.from(String.class, name),
                ReflectionHelpers.ClassParameter.from(int.class, def),
                ReflectionHelpers.ClassParameter.from(int.class, userHandle));
    }

    @Implementation
    protected static boolean putIntForUser(ContentResolver resolver, String name, int value,
            int userHandle) {
        if (Settings.Secure.LOCATION_MODE.equals(name)) {
            // Map LOCATION_MODE to underlying location provider storage API
            return Shadow.directlyOn(
                    Settings.Secure.class,
                    "setLocationModeForUser",
                    ReflectionHelpers.ClassParameter.from(ContentResolver.class, resolver),
                    ReflectionHelpers.ClassParameter.from(int.class, value),
                    ReflectionHelpers.ClassParameter.from(int.class, userHandle));
        }
        return Shadow.directlyOn(
                Settings.Secure.class,
                "putIntForUser",
                ReflectionHelpers.ClassParameter.from(ContentResolver.class, resolver),
                ReflectionHelpers.ClassParameter.from(String.class, name),
                ReflectionHelpers.ClassParameter.from(int.class, value),
                ReflectionHelpers.ClassParameter.from(int.class, userHandle));
    }
}
