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

package com.android.car.settings.activityembedding;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.WindowManager;

import androidx.window.embedding.ActivityEmbeddingController;
import androidx.window.embedding.SplitController;

import com.android.car.settings.R;
import com.android.car.settings.common.Logger;
/**
 * Utility class for retrieving Activity Embedding configurations to handle dual-pane layout.
 */
public class ActivityEmbeddingUtils {
    // The smallest value of the smallest-width (sw) of the window in any rotation when
    // the split should be used.
    // Without rotation enabled, it is safe to set this number to 0dp;
    private static final int MIN_SMALLEST_SCREEN_SPLIT_WIDTH_DP = 0;

    private static final Logger LOG = new Logger(ActivityEmbeddingUtils.class);

    private static float sSplitRatio;

    /**
     * Returns {@code true} if Activity Split API is enabled and CarSettings is
     * not configured to force single pane.
     */
    public static boolean isEmbeddingActivityEnabled(Context context) {
        boolean isSplitAvailable = SplitController.getInstance(context).getSplitSupportStatus()
                == SplitController.SplitSupportStatus.SPLIT_AVAILABLE;
        LOG.d("Is activity split enabled on this device: " + isSplitAvailable);
        boolean configForceSinglePane = context.getResources().getBoolean(
                R.bool.config_global_force_single_pane);
        return isSplitAvailable && !configForceSinglePane;
    }

    /**
     * Returns {@code true} if activity embedding is enabled by the system and CarSettings has
     * activated Activity Embedding based on the current application context.
     */
    public static boolean isEmbeddingSplitActivated(Activity activity) {
        return isEmbeddingActivityEnabled(activity)
                && ActivityEmbeddingController.getInstance(activity).isActivityEmbedded(activity);
    }

    /** Get the smallest pixel value of WIDTH of the window when the split should be used. */
    static int getMinCurrentScreenSplitWidthDp(Context context) {
        return context.getResources().getInteger(R.integer.config_activity_embed_split_min_cur_dp);
    }

    /**
     * Get the smallest pixel value of the SMALLEST-WIDTH (sw) of the window in any rotation when
     * the split should be used.
     * Effectively, (sw) is the height in dp when the width > height. Without rotation enabled, it
     * is safe to set this number to 0dp;
     */
    static int getMinSmallestScreenSplitWidthDp() {
        return MIN_SMALLEST_SCREEN_SPLIT_WIDTH_DP;
    }

    /**
     * Returns the ratio of the screen reserved for the left pane of Settings.
     */
    static float getSplitRatio(Context context) {
        if (sSplitRatio == 0) {
            float homepageWidth = context.getResources().getDimension(R.dimen.top_level_menu_width)
                    + context.getResources().getDimension(R.dimen.top_level_divider_width);
            sSplitRatio = homepageWidth / getTaskAreaMaxWindowWidth(context);
        }
        return sSplitRatio;
    }

    /**
     * Returns the Intent to launch the default activity in dual-pane settings.
     */
    public static Intent getPlaceholderIntent(Context context) {
        return new Intent().setClassName(context.getPackageName(),
                context.getString(R.string.config_homepage_placeholder_class));
    }

    /**
     * Returns the preference key of the default activity dual-pane settings.
     */
    public static String getPlaceholderPreferenceHighlightKey(Context context) {
        return context.getResources().getString(
                R.string.config_homepage_placeholder_preference_key);
    }

    /**
     * Returns the well-formed ComponentName String for the activity-alias for
     * {@link com.android.car.settings.common.CarSettingActivities.HomepageActivity}.
     */
    static String getHomepageActivityAliasName(Context context) {
        return context.getResources().getString(R.string.config_homepage_activity_alias_name);
    }

    private static int getTaskAreaMaxWindowWidth(Context context) {
        WindowManager wm = context.getSystemService(WindowManager.class);
        return wm.getMaximumWindowMetrics().getBounds().width();
    }
}
