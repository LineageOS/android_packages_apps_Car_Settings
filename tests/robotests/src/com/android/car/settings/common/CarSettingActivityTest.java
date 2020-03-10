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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.app.AlertDialog;
import android.app.Dialog;
import android.car.Car;
import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.android.car.settings.R;
import com.android.car.settings.datetime.DatetimeSettingsFragment;
import com.android.car.settings.testutils.DummyFragment;
import com.android.car.settings.testutils.ShadowCar;
import com.android.car.ui.core.testsupport.CarUiInstallerRobolectric;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;

/** Unit test for {@link CarSettingActivity}. */
@RunWith(RobolectricTestRunner.class)
public class CarSettingActivityTest {

    private Context mContext;
    private ActivityController<CarSettingActivity> mActivityController;
    private CarSettingActivity mActivity;

    @Mock
    private CarUxRestrictionsManager mMockCarUxRestrictionsManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Robolectric.getForegroundThreadScheduler().pause();
        CarUxRestrictions noSetupRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* time= */ 0).build();
        when(mMockCarUxRestrictionsManager.getCurrentCarUxRestrictions())
                .thenReturn(noSetupRestrictions);
        ShadowCar.setCarManager(Car.CAR_UX_RESTRICTION_SERVICE, mMockCarUxRestrictionsManager);
        mContext = RuntimeEnvironment.application;
        mActivityController = ActivityController.of(new CarSettingActivity());
        mActivity = mActivityController.get();
        mActivityController.setup();

        // Needed to install Install CarUiLib BaseLayouts Toolbar for test activity
        CarUiInstallerRobolectric.install();
    }

    @Test
    public void launchWithIntent_resolveToFragment() {
        MockitoAnnotations.initMocks(this);
        Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
        CarSettingActivity activity =
                Robolectric.buildActivity(CarSettingActivity.class, intent).setup().get();
        assertThat(activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container))
                .isInstanceOf(DatetimeSettingsFragment.class);
    }

    @Test
    public void launchWithIntent_hasNoBackstack() {
        MockitoAnnotations.initMocks(this);
        Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
        CarSettingActivity activity =
                Robolectric.buildActivity(CarSettingActivity.class, intent).setup().get();
        assertThat(activity.getSupportFragmentManager().getBackStackEntryCount()).isEqualTo(1);
    }

    @Test
    public void launchWithIntentFromExternalPackage_hasNoBackstack() {
        MockitoAnnotations.initMocks(this);
        Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
        intent.putExtra(Intent.EXTRA_CALLING_PACKAGE, "com.test.package");
        CarSettingActivity activity =
                Robolectric.buildActivity(CarSettingActivity.class, intent).setup().get();
        assertThat(activity.getSupportFragmentManager().getBackStackEntryCount()).isEqualTo(1);
    }

    @Test
    public void launchWithIntentFromSettings_hasBackstack() {
        MockitoAnnotations.initMocks(this);
        Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
        intent.putExtra(Intent.EXTRA_CALLING_PACKAGE, mContext.getPackageName());
        CarSettingActivity activity =
                Robolectric.buildActivity(CarSettingActivity.class, intent).setup().get();
        assertThat(activity.getSupportFragmentManager().getBackStackEntryCount()).isEqualTo(2);
    }

    @Test
    public void launchWithEmptyIntent_resolveToDefaultFragment() {
        CarSettingActivity activity =
                Robolectric.buildActivity(CarSettingActivity.class).setup().get();
        assertThat(activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container))
                .isInstanceOf(DummyFragment.class);
    }

    @Test
    public void onResume_newIntent_launchesNewFragment() {
        Robolectric.getForegroundThreadScheduler().unPause();

        TestFragment testFragment = new TestFragment();
        mActivity.launchFragment(testFragment);
        assertThat(mActivity.getCurrentFragment()).isEqualTo(testFragment);

        mActivity.onNewIntent(new Intent(Settings.ACTION_DATE_SETTINGS));
        mActivity.onResume();

        assertThat(mActivity.getCurrentFragment()).isNotEqualTo(testFragment);
    }

    @Test
    public void onResume_savedInstanceState_doesNotLaunchFragmentFromOldIntent() {
        Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
        mActivity.onNewIntent(intent);
        assertThat(mActivity.getCurrentFragment()).isNotInstanceOf(TestFragment.class);
        mActivity.onResume(); // Showing date time settings (old intent)
        mActivity.launchFragment(new TestFragment()); // Replace with test fragment.

        // Recreate with saved state (e.g. during config change).
        Bundle outState = new Bundle();
        mActivityController.pause().saveInstanceState(outState);
        mActivityController = ActivityController.of(new CarSettingActivity(), intent);
        mActivityController.setup(outState);

        // Should still display most recently launched fragment.
        assertThat(mActivityController.get().getCurrentFragment()).isInstanceOf(TestFragment.class);
    }

    @Test
    public void launchWithIntentNoPackage_clearsBackStack() {
        Robolectric.getForegroundThreadScheduler().unPause();
        // Add fragment 1
        TestFragment testFragment1 = new TestFragment();
        mActivity.launchFragment(testFragment1);

        // Add fragment 2
        TestFragment testFragment2 = new TestFragment();
        mActivity.launchFragment(testFragment2);

        mActivity.onNewIntent(new Intent(Settings.ACTION_DATE_SETTINGS));
        mActivity.onResume();

        assertThat(mActivity.getSupportFragmentManager().getBackStackEntryCount())
                .isEqualTo(1);
    }

    @Test
    public void launchWithIntentNoPackage_dismissesDialogs() {
        Robolectric.getForegroundThreadScheduler().unPause();
        // Add fragment 1
        TestFragment testFragment1 = new TestFragment();
        mActivity.launchFragment(testFragment1);

        // Show dialog 1
        String tag1 = "tag1";
        TestDialogFragment testDialogFragment1 = new TestDialogFragment();
        testDialogFragment1.show(mActivity.getSupportFragmentManager(), tag1);

        // Show dialog 2
        String tag2 = "tag2";
        TestDialogFragment testDialogFragment2 = new TestDialogFragment();
        testDialogFragment2.show(mActivity.getSupportFragmentManager(), tag2);

        assertThat(mActivity.getSupportFragmentManager().findFragmentByTag(tag1)).isNotNull();
        assertThat(mActivity.getSupportFragmentManager().findFragmentByTag(tag2)).isNotNull();

        mActivity.onNewIntent(new Intent(Settings.ACTION_DATE_SETTINGS));
        mActivity.onResume();

        assertThat(mActivity.getSupportFragmentManager().findFragmentByTag(tag1)).isNull();
        assertThat(mActivity.getSupportFragmentManager().findFragmentByTag(tag2)).isNull();
    }

    @Test
    public void launchWithIntentFromSettings_doesNotClearBackStack() {
        Robolectric.getForegroundThreadScheduler().unPause();
        // Add fragment 1
        TestFragment testFragment1 = new TestFragment();
        mActivity.launchFragment(testFragment1);

        // Add fragment 2
        TestFragment testFragment2 = new TestFragment();
        mActivity.launchFragment(testFragment2);

        Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
        intent.putExtra(Intent.EXTRA_CALLING_PACKAGE, mContext.getPackageName());
        mActivity.onNewIntent(intent);
        mActivity.onResume();

        assertThat(mActivity.getSupportFragmentManager().getBackStackEntryCount())
                .isGreaterThan(1);
    }

    @Test
    public void launchWithIntentFromSettings_dismissesDialogs() {
        Robolectric.getForegroundThreadScheduler().unPause();
        // Add fragment 1
        TestFragment testFragment1 = new TestFragment();
        mActivity.launchFragment(testFragment1);

        // Show dialog 1
        String tag1 = "tag1";
        TestDialogFragment testDialogFragment1 = new TestDialogFragment();
        testDialogFragment1.show(mActivity.getSupportFragmentManager(), tag1);

        // Show dialog 2
        String tag2 = "tag2";
        TestDialogFragment testDialogFragment2 = new TestDialogFragment();
        testDialogFragment2.show(mActivity.getSupportFragmentManager(), tag2);

        assertThat(mActivity.getSupportFragmentManager().findFragmentByTag(tag1)).isNotNull();
        assertThat(mActivity.getSupportFragmentManager().findFragmentByTag(tag2)).isNotNull();

        Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
        intent.putExtra(Intent.EXTRA_CALLING_PACKAGE, mContext.getPackageName());
        mActivity.onNewIntent(intent);
        mActivity.onResume();

        assertThat(mActivity.getSupportFragmentManager().findFragmentByTag(tag1)).isNull();
        assertThat(mActivity.getSupportFragmentManager().findFragmentByTag(tag2)).isNull();
    }

    @Test
    public void launchFragment_validIntent_clearsBackStack() {
        Robolectric.getForegroundThreadScheduler().unPause();

        // Add fragment 1
        TestFragment testFragment1 = new TestFragment();
        mActivity.launchFragment(testFragment1);

        // Add fragment 2
        TestFragment testFragment2 = new TestFragment();
        mActivity.launchFragment(testFragment2);

        Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
        mActivity.onNewIntent(intent);
        mActivity.launchFragment(new DatetimeSettingsFragment());

        assertThat(mActivity.getSupportFragmentManager().getBackStackEntryCount())
                .isEqualTo(1);
    }

    /** Simple Fragment for testing use. */
    public static class TestFragment extends Fragment {
    }

    /** Simple Dialog Fragment for testing use. */
    public static class TestDialogFragment extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            return new AlertDialog.Builder(getContext()).setTitle("Test Dialog").create();
        }
    }
}
