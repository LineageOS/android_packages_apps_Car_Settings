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

import android.car.drivingstate.CarUxRestrictions;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TtsEngines;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;

import com.android.car.settings.R;
import com.android.car.settings.common.ActivityResultCallback;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.PreferenceController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/** Business logic for selecting the default language for the tts engine. */
public class DefaultLanguagePreferenceController extends
        PreferenceController<ListPreference> implements ActivityResultCallback {

    private static final Logger LOG = new Logger(DefaultLanguagePreferenceController.class);

    @VisibleForTesting
    static final int VOICE_DATA_CHECK = 1;

    private final TtsEngines mEnginesHelper;
    private TextToSpeech mTts;
    private int mSelectedLocaleIndex;

    /** True if initialized with no errors. */
    private boolean mTtsInitialized = false;

    private final TextToSpeech.OnInitListener mOnInitListener = status -> {
        if (status == TextToSpeech.SUCCESS) {
            mTtsInitialized = true;
            refreshUi();
        }
    };

    public DefaultLanguagePreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mEnginesHelper = new TtsEngines(context);
    }

    @Override
    protected Class<ListPreference> getPreferenceType() {
        return ListPreference.class;
    }

    @Override
    protected void onCreateInternal() {
        initTts();
    }

    @Override
    protected void onDestroyInternal() {
        shutdownTts();
    }

    @Override
    protected void updateState(ListPreference preference) {
        preference.setEnabled(mTtsInitialized);
        if (preference.getEntries() != null) {
            preference.setValueIndex(mSelectedLocaleIndex);
            preference.setSummary(preference.getEntries()[mSelectedLocaleIndex]);
        }
    }

    @Override
    protected boolean handlePreferenceChanged(ListPreference preference, Object newValue) {
        String localeString = (String) newValue;
        updateLanguageTo(
                !TextUtils.isEmpty(localeString) ? mEnginesHelper.parseLocaleString(localeString)
                        : null);
        return true;
    }

    @Override
    public void processActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == VOICE_DATA_CHECK) {
            onVoiceDataIntegrityCheckDone(data);
            if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL) {
                updateDefaultLocalePref(data);
            }
        }
    }

    private void initTts() {
        mTts = new TextToSpeech(getContext(), mOnInitListener);
        startEngineVoiceDataCheckActivity(mTts.getCurrentEngine());
    }

    private void shutdownTts() {
        if (mTts != null) {
            mTts.shutdown();
            mTts = null;
        }
    }

    private void startEngineVoiceDataCheckActivity(String engine) {
        Intent intent = new Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        intent.setPackage(engine);
        try {
            LOG.d("Updating engine: Checking voice data: " + intent.toUri(0));
            getFragmentController().startActivityForResult(intent, VOICE_DATA_CHECK,
                    this);
        } catch (ActivityNotFoundException ex) {
            LOG.e("Failed to check TTS data, no activity found for " + intent);
        }
    }

    /** The voice data check is complete. */
    private void onVoiceDataIntegrityCheckDone(Intent data) {
        String engine = mTts.getCurrentEngine();

        if (engine == null) {
            LOG.e("Voice data check complete, but no engine bound");
            return;
        }

        if (data == null) {
            LOG.e("Engine failed voice data integrity check (null return)"
                    + mTts.getCurrentEngine());
            return;
        }

        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.TTS_DEFAULT_SYNTH, engine);
    }

    private void updateDefaultLocalePref(Intent data) {
        ArrayList<String> availableLangs =
                data.getStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES);
        if (availableLangs == null || availableLangs.size() == 0) {
            getPreference().setEnabled(false);
            return;
        }

        // Sort locales by display name.
        ArrayList<Pair<String, Locale>> entryPairs = new ArrayList<>();
        for (int i = 0; i < availableLangs.size(); i++) {
            Locale locale = mEnginesHelper.parseLocaleString(availableLangs.get(i));
            if (locale != null) {
                entryPairs.add(new Pair<>(locale.getDisplayName(), locale));
            }
        }
        Collections.sort(entryPairs, (lhs, rhs) -> lhs.first.compareToIgnoreCase(rhs.first));

        // Separate pairs into two separate arrays.
        mSelectedLocaleIndex = 0;
        CharSequence[] entries = new CharSequence[availableLangs.size() + 1];
        CharSequence[] entryValues = new CharSequence[availableLangs.size() + 1];

        entries[0] = getContext().getString(R.string.tts_lang_use_system);
        entryValues[0] = "";

        // If current locale is selected, get the locale in order to find the locale index.
        Locale currentLocale = null;
        if (!mEnginesHelper.isLocaleSetToDefaultForEngine(mTts.getCurrentEngine())) {
            currentLocale = mEnginesHelper.getLocalePrefForEngine(mTts.getCurrentEngine());
        }

        int i = 1;
        for (Pair<String, Locale> entry : entryPairs) {
            if (entry.second.equals(currentLocale)) {
                mSelectedLocaleIndex = i;
            }
            entries[i] = entry.first;
            entryValues[i++] = entry.second.toString();
        }

        getPreference().setEntries(entries);
        getPreference().setEntryValues(entryValues);
        getPreference().setEnabled(true);
        refreshUi();
    }

    private void updateLanguageTo(Locale locale) {
        int selectedLocaleIndex = -1;
        String localeString = (locale != null) ? locale.toString() : "";
        for (int i = 0; i < getPreference().getEntryValues().length; i++) {
            if (localeString.equalsIgnoreCase(getPreference().getEntryValues()[i].toString())) {
                selectedLocaleIndex = i;
                break;
            }
        }

        if (selectedLocaleIndex == -1) {
            LOG.w("updateLanguageTo called with unknown locale argument");
            return;
        }
        mSelectedLocaleIndex = selectedLocaleIndex;
        mEnginesHelper.updateLocalePrefForEngine(mTts.getCurrentEngine(), locale);
        mTts.setLanguage((locale != null) ? locale : Locale.getDefault());
        refreshUi();
    }
}
