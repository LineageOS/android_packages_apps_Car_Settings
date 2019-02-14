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
 * limitations under the License.
 */

package com.android.car.settings.common;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertThrows;

import android.car.Car;
import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.datetime.DatetimeSettingsFragment;
import com.android.car.settings.testutils.DummyFragment;
import com.android.car.settings.testutils.ShadowCar;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;

/** Unit test for {@link CarSettingActivity}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
public class CarSettingActivityTest {

    private static final String TEST_TAG = "test_tag";

    private Context mContext;
    private ActivityController<CarSettingActivity> mActivityController;
    private CarSettingActivity mActivity;

    @Mock
    private CarUxRestrictionsManager mMockCarUxRestrictionsManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ShadowCar.setCarManager(Car.CAR_UX_RESTRICTION_SERVICE, mMockCarUxRestrictionsManager);
        mContext = RuntimeEnvironment.application;
        mActivityController = ActivityController.of(new CarSettingActivity());
        mActivity = mActivityController.get();
        mActivityController.create();

        CarUxRestrictions noSetupRestrictions = new CarUxRestrictions.Builder(
                true, CarUxRestrictions.UX_RESTRICTIONS_BASELINE, 0).build();

        when(mMockCarUxRestrictionsManager.getCurrentCarUxRestrictions())
                .thenReturn(noSetupRestrictions);
    }

    @Test
    public void launchWithIntent_resolveToFragment() {
        MockitoAnnotations.initMocks(this);
        Intent intent = new Intent("android.settings.DATE_SETTINGS");
        CarSettingActivity activity =
                Robolectric.buildActivity(CarSettingActivity.class, intent).setup().get();
        assertThat(activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container))
                .isInstanceOf(DatetimeSettingsFragment.class);
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
        mActivityController.start().postCreate(null).resume();
        TestFragment testFragment = new TestFragment();
        mActivity.launchFragment(testFragment);
        assertThat(mActivity.getFragment()).isEqualTo(testFragment);

        mActivity.onNewIntent(new Intent(Settings.ACTION_SETTINGS));
        mActivity.onResume(); // Should launch default.

        assertThat(mActivity.getFragment()).isNotEqualTo(testFragment);
    }

    @Test
    public void onResume_savedInstanceState_doesNotLaunchFragmentFromOldIntent() {
        mActivityController.start().postCreate(null).resume();
        Intent intent = new Intent(Settings.ACTION_SETTINGS);
        mActivity.onNewIntent(intent);
        assertThat(mActivity.getFragment()).isNotInstanceOf(TestFragment.class);
        mActivity.onResume(); // Should launch default. (old intent)
        mActivity.launchFragment(new TestFragment()); // Replace with test fragment.

        // Recreate with saved state (e.g. during config change).
        Bundle outState = new Bundle();
        mActivityController.pause().saveInstanceState(outState);
        mActivityController = ActivityController.of(new CarSettingActivity(), intent);
        mActivityController.setup(outState);

        // Should still display most recently launched fragment.
        assertThat(mActivityController.get().getFragment()).isInstanceOf(TestFragment.class);
    }

    @Test
    public void onPreferenceStartFragment_launchesFragment() {
        Preference pref = new Preference(mContext);
        pref.setFragment(TestFragment.class.getName());

        mActivity.onPreferenceStartFragment(/* caller= */ null, pref);

        assertThat(mActivity.getSupportFragmentManager().findFragmentById(
                R.id.fragment_container)).isInstanceOf(TestFragment.class);
    }

    @Test
    public void launchFragment_rootFragment_clearsBackStack() {
        // Add fragment 1
        TestFragment testFragment1 = new TestFragment();
        mActivity.launchFragment(testFragment1);

        // Add fragment 2
        TestFragment testFragment2 = new TestFragment();
        mActivity.launchFragment(testFragment2);

        // Add root fragment
        Fragment root = Fragment.instantiate(mContext,
                mContext.getString(R.string.config_settings_hierarchy_root_fragment));
        mActivity.launchFragment(root);

        assertThat(mActivity.getSupportFragmentManager().getBackStackEntryCount())
                .isEqualTo(1);
    }

    @Test
    public void launchFragment_dialogFragment_throwsError() {
        DialogFragment dialogFragment = new DialogFragment();

        assertThrows(IllegalArgumentException.class,
                () -> mActivity.launchFragment(dialogFragment));
    }

    @Test
    public void showDialog_launchDialogFragment_noTag() {
        DialogFragment dialogFragment = mock(DialogFragment.class);
        mActivity.showDialog(dialogFragment, /* tag */ null);
        verify(dialogFragment).show(mActivity.getSupportFragmentManager(), null);
    }

    @Test
    public void showDialog_launchDialogFragment_withTag() {
        DialogFragment dialogFragment = mock(DialogFragment.class);
        mActivity.showDialog(dialogFragment, TEST_TAG);
        verify(dialogFragment).show(mActivity.getSupportFragmentManager(), TEST_TAG);
    }

    @Test
    public void findDialogByTag_retrieveOriginalDialog() {
        DialogFragment dialogFragment = new DialogFragment();
        mActivity.showDialog(dialogFragment, TEST_TAG);
        assertThat(mActivity.findDialogByTag(TEST_TAG)).isEqualTo(dialogFragment);
    }

    @Test
    public void findDialogByTag_notDialogFragment() {
        TestFragment fragment = new TestFragment();
        mActivity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                fragment, TEST_TAG).commit();
        assertThat(mActivity.findDialogByTag(TEST_TAG)).isNull();
    }

    @Test
    public void findDialogByTag_noSuchFragment() {
        assertThat(mActivity.findDialogByTag(TEST_TAG)).isNull();
    }

    /** Simple Fragment for testing use. */
    public static class TestFragment extends Fragment {
    }
}
