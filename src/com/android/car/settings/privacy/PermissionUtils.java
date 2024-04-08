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

import android.annotation.FlaggedApi;
import android.app.ActivityManager;
import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ParceledListSlice;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.os.RemoteException;
import android.os.UserHandle;

import com.android.car.settings.R;
import com.android.car.settings.common.Logger;
import com.android.internal.camera.flags.Flags;
import com.android.net.module.util.CollectionUtils;

import java.util.List;
import java.util.ListIterator;

/**
 * Utility class for permission related functions.
 */
@FlaggedApi(Flags.FLAG_CAMERA_PRIVACY_ALLOWLIST)
public final class PermissionUtils {
    public static final String CAMERA = "android.permission.CAMERA";
    public static final String MICROPHONE = "android.permission.RECORD_AUDIO";
    private static final Logger LOG = new Logger(PermissionUtils.class);

    private PermissionUtils() {}

    private static class PermissionState {
        public int mPermissionFlags;
        public PermissionInfo mPermissionInfo;
        public boolean mGranted;

        PermissionState(int permissionFlags, PermissionInfo permInfo, boolean granted) {
            mPermissionFlags = permissionFlags;
            mPermissionInfo = permInfo;
            mGranted = granted;
        }
    }

    /**
     * Returns a list of the packages holding a specific permission.
     *
     * @param context current context.
     * @param permissionName permission name for filtering the packages.
     * @param userHandle current user handle.
     * @param showSystem whether to return system packages.
     *
     * @return List List of packages.
     */
    public static List<PackageInfo> getPackagesWithPermission(Context context,
            String permissionName, UserHandle userHandle, boolean showSystem) {
        List<PackageInfo> packages = null;
        try {
            ParceledListSlice list = AppGlobals.getPackageManager()
                    .getPackagesHoldingPermissions(new String[]{permissionName},
                    PackageManager.GET_PERMISSIONS, ActivityManager.getCurrentUser());
            packages = list.getList();
        } catch (RemoteException e) {
            LOG.e("Cannot reach packagemanager", e);
        }

        ListIterator<PackageInfo> iter = packages.listIterator();
        while (iter.hasNext()) {
            PackageInfo packageInfo = iter.next();
            PermissionState permState = getPermissionState(context, packageInfo, permissionName,
                    userHandle);
            if (permState == null) {
                iter.remove();
                continue;
            }

            boolean isUserSensitive = ((permState.mGranted && (permState.mPermissionFlags
                    & PackageManager.FLAG_PERMISSION_USER_SENSITIVE_WHEN_GRANTED) != 0)
                    || (!permState.mGranted && (permState.mPermissionFlags
                    & PackageManager.FLAG_PERMISSION_USER_SENSITIVE_WHEN_DENIED) != 0));
            // If an app is not user sensitive, then it is considered a system app,
            // and hidden in the UI by default.
            if (!showSystem && !isUserSensitive) {
                iter.remove();
                continue;
            }
        }

        return packages;
    }

    private static PermissionState getPermissionState(Context context, PackageInfo packageInfo,
            String permissionName, UserHandle userHandle) {
        int permissionFlags = context.getPackageManager().getPermissionFlags(permissionName,
                packageInfo.packageName, userHandle);
        String[] requestedPermissions = packageInfo.requestedPermissions;
        if (requestedPermissions == null) {
            return null;
        }
        int requestedPermissionsIndex = CollectionUtils.indexOf(requestedPermissions,
                permissionName);
        int[] requestedPermissionsFlags = packageInfo.requestedPermissionsFlags;
        PermissionInfo permInfo = null;
        try {
            permInfo = context.getPackageManager().getPermissionInfo(permissionName, 0);
        } catch (NameNotFoundException ex) {
            LOG.e("Failed to get application info for " + packageInfo.packageName);
            return null;
        }

        if (requestedPermissionsIndex == -1) {
            return null;
        }

        boolean hasPreRuntime = false;
        boolean hasInstantPerm = false;

        if ((permInfo.getProtectionFlags() & PermissionInfo.PROTECTION_FLAG_RUNTIME_ONLY) == 0) {
            hasPreRuntime = true;
        }

        if ((permInfo.getProtectionFlags() & PermissionInfo.PROTECTION_FLAG_INSTANT) != 0) {
            hasInstantPerm = true;
        }

        boolean supportsRuntime = packageInfo.applicationInfo.targetSdkVersion
                >= Build.VERSION_CODES.M;
        boolean isGrantingAllowed = (!packageInfo.applicationInfo.isInstantApp() || hasInstantPerm)
                && (supportsRuntime || hasPreRuntime);
        if (!isGrantingAllowed) {
            return null;
        }

        boolean granted = (((requestedPermissionsFlags[requestedPermissionsIndex]
                & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0)
                && ((permissionFlags & PackageManager.FLAG_PERMISSION_REVOKED_COMPAT) == 0));

        return new PermissionState(permissionFlags, permInfo, granted);
    }

     /**
     * Returns the permission grant status of a specific package.
     *
     * @param context current context.
     * @param packageInfo package for which to check the grant status
     * @param permissionName permission name for filtering the packages.
     * @param userHandle current user handle.
     *
     * @return Integer permission grant status.
     */
    public static Integer getPermissionGrantStatus(Context context, PackageInfo packageInfo,
            String permissionName, UserHandle userHandle) {
        PermissionState permState = getPermissionState(context, packageInfo, permissionName,
                userHandle);
        if (permState != null) {
            boolean isOneTime = ((permState.mPermissionFlags
                    & PackageManager.FLAG_PERMISSION_ONE_TIME) != 0);
            boolean isUserFixed = ((permState.mPermissionFlags
                    & PackageManager.FLAG_PERMISSION_USER_FIXED) != 0);
            boolean supportsRuntime = packageInfo.applicationInfo.targetSdkVersion
                     >= Build.VERSION_CODES.M;
            boolean isAllowed = permState.mGranted || (supportsRuntime
                    && (permState.mPermissionFlags
                    & PackageManager.FLAG_PERMISSION_REVIEW_REQUIRED) != 0);
            boolean hasPermWithBackground =
                    (permState.mPermissionInfo.backgroundPermission != null);
            boolean shouldShowAsForegroundGroup = permissionName.equals(CAMERA)
                    || permissionName.equals(MICROPHONE);

            if (isAllowed) {
                if (isOneTime) {
                    return R.string.permission_grant_ask;
                } else {
                    return (hasPermWithBackground || shouldShowAsForegroundGroup)
                            ? R.string.permission_grant_in_use : R.string.permission_grant_always;
                }
            }
            if (isUserFixed) {
                return R.string.permission_grant_never;
            }
            if (isOneTime) {
                return R.string.permission_grant_ask;
            }
        }
        return R.string.permission_grant_never;
    }
}
