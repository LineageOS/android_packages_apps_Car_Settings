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

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;

import com.android.car.settings.R;
import com.android.car.settings.applications.ApplicationPreferenceController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.SettingsFragment;
import com.android.settingslib.applications.ApplicationsState;

/**
 * Shows aspect ratio selections for an application.
 */
public class AppAspectRatioFragment extends SettingsFragment {

    private static final Logger LOG = new Logger(AppAspectRatioFragment.class);

    private static final String EXTRA_PACKAGE_NAME = "aspect_ratio_package_name";

    private String mPackageName;

    // Application state info
    private ApplicationsState.AppEntry mAppEntry;
    private ApplicationsState mAppState;

    /** Creates an instance of this fragment, passing packageName as an argument. */
    public static AppAspectRatioFragment getInstance(String packageName) {
        AppAspectRatioFragment appAspectRatioFragment =
                new AppAspectRatioFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_PACKAGE_NAME, packageName);
        appAspectRatioFragment.setArguments(bundle);
        return appAspectRatioFragment;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.app_aspect_ratio_fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        int userId = UserHandle.myUserId();
        mPackageName = getArguments().getString(EXTRA_PACKAGE_NAME);
        mAppState = ApplicationsState.getInstance(requireActivity().getApplication());
        mAppEntry = mAppState.getEntry(mPackageName, userId);

        use(ApplicationPreferenceController.class,
                R.string.pk_app_aspect_ratio_details)
                .setAppEntry(mAppEntry).setAppState(mAppState);
        use(AppAspectRatioActionButtonsPreferenceController.class,
                R.string.pk_app_aspect_ratio_details_action_buttons)
                .setPackageName(mPackageName);
        use(AppAspectRatiosGroupPreferenceController.class,
                R.string.pk_app_aspect_ratio_details_group)
                .setPackageName(mPackageName);
    }
}
