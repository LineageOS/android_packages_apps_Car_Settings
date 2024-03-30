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

package com.android.car.settings.common;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;

/**
 * A controller for Preference categories.
 */
public class PreferenceCategoryController extends PreferenceController<PreferenceCategory> {

    public PreferenceCategoryController(Context context, String preferenceKey,
            FragmentController fragmentController,
            CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<PreferenceCategory> getPreferenceType() {
        return PreferenceCategory.class;
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        if (getPreference().getPreferenceCount() == 0) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        int count = getVisibleChildrenCount(getPreference());
        if (count == 0) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        return AVAILABLE;
    }

    private int getVisibleChildrenCount(PreferenceGroup preferenceGroup) {
        int preferenceCount = 0;
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            Preference preference = preferenceGroup.getPreference(i);
            if (preference instanceof PreferenceGroup) {
                preferenceCount += getVisibleChildrenCount((PreferenceGroup) preference);
            } else if (preference.isVisible()) {
                preferenceCount++;
            }
        }
        return preferenceCount;
    }
}
