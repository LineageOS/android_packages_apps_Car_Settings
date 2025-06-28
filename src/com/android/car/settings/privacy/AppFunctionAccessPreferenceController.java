/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.app.appfunctions.AppFunctionManager;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.Intent;

import androidx.preference.Preference;

import com.android.car.settings.Flags;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.PreferenceController;

/**
 * Controller for displaying camera access page.
 */
public class AppFunctionAccessPreferenceController extends PreferenceController<Preference> {
    private static final Logger LOG = new Logger(AppFunctionAccessPreferenceController.class);
    private final AppFunctionManager mAppFunctionManager;

    public AppFunctionAccessPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        this(context, preferenceKey, fragmentController, uxRestrictions,
                context.getSystemService(AppFunctionManager.class));
    }

    public AppFunctionAccessPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions,
            AppFunctionManager appFunctionManager) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mAppFunctionManager = appFunctionManager;
    }

    @Override
    protected boolean handlePreferenceClicked(Preference preference) {
        String targetPackage = getContext().getPackageManager()
                .getPermissionControllerPackageName();
        Intent intent = new Intent(AppFunctionManager.ACTION_MANAGE_APP_FUNCTION_ACCESS)
                .setPackage(targetPackage);
        LOG.d("Starting manage app function page in: " + targetPackage);
        getContext().startActivity(intent);
        return true;
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        if (mAppFunctionManager != null && featureFlagsEnabled()) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    private boolean featureFlagsEnabled() {
        return android.permission.flags.Flags.appFunctionAccessUiEnabled()
                && Flags.appFunctionAccessPermissionsUi();
    }

    @Override
    protected Class<Preference> getPreferenceType() {
        return Preference.class;
    }
}
