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

import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.UserManager;
import android.provider.Settings;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * A utility to set/check development settings mode.
 *
 * <p>Shared logic with {@link com.android.settingslib.development.DevelopmentSettingsEnabler} with
 * modifications to use CarUserManagerHelper instead of UserManager.
 */
public class DevelopmentSettingsUtil {

    /**
     * Local broadcast action that can be used to know when the development settings have been
     * enabled or disabled.
     */
    public static final String DEVELOPMENT_SETTINGS_CHANGED_ACTION =
            "com.android.car.settings.development.DevelopmentSettingsUtil.SETTINGS_CHANGED";

    private DevelopmentSettingsUtil() {
    }

    /**
     * Sets the global toggle for developer settings and sends out a local broadcast to notify other
     * of this change.
     */
    public static void setDevelopmentSettingsEnabled(Context context, boolean enable) {
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, enable ? 1 : 0);
        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(new Intent(DEVELOPMENT_SETTINGS_CHANGED_ACTION));
    }

    /**
     * Checks that the development settings should be enabled. Returns true if global toggle is set,
     * debugging is allowed for user, and the user is an admin or a demo user.
     */
    public static boolean isDevelopmentSettingsEnabled(Context context,
            CarUserManagerHelper carUserManagerHelper) {
        boolean settingEnabled = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, Build.IS_ENG ? 1 : 0) != 0;
        boolean hasRestriction = carUserManagerHelper.hasUserRestriction(
                UserManager.DISALLOW_DEBUGGING_FEATURES,
                carUserManagerHelper.getCurrentProcessUserInfo());
        boolean isAdminOrDemo = carUserManagerHelper.isCurrentProcessAdminUser()
                || carUserManagerHelper.isCurrentProcessDemoUser();
        return isAdminOrDemo && !hasRestriction && settingEnabled;
    }

    /** Checks whether the device is provisioned or not. */
    public static boolean isDeviceProvisioned(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0) != 0;
    }
}
