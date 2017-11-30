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
import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.media.CarAudioManager;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.android.car.list.TypedPagedListAdapter;
import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;

import java.util.ArrayList;
import java.util.List;

import androidx.car.widget.DayNightStyle;
import androidx.car.widget.PagedListView;

/**
 * Activity hosts sound related settings.
 */
public class SoundSettingsFragment extends BaseFragment {
    private static final String TAG = "SoundSettingsFragment";

    private final List<VolumeLineItem> mVolumeLineItems = new ArrayList<>();

    private final List<VolumeControlSliderItem> mVolumeControlSliderItems = new ArrayList<>();

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                mCarAudioManager = (CarAudioManager) mCar.getCarManager(Car.AUDIO_SERVICE);
                // Populates volume slider items from mVolumeControlSliderItems to UI.
                for (VolumeControlSliderItem volumeControlSliderItem : mVolumeControlSliderItems) {
                    mVolumeLineItems.add(new VolumeLineItem(
                            getContext(),
                            mCarAudioManager,
                            volumeControlSliderItem.carAudioUsage,
                            volumeControlSliderItem.nameStringId,
                            volumeControlSliderItem.iconId));
                }
                // if list is already initiated, update it's content.
                if (mPagedListAdapter != null) {
                    mPagedListAdapter.updateList(new ArrayList<>(mVolumeLineItems));
                }
            } catch (CarNotConnectedException e) {
                Log.e(TAG, "Car is not connected!", e);
                return;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            try {
                if (mCarAudioManager != null) {
                    mCarAudioManager.setVolumeController(null);
                }
            } catch (CarNotConnectedException e) {
                Log.e(TAG, "Car is not connected!", e);
                return;
            }
            mCarAudioManager = null;
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
        // Defines available volume slider items here. These items would be populated to UI
        // once car audio service is connected.
        mVolumeControlSliderItems.add(new VolumeControlSliderItem(
                CarAudioManager.CAR_AUDIO_USAGE_MUSIC,
                R.string.media_volume_title,
                com.android.internal.R.drawable.ic_audio_media));
        mVolumeControlSliderItems.add(new VolumeControlSliderItem(
                CarAudioManager.CAR_AUDIO_USAGE_RINGTONE,
                R.string.ring_volume_title,
                com.android.internal.R.drawable.ic_audio_ring_notif));
        mVolumeControlSliderItems.add(new VolumeControlSliderItem(
                CarAudioManager.CAR_AUDIO_USAGE_NAVIGATION_GUIDANCE,
                R.string.navi_volume_title,
                R.drawable.ic_audio_navi));
        mCar = Car.createCar(getContext(), mServiceConnection);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView = getView().findViewById(R.id.list);
        mListView.setDayNightStyle(DayNightStyle.FORCE_DAY);
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
     * Represents a slider item for volume control.
     */
    private static final class VolumeControlSliderItem {
        @CarAudioManager.CarAudioUsage
        final int carAudioUsage;

        @StringRes
        final int nameStringId;

        @DrawableRes
        final int iconId;

        VolumeControlSliderItem(
                @CarAudioManager.CarAudioUsage int carAudioUsage,
                @StringRes int nameStringId,
                @DrawableRes int iconId) {
            this.carAudioUsage = carAudioUsage;
            this.nameStringId = nameStringId;
            this.iconId = iconId;
        }

        @Override
        public int hashCode() {
            return carAudioUsage;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof VolumeControlSliderItem)) {
                return false;
            }
            return carAudioUsage == ((VolumeControlSliderItem) o).carAudioUsage;
        }
    }
}
