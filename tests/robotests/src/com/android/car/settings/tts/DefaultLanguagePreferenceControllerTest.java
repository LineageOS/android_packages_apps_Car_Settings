/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.settings.tts;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;

import androidx.lifecycle.Lifecycle;
import androidx.preference.ListPreference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.ActivityResultCallback;
import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.car.settings.testutils.ShadowSecureSettings;
import com.android.car.settings.testutils.ShadowTextToSpeech;
import com.android.car.settings.testutils.ShadowTtsEngines;

import com.google.android.collect.Lists;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Locale;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowTtsEngines.class, ShadowTextToSpeech.class, ShadowSecureSettings.class})
public class DefaultLanguagePreferenceControllerTest {

    private static final String DEFAULT_ENGINE_NAME = "com.android.car.settings.tts.test.default";
    private static final TextToSpeech.EngineInfo ENGINE_INFO = new TextToSpeech.EngineInfo();

    static {
        ENGINE_INFO.label = "Test Engine";
        ENGINE_INFO.name = "com.android.car.settings.tts.test.other";
    }

    private Context mContext;
    private PreferenceControllerTestHelper<DefaultLanguagePreferenceController>
            mControllerHelper;
    private DefaultLanguagePreferenceController mController;
    private ListPreference mListPreference;
    @Mock
    private TextToSpeech mTextToSpeech;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowTextToSpeech.setInstance(mTextToSpeech);

        mContext = RuntimeEnvironment.application;
        mListPreference = new ListPreference(mContext);
        mControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                DefaultLanguagePreferenceController.class, mListPreference);
        mController = mControllerHelper.getController();
    }

    @After
    public void tearDown() {
        ShadowTtsEngines.reset();
        ShadowTextToSpeech.reset();
    }

    @Test
    public void onCreate_startCheckVoiceData() {
        when(mTextToSpeech.getCurrentEngine()).thenReturn(ENGINE_INFO.name);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        ArgumentCaptor<Intent> intent = ArgumentCaptor.forClass(Intent.class);
        verify(mControllerHelper.getMockFragmentController()).startActivityForResult(
                intent.capture(), eq(DefaultLanguagePreferenceController.VOICE_DATA_CHECK),
                any(ActivityResultCallback.class));

        assertThat(intent.getValue().getAction()).isEqualTo(
                TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        assertThat(intent.getValue().getPackage()).isEqualTo(ENGINE_INFO.name);
    }

    @Test
    public void onCreate_initializeSucceed_preferenceEnabled() {
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        ShadowTextToSpeech.callInitializationCallbackWithStatus(TextToSpeech.SUCCESS);

        assertThat(mListPreference.isEnabled()).isTrue();
    }

    @Test
    public void onCreate_initializeFail_preferenceDisabled() {
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        ShadowTextToSpeech.callInitializationCallbackWithStatus(TextToSpeech.ERROR);

        assertThat(mListPreference.isEnabled()).isFalse();
    }

    @Test
    public void processActivityResult_dataIsNull_defaultSynthUnchanged() {
        when(mTextToSpeech.getCurrentEngine()).thenReturn(ENGINE_INFO.name);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        Settings.Secure.putString(mContext.getContentResolver(), Settings.Secure.TTS_DEFAULT_SYNTH,
                DEFAULT_ENGINE_NAME);

        Intent data = null;
        mController.processActivityResult(
                DefaultLanguagePreferenceController.VOICE_DATA_CHECK, /* resultCode= */ 0, data);

        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.TTS_DEFAULT_SYNTH)).isEqualTo(DEFAULT_ENGINE_NAME);
    }

    @Test
    public void processActivityResult_dataIsNotNull_defaultSynthUpdated() {
        when(mTextToSpeech.getCurrentEngine()).thenReturn(ENGINE_INFO.name);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        Settings.Secure.putString(mContext.getContentResolver(), Settings.Secure.TTS_DEFAULT_SYNTH,
                DEFAULT_ENGINE_NAME);

        Intent data = new Intent();
        mController.processActivityResult(
                DefaultLanguagePreferenceController.VOICE_DATA_CHECK, /* resultCode= */ 0, data);

        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.TTS_DEFAULT_SYNTH)).isEqualTo(ENGINE_INFO.name);
    }

    @Test
    public void processActivityResult_checkVoiceDataSuccess_noVoices_preferenceDisabled() {
        when(mTextToSpeech.getCurrentEngine()).thenReturn(ENGINE_INFO.name);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        Intent data = new Intent();
        data.putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES,
                Lists.newArrayList());

        mController.processActivityResult(DefaultLanguagePreferenceController.VOICE_DATA_CHECK,
                TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, data);
        assertThat(mListPreference.isEnabled()).isFalse();
    }

    @Test
    public void processActivityResult_checkVoiceDataSuccess_hasVoices_preferenceEnabled() {
        when(mTextToSpeech.getCurrentEngine()).thenReturn(ENGINE_INFO.name);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        Intent data = new Intent();
        data.putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES,
                Lists.newArrayList(
                        Locale.ENGLISH.getISO3Language(),
                        Locale.CANADA.getISO3Language(),
                        Locale.KOREA.getISO3Language()
                ));

        ShadowTextToSpeech.callInitializationCallbackWithStatus(TextToSpeech.SUCCESS);
        mController.processActivityResult(DefaultLanguagePreferenceController.VOICE_DATA_CHECK,
                TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, data);
        assertThat(mListPreference.isEnabled()).isTrue();
    }

    @Test
    public void processActivityResult_checkVoiceDataSuccess_hasVoices_preferencePopulated() {
        when(mTextToSpeech.getCurrentEngine()).thenReturn(ENGINE_INFO.name);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        // Check that the length is 0 initially.
        assertThat(mListPreference.getEntries()).isNull();

        Intent data = new Intent();
        data.putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES,
                Lists.newArrayList(
                        Locale.ENGLISH.getISO3Language(),
                        Locale.CANADA.getISO3Language(),
                        Locale.KOREA.getISO3Language()
                ));

        ShadowTextToSpeech.callInitializationCallbackWithStatus(TextToSpeech.SUCCESS);
        mController.processActivityResult(DefaultLanguagePreferenceController.VOICE_DATA_CHECK,
                TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, data);

        // Length is 3 languages + default language.
        assertThat(mListPreference.getEntries().length).isEqualTo(4);
    }

    @Test
    public void handlePreferenceChanged_passEmptyValue_defaultSet() {
        when(mTextToSpeech.getCurrentEngine()).thenReturn(ENGINE_INFO.name);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        Intent data = new Intent();
        data.putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES,
                Lists.newArrayList(
                        Locale.ENGLISH.getISO3Language(),
                        Locale.CANADA.getISO3Language(),
                        Locale.KOREA.getISO3Language()
                ));
        mController.processActivityResult(DefaultLanguagePreferenceController.VOICE_DATA_CHECK,
                TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, data);

        // Test change listener.
        mListPreference.callChangeListener("");

        verify(mTextToSpeech).setLanguage(Locale.getDefault());
    }

    @Test
    public void handlePreferenceChanged_passLocale_setLocale() {
        when(mTextToSpeech.getCurrentEngine()).thenReturn(ENGINE_INFO.name);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        Intent data = new Intent();
        data.putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES,
                Lists.newArrayList(
                        Locale.ENGLISH.getISO3Language(),
                        Locale.CANADA.getISO3Language(),
                        Locale.KOREA.getISO3Language()
                ));
        mController.processActivityResult(DefaultLanguagePreferenceController.VOICE_DATA_CHECK,
                TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, data);

        // Test change listener.
        mListPreference.callChangeListener(Locale.ENGLISH.getISO3Language());

        verify(mTextToSpeech).setLanguage(Locale.ENGLISH);
    }

    @Test
    public void handlePreferenceChanged_passLocale_setSummary() {
        when(mTextToSpeech.getCurrentEngine()).thenReturn(ENGINE_INFO.name);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        Intent data = new Intent();
        data.putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES,
                Lists.newArrayList(
                        Locale.ENGLISH.getISO3Language(),
                        Locale.CANADA.getISO3Language(),
                        Locale.KOREA.getISO3Language()
                ));
        mController.processActivityResult(DefaultLanguagePreferenceController.VOICE_DATA_CHECK,
                TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, data);
        mListPreference.callChangeListener(Locale.ENGLISH.getISO3Language());

        assertThat(mListPreference.getSummary()).isEqualTo(Locale.ENGLISH.getDisplayName());
    }
}
