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

package com.android.car.settings.applications;

/**
 * Helper class providing simpler alternative for some APIs provided by
 * {@link ApplicationFeatureProvider}, as {@code CarSettings} don't need the async way.
 */
public interface SyncApplicationFeatureProvider {

    /**
     * Sync-only version of
     * {@link ApplicationFeatureProvider#calculateNumberOfAppsWithAdminGrantedPermissions(String[],
     * boolean,
     * com.android.car.settings.applications.ApplicationFeatureProvider.NumberOfAppsCallback)}
     *
     * @return numver of apps or {@code -1} if interrupted
     */
    int getNumberOfAppsWithAdminGrantedPermissions(String[] permissions);
}
