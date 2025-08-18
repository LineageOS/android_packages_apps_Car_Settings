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

import static com.google.common.truth.Truth.assertThat;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class CarAspectRatioDialogActivityTest {
    private static final String ACTION_SHOW_DIALOG =
            "com.android.car.settings.aspectRatio.action.SHOW_DIALOG";
    private static final String EXTRA_KEY_COMPONENT_NAME =
            "com.android.car.settings.aspectRatio.extra.COMPONENT_NAME";
    private static final String EXTRA_KEY_USER_ID =
            "com.android.car.settings.aspectRatio.extra.USER_ID";

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private IPackageManager mPackageManager;
    @Mock
    private IActivityManager mActivityManager;

    private MockedStatic<AppGlobals> mAppGlobalsMock;
    private MockedStatic<ActivityManager> mActivityManagerMock;

    @Before
    public void setUp() {
        mSetFlagsRule.enableFlags(com.android.systemui.car.Flags.FLAG_DISPLAY_COMPATIBILITY_V2);
        mAppGlobalsMock = Mockito.mockStatic(AppGlobals.class);
        mActivityManagerMock = Mockito.mockStatic(ActivityManager.class);

        mAppGlobalsMock.when(AppGlobals::getPackageManager).thenReturn(mPackageManager);
        mActivityManagerMock.when(ActivityManager::getService).thenReturn(mActivityManager);
    }

    @After
    public void tearDown() {
        mAppGlobalsMock.close();
        mActivityManagerMock.close();
    }

    @Test
    public void testOnCreate_invalidAction_finishesActivity() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                CarAspectRatioDialogActivity.class);

        try (ActivityScenario<CarAspectRatioDialogActivity> scenario =
                     ActivityScenario.launch(intent)) {
            assertThat(scenario.getState()).isEqualTo(Lifecycle.State.DESTROYED);
        }
    }

    @Test
    public void testOnCreate_nullComponent_finishesActivity() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                CarAspectRatioDialogActivity.class);
        intent.setAction(ACTION_SHOW_DIALOG);

        try (ActivityScenario<CarAspectRatioDialogActivity> scenario =
                     ActivityScenario.launch(intent)) {
            assertThat(scenario.getState()).isEqualTo(Lifecycle.State.DESTROYED);
        }
    }

    @Test
    public void testOnCreate_validIntent_activityResumed() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                CarAspectRatioDialogActivity.class);
        intent.setAction(ACTION_SHOW_DIALOG);
        ComponentName componentName = new ComponentName("test.pkg", "test.class");
        int userId = 10;
        intent.putExtra(EXTRA_KEY_COMPONENT_NAME, componentName);
        intent.putExtra(EXTRA_KEY_USER_ID, userId);

        try (ActivityScenario<CarAspectRatioDialogActivity> scenario =
                     ActivityScenario.launch(intent)) {
            assertThat(scenario.getState()).isEqualTo(Lifecycle.State.RESUMED);
        }
    }
}
