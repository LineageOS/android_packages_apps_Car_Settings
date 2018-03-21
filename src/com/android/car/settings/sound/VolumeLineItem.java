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
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.SeekBar;

import androidx.car.widget.SeekbarListItem;

/**
 * Contains logic about volume controller UI.
 */
public class VolumeLineItem extends SeekbarListItem {
    private static final String TAG = "VolumeLineItem";
    private final SeekbarListener mSeekbarListener;

    public VolumeLineItem(
            Context context,
            CarAudioManager carAudioManager,
            int volumeGroupId,
            @AudioAttributes.AttributeUsage int usage,
            @DrawableRes int iconResId,
            SeekbarListener seekbarListener) throws CarNotConnectedException {
        super(context, getMaxSeekbarValue(carAudioManager, volumeGroupId),
            getSeekbarValue(carAudioManager, volumeGroupId), seekbarListener, null);
        setPrimaryActionIcon(iconResId);
        mSeekbarListener = seekbarListener;
    }

    /**
     * Handles seekbar progress change from user.
     * TODO: refactor SeekbarListItem so we don't need a separate listener class.
     */
    public static class SeekbarListener implements SeekBar.OnSeekBarChangeListener {
        private static final int AUDIO_FEEDBACK_DURATION_MS = 1000;

        private final Handler mUiHandler;
        private final Ringtone mRingtone;
        private final int mVolumeGroupId;
        private final CarAudioManager mCarAudioManager;

        public SeekbarListener(Context context,
                CarAudioManager carAudioManager,
                int volumeGroupId,
                @AudioAttributes.AttributeUsage int usage) {
            mCarAudioManager = carAudioManager;
            mUiHandler = new Handler(Looper.getMainLooper());
            mRingtone = RingtoneManager.getRingtone(context, getRingtoneUri(usage));
            mRingtone.setAudioAttributes(new AudioAttributes.Builder().setUsage(usage).build());
            mVolumeGroupId = volumeGroupId;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // no-op
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // no-op
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
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
                // AudioManager.FLAG_PLAY_SOUND does not guarantee play sound, use our own
                // playback here instead.
                mCarAudioManager.setGroupVolume(mVolumeGroupId, progress, 0);
                playAudioFeedback();
            } catch (CarNotConnectedException e) {
                Log.e(TAG, "Car is not connected!", e);
            }
        }

        /**
         * Cleans up. Should be called before this object is no longer needed.
         */
        public void stop() {
            mUiHandler.removeCallbacksAndMessages(null);
            mRingtone.stop();
        }

        private void playAudioFeedback() {
            mUiHandler.removeCallbacksAndMessages(null);
            mRingtone.play();
            mUiHandler.postDelayed(() -> {
                if (mRingtone.isPlaying()) {
                    mRingtone.stop();
                }
            }, AUDIO_FEEDBACK_DURATION_MS);
        }

        // TODO: bundle car-specific audio sample assets in res/raw by usage
        private Uri getRingtoneUri(@AudioAttributes.AttributeUsage int usage) {
            switch (usage) {
                case AudioAttributes.USAGE_NOTIFICATION:
                    return Settings.System.DEFAULT_NOTIFICATION_URI;
                case AudioAttributes.USAGE_ALARM:
                    return Settings.System.DEFAULT_ALARM_ALERT_URI;
                default:
                    return Settings.System.DEFAULT_RINGTONE_URI;
            }
        }
    }

    public void stop() {
        mSeekbarListener.stop();
    }


    private static int getSeekbarValue(CarAudioManager carAudioManager, int volumeGroupId) {
        try {
            return carAudioManager.getGroupVolume(volumeGroupId);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!", e);
        }
        return 0;
    }

    private static int getMaxSeekbarValue(CarAudioManager carAudioManager, int volumeGroupId) {
        try {
            return carAudioManager.getGroupMaxVolume(volumeGroupId);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!", e);
        }
        return 0;
    }
}
