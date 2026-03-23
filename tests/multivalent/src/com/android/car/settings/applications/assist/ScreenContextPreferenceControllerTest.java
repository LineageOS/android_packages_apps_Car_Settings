/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.car.settings.applications.assist;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AppOpsManager;
import android.car.drivingstate.CarUxRestrictions;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.testutils.TestLifecycleOwner;
import com.android.internal.app.AssistUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class ScreenContextPreferenceControllerTest {

    private static final String DEFAULT_ASSISTANT_PKG = "com.android.assistant";
    private static final int UID = 12345;

    @Mock
    private FragmentController mFragmentController;
    @Mock
    private AssistUtils mAssistUtils;
    @Mock
    private AppOpsManager mAppOpsManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private Context mMockContext;

    private LifecycleOwner mLifecycleOwner;
    private ScreenContextPreferenceController mPreferenceController;
    private TwoStatePreference mPreference;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = new TestLifecycleOwner();
        Context context = ApplicationProvider.getApplicationContext();

        when(mMockContext.getSystemService(AppOpsManager.class)).thenReturn(mAppOpsManager);
        when(mMockContext.getPackageManager()).thenReturn(mPackageManager);
        when(mMockContext.getContentResolver()).thenReturn(context.getContentResolver());
        when(mMockContext.getSystemService(Context.UI_MODE_SERVICE)).thenReturn(
                context.getSystemService(Context.UI_MODE_SERVICE));
        when(mMockContext.getMainLooper()).thenReturn(context.getMainLooper());
        when(mMockContext.getTheme()).thenReturn(context.getTheme());
        when(mMockContext.getResources()).thenReturn(context.getResources());
        when(mPackageManager.getPackageUidAsUser(anyString(), anyInt())).thenReturn(UID);

        mPreferenceController = new ScreenContextPreferenceController(mMockContext,
                "key", mFragmentController, new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_FULLY_RESTRICTED, 0).build(), mAssistUtils);
        mPreference = new SwitchPreference(context);

        ComponentName assistComponent = new ComponentName(
                DEFAULT_ASSISTANT_PKG, "AssistantActivity");
        when(mAssistUtils.getAssistComponentForUser(anyInt())).thenReturn(assistComponent);
    }

    @Test
    public void testUpdateState_defaultOn() {
        when(mAppOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_ASSIST_STRUCTURE, UID, DEFAULT_ASSISTANT_PKG))
                .thenReturn(AppOpsManager.MODE_DEFAULT);

        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void testUpdateState_allowed() {
        when(mAppOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_ASSIST_STRUCTURE, UID, DEFAULT_ASSISTANT_PKG))
                .thenReturn(AppOpsManager.MODE_ALLOWED);

        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void testUpdateState_ignored() {
        when(mAppOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_ASSIST_STRUCTURE, UID, DEFAULT_ASSISTANT_PKG))
                .thenReturn(AppOpsManager.MODE_IGNORED);

        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void testHandlePreferenceChanged_checked_setsModeAllowed() {
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        mPreferenceController.handlePreferenceChanged(mPreference, true);

        verify(mAppOpsManager).setUidMode(
                AppOpsManager.OPSTR_ASSIST_STRUCTURE, UID, AppOpsManager.MODE_ALLOWED);
    }

    @Test
    public void testHandlePreferenceChanged_unchecked_setsModeIgnored() {
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        mPreferenceController.handlePreferenceChanged(mPreference, false);

        verify(mAppOpsManager).setUidMode(
                AppOpsManager.OPSTR_ASSIST_STRUCTURE, UID, AppOpsManager.MODE_IGNORED);
    }

    @Test
    public void testGetAvailabilityStatus_withAssistant() {
        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(
                ScreenContextPreferenceController.AVAILABLE);
    }

    @Test
    public void testGetAvailabilityStatus_withoutAssistant() {
        when(mAssistUtils.getAssistComponentForUser(anyInt())).thenReturn(null);
        mPreferenceController = new ScreenContextPreferenceController(mMockContext,
                "key", mFragmentController, new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_FULLY_RESTRICTED, 0).build(), mAssistUtils);
        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(
                ScreenContextPreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void testOnStart_registersOpChangedListener() {
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        verify(mAppOpsManager).startWatchingMode(
                eq(AppOpsManager.OPSTR_ASSIST_STRUCTURE), isNull(),
                any(AppOpsManager.OnOpChangedListener.class));
    }

    @Test
    public void testOnStop_unregistersOpChangedListener() {
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
        mPreferenceController.onStop(mLifecycleOwner);

        verify(mAppOpsManager).stopWatchingMode(any(AppOpsManager.OnOpChangedListener.class));
    }

    @Test
    public void testOnOpChanged_updatesPreference() {
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        ArgumentCaptor<AppOpsManager.OnOpChangedListener> captor =
                ArgumentCaptor.forClass(AppOpsManager.OnOpChangedListener.class);
        verify(mAppOpsManager).startWatchingMode(
                eq(AppOpsManager.OPSTR_ASSIST_STRUCTURE), isNull(), captor.capture());

        when(mAppOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_ASSIST_STRUCTURE, UID, DEFAULT_ASSISTANT_PKG))
                .thenReturn(AppOpsManager.MODE_ALLOWED);

        captor.getValue().onOpChanged(AppOpsManager.OPSTR_ASSIST_STRUCTURE, DEFAULT_ASSISTANT_PKG);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(mPreference.isChecked()).isTrue();
    }
}
