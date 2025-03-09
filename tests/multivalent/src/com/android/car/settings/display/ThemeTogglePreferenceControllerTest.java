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

package com.android.car.settings.display;

import static com.android.car.settings.display.ThemeTogglePreferenceController.FORCED_DAY_MODE;
import static com.android.car.settings.display.ThemeTogglePreferenceController.FORCED_NIGHT_MODE;
import static com.android.car.settings.display.ThemeTogglePreferenceController.FORCED_SENSOR_MODE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.car.settings.CarSettings;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.Flags;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.MultiActionPreference;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.common.ToggleButtonActionItem;
import com.android.car.settings.testutils.TestLifecycleOwner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class ThemeTogglePreferenceControllerTest {
    private Context mContext;
    private Resources mResources;
    private LifecycleOwner mLifecycleOwner;
    private ThemeTogglePreferenceController mPreferenceController;
    private MultiActionPreference mPreference;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private FragmentController mFragmentController;

    @Before
    @UiThreadTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = new TestLifecycleOwner();

        mSetFlagsRule.enableFlags(android.car.feature.Flags.FLAG_CAR_NIGHT_GLOBAL_SETTING);
        mSetFlagsRule.enableFlags(Flags.FLAG_UI_THEME_TOGGLE);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(mResources);
        mPreference = new TestMultiActionPreference(mContext);

        CarUxRestrictions carUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();
        mPreferenceController = new ThemeTogglePreferenceController(mContext,
                /* preferenceKey= */ "key", mFragmentController, carUxRestrictions);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);

        mPreferenceController.onCreate(mLifecycleOwner);
    }

    @Test
    public void onStart_registersContentObserver() {
        ContentResolver resolver = spy(mContext.getContentResolver());
        when(mContext.getContentResolver()).thenReturn(resolver);

        mPreferenceController.onStart(mLifecycleOwner);

        verify(resolver).registerContentObserver(
                eq(Settings.Global.getUriFor(CarSettings.Global.FORCED_DAY_NIGHT_MODE)), eq(false),
                any(ContentObserver.class));
    }

    @Test
    public void onStop_unregistersContentObserver() {
        ContentResolver resolver = spy(mContext.getContentResolver());
        when(mContext.getContentResolver()).thenReturn(resolver);

        mPreferenceController.onStart(mLifecycleOwner);
        mPreferenceController.onStop(mLifecycleOwner);

        verify(resolver).unregisterContentObserver(any(ContentObserver.class));
    }

    @Test
    public void testRefreshUi_sensorMode_sensorButtonChecked() {
        Settings.Global.putInt(mContext.getContentResolver(),
                CarSettings.Global.FORCED_DAY_NIGHT_MODE,
                FORCED_SENSOR_MODE);

        mPreferenceController.refreshUi();

        assertThat(mPreferenceController.getAutoButton().isChecked()).isTrue();
        assertThat(mPreferenceController.getAutoButton().isEnabled()).isFalse();
        assertThat(mPreferenceController.getDayButton().isChecked()).isFalse();
        assertThat(mPreferenceController.getDayButton().isEnabled()).isTrue();
        assertThat(mPreferenceController.getNightButton().isChecked()).isFalse();
        assertThat(mPreferenceController.getNightButton().isEnabled()).isTrue();
    }

    @Test
    public void testRefreshUi_dayMode_dayButtonChecked() {
        Settings.Global.putInt(mContext.getContentResolver(),
                CarSettings.Global.FORCED_DAY_NIGHT_MODE,
                FORCED_DAY_MODE);

        mPreferenceController.refreshUi();

        assertThat(mPreferenceController.getAutoButton().isChecked()).isFalse();
        assertThat(mPreferenceController.getAutoButton().isEnabled()).isTrue();
        assertThat(mPreferenceController.getDayButton().isChecked()).isTrue();
        assertThat(mPreferenceController.getDayButton().isEnabled()).isFalse();
        assertThat(mPreferenceController.getNightButton().isChecked()).isFalse();
        assertThat(mPreferenceController.getNightButton().isEnabled()).isTrue();
    }

    @Test
    public void testRefreshUi_nightMode_nightButtonChecked() {
        Settings.Global.putInt(mContext.getContentResolver(),
                CarSettings.Global.FORCED_DAY_NIGHT_MODE,
                FORCED_NIGHT_MODE);

        mPreferenceController.refreshUi();

        assertThat(mPreferenceController.getAutoButton().isChecked()).isFalse();
        assertThat(mPreferenceController.getAutoButton().isEnabled()).isTrue();
        assertThat(mPreferenceController.getDayButton().isChecked()).isFalse();
        assertThat(mPreferenceController.getDayButton().isEnabled()).isTrue();
        assertThat(mPreferenceController.getNightButton().isChecked()).isTrue();
        assertThat(mPreferenceController.getNightButton().isEnabled()).isFalse();
    }

    @Test
    public void testHandlePreferenceChanged_setSensor() {
        Settings.Global.putInt(mContext.getContentResolver(),
                CarSettings.Global.FORCED_DAY_NIGHT_MODE,
                FORCED_DAY_MODE);
        mPreferenceController.refreshUi();

        mPreferenceController.getAutoButton().onClick();

        int themeMode = Settings.Global.getInt(mContext.getContentResolver(),
                CarSettings.Global.FORCED_DAY_NIGHT_MODE,
                FORCED_DAY_MODE);
        assertThat(themeMode).isEqualTo(FORCED_SENSOR_MODE);
    }

    @Test
    public void testHandlePreferenceChanged_setDay() {
        Settings.Global.putInt(mContext.getContentResolver(),
                CarSettings.Global.FORCED_DAY_NIGHT_MODE,
                FORCED_SENSOR_MODE);
        mPreferenceController.refreshUi();

        mPreferenceController.getDayButton().onClick();
        int themeMode = Settings.Global.getInt(mContext.getContentResolver(),
                CarSettings.Global.FORCED_DAY_NIGHT_MODE,
                FORCED_SENSOR_MODE);

        assertThat(themeMode).isEqualTo(FORCED_DAY_MODE);
    }

    @Test
    public void testHandlePreferenceChanged_setNight() {
        Settings.Global.putInt(mContext.getContentResolver(),
                CarSettings.Global.FORCED_DAY_NIGHT_MODE,
                FORCED_SENSOR_MODE);
        mPreferenceController.refreshUi();

        mPreferenceController.getNightButton().onClick();

        int themeMode = Settings.Global.getInt(mContext.getContentResolver(),
                CarSettings.Global.FORCED_DAY_NIGHT_MODE,
                FORCED_DAY_MODE);
        assertThat(themeMode).isEqualTo(FORCED_NIGHT_MODE);
    }

    private static class TestMultiActionPreference extends MultiActionPreference {
        TestMultiActionPreference(Context context) {
            super(context);
            mActionItemArray[0] = new ToggleButtonActionItem(this);
            mActionItemArray[1] = new ToggleButtonActionItem(this);
            mActionItemArray[2] = new ToggleButtonActionItem(this);
        }
    }
}
