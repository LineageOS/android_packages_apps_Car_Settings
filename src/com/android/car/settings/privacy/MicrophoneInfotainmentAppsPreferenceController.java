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

package com.android.car.settings.privacy;

import android.Manifest;
import android.annotation.FlaggedApi;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.SensorPrivacyManager;
import android.os.Process;
import android.os.UserHandle;

import com.android.car.settings.Flags;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.ui.preference.CarUiPreference;
import com.android.internal.annotations.VisibleForTesting;

import java.util.List;

@FlaggedApi(Flags.FLAG_MICROPHONE_PRIVACY_UPDATES)
public class MicrophoneInfotainmentAppsPreferenceController extends
        PrivacyBasePreferenceController<LogicalPreferenceGroup> {
    private static final Logger LOG =
            new Logger(MicrophoneInfotainmentAppsPreferenceController.class);
    private final PackageManager mPackageManager;
    private final Context mContext;

    public MicrophoneInfotainmentAppsPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        this(context, preferenceKey, fragmentController, uxRestrictions,
                context.getPackageManager(), SensorPrivacyManager.getInstance(context));
    }

    @VisibleForTesting
    MicrophoneInfotainmentAppsPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions,
            PackageManager packageManager, SensorPrivacyManager sensorPrivacyManager) {
        super(context, preferenceKey, fragmentController, uxRestrictions,
                sensorPrivacyManager, SensorPrivacyManager.Sensors.MICROPHONE);
        mPackageManager = packageManager;
        mContext = context;
    }

    @Override
    protected Class<LogicalPreferenceGroup> getPreferenceType() {
        return LogicalPreferenceGroup.class;
    }

    @Override
    protected void updateState(LogicalPreferenceGroup preference) {
        loadInfotainmentAppsWithMicrophonePermission();
    }

    private void loadInfotainmentAppsWithMicrophonePermission() {
        getPreference().removeAll();

        UserHandle userHandle = Process.myUserHandle();
        List<PackageInfo> packagesWithPermissions = PermissionUtils.getPackagesWithPermissionGroup(
                mContext,  Manifest.permission_group.MICROPHONE, userHandle,
                /* showSystem= */ false);
        int sensorPrivacyState = getSensorPrivacyManager().getSensorPrivacyState(
                SensorPrivacyManager.TOGGLE_TYPE_SOFTWARE,
                SensorPrivacyManager.Sensors.MICROPHONE);
        boolean showSummary = sensorPrivacyState == SensorPrivacyManager.StateTypes.DISABLED;

        for (PackageInfo packageInfo : packagesWithPermissions) {
            CarUiPreference preference =
                    RequiredInfotainmentAppsUtils.createInfotainmentAppPreference(
                            getContext(), mPackageManager, packageInfo.packageName, userHandle,
                            Manifest.permission_group.MICROPHONE, showSummary);

            if (preference != null) {
                getPreference().addPreference(preference);
            }
        }
    }
}
