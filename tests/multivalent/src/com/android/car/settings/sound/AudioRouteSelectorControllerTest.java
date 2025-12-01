/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.media.AudioDeviceAttributes;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.widget.Toast;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.Flags;
import com.android.car.settings.common.CollapsibleSeekbarPreference;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.sound.audiorouting.AudioRoutePreferenceGroup;
import com.android.car.settings.testutils.TestLifecycleOwner;
import com.android.dx.mockito.inline.extended.ExtendedMockito;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class AudioRouteSelectorControllerTest {
    private static final String AUDIO_DEVICE_NAME = "audio_device_name";
    private static final String AUDIO_DEVICE_ADDRESS = "audio_device_address";
    private static final String BT_DEVICE_NAME = "bt_device_name";
    private static final String BT_DEVICE_ADDRESS = "bt_device_address";
    private static final String BROADCAST_NAME = "broadcast_name";
    private static final String BROADCAST_ADDRESS = "broadcast_address";
    private Context mContext = ApplicationProvider.getApplicationContext();
    private LifecycleOwner mLifecycleOwner;
    private CarUxRestrictions mCarUxRestrictions;
    private AudioRouteSelectorController mPreferenceController;
    private AudioRoutePreferenceGroup mPreference;
    private MockitoSession mSession;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private FragmentController mFragmentController;
    @Mock
    private AudioRoutesManager mAudioRoutesManager;
    @Mock
    private Toast mMockToast;
    private ArgumentCaptor<AudioRoutesManager.AudioRoutesUpdateListener> mListenerCaptor;
    @Mock
    private AudioDeviceAttributes mAudioDeviceAttributes;

    @Before
    public void setUp() {
        mLifecycleOwner = new TestLifecycleOwner();

        mSession = ExtendedMockito.mockitoSession()
                .initMocks(this)
                .mockStatic(Toast.class)
                .strictness(Strictness.LENIENT)
                .startMocking();
        ExtendedMockito.when(Toast.makeText(any(), anyString(), anyInt())).thenReturn(mMockToast);

        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();
        mPreferenceController = new AudioRouteSelectorController(mContext,
                /* preferenceKey= */ "key", mFragmentController,
                mCarUxRestrictions);
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen preferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = new AudioRoutePreferenceGroup(mContext);
        preferenceScreen.addPreference(mPreference);
        mPreferenceController.setAudioRoutesManager(mAudioRoutesManager);
        mListenerCaptor = ArgumentCaptor.forClass(
                AudioRoutesManager.AudioRoutesUpdateListener.class);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        when(mAudioRoutesManager.isAudioRoutingEnabled()).thenReturn(true);
    }

    @After
    public void tearDown() {
        if (mAudioRoutesManager != null) {
            mAudioRoutesManager.tearDown();
        }
        if (mSession != null) {
            mSession.finishMocking();
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_NEW_AUDIO_ROUTING_UI)
    public void onCreate_testUpdatePreferenceOptions() {
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
        verify(mAudioRoutesManager).setAudioRoutesUpdateListener(mListenerCaptor.capture());
        AudioRoutesManager.AudioRoutesUpdateListener capturedListener = mListenerCaptor.getValue();
        when(mAudioDeviceAttributes.getName()).thenReturn(AUDIO_DEVICE_NAME);
        when(mAudioDeviceAttributes.getAddress()).thenReturn(AUDIO_DEVICE_ADDRESS);
        AudioRouteItem audioDevice = new AudioRouteItem.Builder(mAudioDeviceAttributes)
                .setAudioZoneConfigState(new AudioRouteItem.AudioZoneConfigState.Builder()
                        .build()
                ).build();
        when(mAudioDeviceAttributes.getName()).thenReturn(BROADCAST_NAME);
        when(mAudioDeviceAttributes.getAddress()).thenReturn(BROADCAST_ADDRESS);
        AudioRouteItem broadcast = new AudioRouteItem.Builder(mAudioDeviceAttributes)
                .setState(AudioRouteItem.State.BROADCAST_ACTIVE)
                .setAudioZoneConfigState(new AudioRouteItem.AudioZoneConfigState.Builder()
                        .build()
                ).build();
        when(mAudioDeviceAttributes.getName()).thenReturn(BT_DEVICE_NAME);
        when(mAudioDeviceAttributes.getAddress()).thenReturn(BT_DEVICE_ADDRESS);
        AudioRouteItem btDevice = new AudioRouteItem.Builder(mAudioDeviceAttributes)
                .setAudioZoneConfigState(new AudioRouteItem.AudioZoneConfigState.Builder()
                        .build()
                ).build();

        capturedListener.onAudioRoutesUpdated(List.of(audioDevice, broadcast, btDevice));

        assertThat(mPreference.getPreferenceCount()).isEqualTo(2);
        CollapsibleSeekbarPreference audioDevicePreference = mPreference.getPreference(0);
        assertThat(audioDevicePreference.getTitle().toString()).isEqualTo(AUDIO_DEVICE_NAME);
        assertThat(audioDevicePreference.getKey()).isEqualTo(AUDIO_DEVICE_ADDRESS);

        CollapsibleSeekbarPreference btDevicePreference = mPreference.getPreference(1);
        assertThat(btDevicePreference.getTitle().toString()).isEqualTo(BT_DEVICE_NAME);
        assertThat(btDevicePreference.getKey()).isEqualTo(BT_DEVICE_ADDRESS);
    }

    @Test
    @EnableFlags(Flags.FLAG_NEW_AUDIO_ROUTING_UI)
    public void onSelected_setUnicast() {
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onSelected(BT_DEVICE_ADDRESS,
                CollapsibleSeekbarPreference.STATE_SELECTED);

        verify(mAudioRoutesManager).setUnicast(BT_DEVICE_ADDRESS);
    }

    @Test
    @EnableFlags(Flags.FLAG_NEW_AUDIO_ROUTING_UI)
    public void onMultiSelected_joinBroadcast() {
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onMultiSelected(BT_DEVICE_ADDRESS,
                CollapsibleSeekbarPreference.STATE_UNSELECTED);
    }

    @Test
    @EnableFlags(Flags.FLAG_NEW_AUDIO_ROUTING_UI)
    public void onPreferenceChange_setVolume() {
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
        verify(mAudioRoutesManager).setAudioRoutesUpdateListener(mListenerCaptor.capture());
        AudioRoutesManager.AudioRoutesUpdateListener capturedListener = mListenerCaptor.getValue();
        when(mAudioDeviceAttributes.getName()).thenReturn(AUDIO_DEVICE_NAME);
        when(mAudioDeviceAttributes.getAddress()).thenReturn(AUDIO_DEVICE_ADDRESS);
        AudioRouteItem audioDevice = new AudioRouteItem.Builder(mAudioDeviceAttributes)
                .setAudioZoneConfigState(new AudioRouteItem.AudioZoneConfigState.Builder()
                        .build()
                ).build();
        capturedListener.onAudioRoutesUpdated(List.of(audioDevice));
        AudioRoutePreference preference = (AudioRoutePreference) mPreference.getPreference(0);

        preference.getOnPreferenceChangeListener().onPreferenceChange(preference, 50);

        verify(mAudioRoutesManager).setVolume(AUDIO_DEVICE_ADDRESS, 50);
    }

    @Test
    @EnableFlags(Flags.FLAG_NEW_AUDIO_ROUTING_UI)
    public void onMultiSelected_leaveBroadcast() {
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onMultiSelected(BT_DEVICE_ADDRESS,
                CollapsibleSeekbarPreference.STATE_MULTI_SELECTED);

        verify(mAudioRoutesManager).leaveBroadcast(BT_DEVICE_ADDRESS);
    }
}