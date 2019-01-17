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

package com.android.car.settings.applications;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.PreferenceControllerTestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class ApplicationsSettingsPreferenceControllerTest {

    private static final String APP_NAME_1 = "Some Application";
    private static final String APP_NAME_2 = "Other Application";
    private static final String PKG_NAME_1 = "Some package";
    private static final String PKG_NAME_2 = "Other package";

    private PreferenceGroup mPreferenceGroup;
    private PreferenceControllerTestHelper<ApplicationsSettingsPreferenceController>
            mPreferenceControllerHelper;
    private ApplicationsSettingsPreferenceController mController;

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ResolveInfo mMockResolveInfo1;
    @Mock
    private ResolveInfo mMockResolveInfo2;
    @Mock
    private ActivityInfo mMockActivityInfo1;
    @Mock
    private ActivityInfo mMockActivityInfo2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context context = spy(RuntimeEnvironment.application);
        when(context.getPackageManager()).thenReturn(mPackageManager);

        mPreferenceGroup = new PreferenceManager(context).createPreferenceScreen(context);
        mPreferenceControllerHelper = new PreferenceControllerTestHelper<>(context,
                ApplicationsSettingsPreferenceController.class, mPreferenceGroup);
        mController = mPreferenceControllerHelper.getController();

        when(mMockResolveInfo1.loadLabel(any(PackageManager.class))).thenReturn(APP_NAME_1);
        when(mMockResolveInfo2.loadLabel(any(PackageManager.class))).thenReturn(APP_NAME_2);
        mMockActivityInfo1.packageName = PKG_NAME_1;
        mMockActivityInfo2.packageName = PKG_NAME_2;
        mMockResolveInfo1.activityInfo = mMockActivityInfo1;
        mMockResolveInfo2.activityInfo = mMockActivityInfo2;

        List<ResolveInfo> testList = new ArrayList<>();
        testList.add(mMockResolveInfo1);
        testList.add(mMockResolveInfo2);

        // Cannot use specific intent because it doesn't have a proper "equals" method.
        when(mPackageManager.queryIntentActivities(any(Intent.class),
                eq(PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
                        | PackageManager.MATCH_DISABLED_COMPONENTS))).thenReturn(testList);

        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
    }

    @Test
    public void refreshUi_hasElements() {
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    public void refreshUi_orderIsCorrect() {
        List<String> computedOrder = new ArrayList<>();
        for (int i = 0; i < mPreferenceGroup.getPreferenceCount(); i++) {
            computedOrder.add(mPreferenceGroup.getPreference(i).getTitle().toString());
        }

        assertThat(computedOrder).containsExactly(APP_NAME_2, APP_NAME_1);
    }

    @Test
    public void preferenceClick_launchesDetailFragment() {
        Preference preference = mPreferenceGroup.getPreference(0);
        preference.performClick();
        verify(mPreferenceControllerHelper.getMockFragmentController()).launchFragment(
                any(ApplicationDetailsFragment.class));
    }

    @Test
    public void refreshUi_multipleCalls() {
        mController.refreshUi();
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(2);

        // Second call shouldn't add more items to the list.
        mController.refreshUi();
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(2);
    }
}
