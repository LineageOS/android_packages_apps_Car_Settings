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

package com.android.car.settings.search;

import static com.android.car.settings.deeplink.DeepLinkHomepageActivity.EMBEDDED_DEEPLINK_INTENT_DATA;
import static com.android.car.settings.deeplink.DeepLinkHomepageActivity.convertToDeepLinkHomepageIntent;

import android.annotation.Nullable;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import com.android.car.settings.R;
import com.android.car.settings.activityembedding.ActivityEmbeddingUtils;
import com.android.car.settings.common.Logger;

import java.net.URISyntaxException;

/**
 * Trampoline activity for consuming, processing, and launching Intent for Settings search results
 * provided by SettingsIntelligence.
 */
public class CarSearchResultTrampoline extends Activity {
    private static final Logger LOG = new Logger(CarSearchResultTrampoline.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (verifySearchResultCallerPackage(getLaunchedFromPackage())) {
            Intent intentToStart = getTargetIntent(getIntent());
            if (intentToStart == null) {
                LOG.e("Could not parse intent to start from settings intelligence.");
                return;
            }
            if (ActivityEmbeddingUtils.isEmbeddingActivityEnabled(this)) {
                LOG.i("Starting the settings search resolved activity with a deep link.");
                intentToStart = convertToDeepLinkHomepageIntent(intentToStart);
                intentToStart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            startActivity(intentToStart);
        }
        finish();
    }

    /**
     * Parses settings search into the original intent.
     */
    @Nullable
    private Intent getTargetIntent(Intent intent) {
        Intent targetIntent = null;
        final String intentUriString = intent.getStringExtra(
                Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_INTENT_URI);
        try {
            targetIntent = Intent.parseUri(intentUriString, Intent.URI_INTENT_SCHEME);
            targetIntent.setData(
                    intent.getParcelableExtra(EMBEDDED_DEEPLINK_INTENT_DATA, Uri.class));
        } catch (URISyntaxException e) {
            LOG.e("Error parsing target intent URI from: "
                    + getSettingsIntelligencePkgName());
        }
        return targetIntent;
    }

    private boolean verifySearchResultCallerPackage(@Nullable String callerPackage) {
        if (callerPackage == null) {
            return false;
        }
        return callerPackage.equals(getSettingsIntelligencePkgName());
    }

    private String getSettingsIntelligencePkgName() {
        return getString(R.string.config_settingsintelligence_package_name);
    }
}
