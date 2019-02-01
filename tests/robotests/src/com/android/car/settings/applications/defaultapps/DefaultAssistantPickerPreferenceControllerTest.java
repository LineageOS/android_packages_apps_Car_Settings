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

package com.android.car.settings.applications.defaultapps;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.car.userlib.CarUserManagerHelper;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.provider.Settings;

import androidx.preference.PreferenceGroup;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.car.settings.testutils.ShadowCarUserManagerHelper;
import com.android.car.settings.testutils.ShadowSecureSettings;
import com.android.car.settings.testutils.ShadowVoiceInteractionServiceInfo;

import com.google.android.collect.Lists;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowSecureSettings.class, ShadowVoiceInteractionServiceInfo.class,
        ShadowCarUserManagerHelper.class})
public class DefaultAssistantPickerPreferenceControllerTest {

    private static final String TEST_PACKAGE_NAME = "com.test.package";
    private static final String TEST_SERVICE = "TestService";
    private static final String TEST_ACTIVITY = "TestActivity";
    private static final int TEST_USER_ID = 10;

    private Context mContext;
    private PreferenceGroup mPreferenceGroup;
    private PreferenceControllerTestHelper<DefaultAssistantPickerPreferenceController>
            mControllerHelper;
    private DefaultAssistantPickerPreferenceController mController;
    @Mock
    private CarUserManagerHelper mCarUserManagerHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowCarUserManagerHelper.setMockInstance(mCarUserManagerHelper);

        mContext = RuntimeEnvironment.application;
        mPreferenceGroup = new LogicalPreferenceGroup(mContext);
        mControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                DefaultAssistantPickerPreferenceController.class, mPreferenceGroup);
        mController = mControllerHelper.getController();

        // Set user.
        when(mCarUserManagerHelper.getCurrentProcessUserId()).thenReturn(TEST_USER_ID);
    }

    @After
    public void tearDown() {
        ShadowCarUserManagerHelper.reset();
        ShadowVoiceInteractionServiceInfo.reset();
    }

    @Test
    public void getCandidates_oneActivityOneService_returnsTwoElements() {
        // Included. Supports assist.
        ResolveInfo serviceResolveInfo = new ResolveInfo();
        serviceResolveInfo.serviceInfo = new ServiceInfo();
        serviceResolveInfo.serviceInfo.packageName = TEST_PACKAGE_NAME;
        serviceResolveInfo.serviceInfo.name = TEST_SERVICE;
        ShadowVoiceInteractionServiceInfo.setSupportsAssist(serviceResolveInfo.serviceInfo, true);

        // Included. Different package.
        ResolveInfo activityResolveInfoDifferentPackage = new ResolveInfo();
        activityResolveInfoDifferentPackage.activityInfo = new ActivityInfo();
        activityResolveInfoDifferentPackage.activityInfo.packageName = "com.other.package";
        activityResolveInfoDifferentPackage.activityInfo.name = TEST_ACTIVITY;

        getShadowApplicationManager().addResolveInfoForIntent(
                DefaultAssistantPickerPreferenceController.ASSIST_SERVICE_PROBE,
                Lists.newArrayList(serviceResolveInfo));
        getShadowApplicationManager().addResolveInfoForIntent(
                DefaultAssistantPickerPreferenceController.ASSIST_ACTIVITY_PROBE,
                Lists.newArrayList(activityResolveInfoDifferentPackage));

        // One service, one activity.
        assertThat(mController.getCandidates()).hasSize(2);
    }

    @Test
    public void getCandidates_filtersServicesWithoutAssist_returnsOneElement() {
        // Included. Supports assist.
        ResolveInfo serviceResolveInfo = new ResolveInfo();
        serviceResolveInfo.serviceInfo = new ServiceInfo();
        serviceResolveInfo.serviceInfo.packageName = TEST_PACKAGE_NAME;
        serviceResolveInfo.serviceInfo.name = TEST_SERVICE;
        ShadowVoiceInteractionServiceInfo.setSupportsAssist(serviceResolveInfo.serviceInfo, true);

        // Not included. Doesn't support assist.
        ResolveInfo serviceResolveInfoNoAssist = new ResolveInfo();
        serviceResolveInfoNoAssist.serviceInfo = new ServiceInfo();
        ShadowVoiceInteractionServiceInfo.setSupportsAssist(serviceResolveInfoNoAssist.serviceInfo,
                false);

        getShadowApplicationManager().addResolveInfoForIntent(
                DefaultAssistantPickerPreferenceController.ASSIST_SERVICE_PROBE,
                Lists.newArrayList(serviceResolveInfo, serviceResolveInfoNoAssist));

        // Single service supporting assist.
        assertThat(mController.getCandidates()).hasSize(1);
    }

    @Test
    public void getCandidates_filtersSamePackageName_returnsOneElement() {
        // Included. Supports assist.
        ResolveInfo serviceResolveInfo = new ResolveInfo();
        serviceResolveInfo.serviceInfo = new ServiceInfo();
        serviceResolveInfo.serviceInfo.packageName = TEST_PACKAGE_NAME;
        serviceResolveInfo.serviceInfo.name = TEST_SERVICE;
        ShadowVoiceInteractionServiceInfo.setSupportsAssist(serviceResolveInfo.serviceInfo, true);

        // Not included. Same package as service above.
        ResolveInfo activityResolveInfo = new ResolveInfo();
        activityResolveInfo.activityInfo = new ActivityInfo();
        activityResolveInfo.activityInfo.packageName = TEST_PACKAGE_NAME;
        activityResolveInfo.activityInfo.name = TEST_ACTIVITY;

        getShadowApplicationManager().addResolveInfoForIntent(
                DefaultAssistantPickerPreferenceController.ASSIST_SERVICE_PROBE,
                Lists.newArrayList(serviceResolveInfo));
        getShadowApplicationManager().addResolveInfoForIntent(
                DefaultAssistantPickerPreferenceController.ASSIST_ACTIVITY_PROBE,
                Lists.newArrayList(activityResolveInfo));

        // Single service due to package name.
        assertThat(mController.getCandidates()).hasSize(1);
    }

    @Test
    public void getCurrentDefaultKey_noneSet_returnsNonePreferenceKey() {
        // Since Settings.Secure.ASSISTANT is not yet set, it should be null in SecureSettings.
        assertThat(mController.getCurrentDefaultKey()).isEqualTo(
                DefaultAppsPickerBasePreferenceController.NONE_PREFERENCE_KEY);
    }

    @Test
    public void getCurrentDefaultKey_testAssistantSet_returnsAssistantPreferenceKey() {
        String key = new ComponentName(TEST_PACKAGE_NAME, TEST_ACTIVITY).flattenToString();

        Settings.Secure.putStringForUser(mContext.getContentResolver(), Settings.Secure.ASSISTANT,
                key, TEST_USER_ID);

        assertThat(mController.getCurrentDefaultKey()).isEqualTo(key);
    }

    @Test
    public void setCurrentDefault_nullKey_setsNonePreference() {
        mController.setCurrentDefault(null);

        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ASSISTANT)).isEmpty();
        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.VOICE_INTERACTION_SERVICE)).isEmpty();
        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.VOICE_RECOGNITION_SERVICE)).isEmpty();
    }

    @Test
    public void setCurrentDefault_keyNotInCandidates_setsNonePreference() {
        ResolveInfo serviceResolveInfo = new ResolveInfo();
        serviceResolveInfo.serviceInfo = new ServiceInfo();
        serviceResolveInfo.serviceInfo.packageName = TEST_PACKAGE_NAME;
        serviceResolveInfo.serviceInfo.name = TEST_SERVICE;
        ShadowVoiceInteractionServiceInfo.setSupportsAssist(serviceResolveInfo.serviceInfo, true);

        ResolveInfo activityResolveInfoDifferentPackage = new ResolveInfo();
        activityResolveInfoDifferentPackage.activityInfo = new ActivityInfo();
        activityResolveInfoDifferentPackage.activityInfo.packageName = "com.other.package";
        activityResolveInfoDifferentPackage.activityInfo.name = TEST_ACTIVITY;

        getShadowApplicationManager().addResolveInfoForIntent(
                DefaultAssistantPickerPreferenceController.ASSIST_SERVICE_PROBE,
                Lists.newArrayList(serviceResolveInfo));
        getShadowApplicationManager().addResolveInfoForIntent(
                DefaultAssistantPickerPreferenceController.ASSIST_ACTIVITY_PROBE,
                Lists.newArrayList(activityResolveInfoDifferentPackage));
        mController.getCandidates();

        String testKey = new ComponentName("com.not.existent.key", "TestApp").flattenToString();
        mController.setCurrentDefault(testKey);

        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ASSISTANT)).isEmpty();
        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.VOICE_INTERACTION_SERVICE)).isEmpty();
        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.VOICE_RECOGNITION_SERVICE)).isEmpty();
    }

    @Test
    public void setCurrentDefaultKey_keySelectsService_setsService() {
        ResolveInfo serviceResolveInfo = new ResolveInfo();
        serviceResolveInfo.serviceInfo = new ServiceInfo();
        serviceResolveInfo.serviceInfo.packageName = TEST_PACKAGE_NAME;
        serviceResolveInfo.serviceInfo.name = TEST_SERVICE;
        ShadowVoiceInteractionServiceInfo.setSupportsAssist(serviceResolveInfo.serviceInfo, true);
        ShadowVoiceInteractionServiceInfo.setRecognitionService(serviceResolveInfo.serviceInfo,
                "TestRecognitionService");

        ResolveInfo activityResolveInfoDifferentPackage = new ResolveInfo();
        activityResolveInfoDifferentPackage.activityInfo = new ActivityInfo();
        activityResolveInfoDifferentPackage.activityInfo.packageName = "com.other.package";
        activityResolveInfoDifferentPackage.activityInfo.name = TEST_ACTIVITY;

        getShadowApplicationManager().addResolveInfoForIntent(
                DefaultAssistantPickerPreferenceController.ASSIST_SERVICE_PROBE,
                Lists.newArrayList(serviceResolveInfo));
        getShadowApplicationManager().addResolveInfoForIntent(
                DefaultAssistantPickerPreferenceController.ASSIST_ACTIVITY_PROBE,
                Lists.newArrayList(activityResolveInfoDifferentPackage));
        mController.getCandidates();

        String testKey = new ComponentName(TEST_PACKAGE_NAME, TEST_SERVICE).flattenToString();
        mController.setCurrentDefault(testKey);

        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ASSISTANT)).isEqualTo(testKey);
        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.VOICE_INTERACTION_SERVICE)).isEqualTo(testKey);
        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.VOICE_RECOGNITION_SERVICE)).isEqualTo(
                new ComponentName(TEST_PACKAGE_NAME, "TestRecognitionService").flattenToString());
    }

    @Test
    public void setCurrentDefaultKey_keySelectsActivity_setsActivity() {
        ResolveInfo serviceResolveInfo = new ResolveInfo();
        serviceResolveInfo.serviceInfo = new ServiceInfo();
        serviceResolveInfo.serviceInfo.packageName = TEST_PACKAGE_NAME;
        serviceResolveInfo.serviceInfo.name = TEST_SERVICE;
        ShadowVoiceInteractionServiceInfo.setSupportsAssist(serviceResolveInfo.serviceInfo, true);

        ResolveInfo activityResolveInfoDifferentPackage = new ResolveInfo();
        activityResolveInfoDifferentPackage.activityInfo = new ActivityInfo();
        activityResolveInfoDifferentPackage.activityInfo.packageName = "com.other.package";
        activityResolveInfoDifferentPackage.activityInfo.name = TEST_ACTIVITY;

        getShadowApplicationManager().addResolveInfoForIntent(
                DefaultAssistantPickerPreferenceController.ASSIST_SERVICE_PROBE,
                Lists.newArrayList(serviceResolveInfo));
        getShadowApplicationManager().addResolveInfoForIntent(
                DefaultAssistantPickerPreferenceController.ASSIST_ACTIVITY_PROBE,
                Lists.newArrayList(activityResolveInfoDifferentPackage));
        mController.getCandidates();

        String testKey = new ComponentName("com.other.package", TEST_ACTIVITY).flattenToString();

        mController.setCurrentDefault(testKey);

        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ASSISTANT)).isEqualTo(testKey);
        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.VOICE_INTERACTION_SERVICE)).isEmpty();
        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.VOICE_RECOGNITION_SERVICE)).isEmpty();
    }

    private ShadowPackageManager getShadowApplicationManager() {
        return Shadows.shadowOf(mContext.getPackageManager());
    }
}
