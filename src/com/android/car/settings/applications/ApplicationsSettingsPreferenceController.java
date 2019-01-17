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

package com.android.car.settings.applications;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Business logic which populates the applications in this setting. */
public class ApplicationsSettingsPreferenceController extends
        PreferenceController<PreferenceGroup> {

    private final PackageManager mPackageManager;
    private List<Preference> mApplications;

    public ApplicationsSettingsPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mPackageManager = context.getPackageManager();
    }

    @Override
    protected Class<PreferenceGroup> getPreferenceType() {
        return PreferenceGroup.class;
    }

    @Override
    protected void updateState(PreferenceGroup preferenceGroup) {
        if (mApplications == null) {
            populateApplicationList();
        }
        for (Preference application : mApplications) {
            preferenceGroup.addPreference(application);
        }
    }

    private void populateApplicationList() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = mPackageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
                        | PackageManager.MATCH_DISABLED_COMPONENTS);
        Collections.sort(resolveInfos, (resolveInfo1, resolveInfo2) -> {
            String appName1 = resolveInfo1.loadLabel(mPackageManager).toString();
            String appName2 = resolveInfo2.loadLabel(mPackageManager).toString();
            return appName1.compareTo(appName2);
        });

        mApplications = new ArrayList<>(resolveInfos.size());
        for (ResolveInfo resolveInfo : resolveInfos) {
            mApplications.add(createApplicationPreference(resolveInfo));
        }
    }

    private Preference createApplicationPreference(ResolveInfo resolveInfo) {
        Preference preference = new Preference(getContext());
        preference.setTitle(resolveInfo.loadLabel(mPackageManager));
        preference.setIcon(resolveInfo.loadIcon(mPackageManager));
        preference.setOnPreferenceClickListener((p) -> {
            getFragmentController().launchFragment(
                    ApplicationDetailsFragment.getInstance(resolveInfo.activityInfo.packageName));
            return true;
        });
        return preference;
    }
}
