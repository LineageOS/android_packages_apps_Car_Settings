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
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.PreferenceController;
import com.android.car.settings.development.DevelopmentSettingsUtil;

/** Controls the visibility of the developer options setting. */
public class DeveloperOptionsEntryPreferenceController extends PreferenceController<Preference> {

    private static final Logger LOG = new Logger(DeveloperOptionsEntryPreferenceController.class);
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
        // TODO(b/137797266): This should be removed when the root cause of the inconsistency is
        //  discovered and fixed.
        if (!DevelopmentSettingsUtil.isDeveloperOptionsModuleEnabled(getContext())) {
            LOG.e("Inconsistent state: developer options enabled, but developer options module "
                    + "disabled. Retry enabling...");
            DevelopmentSettingsUtil.setDevelopmentSettingsEnabled(getContext(), /* enable= */ true);
        }

        getContext().startActivity(preference.getIntent());
        return true;
    }
}
