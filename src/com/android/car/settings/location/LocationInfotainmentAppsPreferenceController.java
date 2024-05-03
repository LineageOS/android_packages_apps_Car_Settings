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

package com.android.car.settings.location;

import android.Manifest;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.os.UserHandle;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.settings.privacy.PermissionUtils;
import com.android.car.settings.privacy.RequiredInfotainmentAppsUtils;
import com.android.car.ui.preference.CarUiPreference;

import java.util.Collection;
import java.util.List;

/**
 * Displays a list of location infotainment apps and a link to their location permission
 * settings.
 */
public final class LocationInfotainmentAppsPreferenceController extends
        LocationStateListenerBasePreferenceController<LogicalPreferenceGroup> {
    private final PackageManager mPackageManager;

    public LocationInfotainmentAppsPreferenceController(
            Context context,
            String preferenceKey,
            FragmentController fragmentController,
            CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mPackageManager = context.getPackageManager();
    }

    @Override
    protected Class<LogicalPreferenceGroup> getPreferenceType() {
        return LogicalPreferenceGroup.class;
    }

    @Override
    protected void onCreateInternal() {
        addDefaultMainLocationStateListener();
    }

    @Override
    protected void updateState(LogicalPreferenceGroup preference) {
        loadInfotainmentAppsWithLocationPermission();
    }

    private void loadInfotainmentAppsWithLocationPermission() {
        getPreference().removeAll();

        UserHandle userHandle = Process.myUserHandle();
        List<PackageInfo> packagesWithPermissions = PermissionUtils.getPackagesWithPermissionGroup(
                getContext(), Manifest.permission_group.LOCATION, userHandle,
                /* showSystem= */ false);

        Collection<String> locationAdasAllowlist =
                getLocationManager().getAdasAllowlist().getPackages();
        boolean showSummary = getLocationManager().isLocationEnabled();
        for (PackageInfo packageInfo : packagesWithPermissions) {
            if (locationAdasAllowlist.contains(packageInfo.packageName)) {
                continue;
            }
            CarUiPreference preference =
                    RequiredInfotainmentAppsUtils.createInfotainmentAppPreference(
                            getContext(), mPackageManager, packageInfo.packageName, userHandle,
                            Manifest.permission_group.LOCATION, showSummary);
            if (preference != null) {
                getPreference().addPreference(preference);
            }
        }
    }
}
