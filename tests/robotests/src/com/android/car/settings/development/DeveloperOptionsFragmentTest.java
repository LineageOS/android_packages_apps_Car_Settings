/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.settings.development;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.provider.Settings;
import android.widget.Switch;

import androidx.fragment.app.DialogFragment;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.testutils.BaseTestActivity;
import com.android.car.settings.testutils.DialogTestUtils;
import com.android.car.settings.testutils.ShadowCarUserManagerHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowCarUserManagerHelper.class})
public class DeveloperOptionsFragmentTest {

    private BaseTestActivity mTestActivity;
    private DeveloperOptionsFragment mDeveloperOptionsFragment;
    private Context mContext;
    @Mock
    private CarUserManagerHelper mCarUserManagerHelper;

    @Before
    public void setUpTestActivity() {
        MockitoAnnotations.initMocks(this);
        ShadowCarUserManagerHelper.setMockInstance(mCarUserManagerHelper);
        mContext = RuntimeEnvironment.application;
        mTestActivity = Robolectric.setupActivity(BaseTestActivity.class);
        mDeveloperOptionsFragment = new DeveloperOptionsFragment();

        // Setup admin user who is able to enable developer settings.
        UserInfo userInfo = new UserInfo();
        when(mCarUserManagerHelper.isCurrentProcessAdminUser()).thenReturn(true);
        when(mCarUserManagerHelper.isCurrentProcessDemoUser()).thenReturn(false);
        when(mCarUserManagerHelper.getCurrentProcessUserInfo()).thenReturn(userInfo);
        new CarUserManagerHelper(mContext).setUserRestriction(userInfo,
                UserManager.DISALLOW_DEBUGGING_FEATURES, false);
    }

    @After
    public void tearDown() {
        ShadowCarUserManagerHelper.reset();
    }

    @Test
    public void testOnActivityCreated_devSettingsEnabled_switchOn() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
        mTestActivity.launchFragment(mDeveloperOptionsFragment);
        Switch onOffSwitch = mTestActivity.findViewById(R.id.toggle_switch);
        assertThat(onOffSwitch.isChecked()).isTrue();
    }

    @Test
    public void testOnActivityCreated_devSettingsDisabled_switchOff() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
        mTestActivity.launchFragment(mDeveloperOptionsFragment);
        Switch onOffSwitch = mTestActivity.findViewById(R.id.toggle_switch);
        assertThat(onOffSwitch.isChecked()).isFalse();
    }

    @Test
    public void testToggleSwitchOff_devSettingsDisabled() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
        mTestActivity.launchFragment(mDeveloperOptionsFragment);
        Switch onOffSwitch = mTestActivity.findViewById(R.id.toggle_switch);
        onOffSwitch.setChecked(false);

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1)).isEqualTo(0);
    }

    @Test
    public void testToggleSwitchOn_dialogShown() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
        mTestActivity.launchFragment(mDeveloperOptionsFragment);
        Switch onOffSwitch = mTestActivity.findViewById(R.id.toggle_switch);
        onOffSwitch.setChecked(true);

        assertThat(mDeveloperOptionsFragment.findDialogByTag(
                EnableDeveloperSettingsWarningDialog.TAG)).isNotNull();
    }

    @Test
    public void testDialogListener_positiveClick_devSettingsEnabled() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
        mTestActivity.launchFragment(mDeveloperOptionsFragment);
        Switch onOffSwitch = mTestActivity.findViewById(R.id.toggle_switch);
        onOffSwitch.setChecked(true);
        DialogFragment dialog = mDeveloperOptionsFragment.findDialogByTag(
                EnableDeveloperSettingsWarningDialog.TAG);

        DialogTestUtils.clickPositiveButton(dialog);

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0)).isEqualTo(1);
    }

    @Test
    public void testDialogListener_negativeClick_switchOff() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
        mTestActivity.launchFragment(mDeveloperOptionsFragment);
        Switch onOffSwitch = mTestActivity.findViewById(R.id.toggle_switch);
        onOffSwitch.setChecked(true);
        DialogFragment dialog = mDeveloperOptionsFragment.findDialogByTag(
                EnableDeveloperSettingsWarningDialog.TAG);

        DialogTestUtils.clickNegativeButton(dialog);

        assertThat(onOffSwitch.isChecked()).isFalse();
    }
}
