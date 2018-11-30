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

package com.android.car.settings.testutils;

import android.bluetooth.BluetoothAdapter;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.shadows.ShadowApplication;

@Implements(BluetoothAdapter.class)
public class ShadowBluetoothAdapter extends org.robolectric.shadows.ShadowBluetoothAdapter {

    private static int sResetCalledCount = 0;

    public static boolean verifyFactoryResetCalled(int numTimes) {
        return sResetCalledCount == numTimes;
    }

    @Implementation
    protected boolean factoryReset() {
        sResetCalledCount++;
        return true;
    }

    @Implementation
    protected static synchronized BluetoothAdapter getDefaultAdapter() {
        return (BluetoothAdapter) ShadowApplication.getInstance().getBluetoothAdapter();
    }

    @Resetter
    public static void reset() {
        sResetCalledCount = 0;
    }
}
