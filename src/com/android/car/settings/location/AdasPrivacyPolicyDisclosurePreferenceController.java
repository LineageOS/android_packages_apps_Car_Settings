/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.content.pm.PackageManager;
import android.os.Process;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.settings.privacy.RequiredInfotainmentAppsUtils;
import com.android.car.ui.preference.CarUiTwoActionTextPreference;

import java.util.Collection;

/**
 * Displays a list of ADAS apps with their privacy policy and a link to their location permission
 * settings.
 */
public final class AdasPrivacyPolicyDisclosurePreferenceController
        extends LocationStateListenerBasePreferenceController<LogicalPreferenceGroup> {
    private final PackageManager mPackageManager;

    public AdasPrivacyPolicyDisclosurePreferenceController(
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
        addDefaultBypassLocationStateListener();
    }

    @Override
    protected void updateState(LogicalPreferenceGroup preference) {
        loadAppsWithLocationPermission();
    }

    private void loadAppsWithLocationPermission() {
        getPreference().removeAll();

        Collection<String> adasApps = getLocationManager().getAdasAllowlist().getPackages();
        boolean showSummary = getLocationManager().isAdasGnssLocationEnabled();
        for (String adasApp : adasApps) {
            CarUiTwoActionTextPreference preference = null;
            if (com.android.internal.camera.flags.Flags.cameraPrivacyAllowlist()) {
                preference = RequiredInfotainmentAppsUtils.createRequiredAppPreference(
                        getContext(), mPackageManager, adasApp, Process.myUserHandle(),
                        Manifest.permission_group.LOCATION, showSummary);
            }
            if (preference != null) {
                getPreference().addPreference(preference);
            }
        }
    }
}
