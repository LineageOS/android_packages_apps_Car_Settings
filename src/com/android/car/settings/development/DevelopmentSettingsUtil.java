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

package com.android.car.settings.development;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import com.android.car.settings.R;
import com.android.car.settings.common.Logger;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

/**
 * A utility to set/check development settings mode.
 *
 * <p>Shared logic with {@link com.android.settingslib.development.DevelopmentSettingsEnabler} with
 * modifications to use CarUserManagerHelper instead of UserManager.
 */
public class DevelopmentSettingsUtil {

    private static final Logger LOG = new Logger(DevelopmentSettingsUtil.class);
    private static final boolean DEBUG = Build.isDebuggable();
    private DevelopmentSettingsUtil() {
    }

    /**
     * Sets the global toggle for developer settings and sends out a local broadcast to notify other
     * of this change.
     */
    public static void setDevelopmentSettingsEnabled(Context context, boolean enable) {
        boolean shouldEnable = showDeveloperOptions(context) && enable;
        LOG.i("Enabling developer options module: "
                + getDeveloperOptionsModule(context).flattenToString()
                + " Currently enabled: " + isDevelopmentSettingsEnabled(context)
                + " Requested value: " + enable
                + " Should enable: " + shouldEnable);
        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(context, shouldEnable);
    }

    /**
     * Checks that the development settings should be enabled. Returns true if global toggle is set,
     * debugging is allowed for user, and the user is an admin user.
     */
    public static boolean isDevelopmentSettingsEnabled(Context context) {
        return DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context);
    }

    /** Checks whether the device is provisioned or not. */
    public static boolean isDeviceProvisioned(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0) != 0;
    }

    private static ComponentName getDeveloperOptionsModule(Context context) {
        return ComponentName.unflattenFromString(
                context.getString(R.string.config_dev_options_module));
    }

    private static boolean showDeveloperOptions(Context context) {
        UserManager userManager = UserManager.get(context);
        boolean showDev = !ActivityManager.isUserAMonkey();
        boolean isAdmin = userManager.isAdminUser();
        if (UserHandle.MU_ENABLED && !isAdmin) {
            showDev = false;
        }

        if (DEBUG) {
            LOG.d("showDeveloperOptions: " + " isUserAMonkey: " + ActivityManager.isUserAMonkey()
                    + " isAdmin: " + isAdmin + " UserHandle.MU_ENABLED: " + UserHandle.MU_ENABLED
                    + " showDev: " + showDev);
        }
        return showDev;
    }

}
