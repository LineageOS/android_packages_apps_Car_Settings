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
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.COL_GRANTED;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS_COL_DIRECTORY;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS_COL_GRANTED;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS_COL_PACKAGE;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS_COL_VOLUME_UUID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertThrows;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.TwoStatePreference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.car.settings.testutils.ShadowStorageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.BaseCursor;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.Arrays;

/** Unit test for {@link DirectoryAccessDetailsPreferenceController}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowStorageManager.class})
public class DirectoryAccessDetailsPreferenceControllerTest {

    private static final String PACKAGE = "test.package";

    @Mock
    private BaseCursor mCursor;
    private Uri mProviderUri;

    private Context mContext;
    private PreferenceGroup mPreferenceGroup;
    private PreferenceControllerTestHelper<DirectoryAccessDetailsPreferenceController>
            mControllerHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPreferenceGroup = new LogicalPreferenceGroup(mContext);
        mControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                DirectoryAccessDetailsPreferenceController.class);
        mControllerHelper.getController().setPackage(PACKAGE);
        mControllerHelper.setPreference(mPreferenceGroup);

        mProviderUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY)
                .appendPath(TABLE_PERMISSIONS)
                .appendPath("*")
                .build();
        getShadowContentResolver().setCursor(mProviderUri, mCursor);
    }

    @Test
    public void checkInitialized_noPackageSet_throwsIllegalStateException() {
        mControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                DirectoryAccessDetailsPreferenceController.class);

        assertThrows(IllegalStateException.class,
                () -> mControllerHelper.setPreference(new LogicalPreferenceGroup(mContext)));
    }

    @Test
    public void onCreate_primaryStoragePermission_addsPreference() {
        when(mCursor.getCount()).thenReturn(1);
        when(mCursor.moveToNext()).thenReturn(true).thenReturn(false);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_PACKAGE)).thenReturn(PACKAGE);
        // Null uuid for primary storage.
        when(mCursor.getString(TABLE_PERMISSIONS_COL_VOLUME_UUID)).thenReturn(null);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_DIRECTORY)).thenReturn(
                Environment.DIRECTORY_PICTURES);

        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void onCreate_primaryStoragePermission_granted_setsChecked() {
        when(mCursor.getCount()).thenReturn(1);
        when(mCursor.moveToNext()).thenReturn(true).thenReturn(false);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_PACKAGE)).thenReturn(PACKAGE);
        // Null uuid for primary storage.
        when(mCursor.getString(TABLE_PERMISSIONS_COL_VOLUME_UUID)).thenReturn(null);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_DIRECTORY)).thenReturn(
                Environment.DIRECTORY_PICTURES);
        when(mCursor.getInt(TABLE_PERMISSIONS_COL_GRANTED)).thenReturn(1);

        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        TwoStatePreference pref = (TwoStatePreference) mPreferenceGroup.getPreference(0);
        assertThat(pref.isChecked()).isTrue();
    }

    @Test
    public void onCreate_primaryStoragePermission_setsPreferenceTitleToDirectory() {
        String dirName = Environment.DIRECTORY_PICTURES;
        when(mCursor.getCount()).thenReturn(1);
        when(mCursor.moveToNext()).thenReturn(true).thenReturn(false);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_PACKAGE)).thenReturn(PACKAGE);
        // Null uuid for primary storage.
        when(mCursor.getString(TABLE_PERMISSIONS_COL_VOLUME_UUID)).thenReturn(null);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_DIRECTORY)).thenReturn(dirName);

        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        Preference pref = mPreferenceGroup.getPreference(0);
        assertThat(pref.getTitle()).isEqualTo(dirName);
    }

    @Test
    public void onCreate_externalVolumePermission_wholeVolume_addsPreference() {
        String name = "external volume";
        String uuid = "external uuid";
        VolumeInfo primaryStorage = mock(VolumeInfo.class);
        VolumeInfo external = mock(VolumeInfo.class);
        when(external.getFsUuid()).thenReturn(uuid);
        getShadowStorageManager().setVolumes(Arrays.asList(primaryStorage, external));
        getShadowStorageManager().setBestVolumeDescription(external, name);

        when(mCursor.getCount()).thenReturn(1);
        when(mCursor.moveToNext()).thenReturn(true).thenReturn(false);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_PACKAGE)).thenReturn(PACKAGE);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_VOLUME_UUID)).thenReturn(uuid);
        // Null directory indicates access for whole volume.
        when(mCursor.getString(TABLE_PERMISSIONS_COL_DIRECTORY)).thenReturn(null);

        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(1);
        assertThat(mPreferenceGroup.getPreference(0).getTitle()).isEqualTo(name);
    }

    @Test
    public void onCreate_externalVolumePermission_directory_addsWholeAndDirectoryPreferences() {
        String name = "external volume";
        String uuid = "external uuid";
        String dirName = Environment.DIRECTORY_PICTURES;
        VolumeInfo primaryStorage = mock(VolumeInfo.class);
        VolumeInfo external = mock(VolumeInfo.class);
        when(external.getFsUuid()).thenReturn(uuid);
        getShadowStorageManager().setVolumes(Arrays.asList(primaryStorage, external));
        getShadowStorageManager().setBestVolumeDescription(external, name);

        when(mCursor.getCount()).thenReturn(2);
        when(mCursor.moveToNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_PACKAGE)).thenReturn(PACKAGE);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_VOLUME_UUID)).thenReturn(uuid);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_DIRECTORY)).thenReturn(null).thenReturn(
                dirName);
        // Root not granted.
        when(mCursor.getInt(TABLE_PERMISSIONS_COL_GRANTED)).thenReturn(0);

        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(2);
        assertThat(mPreferenceGroup.getPreference(0).getTitle()).isEqualTo(name);
        // External volume directory preference should have name of volume and directory.
        assertThat(mPreferenceGroup.getPreference(1).getTitle().toString()).contains(name);
        assertThat(mPreferenceGroup.getPreference(1).getTitle().toString()).contains(dirName);
    }

    @Test
    public void onCreate_externalVolumePermission_rootGranted_hidesDirectoryPreference() {
        String name = "external volume";
        String uuid = "external uuid";
        String dirName = Environment.DIRECTORY_PICTURES;
        VolumeInfo primaryStorage = mock(VolumeInfo.class);
        VolumeInfo external = mock(VolumeInfo.class);
        when(external.getFsUuid()).thenReturn(uuid);
        getShadowStorageManager().setVolumes(Arrays.asList(primaryStorage, external));
        getShadowStorageManager().setBestVolumeDescription(external, name);

        when(mCursor.getCount()).thenReturn(2);
        when(mCursor.moveToNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_PACKAGE)).thenReturn(PACKAGE);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_VOLUME_UUID)).thenReturn(uuid);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_DIRECTORY)).thenReturn(null).thenReturn(
                dirName);
        // Root granted.
        when(mCursor.getInt(TABLE_PERMISSIONS_COL_GRANTED)).thenReturn(1);

        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(1);
        assertThat(mPreferenceGroup.getPreference(0).getTitle()).isEqualTo(name);
    }

    @Test
    public void onPreferenceClicked_primaryStorage_updatesContentProvider() {
        String dirName = Environment.DIRECTORY_PICTURES;
        when(mCursor.getCount()).thenReturn(1);
        when(mCursor.moveToNext()).thenReturn(true).thenReturn(false);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_PACKAGE)).thenReturn(PACKAGE);
        // Null uuid for primary storage.
        when(mCursor.getString(TABLE_PERMISSIONS_COL_VOLUME_UUID)).thenReturn(null);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_DIRECTORY)).thenReturn(dirName);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        Preference pref = mPreferenceGroup.getPreference(0);

        pref.performClick();

        assertThat(getShadowContentResolver().getUpdateStatements()).hasSize(1);
        ShadowContentResolver.UpdateStatement updateStatement =
                getShadowContentResolver().getUpdateStatements().get(0);
        assertThat(updateStatement.getUri()).isEqualTo(mProviderUri);
        assertThat(updateStatement.getContentValues().get(COL_GRANTED)).isEqualTo(true);
        assertThat(updateStatement.getSelectionArgs()).isEqualTo(
                new String[]{PACKAGE, null, dirName});
    }

    @Test
    public void onPreferenceClicked_externalStorage_updatesContentProvider() {
        String uuid = "external uuid";
        VolumeInfo primaryStorage = mock(VolumeInfo.class);
        VolumeInfo external = mock(VolumeInfo.class);
        when(external.getFsUuid()).thenReturn(uuid);
        getShadowStorageManager().setVolumes(Arrays.asList(primaryStorage, external));
        getShadowStorageManager().setBestVolumeDescription(external, "external volume");

        when(mCursor.getCount()).thenReturn(1);
        when(mCursor.moveToNext()).thenReturn(true).thenReturn(false);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_PACKAGE)).thenReturn(PACKAGE);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_VOLUME_UUID)).thenReturn(uuid);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_DIRECTORY)).thenReturn(null);
        // Root granted
        when(mCursor.getInt(TABLE_PERMISSIONS_COL_GRANTED)).thenReturn(1);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        Preference pref = mPreferenceGroup.getPreference(0);

        pref.performClick();

        assertThat(getShadowContentResolver().getUpdateStatements()).hasSize(1);
        ShadowContentResolver.UpdateStatement updateStatement =
                getShadowContentResolver().getUpdateStatements().get(0);
        assertThat(updateStatement.getUri()).isEqualTo(mProviderUri);
        assertThat(updateStatement.getContentValues().get(COL_GRANTED)).isEqualTo(false);
        assertThat(updateStatement.getSelectionArgs()).isEqualTo(
                new String[]{PACKAGE, uuid, null});
    }

    @Test
    public void onPreferenceClicked_externalStorage_directory_updatesContentProvider() {
        String uuid = "external uuid";
        String dirName = Environment.DIRECTORY_PICTURES;
        VolumeInfo primaryStorage = mock(VolumeInfo.class);
        VolumeInfo external = mock(VolumeInfo.class);
        when(external.getFsUuid()).thenReturn(uuid);
        getShadowStorageManager().setVolumes(Arrays.asList(primaryStorage, external));
        getShadowStorageManager().setBestVolumeDescription(external, "external volume");

        when(mCursor.getCount()).thenReturn(2);
        when(mCursor.moveToNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_PACKAGE)).thenReturn(PACKAGE);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_VOLUME_UUID)).thenReturn(uuid);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_DIRECTORY)).thenReturn(null).thenReturn(
                dirName);
        // Root not granted.
        when(mCursor.getInt(TABLE_PERMISSIONS_COL_GRANTED)).thenReturn(0);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        Preference pref = mPreferenceGroup.getPreference(1);

        pref.performClick();

        assertThat(getShadowContentResolver().getUpdateStatements()).hasSize(1);
        ShadowContentResolver.UpdateStatement updateStatement =
                getShadowContentResolver().getUpdateStatements().get(0);
        assertThat(updateStatement.getUri()).isEqualTo(mProviderUri);
        assertThat(updateStatement.getContentValues().get(COL_GRANTED)).isEqualTo(true);
        assertThat(updateStatement.getSelectionArgs()).isEqualTo(
                new String[]{PACKAGE, uuid, dirName});
    }

    @Test
    public void onPreferenceClicked_refreshesUi() {
        String uuid = "external uuid";
        String dirName = Environment.DIRECTORY_PICTURES;
        VolumeInfo primaryStorage = mock(VolumeInfo.class);
        VolumeInfo external = mock(VolumeInfo.class);
        when(external.getFsUuid()).thenReturn(uuid);
        getShadowStorageManager().setVolumes(Arrays.asList(primaryStorage, external));
        getShadowStorageManager().setBestVolumeDescription(external, "external volume");

        when(mCursor.getCount()).thenReturn(2);
        // Setup for two iterations over cursor with two rows.
        when(mCursor.moveToNext()).thenReturn(true).thenReturn(true).thenReturn(false).thenReturn(
                true).thenReturn(true).thenReturn(false);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_PACKAGE)).thenReturn(PACKAGE);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_VOLUME_UUID)).thenReturn(uuid);
        when(mCursor.getString(TABLE_PERMISSIONS_COL_DIRECTORY)).thenReturn(null).thenReturn(
                dirName).thenReturn(null).thenReturn(dirName);
        // Root not granted, dir not granted -> root granted, dir not explicitly granted.
        when(mCursor.getInt(TABLE_PERMISSIONS_COL_GRANTED)).thenReturn(0).thenReturn(0).thenReturn(
                1).thenReturn(0);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(2);
        Preference pref = mPreferenceGroup.getPreference(0);

        pref.performClick();

        // Granting access to root should hide the directory preference on refresh.
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(1);
    }

    private ShadowContentResolver getShadowContentResolver() {
        return Shadows.shadowOf(mContext.getContentResolver());
    }

    private ShadowStorageManager getShadowStorageManager() {
        return Shadow.extract(mContext.getSystemService(StorageManager.class));
    }
}
