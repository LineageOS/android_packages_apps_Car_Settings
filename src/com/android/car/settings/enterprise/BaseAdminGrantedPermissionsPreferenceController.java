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
import com.android.car.settings.applications.SyncApplicationFeatureProvider;
import com.android.car.settings.applications.SyncApplicationFeatureProviderImpl;
import com.android.car.settings.common.FragmentController;
import com.android.car.settingslib.applications.ApplicationFeatureProvider;
import com.android.car.settingslib.applications.ApplicationFeatureProviderImpl;

/**
 * Base class for controllers that show the number of apps that were granted permissions by the
 * admin.
 */
abstract class BaseAdminGrantedPermissionsPreferenceController
        extends BaseEnterprisePrivacyPreferenceController<Preference> {

    private final ApplicationFeatureProvider mApplicationFeatureProvider;
    private final SyncApplicationFeatureProvider mSyncApplicationFeatureProvider;
    private final String[] mPermissions;

    /**
     * Cached, lazy-loaded count of number of apps - it 's called just once as it's expensive and
     * CarSettings calls updateState() / getAvailabilityStatus() multiple times.
     */
    @Nullable
    private Integer mCount;

    BaseAdminGrantedPermissionsPreferenceController(Context context,
            String preferenceKey, FragmentController fragmentController,
            CarUxRestrictions uxRestrictions,
            @Nullable SyncApplicationFeatureProvider syncProvider,
            String[] permissions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);

        mApplicationFeatureProvider = new ApplicationFeatureProviderImpl(context, mPm,
                AppGlobals.getPackageManager(), mDpm);
        // syncProvider is only passed as argument on unit tests
        mSyncApplicationFeatureProvider = syncProvider != null ? syncProvider
                : new SyncApplicationFeatureProviderImpl(mApplicationFeatureProvider);
        mPermissions = permissions;
    }

    @Override
    public void updateState(Preference preference) {
        int count = getCount();
        preference.setSummary(getContext().getResources().getQuantityString(
                R.plurals.enterprise_privacy_number_packages_lower_bound, count, count));
    }

    @Override
    protected int getAvailabilityStatus() {
        int superStatus = super.getAvailabilityStatus();
        if (superStatus != AVAILABLE) return superStatus;

        return getCount() > 0 ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    private int getCount() {
        if (mCount == null) {
            mLogger.d("initializing mCount");
            mCount = mSyncApplicationFeatureProvider
                    .getNumberOfAppsWithAdminGrantedPermissions(mPermissions);
            mLogger.d("mCount = " + mCount);
        }
        return mCount;
    }
}
