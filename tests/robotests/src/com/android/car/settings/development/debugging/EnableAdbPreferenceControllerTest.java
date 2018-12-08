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

import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.testutils.BaseTestActivity;
import com.android.car.settings.testutils.DialogTestUtils;
import com.android.car.settings.testutils.ShadowLocalBroadcastManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowLocalBroadcastManager.class})
public class EnableAdbPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "preference_key";

    private Context mContext;
    private EnableAdbPreferenceController mController;
    private TwoStatePreference mPreference;
    @Mock
    private FragmentController mFragmentController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new EnableAdbPreferenceController(mContext, PREFERENCE_KEY,
                mFragmentController);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreference = new SwitchPreference(mContext);
        mPreference.setKey(PREFERENCE_KEY);
        screen.addPreference(mPreference);
        mController.displayPreference(screen);
    }

    @After
    public void tearDown() {
        ShadowLocalBroadcastManager.reset();
    }

    @Test
    public void testUpdateState_adbEnabled_setTrue() {
        mPreference.setChecked(false);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 1);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void testUpdateState_adbDisabled_setFalse() {
        mPreference.setChecked(true);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void testOnPreferenceChange_changeToTrue_openDialog() {
        mController.onPreferenceChange(mPreference, true);
        verify(mFragmentController).showDialog(any(EnableAdbWarningDialog.class),
                eq(EnableAdbWarningDialog.TAG));
    }

    @Test
    public void testOnPreferenceChange_changeToFalse_disableAdb() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 1);
        mController.onPreferenceChange(mPreference, false);
        assertThat(
                Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED,
                        1)).isEqualTo(0);
    }

    @Test
    public void testOnPreferenceChange_changeToFalse_sendBroadcast() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 1);
        mController.onPreferenceChange(mPreference, false);

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
        assertThat(
                Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED,
                        1)).isEqualTo(0);
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
        assertThat(
                Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED,
                        1)).isEqualTo(0);
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
        FragmentController fragmentController =
                setupPreferenceControllerWithRealFragmentController();

        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0);
        mController.onPreferenceChange(mPreference, true);
        DialogFragment dialog = fragmentController.findDialogByTag(EnableAdbWarningDialog.TAG);
        DialogTestUtils.clickPositiveButton(dialog);
        assertThat(
                Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED,
                        0)).isEqualTo(1);
    }

    @Test
    public void testDialogConfirm_falseBefore_sendBroadcast() {
        FragmentController fragmentController =
                setupPreferenceControllerWithRealFragmentController();

        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0);
        mController.onPreferenceChange(mPreference, true);
        DialogFragment dialog = fragmentController.findDialogByTag(EnableAdbWarningDialog.TAG);
        DialogTestUtils.clickPositiveButton(dialog);

        List<Intent> intentsFired = ShadowLocalBroadcastManager.getSentBroadcastIntents();
        assertThat(intentsFired.size()).isEqualTo(1);
        Intent intentFired = intentsFired.get(0);
        assertThat(intentFired.getAction()).isEqualTo(ACTION_ENABLE_ADB_STATE_CHANGED);
    }

    @Test
    public void testDialogConfirm_trueBefore_sendBroadcast() {
        FragmentController fragmentController =
                setupPreferenceControllerWithRealFragmentController();

        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 1);
        mController.onPreferenceChange(mPreference, true);
        DialogFragment dialog = fragmentController.findDialogByTag(EnableAdbWarningDialog.TAG);
        DialogTestUtils.clickPositiveButton(dialog);

        List<Intent> intentsFired = ShadowLocalBroadcastManager.getSentBroadcastIntents();
        assertThat(intentsFired).isEmpty();
    }

    @Test
    public void testDialogReject_toggleOff() {
        FragmentController fragmentController =
                setupPreferenceControllerWithRealFragmentController();

        mPreference.setChecked(true);
        mController.onPreferenceChange(mPreference, true);
        DialogFragment dialog = fragmentController.findDialogByTag(EnableAdbWarningDialog.TAG);
        DialogTestUtils.clickNegativeButton(dialog);
        assertThat(mPreference.isChecked()).isFalse();
    }

    /** Returns the BaseTestActivity, which is the real fragment controller. */
    private FragmentController setupPreferenceControllerWithRealFragmentController() {
        BaseTestActivity activity = Robolectric.setupActivity(BaseTestActivity.class);
        mController = new EnableAdbPreferenceController(mContext, PREFERENCE_KEY, activity);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreference = new SwitchPreference(mContext);
        mPreference.setKey(PREFERENCE_KEY);
        screen.addPreference(mPreference);
        mController.displayPreference(screen);

        return activity;
    }
}
