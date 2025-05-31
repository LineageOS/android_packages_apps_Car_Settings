/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.car.settings.bluetooth.audiosharing;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;

import androidx.preference.PreferenceCategory;

import com.android.car.settings.common.FragmentController;

/**
 * Simple audio sharing bluetooth preference controller for static {@link PreferenceCategory} texts.
 */
public class BroadcastDescriptionPreferenceController extends
        BaseAudioSharingPreferenceController<PreferenceCategory> {
    public BroadcastDescriptionPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<PreferenceCategory> getPreferenceType() {
        return PreferenceCategory.class;
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        if (isUserEnabled() && isBroadcastAvailable()) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }
}
