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

import android.car.Car;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.Context;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.testutils.ShadowCar;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;

/** Unit test for {@link CarSettingActivity}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
public class CarSettingActivityTest {

    private Context mContext;
    private ActivityController<CarSettingActivity> mActivityController;
    private CarSettingActivity mActivity;

    @Before
    public void setUp() {
        ShadowCar.setCarManager(Car.CAR_UX_RESTRICTION_SERVICE,
                mock(CarUxRestrictionsManager.class));
        mContext = RuntimeEnvironment.application;
        mActivityController = ActivityController.of(new CarSettingActivity());
        mActivity = mActivityController.get();
    }

    @Test
    public void onPreferenceStartFragment_launchesFragment() {
        mActivityController.create();
        Preference pref = new Preference(mContext);
        pref.setFragment(TestFragment.class.getName());

        mActivity.onPreferenceStartFragment(/* caller= */ null, pref);

        assertThat(mActivity.getSupportFragmentManager().findFragmentById(
                R.id.fragment_container)).isInstanceOf(TestFragment.class);
    }

    @Test
    public void testLaunchFragment_launchDialogFragment() {
        DialogFragment dialogFragment = mock(DialogFragment.class);
        mActivity.launchFragment(dialogFragment);
        verify(dialogFragment).show(mActivity.getSupportFragmentManager(), null);
    }

    /** Simple Fragment for testing use. */
    public static class TestFragment extends Fragment {
    }
}
