/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static android.net.NetworkPolicy.LIMIT_DISABLED;
import static android.net.NetworkPolicy.WARNING_DISABLED;

import static org.mockito.Mockito.mock;

import android.net.NetworkPolicy;
import android.net.NetworkTemplate;
import android.util.RecurrenceRule;

import com.android.settingslib.NetworkPolicyEditor;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.HashMap;
import java.util.Map;

@Implements(NetworkPolicyEditor.class)
public class ShadowNetworkPolicyEditor {

    private static final Map<String, Long> sWarningBytesMap = new HashMap<>();
    private static final Map<String, Long> sLimitBytesMap = new HashMap<>();

    @Implementation
    protected long getPolicyWarningBytes(NetworkTemplate template) {
        return sWarningBytesMap.getOrDefault(template.getSubscriberId(), WARNING_DISABLED);
    }

    @Implementation
    protected void setPolicyWarningBytes(NetworkTemplate template, long warningBytes) {
        sWarningBytesMap.put(template.getSubscriberId(), warningBytes);
    }

    @Implementation
    protected long getPolicyLimitBytes(NetworkTemplate template) {
        return sLimitBytesMap.getOrDefault(template.getSubscriberId(), LIMIT_DISABLED);
    }

    @Implementation
    protected void setPolicyLimitBytes(NetworkTemplate template, long limitBytes) {
        sLimitBytesMap.put(template.getSubscriberId(), limitBytes);
    }

    @Implementation
    protected NetworkPolicy getPolicy(NetworkTemplate template) {
        NetworkPolicy policy = new NetworkPolicy(template, mock(RecurrenceRule.class),
                getPolicyWarningBytes(template), getPolicyLimitBytes(template), 0, 0, 0, false,
                false);
        if (sWarningBytesMap.containsKey(template.getSubscriberId()) || sLimitBytesMap.containsKey(
                template.getSubscriberId())) {
            return policy;
        }
        return null;
    }

    @Resetter
    public static void reset() {
        sWarningBytesMap.clear();
        sLimitBytesMap.clear();
    }
}
