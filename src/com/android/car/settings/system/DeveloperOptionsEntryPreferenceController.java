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

package com.android.car.settings.system;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.os.Build;
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;
import com.android.car.settings.development.DevelopmentSettingsUtil;

/** Controls the visibility of the developer options setting. */
public class DeveloperOptionsEntryPreferenceController extends PreferenceController<Preference> {

    private UserManager mUserManager;

    public DeveloperOptionsEntryPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mUserManager = UserManager.get(context);
    }

    @Override
    protected Class<Preference> getPreferenceType() {
        return Preference.class;
    }

    @Override
    protected int getAvailabilityStatus() {
        return DevelopmentSettingsUtil.isDevelopmentSettingsEnabled(getContext(), mUserManager)
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    protected boolean handlePreferenceClicked(Preference preference) {
        // Needed to allow developer options to be enabled by default on eng builds
        // On first launch, the developer options module is disabled while the setting is enabled,
        // so we need to enable the module
        if (!DevelopmentSettingsUtil.isDeveloperOptionsModuleEnabled(getContext())) {
            if (Build.IS_ENG) {
                DevelopmentSettingsUtil.setDevelopmentSettingsEnabled(getContext(), /* enable= */
                        true);
            } else {
                throw new IllegalStateException("Inconsistent state: developer options enabled, but"
                        + " developer options module disabled.");
            }
        }
        getContext().startActivity(preference.getIntent());
        return true;
    }
}
