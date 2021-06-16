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

package com.android.car.settings.system.legal;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.testutils.TestLifecycleOwner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/** Unit test for {@link LegalPreferenceController}. */
@RunWith(AndroidJUnit4.class)
public class LegalPreferenceControllerTest {
    private static final String TEST_LABEL = "test_label";

    private Context mContext = ApplicationProvider.getApplicationContext();
    private LifecycleOwner mLifecycleOwner;
    private Preference mPreference;
    private LegalPreferenceController mPreferenceController;
    private CarUxRestrictions mCarUxRestrictions;

    @Mock
    private FragmentController mFragmentController;
    @Mock
    private PackageManager mMockPm;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = new TestLifecycleOwner();
        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();

        mPreference = new Preference(mContext);
        mPreferenceController = new TestLegalPreferenceController(mContext,
                "key", mFragmentController, mCarUxRestrictions, mMockPm);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
    }

    @Test
    public void onCreate_intentResolvesToActivity_isVisible() {
        Intent intent = mPreferenceController.getIntent();

        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = "some.test.package";
        activityInfo.name = "SomeActivity";
        activityInfo.applicationInfo = new ApplicationInfo();
        activityInfo.applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;

        ResolveInfo resolveInfo = new ResolveInfo() {
            @Override
            public CharSequence loadLabel(PackageManager pm) {
                return TEST_LABEL;
            }
        };
        resolveInfo.activityInfo = activityInfo;
        List<ResolveInfo> list = new LinkedList();
        list.add(resolveInfo);

        when(mMockPm.queryIntentActivities(eq(intent), anyInt())).thenReturn(list);

        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void onCreate_intentResolvesToActivity_updatesTitle() {
        Intent intent = mPreferenceController.getIntent();

        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = "some.test.package";
        activityInfo.name = "SomeActivity";
        activityInfo.applicationInfo = new ApplicationInfo();
        activityInfo.applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;
        activityInfo.applicationInfo.nonLocalizedLabel = TEST_LABEL;

        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;

        List<ResolveInfo> list = new LinkedList();
        list.add(resolveInfo);

        when(mMockPm.queryIntentActivities(eq(intent), anyInt())).thenReturn(list);

        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.getTitle()).isEqualTo(TEST_LABEL);
    }

    @Test
    public void onCreate_intentResolvesToActivity_updatesIntentToSpecificActivity() {
        Intent intent = mPreferenceController.getIntent();

        String packageName = "com.android.car.settings.testutils";
        String activityName = "BaseTestActivity";

        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = packageName;
        activityInfo.name = activityName;
        activityInfo.applicationInfo = new ApplicationInfo();
        activityInfo.applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;

        ResolveInfo resolveInfo = new ResolveInfo() {
            @Override
            public CharSequence loadLabel(PackageManager pm) {
                return TEST_LABEL;
            }
        };
        resolveInfo.activityInfo = activityInfo;
        List<ResolveInfo> list = new LinkedList();
        list.add(resolveInfo);

        when(mMockPm.queryIntentActivities(eq(intent), anyInt())).thenReturn(list);

        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.getIntent().getComponent().flattenToString()).isEqualTo(
                packageName + "/" + activityName);
    }

    @Test
    public void onCreate_intentResolvesToNull_isNotVisible() {
        when(mMockPm.queryIntentActivities(eq(mPreferenceController.getIntent()), anyInt()))
                .thenReturn(Collections.emptyList());
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.isVisible()).isFalse();
    }

    private static class TestLegalPreferenceController extends LegalPreferenceController {
        private static final Intent INTENT = new Intent("test_intent");

        TestLegalPreferenceController(Context context, String preferenceKey,
                FragmentController fragmentController, CarUxRestrictions uxRestrictions,
                PackageManager packageManager) {
            super(context, preferenceKey, fragmentController, uxRestrictions, packageManager);
        }

        @Override
        protected Intent getIntent() {
            return INTENT;
        }
    }
}
