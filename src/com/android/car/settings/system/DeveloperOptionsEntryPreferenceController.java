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

import static android.os.UserManager.DISALLOW_DEBUGGING_FEATURES;

import static com.android.car.settings.enterprise.ActionDisabledByAdminDialogFragment.DISABLED_BY_ADMIN_CONFIRM_DIALOG_TAG;
import static com.android.car.settings.enterprise.EnterpriseUtils.hasUserRestrictionByDpm;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.PreferenceController;
import com.android.car.settings.development.DevelopmentSettingsUtil;
import com.android.car.settings.enterprise.EnterpriseUtils;
import com.android.car.ui.preference.CarUiPreference;


/** Controls the visibility of the developer options setting. */
public class DeveloperOptionsEntryPreferenceController
        extends PreferenceController<CarUiPreference> {

    private static final Logger LOG = new Logger(DeveloperOptionsEntryPreferenceController.class);

    public DeveloperOptionsEntryPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<CarUiPreference> getPreferenceType() {
        return CarUiPreference.class;
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        if (DevelopmentSettingsUtil.isDevelopmentSettingsEnabled(getContext())) {
            return AVAILABLE;
        }

        if (hasUserRestrictionByDpm(getContext(), DISALLOW_DEBUGGING_FEATURES)) {
            return AVAILABLE_FOR_VIEWING;
        }

        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();
        setClickableWhileDisabled(getPreference(), /* clickable= */ true, p -> {
            if (hasUserRestrictionByDpm(getContext(), DISALLOW_DEBUGGING_FEATURES)) {
                showActionDisabledByAdminDialog();
            }
        });
    }

    @Override
    protected boolean handlePreferenceClicked(CarUiPreference preference) {
        LOG.d("handlePreferenceClicked");
        getContext().startActivity(preference.getIntent());
        return true;
    }

    private void showActionDisabledByAdminDialog() {
        getFragmentController().showDialog(
                EnterpriseUtils.getActionDisabledByAdminDialog(getContext(),
                        DISALLOW_DEBUGGING_FEATURES),
                DISABLED_BY_ADMIN_CONFIRM_DIALOG_TAG);
    }
}
