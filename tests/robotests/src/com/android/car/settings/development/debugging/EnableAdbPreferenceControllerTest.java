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

package com.android.car.settings.development.debugging;

import static com.android.car.settings.development.debugging.EnableAdbPreferenceController.ACTION_ENABLE_ADB_STATE_CHANGED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.lifecycle.Lifecycle;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.car.settings.testutils.ShadowLocalBroadcastManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowLocalBroadcastManager.class})
public class EnableAdbPreferenceControllerTest {

    private Context mContext;
    private PreferenceControllerTestHelper<EnableAdbPreferenceController>
            mPreferenceControllerHelper;
    private EnableAdbPreferenceController mController;
    private TwoStatePreference mPreference;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);
        mPreferenceControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                EnableAdbPreferenceController.class, mPreference);
        mController = mPreferenceControllerHelper.getController();
        mPreferenceControllerHelper.markState(Lifecycle.State.CREATED);
    }

    @After
    public void tearDown() {
        ShadowLocalBroadcastManager.reset();
    }

    @Test
    public void testRefreshUi_adbEnabled_setTrue() {
        mPreference.setChecked(false);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 1);
        mController.refreshUi();
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void testRefreshUi_adbDisabled_setFalse() {
        mPreference.setChecked(true);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0);
        mController.refreshUi();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void testOnPreferenceChange_changeToTrue_openDialog() {
        mPreference.callChangeListener(true);
        verify(mPreferenceControllerHelper.getMockFragmentController()).showDialog(
                any(EnableAdbWarningDialog.class), eq(EnableAdbWarningDialog.TAG));
    }

    @Test
    public void testOnPreferenceChange_changeToFalse_disableAdb() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 1);
        mPreference.callChangeListener(false);
        assertThat(
                Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED,
                        1)).isEqualTo(0);
    }

    @Test
    public void testOnPreferenceChange_changeToFalse_sendBroadcast() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 1);
        mPreference.callChangeListener(false);

        List<Intent> intentsFired = ShadowLocalBroadcastManager.getSentBroadcastIntents();
        assertThat(intentsFired.size()).isEqualTo(1);
        Intent intentFired = intentsFired.get(0);
        assertThat(intentFired.getAction()).isEqualTo(ACTION_ENABLE_ADB_STATE_CHANGED);
    }

    @Test
    public void testOnDeveloperOptionsDisabled_wasDisabled_dontSendBroadcast() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0);
        mController.onDeveloperOptionsDisabled();
        List<Intent> intentsFired = ShadowLocalBroadcastManager.getSentBroadcastIntents();
        assertThat(intentsFired).isEmpty();
    }

    @Test
    public void testOnDeveloperOptionsDisabled_wasDisabled_noChange() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0);
        mController.onDeveloperOptionsDisabled();
        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ADB_ENABLED, 1)).isEqualTo(0);
    }

    @Test
    public void testOnDeveloperOptionsDisabled_wasEnabled_sendBroadcast() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 1);
        mController.onDeveloperOptionsDisabled();

        List<Intent> intentsFired = ShadowLocalBroadcastManager.getSentBroadcastIntents();
        assertThat(intentsFired.size()).isEqualTo(1);
        Intent intentFired = intentsFired.get(0);
        assertThat(intentFired.getAction()).isEqualTo(ACTION_ENABLE_ADB_STATE_CHANGED);
    }

    @Test
    public void testOnDeveloperOptionsDisabled_wasEnabled_settingDisabled() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 1);
        mController.onDeveloperOptionsDisabled();
        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ADB_ENABLED, 1)).isEqualTo(0);
    }

    @Test
    public void testOnDeveloperOptionsDisabled_wasDisabled_sendBroadcast() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0);
        mController.onDeveloperOptionsDisabled();

        List<Intent> intentsFired = ShadowLocalBroadcastManager.getSentBroadcastIntents();
        assertThat(intentsFired).isEmpty();
    }

    @Test
    public void testDialogConfirm_enableAdb() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0);
        mController.mListener.onAdbEnableConfirmed();
        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ADB_ENABLED, 0)).isEqualTo(1);
    }

    @Test
    public void testDialogConfirm_falseBefore_sendBroadcast() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0);
        mController.mListener.onAdbEnableConfirmed();

        List<Intent> intentsFired = ShadowLocalBroadcastManager.getSentBroadcastIntents();
        assertThat(intentsFired.size()).isEqualTo(1);
        Intent intentFired = intentsFired.get(0);
        assertThat(intentFired.getAction()).isEqualTo(ACTION_ENABLE_ADB_STATE_CHANGED);
    }

    @Test
    public void testDialogConfirm_trueBefore_dontSendBroadcast() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 1);
        mController.mListener.onAdbEnableConfirmed();

        List<Intent> intentsFired = ShadowLocalBroadcastManager.getSentBroadcastIntents();
        assertThat(intentsFired).isEmpty();
    }

    @Test
    public void testDialogReject_toggleOff() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0);
        mPreference.setChecked(true);
        mController.mListener.onAdbEnableRejected();
        assertThat(mPreference.isChecked()).isFalse();
    }
}
