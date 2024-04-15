/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.car.settings.applications.specialaccess;

import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.SparseArray;

import androidx.annotation.VisibleForTesting;

import com.android.car.settings.common.Logger;
import com.android.internal.util.ArrayUtils;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bridges {@link AppOpsManager} app operation permission information into {@link
 * AppEntry#extraInfo} as {@link PermissionState} objects.
 */
public class AppStateAppOpsBridge implements AppEntryListManager.ExtraInfoBridge {

    private static final Logger LOG = new Logger(AppStateAppOpsBridge.class);

    private final Context mContext;
    private final IPackageManager mIPackageManager;
    private final List<UserHandle> mProfiles;
    private final AppOpsManager mAppOpsManager;
    private final int mAppOpsOpCode;
    private final String mPermission;

    /**
     * Constructor.
     *
     * @param appOpsOpCode the {@link AppOpsManager} op code constant to fetch information for.
     * @param permission   the {@link android.Manifest.permission} required to perform the
     *                     operation.
     */
    public AppStateAppOpsBridge(Context context, int appOpsOpCode, String permission) {
        this(context, appOpsOpCode, permission, AppGlobals.getPackageManager(),
                UserManager.get(context).getUserProfiles(),
                context.getSystemService(AppOpsManager.class));
    }

    @VisibleForTesting
    AppStateAppOpsBridge(Context context, int appOpsOpCode, String permission,
            IPackageManager packageManager, List<UserHandle> profiles,
            AppOpsManager appOpsManager) {
        mContext = context;
        mIPackageManager = packageManager;
        mProfiles = profiles;
        mAppOpsManager = appOpsManager;
        mAppOpsOpCode = appOpsOpCode;
        mPermission = permission;
    }

    @Override
    public void loadExtraInfo(List<AppEntry> entries) {
        SparseArray<Map<String, PermissionState>> packageToStatesMapByProfileId =
                getPackageToStateMapsByProfileId();
        loadAppOpModes(packageToStatesMapByProfileId);

        for (AppEntry entry : entries) {
            Map<String, PermissionState> packageStatesMap = packageToStatesMapByProfileId.get(
                    UserHandle.getUserId(entry.info.uid));
            entry.extraInfo = (packageStatesMap != null) ? packageStatesMap.get(
                    entry.info.packageName) : null;
        }
    }

    private SparseArray<Map<String, PermissionState>> getPackageToStateMapsByProfileId() {
        SparseArray<Map<String, PermissionState>> entries = new SparseArray<>();
        try {
            for (UserHandle profile : mProfiles) {
                int profileId = profile.getIdentifier();
                List<PackageInfo> packageInfos = getPackageInfos(profileId);
                Map<String, PermissionState> entriesForProfile = new ArrayMap<>();
                entries.put(profileId, entriesForProfile);
                for (PackageInfo packageInfo : packageInfos) {
                    boolean isAvailable = mIPackageManager.isPackageAvailable(
                            packageInfo.packageName,
                            profileId);
                    if (shouldIgnorePackage(packageInfo) || !isAvailable) {
                        LOG.d("Ignoring " + packageInfo.packageName + " isAvailable="
                                + isAvailable);
                        continue;
                    }
                    PermissionState newEntry = getPermissionState(packageInfo);
                    newEntry.mRequestedPermissions = packageInfo.requestedPermissions;
                    entriesForProfile.put(packageInfo.packageName, newEntry);
                }
            }
        } catch (RemoteException e) {
            LOG.w("PackageManager is dead. Can't get list of packages requesting "
                    + mPermission, e);
        }
        return entries;
    }

    private PermissionState getPermissionState(PackageInfo packageInfo) {
        String[] requestedPermissions = packageInfo.requestedPermissions;
        int[] permissionFlags = packageInfo.requestedPermissionsFlags;
        if (requestedPermissions != null) {
            for (int i = 0; i < requestedPermissions.length; i++) {
                if (requestedPermissions[i].equals(mPermission) &&
                        (permissionFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                    return new PermissionState(/* isPermissionGranted */ true);
                }
            }
        }
        return new PermissionState(/* isPermissionGranted */ false);
    }

    @SuppressWarnings("unchecked") // safe by specification.
    private List<PackageInfo> getPackageInfos(int profileId) throws RemoteException {
        // apps that has requested, and currently hold the permission.
        List<PackageInfo> packageInfos =
                mIPackageManager.getPackagesHoldingPermissions(new String[]{mPermission},
                        PackageManager.GET_PERMISSIONS, profileId).getList();
        Set<String> packageNames = getPackageNames(packageInfos);
        // other apps may have defined the permission in Manifest, but do not currently hold the
        // permission. (i.e. it was previously revoked).
        String[] appOpPkgs = mIPackageManager.getAppOpPermissionPackages(mPermission, profileId);
        appOpPkgs = (appOpPkgs == null) ? new String[0] : appOpPkgs; // result is nullable
        for (String pkgName : appOpPkgs) {
            if (!packageNames.contains(pkgName)) {
                PackageInfo pkgInfo = mIPackageManager.getPackageInfo(pkgName,
                        PackageManager.GET_PERMISSIONS, profileId);
                packageInfos.add(pkgInfo);
            }
        }
        return packageInfos;
    }

    private Set<String> getPackageNames(List<PackageInfo> packageInfos) {
        Set<String> pkgNames = new HashSet<>();
        for (PackageInfo info : packageInfos) {
            pkgNames.add(info.packageName);
        }
        return pkgNames;
    }

    private boolean shouldIgnorePackage(PackageInfo packageInfo) {
        return packageInfo.packageName.equals("android")
                || packageInfo.packageName.equals(mContext.getPackageName())
                || !ArrayUtils.contains(packageInfo.requestedPermissions, mPermission);
    }

    /** Sets the {@link PermissionState#mAppOpMode} field. */
    private void loadAppOpModes(
            SparseArray<Map<String, PermissionState>> packageToStateMapsByProfileId) {
        // Find out which packages have been granted permission from AppOps.
        List<AppOpsManager.PackageOps> packageOps = mAppOpsManager.getPackagesForOps(
                new int[]{mAppOpsOpCode});
        if (packageOps == null) {
            return;
        }
        for (AppOpsManager.PackageOps packageOp : packageOps) {
            int userId = UserHandle.getUserId(packageOp.getUid());
            Map<String, PermissionState> packageStateMap = packageToStateMapsByProfileId.get(
                    userId);
            if (packageStateMap == null) {
                // Profile is not for the current user.
                continue;
            }
            PermissionState permissionState = packageStateMap.get(packageOp.getPackageName());
            if (permissionState == null) {
                LOG.w("AppOp permission exists for package " + packageOp.getPackageName()
                        + " of user " + userId + " but package doesn't exist or did not request "
                        + mPermission + " access");
                continue;
            }
            if (packageOp.getOps().size() < 1) {
                LOG.w("No AppOps permission exists for package " + packageOp.getPackageName());
                continue;
            }
            permissionState.mAppOpMode = packageOp.getOps().get(0).getMode();
        }
    }

    /**
     * Data class for use in {@link AppEntry#extraInfo} which indicates whether
     * the app operation used to construct the data bridge is permitted for the associated
     * application.
     */
    public static class PermissionState {
        private String[] mRequestedPermissions;
        private boolean mIsPermissionGranted;
        private int mAppOpMode = AppOpsManager.MODE_DEFAULT;

        public PermissionState(boolean isPermissionGranted) {
            mIsPermissionGranted = isPermissionGranted;
        }

        /** Returns {@code true} if the entry's application is allowed to perform the operation. */
        public boolean isPermissible() {
            // Default behavior is permissible as long as the package requested the permission
            if (mAppOpMode == AppOpsManager.MODE_DEFAULT) {
                return mIsPermissionGranted;
            }
            return mAppOpMode == AppOpsManager.MODE_ALLOWED;
        }

        /** Returns the permissions requested by the entry's application. */
        public String[] getRequestedPermissions() {
            return mRequestedPermissions;
        }
    }
}
