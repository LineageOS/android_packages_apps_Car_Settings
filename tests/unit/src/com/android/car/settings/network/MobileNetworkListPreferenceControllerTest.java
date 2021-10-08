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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.telephony.SubscriptionInfo;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.settings.common.PreferenceControllerTestUtil;

import com.google.android.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class MobileNetworkListPreferenceControllerTest extends MobileNetworkTestCase {

    private PreferenceGroup mPreferenceGroup;
    private MobileNetworkListPreferenceController mPreferenceController;

    @Before
    @UiThreadTest
    public void setUp() {
        super.setUp();

        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);
        mPreferenceGroup = new LogicalPreferenceGroup(mContext);
        screen.addPreference(mPreferenceGroup);
        mPreferenceController = new MobileNetworkListPreferenceController(mContext,
                "key", mFragmentController, mCarUxRestrictions);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreferenceGroup);
    }

    @Test
    public void onCreate_noNetworks_noPreferences() {
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void onCreate_containsElements() {
        SubscriptionInfo info = createSubscriptionInfo(/* subId= */ TEST_SUBSCRIPTION_ID,
                /* simSlotIndex= */ 1, /* displayName= */TEST_DISPLAY_NAME);
        List<SubscriptionInfo> selectable = Lists.newArrayList(info);
        when(mSubscriptionManager.getSelectableSubscriptionInfoList()).thenReturn(selectable);

        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.mNetworkCallback.onAvailable(mNetwork);

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    @UiThreadTest
    public void onPreferenceClicked_launchesFragment() {
        SubscriptionInfo info = createSubscriptionInfo(/* subId= */ TEST_SUBSCRIPTION_ID,
                /* simSlotIndex= */ 1, /* displayName= */TEST_DISPLAY_NAME);
        List<SubscriptionInfo> selectable = Lists.newArrayList(info);
        when(mSubscriptionManager.getSelectableSubscriptionInfoList()).thenReturn(selectable);

        mPreferenceController.mNetworkCallback.onAvailable(mNetwork);
        mPreferenceController.onCreate(mLifecycleOwner);

        Preference preference = mPreferenceGroup.getPreference(0);
        preference.performClick();

        ArgumentCaptor<MobileNetworkFragment> captor = ArgumentCaptor.forClass(
                MobileNetworkFragment.class);
        verify(mFragmentController).launchFragment(captor.capture());

        assertThat(captor.getValue().getArguments().getInt(MobileNetworkFragment.ARG_NETWORK_SUB_ID,
                -1)).isEqualTo(TEST_SUBSCRIPTION_ID);
    }
}
