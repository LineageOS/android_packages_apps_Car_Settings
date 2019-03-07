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

package com.android.car.settings.datausage;

import static android.net.NetworkPolicy.LIMIT_DISABLED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.NetworkPolicyManager;
import android.net.NetworkTemplate;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.common.ConfirmationDialogFragment;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.car.settings.testutils.ShadowNetworkPolicyEditor;
import com.android.car.settings.testutils.ShadowSubscriptionManager;
import com.android.car.settings.testutils.ShadowTelephonyManager;
import com.android.settingslib.NetworkPolicyEditor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowTelephonyManager.class, ShadowSubscriptionManager.class,
        ShadowNetworkPolicyEditor.class})
public class DataLimitPreferenceControllerTest {

    private static final long GIB_IN_BYTES = 1024 * 1024 * 1024;
    private static final long EPSILON = 100;

    private TwoStatePreference mEnablePreference;
    private Preference mLimitPreference;
    private DataLimitPreferenceController mController;
    private NetworkPolicyEditor mPolicyEditor;
    private NetworkTemplate mNetworkTemplate;
    private FragmentController mFragmentController;

    @Before
    public void setUp() {
        SubscriptionInfo info = mock(SubscriptionInfo.class);
        when(info.getSubscriptionId()).thenReturn(1);
        ShadowSubscriptionManager.setDefaultDataSubscriptionInfo(info);
        Shadows.shadowOf(RuntimeEnvironment.application).setSystemService(
                Context.NETWORK_POLICY_SERVICE, mock(NetworkPolicyManager.class));

        Context context = RuntimeEnvironment.application;

        PreferenceGroup preferenceGroup = new LogicalPreferenceGroup(context);
        PreferenceControllerTestHelper<DataLimitPreferenceController> controllerHelper =
                new PreferenceControllerTestHelper<>(context,
                        DataLimitPreferenceController.class, preferenceGroup);
        mController = controllerHelper.getController();
        mFragmentController = controllerHelper.getMockFragmentController();

        mEnablePreference = new SwitchPreference(context);
        mEnablePreference.setKey(context.getString(R.string.pk_data_set_limit));
        preferenceGroup.addPreference(mEnablePreference);
        mLimitPreference = new Preference(context);
        mLimitPreference.setKey(context.getString(R.string.pk_data_limit));
        preferenceGroup.addPreference(mLimitPreference);

        // Used to set the policy editor values for test purposes.
        mPolicyEditor = new NetworkPolicyEditor(NetworkPolicyManager.from(context));
        mNetworkTemplate = DataUsageUtils.getMobileNetworkTemplate(
                context.getSystemService(TelephonyManager.class),
                DataUsageUtils.getDefaultSubscriptionId(
                        context.getSystemService(SubscriptionManager.class)));

        controllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
    }

    @After
    public void tearDown() {
        ShadowTelephonyManager.reset();
        ShadowSubscriptionManager.reset();
        ShadowNetworkPolicyEditor.reset();
    }

    @Test
    public void refreshUi_limitDisabled_summaryEmpty() {
        mPolicyEditor.setPolicyLimitBytes(mNetworkTemplate, LIMIT_DISABLED);
        mController.refreshUi();

        assertThat(mLimitPreference.getSummary()).isNull();
    }

    @Test
    public void refreshUi_limitDisabled_preferenceDisabled() {
        mPolicyEditor.setPolicyLimitBytes(mNetworkTemplate, LIMIT_DISABLED);
        mController.refreshUi();

        assertThat(mLimitPreference.isEnabled()).isFalse();
    }

    @Test
    public void refreshUi_limitDisabled_switchUnchecked() {
        mPolicyEditor.setPolicyLimitBytes(mNetworkTemplate, LIMIT_DISABLED);
        mController.refreshUi();

        assertThat(mEnablePreference.isChecked()).isFalse();
    }

    @Test
    public void refreshUi_limitEnabled_summaryPopulated() {
        mPolicyEditor.setPolicyLimitBytes(mNetworkTemplate, 5 * GIB_IN_BYTES);
        mController.refreshUi();

        assertThat(mLimitPreference.getSummary().toString()).isNotEmpty();
    }

    @Test
    public void refreshUi_limitEnabled_preferenceEnabled() {
        mPolicyEditor.setPolicyLimitBytes(mNetworkTemplate, 5 * GIB_IN_BYTES);
        mController.refreshUi();

        assertThat(mLimitPreference.isEnabled()).isTrue();
    }

    @Test
    public void refreshUi_limitEnabled_switchChecked() {
        mPolicyEditor.setPolicyLimitBytes(mNetworkTemplate, 5 * GIB_IN_BYTES);
        mController.refreshUi();

        assertThat(mEnablePreference.isChecked()).isTrue();
    }

    @Test
    public void onPreferenceChanged_toggleFalse_summaryRemoved() {
        mLimitPreference.setSummary("test summary");
        mEnablePreference.callChangeListener(false);

        assertThat(mLimitPreference.getSummary()).isNull();
    }

    @Test
    public void onPreferenceChanged_toggleFalse_preferenceDisabled() {
        mLimitPreference.setEnabled(true);
        mEnablePreference.callChangeListener(false);

        assertThat(mLimitPreference.isEnabled()).isFalse();
    }

    @Test
    public void onPreferenceChanged_toggleTrue_showsDialog() {
        mEnablePreference.callChangeListener(true);

        verify(mFragmentController).showDialog(any(ConfirmationDialogFragment.class),
                eq(ConfirmationDialogFragment.TAG));
    }

    @Test
    public void onDialogConfirm_noWarningThreshold_setsLimitTo5GB() {
        mController.onConfirm(null);

        assertThat(mPolicyEditor.getPolicyLimitBytes(mNetworkTemplate)).isEqualTo(5 * GIB_IN_BYTES);
    }

    @Test
    public void onDialogConfirm_hasWarningThreshold_setsLimitToWithMultiplier() {
        mPolicyEditor.setPolicyWarningBytes(mNetworkTemplate, 5 * GIB_IN_BYTES);
        mController.onConfirm(null);

        // Due to precision errors, add and subtract a small epsilon.
        assertThat(mPolicyEditor.getPolicyLimitBytes(mNetworkTemplate)).isGreaterThan(
                (long) (5 * GIB_IN_BYTES * DataLimitPreferenceController.LIMIT_BYTES_MULTIPLIER)
                        - EPSILON);
        assertThat(mPolicyEditor.getPolicyLimitBytes(mNetworkTemplate)).isLessThan(
                (long) (5 * GIB_IN_BYTES * DataLimitPreferenceController.LIMIT_BYTES_MULTIPLIER)
                        + EPSILON);
    }

    @Test
    public void onDialogConfirm_summaryPopulated() {
        mController.onConfirm(null);

        assertThat(mLimitPreference.getSummary().toString()).isNotEmpty();
    }

    @Test
    public void onDialogConfirm_preferenceEnabled() {
        mController.onConfirm(null);

        assertThat(mLimitPreference.isEnabled()).isTrue();
    }
}
