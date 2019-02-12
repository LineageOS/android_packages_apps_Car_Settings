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

import static android.net.NetworkPolicy.WARNING_DISABLED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
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
import org.robolectric.annotation.Config;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowTelephonyManager.class, ShadowSubscriptionManager.class,
        ShadowNetworkPolicyEditor.class})
public class DataWarningPreferenceControllerTest {

    private static final long BYTES_IN_GIGABYTE = 1024 * 1024 * 1024;

    private TwoStatePreference mEnablePreference;
    private Preference mWarningPreference;
    private DataWarningPreferenceController mController;
    private NetworkPolicyEditor mPolicyEditor;
    private NetworkTemplate mNetworkTemplate;

    @Before
    public void setUp() {
        SubscriptionInfo info = mock(SubscriptionInfo.class);
        when(info.getSubscriptionId()).thenReturn(1);
        ShadowSubscriptionManager.setDefaultDataSubscriptionInfo(info);

        Context context = RuntimeEnvironment.application;

        PreferenceGroup preferenceGroup = new LogicalPreferenceGroup(context);
        PreferenceControllerTestHelper<DataWarningPreferenceController> controllerHelper =
                new PreferenceControllerTestHelper<>(context,
                        DataWarningPreferenceController.class, preferenceGroup);
        mController = controllerHelper.getController();

        mEnablePreference = new SwitchPreference(context);
        mEnablePreference.setKey(context.getString(R.string.pk_data_set_warning));
        preferenceGroup.addPreference(mEnablePreference);
        mWarningPreference = new Preference(context);
        mWarningPreference.setKey(context.getString(R.string.pk_data_warning));
        preferenceGroup.addPreference(mWarningPreference);

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
    public void refreshUi_warningDisabled_summaryEmpty() {
        mPolicyEditor.setPolicyWarningBytes(mNetworkTemplate, WARNING_DISABLED);
        mController.refreshUi();

        assertThat(mWarningPreference.getSummary()).isNull();
    }

    @Test
    public void refreshUi_warningDisabled_preferenceDisabled() {
        mPolicyEditor.setPolicyWarningBytes(mNetworkTemplate, WARNING_DISABLED);
        mController.refreshUi();

        assertThat(mWarningPreference.isEnabled()).isFalse();
    }

    @Test
    public void refreshUi_warningDisabled_switchUnchecked() {
        mPolicyEditor.setPolicyWarningBytes(mNetworkTemplate, WARNING_DISABLED);
        mController.refreshUi();

        assertThat(mEnablePreference.isChecked()).isFalse();
    }

    @Test
    public void refreshUi_warningEnabled_summaryPopulated() {
        mPolicyEditor.setPolicyWarningBytes(mNetworkTemplate, 3 * BYTES_IN_GIGABYTE);
        mController.refreshUi();

        assertThat(mWarningPreference.getSummary().toString()).isNotEmpty();
    }

    @Test
    public void refreshUi_warningEnabled_preferenceEnabled() {
        mPolicyEditor.setPolicyWarningBytes(mNetworkTemplate, 3 * BYTES_IN_GIGABYTE);
        mController.refreshUi();

        assertThat(mWarningPreference.isEnabled()).isTrue();
    }

    @Test
    public void refreshUi_warningEnabled_switchChecked() {
        mPolicyEditor.setPolicyWarningBytes(mNetworkTemplate, 3 * BYTES_IN_GIGABYTE);
        mController.refreshUi();

        assertThat(mEnablePreference.isChecked()).isTrue();
    }

    @Test
    public void onPreferenceChanged_toggleFalse_summaryRemoved() {
        mWarningPreference.setSummary("test summary");
        mEnablePreference.callChangeListener(false);

        assertThat(mWarningPreference.getSummary()).isNull();
    }

    @Test
    public void onPreferenceChanged_toggleFalse_preferenceDisabled() {
        mWarningPreference.setEnabled(true);
        mEnablePreference.callChangeListener(false);

        assertThat(mWarningPreference.isEnabled()).isFalse();
    }

    @Test
    public void onPreferenceChanged_toggleTrue_summaryAdded() {
        mWarningPreference.setSummary(null);
        mEnablePreference.callChangeListener(true);

        assertThat(mWarningPreference.getSummary().toString()).isNotEmpty();
    }

    @Test
    public void onPreferenceChanged_toggleTrue_preferenceEnabled() {
        mWarningPreference.setEnabled(false);
        mEnablePreference.callChangeListener(true);

        assertThat(mWarningPreference.isEnabled()).isTrue();
    }
}
