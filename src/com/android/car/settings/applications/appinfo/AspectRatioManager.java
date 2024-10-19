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

package com.android.car.settings.applications.appinfo;

import static android.os.UserHandle.getUserHandleForUid;
import static android.view.WindowManager.PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_OVERRIDE;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.DeviceConfig;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.car.settings.common.Logger;

/**
 * Manager for handling app aspect ratio override behavior
 */
public final class AspectRatioManager {
    private static final Logger LOG = new Logger(AspectRatioManager.class);
    private static final boolean DEFAULT_VALUE_ENABLE_USER_ASPECT_RATIO_SETTINGS = true;
    private static final String KEY_ENABLE_USER_ASPECT_RATIO_SETTINGS =
            "enable_app_compat_aspect_ratio_user_settings";

    private final Context mContext;

    public AspectRatioManager(Context context) {
        mContext = context;
    }

    /** Determines whether an app should have aspect ratio settings */
    public boolean shouldShowAspectRatioSettingsForApp(ApplicationInfo appInfo) {
        if (appInfo == null) {
            return false;
        }

        // This class is only intended to be used for overrides testing for now b/372710124
        if (!(Build.IS_USERDEBUG || Build.IS_ENG)) {
            return false;
        }

        return isFeatureEnabled() && canDisplayAspectRatioUi(appInfo);
    }

    /**
     * Whether user aspect ratio settings is enabled for device.
     */
    private boolean isFeatureEnabled() {
        final boolean isBuildTimeFlagEnabled = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_appCompatUserAppAspectRatioSettingsIsEnabled);
        return getValueFromDeviceConfig(KEY_ENABLE_USER_ASPECT_RATIO_SETTINGS,
                DEFAULT_VALUE_ENABLE_USER_ASPECT_RATIO_SETTINGS) && isBuildTimeFlagEnabled;
    }

    private boolean getValueFromDeviceConfig(String name, boolean defaultValue) {
        return DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_WINDOW_MANAGER, name, defaultValue);
    }

    /**
     * Whether an app's aspect ratio can be overridden by user. Only apps with launcher entry
     * will be overridable.
     */
    private boolean canDisplayAspectRatioUi(@NonNull ApplicationInfo appInfo) {
        Boolean appAllowsUserAspectRatioOverride = readComponentProperty(
                mContext.getPackageManager(), appInfo.packageName,
                PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_OVERRIDE);
        return !(Boolean.FALSE.equals(appAllowsUserAspectRatioOverride))
                && hasLauncherEntry(appInfo);
    }

    private boolean hasLauncherEntry(@NonNull ApplicationInfo app) {
        LauncherApps launcherApps = mContext.getSystemService(LauncherApps.class);
        if (launcherApps == null) {
            return false;
        }

        return !launcherApps
                .getActivityList(app.packageName, getUserHandleForUid(app.uid))
                .isEmpty();
    }

    @Nullable
    private static Boolean readComponentProperty(PackageManager pm, String packageName,
            String propertyName) {
        try {
            return pm.getProperty(propertyName, packageName).getBoolean();
        } catch (PackageManager.NameNotFoundException e) {
            LOG.e("Error getting property " + propertyName + " for package: " + packageName + " "
                    + e);
        }
        return null;
    }
}
