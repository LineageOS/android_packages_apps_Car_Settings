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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.window.embedding.ActivityFilter;
import androidx.window.embedding.ActivityRule;
import androidx.window.embedding.EmbeddingAspectRatio;
import androidx.window.embedding.RuleController;
import androidx.window.embedding.SplitAttributes;
import androidx.window.embedding.SplitPairFilter;
import androidx.window.embedding.SplitPairRule;
import androidx.window.embedding.SplitPlaceholderRule;
import androidx.window.embedding.SplitRule;

import com.android.car.settings.applications.managedomainurls.ManageDomainUrlsActivity;
import com.android.car.settings.common.CarSettingActivities;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.SubSettingsActivity;
import com.android.car.settings.deeplink.DeepLinkHomepageActivity;
import com.android.car.settings.enterprise.DeviceAdminAddActivity;
import com.android.car.settings.enterprise.EnterprisePrivacySettingsActivity;
import com.android.car.settings.profiles.ProfileSwitcherActivity;
import com.android.car.settings.security.CheckLockActivity;
import com.android.car.settings.security.ChooseLockPasswordActivity;
import com.android.car.settings.security.ChooseLockPatternActivity;
import com.android.car.settings.security.ChooseLockPinActivity;
import com.android.car.settings.security.SettingsScreenLockActivity;
import com.android.car.settings.security.VerifyLockChangeActivity;

import java.util.HashSet;
import java.util.Set;

/**
 * Main controller class for registering and updating rules for pane split before launching another
 * settings activity. */
public class ActivityEmbeddingRulesController {
    private final Context mContext;
    private final RuleController mRuleController;
    private static final Logger LOG = new Logger(ActivityEmbeddingRulesController.class);
    private static final ComponentName COMPONENT_NAME_WILDCARD = new ComponentName(
            /* pkg */ "*", /* cls */ "*");

    public ActivityEmbeddingRulesController(Context context) {
        mContext = context;
        mRuleController = RuleController.getInstance(mContext);
    }

    /**
     * Registers a new dual pane split rule.
     *
     * Method is defined to be static to allow dynamic adding of additional secondary components and
     * intents when the activity to be embedded is not defined inside CarSettings package.
     */
    public static void registerDualPaneSplitRule(Context context,
            ComponentName primaryComponent,
            ComponentName secondaryComponent,
            String secondaryIntentAction,
            SplitRule.FinishBehavior finishPrimaryWithSecondary,
            SplitRule.FinishBehavior finishSecondaryWithPrimary,
            boolean clearTop) {
        if (!ActivityEmbeddingUtils.isEmbeddingActivityEnabled(context)) {
            LOG.d("Embedding is not enabled for Settings.");
            return;
        }
        final Set<SplitPairFilter> filters = new HashSet<>();
        filters.add(new SplitPairFilter(primaryComponent, secondaryComponent,
                secondaryIntentAction));
        SplitAttributes attributes = new SplitAttributes.Builder()
                .setSplitType(SplitAttributes.SplitType.ratio(
                        ActivityEmbeddingUtils.getSplitRatio(context)))
                .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                .build();
        SplitPairRule splitPairRule = new SplitPairRule.Builder(filters)
                .setFinishPrimaryWithSecondary(finishPrimaryWithSecondary)
                .setFinishSecondaryWithPrimary(finishSecondaryWithPrimary)
                .setClearTop(clearTop)
                .setMinWidthDp(
                        ActivityEmbeddingUtils.getMinCurrentScreenSplitWidthDp(context))
                .setMinSmallestWidthDp(
                        ActivityEmbeddingUtils.getMinSmallestScreenSplitWidthDp())
                .setMaxAspectRatioInPortrait(EmbeddingAspectRatio.ALWAYS_ALLOW)
                .setDefaultSplitAttributes(attributes)
                .build();
        RuleController.getInstance(context).addRule(splitPairRule);
    }
    /**
     * Registers a new dual pane split rule for all instances of Homepage Activity.
     */
    public static void registerHomepageDualPaneSplitRule(
            Context context,
            ComponentName secondaryComponent,
            String secondaryIntentAction,
            boolean clearTop) {
        registerDualPaneSplitRule(/* context= */ context,
                /* primaryComponent= */ getComponentName(context,
                        CarSettingActivities.HomepageActivity.class),
                /* secondaryComponent= */ secondaryComponent,
                /* secondaryIntentAction= */ secondaryIntentAction,
                /* finishPrimaryWithSecondary= */ SplitRule.FinishBehavior.ALWAYS,
                /* finishSecondaryWithPrimary= */ SplitRule.FinishBehavior.ALWAYS,
                /* clearTop= */ clearTop);
        registerDualPaneSplitRule(/* context= */ context,
                /* primaryComponent= */ getComponentName(context, DeepLinkHomepageActivity.class),
                /* secondaryComponent= */ secondaryComponent,
                /* secondaryIntentAction= */ secondaryIntentAction,
                /* finishPrimaryWithSecondary= */ SplitRule.FinishBehavior.ALWAYS,
                /* finishSecondaryWithPrimary= */ SplitRule.FinishBehavior.ALWAYS,
                /* clearTop= */ clearTop);
        registerDualPaneSplitRule(/* context= */ context,
                /* primaryComponent= */ getComponentName(context,
                        ActivityEmbeddingUtils.getHomepageActivityAliasName(context)),
                /* secondaryComponent= */ secondaryComponent,
                /* secondaryIntentAction= */ secondaryIntentAction,
                /* finishPrimaryWithSecondary= */ SplitRule.FinishBehavior.ALWAYS,
                /* finishSecondaryWithPrimary= */ SplitRule.FinishBehavior.ALWAYS,
                /* clearTop= */ clearTop);
    }

    /**
     * Set up all embedding rules.
     */
    public void initActivityEmbeddingRules() {
        if (!ActivityEmbeddingUtils.isEmbeddingActivityEnabled(mContext)) {
            LOG.d("Embedding is not enabled for Settings.");
            return;
        }
        mRuleController.clearRules();
        registerDefaultAlwaysExpandRules();
        registerHomepagePlaceholderRule();
        registerSubSettingsActivityPairRule();
    }

    private void registerDefaultAlwaysExpandRules() {
        final Set<ActivityFilter> activityFilters = new HashSet<>();
        ActivityRule activityRule = new ActivityRule.Builder(activityFilters)
                .setAlwaysExpand(true)
                .build();
        addActivityIntentFilter(activityFilters, new Intent(Settings.ACTION_APP_SEARCH_SETTINGS));
        addActivityClassFilter(activityFilters, ManageDomainUrlsActivity.class);
        addActivityClassFilter(activityFilters, SettingsScreenLockActivity.class);
        addActivityClassFilter(activityFilters, CheckLockActivity.class);
        addActivityClassFilter(activityFilters, VerifyLockChangeActivity.class);
        addActivityClassFilter(activityFilters, ChooseLockPatternActivity.class);
        addActivityClassFilter(activityFilters, ChooseLockPinActivity.class);
        addActivityClassFilter(activityFilters, ChooseLockPasswordActivity.class);
        addActivityClassFilter(activityFilters, ProfileSwitcherActivity.class);
        addActivityClassFilter(activityFilters, DeviceAdminAddActivity.class);
        addActivityClassFilter(activityFilters, EnterprisePrivacySettingsActivity.class);
        RuleController.getInstance(mContext).addRule(activityRule);
    }

    /**
     * Registers placeholders to launch {@link CarSettingActivities.HomepageActivity} and
     * {@link CarSettingActivities.BluetoothSettingsActivity} together without animation.
     */
    private void registerHomepagePlaceholderRule() {
        final Set<ActivityFilter> activityFilters = new HashSet<>();
        addActivityClassFilter(activityFilters, CarSettingActivities.HomepageActivity.class);
        addActivityClassFilter(activityFilters, DeepLinkHomepageActivity.class);
        activityFilters.add(new ActivityFilter(new ComponentName(mContext,
                ActivityEmbeddingUtils.getHomepageActivityAliasName(mContext)), null));

        final Intent startUpIntent = ActivityEmbeddingUtils.getPlaceholderIntent(mContext);
        SplitAttributes attributes = new SplitAttributes.Builder()
                .setSplitType(SplitAttributes.SplitType.ratio(
                        ActivityEmbeddingUtils.getSplitRatio(mContext)))
                .build();
        final SplitPlaceholderRule placeholderRule = new SplitPlaceholderRule.Builder(
                activityFilters, startUpIntent)
                .setMinWidthDp(ActivityEmbeddingUtils.getMinCurrentScreenSplitWidthDp(mContext))
                .setMinSmallestWidthDp(
                        ActivityEmbeddingUtils.getMinSmallestScreenSplitWidthDp())
                .setMaxAspectRatioInPortrait(EmbeddingAspectRatio.ALWAYS_ALLOW)
                .setSticky(false)
                .setFinishPrimaryWithPlaceholder(SplitRule.FinishBehavior.ADJACENT)
                .setDefaultSplitAttributes(attributes)
                .build();

        mRuleController.addRule(placeholderRule);
    }

    /**
     * Registers rule for splitting all {@link SubSettingsActivity} activity to the right of
     * {@link CarSettingActivities.HomepageActivity}.
     */
    private void registerSubSettingsActivityPairRule() {
        registerHomepageDualPaneSplitRule(/* context= */ mContext,
                /* secondaryComponent= */ getComponentName(mContext, SubSettingsActivity.class),
                /* secondaryIntentAction= */ null,
                /* clearTop= */ true);
    }

    private static ComponentName getComponentName(
            Context context, Class<? extends Activity> activityClass) {
        return getComponentName(context, activityClass.getName());
    }

    private static ComponentName getComponentName(Context context, String activityClassName) {
        return new ComponentName(context.getPackageName(), activityClassName);
    }

    private static void addActivityIntentFilter(Set<ActivityFilter> activityFilters,
            Intent intent) {
        activityFilters.add(new ActivityFilter(COMPONENT_NAME_WILDCARD, intent.getAction()));
    }

    private void addActivityClassFilter(Set<ActivityFilter> activityFilters,
            Class<? extends Activity> activityClass) {
        activityFilters.add(new ActivityFilter(new ComponentName(mContext, activityClass), null));
    }
}
