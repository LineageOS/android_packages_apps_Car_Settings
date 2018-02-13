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
                int volumeGroupCount = mCarAudioManager.getVolumeGroupCount();
                // Populates volume slider items from volume groups to UI.
                for (int groupId = 0; groupId < volumeGroupCount; groupId++) {
                    mVolumeLineItems.add(new VolumeLineItem(
                            getContext(),
                            mCarAudioManager,
                            groupId,
                            getTitleResId(groupId),
                            getIconResId(groupId)));
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
     * Gets the title string resource id by a given volume group id.
     *
     * TODO(hwwang): enumerate the audio usages by group id and returns the title
     * string resource id by the hero usage.
     *
     * @param volumeGroupId The id of a volume group to get the title resource id for
     * @return String resource id to represent the volume group on UI
     */
    private @StringRes int getTitleResId(int volumeGroupId) {
        return R.string.about_settings;
    }

    /**
     * Gets the drawable resource id by a given volume group id.
     *
     * TODO(hwwang): enumerate the audio usages by group id and returns the icon
     * drawable resource id by the first recognized usage.
     *
     * @param volumeGroupId The id of a volume group to get the drawable resource id for
     * @return Drawable resource id to represent the volume group on UI
     */
    private @DrawableRes int getIconResId(int volumeGroupId) {
        return R.drawable.ic_audio_navi;
    }
}
