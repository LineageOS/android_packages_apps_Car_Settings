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

package com.android.car.settings.common;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.android.car.settings.testutils.BaseTestSettingsFragment;
import com.android.car.settings.testutils.TestSubSettingsActivity;
import com.android.car.ui.toolbar.NavButtonMode;
import com.android.car.ui.toolbar.ToolbarController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class DualPaneSubSettingsActivityTest {
    Context mContext = ApplicationProvider.getApplicationContext();
    TestSubSettingsActivity mActivity;
    FragmentManager mFragmentManager;

    @Rule
    public ActivityTestRule<TestSubSettingsActivity> mActivityTestRule =
            new ActivityTestRule<>(TestSubSettingsActivity.class);

    @Before
    public void setUp() throws Throwable {
        mActivity = mActivityTestRule.getActivity();
        mFragmentManager = mActivity.getSupportFragmentManager();
    }

    @Test
    public void onPreferenceStartFragment_updateFragmentContainer() throws Throwable {
        Preference pref = new Preference(mContext);
        pref.setFragment(BaseTestSettingsFragment.class.getName());

        SubSettingsActivity spiedActivity = spy(mActivity);
        verify(spiedActivity, never()).updateFragmentContainer(any(Fragment.class));

        mActivityTestRule.runOnUiThread(() ->
                spiedActivity.onPreferenceStartFragment(/* caller= */ null, pref));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        verify(spiedActivity).updateFragmentContainer(any(Fragment.class));
    }

    @Test
    public void onUxRestrictionsChanged_topFragmentInBackStackHasUpdatedUxRestrictions()
            throws Throwable {
        CarUxRestrictions oldUxRestrictions = new CarUxRestrictions.Builder(
                /* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE,
                /* timestamp= */ 0
        ).build();

        CarUxRestrictions newUxRestrictions = new CarUxRestrictions.Builder(
                /* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP,
                /* timestamp= */ 0
        ).build();

        AtomicReference<BaseTestSettingsFragment> fragmentA = new AtomicReference<>();
        AtomicReference<BaseTestSettingsFragment> fragmentB = new AtomicReference<>();

        mActivityTestRule.runOnUiThread(() -> {
            fragmentA.set(new BaseTestSettingsFragment());
            fragmentB.set(new BaseTestSettingsFragment());
            mActivity.launchFragment(fragmentA.get());
            mActivity.onUxRestrictionsChanged(oldUxRestrictions);
            mActivity.launchFragment(fragmentB.get());
            mActivity.onUxRestrictionsChanged(newUxRestrictions);
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(
                fragmentB.get().getUxRestrictions().isSameRestrictions(newUxRestrictions)).isTrue();
    }

    @Test
    public void onBackStackChanged_uxRestrictionsChanged_currentFragmentHasUpdatedUxRestrictions()
            throws Throwable {
        CarUxRestrictions oldUxRestrictions = new CarUxRestrictions.Builder(
                /* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE,
                /* timestamp= */ 0
        ).build();

        CarUxRestrictions newUxRestrictions = new CarUxRestrictions.Builder(
                /* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP,
                /* timestamp= */ 0
        ).build();

        AtomicReference<BaseTestSettingsFragment> fragmentA = new AtomicReference<>();
        AtomicReference<BaseTestSettingsFragment> fragmentB = new AtomicReference<>();

        mActivityTestRule.runOnUiThread(() -> {
            fragmentA.set(new BaseTestSettingsFragment());
            fragmentB.set(new BaseTestSettingsFragment());
            mActivity.launchFragment(fragmentA.get());
            mActivity.onUxRestrictionsChanged(oldUxRestrictions);
            mActivity.launchFragment(fragmentB.get());
            mActivity.onUxRestrictionsChanged(newUxRestrictions);
            mActivity.goBack();
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(
                fragmentA.get().getUxRestrictions().isSameRestrictions(newUxRestrictions)).isTrue();
    }

    @Test
    public void getToolbar_updateFragmentContainer_navButtonAlwaysBack() throws Throwable {
        ToolbarController toolbar = mActivity.getToolbar();
        assertThat(toolbar.getNavButtonMode()).isEquivalentAccordingToCompareTo(
                NavButtonMode.BACK);

        BaseTestSettingsFragment fragment1 = new BaseTestSettingsFragment();
        mActivityTestRule.runOnUiThread(() -> {
            mActivity.updateFragmentContainer(fragment1);
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        mActivityTestRule.runOnUiThread(() -> {
            fragment1.setupToolbar(fragment1.getToolbar());
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(toolbar.getNavButtonMode()).isEquivalentAccordingToCompareTo(
                NavButtonMode.BACK);


        assertThat(toolbar.getNavButtonMode()).isEqualTo(NavButtonMode.BACK);

        mActivityTestRule.runOnUiThread(() -> mActivity.goBack());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(toolbar.getNavButtonMode()).isEquivalentAccordingToCompareTo(
                NavButtonMode.BACK);
    }
}
