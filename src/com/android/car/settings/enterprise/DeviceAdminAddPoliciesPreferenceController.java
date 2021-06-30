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

package com.android.car.settings.enterprise;

import android.app.admin.DeviceAdminInfo;
import android.car.drivingstate.CarUxRestrictions;
import android.content.ComponentName;
import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.car.settings.common.FragmentController;

/**
 * Controller for the screen that shows the device policies requested by a device admin app.
 */
public final class DeviceAdminAddPoliciesPreferenceController
        extends BaseDeviceAdminAddPreferenceController<PreferenceGroup> {

    public DeviceAdminAddPoliciesPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<PreferenceGroup> getPreferenceType() {
        return PreferenceGroup.class;
    }

    @Override
    protected int getAvailabilityStatus() {
        int superStatus = super.getAvailabilityStatus();
        if (superStatus != AVAILABLE) {
            return superStatus;
        }

        ComponentName admin = mDeviceAdminInfo.getComponent();
        boolean isOwner = admin.equals(mDpm.getProfileOwner())
                || admin.equals(mDpm.getDeviceOwnerComponentOnCallingUser());

        return isOwner ? DISABLED_FOR_PROFILE : AVAILABLE;
    }

    // TODO(b/188585303): add unit tests
    // TODO(b/188585303): make PreferenceGroup scrolable / limit to ~5 or less values
    @Override
    protected void updateState(PreferenceGroup preferenceGroup) {
        preferenceGroup.removeAll();
        Context context = getContext();
        boolean isAdminUser = mUm.isAdminUser();

        for (DeviceAdminInfo.PolicyInfo pi : mDeviceAdminInfo.getUsedPolicies()) {
            int descriptionId = isAdminUser ? pi.description : pi.descriptionForSecondaryUsers;
            int labelId = isAdminUser ? pi.label : pi.labelForSecondaryUsers;
            CharSequence label = context.getText(labelId);
            CharSequence description = context.getText(descriptionId);
            mLogger.v("Adding policy '" + label + "': " + description);
            Preference preference = new Preference(context);
            preference.setTitle(label);
            preference.setSummary(description);
            preferenceGroup.addPreference(preference);
        }
    }
}
