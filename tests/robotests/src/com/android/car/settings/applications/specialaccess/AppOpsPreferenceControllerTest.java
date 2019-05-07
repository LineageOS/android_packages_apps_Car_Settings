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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertThrows;

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

import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceGroup;
import androidx.preference.TwoStatePreference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.car.settings.testutils.ShadowActivityThread;
import com.android.car.settings.testutils.ShadowAppOpsManager;
import com.android.car.settings.testutils.ShadowApplicationsState;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.Collections;

/** Unit test for {@link AppOpsPreferenceController}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowAppOpsManager.class, ShadowApplicationsState.class,
        ShadowActivityThread.class})
public class AppOpsPreferenceControllerTest {

    private static final int APP_OP_CODE = AppOpsManager.OP_WRITE_SETTINGS;
    private static final String PERMISSION = Manifest.permission.WRITE_SETTINGS;
    private static final int NEGATIVE_MODE = AppOpsManager.MODE_ERRORED;

    @Mock
    private IPackageManager mIPackageManager;
    @Mock
    private ParceledListSlice<PackageInfo> mParceledPackages;
    @Mock
    private ApplicationsState mApplicationsState;
    @Mock
    private ApplicationsState.Session mSession;
    @Mock
    private ApplicationsState.Session mBridgeSession;
    @Captor
    private ArgumentCaptor<ApplicationsState.Callbacks> mCallbackCaptor;

    private Context mContext;
    private PreferenceGroup mPreferenceGroup;
    private PreferenceControllerTestHelper<AppOpsPreferenceController> mControllerHelper;
    private AppOpsPreferenceController mController;

    @Before
    public void setUp() throws RemoteException {
        MockitoAnnotations.initMocks(this);
        ShadowActivityThread.setPackageManager(mIPackageManager);
        when(mIPackageManager.getPackagesHoldingPermissions(
                AdditionalMatchers.aryEq(new String[]{PERMISSION}),
                eq(PackageManager.GET_PERMISSIONS),
                eq(UserHandle.myUserId())))
                .thenReturn(mParceledPackages);
        when(mParceledPackages.getList()).thenReturn(Collections.emptyList());
        ShadowApplicationsState.setInstance(mApplicationsState);
        when(mApplicationsState.newSession(mCallbackCaptor.capture()))
                .thenReturn(mSession)
                .thenReturn(mBridgeSession);
        when(mApplicationsState.getBackgroundLooper()).thenReturn(Looper.getMainLooper());

        mContext = RuntimeEnvironment.application;
        mPreferenceGroup = new LogicalPreferenceGroup(mContext);
        mControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                AppOpsPreferenceController.class);
        mController = mControllerHelper.getController();
        mController.init(APP_OP_CODE, PERMISSION, NEGATIVE_MODE);
        mControllerHelper.setPreference(mPreferenceGroup);
        mControllerHelper.markState(Lifecycle.State.CREATED);
    }

    @After
    public void tearDown() {
        ShadowApplicationsState.reset();
        ShadowActivityThread.reset();
    }

    @Test
    public void checkInitialized_noOpCode_throwsIllegalStateException() {
        mControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                AppOpsPreferenceController.class);
        mController = mControllerHelper.getController();

        mController.init(AppOpsManager.OP_NONE, PERMISSION, NEGATIVE_MODE);

        assertThrows(IllegalStateException.class,
                () -> mControllerHelper.setPreference(mPreferenceGroup));
    }

    @Test
    public void checkInitialized_noPermission_throwsIllegalStateException() {
        mControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                AppOpsPreferenceController.class);
        mController = mControllerHelper.getController();

        mController.init(APP_OP_CODE, /* permission= */ null, NEGATIVE_MODE);

        assertThrows(IllegalStateException.class,
                () -> mControllerHelper.setPreference(mPreferenceGroup));
    }

    @Test
    public void checkInitialized_noNegativeOpMode_throwsIllegalStateException() {
        mControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                AppOpsPreferenceController.class);
        mController = mControllerHelper.getController();

        mController.init(APP_OP_CODE, PERMISSION, AppOpsManager.MODE_DEFAULT);

        assertThrows(IllegalStateException.class,
                () -> mControllerHelper.setPreference(mPreferenceGroup));
    }

    @Test
    public void onStart_resumesSessions() {
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);

        verify(mSession).onResume();
        verify(mBridgeSession).onResume();
    }

    @Test
    public void onStop_pausesSessions() {
        mControllerHelper.markState(Lifecycle.State.STARTED);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_STOP);

        verify(mSession).onPause();
        verify(mBridgeSession).onPause();
    }

    @Test
    public void onDestroy_destroysSessions() {
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_DESTROY);

        verify(mSession).onDestroy();
        verify(mBridgeSession).onDestroy();
    }

    @Test
    public void onLoadEntriesCompleted_extraInfoUpdated_rebuildsEntries() {
        mControllerHelper.markState(Lifecycle.State.STARTED);
        ApplicationsState.Callbacks callbacks = mCallbackCaptor.getAllValues().get(0);

        // Extra info updated callback happens synchronously onStart since we are using the main
        // looper for testing.
        callbacks.onLoadEntriesCompleted();

        verify(mSession).rebuild(any(), eq(ApplicationsState.ALPHA_COMPARATOR), /* foreground= */
                eq(false));
    }

    @Test
    public void onRebuildComplete_addsPreferencesForEntries() {
        mControllerHelper.markState(Lifecycle.State.STARTED);
        ApplicationsState.Callbacks callbacks = mCallbackCaptor.getAllValues().get(0);
        ArrayList<AppEntry> entries = new ArrayList<>();
        entries.add(createAppEntry("test.package", /* uid= */ 1, /* isOpPermissible= */ true));
        entries.add(
                createAppEntry("another.test.package", /* uid= */ 2, /* isOpPermissible= */ false));
        callbacks.onLoadEntriesCompleted();

        callbacks.onRebuildComplete(entries);

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(2);
        assertThat(((TwoStatePreference) mPreferenceGroup.getPreference(0)).isChecked()).isTrue();
        assertThat(((TwoStatePreference) mPreferenceGroup.getPreference(1)).isChecked()).isFalse();
    }

    @Test
    public void onPreferenceChange_checkedState_setsAppOpModeAllowed() {
        mControllerHelper.markState(Lifecycle.State.STARTED);
        ApplicationsState.Callbacks callbacks = mCallbackCaptor.getAllValues().get(0);
        ArrayList<AppEntry> entries = new ArrayList<>();
        String packageName = "test.package";
        int uid = 1;
        entries.add(createAppEntry("test.package", uid, /* isOpPermissible= */ false));
        callbacks.onLoadEntriesCompleted();
        callbacks.onRebuildComplete(entries);
        TwoStatePreference appPref = (TwoStatePreference) mPreferenceGroup.getPreference(0);

        appPref.performClick();

        assertThat(getShadowAppOpsManager().getMode(APP_OP_CODE, uid, packageName)).isEqualTo(
                AppOpsManager.MODE_ALLOWED);
    }

    @Test
    public void onPreferenceChange_uncheckedState_setsNegativeAppOpMode() {
        mControllerHelper.markState(Lifecycle.State.STARTED);
        ApplicationsState.Callbacks callbacks = mCallbackCaptor.getAllValues().get(0);
        ArrayList<AppEntry> entries = new ArrayList<>();
        String packageName = "test.package";
        int uid = 1;
        entries.add(createAppEntry("test.package", uid, /* isOpPermissible= */ true));
        callbacks.onLoadEntriesCompleted();
        callbacks.onRebuildComplete(entries);
        TwoStatePreference appPref = (TwoStatePreference) mPreferenceGroup.getPreference(0);

        appPref.performClick();

        assertThat(getShadowAppOpsManager().getMode(APP_OP_CODE, uid, packageName)).isEqualTo(
                NEGATIVE_MODE);
    }

    @Test
    public void onPreferenceChange_rebuildsEntries() {
        mControllerHelper.markState(Lifecycle.State.STARTED);
        ApplicationsState.Callbacks callbacks = mCallbackCaptor.getAllValues().get(0);
        ArrayList<AppEntry> entries = new ArrayList<>();
        String packageName = "test.package";
        int uid = 1;
        entries.add(createAppEntry("test.package", uid, /* isOpPermissible= */ false));
        callbacks.onLoadEntriesCompleted();
        callbacks.onRebuildComplete(entries);
        TwoStatePreference appPref = (TwoStatePreference) mPreferenceGroup.getPreference(0);

        appPref.performClick();

        // 2 times: onLoadEntriesCompleted, onPreferenceChange
        verify(mSession, times(2)).rebuild(any(),
                eq(ApplicationsState.ALPHA_COMPARATOR), /* foreground= */
                eq(false));
    }

    @Test
    public void showSystem_rebuildsEntries() {
        mControllerHelper.markState(Lifecycle.State.STARTED);
        ApplicationsState.Callbacks callbacks = mCallbackCaptor.getAllValues().get(0);
        callbacks.onLoadEntriesCompleted();

        mController.setShowSystem(true);

        // 2 times: onLoadEntriesCompleted, setShowSystem
        verify(mSession, times(2)).rebuild(any(),
                eq(ApplicationsState.ALPHA_COMPARATOR), /* foreground= */
                eq(false));
    }

    @Test
    public void rebuildFilter_showingSystemApps_keepsSystemEntries() {
        mControllerHelper.markState(Lifecycle.State.STARTED);
        ApplicationsState.Callbacks callbacks = mCallbackCaptor.getAllValues().get(0);
        callbacks.onLoadEntriesCompleted();
        mController.setShowSystem(true);
        ArgumentCaptor<ApplicationsState.AppFilter> filterCaptor = ArgumentCaptor.forClass(
                ApplicationsState.AppFilter.class);
        // 2 times: onLoadEntriesCompleted, setShowSystem
        verify(mSession, times(2)).rebuild(filterCaptor.capture(),
                eq(ApplicationsState.ALPHA_COMPARATOR), /* foreground= */
                eq(false));

        // Get the filter from setShowSystem.
        ApplicationsState.AppFilter filter = filterCaptor.getAllValues().get(1);

        AppEntry systemApp = createAppEntry("test.package", /* uid= */ 1, /* isOpPermissible= */
                false);
        systemApp.info.flags |= ApplicationInfo.FLAG_SYSTEM;

        assertThat(filter.filterApp(systemApp)).isTrue();
    }

    @Test
    public void rebuildFilter_notShowingSystemApps_removesSystemEntries() {
        mControllerHelper.markState(Lifecycle.State.STARTED);
        ApplicationsState.Callbacks callbacks = mCallbackCaptor.getAllValues().get(0);
        callbacks.onLoadEntriesCompleted();
        ArgumentCaptor<ApplicationsState.AppFilter> filterCaptor = ArgumentCaptor.forClass(
                ApplicationsState.AppFilter.class);
        verify(mSession).rebuild(filterCaptor.capture(),
                eq(ApplicationsState.ALPHA_COMPARATOR), /* foreground= */
                eq(false));

        // Not showing system by default
        ApplicationsState.AppFilter filter = filterCaptor.getValue();

        AppEntry systemApp = createAppEntry("test.package", /* uid= */ 1, /* isOpPermissible= */
                false);
        systemApp.info.flags |= ApplicationInfo.FLAG_SYSTEM;

        assertThat(filter.filterApp(systemApp)).isFalse();
    }

    @Test
    public void rebuildFilter_removesNullExtraInfoEntries() {
        mControllerHelper.markState(Lifecycle.State.STARTED);
        ApplicationsState.Callbacks callbacks = mCallbackCaptor.getAllValues().get(0);
        callbacks.onLoadEntriesCompleted();
        ArgumentCaptor<ApplicationsState.AppFilter> filterCaptor = ArgumentCaptor.forClass(
                ApplicationsState.AppFilter.class);
        verify(mSession).rebuild(filterCaptor.capture(),
                eq(ApplicationsState.ALPHA_COMPARATOR), /* foreground= */
                eq(false));

        ApplicationsState.AppFilter filter = filterCaptor.getValue();

        AppEntry appEntry = createAppEntry("test.package", /* uid= */ 1, /* isOpPermissible= */
                false);
        appEntry.extraInfo = null;

        assertThat(filter.filterApp(appEntry)).isFalse();
    }

    private AppEntry createAppEntry(String packageName, int uid, boolean isOpPermissible) {
        ApplicationInfo info = new ApplicationInfo();
        info.packageName = packageName;
        info.uid = uid;

        AppStateAppOpsBridge.PermissionState extraInfo = mock(
                AppStateAppOpsBridge.PermissionState.class);
        when(extraInfo.isPermissible()).thenReturn(isOpPermissible);

        AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = info;
        appEntry.label = packageName;
        appEntry.extraInfo = extraInfo;

        return appEntry;
    }

    private ShadowAppOpsManager getShadowAppOpsManager() {
        return Shadow.extract(mContext.getSystemService(Context.APP_OPS_SERVICE));
    }
}
