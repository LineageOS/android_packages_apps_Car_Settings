/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.car.settings.displaycompat;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.doReturn;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockitoSession;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.car.Car;
import android.car.content.pm.CarPackageManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.testutils.RobolectricTestUtils;
import com.android.dx.mockito.inline.extended.ExtendedMockito;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

@RunWith(AndroidJUnit4.class)
public class CarDisplayDensityDialogActivityTest {
    private static final String ACTION_SHOW_DIALOG =
            "com.android.car.settings.displayDensity.action.SHOW_DIALOG";
    private static final String EXTRA_KEY_COMPONENT_NAME =
            "com.android.car.settings.displayDensity.extra.COMPONENT_NAME";
    private static final String EXTRA_KEY_USER_ID =
            "com.android.car.settings.displayDensity.extra.USER_ID";
    private static final String EXTRA_KEY_DISPLAY_ID =
            "com.android.car.settings.displayDensity.extra.DISPLAY_ID";

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private IActivityManager mActivityManager;
    @Mock
    private Car mCar;
    @Mock
    private CarPackageManager mCarPackageManager;

    private MockitoSession mSession;

    @Before
    public void setUp() {
        mSetFlagsRule.enableFlags(com.android.systemui.car.Flags.FLAG_DISPLAY_COMPATIBILITY_V2);
        if (!RobolectricTestUtils.isRunningOnRobolectric()) {
            mSession = ExtendedMockito.mockitoSession()
                    .initMocks(this)
                    .mockStatic(AppGlobals.class)
                    .mockStatic(ActivityManager.class)
                    .mockStatic(Car.class)
                    .strictness(Strictness.LENIENT)
                    .startMocking();
            doReturn(mCar).when(() -> Car.createCar(any(Context.class)));
            when(mCar.getCarManager(eq(CarPackageManager.class))).thenReturn(mCarPackageManager);
            doReturn(mActivityManager).when(ActivityManager::getService);

        } else {
            mSession = mockitoSession()
                    .initMocks(this)
                    .strictness(Strictness.LENIENT)
                    .startMocking();
        }
    }

    @After
    public void tearDown() {
        if (mSession != null) {
            mSession.finishMocking();
        }
    }

    @Test
    public void testOnCreate_invalidAction_finishesActivity() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                CarDisplayDensityDialogActivity.class);

        try (ActivityScenario<CarDisplayDensityDialogActivity> scenario =
                     ActivityScenario.launch(intent)) {
            assertThat(scenario.getState()).isEqualTo(Lifecycle.State.DESTROYED);
        }
    }

    @Test
    public void testOnCreate_nullComponent_finishesActivity() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                CarDisplayDensityDialogActivity.class);
        intent.setAction(ACTION_SHOW_DIALOG);

        try (ActivityScenario<CarDisplayDensityDialogActivity> scenario =
                     ActivityScenario.launch(intent)) {
            assertThat(scenario.getState()).isEqualTo(Lifecycle.State.DESTROYED);
        }
    }

    @Test
    public void testOnCreate_validIntent_activityResumed() {
        // TODO(b/394651425): Remove this assumption once the bug is fixed.
        Assume.assumeFalse(
                "Skipping test on Robolectric b/394651425",
                RobolectricTestUtils.isRunningOnRobolectric());

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                CarDisplayDensityDialogActivity.class);
        intent.setAction(ACTION_SHOW_DIALOG);
        ComponentName componentName = new ComponentName("test.pkg", "test.class");
        int userId = 99;
        int displayId = 20;
        intent.putExtra(EXTRA_KEY_COMPONENT_NAME, componentName);
        intent.putExtra(EXTRA_KEY_USER_ID, userId);
        intent.putExtra(EXTRA_KEY_DISPLAY_ID, displayId);

        try (ActivityScenario<CarDisplayDensityDialogActivity> scenario =
                     ActivityScenario.launch(intent)) {
            assertThat(scenario.getState()).isEqualTo(Lifecycle.State.RESUMED);
        }
    }
}
