/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.settings;

import com.android.car.settings.common.CarSettingActivity;

/**
 * A collection of all entry point acitvities that are needed for Activity entries in
 * AndroidManifest file. In order to register intent filters at system level, we need to create
 * activity entries in AndroidManifest file, and each entry expects a different Activity as name.
 */
public class EntryPointActivities {
    /** Entry point class to handle {@link android.settings$LOCATION_SOURCE_SETTINGS} */
    public static class LocationSettingsActivity extends CarSettingActivity { /* empty */ }
    /** Entry point class to handle {@link android.net.wifi$PICK_WIFI_NETWORK} */
    public static class WifiPickerActivity extends CarSettingActivity { /* empty */ }
}
