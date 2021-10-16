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

import static com.android.car.settings.common.PreferenceController.AVAILABLE;
import static com.android.car.settings.common.PreferenceController.DISABLED_FOR_PROFILE;
import static com.android.car.settings.common.PreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.net.NetworkRequest;
import android.os.UserManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.ui.preference.CarUiPreference;

import com.google.android.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class MobileNetworkBasePreferenceControllerTest extends MobileNetworkTestCase {

    private CarUiPreference mPreference;
    private TestMobileNetworkBasePreferenceController mPreferenceController;

    @Before
    @UiThreadTest
    public void setUp() {
        super.setUp();
        mPreference = new CarUiPreference(mContext);
        mPreferenceController = new TestMobileNetworkBasePreferenceController(mContext,
                "key", mFragmentController, mCarUxRestrictions);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
    }

    @Test
    public void getAvailabilityStatus_noSim_unsupported() {
        when(mTelephonyManager.getSimState()).thenReturn(TelephonyManager.SIM_STATE_ABSENT);

        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_notAdmin_disabledForUser() {
        when(mUserManager.isAdminUser()).thenReturn(false);

        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(DISABLED_FOR_PROFILE);
    }

    @Test
    public void getAvailabilityStatus_hasRestriction_disabledForUser() {
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS))
                .thenReturn(true);

        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(DISABLED_FOR_PROFILE);
    }

    @Test
    public void getAvailabilityStatus_hasMobileNetwork_isAdmin_noRestriction_available() {
        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void onStart_networkFound_updateState() {
        SubscriptionInfo info = createSubscriptionInfo(/* subId= */ TEST_SUBSCRIPTION_ID,
                /* simSlotIndex= */ 0, /* displayName= */ TEST_DISPLAY_NAME);
        List<SubscriptionInfo> selectable = Lists.newArrayList(info);
        when(mSubscriptionManager.getSelectableSubscriptionInfoList()).thenReturn(selectable);

        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        mPreferenceController.resetUpdateStateCalled();
        mPreferenceController.mNetworkCallback.onAvailable(mNetwork);

        assertThat(mPreferenceController.mSubIds).isNotEmpty();
        assertThat(mPreferenceController.mSubIds.get(0)).isEqualTo(TEST_SUBSCRIPTION_ID);
        assertThat(mPreferenceController.isUpdateStateCalled()).isTrue();
    }

    @Test
    public void onAvailable_differentSubIds_saveAll() {
        SubscriptionInfo info1 = createSubscriptionInfo(/* subId= */ TEST_SUBSCRIPTION_ID,
                /* simSlotIndex= */ 0, /* displayName= */ TEST_DISPLAY_NAME);
        SubscriptionInfo info2 = createSubscriptionInfo(/* subId= */ TEST_OTHER_SUBSCRIPTION_ID,
                /* simSlotIndex= */ 0, /* displayName= */ TEST_DISPLAY_NAME);
        List<SubscriptionInfo> selectable = Lists.newArrayList(info1, info2);
        when(mSubscriptionManager.getSelectableSubscriptionInfoList()).thenReturn(selectable);

        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        mPreferenceController.mNetworkCallback.onAvailable(mNetwork);
        when(mTelephonyNetworkSpecifier.getSubscriptionId()).thenReturn(TEST_OTHER_SUBSCRIPTION_ID);
        mPreferenceController.mNetworkCallback.onAvailable(mNetwork);

        assertThat(mPreferenceController.mSubIds.size()).isEqualTo(2);
    }

    @Test
    public void onAvailable_sameSubId_notSaved() {
        SubscriptionInfo info1 = createSubscriptionInfo(/* subId= */ TEST_SUBSCRIPTION_ID,
                /* simSlotIndex= */ 0, /* displayName= */ TEST_DISPLAY_NAME);
        List<SubscriptionInfo> selectable = Lists.newArrayList(info1);
        when(mSubscriptionManager.getSelectableSubscriptionInfoList()).thenReturn(selectable);

        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        mPreferenceController.mNetworkCallback.onAvailable(mNetwork);
        mPreferenceController.mNetworkCallback.onAvailable(mNetwork);

        assertThat(mPreferenceController.mSubIds.size()).isEqualTo(1);
    }

    private static class TestMobileNetworkBasePreferenceController extends
            MobileNetworkBasePreferenceController<Preference> {

        private boolean mUpdateStateCalled;

        TestMobileNetworkBasePreferenceController(Context context,
                String preferenceKey, FragmentController fragmentController,
                CarUxRestrictions uxRestrictions) {
            super(context, preferenceKey, fragmentController, uxRestrictions);
        }

        @Override
        protected Class<Preference> getPreferenceType() {
            return Preference.class;
        }

        @Override
        protected void updateState(Preference preference) {
            mUpdateStateCalled = true;
        }


        @Override
        protected NetworkRequest getNetworkRequest() {
            return new NetworkRequest.Builder()
                    .build();
        }

        public boolean isUpdateStateCalled() {
            return mUpdateStateCalled;
        }

        public void resetUpdateStateCalled() {
            mUpdateStateCalled = false;
        }
    }
}
