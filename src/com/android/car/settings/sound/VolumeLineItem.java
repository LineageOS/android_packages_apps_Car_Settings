/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.car.settings.sound;

import android.annotation.DrawableRes;
import android.annotation.StringRes;
import android.car.CarNotConnectedException;
import android.car.media.CarAudioManager;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import com.android.car.list.SeekbarLineItem;

/**
 * Contains logic about volume controller UI.
 */
public class VolumeLineItem extends SeekbarLineItem {
    private static final String TAG = "VolumeLineItem";

    private final CarAudioManager mCarAudioManager;
    private final int mVolumeGroupId;

    public VolumeLineItem(
            Context context,
            CarAudioManager carAudioManager,
            int volumeGroupId,
            @StringRes int titleResId,
            @DrawableRes int iconResId) throws CarNotConnectedException {
        super(context.getString(titleResId), iconResId);
        mCarAudioManager = carAudioManager;
        mVolumeGroupId = volumeGroupId;
    }

    @Override
    public int getSeekbarValue() {
        try {
            return mCarAudioManager.getGroupVolume(mVolumeGroupId);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!", e);
        }
        return 0;
    }

    @Override
    public int getMaxSeekbarValue() {
        try {
            return mCarAudioManager.getGroupMaxVolume(mVolumeGroupId);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!", e);
        }
        return 0;
    }

    @Override
    public void onSeekbarChanged(int progress, boolean fromUser) {
        if (!fromUser) {
            // For instance, if this event is originated from AudioService,
            // we can ignore it as it has already been handled and doesn't need to be
            // sent back down again.
            return;
        }
        try {
            if (mCarAudioManager == null) {
                Log.w(TAG, "Ignoring volume change event because the car isn't connected");
                return;
            }
            // Sets the flag to FLAG_PLAY_AUDIO since this is a volume change originated from user
            // interaction, an audio feedback should be requested in this case.
            mCarAudioManager.setGroupVolume(mVolumeGroupId, progress, AudioManager.FLAG_PLAY_SOUND);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!", e);
        }
    }
}
