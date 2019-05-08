/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.car.settings.applications.specialaccess;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.applications.specialaccess.AppStateAppOpsBridge.PermissionState;
import com.android.car.settings.testutils.ShadowAppOpsManager;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowUserManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Unit test for {@link AppStateAppOpsBridge}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowAppOpsManager.class})
public class AppStateAppOpsBridgeTest {

    private static final int APP_OP_CODE = AppOpsManager.OP_WRITE_SETTINGS;
    private static final String PERMISSION = Manifest.permission.WRITE_SETTINGS;

    @Mock
    private IPackageManager mIPackageManager;
    @Mock
    private ApplicationsState mApplicationsState;
    @Mock
    private ApplicationsState.Session mSession;
    @Mock
    private ParceledListSlice<PackageInfo> mParceledPackages;
    @Mock
    private ParceledListSlice<PackageInfo> mParceledPackagesOtherProfile;

    private List<PackageInfo> mPackages;
    private ArrayList<AppEntry> mAppEntries;

    private Context mContext;
    private AppOpsManager mAppOpsManager;
    private AppStateAppOpsBridge mBridge;

    @Before
    public void setUp() throws RemoteException {
        MockitoAnnotations.initMocks(this);
        mPackages = new ArrayList<>();
        when(mIPackageManager.getPackagesHoldingPermissions(
                AdditionalMatchers.aryEq(new String[]{PERMISSION}),
                eq(PackageManager.GET_PERMISSIONS),
                eq(UserHandle.myUserId())))
                .thenReturn(mParceledPackages);
        when(mParceledPackages.getList()).thenReturn(mPackages);

        mAppEntries = new ArrayList<>();
        when(mApplicationsState.newSession(any())).thenReturn(mSession);
        when(mApplicationsState.getBackgroundLooper()).thenReturn(Looper.getMainLooper());
        when(mSession.getAllApps()).thenReturn(mAppEntries);

        mContext = RuntimeEnvironment.application;
        mAppOpsManager = (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);
        mBridge = new AppStateAppOpsBridge(mContext, mApplicationsState, APP_OP_CODE, PERMISSION,
                mock(AppStateBaseBridge.Callback.class), mIPackageManager);
    }

    @Test
    public void androidPackagesIgnored() throws RemoteException {
        String packageName = "android";
        int uid = UserHandle.getUid(UserHandle.myUserId(), /* appId= */ 1);
        PackageInfo packageInfo = createPackageInfo(packageName, uid);
        addPackageWithPermission(packageInfo, AppOpsManager.MODE_ALLOWED);
        addEntry(packageInfo);

        AppEntry entry = mAppEntries.get(0);

        mBridge.start();

        assertThat(entry.extraInfo).isNull();
    }

    @Test
    public void selfPackageIgnored() throws RemoteException {
        String packageName = mContext.getPackageName();
        int uid = UserHandle.getUid(UserHandle.myUserId(), /* appId= */ 1);
        PackageInfo packageInfo = createPackageInfo(packageName, uid);
        addPackageWithPermission(packageInfo, AppOpsManager.MODE_ALLOWED);
        addEntry(packageInfo);

        AppEntry entry = mAppEntries.get(0);

        mBridge.start();

        assertThat(entry.extraInfo).isNull();
    }

    @Test
    public void packagesNotRequestingPermissionIgnored() throws RemoteException {
        String packageName = "test.package";
        int uid = UserHandle.getUid(UserHandle.myUserId(), /* appId= */ 1);
        PackageInfo packageInfo = createPackageInfo(packageName, uid);
        packageInfo.requestedPermissions = null;
        mPackages.add(packageInfo);
        when(mIPackageManager.isPackageAvailable(packageInfo.packageName,
                UserHandle.myUserId())).thenReturn(true);
        addEntry(packageInfo);

        AppEntry entry = mAppEntries.get(0);

        mBridge.start();

        assertThat(entry.extraInfo).isNull();
    }

    @Test
    public void unavailablePackageIgnored() throws RemoteException {
        String packageName = "test.package";
        int uid = UserHandle.getUid(UserHandle.myUserId(), /* appId= */ 1);
        PackageInfo packageInfo = createPackageInfo(packageName, uid);
        addPackageWithPermission(packageInfo, AppOpsManager.MODE_ALLOWED);
        addEntry(packageInfo);

        when(mIPackageManager.isPackageAvailable(packageInfo.packageName,
                UserHandle.myUserId())).thenReturn(false);

        AppEntry entry = mAppEntries.get(0);

        mBridge.start();

        assertThat(entry.extraInfo).isNull();
    }

    @Test
    public void loadsAppOpsExtraInfo_modeAllowed_isPermissible() throws RemoteException {
        String packageName = "test.package";
        int uid = UserHandle.getUid(UserHandle.myUserId(), /* appId= */ 1);
        PackageInfo packageInfo = createPackageInfo(packageName, uid);
        addPackageWithPermission(packageInfo, AppOpsManager.MODE_ALLOWED);
        addEntry(packageInfo);

        AppEntry entry = mAppEntries.get(0);
        assertThat(entry.extraInfo).isNull();

        mBridge.start();

        assertThat(entry.extraInfo).isNotNull();
        assertThat(((PermissionState) entry.extraInfo).isPermissible()).isTrue();
    }

    @Test
    public void loadsAppOpsExtraInfo_modeDefault_isPermissible() throws RemoteException {
        String packageName = "test.package";
        int uid = UserHandle.getUid(UserHandle.myUserId(), /* appId= */ 1);
        PackageInfo packageInfo = createPackageInfo(packageName, uid);
        addPackageWithPermission(packageInfo, AppOpsManager.MODE_DEFAULT);
        addEntry(packageInfo);

        AppEntry entry = mAppEntries.get(0);
        assertThat(entry.extraInfo).isNull();

        mBridge.start();

        assertThat(entry.extraInfo).isNotNull();
        assertThat(((PermissionState) entry.extraInfo).isPermissible()).isTrue();
    }

    @Test
    public void loadsAppOpsExtraInfo_modeIgnored_isNotPermissible() throws RemoteException {
        String packageName = "test.package";
        int uid = UserHandle.getUid(UserHandle.myUserId(), /* appId= */ 1);
        PackageInfo packageInfo = createPackageInfo(packageName, uid);
        addPackageWithPermission(packageInfo, AppOpsManager.MODE_IGNORED);
        addEntry(packageInfo);

        AppEntry entry = mAppEntries.get(0);
        assertThat(entry.extraInfo).isNull();

        mBridge.start();

        assertThat(entry.extraInfo).isNotNull();
        assertThat(((PermissionState) entry.extraInfo).isPermissible()).isFalse();
    }

    @Test
    public void loadsAppOpsExtraInfo_multipleApps() throws RemoteException {
        String packageName1 = "test.package1";
        int uid1 = UserHandle.getUid(UserHandle.myUserId(), /* appId= */ 1);
        PackageInfo packageInfo1 = createPackageInfo(packageName1, uid1);
        addPackageWithPermission(packageInfo1, AppOpsManager.MODE_ALLOWED);
        addEntry(packageInfo1);

        String packageName2 = "test.package2";
        int uid2 = UserHandle.getUid(UserHandle.myUserId(), /* appId= */ 2);
        PackageInfo packageInfo2 = createPackageInfo(packageName2, uid2);
        addPackageWithPermission(packageInfo2, AppOpsManager.MODE_ALLOWED);
        addEntry(packageInfo2);

        AppEntry entry1 = mAppEntries.get(0);
        AppEntry entry2 = mAppEntries.get(1);

        mBridge.start();

        assertThat(entry1.extraInfo).isNotNull();
        assertThat(entry2.extraInfo).isNotNull();
    }

    @Test
    public void loadsAppOpExtraInfo_multipleProfiles() throws RemoteException {
        String packageName1 = "test.package1";
        int uid1 = UserHandle.getUid(UserHandle.myUserId(), /* appId= */ 1);
        PackageInfo packageInfo1 = createPackageInfo(packageName1, uid1);
        addPackageWithPermission(packageInfo1, AppOpsManager.MODE_ALLOWED);
        addEntry(packageInfo1);

        // Add a package for another profile.
        int otherUserId = UserHandle.myUserId() + 1;
        String packageName2 = "test.package2";
        int uid2 = UserHandle.getUid(otherUserId, /* appId= */ 2);
        PackageInfo packageInfo2 = createPackageInfo(packageName2, uid2);
        when(mIPackageManager.getPackagesHoldingPermissions(
                AdditionalMatchers.aryEq(new String[]{PERMISSION}),
                eq(PackageManager.GET_PERMISSIONS),
                eq(otherUserId)))
                .thenReturn(mParceledPackagesOtherProfile);
        when(mParceledPackagesOtherProfile.getList()).thenReturn(
                Collections.singletonList(packageInfo2));
        when(mIPackageManager.isPackageAvailable(packageInfo2.packageName,
                otherUserId)).thenReturn(true);
        mAppOpsManager.setMode(APP_OP_CODE, packageInfo2.applicationInfo.uid,
                packageInfo2.packageName, AppOpsManager.MODE_ALLOWED);
        addEntry(packageInfo2);

        AppEntry entry1 = mAppEntries.get(0);
        AppEntry entry2 = mAppEntries.get(1);

        getShadowUserManager().addUserProfile(UserHandle.of(otherUserId));
        // Recreate the bridge so it has all user profiles.
        mBridge = new AppStateAppOpsBridge(mContext, mApplicationsState, APP_OP_CODE, PERMISSION,
                mock(AppStateBaseBridge.Callback.class), mIPackageManager);

        mBridge.start();

        assertThat(entry1.extraInfo).isNotNull();
        assertThat(entry2.extraInfo).isNotNull();
    }

    @Test
    public void appEntryNotIncluded_extraInfoCleared() {
        String packageName = "test.package";
        int uid = UserHandle.getUid(UserHandle.myUserId(), /* appId= */ 1);
        PackageInfo packageInfo = createPackageInfo(packageName, uid);
        addEntry(packageInfo);

        AppEntry entry = mAppEntries.get(0);
        entry.extraInfo = new Object();

        mBridge.start();

        assertThat(entry.extraInfo).isNull();
    }

    private PackageInfo createPackageInfo(String packageName, int uid) {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName = packageName;
        applicationInfo.uid = uid;

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        packageInfo.applicationInfo = applicationInfo;
        packageInfo.requestedPermissions = new String[]{PERMISSION};

        return packageInfo;
    }

    private void addPackageWithPermission(PackageInfo packageInfo, int mode)
            throws RemoteException {
        mPackages.add(packageInfo);
        when(mIPackageManager.isPackageAvailable(packageInfo.packageName,
                UserHandle.myUserId())).thenReturn(true);
        mAppOpsManager.setMode(APP_OP_CODE, packageInfo.applicationInfo.uid,
                packageInfo.packageName, mode);
    }

    private void addEntry(PackageInfo packageInfo) {
        AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = packageInfo.applicationInfo;
        mAppEntries.add(appEntry);
    }

    private ShadowUserManager getShadowUserManager() {
        return Shadows.shadowOf(UserManager.get(mContext));
    }
}
