/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.car.settings.privacy;

import static com.android.car.ui.preference.CarUiTwoActionTextPreference.SECONDARY_ACTION_STYLE_BORDERLESS;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManager.PackageInfoFlags;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.IconDrawableFactory;

import androidx.annotation.Nullable;

import com.android.car.settings.R;
import com.android.car.settings.common.Logger;
import com.android.car.ui.preference.CarUiPreference;
import com.android.car.ui.preference.CarUiTwoActionTextPreference;

/** Utilities related to preferences with required and infotainment apps. */
public final class RequiredInfotainmentAppsUtils {
    private static final Logger LOG = new Logger(RequiredInfotainmentAppsUtils.class);
    private static final String PRIVACY_POLICY_KEY = "privacy_policy";

    private RequiredInfotainmentAppsUtils() {}

    /**
     * Creates a {@link CarUiTwoActionTextPreference} for a required app with its privacy policy and
     * a link to its permission settings.
     */
    @Nullable
    public static CarUiTwoActionTextPreference createRequiredAppPreference(
            Context context, PackageManager packageManager, String pkgName, UserHandle userHandle,
            String permissionGroup, boolean showSummary) {
        ApplicationInfo appInfo = getApplicationInfo(packageManager, pkgName, userHandle);
        if (appInfo == null || !appInfo.enabled) {
            return null;
        }

        CarUiTwoActionTextPreference pref =
                new CarUiTwoActionTextPreference(context, SECONDARY_ACTION_STYLE_BORDERLESS);
        setAppPreference(context, packageManager, pkgName, userHandle, permissionGroup, showSummary,
                pref, appInfo);

        pref.setSecondaryActionText(R.string.required_apps_privacy_policy_button_text);

        Bundle bundle = appInfo.metaData;
        if (bundle == null) {
            LOG.e(pkgName + "doesn't provide meta data in manifest");
            return pref;
        }

        CharSequence privacyPolicyLink = bundle.getCharSequence(PRIVACY_POLICY_KEY);
        if (privacyPolicyLink == null) {
            LOG.e(pkgName + " doesn't provide privacy policy");
            return pref;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyLink.toString()));
        pref.setOnSecondaryActionClickListener(
                () -> {
                    context.startActivity(intent);
                });
        return pref;
    }

    /**
     * Creates a {@link CarUiPreference} for an infotainment app.
     */
    @Nullable
    public static CarUiPreference createInfotainmentAppPreference(
            Context context, PackageManager packageManager, String pkgName, UserHandle userHandle,
            String permissionGroup, boolean showSummary) {
        ApplicationInfo appInfo = getApplicationInfo(packageManager, pkgName, userHandle);
        if (appInfo == null || !appInfo.enabled) {
            return null;
        }

        CarUiPreference pref = new CarUiPreference(context);
        setAppPreference(context, packageManager, pkgName, userHandle, permissionGroup, showSummary,
                pref, appInfo);
        return pref;
    }

    private static ApplicationInfo getApplicationInfo(
            PackageManager packageManager, String pkgName, UserHandle userHandle) {
        ApplicationInfo appInfo = null;
        try {
            appInfo = packageManager.getApplicationInfoAsUser(
                    pkgName, PackageManager.GET_META_DATA, userHandle.getIdentifier());
        } catch (NameNotFoundException ex) {
            LOG.e("Failed to get application info for " + pkgName);
        }
        return appInfo;
    }

    private static void setAppPreference(
            Context context, PackageManager packageManager, String pkgName, UserHandle userHandle,
            String permissionGroup, boolean showSummary, CarUiPreference pref,
            ApplicationInfo appInfo) {
        IconDrawableFactory drawableFactory = IconDrawableFactory.newInstance(context);
        pref.setIcon(drawableFactory.getBadgedIcon(appInfo, userHandle.getIdentifier()));

        CharSequence appLabel = packageManager.getApplicationLabel(appInfo);
        CharSequence badgedAppLabel = packageManager.getUserBadgedLabel(appLabel, userHandle);
        pref.setTitle(badgedAppLabel);

        pref.setOnPreferenceClickListener(
                preference -> {
                    Intent intent = new Intent(Intent.ACTION_MANAGE_APP_PERMISSION);
                    intent.putExtra(Intent.EXTRA_PERMISSION_GROUP_NAME, permissionGroup);
                    intent.putExtra(Intent.EXTRA_PACKAGE_NAME, pkgName);
                    intent.putExtra(Intent.EXTRA_USER, userHandle);
                    context.startActivity(intent);
                    return true;
                });

        if (showSummary) {
            PackageInfo packageInfo;
            try {
                packageInfo = packageManager.getPackageInfo(pkgName,
                        PackageInfoFlags.of(PackageManager.GET_PERMISSIONS));
            } catch (NameNotFoundException ex) {
                LOG.e("Failed to get package info for " + pkgName);
                return;
            }
            int permGroupGrantStatus = PermissionUtils.getPermissionGroupGrantStatus(
                    context, packageInfo, permissionGroup, userHandle);
            pref.setSummary(permGroupGrantStatus);
        }
    }
}
