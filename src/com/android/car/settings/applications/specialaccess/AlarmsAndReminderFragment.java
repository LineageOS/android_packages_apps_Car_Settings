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

package com.android.car.settings.applications.specialaccess;

import static android.Manifest.permission.SCHEDULE_EXACT_ALARM;

import android.app.AppOpsManager;
import android.content.Context;

import com.android.car.settings.R;
import com.android.car.settings.applications.AppListFragment;

/**
 * Displays a list of apps which have declared in their Manifest the permission
 * #{@link android.Manifest.permission#SCHEDULE_EXACT_ALARM} and allows the users to grant or revoke
 * the permission from those apps.
 */
public class AlarmsAndReminderFragment extends AppListFragment {

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.alarms_and_reminders_fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(AppOpsPreferenceController.class, R.string.pk_alarms_and_reminders).init(
                AppOpsManager.OP_SCHEDULE_EXACT_ALARM,
                SCHEDULE_EXACT_ALARM,
                AppOpsManager.MODE_ERRORED);
    }

    @Override
    protected void onToggleShowSystemApps(boolean showSystem) {
        use(AppOpsPreferenceController.class, R.string.pk_alarms_and_reminders).setShowSystem(
                showSystem);
    }
}
