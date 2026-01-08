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

package com.android.car.settings.appwidget;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AlertDialog;
import android.app.Instrumentation;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.dx.mockito.inline.extended.ExtendedMockito;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class AllowBindAppWidgetActivityTest {

    private static final int APP_WIDGET_ID = 123;
    private static final String CALLING_PACKAGE = "com.android.test.package";
    private static final String PROVIDER_PACKAGE = "com.android.provider.package";
    private static final String PROVIDER_CLASS = "com.android.provider.Provider";
    private static final String APP_LABEL = "Test App";
    private static final String WIDGET_LABEL = "Test Widget";

    @Mock private AppWidgetManager mAppWidgetManager;
    @Mock private PackageManager mPackageManager;

    private MockitoSession mMockitoSession;
    private ComponentName mComponentName;
    private UserHandle mUserHandle;
    private AppWidgetProviderInfo mAppWidgetProviderInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        TestAllowBindAppWidgetActivity.sPackageManager = mPackageManager;
        TestAllowBindAppWidgetActivity.sCallingPackage = CALLING_PACKAGE;

        mMockitoSession = ExtendedMockito.mockitoSession()
                .mockStatic(AppWidgetManager.class)
                .strictness(Strictness.LENIENT)
                .startMocking();

        when(AppWidgetManager.getInstance(any())).thenReturn(mAppWidgetManager);

        mComponentName = new ComponentName(PROVIDER_PACKAGE, PROVIDER_CLASS);
        mUserHandle = android.os.Process.myUserHandle();

        // Setup Package Manager
        ApplicationInfo applicationInfo = new ApplicationInfo();
        try {
            when(mPackageManager.getApplicationInfo(eq(CALLING_PACKAGE), anyInt()))
                    .thenReturn(applicationInfo);
            when(mPackageManager.getApplicationLabel(applicationInfo)).thenReturn(APP_LABEL);
            when(mPackageManager.getText(eq(PROVIDER_PACKAGE), anyInt(), any()))
                    .thenReturn(WIDGET_LABEL);
        } catch (PackageManager.NameNotFoundException e) {
            // Should not happen
        }

        // Setup Widget Provider Info
        mAppWidgetProviderInfo = mock(AppWidgetProviderInfo.class);
        mAppWidgetProviderInfo.provider = mComponentName;
        when(mAppWidgetProviderInfo.loadLabel(any(PackageManager.class))).thenReturn(WIDGET_LABEL);

        when(mAppWidgetManager.getInstalledProviders())
                .thenReturn(Collections.singletonList(mAppWidgetProviderInfo));
    }

    @After
    public void tearDown() {
        if (mMockitoSession != null) {
            mMockitoSession.finishMocking();
        }
        TestAllowBindAppWidgetActivity.sPackageManager = null;
        TestAllowBindAppWidgetActivity.sCallingPackage = null;
    }

    @Test
    public void onCreate_showsDialogWithCorrectMessage() {
        try (ActivityScenario<TestAllowBindAppWidgetActivity> scenario = launchActivity()) {
            scenario.onActivity(activity -> {
                AlertDialog dialog = activity.mDialog;
                assertThat(dialog).isNotNull();
                assertThat(dialog.isShowing()).isTrue();

                TextView messageView = dialog.findViewById(android.R.id.message);
                assertThat(messageView.getText().toString()).contains(WIDGET_LABEL);
                assertThat(messageView.getText().toString()).contains(APP_LABEL);
            });
        }
    }

    @Test
    public void onPositiveClick_bindsWidget() {
        when(mAppWidgetManager.bindAppWidgetIdIfAllowed(
                eq(APP_WIDGET_ID), eq(mUserHandle), eq(mComponentName), any()))
                .thenReturn(true);

        try (ActivityScenario<TestAllowBindAppWidgetActivity> scenario = launchActivity()) {
            scenario.onActivity(activity -> {
                AlertDialog dialog = activity.mDialog;
                Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                button.performClick();
            });

            Instrumentation.ActivityResult result = scenario.getResult();
            assertThat(result.getResultCode()).isEqualTo(RESULT_OK);
            Intent resultData = result.getResultData();
            assertThat(resultData).isNotNull();
            assertThat(resultData.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))
                    .isEqualTo(APP_WIDGET_ID);

            verify(mAppWidgetManager).bindAppWidgetIdIfAllowed(
                    eq(APP_WIDGET_ID), eq(mUserHandle), eq(mComponentName), any());
        }
    }

    @Test
    public void onPositiveClick_bindFails_returnsCanceled() {
        when(mAppWidgetManager.bindAppWidgetIdIfAllowed(
                eq(APP_WIDGET_ID), eq(mUserHandle), eq(mComponentName), any()))
                .thenReturn(false);

        try (ActivityScenario<TestAllowBindAppWidgetActivity> scenario = launchActivity()) {
            scenario.onActivity(activity -> {
                AlertDialog dialog = activity.mDialog;
                Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                button.performClick();
            });

            Instrumentation.ActivityResult result = scenario.getResult();
            assertThat(result.getResultCode()).isNotEqualTo(RESULT_OK);

            verify(mAppWidgetManager).bindAppWidgetIdIfAllowed(
                    eq(APP_WIDGET_ID), eq(mUserHandle), eq(mComponentName), any());
        }
    }

    @Test
    public void onNegativeClick_returnsCanceled() {
        try (ActivityScenario<TestAllowBindAppWidgetActivity> scenario = launchActivity()) {
            scenario.onActivity(activity -> {
                AlertDialog dialog = activity.mDialog;
                Button button = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                button.performClick();
            });

            Instrumentation.ActivityResult result = scenario.getResult();
            assertThat(result.getResultCode()).isEqualTo(RESULT_CANCELED);

            verify(mAppWidgetManager, ExtendedMockito.never())
                    .bindAppWidgetIdIfAllowed(anyInt(), any(), any(), any());
        }
    }

    @Test
    public void onPositiveClick_withAlwaysAllow_setsPermission() {
        when(mAppWidgetManager.bindAppWidgetIdIfAllowed(
                eq(APP_WIDGET_ID), eq(mUserHandle), eq(mComponentName), any()))
                .thenReturn(true);
        when(mAppWidgetManager.hasBindAppWidgetPermission(anyString(), anyInt()))
                .thenReturn(true);

        try (ActivityScenario<TestAllowBindAppWidgetActivity> scenario = launchActivity()) {
            scenario.onActivity(activity -> {
                AlertDialog dialog = activity.mDialog;

                reset(mAppWidgetManager);
                when(mAppWidgetManager.hasBindAppWidgetPermission(anyString(), anyInt()))
                        .thenReturn(false);

                Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                button.performClick();
            });

            Instrumentation.ActivityResult result = scenario.getResult();
            assertThat(result.getResultCode()).isNotEqualTo(RESULT_OK);

            verify(mAppWidgetManager).setBindAppWidgetPermission(eq(CALLING_PACKAGE), eq(true));
        }
    }

    private ActivityScenario<TestAllowBindAppWidgetActivity> launchActivity() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                TestAllowBindAppWidgetActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, APP_WIDGET_ID);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, mComponentName);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, mUserHandle);
        return ActivityScenario.launchActivityForResult(intent);
    }

    public static class TestAllowBindAppWidgetActivity extends AllowBindAppWidgetActivity {
        static PackageManager sPackageManager;
        static String sCallingPackage;

        @Override
        public PackageManager getPackageManager() {
            if (sPackageManager != null) return sPackageManager;
            return super.getPackageManager();
        }

        @Override
        public String getCallingPackage() {
            if (sCallingPackage != null) return sCallingPackage;
            return super.getCallingPackage();
        }
    }
}
