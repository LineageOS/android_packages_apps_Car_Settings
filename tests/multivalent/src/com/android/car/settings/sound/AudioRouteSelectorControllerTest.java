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

import static android.car.media.CarAudioManager.AUDIO_FEATURE_DYNAMIC_ROUTING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.car.media.CarAudioManager;
import android.content.Context;
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
import org.mockito.Mock;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class AudioRouteSelectorControllerTest {
    private static final String ACTIVE_ADDRESS = "active_address";
    private static final String ACTIVE_NAME = "active_name";
    private static final String INACTIVE_NAME = "inactive_name";
    private static final String INACTIVE_ADDRESS = "inactive_address";

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
    private CarAudioManager mCarAudioManager;
    @Mock
    private AudioRouteItem mAudioRouteItem1;
    @Mock
    private AudioRouteItem mAudioRouteItem2;
    @Mock
    private Toast mMockToast;

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
        when(mAudioRoutesManager.getCarAudioManager()).thenReturn(mCarAudioManager);
        when(mCarAudioManager.isAudioFeatureEnabled(AUDIO_FEATURE_DYNAMIC_ROUTING))
                .thenReturn(true);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);

        when(mAudioRouteItem1.getAddress()).thenReturn(ACTIVE_ADDRESS);
        when(mAudioRouteItem1.getName()).thenReturn(ACTIVE_NAME);
        when(mAudioRouteItem2.getAddress()).thenReturn(INACTIVE_ADDRESS);
        when(mAudioRouteItem2.getName()).thenReturn(INACTIVE_NAME);
        when(mAudioRoutesManager.isAudioRoutingEnabled()).thenReturn(true);
        when(mAudioRoutesManager.getOutputAddress()).thenReturn(ACTIVE_ADDRESS);
        when(mAudioRoutesManager.getAudioRouteList()).thenReturn(
                Arrays.asList(ACTIVE_ADDRESS, INACTIVE_ADDRESS));
        when(mAudioRoutesManager.getDeviceName(ACTIVE_ADDRESS)).thenReturn(ACTIVE_NAME);
        when(mAudioRoutesManager.getDeviceName(INACTIVE_ADDRESS))
                .thenReturn(INACTIVE_NAME);
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
        when(mAudioRoutesManager.getRouteItem(ACTIVE_ADDRESS)).thenReturn(mAudioRouteItem1);
        when(mAudioRoutesManager.getRouteItem(INACTIVE_ADDRESS)).thenReturn(mAudioRouteItem2);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        assertThat(mPreference.getPreferenceCount()).isEqualTo(2);

        CollapsibleSeekbarPreference activePreference =
                (CollapsibleSeekbarPreference) mPreference.getPreference(0);
        assertThat(activePreference.getTitle().toString()).isEqualTo(ACTIVE_NAME);
        assertThat(activePreference.getKey()).isEqualTo(ACTIVE_ADDRESS);

        CollapsibleSeekbarPreference inactivePreference =
                (CollapsibleSeekbarPreference) mPreference.getPreference(1);
        assertThat(inactivePreference.getTitle().toString()).isEqualTo(INACTIVE_NAME);
        assertThat(inactivePreference.getKey()).isEqualTo(INACTIVE_ADDRESS);
    }
}
