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

package com.android.car.settings.common;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import static com.android.car.settings.deeplink.DeepLinkHomepageActivity.EXTRA_TARGET_SECONDARY_CONTAINER;
import static com.android.car.settings.deeplink.DeepLinkHomepageActivity.convertToDeepLinkHomepageIntent;

import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager.OnUxRestrictionsChangedListener;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.android.car.apps.common.util.Themes;
import com.android.car.settings.R;
import com.android.car.settings.activityembedding.ActivityEmbeddingUtils;
import com.android.car.ui.baselayout.Insets;
import com.android.car.ui.baselayout.InsetsChangedListener;
import com.android.car.ui.core.CarUi;
import com.android.car.ui.toolbar.NavButtonMode;
import com.android.car.ui.toolbar.ToolbarController;
import com.android.settingslib.core.lifecycle.HideNonSystemOverlayMixin;

/**
 * Base activity class for car settings, provides a action bar with a back button that goes to
 * previous activity.
 */
public abstract class BaseCarSettingsActivity extends FragmentActivity implements
        FragmentHost, OnUxRestrictionsChangedListener, UxRestrictionsProvider,
        OnBackStackChangedListener, PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        InsetsChangedListener {

    /**
     * Meta data key for specifying the preference key of the top level menu preference that the
     * initial activity's fragment falls under. If this is not specified in the activity's
     * metadata, the top level menu preference will not be highlighted upon activity launch.
     */
    public static final String META_DATA_KEY_HEADER_KEY =
            "com.android.car.settings.TOP_LEVEL_HEADER_KEY";

    /**
     * Meta data key for specifying activities that should always be shown in the single pane
     * configuration. If not specified for the activity, the activity will default to the value
     * {@link R.bool.config_global_force_single_pane}.
     */
    public static final String META_DATA_KEY_SINGLE_PANE = "com.android.car.settings.SINGLE_PANE";

    private static final Logger LOG = new Logger(BaseCarSettingsActivity.class);

    private String mTopLevelHeaderKey;
    private boolean mIsSinglePane;

    private ToolbarController mToolbar;

    private CarUxRestrictionsHelper mUxRestrictionsHelper;
    private View mRestrictedMessage;
    // Default to minimum restriction.
    private CarUxRestrictions mCarUxRestrictions = new CarUxRestrictions.Builder(
            /* reqOpt= */ true,
            CarUxRestrictions.UX_RESTRICTIONS_BASELINE,
            /* timestamp= */ 0
    ).build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        populateMetaData();
        // When dual-pane is enabled, all activity-filter Intents into Settings should be relaunched
        // into the secondary container with the exception of HomepageActivity.
        // For any instance of BaseCarSettingsActivity, if its start-up Intent meets the conditions
        // for deep link, trampoline it and restart the activity on the secondary container.
        if (shouldUseSecondaryPaneForActivity()) {
            startActivity(convertToDeepLinkHomepageIntent(getIntent()));
            finish();
            return;
        }
        getLifecycle().addObserver(new HideNonSystemOverlayMixin(this));
        setContentView(this instanceof CarSettingActivities.HomepageActivity
                ? R.layout.homepage_activity : R.layout.car_setting_activity);

        // We do this so that the insets are not automatically sent to the fragments.
        // The fragments have their own insets handled by the installBaseLayoutAround() method.
        CarUi.replaceInsetsChangedListenerWith(this, this);

        setUpToolbarAndDivider();
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        mRestrictedMessage = findViewById(R.id.restricted_message);
        mUxRestrictionsHelper = new CarUxRestrictionsHelper(/* context= */ this, /* listener= */
                this);

        handleNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNewIntent(intent);
    }

    /**
     * Handles when an intent being processed by this class, and should be called every time a new
     * {@code Intent} is received by this Activity, including during {@link #onCreate(Bundle)}
     * when this Activity first starts, and during subsequent calls to {@link #onNewIntent(Intent)}.
     */
    protected void handleNewIntent(Intent intent) {
        launchIfDifferent(getInitialFragment());
    }

    private boolean shouldUseSecondaryPaneForActivity() {
        if (!ActivityEmbeddingUtils.isEmbeddingActivityEnabled(this)) {
            return false;
        }
        // Homepage and deeplink activity should never be hosted on the secondary pane.
        if (this instanceof CarSettingActivities.HomepageActivity) {
            return false;
        }
        // All deeplink intents are received via intent-filter so getAction must not be null.
        // Only starts trampoline for deep link intents. Should return false for all the cases that
        // CarSettings app starts a SubSettingsActivity.
        if (getIntent().getAction() == null) {
            return false;
        }
        // If the activity's launch mode is "singleInstance", it can't be embedded in Settings since
        // it will always be created in a new task.
        ActivityInfo info = getIntent().resolveActivityInfo(getPackageManager(),
                PackageManager.MATCH_DEFAULT_ONLY);
        if (info.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
            return false;
        }
        // If the activity metadata is configured to be single pane, it should be directly shown.
        info = getActivityInfo(getPackageManager(), getComponentName());
        if (info != null && info.metaData != null
                && info.metaData.getBoolean(META_DATA_KEY_SINGLE_PANE, false)) {
            return false;
        }
        // This intent has already been restarted as deeplink intent, or was launched by another
        // activity already embedded on the secondary pane.
        if (getIntent().getBooleanExtra(EXTRA_TARGET_SECONDARY_CONTAINER, false)) {
            return false;
        }
        return true;
    }

    private void populateMetaData() {
        ActivityInfo ai = getActivityInfo(getPackageManager(), getComponentName());
        mIsSinglePane = !ActivityEmbeddingUtils.isEmbeddingSplitActivated(this);
        if (ai != null && ai.metaData != null) {
            setTopLevelHeaderKey(ai.metaData.getString(META_DATA_KEY_HEADER_KEY));
            mIsSinglePane = ai.metaData.getBoolean(META_DATA_KEY_SINGLE_PANE, mIsSinglePane);
        }
    }

    protected String getTopLevelHeaderKey() {
        return mTopLevelHeaderKey;
    }

    protected void setTopLevelHeaderKey(@Nullable String key) {
        mTopLevelHeaderKey = key;
    }

    private void launchIfDifferent(Fragment newFragment) {
        Fragment currentFragment = getCurrentFragment();
        if ((newFragment != null) && differentFragment(newFragment, currentFragment)) {
            updateFragmentContainer(newFragment);
        }
    }

    private boolean differentFragment(Fragment newFragment, Fragment currentFragment) {
        return (currentFragment == null)
                || (!currentFragment.getClass().equals(newFragment.getClass()));
    }

    @Override
    public void onDestroy() {
        if (mUxRestrictionsHelper != null) {
            mUxRestrictionsHelper.destroy();
            mUxRestrictionsHelper = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        hideKeyboard();
        // If the backstack is empty, finish the activity.
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            finish();
        }
    }

    @Override
    public Intent getIntent() {
        Intent superIntent = super.getIntent();
        if (mTopLevelHeaderKey != null) {
            superIntent.putExtra(META_DATA_KEY_HEADER_KEY, mTopLevelHeaderKey);
        }
        superIntent.putExtra(META_DATA_KEY_SINGLE_PANE, mIsSinglePane);
        return superIntent;
    }

    @Override
    public void launchFragment(Fragment fragment) {
        if (fragment instanceof DialogFragment) {
            throw new IllegalArgumentException(
                    "cannot launch dialogs with launchFragment() - use showDialog() instead");
        }
        if (mIsSinglePane || this instanceof SubSettingsActivity) {
            updateFragmentContainer(fragment);
        } else {
            Intent intent = SubSettingsActivity.newInstance(this, fragment);
            setIntent(intent);
            startActivity(intent);
        }
    }

    protected void updateFragmentContainer(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        Themes.getAttrResourceId(/* context= */ this,
                                android.R.attr.fragmentOpenEnterAnimation),
                        Themes.getAttrResourceId(/* context= */ this,
                                android.R.attr.fragmentOpenExitAnimation),
                        Themes.getAttrResourceId(/* context= */ this,
                                android.R.attr.fragmentCloseEnterAnimation),
                        Themes.getAttrResourceId(/* context= */ this,
                                android.R.attr.fragmentCloseExitAnimation))
                .replace(getFragmentContainerId(), fragment,
                        Integer.toString(getSupportFragmentManager().getBackStackEntryCount()))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void goBack() {
        onBackPressed();
    }

    @Override
    public void showBlockingMessage() {
        Toast.makeText(this, R.string.restricted_while_driving, Toast.LENGTH_SHORT).show();
    }

    @Override
    public ToolbarController getToolbar() {
        return mToolbar;
    }

    @Override
    public void onUxRestrictionsChanged(CarUxRestrictions restrictionInfo) {
        mCarUxRestrictions = restrictionInfo;

        // Update restrictions for current fragment.
        Fragment currentFragment = getCurrentFragment();
        if (currentFragment instanceof OnUxRestrictionsChangedListener) {
            ((OnUxRestrictionsChangedListener) currentFragment)
                    .onUxRestrictionsChanged(restrictionInfo);
        }
        updateBlockingView(currentFragment);
    }

    @Override
    public CarUxRestrictions getCarUxRestrictions() {
        return mCarUxRestrictions;
    }

    @Override
    public void onBackStackChanged() {
        onUxRestrictionsChanged(getCarUxRestrictions());
    }

    @Override
    public void onCarUiInsetsChanged(Insets insets) {
        // intentional no-op - insets are handled by the listeners created during toolbar setup
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        if (pref.getFragment() != null) {
            Fragment fragment = Fragment.instantiate(/* context= */ this, pref.getFragment(),
                    pref.getExtras());
            launchFragment(fragment);
            return true;
        }
        return false;
    }

    /**
     * Gets the fragment to show onCreate. If null, the activity will not perform an initial
     * fragment transaction.
     */
    @Nullable
    protected abstract Fragment getInitialFragment();

    protected Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(getFragmentContainerId());
    }

    private int getFragmentContainerId() {
        return this instanceof CarSettingActivities.HomepageActivity
                ? R.id.top_level_menu_container : R.id.fragment_container;
    }


    private void updateBlockingView(@Nullable Fragment currentFragment) {
        if (mRestrictedMessage == null) {
            return;
        }
        if (currentFragment instanceof BaseFragment
                && !((BaseFragment) currentFragment).canBeShown(mCarUxRestrictions)) {
            mRestrictedMessage.setVisibility(View.VISIBLE);
            hideKeyboard();
        } else {
            mRestrictedMessage.setVisibility(View.GONE);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
    }

    private void setUpToolbarAndDivider() {
        boolean isHomepageActivity = this instanceof CarSettingActivities.HomepageActivity;
        if (isHomepageActivity && !ActivityEmbeddingUtils.isEmbeddingSplitActivated(this)) {
            findViewById(R.id.top_level_menu_container).setLayoutParams(
                    new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
            findViewById(R.id.top_level_divider).setVisibility(View.GONE);
        }
        View globalToolbarWrappedView = findViewById(isHomepageActivity
                ? R.id.top_level_menu_container : R.id.fragment_container_wrapper);
        mToolbar = CarUi.installBaseLayoutAround(
                globalToolbarWrappedView,
                insets -> globalToolbarWrappedView.setPadding(
                        insets.getLeft(), insets.getTop(), insets.getRight(),
                        insets.getBottom()), /* hasToolbar= */ true);
        mToolbar.setNavButtonMode(NavButtonMode.BACK);
    }

    /**
     * Returns the ActivityInfo of the given componentName.
     */
    @Nullable
    public ActivityInfo getActivityInfo(PackageManager pm, ComponentName componentName) {
        try {
            return pm.getActivityInfo(componentName, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            LOG.w("Unable to find package", e);
        }
        return null;
    }
}
