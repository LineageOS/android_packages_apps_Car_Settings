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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Intent;

import com.android.car.settingslib.applications.ApplicationFeatureProvider;
import com.android.car.settingslib.enterprise.EnterpriseDefaultApps;
import com.android.car.settingslib.enterprise.EnterprisePrivacyFeatureProvider;

import org.mockito.ArgumentMatcher;
import org.mockito.Mock;

/**
 * Base test for all controllers in the enterprise privacy / managed device info screen
 */
abstract class BaseEnterprisePrivacyPreferenceControllerTestCase
        extends BaseEnterprisePreferenceControllerTestCase {

    @Mock
    protected ApplicationFeatureProvider mApplicationFeatureProvider;

    @Mock
    protected EnterprisePrivacyFeatureProvider mEnterprisePrivacyFeatureProvider;

    protected void verifyFindPersistentPreferredActivitiesCalledOnce() {
        verify(mApplicationFeatureProvider, times(EnterpriseDefaultApps.values().length))
                .findPersistentPreferredActivities(anyInt(), any());
    }

    protected ArgumentMatcher<Intent[]> matchesIntents(Intent[] intents) {
        return (Intent[] actualIntents) -> {
            if (actualIntents == null) {
                return false;
            }
            if (actualIntents.length != intents.length) {
                return false;
            }
            for (int i = 0; i < intents.length; i++) {
                if (!intents[i].filterEquals(actualIntents[i])) {
                    return false;
                }
            }
            return true;
        };
    }
}
