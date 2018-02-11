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
import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.media.CarAudioManager;
import android.car.media.CarVolumeGroup;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.android.car.list.TypedPagedListAdapter;
import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;

import java.util.ArrayList;
import java.util.List;

import androidx.car.widget.PagedListView;

/**
 * Activity hosts sound related settings.
 */
public class SoundSettingsFragment extends BaseFragment {
    private static final String TAG = "SoundSettingsFragment";

    private final List<VolumeLineItem> mVolumeLineItems = new ArrayList<>();

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                mCarAudioManager = (CarAudioManager) mCar.getCarManager(Car.AUDIO_SERVICE);
                CarVolumeGroup[] carVolumeGroups = mCarAudioManager.getVolumeGroups();
                // Populates volume slider items from volume groups to UI.
                for (CarVolumeGroup volumeGroup : carVolumeGroups) {
                    mVolumeLineItems.add(new VolumeLineItem(
                            mCarAudioManager,
                            volumeGroup,
                            getIconResId(volumeGroup)));
                }
                // if list is already initiated, update it's content.
                if (mPagedListAdapter != null) {
                    mPagedListAdapter.updateList(new ArrayList<>(mVolumeLineItems));
                }
                mCarAudioManager.registerVolumeChangeObserver(mVolumeChangeObserver);
            } catch (CarNotConnectedException e) {
                Log.e(TAG, "Car is not connected!", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCarAudioManager.unregisterVolumeChangeObserver(mVolumeChangeObserver);
            mVolumeLineItems.clear();
            mCarAudioManager = null;
        }
    };

    private final ContentObserver mVolumeChangeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            mPagedListAdapter.notifyDataSetChanged();
        }
    };

    private Car mCar;
    private CarAudioManager mCarAudioManager;
    private PagedListView mListView;
    private TypedPagedListAdapter mPagedListAdapter;

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
        mListView = getView().findViewById(R.id.list);
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
        mCar.disconnect();
    }

    /**
     * TODO: return the drawable resource id by a given {@link CarVolumeGroup}.
     *
     * Settings and Car service normally won't be in the same package, therefore it's impractical
     * to include the drawable resource id in {@link CarVolumeGroup}.
     *
     * @param carVolumeGroup {@link CarVolumeGroup} instance to get the drawable resource id for
     * @return Drawable resource id to represent the {@link CarVolumeGroup} on UI
     */
    private @DrawableRes int getIconResId(CarVolumeGroup carVolumeGroup) {
        return R.drawable.ic_audio_navi;
    }
}
