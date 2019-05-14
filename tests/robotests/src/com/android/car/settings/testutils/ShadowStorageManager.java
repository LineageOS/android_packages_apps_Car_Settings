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

package com.android.car.settings.testutils;

import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.ArrayMap;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.List;
import java.util.Map;

@Implements(StorageManager.class)
public class ShadowStorageManager {

    private List<VolumeInfo> mVolumes;
    private Map<VolumeInfo, String> mBestVolumeDescriptions = new ArrayMap<>();

    public void setVolumes(List<VolumeInfo> volumes) {
        mVolumes = volumes;
    }

    @Implementation
    protected List<VolumeInfo> getVolumes() {
        return mVolumes;
    }

    public void setBestVolumeDescription(VolumeInfo volume, String description) {
        mBestVolumeDescriptions.put(volume, description);
    }

    @Implementation
    protected String getBestVolumeDescription(VolumeInfo volume) {
        return mBestVolumeDescriptions.get(volume);
    }
}
