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

import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.media.CarAudioManager;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.IVolumeController;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.car.settings.common.CarSettingActivity;
import com.android.car.settings.common.TypedPagedListAdapter;
import com.android.car.settings.R;
import com.android.car.view.PagedListView;

import java.util.ArrayList;

/**
 * Activity hosts sound related settings.
 */
public class SoundSettingsActivity extends CarSettingActivity {
    private static final String TAG = "SoundSettingsActivity";
    private Car mCar;
    private CarAudioManager mCarAudioManager;
    private PagedListView mListView;
    private TypedPagedListAdapter mPagedListAdapter;

    private final ArrayList<VolumeLineItem> mVolumeLineItems = new ArrayList<>();
    private final SoundSettingsActivity.VolumnCallback
            mVolumeCallback = new SoundSettingsActivity.VolumnCallback();

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                mCarAudioManager = (CarAudioManager) mCar.getCarManager(Car.AUDIO_SERVICE);
                mCarAudioManager.setVolumeController(mVolumeCallback);
            } catch (CarNotConnectedException e) {
                Log.e(TAG, "Car is not connected!", e);
            }
            for (VolumeLineItem item : mVolumeLineItems) {
                item.setCarAudioManager(mCarAudioManager);
            }
            mListView = (PagedListView) findViewById(R.id.list);
            mListView.setDefaultItemDecoration(
                    new PagedListView.Decoration(SoundSettingsActivity.this));
            mListView.setDarkMode();
            mPagedListAdapter = new TypedPagedListAdapter(
                    SoundSettingsActivity.this /* context */, mVolumeLineItems);
            mListView.setAdapter(mPagedListAdapter);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCarAudioManager = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);
        mVolumeLineItems.add(new VolumeLineItem(
                SoundSettingsActivity.this,
                AudioManager.STREAM_MUSIC,
                R.string.media_volume_title,
                com.android.internal.R.drawable.ic_audio_media));
        mVolumeLineItems.add(new VolumeLineItem(
                SoundSettingsActivity.this,
                AudioManager.STREAM_RING,
                R.string.ring_volume_title,
                com.android.internal.R.drawable.ic_audio_ring_notif));
        mCar = Car.createCar(this /* context */, mServiceConnection);
    }

    @Override
    public void onStart() {
        super.onStart();
        mCar.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        for (VolumeLineItem item : mVolumeLineItems) {
            item.stop();
        }
        mCar.disconnect();
    }

    /**
     * The interface has a terrible name, it is actually a callback, so here name it accordingly.
     */
    private final class VolumnCallback extends IVolumeController.Stub {
        @Override
        public void displaySafeVolumeWarning(int flags) throws RemoteException {
        }

        @Override
        public void volumeChanged(int streamType, int flags) throws RemoteException {
            for (VolumeLineItem item : mVolumeLineItems) {
                if (streamType == item.getStreamType()) {
                    break;
                }
                return;
            }
            mPagedListAdapter.notifyDataSetChanged();
        }

        // this is not mute of this stream
        @Override
        public void masterMuteChanged(int flags) throws RemoteException {
        }

        @Override
        public void setLayoutDirection(int layoutDirection) throws RemoteException {
        }

        @Override
        public void dismiss() throws RemoteException {
        }

        @Override
        public void setA11yMode(int mode) {
        }
    }
}
