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
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;

import androidx.preference.Preference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settingslib.applications.ApplicationFeatureProvider;

/**
 * Base class for controllers that shows a number of apps in the Enterprise Privacy /
 * Managed Device Info screen.
 *
 * <p>The counter is calculated asynchronously.
 */
abstract class BaseEnterprisePrivacyAppsCounterPreferenceController<P extends Preference>
        extends BaseEnterprisePrivacyPreferenceController<P> {

    private int mCount;

    @Nullable
    private ApplicationFeatureProvider.NumberOfAppsCallback mCallback;

    @Nullable
    private Integer mAvailabilityStatus;

    protected BaseEnterprisePrivacyAppsCounterPreferenceController(Context context,
            String preferenceKey, FragmentController fragmentController,
            CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    /**
     * Calls the method that will lazy-load the counter.
     */
    protected abstract void lazyLoad(ApplicationFeatureProvider.NumberOfAppsCallback callback);

    /**
     * Gets the count returned by the callback.
     */
    protected int getCount() {
        return mCount;
    }

    @Override
    protected final int getAvailabilityStatus() {
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

    private void onLazyLoaded(Integer count) {
        mCount = count;
        mAvailabilityStatus = (count != null && count > 0)
                ? AVAILABLE
                : DISABLED_FOR_PROFILE;
        mLogger.d("onLazyLoaded(): count=" + count + ", status=" + mAvailabilityStatus);

        if (count <= 0) {
            // No need to update anything
            return;
        }

        refreshUi();
    }
}
