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

package com.android.car.settings.common;

import static com.android.car.settings.common.ExtraSettingsLoader.DEVICE_CATEGORY;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_ICON;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_SUMMARY;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_TITLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.preference.Preference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.testutils.ShadowApplicationPackageManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/** Unit test for {@link ExtraSettingsLoader}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowApplicationPackageManager.class})
public class ExtraSettingsLoaderTest {
    private Context mContext;
    private ExtraSettingsLoader mExtraSettingsLoader;
    private static final String META_DATA_PREFERENCE_CATEGORY = "com.android.settings.category";
    private static final String FAKE_CATEGORY = "fake_category";
    private static final String FAKE_TITLE = "fake_title";
    private static final String FAKE_SUMMARY = "fake_summary";

    @Mock
    private Resources mResourcesMock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mExtraSettingsLoader = new ExtraSettingsLoader(mContext);
    }

    @After
    public void tearDown() {
        ShadowApplicationPackageManager.reset();
    }

    @Test
    public void testLoadPreference_stringResources_shouldLoadResources() {
        Intent intent = new Intent();
        intent.putExtra(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);
        Bundle bundle = new Bundle();
        bundle.putString(META_DATA_PREFERENCE_TITLE, FAKE_TITLE);
        bundle.putString(META_DATA_PREFERENCE_SUMMARY, FAKE_SUMMARY);
        bundle.putString(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);

        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.metaData = bundle;
        activityInfo.packageName = "package_name";
        activityInfo.name = "class_name";

        ResolveInfo resolveInfo_system = new ResolveInfo();
        resolveInfo_system.system = true;
        resolveInfo_system.activityInfo = activityInfo;

        ShadowApplicationPackageManager.setListOfActivities(
                new ArrayList<>(Arrays.asList(resolveInfo_system)));
        Map<Preference, Bundle> preferenceToBundleMap = mExtraSettingsLoader.loadPreferences(
                intent);

        assertThat(preferenceToBundleMap).hasSize(1);

        for (Preference p : preferenceToBundleMap.keySet()) {
            assertThat(p.getTitle()).isEqualTo(FAKE_TITLE);
            assertThat(p.getSummary()).isEqualTo(FAKE_SUMMARY);
        }
    }

    @Test
    public void testLoadPreference_metadataBundleIsValue() {
        Intent intent = new Intent();
        intent.putExtra(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);
        Bundle bundle = new Bundle();
        bundle.putString(META_DATA_PREFERENCE_TITLE, FAKE_TITLE);
        bundle.putString(META_DATA_PREFERENCE_SUMMARY, FAKE_SUMMARY);
        bundle.putString(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);

        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.metaData = bundle;
        activityInfo.packageName = "package_name";
        activityInfo.name = "class_name";

        ResolveInfo resolveInfo_system = new ResolveInfo();
        resolveInfo_system.system = true;
        resolveInfo_system.activityInfo = activityInfo;

        ResolveInfo resolveInfo_nonSystem = new ResolveInfo();
        resolveInfo_nonSystem.system = false;
        ShadowApplicationPackageManager.setListOfActivities(
                new ArrayList<>(Arrays.asList(resolveInfo_nonSystem, resolveInfo_system)));
        Map<Preference, Bundle> preferenceToBundleMap = mExtraSettingsLoader.loadPreferences(
                intent);

        assertThat(preferenceToBundleMap).hasSize(1);

        for (Preference p : preferenceToBundleMap.keySet()) {
            assertThat(p.getTitle()).isEqualTo(FAKE_TITLE);
            assertThat(p.getSummary()).isEqualTo(FAKE_SUMMARY);

            Bundle b = preferenceToBundleMap.get(p);
            assertThat(b.getString(META_DATA_PREFERENCE_TITLE)).isEqualTo(FAKE_TITLE);
            assertThat(b.getString(META_DATA_PREFERENCE_SUMMARY)).isEqualTo(FAKE_SUMMARY);
            assertThat(b.getString(META_DATA_PREFERENCE_CATEGORY)).isEqualTo(FAKE_CATEGORY);
            assertThat(b.getInt(META_DATA_PREFERENCE_ICON)).isNotNull();
        }
    }

    @Test
    public void testLoadPreference_integerResources_shouldLoadResources() {
        Intent intent = new Intent();
        intent.putExtra(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);
        Bundle bundle = new Bundle();
        bundle.putInt(META_DATA_PREFERENCE_TITLE, 1);
        bundle.putInt(META_DATA_PREFERENCE_SUMMARY, 2);
        bundle.putInt(META_DATA_PREFERENCE_CATEGORY, 3);

        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.metaData = bundle;
        activityInfo.packageName = "package_name";
        activityInfo.name = "class_name";

        ResolveInfo resolveInfo_system = new ResolveInfo();
        resolveInfo_system.system = true;
        resolveInfo_system.activityInfo = activityInfo;

        ResolveInfo resolveInfo_nonSystem = new ResolveInfo();
        resolveInfo_nonSystem.system = false;
        ShadowApplicationPackageManager.setListOfActivities(
                new ArrayList<>(Arrays.asList(resolveInfo_nonSystem, resolveInfo_system)));

        when(mResourcesMock.getString(1)).thenReturn(FAKE_TITLE);
        when(mResourcesMock.getString(2)).thenReturn(FAKE_SUMMARY);
        when(mResourcesMock.getString(3)).thenReturn(FAKE_CATEGORY);
        ShadowApplicationPackageManager.setResources(mResourcesMock);

        Map<Preference, Bundle> preferenceToBundleMap = mExtraSettingsLoader.loadPreferences(
                intent);

        assertThat(preferenceToBundleMap).hasSize(1);

        for (Preference p : preferenceToBundleMap.keySet()) {
            assertThat(p.getTitle()).isEqualTo(FAKE_TITLE);
            assertThat(p.getSummary()).isEqualTo(FAKE_SUMMARY);
            assertThat(p.getIcon()).isNotNull();

        }
    }

    @Test
    public void testLoadPreference_noDefaultSummary() {
        Intent intent = new Intent();
        intent.putExtra(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);
        Bundle bundle = new Bundle();
        bundle.putString(META_DATA_PREFERENCE_TITLE, FAKE_TITLE);
        bundle.putString(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);

        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.metaData = bundle;
        activityInfo.packageName = "package_name";
        activityInfo.name = "class_name";

        ResolveInfo resolveInfo_system = new ResolveInfo();
        resolveInfo_system.system = true;
        resolveInfo_system.activityInfo = activityInfo;

        ShadowApplicationPackageManager.setListOfActivities(
                new ArrayList<>(Arrays.asList(resolveInfo_system)));
        Map<Preference, Bundle> preferenceToBundleMap = mExtraSettingsLoader.loadPreferences(
                intent);

        for (Preference p : preferenceToBundleMap.keySet()) {
            assertThat(p.getTitle()).isEqualTo(FAKE_TITLE);
            assertThat(p.getSummary()).isNull();

        }
    }

    @Test
    public void testLoadPreference_noCategory_shouldSetToDeviceCategory() {
        Intent intent = new Intent();
        intent.putExtra(META_DATA_PREFERENCE_CATEGORY, DEVICE_CATEGORY);
        Bundle bundle = new Bundle();
        bundle.putString(META_DATA_PREFERENCE_TITLE, FAKE_TITLE);
        bundle.putString(META_DATA_PREFERENCE_SUMMARY, FAKE_SUMMARY);

        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.metaData = bundle;
        activityInfo.packageName = "package_name";
        activityInfo.name = "class_name";

        ResolveInfo resolveInfo_system = new ResolveInfo();
        resolveInfo_system.system = true;
        resolveInfo_system.activityInfo = activityInfo;

        ShadowApplicationPackageManager.setListOfActivities(
                new ArrayList<>(Arrays.asList(resolveInfo_system)));
        Map<Preference, Bundle> preferenceToBundleMap = mExtraSettingsLoader.loadPreferences(
                intent);

        assertThat(preferenceToBundleMap).hasSize(1);

        for (Preference p : preferenceToBundleMap.keySet()) {
            assertThat(p.getTitle()).isEqualTo(FAKE_TITLE);
            assertThat(p.getSummary()).isEqualTo(FAKE_SUMMARY);
        }
    }

    @Test
    public void testLoadPreference_noCategoryMatched_shouldNotReturnPreferences() {
        Intent intent = new Intent();
        intent.putExtra(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);
        Bundle bundle = new Bundle();
        bundle.putString(META_DATA_PREFERENCE_TITLE, FAKE_TITLE);
        bundle.putString(META_DATA_PREFERENCE_SUMMARY, FAKE_SUMMARY);

        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.metaData = bundle;
        activityInfo.packageName = "package_name";
        activityInfo.name = "class_name";

        ResolveInfo resolveInfo_system = new ResolveInfo();
        resolveInfo_system.system = true;
        resolveInfo_system.activityInfo = activityInfo;

        ShadowApplicationPackageManager.setListOfActivities(
                new ArrayList<>(Arrays.asList(resolveInfo_system)));
        Map<Preference, Bundle> preferenceToBundleMap = mExtraSettingsLoader.loadPreferences(
                intent);

        assertThat(preferenceToBundleMap).isEmpty();
    }

    @Test
    public void testLoadPreference_shouldLoadDefaultIcon() {
        Intent intent = new Intent();
        intent.putExtra(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);
        Bundle bundle = new Bundle();
        bundle.putString(META_DATA_PREFERENCE_TITLE, FAKE_TITLE);
        bundle.putString(META_DATA_PREFERENCE_SUMMARY, FAKE_SUMMARY);
        bundle.putString(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);

        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.metaData = bundle;
        activityInfo.packageName = "package_name";
        activityInfo.name = "class_name";

        ResolveInfo resolveInfo_system = new ResolveInfo();
        resolveInfo_system.system = true;
        resolveInfo_system.activityInfo = activityInfo;

        ShadowApplicationPackageManager.setListOfActivities(
                new ArrayList<>(Arrays.asList(resolveInfo_system)));
        Map<Preference, Bundle> preferenceToBundleMap = mExtraSettingsLoader.loadPreferences(
                intent);

        for (Preference p : preferenceToBundleMap.keySet()) {
            assertThat(p.getTitle()).isEqualTo(FAKE_TITLE);
            assertThat(p.getSummary()).isEqualTo(FAKE_SUMMARY);
            assertThat(p.getIcon()).isNotNull();
        }
    }

    @Test
    public void testLoadPreference_noSystemApp_returnsNoPreferences() {
        Intent intent = new Intent();
        intent.putExtra(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);
        Bundle bundle = new Bundle();
        bundle.putString(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);

        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.metaData = bundle;

        ResolveInfo resolveInfo_nonSystem_1 = new ResolveInfo();
        resolveInfo_nonSystem_1.system = false;
        resolveInfo_nonSystem_1.activityInfo = activityInfo;

        ResolveInfo resolveInfo_nonSystem_2 = new ResolveInfo();
        resolveInfo_nonSystem_2.system = false;
        resolveInfo_nonSystem_2.activityInfo = activityInfo;

        ShadowApplicationPackageManager.setListOfActivities(
                new ArrayList<>(Arrays.asList(resolveInfo_nonSystem_1, resolveInfo_nonSystem_2)));
        Map<Preference, Bundle> preferenceToBundleMap = mExtraSettingsLoader.loadPreferences(
                intent);

        assertThat(preferenceToBundleMap).isEmpty();
    }

    @Test
    public void testLoadPreference_systemApp_returnsPreferences() {
        Intent intent = new Intent();
        intent.putExtra(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);
        Bundle bundle = new Bundle();
        bundle.putString(META_DATA_PREFERENCE_TITLE, FAKE_TITLE);
        bundle.putString(META_DATA_PREFERENCE_SUMMARY, FAKE_SUMMARY);
        bundle.putString(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);

        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.metaData = bundle;
        activityInfo.packageName = "package_name";
        activityInfo.name = "class_name";

        ResolveInfo resolveInfo_system_1 = new ResolveInfo();
        resolveInfo_system_1.system = true;
        resolveInfo_system_1.activityInfo = activityInfo;

        ResolveInfo resolveInfo_nonSystem = new ResolveInfo();
        resolveInfo_nonSystem.system = false;
        resolveInfo_nonSystem.activityInfo = activityInfo;

        ResolveInfo resolveInfo_system_2 = new ResolveInfo();
        resolveInfo_system_2.system = true;
        resolveInfo_system_2.activityInfo = activityInfo;

        ShadowApplicationPackageManager.setListOfActivities(
                new ArrayList<>(Arrays.asList(resolveInfo_system_1, resolveInfo_nonSystem,
                        resolveInfo_system_2)));
        Map<Preference, Bundle> preferenceToBundleMap = mExtraSettingsLoader.loadPreferences(
                intent);

        assertThat(preferenceToBundleMap).hasSize(2);

        for (Preference p : preferenceToBundleMap.keySet()) {
            assertThat(p.getTitle()).isEqualTo(FAKE_TITLE);
            assertThat(p.getSummary()).isEqualTo(FAKE_SUMMARY);
        }
    }
}

