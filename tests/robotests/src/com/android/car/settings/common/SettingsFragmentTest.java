/*
 * Copyright 2018 The Android Open Source Project
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

import android.car.drivingstate.CarUxRestrictions;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.testutils.FragmentController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Unit test for {@link SettingsFragment}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
public class SettingsFragmentTest {

    private FragmentController<TestSettingsFragment> mFragmentController;
    private SettingsFragment mFragment;

    @Before
    public void setUp() {
        mFragmentController = FragmentController.of(new TestSettingsFragment());
        mFragment = mFragmentController.get();
    }

    @Test
    public void use_returnsController() {
        mFragmentController.setup();

        assertThat(mFragment.use(FakePreferenceController.class,
                R.string.tpk_fake_controller)).isNotNull();
    }

    @Test
    public void onAttach_registersLifecycleObservers() {
        mFragmentController.create();
        FakePreferenceController controller = mFragment.use(FakePreferenceController.class,
                R.string.tpk_fake_controller);

        assertThat(controller.getOnCreateInternalCallCount()).isEqualTo(1);

        mFragmentController.destroy();

        assertThat(controller.getOnDestroyInternalCallCount()).isEqualTo(1);
    }

    @Test
    public void onUxRestrictionsChanged_propagatesToControllers() {
        mFragmentController.setup();
        FakePreferenceController controller = mFragment.use(FakePreferenceController.class,
                R.string.tpk_fake_controller);
        CarUxRestrictions uxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_NO_KEYBOARD, /* timestamp= */ 0).build();

        mFragment.onUxRestrictionsChanged(uxRestrictions);

        assertThat(controller.getUxRestrictions()).isEqualTo(uxRestrictions);
    }

    /** Concrete {@link SettingsFragment} for testing. */
    public static class TestSettingsFragment extends SettingsFragment {
        @Override
        protected int getPreferenceScreenResId() {
            return R.xml.settings_fragment;
        }
    }
}
