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

import static android.view.ViewGroup.FOCUS_BEFORE_DESCENDANTS;
import static android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS;

import static com.android.car.ui.core.CarUi.requireToolbar;

import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.car.drivingstate.CarUxRestrictionsManager.OnUxRestrictionsChangedListener;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.android.car.apps.common.util.Themes;
import com.android.car.settings.R;
import com.android.car.ui.baselayout.Insets;
import com.android.car.ui.baselayout.InsetsChangedListener;
import com.android.car.ui.core.CarUi;
import com.android.car.ui.toolbar.MenuItem;
import com.android.car.ui.toolbar.Toolbar;
import com.android.car.ui.toolbar.ToolbarController;

import java.util.Collections;
import java.util.List;

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
    private static final int SEARCH_REQUEST_CODE = 501;
    private static final String KEY_HAS_NEW_INTENT = "key_has_new_intent";

    private boolean mHasNewIntent = true;
    private String mTopLevelHeaderKey;
    private boolean mIsSinglePane;

    private ToolbarController mGlobalToolbar;
    private ToolbarController mMiniToolbar;

    private CarUxRestrictionsHelper mUxRestrictionsHelper;
    private ViewGroup mFragmentContainer;
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
        if (savedInstanceState != null) {
            mHasNewIntent = savedInstanceState.getBoolean(KEY_HAS_NEW_INTENT, mHasNewIntent);
        }
        populateMetaData();
        setContentView(R.layout.car_setting_activity);
        mFragmentContainer = findViewById(R.id.fragment_container);
        if (mUxRestrictionsHelper == null) {
            mUxRestrictionsHelper = new CarUxRestrictionsHelper(/* context= */ this, /* listener= */
                    this);
        }
        mUxRestrictionsHelper.start();

        // We do this so that the insets are not automatically sent to the fragments.
        // The fragments have their own insets handled by the installBaseLayoutAround() method.
        CarUi.replaceInsetsChangedListenerWith(this, this);

        setUpToolbars();
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        mRestrictedMessage = findViewById(R.id.restricted_message);

        if (mHasNewIntent) {
            launchIfDifferent(getInitialFragment());
            mHasNewIntent = false;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_HAS_NEW_INTENT, mHasNewIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mUxRestrictionsHelper.stop();
        mUxRestrictionsHelper = null;
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
                .replace(R.id.fragment_container, fragment,
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
        if (mIsSinglePane) {
            return mGlobalToolbar;
        }
        return mMiniToolbar;
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

        if (!mIsSinglePane) {
            // Update restrictions for top level menu (if present).
            Fragment topLevelMenu =
                    getSupportFragmentManager().findFragmentById(R.id.top_level_menu);
            if (topLevelMenu instanceof CarUxRestrictionsManager.OnUxRestrictionsChangedListener) {
                ((CarUxRestrictionsManager.OnUxRestrictionsChangedListener) topLevelMenu)
                        .onUxRestrictionsChanged(restrictionInfo);
            }
        }
    }

    @Override
    public CarUxRestrictions getCarUxRestrictions() {
        return mCarUxRestrictions;
    }

    @Override
    public void onBackStackChanged() {
        onUxRestrictionsChanged(getCarUxRestrictions());
        if (!mIsSinglePane) {
            if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
                mMiniToolbar.setState(Toolbar.State.SUBPAGE);
                mMiniToolbar.setNavButtonMode(Toolbar.NavButtonMode.BACK);
            } else {
                mMiniToolbar.setState(Toolbar.State.HOME);
            }
        }
    }

    @Override
    public void onCarUiInsetsChanged(Insets insets) {
        findViewById(R.id.car_settings_activity_wrapper).setPadding(insets.getLeft(),
                insets.getTop(), insets.getRight(), insets.getBottom());
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

    protected void launchIfDifferent(Fragment newFragment) {
        Fragment currentFragment = getCurrentFragment();
        if ((newFragment != null) && differentFragment(newFragment, currentFragment)) {
            LOG.d("launchIfDifferent: " + newFragment + " replacing " + currentFragment);
            launchFragment(newFragment);
        }
    }

    protected Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    /**
     * Returns {code true} if newFragment is different from current fragment.
     */
    private boolean differentFragment(Fragment newFragment, Fragment currentFragment) {
        return (currentFragment == null)
                || (!currentFragment.getClass().equals(newFragment.getClass()));
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
    }

    private void updateBlockingView(@Nullable Fragment currentFragment) {
        if (currentFragment instanceof BaseFragment
                && !((BaseFragment) currentFragment).canBeShown(mCarUxRestrictions)) {
            mRestrictedMessage.setVisibility(View.VISIBLE);
            mFragmentContainer.setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
            mFragmentContainer.clearFocus();
            hideKeyboard();
        } else {
            mRestrictedMessage.setVisibility(View.GONE);
            mFragmentContainer.setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS);
        }
    }

    private void populateMetaData() {
        try {
            ActivityInfo ai = getPackageManager().getActivityInfo(getComponentName(),
                    PackageManager.GET_META_DATA);
            if (ai == null || ai.metaData == null) return;
            mTopLevelHeaderKey = ai.metaData.getString(META_DATA_KEY_HEADER_KEY);
            mIsSinglePane = ai.metaData.getBoolean(META_DATA_KEY_SINGLE_PANE,
                    getResources().getBoolean(R.bool.config_global_force_single_pane));
        } catch (PackageManager.NameNotFoundException e) {
            LOG.w("Unable to find package", e);
        }
    }

    private void setUpToolbars() {
        mGlobalToolbar = requireToolbar(this);
        mGlobalToolbar.setState(Toolbar.State.SUBPAGE);
        if (mIsSinglePane) {
            findViewById(R.id.top_level_menu).setVisibility(View.GONE);
            return;
        }
        mMiniToolbar = CarUi.installBaseLayoutAround(
                findViewById(R.id.fragment_container_wrapper),
                insets -> findViewById(R.id.fragment_container_wrapper).setPadding(
                        insets.getLeft(), insets.getTop(), insets.getRight(),
                        insets.getBottom()), /* hasToolbar= */ true);

        MenuItem searchButton = new MenuItem.Builder(this)
                .setToSearch()
                .setOnClickListener(i -> onSearchButtonClicked())
                .setUxRestrictions(CarUxRestrictions.UX_RESTRICTIONS_NO_KEYBOARD)
                .setId(R.id.toolbar_menu_item_0)
                .build();
        List<MenuItem> items = Collections.singletonList(searchButton);

        mGlobalToolbar.setTitle(R.string.settings_label);
        mGlobalToolbar.setState(Toolbar.State.SUBPAGE);
        mGlobalToolbar.setNavButtonMode(Toolbar.NavButtonMode.CLOSE);
        mGlobalToolbar.registerOnBackListener(() -> {
            finish();
            return true;
        });
        mGlobalToolbar.setLogo(R.drawable.ic_launcher_settings);
        mGlobalToolbar.setMenuItems(items);
    }

    private void onSearchButtonClicked() {
        Intent intent = new Intent(Settings.ACTION_APP_SEARCH_SETTINGS)
                .setPackage(getSettingsIntelligencePkgName());
        if (intent.resolveActivity(getPackageManager()) == null) {
            return;
        }
        startActivityForResult(intent, SEARCH_REQUEST_CODE);
    }

    private String getSettingsIntelligencePkgName() {
        return getString(R.string.config_settingsintelligence_package_name);
    }
}
