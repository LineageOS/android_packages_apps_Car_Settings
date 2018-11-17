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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.media.CarAudioManager;
import android.content.Context;
import android.media.Ringtone;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.SeekBarPreference;
import com.android.car.settings.testutils.ShadowCar;
import com.android.car.settings.testutils.ShadowRingtoneManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowRingtoneManager.class})
public class SoundSettingsPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "sound_settings";
    private static final int GROUP_ID = 0;
    private static final int TEST_MIN_VOLUME = 0;
    private static final int TEST_VOLUME = 40;
    private static final int TEST_NEW_VOLUME = 80;
    private static final int TEST_MAX_VOLUME = 100;

    private Context mContext;
    private TestSoundSettingsPreferenceController mController;
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private CarAudioManager mCarAudioManager;
    @Mock
    private Ringtone mRingtone;

    /** Extend class to provide test resource which doesn't require internal android resources. */
    public static class TestSoundSettingsPreferenceController extends
            SoundSettingsPreferenceController {

        public TestSoundSettingsPreferenceController(Context context, String preferenceKey,
                FragmentController fragmentController) {
            super(context, preferenceKey, fragmentController);
        }

        @Override
        public int carVolumeItemsXml() {
            return R.xml.test_car_volume_items;
        }
    }

    @Before
    public void setUp() throws CarNotConnectedException {
        MockitoAnnotations.initMocks(this);
        ShadowCar.setCarManager(Car.AUDIO_SERVICE, mCarAudioManager);
        ShadowRingtoneManager.setRingtone(mRingtone);

        mContext = RuntimeEnvironment.application;
        mController = new TestSoundSettingsPreferenceController(mContext, PREFERENCE_KEY,
                mock(FragmentController.class));
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreferenceScreen.setKey(PREFERENCE_KEY);
        when(mCarAudioManager.getVolumeGroupCount()).thenReturn(1);
        when(mCarAudioManager.getUsagesForVolumeGroupId(GROUP_ID)).thenReturn(new int[]{1, 2});
        when(mCarAudioManager.getGroupMinVolume(GROUP_ID)).thenReturn(TEST_MIN_VOLUME);
        when(mCarAudioManager.getGroupVolume(GROUP_ID)).thenReturn(TEST_VOLUME);
        when(mCarAudioManager.getGroupMaxVolume(GROUP_ID)).thenReturn(TEST_MAX_VOLUME);
    }

    @After
    public void tearDown() {
        ShadowCar.reset();
        ShadowRingtoneManager.reset();
    }

    @Test
    public void testDisplayPreference_serviceNotStarted() {
        mController.displayPreference(mPreferenceScreen);
        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void testDisplayPreference_serviceStarted() {
        mController.displayPreference(mPreferenceScreen);
        mController.onCreate();
        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void testDisplayPreference_serviceStarted_multipleCalls() {
        mController.displayPreference(mPreferenceScreen);
        mController.onCreate();

        // Calling this multiple times shouldn't increase the number of elements.
        mController.displayPreference(mPreferenceScreen);
        mController.displayPreference(mPreferenceScreen);
        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void testDisplayPreference_createdPreferenceHasMinMax() {
        mController.displayPreference(mPreferenceScreen);
        mController.onCreate();
        SeekBarPreference preference = (SeekBarPreference) mPreferenceScreen.getPreference(0);
        assertThat(preference.getMin()).isEqualTo(TEST_MIN_VOLUME);
        assertThat(preference.getValue()).isEqualTo(TEST_VOLUME);
        assertThat(preference.getMax()).isEqualTo(TEST_MAX_VOLUME);
    }

    @Test
    public void testOnPreferenceClick_ringtonePlays() {
        mController.displayPreference(mPreferenceScreen);
        mController.onCreate();
        SeekBarPreference preference = (SeekBarPreference) mPreferenceScreen.getPreference(0);
        preference.getOnPreferenceChangeListener().onPreferenceChange(preference, TEST_NEW_VOLUME);
        verify(mRingtone).play();
    }

    @Test
    public void testOnPreferenceClick_audioManagerSet() throws CarNotConnectedException {
        mController.displayPreference(mPreferenceScreen);
        mController.onCreate();
        SeekBarPreference preference = (SeekBarPreference) mPreferenceScreen.getPreference(0);
        preference.getOnPreferenceChangeListener().onPreferenceChange(preference, TEST_NEW_VOLUME);
        verify(mCarAudioManager).setGroupVolume(GROUP_ID, TEST_NEW_VOLUME, 0);
    }
}
