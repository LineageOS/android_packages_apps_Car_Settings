/*
 * Copyright (C) 2022 The Android Open Source Project
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionInfo;
import android.location.LocationManager;
import android.os.Build;
import android.os.PackageTagsList;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.testutils.TestLifecycleOwner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class LocationInfotainmentAppsPreferenceControllerTest {
    private static final String PACKAGE_NAME = "testApp";
    private final Context mContext = Mockito.spy(ApplicationProvider.getApplicationContext());
    private LifecycleOwner mLifecycleOwner;
    private LogicalPreferenceGroup mPreference;
    private LocationInfotainmentAppsPreferenceController mPreferenceController;

    @Mock
    private FragmentController mFragmentController;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private LocationManager mLocationManager;

    @Before
    @UiThreadTest
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = new TestLifecycleOwner();

        when(mContext.getSystemService(LocationManager.class)).thenReturn(mLocationManager);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        CarUxRestrictions carUxRestrictions =
                new CarUxRestrictions.Builder(
                                /* reqOpt= */ true,
                                CarUxRestrictions.UX_RESTRICTIONS_BASELINE,
                                /* timestamp= */ 0)
                        .build();

        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = new LogicalPreferenceGroup(mContext);
        screen.addPreference(mPreference);
        mPreferenceController =
                new LocationInfotainmentAppsPreferenceController(
                        mContext,
                        "key",
                        mFragmentController,
                        carUxRestrictions);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        doNothing().when(mContext).startActivity(any());

        initializePreference();
    }

    @Test
    public void refreshUi_oneInfotainmentApp_showApp() {
        mPreferenceController.refreshUi();
        assertThat(mPreference.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void refreshUi_infotainmentAppWithLocationPermission_launchLocationSettings() {
        mPreferenceController.refreshUi();
        mPreference.getPreference(0).performClick();

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(captor.capture());

        Intent intent = captor.getValue();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_MANAGE_APP_PERMISSION);
        assertThat(intent.getStringExtra(Intent.EXTRA_PERMISSION_GROUP_NAME))
                .isEqualTo(Manifest.permission_group.LOCATION);
    }

    @Test
    public void refreshUi_throwsNoException() throws Exception {
        when(mPackageManager.getApplicationInfoAsUser(any(), anyInt(), anyInt()))
                .thenThrow(new NameNotFoundException());

        mPreferenceController.refreshUi();
        assertThat(mPreference.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void refreshUi_isRequiredApp() {
        PackageTagsList list = new PackageTagsList.Builder().add(PACKAGE_NAME).build();
        when(mLocationManager.getAdasAllowlist()).thenReturn(list);

        mPreferenceController.refreshUi();
        assertThat(mPreference.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void refreshUi_permissionNotGranted_hideApp() {
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.targetSdkVersion = Build.VERSION_CODES.CUR_DEVELOPMENT;
        appInfo.enabled = true;

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = PACKAGE_NAME;
        packageInfo.applicationInfo = appInfo;
        packageInfo.requestedPermissions =
                new String[] {Manifest.permission.ACCESS_COARSE_LOCATION};
        packageInfo.requestedPermissionsFlags = new int[] {0};

        when(mPackageManager.getPackagesHoldingPermissions(any(), anyInt()))
                .thenReturn(List.of(packageInfo));

        mPreferenceController.refreshUi();
        assertThat(mPreference.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void refreshUi_appDisabled_hideApp() throws Exception {
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.targetSdkVersion = Build.VERSION_CODES.CUR_DEVELOPMENT;
        appInfo.enabled = false;

        when(mPackageManager.getApplicationInfoAsUser(any(), anyInt(), anyInt()))
                .thenReturn(appInfo);

        mPreferenceController.refreshUi();
        assertThat(mPreference.getPreferenceCount()).isEqualTo(0);
    }


    private void initializePreference() throws Exception {
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.targetSdkVersion = Build.VERSION_CODES.CUR_DEVELOPMENT;
        appInfo.enabled = true;

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = PACKAGE_NAME;
        packageInfo.applicationInfo = appInfo;
        packageInfo.requestedPermissions =
                new String[] {Manifest.permission.ACCESS_COARSE_LOCATION};
        packageInfo.requestedPermissionsFlags =
                new int[] {PackageInfo.REQUESTED_PERMISSION_GRANTED};

        when(mPackageManager.getApplicationInfoAsUser(any(), anyInt(), anyInt()))
                .thenReturn(appInfo);
        when(mPackageManager.getPackagesHoldingPermissions(any(), anyInt()))
                .thenReturn(List.of(packageInfo));
        when(mPackageManager.getPermissionFlags(any(), any(), any()))
                .thenReturn(PackageManager.FLAG_PERMISSION_USER_SENSITIVE_WHEN_GRANTED);
        when(mPackageManager.getPermissionInfo(any(), anyInt())).thenReturn(new PermissionInfo());

        when(mLocationManager.getAdasAllowlist()).thenReturn(new PackageTagsList.Builder().build());
        when(mLocationManager.isLocationEnabled()).thenReturn(false);

        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
    }
}
