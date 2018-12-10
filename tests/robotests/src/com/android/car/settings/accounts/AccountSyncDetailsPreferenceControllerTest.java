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

package com.android.car.settings.accounts;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.robolectric.RuntimeEnvironment.application;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.SyncAdapterType;
import android.content.SyncInfo;
import android.content.SyncStatusInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.UserHandle;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.testutils.ShadowApplicationPackageManager;
import com.android.car.settings.testutils.ShadowContentResolver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Unit test for {@link AccountSyncDetailsPreferenceController}.
 */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowContentResolver.class, ShadowApplicationPackageManager.class})
public class AccountSyncDetailsPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "preference_key";

    private static final int SYNCABLE = 1;
    private static final int NOT_SYNCABLE = 0;

    private static final int USER_ID = 3;
    private static final int NOT_USER_ID = 5;

    private static final String AUTHORITY = "authority";
    private static final String ACCOUNT_TYPE = "com.acct1";
    private static final String DIFFERENT_ACCOUNT_TYPE = "com.acct2";

    private final Account mAccount = new Account("acct1", ACCOUNT_TYPE);
    private final UserHandle mUserHandle = new UserHandle(USER_ID);

    private AccountSyncDetailsPreferenceController mController;
    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mPreferenceScreen = new PreferenceManager(application).createPreferenceScreen(application);
        mPreferenceScreen.setKey(PREFERENCE_KEY);

        mController = new AccountSyncDetailsPreferenceController(application, PREFERENCE_KEY,
                mock(FragmentController.class));
        mController.setAccount(mAccount);
        mController.setUserHandle(mUserHandle);
    }

    @After
    public void tearDown() {
        ShadowContentResolver.reset();
    }

    @Test
    public void displayPreference_syncAdapterDoesNotHaveSameAccountType_shouldNotBeShown() {
        // Adds a sync adapter type that is visible but does not have the right account type.
        SyncAdapterType syncAdapterType = new SyncAdapterType(AUTHORITY,
                DIFFERENT_ACCOUNT_TYPE, /* userVisible */ true, /* supportsUploading */ true);
        SyncAdapterType[] syncAdapters = {syncAdapterType};
        ShadowContentResolver.setSyncAdapterTypes(syncAdapters);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void displayPreference_syncAdapterIsNotVisible_shouldNotBeShown() {
        // Adds a sync adapter type that has the right account type but is not visible.
        SyncAdapterType syncAdapterType = new SyncAdapterType(AUTHORITY,
                ACCOUNT_TYPE, /* userVisible */ false, /* supportsUploading */ true);
        SyncAdapterType[] syncAdapters = {syncAdapterType};
        ShadowContentResolver.setSyncAdapterTypes(syncAdapters);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void displayPreference_syncAdapterIsNotSyncable_shouldNotBeShown() {
        // Adds a sync adapter type that has the right account type and is visible.
        SyncAdapterType syncAdapterType = new SyncAdapterType(AUTHORITY,
                ACCOUNT_TYPE, /* userVisible */ true, /* supportsUploading */ true);
        SyncAdapterType[] syncAdapters = {syncAdapterType};
        ShadowContentResolver.setSyncAdapterTypes(syncAdapters);
        // Sets that the sync adapter to not syncable.
        ShadowContentResolver.setIsSyncable(mAccount, AUTHORITY, /* syncable= */ NOT_SYNCABLE);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void displayPreference_syncAdapterDoesNotHaveProviderInfo_shouldNotBeShown() {
        // Adds a sync adapter type that has the right account type and is visible.
        SyncAdapterType syncAdapterType = new SyncAdapterType(AUTHORITY,
                ACCOUNT_TYPE, /* userVisible */ true, /* supportsUploading */ true);
        SyncAdapterType[] syncAdapters = {syncAdapterType};
        ShadowContentResolver.setSyncAdapterTypes(syncAdapters);
        // Sets that the sync adapter to syncable.
        ShadowContentResolver.setIsSyncable(mAccount, AUTHORITY, /* syncable= */ SYNCABLE);

        // However, no provider info is set for the sync adapter, so it shouldn't be visible.

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void displayPreference_providerInfoDoesNotHaveLabel_shouldNotBeShown() {
        // Adds a sync adapter type that has the right account type and is visible.
        SyncAdapterType syncAdapterType = new SyncAdapterType(AUTHORITY,
                ACCOUNT_TYPE, /* userVisible */ true, /* supportsUploading */ true);
        SyncAdapterType[] syncAdapters = {syncAdapterType};
        ShadowContentResolver.setSyncAdapterTypes(syncAdapters);
        // Sets that the sync adapter to syncable.
        ShadowContentResolver.setIsSyncable(mAccount, AUTHORITY, /* syncable= */ SYNCABLE);
        // Sets provider info for the sync adapter but it does not have a label.
        ProviderInfo info = mock(ProviderInfo.class);
        info.authority = AUTHORITY;
        doReturn("").when(info).loadLabel(any(PackageManager.class));

        ProviderInfo[] providers = {info};
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = AUTHORITY;
        packageInfo.providers = providers;
        getShadowApplicationManager().addPackage(packageInfo);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void displayPreference_providerLabelShouldBeSet() {
        // Adds a sync adapter type that has the right account type and is visible.
        SyncAdapterType syncAdapterType = new SyncAdapterType(AUTHORITY,
                ACCOUNT_TYPE, /* userVisible */ true, /* supportsUploading */ true);
        SyncAdapterType[] syncAdapters = {syncAdapterType};
        ShadowContentResolver.setSyncAdapterTypes(syncAdapters);
        // Sets that the sync adapter to syncable.
        ShadowContentResolver.setIsSyncable(mAccount, AUTHORITY, /* syncable= */ SYNCABLE);
        // Sets provider info for the sync adapter with a label.
        ProviderInfo info = mock(ProviderInfo.class);
        info.authority = AUTHORITY;
        doReturn("label").when(info).loadLabel(any(PackageManager.class));

        ProviderInfo[] providers = {info};
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = AUTHORITY;
        packageInfo.providers = providers;
        getShadowApplicationManager().addPackage(packageInfo);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(1);
        Preference pref = mPreferenceScreen.getPreference(0);
        assertThat(pref.getTitle()).isEqualTo("label");
    }

    @Test
    public void displayPreference_masterSyncOff_syncDisabled_shouldNotBeChecked() {
        setUpVisibleSyncAdapter();
        // Turns off master sync and automatic sync for the adapter.
        ContentResolver.setMasterSyncAutomaticallyAsUser(/* sync= */ true, USER_ID);
        ContentResolver.setSyncAutomaticallyAsUser(mAccount, AUTHORITY, /* sync= */ false,
                USER_ID);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(1);
        SwitchPreference pref = (SwitchPreference) mPreferenceScreen.getPreference(0);
        assertThat(pref.isChecked()).isFalse();
    }

    @Test
    public void displayPreference_masterSyncOn_syncDisabled_shouldBeChecked() {
        setUpVisibleSyncAdapter();
        // Turns on master sync and turns off automatic sync for the adapter.
        ContentResolver.setMasterSyncAutomaticallyAsUser(/* sync= */ false, USER_ID);
        ContentResolver.setSyncAutomaticallyAsUser(mAccount, AUTHORITY, /* sync= */ false,
                USER_ID);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(1);
        SwitchPreference pref = (SwitchPreference) mPreferenceScreen.getPreference(0);
        assertThat(pref.isChecked()).isTrue();
    }

    @Test
    public void displayPreference_masterSyncOff_syncEnabled_shouldBeChecked() {
        setUpVisibleSyncAdapter();
        // Turns off master sync and turns on automatic sync for the adapter.
        ContentResolver.setMasterSyncAutomaticallyAsUser(/* sync= */ true, USER_ID);
        ContentResolver.setSyncAutomaticallyAsUser(mAccount, AUTHORITY, /* sync= */ true,
                USER_ID);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(1);
        SwitchPreference pref = (SwitchPreference) mPreferenceScreen.getPreference(0);
        assertThat(pref.isChecked()).isTrue();
    }

    @Test
    public void displayPreference_syncDisabled_summaryShouldBeSet() {
        setUpVisibleSyncAdapter();
        // Turns off automatic sync for the the sync adapter.
        ContentResolver.setSyncAutomaticallyAsUser(mAccount, AUTHORITY, /* sync= */ false,
                mUserHandle.getIdentifier());

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(1);
        Preference pref = mPreferenceScreen.getPreference(0);
        assertThat(pref.getSummary()).isEqualTo(application.getString(R.string.sync_disabled));
    }

    @Test
    public void displayPreference_syncEnabled_activelySyncing_summaryShouldBeSet() {
        setUpVisibleSyncAdapter();
        // Turns on automatic sync for the the sync adapter.
        ContentResolver.setSyncAutomaticallyAsUser(mAccount, AUTHORITY, /* sync= */ true,
                mUserHandle.getIdentifier());
        // Adds the sync adapter to the list of currently syncing adapters.
        SyncInfo syncInfo = new SyncInfo(/* authorityId= */ 0, mAccount, AUTHORITY, /* startTime= */
                0);
        List<SyncInfo> syncs = new ArrayList<>();
        syncs.add(syncInfo);
        ShadowContentResolver.setCurrentSyncs(syncs);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(1);
        Preference pref = mPreferenceScreen.getPreference(0);
        assertThat(pref.getSummary()).isEqualTo(application.getString(R.string.sync_in_progress));
    }

    @Test
    public void displayPreference_syncEnabled_syncHasHappened_summaryShouldBeSet() {
        setUpVisibleSyncAdapter();
        // Turns on automatic sync for the the sync adapter.
        ContentResolver.setSyncAutomaticallyAsUser(mAccount, AUTHORITY, /* sync= */ true,
                mUserHandle.getIdentifier());
        // Sets the sync adapter's last successful sync time.
        SyncStatusInfo status = new SyncStatusInfo(0);
        status.setLastSuccess(0, 83091);
        ShadowContentResolver.setSyncStatus(mAccount, AUTHORITY, status);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(1);
        Preference pref = mPreferenceScreen.getPreference(0);

        String expectedTimeString = mController.formatSyncDate(new Date(83091));
        assertThat(pref.getSummary()).isEqualTo(
                application.getString(R.string.last_synced, expectedTimeString));
    }

    @Test
    public void displayPreference_activelySyncing_notInitialSync_shouldHaveActiveSyncIcon() {
        setUpVisibleSyncAdapter();
        // Adds the sync adapter to the list of currently syncing adapters.
        SyncInfo syncInfo = new SyncInfo(/* authorityId= */ 0, mAccount, AUTHORITY, /* startTime= */
                0);
        List<SyncInfo> syncs = new ArrayList<>();
        syncs.add(syncInfo);
        ShadowContentResolver.setCurrentSyncs(syncs);
        // Sets the sync adapter's initializing state to false (i.e. it's not performing an
        // initial sync).
        SyncStatusInfo status = new SyncStatusInfo(0);
        status.initialize = false;
        ShadowContentResolver.setSyncStatus(mAccount, AUTHORITY, status);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(1);
        Preference pref = mPreferenceScreen.getPreference(0);

        assertThat(Shadows.shadowOf(pref.getIcon()).getCreatedFromResId()).isEqualTo(
                R.drawable.ic_list_sync_anim);
    }

    @Test
    public void displayPreference_syncPending_notInitialSync_shouldHaveActiveSyncIcon() {
        setUpVisibleSyncAdapter();
        // Sets the sync adapter's initializing state to false (i.e. it's not performing an
        // initial sync).
        // Also sets the the sync status to pending
        SyncStatusInfo status = new SyncStatusInfo(0);
        status.initialize = false;
        status.pending = true;
        ShadowContentResolver.setSyncStatus(mAccount, AUTHORITY, status);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(1);
        Preference pref = mPreferenceScreen.getPreference(0);

        assertThat(Shadows.shadowOf(pref.getIcon()).getCreatedFromResId()).isEqualTo(
                R.drawable.ic_list_sync_anim);
    }

    @Test
    public void displayPreference_syncFailed_shouldHaveProblemSyncIcon() {
        setUpVisibleSyncAdapter();
        // Turns on automatic sync for the the sync adapter.
        ContentResolver.setSyncAutomaticallyAsUser(mAccount, AUTHORITY, /* sync= */ true,
                mUserHandle.getIdentifier());
        // Sets the sync adapter's last failure time and message so it appears to have failed
        // previously.
        SyncStatusInfo status = new SyncStatusInfo(0);
        status.lastFailureTime = 10;
        status.lastFailureMesg = "too-many-deletions";
        ShadowContentResolver.setSyncStatus(mAccount, AUTHORITY, status);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(1);
        Preference pref = mPreferenceScreen.getPreference(0);

        assertThat(Shadows.shadowOf(pref.getIcon()).getCreatedFromResId()).isEqualTo(
                R.drawable.ic_sync_problem);
    }

    @Test
    public void displayPreference_noSyncStatus_shouldHaveNoIcon() {
        setUpVisibleSyncAdapter();

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(1);
        Preference pref = mPreferenceScreen.getPreference(0);

        assertThat(pref.getIcon()).isNull();
        assertThat(pref.isIconSpaceReserved()).isTrue();
    }

    @Test
    public void onAccountsUpdate_correctUserId_shouldForceUpdatePreferences() {
        setUpVisibleSyncAdapter();

        mController.displayPreference(mPreferenceScreen);
        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(1);

        ShadowContentResolver.reset();
        mController.onAccountsUpdate(mUserHandle);

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void onAccountsUpdate_incorrectUserId_shouldNotForceUpdatePreferences() {
        setUpVisibleSyncAdapter();

        mController.displayPreference(mPreferenceScreen);
        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(1);

        ShadowContentResolver.reset();
        mController.onAccountsUpdate(new UserHandle(NOT_USER_ID));

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(1);
    }

    private void setUpVisibleSyncAdapter() {
        // Adds a sync adapter type that has the right account type and is visible.
        SyncAdapterType syncAdapterType = new SyncAdapterType(AUTHORITY,
                ACCOUNT_TYPE, /* userVisible */ true, /* supportsUploading */ true);
        SyncAdapterType[] syncAdapters = {syncAdapterType};
        ShadowContentResolver.setSyncAdapterTypes(syncAdapters);
        // Sets that the sync adapter to syncable.
        ShadowContentResolver.setIsSyncable(mAccount, AUTHORITY, /* syncable= */ SYNCABLE);
        // Sets provider info for the sync adapter with a label.
        ProviderInfo info = mock(ProviderInfo.class);
        info.authority = AUTHORITY;
        doReturn("label").when(info).loadLabel(any(PackageManager.class));

        ProviderInfo[] providers = {info};
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = AUTHORITY;
        packageInfo.providers = providers;
        getShadowApplicationManager().addPackage(packageInfo);
    }

    private ShadowApplicationPackageManager getShadowApplicationManager() {
        return Shadow.extract(application.getPackageManager());
    }
}
