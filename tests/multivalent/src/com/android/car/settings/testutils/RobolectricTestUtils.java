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

package com.android.car.settings.testutils;

import org.junit.Assume;

public class RobolectricTestUtils {
    private final static String ROBOLECTRIC_CLASS_NAME = "org.robolectric.Robolectric";

    /**
     * Checks if the test is currently running under Robolectric.
     *
     * @return true if running under Robolectric, false otherwise.
     */
    public static boolean isRunningOnRobolectric() {
        try {
            Class.forName(ROBOLECTRIC_CLASS_NAME);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Assumes that the test is not running under Robolectric.
     *
     * @param message the message to display if the assumption is not met.
     */
    public static void assumeNotRunningOnRobolectric(String message) {
        Assume.assumeFalse(message, isRunningOnRobolectric());
    }
}
