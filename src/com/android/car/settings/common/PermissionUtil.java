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

package com.android.car.settings.common;

import android.content.pm.PackageManager;

/** Utility class for common permission behaviors. */
public class PermissionUtil {
    private static final Logger LOG = new Logger(PermissionUtil.class);
    private PermissionUtil() {}

    /** Checks if a given app requests a given permission */
    public static boolean doesPackageRequestPermission(String packageName,
            PackageManager packageManager, String permission) {
        try {
            String[] requestedPermissions = packageManager.getPackageInfo(
                    packageName, PackageManager.GET_PERMISSIONS)
                    .requestedPermissions;
            if (requestedPermissions != null) {
                for (String requestedPermission : requestedPermissions) {
                    if (permission.equals(requestedPermission)) {
                        return true;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            LOG.e("Unable to query app permissions for " + packageName + " " + e);
        }
        return false;
    }
}
