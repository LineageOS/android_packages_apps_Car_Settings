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

import static com.android.settingslib.drawer.CategoryKey.CATEGORY_DEVICE;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_ICON;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_ICON_URI;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_SUMMARY;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_TITLE;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.car.settings.R;
import com.android.car.settings.testutils.ShadowApplicationPackageManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.shadow.api.Shadow;

import java.util.Map;

/** Unit test for {@link ExtraSettingsLoader}. */
@RunWith(AndroidJUnit4.class)
public class ExtraSettingsLoaderTest {
    private Context mContext;
    private ExtraSettingsLoader mExtraSettingsLoader;
    private static final String META_DATA_PREFERENCE_CATEGORY = "com.android.settings.category";
    private static final String TEST_CONTENT_PROVIDER =
            "content://com.android.car.settings.testutils.TestContentProvider";
    private static final String FAKE_CATEGORY = "fake_category";
    private static final String FAKE_TITLE = "fake_title";
    private static final String FAKE_SUMMARY = "fake_summary";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        ShadowApplicationPackageManager.setResources(mContext.getResources());
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

        ResolveInfo resolveInfoSystem = createResolveInfo(bundle, /* isSystem= */ true);
        getShadowPackageManager().addResolveInfoForIntent(intent, resolveInfoSystem);
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

        ResolveInfo resolveInfoSystem = createResolveInfo(bundle, /* isSystem= */ true);
        getShadowPackageManager().addResolveInfoForIntent(intent, resolveInfoSystem);

        ResolveInfo resolveInfoNonSystem = createResolveInfo(bundle, /* isSystem= */ false);
        getShadowPackageManager().addResolveInfoForIntent(intent, resolveInfoNonSystem);

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
        bundle.putInt(META_DATA_PREFERENCE_TITLE, R.string.fake_title);
        bundle.putInt(META_DATA_PREFERENCE_SUMMARY, R.string.fake_summary);
        bundle.putInt(META_DATA_PREFERENCE_CATEGORY, R.string.fake_category);

        ResolveInfo resolveInfoSystem = createResolveInfo(bundle, /* isSystem= */ true);
        getShadowPackageManager().addResolveInfoForIntent(intent, resolveInfoSystem);

        ResolveInfo resolveInfoNonSystem = createResolveInfo(bundle, /* isSystem= */ false);
        getShadowPackageManager().addResolveInfoForIntent(intent, resolveInfoNonSystem);

        Map<Preference, Bundle> preferenceToBundleMap = mExtraSettingsLoader.loadPreferences(
                intent);

        assertThat(preferenceToBundleMap).hasSize(1);

        for (Preference p : preferenceToBundleMap.keySet()) {
            assertThat(p.getTitle()).isEqualTo(FAKE_TITLE);
            assertThat(p.getSummary()).isEqualTo(FAKE_SUMMARY);
            assertThat(p.getIcon()).isNull();
        }
    }

    @Test
    public void testLoadPreference_noDefaultSummary() {
        Intent intent = new Intent();
        intent.putExtra(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);
        Bundle bundle = new Bundle();
        bundle.putString(META_DATA_PREFERENCE_TITLE, FAKE_TITLE);
        bundle.putString(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);

        ResolveInfo resolveInfoSystem = createResolveInfo(bundle, /* isSystem= */ true);

        getShadowPackageManager().addResolveInfoForIntent(intent, resolveInfoSystem);
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
        intent.putExtra(META_DATA_PREFERENCE_CATEGORY, CATEGORY_DEVICE);
        Bundle bundle = new Bundle();
        bundle.putString(META_DATA_PREFERENCE_TITLE, FAKE_TITLE);
        bundle.putString(META_DATA_PREFERENCE_SUMMARY, FAKE_SUMMARY);

        ResolveInfo resolveInfoSystem = createResolveInfo(bundle, /* isSystem= */ true);

        getShadowPackageManager().addResolveInfoForIntent(intent, resolveInfoSystem);
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

        ResolveInfo resolveInfoSystem = createResolveInfo(bundle, /* isSystem= */ true);

        getShadowPackageManager().addResolveInfoForIntent(intent, resolveInfoSystem);
        Map<Preference, Bundle> preferenceToBundleMap = mExtraSettingsLoader.loadPreferences(
                intent);

        assertThat(preferenceToBundleMap).isEmpty();
    }

    @Test
    public void testLoadPreference_shouldLoadDefaultNullIcon() {
        Intent intent = new Intent();
        intent.putExtra(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);
        Bundle bundle = new Bundle();
        bundle.putString(META_DATA_PREFERENCE_TITLE, FAKE_TITLE);
        bundle.putString(META_DATA_PREFERENCE_SUMMARY, FAKE_SUMMARY);
        bundle.putString(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);

        ResolveInfo resolveInfoSystem = createResolveInfo(bundle, /* isSystem= */ true);
        getShadowPackageManager().addResolveInfoForIntent(intent, resolveInfoSystem);
        Map<Preference, Bundle> preferenceToBundleMap = mExtraSettingsLoader.loadPreferences(
                intent);

        for (Preference p : preferenceToBundleMap.keySet()) {
            assertThat(p.getTitle()).isEqualTo(FAKE_TITLE);
            assertThat(p.getSummary()).isEqualTo(FAKE_SUMMARY);
            assertThat(p.getIcon()).isNull();
        }
    }

    @Test
    public void testLoadPreference_uriResources_shouldNotLoadStaticResources() {
        Intent intent = new Intent();
        intent.putExtra(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);
        Bundle bundle = new Bundle();
        bundle.putString(META_DATA_PREFERENCE_TITLE, FAKE_TITLE);
        bundle.putString(META_DATA_PREFERENCE_SUMMARY, FAKE_SUMMARY);
        bundle.putString(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);
        bundle.putString(META_DATA_PREFERENCE_ICON_URI, TEST_CONTENT_PROVIDER);

        ResolveInfo resolveInfoSystem = createResolveInfo(bundle, /* isSystem= */ true);
        getShadowPackageManager().addResolveInfoForIntent(intent, resolveInfoSystem);
        Map<Preference, Bundle> preferenceToBundleMap = mExtraSettingsLoader.loadPreferences(
                intent);

        for (Preference p : preferenceToBundleMap.keySet()) {
            assertThat(p.getTitle()).isEqualTo(FAKE_TITLE);
            assertThat(p.getSummary()).isEqualTo(FAKE_SUMMARY);
            assertThat(p.getIcon()).isNull();
        }
    }

    @Test
    public void testLoadPreference_noSystemApp_returnsNoPreferences() {
        Intent intent = new Intent();
        intent.putExtra(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);
        Bundle bundle = new Bundle();
        bundle.putString(META_DATA_PREFERENCE_CATEGORY, FAKE_CATEGORY);

        ResolveInfo resolveInfoNonSystem1 = createResolveInfo(bundle, /* isSystem= */ false);
        getShadowPackageManager().addResolveInfoForIntent(intent, resolveInfoNonSystem1);

        ResolveInfo resolveInfoNonSystem2 = createResolveInfo(bundle, /* isSystem= */ false);
        getShadowPackageManager().addResolveInfoForIntent(intent, resolveInfoNonSystem2);

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

        ResolveInfo resolveInfoSystem1 = createResolveInfo(bundle, /* isSystem= */ true);
        getShadowPackageManager().addResolveInfoForIntent(intent, resolveInfoSystem1);

        ResolveInfo resolveInfoNonSystem1 = createResolveInfo(bundle, /* isSystem= */ false);
        getShadowPackageManager().addResolveInfoForIntent(intent, resolveInfoNonSystem1);

        ResolveInfo resolveInfoSystem2 = createResolveInfo(bundle, /* isSystem= */ true);
        getShadowPackageManager().addResolveInfoForIntent(intent, resolveInfoSystem2);

        Map<Preference, Bundle> preferenceToBundleMap = mExtraSettingsLoader.loadPreferences(
                intent);

        assertThat(preferenceToBundleMap).hasSize(2);

        for (Preference p : preferenceToBundleMap.keySet()) {
            assertThat(p.getTitle()).isEqualTo(FAKE_TITLE);
            assertThat(p.getSummary()).isEqualTo(FAKE_SUMMARY);
        }
    }

    private ShadowApplicationPackageManager getShadowPackageManager() {
        return Shadow.extract(mContext.getPackageManager());
    }

    private ResolveInfo createResolveInfo(Bundle bundle, boolean isSystem) {
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.metaData = bundle;
        activityInfo.packageName = "package_name";
        activityInfo.name = "class_name";

        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.system = isSystem;
        resolveInfo.activityInfo = activityInfo;

        return resolveInfo;
    }
}
