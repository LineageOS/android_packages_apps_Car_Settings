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

import android.annotation.Nullable;
import android.app.AppGlobals;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;

import androidx.preference.Preference;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settingslib.applications.ApplicationFeatureProvider;
import com.android.car.settingslib.applications.ApplicationFeatureProvider.NumberOfAppsCallback;
import com.android.car.settingslib.applications.ApplicationFeatureProviderImpl;

/**
 * Base class for controllers that show the number of apps that were granted permissions by the
 * admin.
 */
abstract class BaseAdminGrantedPermissionsPreferenceController
        extends BaseEnterprisePrivacyAppsCounterPreferenceController<Preference> {

    private final String[] mPermissions;

    private final ApplicationFeatureProvider mApplicationFeatureProvider;

    BaseAdminGrantedPermissionsPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions,
            @Nullable ApplicationFeatureProvider provider,
            String... permissions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);

        // provider is only non-null in test cases
        mApplicationFeatureProvider = provider != null ? provider :
                new ApplicationFeatureProviderImpl(context, mPm, AppGlobals.getPackageManager(),
                        mDpm);
        mPermissions = permissions;
    }

    @Override
    protected void lazyLoad(NumberOfAppsCallback callback) {
        mApplicationFeatureProvider.calculateNumberOfAppsWithAdminGrantedPermissions(mPermissions,
                /* async= */ true, callback);
    }

    @Override
    protected void updateState(Preference p) {
        int count = getCount();
        p.setSummary(getContext().getResources().getQuantityString(
                R.plurals.enterprise_privacy_number_packages_lower_bound, count, count));
    }
}
