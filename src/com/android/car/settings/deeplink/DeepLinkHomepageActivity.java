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

package com.android.car.settings.deeplink;

import static android.provider.Settings.ACTION_SETTINGS_EMBED_DEEP_LINK_ACTIVITY;
import static android.provider.Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_HIGHLIGHT_MENU_KEY;
import static android.provider.Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_INTENT_URI;

import static com.android.car.settings.CarSettingsApplication.CAR_SETTINGS_PACKAGE_NAME;

import android.annotation.NonNull;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.util.Log;

import androidx.window.embedding.SplitRule;

import com.android.car.settings.activityembedding.ActivityEmbeddingRulesController;
import com.android.car.settings.activityembedding.ActivityEmbeddingUtils;
import com.android.car.settings.common.CarSettingActivities;
import com.android.car.settings.common.Logger;

import java.net.URISyntaxException;

/**
 * {@link CarSettingActivities.HomepageActivity} for when dual-pane is enable via ActivityEmbedding.
 * This class should only be enabled when dual-pane is active, and should only host activities
 * that have the permission {@link android.permission.LAUNCH_MULTI_PANE_SETTINGS_DEEP_LINK}.
 * Intents that target an Activity defined within this package, such as subclasses of
 * {@link com.android.car.settings.common.BaseCarSettingsActivity}, will directly be started in dual
 * pane unless it is targeting a single pane Activity. There is no need to convert these Intents
 * using {@code convertToDeepLinkHomepageIntent}.
 * Other Intents that target external setting defined outside of this package, such as those added
 * by {@link com.android.car.settings.common.ExtraSettingsLoader}, should be started using a similar
 * scheme as defined in the method {@code convertToDeepLinkHomepageIntent}.
 */
public class DeepLinkHomepageActivity extends CarSettingActivities.HomepageActivity {
    public static final String EMBEDDED_DEEPLINK_INTENT_DATA =
            "com.android.car.settings.EMBEDDED_DEEPLINK_INTENT_DATA";
    public static final String EXTRA_TARGET_SECONDARY_CONTAINER =
            "com.android.car.settings.EXTRA_TARGET_SECONDARY_CONTAINER";
    private static final String TAG = "DeepLinkHomepageActivity";

    private static final Logger LOG = new Logger(DeepLinkHomepageActivity.class);

    @Override
    protected void handleNewIntent(Intent intent) {
        super.handleNewIntent(intent);
        maybeLaunchDeepLinkActivity(intent);
    }

    /**
     * Launches deep link activity to be directly two pane if it is supported.
     */
    private void maybeLaunchDeepLinkActivity(Intent intent) {
        if (!ActivityEmbeddingUtils.isEmbeddingActivityEnabled(this)) {
            Log.e(TAG, "Embedding is not enabled. Finishing DeepLinkHomepageActivity.");
            finish();
        }
        Intent targetIntent;
        try {
            String intentUriString = intent.getStringExtra(
                    EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_INTENT_URI);
            if (intentUriString == null) {
                LOG.e("Unable to parse trampoline intent. Intent URI is null");
                return;
            }
            targetIntent = Intent.parseUri(intentUriString, Intent.URI_INTENT_SCHEME);
            targetIntent.setData(
                    intent.getParcelableExtra(EMBEDDED_DEEPLINK_INTENT_DATA, Uri.class));
        } catch (URISyntaxException e) {
            LOG.e("Error parsing trampoline intent: " + e);
            return;
        }
        ComponentName targetComponentName = targetIntent.resolveActivity(getPackageManager());
        targetIntent.setComponent(targetComponentName);
        targetIntent.removeFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        targetIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        targetIntent.replaceExtras(intent);
        ActivityEmbeddingRulesController.registerDualPaneSplitRule(this,
                /* primaryComponent= */ new ComponentName(getApplicationContext(), getClass()),
                /* secondaryComponent= */ targetComponentName,
                /* secondaryIntentAction= */ targetIntent.getAction(),
                /* finishPrimaryWithSecondary= */ SplitRule.FinishBehavior.ALWAYS,
                /* finishSecondaryWithPrimary= */ SplitRule.FinishBehavior.ALWAYS,
                /* clearTop= */ true);
        setTopLevelHeaderKey(getTopLevelHeaderKey(targetIntent));
        targetIntent.putExtra(EXTRA_TARGET_SECONDARY_CONTAINER, true);
        startActivity(targetIntent);
    }

    /**
     * Retrieves the top level header key from the deeplink Intent. If it is available via resolved
     * {@link ActivityInfo}, then we prioritize displaying that over the intent extra.
     */
    private String getTopLevelHeaderKey(Intent targetIntent) {
        String metaDataHeaderKey = null;
        ActivityInfo ai = getActivityInfo(getPackageManager(), targetIntent.getComponent());
        if (ai != null && ai.metaData != null) {
            metaDataHeaderKey = ai.metaData.getString(META_DATA_KEY_HEADER_KEY);
        }
        String deepLinkExtraHeaderKey = targetIntent.getStringExtra(
                EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_HIGHLIGHT_MENU_KEY);
        return metaDataHeaderKey != null ? metaDataHeaderKey : deepLinkExtraHeaderKey;
    }

    /**
     * Converts an  {@link Intent} to a deeplink intent that, upon being started, will be
     * trampolined and then handled by this class {@link DeepLinkHomepageActivity}.
     */
    @NonNull
    public static Intent convertToDeepLinkHomepageIntent(@NonNull Intent targetIntent) {
        targetIntent = new Intent(targetIntent);
        targetIntent.setSelector(null);

        Intent trampolineIntent = new Intent(ACTION_SETTINGS_EMBED_DEEP_LINK_ACTIVITY);
        trampolineIntent.setPackage(CAR_SETTINGS_PACKAGE_NAME);
        trampolineIntent.replaceExtras(targetIntent);
        trampolineIntent.putExtra(EMBEDDED_DEEPLINK_INTENT_DATA, targetIntent.getData());
        // If Intent#getData() is not null, Intent#toUri will return an Uri which has the scheme
        // of Intent#getData(), and it may not be the scheme of the original Intent (i.e http:
        // instead of ACTION_VIEW.
        targetIntent.setData(null);
        trampolineIntent.putExtra(EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_INTENT_URI,
                targetIntent.toUri(Intent.URI_INTENT_SCHEME));

        return trampolineIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT
                | Intent.FLAG_ACTIVITY_NEW_TASK);
    }
}
