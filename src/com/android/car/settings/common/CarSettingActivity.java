/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.car.settings.common;

import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager.OnUxRestrictionsChangedListener;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.android.car.settings.R;
import com.android.car.theme.Themes;

/**
 * Base activity class for car settings, provides a action bar with a back button that goes to
 * previous activity.
 */
public class CarSettingActivity extends FragmentActivity implements FragmentController,
        OnUxRestrictionsChangedListener, UxRestrictionsProvider, OnBackStackChangedListener,
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private static final Logger LOG = new Logger(CarSettingActivity.class);

    public static final String META_DATA_KEY_FRAGMENT_CLASS =
            "com.android.car.settings.FRAGMENT_CLASS";

    private boolean mHasNewIntent = true;
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
        setContentView(R.layout.car_setting_activity);
        if (mUxRestrictionsHelper == null) {
            mUxRestrictionsHelper = new CarUxRestrictionsHelper(/* context= */ this, /* listener= */
                    this);
        }
        mUxRestrictionsHelper.start();
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        mRestrictedMessage = findViewById(R.id.restricted_message);

        launchIfDifferent(getFragment());
    }

    @Override
    public void onNewIntent(Intent intent) {
        LOG.d("onNewIntent" + intent);
        setIntent(intent);
        mHasNewIntent = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mHasNewIntent) {
            Fragment fragment = FragmentResolver.getFragmentForIntent(getIntent());
            launchIfDifferent(fragment);
            mHasNewIntent = false;
        }
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
        // if the backstack is empty, finish the activity.
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            finish();
        }
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
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void goBack() {
        onBackPressed();
    }

    @Override
    public void showBlockingMessage() {
        Toast.makeText(
                this, R.string.restricted_while_driving, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showDialog(DialogFragment dialogFragment, @Nullable String tag) {
        dialogFragment.show(getSupportFragmentManager(), tag);
    }

    @Override
    @Nullable
    public DialogFragment findDialogByTag(String tag) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (fragment instanceof DialogFragment) {
            return (DialogFragment) fragment;
        }
        return null;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode,
            ActivityResultCallback callback) {
        throw new UnsupportedOperationException(
                "Unimplemented for activities that implement FragmentController");
    }

    @Override
    public void startIntentSenderForResult(IntentSender intent, int requestCode,
            @Nullable Intent fillInIntent, int flagsMask, int flagsValues, Bundle options,
            ActivityResultCallback callback)
            throws IntentSender.SendIntentException {
        throw new UnsupportedOperationException(
                "Unimplemented for activities that implement FragmentController");
    }

    @Override
    public void onUxRestrictionsChanged(CarUxRestrictions restrictionInfo) {
        mCarUxRestrictions = restrictionInfo;
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
        updateBlockingView(getCurrentFragment());
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
     * Gets the fragment to show onCreate. This will only be launched if it is different from the
     * current fragment shown.
     */
    @Nullable
    protected Fragment getFragment() {
        if (getCurrentFragment() != null) {
            return getCurrentFragment();
        }
        return Fragment.instantiate(this,
                getString(R.string.config_settings_hierarchy_root_fragment));
    }

    private void launchIfDifferent(Fragment newFragment) {
        if ((newFragment != null) && differentFragment(newFragment, getCurrentFragment())) {
            launchFragment(newFragment);
        }
    }

    /**
     * Returns {code true} if newFragment is different from current fragment.
     */
    private boolean differentFragment(Fragment newFragment, Fragment currentFragment) {
        return (currentFragment == null)
                || (!currentFragment.getClass().equals(newFragment.getClass()));
    }

    private Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
    }

    private void updateBlockingView(@Nullable Fragment currentFragment) {
        if (currentFragment instanceof BaseFragment) {
            boolean canBeShown = ((BaseFragment) currentFragment).canBeShown(mCarUxRestrictions);
            mRestrictedMessage.setVisibility(canBeShown ? View.GONE : View.VISIBLE);
        }
    }
}
