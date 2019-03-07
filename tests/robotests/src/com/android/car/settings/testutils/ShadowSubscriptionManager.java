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

package com.android.car.settings.testutils;

import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;

import android.annotation.NonNull;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionPlan;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.List;

@Implements(SubscriptionManager.class)
public class ShadowSubscriptionManager {

    private static int sDefaultDataSubscriptionId = INVALID_SUBSCRIPTION_ID;
    private static int sDefaultVoiceSubscriptionId = INVALID_SUBSCRIPTION_ID;
    private static int sDefaultSmsSubscriptionId = INVALID_SUBSCRIPTION_ID;
    private static int sDefaultSubscriptionId = INVALID_SUBSCRIPTION_ID;
    private static SubscriptionInfo sDefaultDataSubscriptionInfo = null;

    private List<SubscriptionInfo> mSubscriptionInfoList;
    private List<SubscriptionPlan> mSubscriptionPlanList;

    @Implementation
    public List<SubscriptionInfo> getActiveSubscriptionInfoList() {
        return mSubscriptionInfoList;
    }

    public void setActiveSubscriptionInfoList(List<SubscriptionInfo> subscriptionInfoList) {
        mSubscriptionInfoList = subscriptionInfoList;
    }

    @Implementation
    protected @NonNull List<SubscriptionPlan> getSubscriptionPlans(int subId) {
        return mSubscriptionPlanList;
    }

    public void setSubscriptionPlans(List<SubscriptionPlan> subscriptionPlanList) {
        mSubscriptionPlanList = subscriptionPlanList;
    }

    @Implementation
    public static int getDefaultDataSubscriptionId() {
        return sDefaultDataSubscriptionId;
    }

    @Implementation
    public static void setDefaultDataSubId(int subId) {
        sDefaultDataSubscriptionId = subId;
    }

    @Implementation
    public static int getDefaultVoiceSubscriptionId() {
        return sDefaultVoiceSubscriptionId;
    }

    @Implementation
    public static void setDefaultVoiceSubId(int subId) {
        sDefaultVoiceSubscriptionId = subId;
    }

    @Implementation
    public static int getDefaultSmsSubscriptionId() {
        return sDefaultSmsSubscriptionId;
    }

    @Implementation
    public static void setDefaultSmsSubId(int subId) {
        sDefaultSmsSubscriptionId = subId;
    }

    @Implementation
    public static int getDefaultSubscriptionId() {
        return sDefaultSubscriptionId;
    }

    public static void setDefaultSubscriptionId(int subId) {
        sDefaultSubscriptionId = subId;
    }

    @Implementation
    public SubscriptionInfo getDefaultDataSubscriptionInfo() {
        return sDefaultDataSubscriptionInfo;
    }

    public static void setDefaultDataSubscriptionInfo(SubscriptionInfo subscriptionInfo) {
        sDefaultDataSubscriptionInfo = subscriptionInfo;
    }

    /** Resets this shadow to its initial state for static values. */
    public static void reset() {
        sDefaultDataSubscriptionId = INVALID_SUBSCRIPTION_ID;
        sDefaultVoiceSubscriptionId = INVALID_SUBSCRIPTION_ID;
        sDefaultSmsSubscriptionId = INVALID_SUBSCRIPTION_ID;
        sDefaultSubscriptionId = INVALID_SUBSCRIPTION_ID;
        sDefaultDataSubscriptionInfo = null;
    }
}
