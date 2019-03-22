/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.settings.storage;

import android.annotation.Nullable;
import android.annotation.XmlRes;
import android.content.Context;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;

import androidx.loader.app.LoaderManager;

import com.android.car.settings.R;
import com.android.car.settings.common.SettingsFragment;

/** Fragment which shows the settings for storage. */
public class StorageSettingsFragment extends SettingsFragment {

    private StorageSettingsManager mStorageSettingsManager;

    @Override
    @XmlRes
    protected int getPreferenceScreenResId() {
        return R.xml.storage_settings_fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        StorageManager sm = context.getSystemService(StorageManager.class);
        VolumeInfo volume = maybeInitializeVolume(sm, getArguments());
        mStorageSettingsManager = new StorageSettingsManager(getContext(), volume);
        mStorageSettingsManager.registerListener(use(StoragePhotoCategoryPreferenceController.class,
                R.string.pk_storage_photos_videos));
        mStorageSettingsManager.registerListener(use(StorageMediaCategoryPreferenceController.class,
                R.string.pk_storage_music_audio));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LoaderManager loaderManager = LoaderManager.getInstance(this);
        mStorageSettingsManager.startLoading(loaderManager);
    }

    /**
     * Tries to initialize a volume with the given bundle. If it is a valid, private, and readable
     * {@link VolumeInfo}, it is returned. If it is not valid, null is returned.
     */
    @Nullable
    private static VolumeInfo maybeInitializeVolume(StorageManager sm, Bundle bundle) {
        String volumeId = bundle.getString(VolumeInfo.EXTRA_VOLUME_ID,
                VolumeInfo.ID_PRIVATE_INTERNAL);
        VolumeInfo volume = sm.findVolumeById(volumeId);
        return isVolumeValid(volume) ? volume : null;
    }

    private static boolean isVolumeValid(VolumeInfo volume) {
        return (volume != null) && (volume.getType() == VolumeInfo.TYPE_PRIVATE)
                && volume.isMountedReadable();
    }
}
