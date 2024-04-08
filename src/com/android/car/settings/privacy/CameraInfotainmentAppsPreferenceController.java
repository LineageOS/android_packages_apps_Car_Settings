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
import android.annotation.FlaggedApi;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.SensorPrivacyManager;
import android.os.Process;
import android.os.UserHandle;
import android.util.IconDrawableFactory;

import com.android.car.settings.R;
import com.android.car.settings.common.CameraPrivacyBasePreferenceController;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.ui.preference.CarUiPreference;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.camera.flags.Flags;

import java.util.List;

/**
 * Displays a list of infotainment apps which holds camera permission.
 */
@FlaggedApi(Flags.FLAG_CAMERA_PRIVACY_ALLOWLIST)
public final class CameraInfotainmentAppsPreferenceController
        extends CameraPrivacyBasePreferenceController<LogicalPreferenceGroup> {
    private static final Logger LOG = new Logger(CameraInfotainmentAppsPreferenceController.class);
    private final PackageManager mPackageManager;
    private final Context mContext;
    private List<String> mCameraPrivacyAllowlist;

    public CameraInfotainmentAppsPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        this(context, preferenceKey, fragmentController, uxRestrictions,
                context.getPackageManager(), SensorPrivacyManager.getInstance(context));
    }

    @VisibleForTesting
    CameraInfotainmentAppsPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions,
            PackageManager packageManager, SensorPrivacyManager sensorPrivacyManager) {
        super(context, preferenceKey, fragmentController, uxRestrictions, sensorPrivacyManager);
        mPackageManager = packageManager;
        mCameraPrivacyAllowlist = sensorPrivacyManager.getCameraPrivacyAllowlist();
        mContext = context;
    }

    @Override
    protected Class<LogicalPreferenceGroup> getPreferenceType() {
        return LogicalPreferenceGroup.class;
    }

    @Override
    protected void updateState(LogicalPreferenceGroup preference) {
        loadInfotainmentAppsWithCameraPermission();
    }

    private void loadInfotainmentAppsWithCameraPermission() {
        LogicalPreferenceGroup infotainmentAppsPref = getPreference().findPreference(mContext
                .getString(R.string.pk_camera_infotainment_apps_list));
        infotainmentAppsPref.removeAll();

        UserHandle userHandle = Process.myUserHandle();
        int userId = userHandle.getIdentifier();
        List<PackageInfo> packagesWithPermissions = PermissionUtils.getPackagesWithPermission(
                mContext, PermissionUtils.CAMERA, userHandle, false);

        for (PackageInfo packageInfo : packagesWithPermissions) {
            if (mCameraPrivacyAllowlist.contains(packageInfo.packageName)) {
                continue;
            }

            ApplicationInfo appInfo = null;
            try {
                appInfo = mPackageManager.getApplicationInfoAsUser(packageInfo.packageName,
                        PackageManager.GET_META_DATA, userId);
            } catch (NameNotFoundException ex) {
                LOG.e("Failed to get application info for " + packageInfo.packageName);
            }

            if (appInfo != null) {
                if (!appInfo.enabled) {
                    continue;
                }
                CarUiPreference pref = new CarUiPreference(mContext);
                IconDrawableFactory drawableFactory = IconDrawableFactory.newInstance(mContext);
                pref.setIcon(drawableFactory.getBadgedIcon(appInfo, userId));

                CharSequence appLabel = mPackageManager.getApplicationLabel(appInfo);
                CharSequence badgedAppLabel = mPackageManager.getUserBadgedLabel(appLabel,
                        userHandle);
                pref.setTitle(badgedAppLabel);
                int state = getSensorPrivacyManager().getSensorPrivacyState(
                        SensorPrivacyManager.TOGGLE_TYPE_SOFTWARE,
                        SensorPrivacyManager.Sensors.CAMERA);
                Integer permGrantStatus = PermissionUtils.getPermissionGrantStatus(mContext,
                        packageInfo, PermissionUtils.CAMERA, userHandle);
                if (state == SensorPrivacyManager.StateTypes.DISABLED) {
                    pref.setSummary(permGrantStatus);
                } else {
                    pref.setSummary(null);
                }
                pref.setOnPreferenceClickListener(
                        preference -> {
                            Intent intent = new Intent(Intent.ACTION_MANAGE_APP_PERMISSION);
                            intent.putExtra(Intent.EXTRA_PERMISSION_GROUP_NAME,
                                    Manifest.permission_group.CAMERA);
                            intent.putExtra(Intent.EXTRA_PACKAGE_NAME, packageInfo.packageName);
                            intent.putExtra(Intent.EXTRA_USER, userHandle);
                            mContext.startActivity(intent);
                            return true;
                        });
                infotainmentAppsPref.addPreference(pref);
            }
        }
    }
}
