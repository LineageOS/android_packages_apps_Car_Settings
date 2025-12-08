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

package com.android.car.settings.notifications;

import static com.android.internal.notification.NotificationAccessConfirmationActivityContract.EXTRA_COMPONENT_NAME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.car.test.mocks.AndroidMockitoHelper;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.car.settings.common.ConfirmationDialogFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Locale;

/**
 * Unit tests for {@link NotificationAccessConfirmationActivity}
 */
@RunWith(AndroidJUnit4.class)
public final class NotificationAccessConfirmationActivityTest {
    private static final ComponentName TEMP_COMPONENT_NAME = ComponentName.unflattenFromString(
            "com.temp/com.temp.k");
    private static final String EXTRA_MOCK_SERVICE_INFO_PERMISSION = "mock_service_info_permission";
    private static final String EXTRA_MOCK_INTENT_FILTER_ACTION = "mock_intent_filter_action";

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ActivityScenario<TestActivity> mActivityScenario;
    private TestActivity mActivity;

    @Mock
    private NotificationManager mNotificationManager;

    private static ServiceInfo createServiceInfoForTempComponent(String permission) {
        ServiceInfo info = new ServiceInfo();
        info.name = TEMP_COMPONENT_NAME.getClassName();
        info.packageName = TEMP_COMPONENT_NAME.getPackageName();
        info.permission = permission;
        return info;
    }

    private static ApplicationInfo createApplicationInfo() {
        ApplicationInfo info = new ApplicationInfo();
        info.name = TEMP_COMPONENT_NAME.getClassName();
        info.packageName = TEMP_COMPONENT_NAME.getPackageName();
        return info;
    }

    private static ResolveInfo createResolveInfo(ServiceInfo service) {
        ResolveInfo info = new ResolveInfo();
        info.serviceInfo = service;
        return info;
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void componentNameEmpty_finishes() {
        launchActivityWithValidIntent(/* componentName */ null, /* hasPermission */ true,
                /* hasIntentFilter= */ true);
        assertThat(mActivityScenario.getState()).isEqualTo(Lifecycle.State.DESTROYED);
    }

    @Test
    public void showsDialog() throws Exception {
        launchActivityWithValidIntent(/* hasPermission */ true, /* hasIntentFilter= */ true);
        assertThat(mActivityScenario.getState()).isAtLeast(Lifecycle.State.CREATED);
        assertThat(mActivity.mFinishTriggered).isFalse();
        assertThat(getConfirmationDialog().isShowing()).isTrue();
    }

    @Test
    public void intentFilterMissing_finishes() {
        launchActivityWithValidIntent(/* hasPermission */ true, /* hasIntentFilter= */ false);
        assertThat(mActivityScenario.getState()).isAtLeast(Lifecycle.State.DESTROYED);
    }

    @Test
    public void permissionMissing_finishes() throws Exception {
        launchActivityWithValidIntent(/* hasPermission */ false, /* hasIntentFilter= */ true);
        assertThat(mActivityScenario.getState()).isAtLeast(Lifecycle.State.DESTROYED);
    }

    @Test
    public void onAllow_permissionAvailable_callsNotificationManager() throws Exception {
        launchActivityWithValidIntent(/* hasPermission */ true, /* hasIntentFilter= */ true);
        assertThat(mActivityScenario.getState()).isAtLeast(Lifecycle.State.CREATED);
        ServiceInfo info = createServiceInfoForTempComponent(
                Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE);

        AndroidMockitoHelper.syncRunOnUiThread(mActivity, () -> {
            getConfirmationDialog().getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        verify(mNotificationManager)
                .setNotificationListenerAccessGranted(eq(info.getComponentName()), eq(true));
    }

    @Test
    public void onDeny_finishes() throws Exception {
        launchActivityWithValidIntent(/* hasPermission */ true, /* hasIntentFilter= */ true);
        assertThat(mActivityScenario.getState()).isAtLeast(Lifecycle.State.CREATED);

        AndroidMockitoHelper.syncRunOnUiThread(mActivity, () -> {
            getConfirmationDialog().getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Not asserting DESTROYED state as mActivityScenario.getState() returns STARTED state for
        // some unknown reason.
        assertThat(mActivity.mFinishTriggered).isTrue();
    }

    private AlertDialog getConfirmationDialog() {
        return (AlertDialog) ((ConfirmationDialogFragment) mActivity.getSupportFragmentManager()
                .findFragmentByTag(ConfirmationDialogFragment.TAG))
                .getDialog();
    }

    private void launchActivityWithValidIntent(ComponentName componentName, boolean hasPermission,
            boolean hasIntentFilter) {
        Intent intent = new Intent(mContext, TestActivity.class)
                .putExtra(EXTRA_MOCK_SERVICE_INFO_PERMISSION, hasPermission)
                .putExtra(EXTRA_MOCK_INTENT_FILTER_ACTION, hasIntentFilter);
        if (componentName != null) {
            intent = intent.putExtra(EXTRA_COMPONENT_NAME, componentName);
        }

        mActivityScenario = ActivityScenario.launch(intent);
        if (hasPermission && hasIntentFilter && componentName != null) {
            // Activity will only complete onCreate() without calling finish() if the Intent is
            // well-formed and contains the proper permission. Otherwise, it will go to state
            // DESTROYED before entering onActivity.
            // Check the Lifecycle of the ActivityScenario to see if it completed successfully.
            mActivityScenario.onActivity(
                    activity -> {
                        mActivity = activity;
                        mActivity.setNotificationManagerSpy(mNotificationManager);
                    });
        }
    }

    private void launchActivityWithValidIntent(boolean hasPermission, boolean hasIntentFilter) {
        launchActivityWithValidIntent(TEMP_COMPONENT_NAME, hasPermission, hasIntentFilter);
    }

    public static final class TestActivity extends NotificationAccessConfirmationActivity {
        @Nullable private NotificationManager mNotificationManagerSpy;
        @Nullable private PackageManager mPackageManagerSpy;
        @Nullable private PackageManager mPmWithPermission;
        @Nullable private PackageManager mPmWithNoPermission;

        boolean mFinishTriggered;
        // {@code true} if we want to mock this TestActivity return a ServiceInfo with
        // {@link Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE}
        boolean mMockServiceInfoPermission = true;
        // {@code true} if we want to return mock this TestActivity to have implemented
        // Notification Listener Service intent-filter.
        boolean mMockIntentFilterAction = true;

        @Override
        protected void onCreate(@android.annotation.Nullable Bundle savedInstanceState) {
            mMockServiceInfoPermission = getIntent().getBooleanExtra(
                    EXTRA_MOCK_SERVICE_INFO_PERMISSION, false);
            mMockIntentFilterAction = getIntent().getBooleanExtra(
                    EXTRA_MOCK_INTENT_FILTER_ACTION, false);
            super.onCreate(savedInstanceState);
        }

        void setNotificationManagerSpy(NotificationManager notificationManagerSpy) {
            mNotificationManagerSpy = notificationManagerSpy;
        }

        @Override
        public PackageManager getPackageManager() {
            try {
                if (mMockIntentFilterAction) {
                    if (mMockServiceInfoPermission) {
                        return getPmWithPermission();
                    }
                    return getPmWithNoPermission();
                }
                if (mPackageManagerSpy == null) {
                    mPackageManagerSpy = spy(super.getPackageManager());
                    doReturn(createApplicationInfo()).when(mPackageManagerSpy)
                            .getApplicationInfo(any(), any());
                }
            } catch (PackageManager.NameNotFoundException e) {
                // do nothing... tests will fail when activity finishes during onCreate()
            }
            return mPackageManagerSpy;
        }

        private PackageManager getPmWithPermission() throws PackageManager.NameNotFoundException {
            if (mPmWithPermission == null) {
                mPmWithPermission = spy(super.getPackageManager());
                doReturn(createApplicationInfo()).when(mPmWithPermission)
                        .getApplicationInfo(any(), any());
                ResolveInfo infoWithPermission = createResolveInfo(
                        createServiceInfoForTempComponent(
                        Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE));
                doReturn(Collections.singletonList(infoWithPermission)).when(mPmWithPermission)
                        .queryIntentServicesAsUser(any(Intent.class), any(int.class),
                                any(int.class));
            }
            return mPmWithPermission;
        }

        private PackageManager getPmWithNoPermission() throws PackageManager.NameNotFoundException {
            if (mPmWithNoPermission == null) {
                mPmWithNoPermission = spy(super.getPackageManager());
                doReturn(createApplicationInfo()).when(mPmWithNoPermission)
                        .getApplicationInfo(any(), any());
                ResolveInfo infoWithPermission = createResolveInfo(
                        createServiceInfoForTempComponent(/* permission= */ ""));
                doReturn(Collections.singletonList(infoWithPermission)).when(mPmWithNoPermission)
                                .queryIntentServicesAsUser(any(Intent.class), any(int.class),
                                        any(int.class));
            }
            return mPmWithNoPermission;
        }

        @Override
        public Object getSystemService(String name) {
            return mNotificationManagerSpy != null
                    && name.toLowerCase(Locale.ROOT).contains("notification")
                    ? mNotificationManagerSpy
                    : super.getSystemService(name);
        }

        @Override
        public void finish() {
            mFinishTriggered = true;
            super.finish();
        }
    }
}
