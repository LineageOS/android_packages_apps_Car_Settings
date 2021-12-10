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
import android.app.admin.DevicePolicyManager;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;

import androidx.preference.PreferenceGroup;

import com.android.car.settings.common.FragmentController;
import com.android.car.settingslib.applications.ApplicationFeatureProvider;
import com.android.car.settingslib.applications.ApplicationFeatureProviderImpl;
import com.android.car.settingslib.applications.UserAppInfo;

import java.util.List;

/**
 * Base class for PreferenceControllers that builds a dynamic list of applications.
 */
abstract class BaseApplicationListPreferenceController
        extends BaseEnterprisePrivacyPreferenceController<PreferenceGroup> {

    protected final ApplicationFeatureProvider mApplicationFeatureProvider;

    @Nullable
    private List<UserAppInfo> mApps;

    @Nullable
    private ApplicationFeatureProvider.ListOfAppsCallback mCallback;

    @Nullable
    private Integer mAvailabilityStatus;

    protected BaseApplicationListPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions,
            @Nullable ApplicationFeatureProvider provider) {
        super(context, preferenceKey, fragmentController, uxRestrictions);

        // provider is only non-null on test cases
        mApplicationFeatureProvider = provider != null ? provider
                : new ApplicationFeatureProviderImpl(context,
                        context.getPackageManager(), AppGlobals.getPackageManager(),
                        context.getSystemService(DevicePolicyManager.class));
    }

    /**
     * Calls the method that will lazy-load the apps.
     */
    protected abstract void lazyLoad(ApplicationFeatureProvider.ListOfAppsCallback callback);

    /**
     * Gets the list of apps returned by the callback.
     */
    protected List<UserAppInfo> getApps() {
        return mApps;
    }

    @Override
    protected Class<PreferenceGroup> getPreferenceType() {
        return PreferenceGroup.class;
    }

    @Override
    protected int getAvailabilityStatus() {
        if (mAvailabilityStatus != null) {
            // Already calculated
            mLogger.d("getAvailabilityStatus(): returning cached result " + mAvailabilityStatus);
            return mAvailabilityStatus;
        }

        int superStatus = super.getAvailabilityStatus();
        if (superStatus != AVAILABLE) {
            mLogger.d("getAvailabilityStatus(): returning superclass status " + superStatus);
            return superStatus;
        }

        if (mCallback != null) {
            mLogger.d("getAvailabilityStatus(): already waiting for callback...");
        } else {
            mLogger.d("getAvailabilityStatus(): lazy-loading number of apps");
            mCallback = (result) -> onLazyLoaded(result);
            lazyLoad(mCallback);
        }

        // Calculating the number of apps can takes a bit of time, so we always return
        // CONDITIONALLY_UNAVAILABLE, so the actual visibility will be set when the result is called
        // back (on onLazyLoaded()).
        return CONDITIONALLY_UNAVAILABLE;
    }

    private void onLazyLoaded(List<UserAppInfo> result) {
        mApps = result;
        mAvailabilityStatus = !result.isEmpty()
                ? AVAILABLE
                : DISABLED_FOR_PROFILE;
        mLogger.d("onLazyLoaded(): apps=" + result + ", status=" + mAvailabilityStatus);

        if (result.isEmpty()) {
            // No need to update anything
            return;
        }

        refreshUi();
    }
}
