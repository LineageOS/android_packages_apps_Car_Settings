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

package com.android.car.settings.system;

import static android.telephony.SubscriptionManager.MIN_SUBSCRIPTION_ID_VALUE;

import static com.android.car.settings.common.BasePreferenceController.AVAILABLE;
import static com.android.car.settings.common.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.shadows.ShadowSubscriptionManager;

import java.util.Arrays;
import java.util.Collections;

/** Unit test for {@link ResetNetworkSubscriptionPreferenceController}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
public class ResetNetworkSubscriptionPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "preference_key";

    private static final int SUBID_1 = MIN_SUBSCRIPTION_ID_VALUE;
    private static final int SUBID_2 = SUBID_1 + 1;
    private static final int SUBID_3 = SUBID_2 + 1;
    private static final int SUBID_4 = SUBID_3 + 1;

    private Context mContext;
    private ShadowSubscriptionManager mShadowSubscriptionManager;
    private PreferenceScreen mScreen;
    private ListPreference mListPreference;
    private ResetNetworkSubscriptionPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mShadowSubscriptionManager = Shadow.extract(
                mContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE));

        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mListPreference = new ListPreference(mContext);
        mListPreference.setKey(PREFERENCE_KEY);
        mScreen.addPreference(mListPreference);
        mController = new ResetNetworkSubscriptionPreferenceController(mContext, PREFERENCE_KEY,
                mock(FragmentController.class));

        // Default to AVAILABLE status. Tests for this behavior will do their own setup.
        ShadowPackageManager shadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_TELEPHONY, /* supported= */
                true);
    }

    @Test
    public void getAvailabilityStatus_telephonyAvailable_available() {
        ShadowPackageManager shadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_TELEPHONY, /* supported= */
                true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_telephonyNotAvailable_unsupportedOnDevice() {
        ShadowPackageManager shadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_TELEPHONY, /* supported= */
                false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void displayPreference_nullSubscriptions_hidesPreference() {
        mController.displayPreference(mScreen);

        assertThat(mListPreference.isVisible()).isFalse();
    }

    @Test
    public void displayPreference_nullSubscriptions_setsValue() {
        mController.displayPreference(mScreen);

        assertThat(mListPreference.getValue()).isEqualTo(
                String.valueOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID));
    }

    @Test
    public void displayPreference_noSubscriptions_hidesPreference() {
        mShadowSubscriptionManager.setActiveSubscriptionInfoList(Collections.emptyList());

        mController.displayPreference(mScreen);

        assertThat(mListPreference.isVisible()).isFalse();
    }

    @Test
    public void displayPreference_noSubscriptions_setsValue() {
        mShadowSubscriptionManager.setActiveSubscriptionInfoList(Collections.emptyList());

        mController.displayPreference(mScreen);

        assertThat(mListPreference.getValue()).isEqualTo(
                String.valueOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID));
    }

    @Test
    public void displayPreference_oneSubscription_hidesPreference() {
        mShadowSubscriptionManager.setActiveSubscriptionInfoList(
                Collections.singletonList(createSubInfo(SUBID_1, "sub1")));

        mController.displayPreference(mScreen);

        assertThat(mListPreference.isVisible()).isFalse();
    }

    @Test
    public void displayPreference_oneSubscription_setsValue() {
        mShadowSubscriptionManager.setActiveSubscriptionInfoList(
                Collections.singletonList(createSubInfo(SUBID_1, "sub1")));

        mController.displayPreference(mScreen);

        assertThat(mListPreference.getValue()).isEqualTo(String.valueOf(SUBID_1));
    }

    @Test
    public void displayPreference_multipleSubscriptions_showsPreference() {
        mShadowSubscriptionManager.setActiveSubscriptionInfoList(
                Arrays.asList(createSubInfo(SUBID_1, "sub1"), createSubInfo(SUBID_2, "sub2")));

        mController.displayPreference(mScreen);

        assertThat(mListPreference.isVisible()).isTrue();
    }

    @Test
    public void displayPreference_multipleSubscriptions_populatesEntries() {
        String displayName1 = "sub1";
        String displayName2 = "sub2";
        mShadowSubscriptionManager.setActiveSubscriptionInfoList(
                Arrays.asList(createSubInfo(SUBID_1, displayName1),
                        createSubInfo(SUBID_2, displayName2)));

        mController.displayPreference(mScreen);

        assertThat(Arrays.asList(mListPreference.getEntries())).containsExactly(displayName1,
                displayName2);
        assertThat(Arrays.asList(mListPreference.getEntryValues())).containsExactly(
                String.valueOf(SUBID_1),
                String.valueOf(SUBID_2));
    }

    @Test
    public void displayPreference_defaultSelection_fourthPriority_system() {
        mShadowSubscriptionManager.setActiveSubscriptionInfoList(
                Arrays.asList(createSubInfo(SUBID_1, "sub1"), createSubInfo(SUBID_2, "sub2"),
                        createSubInfo(SUBID_3, "sub3"), createSubInfo(SUBID_4, "sub4")));

        ShadowSubscriptionManager.setDefaultSubscriptionId(SUBID_4);
        mController.displayPreference(mScreen);

        assertThat(mListPreference.getValue()).isEqualTo(String.valueOf(SUBID_4));
    }

    @Test
    public void displayPreference_defaultSelection_thirdPriority_sms() {
        mShadowSubscriptionManager.setActiveSubscriptionInfoList(
                Arrays.asList(createSubInfo(SUBID_1, "sub1"), createSubInfo(SUBID_2, "sub2"),
                        createSubInfo(SUBID_3, "sub3"), createSubInfo(SUBID_4, "sub4")));

        ShadowSubscriptionManager.setDefaultSubscriptionId(SUBID_4);
        ShadowSubscriptionManager.setDefaultSmsSubscriptionId(SUBID_3);
        mController.displayPreference(mScreen);

        assertThat(mListPreference.getValue()).isEqualTo(String.valueOf(SUBID_3));
    }

    @Test
    public void displayPreference_defaultSelection_secondPriority_voice() {
        mShadowSubscriptionManager.setActiveSubscriptionInfoList(
                Arrays.asList(createSubInfo(SUBID_1, "sub1"), createSubInfo(SUBID_2, "sub2"),
                        createSubInfo(SUBID_3, "sub3"), createSubInfo(SUBID_4, "sub4")));

        ShadowSubscriptionManager.setDefaultSubscriptionId(SUBID_4);
        ShadowSubscriptionManager.setDefaultSmsSubscriptionId(SUBID_3);
        ShadowSubscriptionManager.setDefaultVoiceSubscriptionId(SUBID_2);
        mController.displayPreference(mScreen);

        assertThat(mListPreference.getValue()).isEqualTo(String.valueOf(SUBID_2));
    }

    @Test
    public void displayPreference_defaultSelection_firstPriority_data() {
        mShadowSubscriptionManager.setActiveSubscriptionInfoList(
                Arrays.asList(createSubInfo(SUBID_1, "sub1"), createSubInfo(SUBID_2, "sub2"),
                        createSubInfo(SUBID_3, "sub3"), createSubInfo(SUBID_4, "sub4")));

        ShadowSubscriptionManager.setDefaultSubscriptionId(SUBID_4);
        ShadowSubscriptionManager.setDefaultSmsSubscriptionId(SUBID_3);
        ShadowSubscriptionManager.setDefaultVoiceSubscriptionId(SUBID_2);
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUBID_1);
        mController.displayPreference(mScreen);

        assertThat(mListPreference.getValue()).isEqualTo(String.valueOf(SUBID_1));
    }

    @Test
    public void displayPreference_title_fourthPriority_subscriptionNetworkIds() {
        SubscriptionInfo subInfo = createSubInfo(
                SUBID_1,
                /* displayName= */ "",
                /* carrierName= */ "",
                /* number= */ "");
        // Multiple subscriptions so that preference is shown / title is set.
        mShadowSubscriptionManager.setActiveSubscriptionInfoList(
                Arrays.asList(subInfo, createSubInfo(SUBID_2, "sub2")));
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUBID_1);

        mController.displayPreference(mScreen);

        String title = mListPreference.getTitle().toString();
        assertThat(title).contains(String.valueOf(subInfo.getMcc()));
        assertThat(title).contains(String.valueOf(subInfo.getMnc()));
        assertThat(title).contains(String.valueOf(subInfo.getSimSlotIndex()));
        assertThat(title).contains(String.valueOf(subInfo.getSubscriptionId()));
    }

    @Test
    public void displayPreference_title_thirdPriority_subscriptionCarrierName() {
        SubscriptionInfo subInfo = createSubInfo(
                SUBID_1,
                /* displayName= */ "",
                "carrierName",
                /* number= */ "");
        // Multiple subscriptions so that preference is shown / title is set.
        mShadowSubscriptionManager.setActiveSubscriptionInfoList(
                Arrays.asList(subInfo, createSubInfo(SUBID_2, "sub2")));
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUBID_1);

        mController.displayPreference(mScreen);

        assertThat(mListPreference.getTitle()).isEqualTo(subInfo.getCarrierName());
    }

    @Test
    public void displayPreference_title_secondPriority_subscriptionNumber() {
        SubscriptionInfo subInfo = createSubInfo(
                SUBID_1,
                /* displayName= */ "",
                "carrierName",
                "number");
        // Multiple subscriptions so that preference is shown / title is set.
        mShadowSubscriptionManager.setActiveSubscriptionInfoList(
                Arrays.asList(subInfo, createSubInfo(SUBID_2, "sub2")));
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUBID_1);

        mController.displayPreference(mScreen);

        assertThat(mListPreference.getTitle()).isEqualTo(subInfo.getNumber());
    }

    @Test
    public void displayPreference_title_firstPriority_subscriptionDisplayName() {
        SubscriptionInfo subInfo = createSubInfo(
                SUBID_1,
                "displayName",
                "carrierName",
                "number");
        // Multiple subscriptions so that preference is shown / title is set.
        mShadowSubscriptionManager.setActiveSubscriptionInfoList(
                Arrays.asList(subInfo, createSubInfo(SUBID_2, "sub2")));
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUBID_1);

        mController.displayPreference(mScreen);

        assertThat(mListPreference.getTitle()).isEqualTo(subInfo.getDisplayName());
    }

    @Test
    public void onPreferenceChange_updatesTitle() {
        mShadowSubscriptionManager.setActiveSubscriptionInfoList(
                Arrays.asList(createSubInfo(SUBID_1, "sub1"), createSubInfo(SUBID_2, "sub2")));
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUBID_1);
        mController.displayPreference(mScreen);

        mController.onPreferenceChange(mListPreference, String.valueOf(SUBID_2));

        assertThat(mListPreference.getTitle()).isEqualTo("sub2");
    }

    @Test
    public void onPreferenceChange_returnsTrue() {
        mShadowSubscriptionManager.setActiveSubscriptionInfoList(
                Arrays.asList(createSubInfo(SUBID_1, "sub1"), createSubInfo(SUBID_2, "sub2")));
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUBID_1);
        mController.displayPreference(mScreen);

        assertThat(
                mController.onPreferenceChange(mListPreference, String.valueOf(SUBID_2))).isTrue();
    }

    /** Reduce SubscriptionInfo constructor args to the ones we care about here. */
    private SubscriptionInfo createSubInfo(int subId, String displayName) {
        return createSubInfo(subId, displayName, "carrierName", "number");
    }

    /** Reduce SubscriptionInfo constructor args to the ones we care about here. */
    private SubscriptionInfo createSubInfo(int subId, String displayName, String carrierName,
            String number) {
        // Hidden constructor so resort to mocking.
        SubscriptionInfo subscriptionInfo = mock(SubscriptionInfo.class);
        when(subscriptionInfo.getSubscriptionId()).thenReturn(subId);
        when(subscriptionInfo.getDisplayName()).thenReturn(displayName);
        when(subscriptionInfo.getCarrierName()).thenReturn(carrierName);
        when(subscriptionInfo.getNumber()).thenReturn(number);
        when(subscriptionInfo.getSimSlotIndex()).thenReturn(111);
        when(subscriptionInfo.getMcc()).thenReturn(222);
        when(subscriptionInfo.getMnc()).thenReturn(333);
        return subscriptionInfo;
    }
}
