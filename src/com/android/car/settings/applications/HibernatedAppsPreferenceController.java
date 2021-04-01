/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.car.settings.applications;

import static com.android.car.settings.applications.ApplicationsUtils.isHibernationEnabled;

import android.apphibernation.AppHibernationManager;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.preference.Preference;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;

import java.util.List;

/**
 * A preference controller handling the logic for updating summary of hibernated apps.
 */
public final class HibernatedAppsPreferenceController extends PreferenceController<Preference> {
    private static final String TAG = "HibernatedAppsPrefController";

    public HibernatedAppsPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController,
            CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<Preference> getPreferenceType() {
        return Preference.class;
    }

    @Override
    public int getAvailabilityStatus() {
        return isHibernationEnabled() && getNumHibernated() > 0
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    protected void updateState(Preference preference) {
        int numHibernated = getNumHibernated();
        preference.setSummary(getContext().getResources().getQuantityString(
                R.plurals.unused_apps_summary, numHibernated, numHibernated));
    }

    private int getNumHibernated() {
        PackageManager pm = getContext().getPackageManager();
        AppHibernationManager ahm =
                getContext().getSystemService(AppHibernationManager.class);
        List<String> hibernatedPackages = ahm.getHibernatingPackagesForUser();
        int numHibernated = hibernatedPackages.size();

        // Also need to count packages that are auto revoked but not hibernated.
        List<PackageInfo> packages = pm.getInstalledPackages(
                PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.GET_PERMISSIONS);
        for (PackageInfo pi : packages) {
            String packageName = pi.packageName;
            if (!hibernatedPackages.contains(packageName) && pi.requestedPermissions != null) {
                for (String perm : pi.requestedPermissions) {
                    if ((pm.getPermissionFlags(perm, packageName, getContext().getUser())
                            & PackageManager.FLAG_PERMISSION_AUTO_REVOKED) != 0) {
                        numHibernated++;
                        break;
                    }
                }
            }
        }
        return numHibernated;
    }
}
