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
package com.android.car.settings.location;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.LogicalPreferenceGroup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class LocationFooterPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "location_footer";
    private static final String TEST_TEXT = "sample text";
    private static final int TEST_RES_ID = 1024;

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private Resources mResources;

    private LocationFooterPreferenceController mController;
    private PreferenceScreen mScreen;
    private PreferenceGroup mGroup;
    private List<ResolveInfo> mResolveInfos;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        Context context = RuntimeEnvironment.application;
        mController = new LocationFooterPreferenceController(context, PREFERENCE_KEY,
                mock(FragmentController.class), mPackageManager);
        mResolveInfos = new ArrayList<>();
        mGroup = new LogicalPreferenceGroup(context);
        mGroup.setKey(PREFERENCE_KEY);
        mScreen = new PreferenceManager(context).createPreferenceScreen(context);
        mScreen.addPreference(mGroup);

        when(mPackageManager.queryBroadcastReceivers(any(Intent.class), anyInt()))
                .thenReturn(mResolveInfos);
        when(mPackageManager.getResourcesForApplication(any(ApplicationInfo.class)))
                .thenReturn(mResources);
        when(mResources.getString(TEST_RES_ID)).thenReturn(TEST_TEXT);
    }

    // Visibility Tests.
    @Test
    public void footer_isVisibleWhenThereAreValidInjections() {
        mResolveInfos.add(
                getTestResolveInfo(/* isSystemApp= */ true, /* hasRequiredMetadata= */ true));
        mController.displayPreference(mScreen);

        assertThat(mGroup.isVisible()).isTrue();
    }

    @Test
    public void footer_isHiddenWhenThereAreNoValidInjections_NotSystemApp() {
        mResolveInfos.add(
                getTestResolveInfo(/* isSystemApp= */ false, /* hasRequiredMetadata= */ true));
        mController.displayPreference(mScreen);

        assertThat(mGroup.isVisible()).isFalse();
    }

    @Test
    public void footer_isHiddenWhenThereAreNoValidInjections_NoMetaData() {
        mResolveInfos.add(
                getTestResolveInfo(/* isSystemApp= */ true, /* hasRequiredMetadata= */ false));
        mController.displayPreference(mScreen);

        assertThat(mGroup.isVisible()).isFalse();
    }

    // Correctness Tests.
    @Test
    public void displayPreference_addsInjectedFooterToGroup() {
        int numFooters = 3;
        for (int i = 0; i < numFooters; i++) {
            mResolveInfos.add(
                    getTestResolveInfo(/* isSystemApp= */ true, /* hasRequiredMetadata= */ true));
        }
        mController.displayPreference(mScreen);

        assertThat(mGroup.getPreferenceCount()).isEqualTo(numFooters);
    }

    @Test
    public void displayPreference_injectedFooterHasCorrectText() {
        mResolveInfos.add(
                getTestResolveInfo(/* isSystemApp= */ true, /* hasRequiredMetadata= */ true));
        mController.displayPreference(mScreen);

        assertThat(mGroup.getPreference(0).getSummary()).isEqualTo(TEST_TEXT);
    }

    // Broadcast Tests.
    @Test
    public void displayPreference_broadcastsFooterDisplayedIntentForValidInjections() {
        ResolveInfo testResolveInfo =
                getTestResolveInfo(/* isSystemApp= */ true, /* hasRequiredMetadata= */ true);
        mResolveInfos.add(testResolveInfo);
        mController.displayPreference(mScreen);

        List<Intent> intentsFired = ShadowApplication.getInstance().getBroadcastIntents();
        assertThat(intentsFired).hasSize(1);
        Intent intentFired = intentsFired.get(0);
        assertThat(intentFired.getAction()).isEqualTo(
                LocationManager.SETTINGS_FOOTER_DISPLAYED_ACTION);
        assertThat(intentFired.getComponent()).isEqualTo(testResolveInfo
                .getComponentInfo().getComponentName());
    }

    @Test
    public void displayPreference_doesNotBroadcastFooterDisplayedIntentIfNoValidInjections() {
        mResolveInfos.add(
                getTestResolveInfo(/* isSystemApp= */ false, /* hasRequiredMetadata= */ true));
        mController.displayPreference(mScreen);

        List<Intent> intentsFired = ShadowApplication.getInstance().getBroadcastIntents();
        assertThat(intentsFired).isEmpty();
    }

    @Test
    public void onStop_broadcastsFooterRemovedIntent() {
        mResolveInfos.add(
                getTestResolveInfo(/* isSystemApp= */ true, /* hasRequiredMetadata= */ true));
        mController.displayPreference(mScreen);
        mController.onStop();

        List<Intent> intentsFired = ShadowApplication.getInstance().getBroadcastIntents();
        assertThat(intentsFired).hasSize(2);
        Intent intentFired = intentsFired.get(1);
        assertThat(intentFired.getAction()).isEqualTo(
                LocationManager.SETTINGS_FOOTER_REMOVED_ACTION);
    }

    /**
     * Returns a ResolveInfo object for testing.
     *
     * <p>Injections are only valid if they are both a system app, and have the required METADATA.
     *
     * @param isSystemApp         true if the application is a system app.
     * @param hasRequiredMetaData true if the broadcast receiver has a valid value for
     *                            {@link LocationManager#METADATA_SETTINGS_FOOTER_STRING}
     */
    private ResolveInfo getTestResolveInfo(boolean isSystemApp, boolean hasRequiredMetaData) {
        ResolveInfo testResolveInfo = new ResolveInfo();
        ApplicationInfo testAppInfo = new ApplicationInfo();
        if (isSystemApp) {
            testAppInfo.flags |= ApplicationInfo.FLAG_SYSTEM;
        }
        ActivityInfo testActivityInfo = new ActivityInfo();
        testActivityInfo.name = "TestActivityName";
        testActivityInfo.packageName = "TestPackageName";
        testActivityInfo.applicationInfo = testAppInfo;
        if (hasRequiredMetaData) {
            testActivityInfo.metaData = new Bundle();
            testActivityInfo.metaData.putInt(
                    LocationManager.METADATA_SETTINGS_FOOTER_STRING, TEST_RES_ID);
        }
        testResolveInfo.activityInfo = testActivityInfo;
        return testResolveInfo;
    }
}
