/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.android.car.settings.datausage.DataUsageSetThresholdBaseFragment.GIB_IN_BYTES;
import static com.android.car.settings.datausage.DataUsageSetThresholdBaseFragment.MB_GB_SUFFIX_THRESHOLD;
import static com.android.car.settings.datausage.DataUsageSetThresholdBaseFragment.MIB_IN_BYTES;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import android.content.Context;
import android.net.NetworkPolicyManager;
import android.net.NetworkTemplate;
import android.os.Bundle;

import androidx.fragment.app.FragmentManager;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import com.android.car.settings.R;
import com.android.car.settings.testutils.BaseCarSettingsTestActivity;
import com.android.car.ui.toolbar.ToolbarController;
import com.android.dx.mockito.inline.extended.ExtendedMockito;
import com.android.settingslib.NetworkPolicyEditor;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;

@RunWith(AndroidJUnit4.class)
public class DataUsageSetThresholdBaseFragmentTest {

    private static final int SUB_ID = 11;

    private Context mContext = ApplicationProvider.getApplicationContext();
    private DataUsageSetThresholdBaseFragment mFragment;
    private BaseCarSettingsTestActivity mActivity;
    private FragmentManager mFragmentManager;
    private MockitoSession mSession;

    @Mock
    private NetworkPolicyEditor mMockNetworkPolicyEditor;
    @Mock
    private NetworkTemplate mMockNetworkTemplate;

    @Rule
    public ActivityTestRule<BaseCarSettingsTestActivity> mActivityTestRule =
            new ActivityTestRule<>(BaseCarSettingsTestActivity.class);

    @Before
    public void setUp() throws Throwable {
        MockitoAnnotations.initMocks(this);
        mActivity = mActivityTestRule.getActivity();
        mFragmentManager = mActivityTestRule.getActivity().getSupportFragmentManager();
        mSession = ExtendedMockito.mockitoSession().mockStatic(DataUsageUtils.class,
                withSettings().lenient()).startMocking();
    }

    @After
    public void tearDown() {
        if (mSession != null) {
            mSession.finishMocking();
        }
    }

    @Test
    @UiThreadTest
    public void onActivityCreated_noTemplateSet_getsDefaultTemplate() throws Throwable {
        when(DataUsageUtils.getDefaultSubscriptionId(any())).thenReturn(SUB_ID);
        when(DataUsageUtils.getMobileNetworkTemplate(any(), eq(SUB_ID)))
                .thenReturn(mMockNetworkTemplate);
        when(mMockNetworkPolicyEditor.getPolicyWarningBytes(mMockNetworkTemplate))
                .thenReturn(MIB_IN_BYTES);
        setUpFragment(/* useTemplate= */ false);

        verify(mMockNetworkPolicyEditor).getPolicyWarningBytes(mMockNetworkTemplate);
    }

    @Test
    @UiThreadTest
    public void onActivityCreated_saveButtonSet() throws Throwable {
        when(mMockNetworkPolicyEditor.getPolicyWarningBytes(mMockNetworkTemplate))
                .thenReturn(MIB_IN_BYTES);
        setUpFragment(/* useTemplate= */ true);

        ToolbarController toolbar = mActivity.getToolbar();
        assertThat(toolbar.getMenuItems().get(0).getTitle().toString()).isEqualTo(
                mContext.getString(R.string.data_usage_warning_save_title));
    }

    @Test
    @UiThreadTest
    public void onActivityCreated_belowGbThreshold_mbSet() throws Throwable {
        when(mMockNetworkPolicyEditor.getPolicyWarningBytes(mMockNetworkTemplate))
                .thenReturn(MIB_IN_BYTES);
        setUpFragment(/* useTemplate= */ true);

        assertThat(mFragment.mDataWarningUnitsPreferenceController.isGbSelected()).isFalse();
    }

    @Test
    @UiThreadTest
    public void onActivityCreated_aboveGbThreshold_gbSet() throws Throwable {
        when(mMockNetworkPolicyEditor.getPolicyWarningBytes(mMockNetworkTemplate))
                .thenReturn((long) (MB_GB_SUFFIX_THRESHOLD * GIB_IN_BYTES * 2));
        setUpFragment(/* useTemplate= */ true);

        assertThat(mFragment.mDataWarningUnitsPreferenceController.isGbSelected()).isTrue();
    }

    @Test
    @UiThreadTest
    public void onActivityCreated_saveButtonClicked_goesBack() throws Throwable {
        when(mMockNetworkPolicyEditor.getPolicyWarningBytes(mMockNetworkTemplate))
                .thenReturn(MIB_IN_BYTES);
        setUpFragment(/* useTemplate= */ true);

        doReturn(MIB_IN_BYTES).when(mFragment).getCurrentThreshold();

        mFragment.onSaveClicked();

        assertThat(mActivity.getOnBackPressedFlag()).isTrue();
    }

    private void setUpFragment(boolean useTemplate) throws Throwable {
        String dataUsageSetThresholdFragmentTag = "data_usage_set_threshold_fragment";
        TestDataUsageSetThresholdBaseFragment fragment = new
                TestDataUsageSetThresholdBaseFragment();
        Bundle args = new Bundle();
        args.putParcelable(NetworkPolicyManager.EXTRA_NETWORK_TEMPLATE, useTemplate
                ? mMockNetworkTemplate : null);
        fragment.setArguments(args);
        fragment.mPolicyEditor = mMockNetworkPolicyEditor;

        mActivityTestRule.runOnUiThread(() -> {
            mFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment, dataUsageSetThresholdFragmentTag)
                    .commitNow();
        });
        mFragment = spy((TestDataUsageSetThresholdBaseFragment) mFragmentManager
                .findFragmentByTag(dataUsageSetThresholdFragmentTag));
    }

    public static class TestDataUsageSetThresholdBaseFragment extends
            DataUsageSetThresholdBaseFragment {

        @Override
        protected void onSave(long threshold) {}
    }
}
