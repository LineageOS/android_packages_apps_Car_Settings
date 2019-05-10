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

import static android.os.storage.StorageVolume.ScopedAccessProviderContract.AUTHORITY;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PACKAGES;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PACKAGES_COL_PACKAGE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Looper;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.car.settings.testutils.ShadowApplicationsState;
import com.android.car.settings.testutils.ShadowContentResolver;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.BaseCursor;
import org.robolectric.shadow.api.Shadow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Unit test for {@link DirectoryAccessPreferenceController}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowApplicationsState.class, ShadowContentResolver.class})
public class DirectoryAccessPreferenceControllerTest {

    @Mock
    private AppEntryListManager mAppEntryListManager;
    @Mock
    private ApplicationsState mApplicationsState;
    @Captor
    private ArgumentCaptor<AppEntryListManager.AppFilterProvider> mFilterCaptor;
    @Captor
    private ArgumentCaptor<AppEntryListManager.Callback> mCallbackCaptor;

    private Context mContext;
    private PreferenceGroup mPreferenceGroup;
    private PreferenceControllerTestHelper<DirectoryAccessPreferenceController> mControllerHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplicationsState.setInstance(mApplicationsState);
        when(mApplicationsState.getBackgroundLooper()).thenReturn(Looper.getMainLooper());

        mContext = RuntimeEnvironment.application;
        mPreferenceGroup = new LogicalPreferenceGroup(mContext);
        mControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                DirectoryAccessPreferenceController.class, mPreferenceGroup);
        mControllerHelper.getController().mAppEntryListManager = mAppEntryListManager;
        mControllerHelper.markState(Lifecycle.State.CREATED);
        verify(mAppEntryListManager).init(isNull(), mFilterCaptor.capture(),
                mCallbackCaptor.capture());
    }

    @After
    public void tearDown() {
        ShadowApplicationsState.reset();
        ShadowContentResolver.reset();
    }

    @Test
    public void onStart_startsListManager() {
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);

        verify(mAppEntryListManager).start();
    }

    @Test
    public void onStop_stopsListManager() {
        mControllerHelper.markState(Lifecycle.State.STARTED);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_STOP);

        verify(mAppEntryListManager).stop();
    }

    @Test
    public void onDestroy_destroysListManager() {
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_DESTROY);

        verify(mAppEntryListManager).destroy();
    }

    @Test
    public void onAppEntryListChanged_addsPreferencesForEntries() {
        mControllerHelper.markState(Lifecycle.State.STARTED);
        List<ApplicationsState.AppEntry> entries = Arrays.asList(
                createAppEntry("test.package", /* uid= */ 1),
                createAppEntry("another.test.package", /* uid= */ 2));

        mCallbackCaptor.getValue().onAppEntryListChanged(entries);

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    public void onPreferenceClicked_launchesDetailsFragmentForPackage() {
        mControllerHelper.markState(Lifecycle.State.STARTED);
        String packageName = "test.package";
        List<ApplicationsState.AppEntry> entries = Collections.singletonList(
                createAppEntry(packageName, /* uid= */ 1));
        mCallbackCaptor.getValue().onAppEntryListChanged(entries);
        Preference appPref = mPreferenceGroup.getPreference(0);

        appPref.performClick();

        ArgumentCaptor<Fragment> fragmentCaptor = ArgumentCaptor.forClass(Fragment.class);
        verify(mControllerHelper.getMockFragmentController()).launchFragment(
                fragmentCaptor.capture());
        assertThat(fragmentCaptor.getValue()).isInstanceOf(DirectoryAccessDetailsFragment.class);
        assertThat(fragmentCaptor.getValue().getArguments().getString(
                DirectoryAccessDetailsFragment.ARG_PACKAGE_NAME)).isEqualTo(packageName);
    }

    @Test
    public void appFilter_removesPackagesNotInScopedAccessProvider() {
        mControllerHelper.markState(Lifecycle.State.STARTED);
        String includedPackage = "test.package";
        String excludedPackage = "test.package2";

        BaseCursor cursor = mock(BaseCursor.class);
        when(cursor.getCount()).thenReturn(1);
        when(cursor.moveToNext()).thenReturn(true).thenReturn(false);
        when(cursor.getString(TABLE_PACKAGES_COL_PACKAGE)).thenReturn(includedPackage);

        Uri providerUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY)
                .appendPath(TABLE_PACKAGES)
                .appendPath("*")
                .build();
        getShadowContentResolver().setCursor(providerUri, cursor);

        ApplicationsState.AppFilter filter = mFilterCaptor.getValue().getAppFilter();
        filter.init(mContext);

        assertThat(filter.filterApp(createAppEntry(includedPackage, /* uid= */ 1))).isTrue();
        assertThat(filter.filterApp(createAppEntry(excludedPackage, /* uid= */ 2))).isFalse();
    }

    private ApplicationsState.AppEntry createAppEntry(String packageName, int uid) {
        ApplicationInfo info = new ApplicationInfo();
        info.packageName = packageName;
        info.uid = uid;

        ApplicationsState.AppEntry appEntry = mock(ApplicationsState.AppEntry.class);
        appEntry.info = info;
        appEntry.label = packageName;

        return appEntry;
    }

    private ShadowContentResolver getShadowContentResolver() {
        return Shadow.extract(mContext.getContentResolver());
    }
}
