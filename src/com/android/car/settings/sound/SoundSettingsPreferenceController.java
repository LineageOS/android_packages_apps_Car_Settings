/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.settings.sound;

import static com.android.car.settings.sound.VolumeItemParser.VolumeItem;

import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.media.CarAudioManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.SparseArray;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.XmlRes;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.NoSetupPreferenceController;
import com.android.car.settings.common.SeekBarPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * Business logic which parses car volume items into groups, creates a seek bar preference for each
 * group, and interfaces with the ringtone manager and audio manager.
 *
 * @see SoundSettingsRingtoneManager
 * @see android.car.media.CarAudioManager
 */
public class SoundSettingsPreferenceController extends NoSetupPreferenceController implements
        LifecycleObserver {
    private static final Logger LOG = new Logger(SoundSettingsPreferenceController.class);
    private static final String VOLUME_GROUP_KEY = "volume_group_key";
    private static final String VOLUME_USAGE_KEY = "volume_usage_key";

    private final SparseArray<VolumeItem> mVolumeItems;
    private final List<SeekBarPreference> mVolumePreferences = new ArrayList<>();
    private final SoundSettingsRingtoneManager mRingtoneManager;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                mCarAudioManager = (CarAudioManager) mCar.getCarManager(Car.AUDIO_SERVICE);
                int volumeGroupCount = mCarAudioManager.getVolumeGroupCount();
                cleanUpVolumePreferences();
                // Populates volume slider items from volume groups to UI.
                for (int groupId = 0; groupId < volumeGroupCount; groupId++) {
                    VolumeItem volumeItem = getVolumeItemForUsages(
                            mCarAudioManager.getUsagesForVolumeGroupId(groupId));
                    SeekBarPreference volumePreference = createVolumeSeekBarPreference(
                            groupId, volumeItem.getUsage(), volumeItem.getIcon(),
                            volumeItem.getTitle());
                    mVolumePreferences.add(volumePreference);
                }

                // If service connected before preference screen was displayed, it should be called
                // later. If the preference screen is already displayed, refresh the views.
                if (mPreferenceScreen != null) {
                    displayPreference(mPreferenceScreen);
                }
            } catch (CarNotConnectedException e) {
                LOG.e("Car is not connected!", e);
            }
        }

        /** Cleanup audio related fields when car is disconnected. */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            cleanupAudioManager();
        }
    };

    private Car mCar;
    private CarAudioManager mCarAudioManager;
    private PreferenceScreen mPreferenceScreen;

    public SoundSettingsPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);

        mCar = Car.createCar(mContext, mServiceConnection);
        mVolumeItems = VolumeItemParser.loadAudioUsageItems(context, carVolumeItemsXml());
        mRingtoneManager = new SoundSettingsRingtoneManager(mContext);
    }

    /** Connect to car on create. */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {
        mCar.connect();
    }

    /** Disconnect from car on destroy. */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        mCar.disconnect();
    }

    /**
     * The resource which lists the car volume resources associated with the various usage enums.
     */
    @XmlRes
    @VisibleForTesting
    int carVolumeItemsXml() {
        return R.xml.car_volume_items;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceScreen = screen;
        for (SeekBarPreference preference : mVolumePreferences) {
            screen.addPreference(preference);
        }
    }

    private SeekBarPreference createVolumeSeekBarPreference(
            int volumeGroupId, int usage, @DrawableRes int iconResId,
            @StringRes int titleId) {
        SeekBarPreference preference = new SeekBarPreference(mContext);
        preference.setTitle(mContext.getString(titleId));
        preference.setIcon(mContext.getDrawable(iconResId));
        try {

            preference.setValue(mCarAudioManager.getGroupVolume(volumeGroupId));
            preference.setMin(mCarAudioManager.getGroupMinVolume(volumeGroupId));
            preference.setMax(mCarAudioManager.getGroupMaxVolume(volumeGroupId));
        } catch (CarNotConnectedException e) {
            LOG.e("Car is not connected!", e);
        }
        preference.setContinuousUpdate(true);
        preference.setShowSeekBarValue(false);
        Bundle bundle = preference.getExtras();
        bundle.putInt(VOLUME_GROUP_KEY, volumeGroupId);
        bundle.putInt(VOLUME_USAGE_KEY, usage);
        preference.setOnPreferenceChangeListener((pref, newValue) -> {
            int prefGroup = pref.getExtras().getInt(VOLUME_GROUP_KEY);
            int prefUsage = pref.getExtras().getInt(VOLUME_USAGE_KEY);
            int newVolume = (Integer) newValue;
            setGroupVolume(prefGroup, newVolume);
            mRingtoneManager.playAudioFeedback(prefGroup, prefUsage);
            return true;
        });
        return preference;
    }

    private void setGroupVolume(int volumeGroupId, int newVolume) {
        try {
            mCarAudioManager.setGroupVolume(volumeGroupId, newVolume, /* flags= */ 0);
        } catch (CarNotConnectedException e) {
            LOG.w("Ignoring volume change event because the car isn't connected", e);
        }
    }

    private void cleanupAudioManager() {
        cleanUpVolumePreferences();
        mCarAudioManager = null;
    }

    private void cleanUpVolumePreferences() {
        mRingtoneManager.stopCurrentRingtone();
        mVolumePreferences.clear();
    }

    private VolumeItem getVolumeItemForUsages(int[] usages) {
        int rank = Integer.MAX_VALUE;
        VolumeItem result = null;
        for (int usage : usages) {
            VolumeItem volumeItem = mVolumeItems.get(usage);
            if (volumeItem.getRank() < rank) {
                rank = volumeItem.getRank();
                result = volumeItem;
            }
        }
        return result;
    }
}
