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

package com.android.car.settings;

import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

/** Unit test for {@link Utils}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
public class UtilsTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void updatePreferenceToSpecificActivity_activityFound_updatesComponent() {
        // Arrange
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.flags |= ApplicationInfo.FLAG_SYSTEM;

        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.applicationInfo = applicationInfo;
        activityInfo.packageName = "some.test.package";
        activityInfo.name = "SomeActivity";

        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;

        Intent intent = new Intent();
        ShadowPackageManager packageManager = Shadows.shadowOf(mContext.getPackageManager());
        packageManager.addResolveInfoForIntent(intent, resolveInfo);

        Preference preference = new Preference(mContext);
        preference.setIntent(intent);

        // Act
        Utils.updatePreferenceToSpecificActivity(mContext, preference, /* flags= */ 0);

        // Assert
        Intent updatedIntent = preference.getIntent();
        assertThat(updatedIntent).isNotEqualTo(intent);
        assertThat(updatedIntent.getComponent()).isEqualTo(
                new ComponentName(activityInfo.packageName, activityInfo.name));
    }

    @Test
    public void updatePreferenceToSpecificActivity_activityFound_returnsTrue() {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.flags |= ApplicationInfo.FLAG_SYSTEM;

        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.applicationInfo = applicationInfo;
        activityInfo.packageName = "some.test.package";
        activityInfo.name = "SomeActivity";

        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;

        Intent intent = new Intent();
        ShadowPackageManager packageManager = Shadows.shadowOf(mContext.getPackageManager());
        packageManager.addResolveInfoForIntent(intent, resolveInfo);

        Preference preference = new Preference(mContext);
        preference.setIntent(intent);

        assertThat(Utils.updatePreferenceToSpecificActivity(mContext, preference, /* flags= */
                0)).isTrue();
    }

    @Test
    public void updatePreferenceToSpecificActivity_setTitleFlag_updatesTitle() {
        // Arrange
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.flags |= ApplicationInfo.FLAG_SYSTEM;

        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.applicationInfo = applicationInfo;
        activityInfo.packageName = "some.test.package";
        activityInfo.name = "SomeActivity";

        String label = "Activity Label";
        ResolveInfo resolveInfo = new ResolveInfo() {
            @Override
            public CharSequence loadLabel(PackageManager pm) {
                return label;
            }
        };
        resolveInfo.activityInfo = activityInfo;

        Intent intent = new Intent();
        ShadowPackageManager packageManager = Shadows.shadowOf(mContext.getPackageManager());
        packageManager.addResolveInfoForIntent(intent, resolveInfo);

        Preference preference = new Preference(mContext);
        preference.setIntent(intent);

        // Act
        assertThat(Utils.updatePreferenceToSpecificActivity(mContext, preference,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY)).isTrue();

        // Assert
        assertThat(preference.getTitle()).isEqualTo(label);
    }

    @Test
    public void updatePreferenceToSpecificActivityOrRemove_activityNotFound_returnsFalse() {
        Intent intent = new Intent();
        Preference preference = new Preference(mContext);
        preference.setIntent(intent);

        assertThat(Utils.updatePreferenceToSpecificActivity(mContext, preference,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY)).isFalse();
    }
}
