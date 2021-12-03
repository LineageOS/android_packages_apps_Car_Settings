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

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;

import com.android.car.settings.applications.SyncApplicationFeatureProvider;
import com.android.car.settings.common.FragmentController;
import com.android.internal.annotations.VisibleForTesting;

/**
* Controller to show apps that were granted location permissions by the device owner.
*/
public final class AdminGrantedLocationPermissionsPreferenceController
        extends BaseAdminGrantedPermissionsPreferenceController {

    public AdminGrantedLocationPermissionsPreferenceController(Context context,
            String preferenceKey, FragmentController fragmentController,
            CarUxRestrictions uxRestrictions) {
        this(context, preferenceKey, fragmentController, uxRestrictions, /* syncProvider= */ null);
    }

    @VisibleForTesting
    AdminGrantedLocationPermissionsPreferenceController(Context context,
            String preferenceKey, FragmentController fragmentController,
            CarUxRestrictions uxRestrictions, SyncApplicationFeatureProvider syncProvider) {
        super(context, preferenceKey, fragmentController, uxRestrictions, syncProvider,
                EnterpriseUtils.LOCATION_PERMISSIONS);
    }
}
