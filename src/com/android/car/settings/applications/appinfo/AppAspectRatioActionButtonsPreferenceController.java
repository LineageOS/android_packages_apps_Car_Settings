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

package com.android.car.settings.applications.appinfo;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import static com.android.car.settings.common.ActionButtonsPreference.ActionButtons.BUTTON1;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserHandle;

import com.android.car.settings.R;
import com.android.car.settings.common.ActionButtonInfo;
import com.android.car.settings.common.ActionButtonsPreference;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.PreferenceController;

/**
 * A PreferenceController handling the logic for the action button on the aspect ratio page.
 */
public class AppAspectRatioActionButtonsPreferenceController extends
        PreferenceController<ActionButtonsPreference> {
    private static final Logger LOG = new Logger(
            AppAspectRatioActionButtonsPreferenceController.class);
    private PackageManager mPackageManager;
    private String mPackageName;
    private int mUserId;
    private ActionButtonInfo mOpenApplicationButton;

    public AppAspectRatioActionButtonsPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mUserId = UserHandle.myUserId();
        mPackageManager = context.getPackageManager();
    }

    @Override
    protected Class<ActionButtonsPreference> getPreferenceType() {
        return ActionButtonsPreference.class;
    }

    /**
     * Set the packageName, which is used to perform actions on a particular package.
     */
    public AppAspectRatioActionButtonsPreferenceController setPackageName(String packageName) {
        mPackageName = packageName;
        return this;
    }

    @Override
    protected void checkInitialized() {
        if (mPackageName == null) {
            throw new IllegalStateException(
                    "PackageName should be set before calling this function");
        }
    }

    @Override
    protected void onCreateInternal() {
        mOpenApplicationButton = getPreference().getButton(BUTTON1);
        mOpenApplicationButton
                .setText(R.string.aspect_ratio_action_button)
                .setIcon(R.drawable.ic_open)
                .setOnClickListener(i -> launchApplication());
    }

    private void launchApplication() {
        Intent launchIntent = mPackageManager.getLaunchIntentForPackage(mPackageName)
                .addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP);
        if (launchIntent != null) {
            getContext().startActivityAsUser(launchIntent, new UserHandle(mUserId));
        }
    }
}
