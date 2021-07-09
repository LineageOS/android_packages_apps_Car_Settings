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

import android.car.drivingstate.CarUxRestrictions;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.UserHandle;

import androidx.preference.Preference;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;

import java.util.List;

/**
 * Controller for showing the device admin apps.
 */
public final class ManageDeviceAdminPreferenceController
        extends BaseDeviceAdminAddPreferenceController {

    private final boolean mEnabled;

    public ManageDeviceAdminPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mEnabled = mPm.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN);
    }

    @Override
    protected void updateState(Preference preference) {
        int activeAdmins = getNumberOfAdmins();
        mLogger.d("updateState(): Number of active device admin apps: " + activeAdmins);
        String summary = activeAdmins == 0
                ? getContext().getString(R.string.number_of_device_admins_none)
                : getContext().getResources().getQuantityString(R.plurals.number_of_device_admins,
                        activeAdmins, activeAdmins);
        preference.setSummary(summary);
    }

    @Override
    protected int getRealAvailabilityStatus() {
        // TODO(b/185182679): Grayed out and disabled for now. Enable once fully implemented.
        return mEnabled ? AVAILABLE_FOR_VIEWING : UNSUPPORTED_ON_DEVICE;
    }

    private int getNumberOfAdmins() {
        int activeAdmins = 0;
        for (UserInfo userInfo : mUm.getProfiles(UserHandle.myUserId())) {
            List<ComponentName> activeAdminsForUser = mDpm.getActiveAdminsAsUser(userInfo.id);
            mLogger.d("Active admin apps: " + activeAdminsForUser + " for user: "
                    + userInfo.toFullString());
            if (activeAdminsForUser != null) {
                activeAdmins += activeAdminsForUser.size();
            }
        }
        return activeAdmins;
    }
}
