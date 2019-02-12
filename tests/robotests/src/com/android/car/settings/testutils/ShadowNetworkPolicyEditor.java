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

import static android.net.NetworkPolicy.WARNING_DISABLED;

import android.net.NetworkTemplate;

import com.android.settingslib.NetworkPolicyEditor;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.HashMap;
import java.util.Map;

@Implements(NetworkPolicyEditor.class)
public class ShadowNetworkPolicyEditor {

    private static final Map<String, Long> sWarningBytesMap = new HashMap<>();

    @Implementation
    protected long getPolicyWarningBytes(NetworkTemplate template) {
        return sWarningBytesMap.getOrDefault(template.getSubscriberId(), WARNING_DISABLED);
    }

    @Implementation
    protected void setPolicyWarningBytes(NetworkTemplate template, long warningBytes) {
        sWarningBytesMap.put(template.getSubscriberId(), warningBytes);
    }

    @Resetter
    public static void reset() {
        sWarningBytesMap.clear();
    }
}
