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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionInfo;
import android.location.LocationManager;
import android.os.Build;
import android.os.UserHandle;
import android.util.ArrayMap;

import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;

import com.android.car.settings.R;
import com.android.car.settings.common.Logger;
import com.android.net.module.util.CollectionUtils;

import com.google.common.collect.ImmutableListMultimap;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for applications permissions. This logic is extracted from {@link
 * com.android.permissioncontroller.permission.data.AppPermGroupUiInfoLiveData}.
 */
public final class PermissionUtils {
    private static final Logger LOG = new Logger(PermissionUtils.class);
    private static final ImmutableListMultimap<String, String> PERMISSION_GROUP_TO_PERMISSIONS =
            ImmutableListMultimap.<String, String>builder()
                    .putAll(Manifest.permission_group.CAMERA,
                            Manifest.permission.CAMERA)
                    .putAll(Manifest.permission_group.LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    .putAll(Manifest.permission_group.MICROPHONE,
                            Manifest.permission.RECORD_AUDIO)
                    .build();

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

    private static final Object sLock = new Object();
    private static final ArrayMap<UserHandle, Context> sUserContexts = new ArrayMap<>();

    /**
     * TODO(b/339236027): Remove extra logic in Android W
     *
     * Creates and caches a PackageContext for the requested user, or returns the previously cached
     * value. The package of the PackageContext is the application's package.
     *
     * Note: All this logic is directly from PermissionController. Ideally, we would've exposed a
     * SystemApi to be used in CarSettings instead, but the API deadline passed. This logic is used
     * below in {@link #getSpecialLocationState(Context, String, String, UserHandle)}. It appears
     * there's a scenario where the locationManager from user context and app context may not align.
     *
     * @param context The context of the currently running application
     * @param user The desired user for the context
     *
     * @return The generated or cached Context for the requested user
     *
     * @throws RuntimeException If the app has no package name attached, which should never happen
     */
    private static Context getUserContext(Context context, UserHandle user) {
        synchronized (sLock) {
            if (!sUserContexts.containsKey(user)) {
                sUserContexts.put(user, context.getApplicationContext()
                        .createContextAsUser(user, 0));
            }
            return Preconditions.checkNotNull(sUserContexts.get(user));
        }
    }

    /**
     * Returns a list of the packages holding a permission from the specified permission group.
     *
     * @param context current context.
     * @param permissionGroup permission group for filtering the packages.
     * @param userHandle current user handle.
     * @param showSystem whether to return system packages.
     *
     * @return List of packages.
     */
    public static List<PackageInfo> getPackagesWithPermissionGroup(Context context,
            String permissionGroup, UserHandle userHandle, boolean showSystem) {
        ArrayMap<String, PackageInfo> packageNameToInfo = new ArrayMap<>();
        List<String> permissionNames = PERMISSION_GROUP_TO_PERMISSIONS.get(permissionGroup);
        for (String permissionName: permissionNames) {
            List<PackageInfo> packages = context.getPackageManager().getPackagesHoldingPermissions(
                    new String[]{permissionName}, PackageManager.GET_PERMISSIONS);

            for (PackageInfo packageInfo : packages) {
                // Ignore duplicate packages
                if (packageNameToInfo.containsKey(packageInfo.packageName)) {
                    continue;
                }
                PermissionState permState =
                        getPermissionState(context, packageInfo, permissionName, userHandle);
                if (permState == null) {
                    continue;
                }

                boolean isUserSensitive = ((permState.mGranted && (permState.mPermissionFlags
                        & PackageManager.FLAG_PERMISSION_USER_SENSITIVE_WHEN_GRANTED) != 0)
                        || (!permState.mGranted && (permState.mPermissionFlags
                        & PackageManager.FLAG_PERMISSION_USER_SENSITIVE_WHEN_DENIED) != 0));
                // If an app is not user sensitive, then it is considered a system app,
                // and hidden in the UI by default.
                if (!showSystem && !isUserSensitive) {
                    continue;
                }
                packageNameToInfo.put(packageInfo.packageName, packageInfo);
            }
        }
        return new ArrayList<>(packageNameToInfo.values());
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
     * Returns the grant status of all permissions in a group for a specific package.
     *
     * @param context current context.
     * @param packageInfo package for which to check the grant status
     * @param permissionGroup permission group for filtering the packages.
     * @param userHandle current user handle.
     *
     * @return resource for permission grant status.
     */
    public static int getPermissionGroupGrantStatus(Context context, PackageInfo packageInfo,
            String permissionGroup, UserHandle userHandle) {
        ArrayMap<String, PermissionState> permissionNameToState = new ArrayMap<>();
        List<String> permissionNames = PERMISSION_GROUP_TO_PERMISSIONS.get(permissionGroup);
        for (String permissionName : permissionNames) {
            PermissionState permState =
                    getPermissionState(context, packageInfo, permissionName, userHandle);
            if (permState != null) {
                permissionNameToState.put(permissionName, permState);
            }
        }

        Boolean specialLocationState = getSpecialLocationState(context, packageInfo.packageName,
                permissionGroup, userHandle);
        boolean supportsRuntime = packageInfo.applicationInfo.targetSdkVersion
                >= Build.VERSION_CODES.M;
        boolean hasPermWithBackground = false;
        boolean anyAllowed = false;
        boolean isUserFixed = false;
        boolean containsOneTimePerm = false;
        boolean containsGrantedNonOneTimePerm = false;
        for (int i = 0; i < permissionNameToState.size(); i++) {
            PermissionState permState = permissionNameToState.valueAt(i);
            if ((permState.mPermissionInfo.backgroundPermission != null)) {
                hasPermWithBackground = true;
                PermissionState backgroundPermState = permissionNameToState.get(
                        permState.mPermissionInfo.backgroundPermission);
                if (backgroundPermState != null && backgroundPermState.mGranted
                        && (backgroundPermState.mPermissionFlags
                                & PackageManager.FLAG_PERMISSION_ONE_TIME) == 0
                        && !Boolean.FALSE.equals(specialLocationState)) {
                    return R.string.permission_grant_always;
                }
            }

            if ((permState.mPermissionFlags & PackageManager.FLAG_PERMISSION_ONE_TIME) != 0) {
                containsOneTimePerm = true;
            }

            if ((permState.mPermissionFlags & PackageManager.FLAG_PERMISSION_ONE_TIME) == 0
                    && permState.mGranted) {
                containsGrantedNonOneTimePerm = true;
            }

            if (specialLocationState != null) {
                anyAllowed = specialLocationState;
            } else if (permState.mGranted
                    || (supportsRuntime
                            && (permState.mPermissionFlags
                                    & PackageManager.FLAG_PERMISSION_REVIEW_REQUIRED) != 0)) {
                anyAllowed = true;
            }

            isUserFixed = isUserFixed
                    || (permState.mPermissionFlags
                            & PackageManager.FLAG_PERMISSION_USER_FIXED) != 0;
        }

        // isOneTime indicates whether all granted permissions in permission states are one-time
        // permissions
        boolean isOneTime = containsOneTimePerm && !containsGrantedNonOneTimePerm;
        boolean shouldShowAsForegroundGroup =
                Manifest.permission_group.CAMERA.equals(permissionGroup)
                || Manifest.permission_group.MICROPHONE.equals(permissionGroup);

        if (anyAllowed) {
            if (isOneTime) {
                return R.string.permission_grant_ask;
            } else if (hasPermWithBackground || shouldShowAsForegroundGroup) {
                return R.string.permission_grant_in_use;
            } else {
                return R.string.permission_grant_allowed;
            }
        }
        if (isUserFixed) {
            return R.string.permission_grant_never;
        }
        if (isOneTime) {
            return R.string.permission_grant_ask;
        }
        return R.string.permission_grant_never;
    }

    @Nullable
    private static Boolean getSpecialLocationState(Context appContext, String packageName,
            String permissionGroup, UserHandle userHandle) {
        if (!Manifest.permission_group.LOCATION.equals(permissionGroup)) {
            return null;
        }

        LocationManager appLocationManager = appContext.getSystemService(LocationManager.class);
        if (!appLocationManager.isProviderPackage(packageName)
                && !packageName.equals(appLocationManager.getExtraLocationControllerPackage())) {
            return null;
        }

        Context userContext = getUserContext(appContext, userHandle);
        LocationManager userLocationManager = userContext.getSystemService(LocationManager.class);
        if (userLocationManager.isProviderPackage(packageName)) {
            return userLocationManager.isLocationEnabled();
        }

        // The permission of the extra location controller package is determined by the
        // status of the controller package itself.
        if (packageName.equals(userLocationManager.getExtraLocationControllerPackage())) {
            try {
                return userLocationManager.isExtraLocationControllerPackageEnabled();
            } catch (Exception e) {
                return false;
            }
        }

        return null;
    }
}
