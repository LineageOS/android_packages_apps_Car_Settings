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

import android.os.Build;
import android.util.Log;

import com.android.car.settings.CarSettingsStatsLog;


/**
 * Helper class that directly interacts with {@link CarSettingsStatsLog}, a generated class that
 * contains logging methods for MobileDataRow and MobileNetworkEntryPreferenceController.
 */
public class DataSubscriptionStatsLogHelper {
    private static final String TAG = DataSubscriptionStatsLogHelper.class.getSimpleName();
    private static DataSubscriptionStatsLogHelper sInstance;

    /**
     * Returns the current logging instance of DataSubscriptionStatsLogHelper to write this devices'
     *  CarSettingsStatsLog.
     *
     * @return the logging instance of DataSubscriptionStatsLogHelper.
     */
    public static DataSubscriptionStatsLogHelper getInstance() {
        if (sInstance == null) {
            sInstance = new DataSubscriptionStatsLogHelper();
        }
        return sInstance;
    }

    /**
     * Writes to CarSettingsDataSubscriptionEventReported atom with {@code launchType}
     * as the only field,
     *
     */
    public void writeDataSubscriptionEventReported() {
        if (Build.isDebuggable()) {
            Log.v(TAG, "writing CAR_SETTINGS_DATA_SUBSCRIPTION_EVENT_REPORTED.");
        }
        CarSettingsStatsLog.write(
                /* atomId */ CarSettingsStatsLog.CAR_SETTINGS_DATA_SUBSCRIPTION_EVENT_REPORTED);
    }
}
