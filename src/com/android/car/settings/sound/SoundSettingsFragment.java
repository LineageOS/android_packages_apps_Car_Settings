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
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.IVolumeController;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.car.settings.common.BaseFragment;
import com.android.car.settings.common.TypedPagedListAdapter;
import com.android.car.settings.R;
import com.android.car.view.PagedListView;

import java.util.ArrayList;

/**
 * Activity hosts sound related settings.
 */
public class SoundSettingsFragment extends BaseFragment {
    private static final String TAG = "SoundSettingsFragment";
    private Car mCar;
    private CarAudioManager mCarAudioManager;
    private PagedListView mListView;
    private TypedPagedListAdapter mPagedListAdapter;

    private final ArrayList<VolumeLineItem> mVolumeLineItems = new ArrayList<>();
    private final SoundSettingsFragment.VolumnCallback
            mVolumeCallback = new SoundSettingsFragment.VolumnCallback();

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioAttributes naviAudioAttributes;
            AudioAttributes systemAudioAttributes;
            AudioAttributes mediaAudioAttributes;
            try {
                mCarAudioManager = (CarAudioManager) mCar.getCarManager(Car.AUDIO_SERVICE);
                mCarAudioManager.setVolumeController(mVolumeCallback);

                systemAudioAttributes = mCarAudioManager.getAudioAttributesForCarUsage(
                        mCarAudioManager.CAR_AUDIO_USAGE_SYSTEM_SOUND);
                mediaAudioAttributes = mCarAudioManager.getAudioAttributesForCarUsage(
                        mCarAudioManager.CAR_AUDIO_USAGE_MUSIC);
            } catch (CarNotConnectedException e) {
                Log.e(TAG, "Car is not connected!", e);
                return;
            }

            // It turns out that the stream id for system and navigation are the same.
            // skip navi for now
            mVolumeLineItems.add(new VolumeLineItem(
                    getContext(),
                    mCarAudioManager,
                    mediaAudioAttributes.getVolumeControlStream(),
                    R.string.media_volume_title,
                    com.android.internal.R.drawable.ic_audio_media));
            mVolumeLineItems.add(new VolumeLineItem(
                    getContext(),
                    mCarAudioManager,
                    systemAudioAttributes.getVolumeControlStream(),
                    R.string.ring_volume_title,
                    com.android.internal.R.drawable.ic_audio_ring_notif));
            // if list is already initiated, update it's content.
            if (mPagedListAdapter != null) {
                mPagedListAdapter.updateList(new ArrayList<>(mVolumeLineItems));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCarAudioManager = null;
        }
    };

    public static SoundSettingsFragment getInstance() {
        SoundSettingsFragment soundSettingsFragment = new SoundSettingsFragment();
        Bundle bundle = BaseFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.sound_settings);
        bundle.putInt(EXTRA_LAYOUT, R.layout.list);
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar);
        soundSettingsFragment.setArguments(bundle);
        return soundSettingsFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCar = Car.createCar(getContext(), mServiceConnection);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView = (PagedListView) getView().findViewById(R.id.list);
        mListView.setDarkMode();
        mPagedListAdapter = new TypedPagedListAdapter(getContext());
        mListView.setAdapter(mPagedListAdapter);
        if (!mVolumeLineItems.isEmpty()) {
            mPagedListAdapter.updateList(new ArrayList<>(mVolumeLineItems));
        }
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
        mVolumeLineItems.clear();
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
