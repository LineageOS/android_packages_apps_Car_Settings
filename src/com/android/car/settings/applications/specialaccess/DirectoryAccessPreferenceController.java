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
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PACKAGES_COLUMNS;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PACKAGES_COL_PACKAGE;

import android.car.drivingstate.CarUxRestrictions;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.ArraySet;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceGroup;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.PreferenceController;
import com.android.car.ui.preference.CarUiPreference;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;

import java.util.List;
import java.util.Set;

/**
 * Displays a list of preferences for apps that have directory access permissions set. Selecting an
 * app launches a detailed view for controlling permissions at the directory level.
 */
public class DirectoryAccessPreferenceController extends PreferenceController<PreferenceGroup> {

    private static final Logger LOG = new Logger(DirectoryAccessPreferenceController.class);

    private static final AppFilter FILTER_APP_HAS_DIRECTORY_ACCESS = new AppFilter() {

        private Set<String> mPackages;

        @Override
        public void init() {
            throw new UnsupportedOperationException("Need to call constructor that takes context");
        }

        @Override
        public void init(Context context) {
            mPackages = null;
            Uri providerUri = new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(AUTHORITY)
                    .appendPath(TABLE_PACKAGES)
                    .appendPath("*")
                    .build();
            try (Cursor cursor = context.getContentResolver().query(providerUri,
                    TABLE_PACKAGES_COLUMNS, /* queryArgs= */ null, /* cancellationSignal= */
                    null)) {
                if (cursor == null) {
                    LOG.w("Didn't get cursor for " + providerUri);
                    return;
                }
                int count = cursor.getCount();
                if (count == 0) {
                    LOG.d("No packages anymore (was " + mPackages + ")");
                    return;
                }
                mPackages = new ArraySet<>(count);
                while (cursor.moveToNext()) {
                    mPackages.add(cursor.getString(TABLE_PACKAGES_COL_PACKAGE));
                }
                LOG.d("init(): " + mPackages);
            }
        }


        @Override
        public boolean filterApp(AppEntry info) {
            return mPackages != null && mPackages.contains(info.info.packageName);
        }
    };

    private final AppEntryListManager.Callback mCallback = new AppEntryListManager.Callback() {
        @Override
        public void onAppEntryListChanged(List<AppEntry> entries) {
            mEntries = entries;
            refreshUi();
        }
    };

    @VisibleForTesting
    AppEntryListManager mAppEntryListManager;
    private List<AppEntry> mEntries;

    public DirectoryAccessPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mAppEntryListManager = new AppEntryListManager(context);
    }

    @Override
    protected Class<PreferenceGroup> getPreferenceType() {
        return PreferenceGroup.class;
    }

    @Override
    protected void onCreateInternal() {
        mAppEntryListManager.init(/* extraInfoBridge= */ null,
                () -> FILTER_APP_HAS_DIRECTORY_ACCESS, mCallback);
    }

    @Override
    protected void onStartInternal() {
        mAppEntryListManager.start();
    }

    @Override
    protected void onStopInternal() {
        mAppEntryListManager.stop();
    }

    @Override
    protected void onDestroyInternal() {
        mAppEntryListManager.destroy();
    }

    @Override
    protected void updateState(PreferenceGroup preference) {
        if (mEntries == null) {
            // Still loading.
            return;
        }
        preference.removeAll();
        for (AppEntry entry : mEntries) {
            CarUiPreference appPreference = new CarUiPreference(getContext());
            String key = entry.info.packageName + "|" + entry.info.uid;
            appPreference.setKey(key);
            appPreference.setTitle(entry.label);
            appPreference.setIcon(entry.icon);
            appPreference.setOnPreferenceClickListener(clickedPref -> {
                getFragmentController().launchFragment(
                        DirectoryAccessDetailsFragment.getInstance(entry.info.packageName));
                return true;
            });
            preference.addPreference(appPreference);
        }
    }
}
