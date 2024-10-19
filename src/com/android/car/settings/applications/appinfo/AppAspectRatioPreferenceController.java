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

import static android.provider.Settings.ACTION_MANAGE_USER_ASPECT_RATIO_SETTINGS;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.PreferenceController;
import com.android.car.ui.preference.CarUiPreference;

/**
 * A PreferenceController handling the logic for setting an app's aspect ratio
 */
public final class AppAspectRatioPreferenceController extends
        PreferenceController<CarUiPreference> {
    private static final Logger LOG = new Logger(AppAspectRatioPreferenceController.class);
    private static final String PACKAGE_UI_SCHEME = "package:";
    private ApplicationInfo mApplicationInfo;
    private AspectRatioManager mAspectRatioManager;

    public AppAspectRatioPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController,
            CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mAspectRatioManager = new AspectRatioManager(context);
    }

    @Override
    protected Class<CarUiPreference> getPreferenceType() {
        return CarUiPreference.class;
    }

    @Override
    public int getDefaultAvailabilityStatus() {
        return mAspectRatioManager.shouldShowAspectRatioSettingsForApp(mApplicationInfo)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    /**
     * Set the ApplicationInfo associated with this setting.
     *
     * @param applicationInfo The applicationInfo of the app to change aspect ratio.
     */
    public void setApplicationInfo(ApplicationInfo applicationInfo) {
        mApplicationInfo = applicationInfo;
    }

    @Override
    protected boolean handlePreferenceClicked(CarUiPreference preference) {
        Intent intent = new Intent(ACTION_MANAGE_USER_ASPECT_RATIO_SETTINGS);
        intent.setData(getPackageUri());

        if (intent.resolveActivity(getContext().getPackageManager()) != null) {
            getContext().startActivity(intent);
        } else {
            LOG.e("No activity found to handle aspect ratio settings.");
        }

        return true;
    }

    private Uri getPackageUri() {
        return Uri.parse(PACKAGE_UI_SCHEME + mApplicationInfo.packageName);
    }
}
