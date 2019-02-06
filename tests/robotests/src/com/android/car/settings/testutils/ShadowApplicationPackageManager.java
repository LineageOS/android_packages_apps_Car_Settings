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

import android.annotation.NonNull;
import android.annotation.UserIdInt;
import android.app.ApplicationPackageManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.Collections;
import java.util.List;

/** Shadow of ApplicationPackageManager that allows the getting of content providers per user. */
@Implements(value = ApplicationPackageManager.class)
public class ShadowApplicationPackageManager extends
        org.robolectric.shadows.ShadowApplicationPackageManager {

    private static List<ResolveInfo> sResolveInfos = null;
    private static Resources sResources = null;

    private List<ResolveInfo> mHomeActivities = Collections.emptyList();
    private ComponentName mDefaultHomeActivity;

    @Resetter
    public static void reset() {
        sResolveInfos = null;
        sResources = null;
    }

    @Implementation
    @NonNull
    protected List<ModuleInfo> getInstalledModules(@PackageManager.ModuleInfoFlags int flags) {
        return Collections.emptyList();
    }

    @Implementation
    protected Drawable getUserBadgedIcon(Drawable icon, UserHandle user) {
        return icon;
    }

    @Override
    @Implementation
    protected ProviderInfo resolveContentProviderAsUser(String name, int flags,
            @UserIdInt int userId) {
        return resolveContentProvider(name, flags);
    }

    @Implementation
    protected int getPackageUidAsUser(String packageName, int flags, int userId)
            throws PackageManager.NameNotFoundException {
        return 0;
    }

    @Implementation
    protected Resources getResourcesForApplication(String appPackageName)
            throws PackageManager.NameNotFoundException {
        return sResources;
    }

    @Implementation
    protected List<ResolveInfo> queryIntentActivitiesAsUser(Intent intent,
            @PackageManager.ResolveInfoFlags int flags, @UserIdInt int userId) {
        return sResolveInfos == null ? Collections.emptyList() : sResolveInfos;
    }

    @Implementation
    @Override
    protected ComponentName getHomeActivities(List<ResolveInfo> outActivities) {
        outActivities.addAll(mHomeActivities);
        return mDefaultHomeActivity;
    }

    public void setHomeActivities(List<ResolveInfo> homeActivities) {
        mHomeActivities = homeActivities;
    }

    public void setDefaultHomeActivity(ComponentName defaultHomeActivity) {
        mDefaultHomeActivity = defaultHomeActivity;
    }

    /**
     * If resolveInfos are set by this method then
     * {@link ShadowApplicationPackageManager#queryIntentActivitiesAsUser}
     * method will return the same list.
     */
    public static void setListOfActivities(List<ResolveInfo> resolveInfos) {
        sResolveInfos = resolveInfos;
    }

    public static void setResources(Resources resources) {
        sResources = resources;
    }
}
