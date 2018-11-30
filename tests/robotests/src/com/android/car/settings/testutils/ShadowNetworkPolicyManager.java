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

import android.net.NetworkPolicyManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.HashMap;
import java.util.Map;

@Implements(NetworkPolicyManager.class)
public class ShadowNetworkPolicyManager {

    private static Map<String, Integer> sResetCalledForSubscriberCount = new HashMap<>();

    public static boolean verifyFactoryResetCalled(String subscriber, int numTimes) {
        if (!sResetCalledForSubscriberCount.containsKey(subscriber)) return false;
        return sResetCalledForSubscriberCount.get(subscriber) == numTimes;
    }

    @Implementation
    protected void factoryReset(String subscriber) {
        sResetCalledForSubscriberCount.put(subscriber,
                sResetCalledForSubscriberCount.getOrDefault(subscriber, 0) + 1);
    }

    @Resetter
    public static void reset() {
        sResetCalledForSubscriberCount.clear();
    }
}
