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
import android.car.CarNotConnectedException;
import android.car.media.CarAudioManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.StringRes;
import android.util.Log;

import com.android.car.list.SeekbarLineItem;

/**
 * Contains logic about volume controller UI.
 */
public class VolumeLineItem extends SeekbarLineItem {
    private static final String TAG = "VolumeLineItem";

    private final @AudioAttributes.AttributeUsage int mAttributeUsage;
    private final Ringtone mRingtone;

    private CarAudioManager mCarAudioManager;

    public VolumeLineItem(
            Context context,
            CarAudioManager carAudioManager,
            @AudioAttributes.AttributeUsage int attributeUsage,
            @StringRes int titleStringResId,
            @DrawableRes int iconResId) throws CarNotConnectedException {
        super(context.getText(titleStringResId), iconResId);
        mCarAudioManager = carAudioManager;
        mAttributeUsage = attributeUsage;
        Uri ringtoneUri;

        switch (mAttributeUsage) {
            case AudioAttributes.USAGE_NOTIFICATION_RINGTONE:
                ringtoneUri = Settings.System.DEFAULT_RINGTONE_URI;
                break;
            case AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE:
                ringtoneUri = Settings.System.DEFAULT_NOTIFICATION_URI;
                break;
            default:
                ringtoneUri = Settings.System.DEFAULT_ALARM_ALERT_URI;
        }
        mRingtone = RingtoneManager.getRingtone(context, ringtoneUri);
        if (mRingtone != null) {
            mRingtone.setAudioAttributes(
                    new AudioAttributes.Builder().setUsage(mAttributeUsage).build());
        }
    }

    public void stop() {
        if (mRingtone != null) {
            mRingtone.stop();
        }
    }

    @Override
    public int getSeekbarValue() {
        try {
            return mCarAudioManager.getUsageVolume(mAttributeUsage);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!", e);
        }
        return 0;
    }

    @Override
    public int getMaxSeekbarValue() {
        try {
            return mCarAudioManager.getUsageMaxVolume(mAttributeUsage);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!", e);
        }
        return 0;
    }

    @Override
    public void onSeekbarChanged(int progress) {
        try {
            if (mCarAudioManager == null) {
                Log.w(TAG, "CarAudiomanager not available, Car is not connected!");
                return;
            }
            // the flag is a request to play sound, depend on implementation, it may not play
            // anything, the listener in SoundSettings class will play audible feedback.
            mCarAudioManager.setUsageVolume(
                    mAttributeUsage, progress, AudioManager.FLAG_PLAY_SOUND);
            // playAudioFeedback();
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!", e);
        }
    }
}
