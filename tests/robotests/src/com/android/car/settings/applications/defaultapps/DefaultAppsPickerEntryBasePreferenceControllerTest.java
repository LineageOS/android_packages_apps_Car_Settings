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

package com.android.car.settings.applications.defaultapps;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.common.ButtonPreference;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.settingslib.applications.DefaultAppInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class DefaultAppsPickerEntryBasePreferenceControllerTest {

    private static final CharSequence TEST_LABEL = "Test Label";
    private static final Intent TEST_INTENT = new Intent(Settings.ACTION_SETTINGS);

    private static class TestDefaultAppsPickerEntryBasePreferenceController extends
            DefaultAppsPickerEntryBasePreferenceController {

        private Intent mSettingIntent;
        private DefaultAppInfo mDefaultAppInfo;

        TestDefaultAppsPickerEntryBasePreferenceController(Context context,
                String preferenceKey, FragmentController fragmentController,
                CarUxRestrictions uxRestrictions) {
            super(context, preferenceKey, fragmentController, uxRestrictions);
        }

        @Nullable
        @Override
        protected Intent getSettingIntent(@Nullable DefaultAppInfo info) {
            return mSettingIntent;
        }

        protected void setSettingIntent(Intent settingIntent) {
            mSettingIntent = settingIntent;
        }

        @Nullable
        @Override
        protected DefaultAppInfo getCurrentDefaultAppInfo() {
            return mDefaultAppInfo;
        }

        protected void setCurrentDefaultAppInfo(DefaultAppInfo defaultAppInfo) {
            mDefaultAppInfo = defaultAppInfo;
        }
    }

    private Context mContext;
    private ButtonPreference mButtonPreference;
    private PreferenceControllerTestHelper<TestDefaultAppsPickerEntryBasePreferenceController>
            mControllerHelper;
    private TestDefaultAppsPickerEntryBasePreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mButtonPreference = new ButtonPreference(mContext);
        mControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                TestDefaultAppsPickerEntryBasePreferenceController.class, mButtonPreference);
        mController = mControllerHelper.getController();
    }

    @Test
    public void refreshUi_hasSettingIntent_actionButtonIsVisible() {
        mController.setSettingIntent(TEST_INTENT);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mController.refreshUi();

        assertThat(mButtonPreference.isActionShown()).isTrue();
    }

    @Test
    public void refreshUi_hasNoSettingIntent_actionButtonIsNotVisible() {
        mController.setSettingIntent(null);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mController.refreshUi();

        assertThat(mButtonPreference.isActionShown()).isFalse();
    }

    @Test
    public void refreshUi_hasDefaultAppWithLabel_summaryAndIconAreSet() {
        DefaultAppInfo defaultAppInfo = mock(DefaultAppInfo.class);
        when(defaultAppInfo.loadLabel()).thenReturn(TEST_LABEL);
        when(defaultAppInfo.loadIcon()).thenReturn(mContext.getDrawable(R.drawable.test_icon));
        mController.setCurrentDefaultAppInfo(defaultAppInfo);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mController.refreshUi();

        assertThat(mButtonPreference.getSummary()).isEqualTo(TEST_LABEL);
        assertThat(mButtonPreference.getIcon()).isNotNull();
    }

    @Test
    public void refreshUi_hasDefaultAppWithoutLabel_summaryAndIconAreNotSet() {
        DefaultAppInfo defaultAppInfo = mock(DefaultAppInfo.class);
        when(defaultAppInfo.loadLabel()).thenReturn(null);
        when(defaultAppInfo.loadIcon()).thenReturn(null);
        mController.setCurrentDefaultAppInfo(defaultAppInfo);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mController.refreshUi();

        assertThat(mButtonPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.app_list_preference_none));
        assertThat(mButtonPreference.getIcon()).isNull();
    }

    @Test
    public void refreshUi_hasNoDefaultApp_summaryAndIconAreNotSet() {
        mController.setCurrentDefaultAppInfo(null);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mController.refreshUi();

        assertThat(mButtonPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.app_list_preference_none));
        assertThat(mButtonPreference.getIcon()).isNull();
    }

    @Test
    public void performButtonClick_launchesIntent() {
        mController.setSettingIntent(TEST_INTENT);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mButtonPreference.performButtonClick();

        Intent actual = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(actual.getAction()).isEqualTo(TEST_INTENT.getAction());
    }
}
