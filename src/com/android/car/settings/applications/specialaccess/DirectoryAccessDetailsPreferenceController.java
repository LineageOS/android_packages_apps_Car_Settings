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
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS_COLUMNS;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS_COL_DIRECTORY;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS_COL_GRANTED;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS_COL_PACKAGE;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS_COL_VOLUME_UUID;

import android.annotation.Nullable;
import android.car.drivingstate.CarUxRestrictions;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.ArrayMap;
import android.util.Pair;

import androidx.preference.PreferenceGroup;
import androidx.preference.SwitchPreference;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.PreferenceController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detailed settings for an app's directory access permissions (A.K.A Scoped Directory Access).
 *
 * <p>It shows the directories for which the user denied access with the "Do not ask again" flag.
 * The user can use the preference toggles to grant access again.
 *
 * <p>This controller dynamically lists all such permissions starting with one preference per
 * directory in the primary storage then adding additional preferences for external volumes (one
 * for the whole volume and one for each individual directory). Granting access to a whole volume
 * will hide individual directory permissions.
 */
public class DirectoryAccessDetailsPreferenceController extends
        PreferenceController<PreferenceGroup> {

    private static final Logger LOG = new Logger(DirectoryAccessDetailsPreferenceController.class);

    private String mPackageName;

    public DirectoryAccessDetailsPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<PreferenceGroup> getPreferenceType() {
        return PreferenceGroup.class;
    }

    /**
     * Sets the package for which to display directory access. This should be called right after the
     * controller is instantiated.
     */
    public void setPackage(String packageName) {
        mPackageName = packageName;
    }

    @Override
    protected void checkInitialized() {
        if (mPackageName == null) {
            throw new IllegalStateException("Must specify package for directory access details");
        }
    }

    @Override
    protected void updateState(PreferenceGroup preferenceGroup) {
        preferenceGroup.removeAll();
        preferenceGroup.setOrderingAsAdded(false);

        Map<String, ExternalVolume> externalVolumes = new ArrayMap<>();
        Uri providerUri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(
                AUTHORITY).appendPath(TABLE_PERMISSIONS).appendPath("*").build();
        // Query provider for entries.
        try (Cursor cursor = getContext().getContentResolver().query(providerUri,
                TABLE_PERMISSIONS_COLUMNS, /* selection= */ null,
                new String[]{mPackageName}, /* sortOrder= */ null)) {
            if (cursor == null) {
                LOG.w("Didn't get cursor for " + mPackageName);
                return;
            }
            int count = cursor.getCount();
            if (count == 0) {
                // This setting screen should not be reached if there was no permission, so just
                // ignore it.
                LOG.w("No permissions for " + mPackageName);
                return;
            }

            while (cursor.moveToNext()) {
                String pkg = cursor.getString(TABLE_PERMISSIONS_COL_PACKAGE);
                String uuid = cursor.getString(TABLE_PERMISSIONS_COL_VOLUME_UUID);
                String dir = cursor.getString(TABLE_PERMISSIONS_COL_DIRECTORY);
                boolean granted = cursor.getInt(TABLE_PERMISSIONS_COL_GRANTED) == 1;
                LOG.v("Pkg:" + pkg + " uuid: " + uuid + " dir: " + dir + " granted:" + granted);

                if (!mPackageName.equals(pkg)) {
                    // Sanity check, shouldn't happen.
                    LOG.w("Ignoring " + uuid + "/" + dir + " due to package mismatch: "
                            + "expected " + mPackageName + ", got " + pkg);
                    continue;
                }

                if (uuid == null) {
                    if (dir == null) {
                        // Sanity check, shouldn't happen.
                        LOG.wtf("Ignoring permission on primary storage root");
                    } else {
                        // Primary storage entry: add right away
                        preferenceGroup.addPreference(
                                createPreference(dir, providerUri, /* uuid= */ null, dir,
                                        granted));
                    }
                } else {
                    // External volume entry: save it for later.
                    ExternalVolume externalVolume = externalVolumes.get(uuid);
                    if (externalVolume == null) {
                        externalVolume = new ExternalVolume(uuid);
                        externalVolumes.put(uuid, externalVolume);
                    }
                    if (dir == null) {
                        // Whole volume.
                        externalVolume.mIsGranted = granted;
                    } else {
                        // Directory only.
                        externalVolume.mChildren.add(new Pair<>(dir, granted));
                    }
                }
            }
        }

        LOG.v("external volumes: " + externalVolumes);

        if (externalVolumes.isEmpty()) {
            // We're done!
            return;
        }

        // Add entries from external volumes

        // Query StorageManager to get the user-friendly volume names.
        StorageManager sm = getContext().getSystemService(StorageManager.class);
        List<VolumeInfo> volumes = sm.getVolumes();
        if (volumes.isEmpty()) {
            LOG.w("StorageManager returned no secondary volumes");
            return;
        }
        Map<String, String> volumeNames = new HashMap<>(volumes.size());
        for (VolumeInfo volume : volumes) {
            String uuid = volume.getFsUuid();
            if (uuid == null) {
                continue; // Primary storage, only directory name used.
            }
            String name = sm.getBestVolumeDescription(volume);
            if (name == null) {
                LOG.w("No description for " + volume + "; using uuid instead: " + uuid);
                name = uuid;
            }
            volumeNames.put(uuid, name);
        }
        LOG.v("UUID -> name mapping: " + volumeNames);

        for (ExternalVolume volume : externalVolumes.values()) {
            String volumeName = volumeNames.get(volume.mUuid);
            if (volumeName == null) {
                LOG.w("Ignoring entry for invalid UUID: " + volume.mUuid);
                continue;
            }
            // First add the preference for the whole volume...
            preferenceGroup.addPreference(createPreference(volumeName, providerUri, volume.mUuid,
                    /* dir= */ null, volume.mIsGranted));

            // ... then the child preferences for directories.
            if (!volume.mIsGranted) {
                volume.mChildren.forEach(pair -> {
                    String dir = pair.first;
                    boolean isGranted = pair.second;
                    String name = getContext().getResources()
                            .getString(R.string.directory_on_volume, volumeName, dir);
                    SwitchPreference childPref =
                            createPreference(name, providerUri, volume.mUuid, dir, isGranted);
                    preferenceGroup.addPreference(childPref);
                });
            }
        }
    }

    private SwitchPreference createPreference(String title, Uri providerUri, String uuid,
            String dir, boolean isGranted) {
        SwitchPreference pref = new SwitchPreference(getContext());
        pref.setKey(String.format("%s:%s", uuid, dir));
        pref.setTitle(title);
        pref.setChecked(isGranted);
        pref.setPersistent(false);
        pref.setOnPreferenceChangeListener((unused, value) -> {
            boolean newGrantedState = (Boolean) value;
            setGranted(newGrantedState, providerUri, uuid, dir);
            refreshUi();
            return true;
        });
        return pref;
    }

    private void setGranted(boolean isGranted, Uri providerUri,
            @Nullable String uuid, @Nullable String directory) {
        LOG.d("Asking " + providerUri + " to update " + uuid + "/" + directory + " to "
                + isGranted);
        ContentValues values = new ContentValues(1);
        values.put(COL_GRANTED, isGranted);
        int updated = getContext().getContentResolver().update(providerUri, values,
                /* where= */ null, new String[]{mPackageName, uuid, directory});
        LOG.d("Updated " + updated + " entries for " + uuid + "/" + directory);
    }

    private static class ExternalVolume {

        String mUuid;
        /** Key: directory, Value: isGranted */
        List<Pair<String, Boolean>> mChildren = new ArrayList<>();
        boolean mIsGranted;

        ExternalVolume(String uuid) {
            mUuid = uuid;
        }

        @Override
        public String toString() {
            return "ExternalVolume: [uuid=" + mUuid + ", granted=" + mIsGranted + ", children="
                    + mChildren + "]";
        }
    }
}
