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

package com.android.car.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.telephony.SubscriptionInfo;

import androidx.fragment.app.Fragment;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.R;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.ui.preference.CarUiTwoActionSwitchPreference;
import com.android.settingslib.utils.StringUtil;

import com.google.android.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class MobileNetworkEntryPreferenceControllerTest extends MobileNetworkTestCase {

    private CarUiTwoActionSwitchPreference mPreference;
    private MobileNetworkEntryPreferenceController mPreferenceController;

    @Before
    @UiThreadTest
    public void setUp() {
        super.setUp();
        mPreference = new CarUiTwoActionSwitchPreference(mContext);
        mPreferenceController = new MobileNetworkEntryPreferenceController(mContext,
                "key", mFragmentController, mCarUxRestrictions);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
    }

    @Test
    public void onCreate_noSims_disabled() {
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void onCreate_oneSim_enabled() {
        SubscriptionInfo info = createSubscriptionInfo(/* subId= */ TEST_SUBSCRIPTION_ID,
                /* simSlotIndex= */ 1, TEST_DISPLAY_NAME);
        List<SubscriptionInfo> selectable = Lists.newArrayList(info);
        when(mSubscriptionManager.getSelectableSubscriptionInfoList()).thenReturn(selectable);

        mPreferenceController.mNetworkCallback.onAvailable(mNetwork);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.isEnabled()).isTrue();
    }


    @Test
    public void onCreate_oneOemSim_hidden() {
        SubscriptionInfo info = createSubscriptionInfo(/* subId= */ TEST_SUBSCRIPTION_ID,
                /* simSlotIndex= */ 1, TEST_DISPLAY_NAME);
        List<SubscriptionInfo> selectable = Lists.newArrayList(info);
        when(mSubscriptionManager.getSelectableSubscriptionInfoList()).thenReturn(selectable);

        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void onCreate_oneSim_summaryIsDisplayName() {
        SubscriptionInfo info = createSubscriptionInfo(/* subId= */ TEST_SUBSCRIPTION_ID,
                /* simSlotIndex= */ 1, TEST_DISPLAY_NAME);
        List<SubscriptionInfo> selectable = Lists.newArrayList(info);
        when(mSubscriptionManager.getSelectableSubscriptionInfoList()).thenReturn(selectable);

        mPreferenceController.mNetworkCallback.onAvailable(mNetwork);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.getSummary()).isEqualTo(TEST_DISPLAY_NAME);
    }

    @Test
    public void onCreate_multiSim_enabled() {
        SubscriptionInfo info1 = createSubscriptionInfo(/* subId= */ TEST_SUBSCRIPTION_ID,
                /* simSlotIndex= */ 1, TEST_DISPLAY_NAME);
        SubscriptionInfo info2 = createSubscriptionInfo(/* subId= */ TEST_OTHER_SUBSCRIPTION_ID,
                /* simSlotIndex= */ 2, TEST_DISPLAY_NAME);
        List<SubscriptionInfo> selectable = Lists.newArrayList(info1, info2);
        when(mSubscriptionManager.getSelectableSubscriptionInfoList()).thenReturn(selectable);

        mPreferenceController.mNetworkCallback.onAvailable(mNetwork);
        when(mTelephonyNetworkSpecifier.getSubscriptionId()).thenReturn(TEST_OTHER_SUBSCRIPTION_ID);
        mPreferenceController.mNetworkCallback.onAvailable(mNetwork);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void onCreate_multiSim_summaryShowsCount() {
        SubscriptionInfo info1 = createSubscriptionInfo(/* subId= */ TEST_SUBSCRIPTION_ID,
                /* simSlotIndex= */ 1, TEST_DISPLAY_NAME);
        SubscriptionInfo info2 = createSubscriptionInfo(/* subId= */ TEST_OTHER_SUBSCRIPTION_ID,
                /* simSlotIndex= */ 2, TEST_DISPLAY_NAME);
        List<SubscriptionInfo> selectable = Lists.newArrayList(info1, info2);
        when(mSubscriptionManager.getSelectableSubscriptionInfoList()).thenReturn(selectable);

        mPreferenceController.mNetworkCallback.onAvailable(mNetwork);
        when(mTelephonyNetworkSpecifier.getSubscriptionId()).thenReturn(TEST_OTHER_SUBSCRIPTION_ID);
        mPreferenceController.mNetworkCallback.onAvailable(mNetwork);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.getSummary()).isEqualTo(StringUtil.getIcuPluralsString(mContext, 2,
                R.string.mobile_network_summary_count));
    }

    @Test
    @UiThreadTest
    public void performClick_noSim_noFragmentStarted() {
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreference.performClick();

        verify(mFragmentController, never()).launchFragment(
                any(Fragment.class));
    }

    @Test
    @UiThreadTest
    public void performClick_oneSim_startsMobileNetworkFragment() {
        SubscriptionInfo info = createSubscriptionInfo(TEST_SUBSCRIPTION_ID,
                /* simSlotIndex= */ 1, TEST_DISPLAY_NAME);
        List<SubscriptionInfo> selectable = Lists.newArrayList(info);
        when(mSubscriptionManager.getSelectableSubscriptionInfoList()).thenReturn(selectable);

        mPreferenceController.mNetworkCallback.onAvailable(mNetwork);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreference.performClick();

        ArgumentCaptor<MobileNetworkFragment> captor = ArgumentCaptor.forClass(
                MobileNetworkFragment.class);
        verify(mFragmentController).launchFragment(captor.capture());

        assertThat(captor.getValue().getArguments().getInt(MobileNetworkFragment.ARG_NETWORK_SUB_ID,
                -1)).isEqualTo(TEST_SUBSCRIPTION_ID);
    }

    @Test
    @UiThreadTest
    public void performClick_multiSim_startsMobileNetworkListFragment() {
        SubscriptionInfo info1 = createSubscriptionInfo(/* subId= */ TEST_SUBSCRIPTION_ID,
                /* simSlotIndex= */ 1, TEST_DISPLAY_NAME);
        SubscriptionInfo info2 = createSubscriptionInfo(/* subId= */ TEST_OTHER_SUBSCRIPTION_ID,
                /* simSlotIndex= */ 2, TEST_DISPLAY_NAME);
        List<SubscriptionInfo> selectable = Lists.newArrayList(info1, info2);
        when(mSubscriptionManager.getSelectableSubscriptionInfoList()).thenReturn(selectable);

        mPreferenceController.mNetworkCallback.onAvailable(mNetwork);
        when(mTelephonyNetworkSpecifier.getSubscriptionId()).thenReturn(TEST_OTHER_SUBSCRIPTION_ID);
        mPreferenceController.mNetworkCallback.onAvailable(mNetwork);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreference.performClick();

        verify(mFragmentController).launchFragment(any(MobileNetworkListFragment.class));
    }
}
