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

package com.android.car.settings.suggestions;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertThrows;

import android.app.PendingIntent;
import android.content.Context;
import android.service.settings.suggestions.Suggestion;

import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;
import com.android.settingslib.suggestions.SuggestionController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Unit test for {@link SuggestionsPreferenceController}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
public class SuggestionsPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "preference_key";
    private static final Suggestion SUGGESTION_1 = new Suggestion.Builder("1").build();
    private static final Suggestion SUGGESTION_2 = new Suggestion.Builder("2").build();

    @Mock
    private LoaderManager mLoaderManager;
    @Mock
    private Loader<List<Suggestion>> mLoader;
    @Mock
    private SuggestionController mSuggestionController;
    private Context mContext;
    private PreferenceScreen mScreen;
    private PreferenceGroup mGroup;
    private SuggestionsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mGroup = new PreferenceCategory(mContext);
        mGroup.setKey(PREFERENCE_KEY);
        mScreen.addPreference(mGroup);

        mController = new SuggestionsPreferenceController(mContext, PREFERENCE_KEY,
                mock(FragmentController.class));
        mController.setLoaderManager(mLoaderManager);
        mController.mSuggestionController = mSuggestionController;
    }

    @Test
    public void checkInitialized_loaderManagerSet_doesNothing() {
        mController = new SuggestionsPreferenceController(mContext, PREFERENCE_KEY,
                mock(FragmentController.class));
        mController.setLoaderManager(mLoaderManager);

        mController.checkInitialized();
    }

    @Test
    public void checkInitialized_nullLoaderManager_throwsIllegalStateException() {
        mController = new SuggestionsPreferenceController(mContext, PREFERENCE_KEY,
                mock(FragmentController.class));

        assertThrows(IllegalStateException.class, () -> mController.checkInitialized());
    }

    @Test
    public void displayPreference_noSuggestions_hidesGroup() {
        mController.displayPreference(mScreen);

        assertThat(mGroup.isVisible()).isFalse();
    }

    @Test
    public void onStart_startsSuggestionController() {
        mController.onStart();

        verify(mSuggestionController).start();
    }

    @Test
    public void onStop_stopsSuggestionController() {
        mController.onStop();

        verify(mSuggestionController).stop();
    }

    @Test
    public void onStop_destroysLoader() {
        mController.onStop();

        verify(mLoaderManager).destroyLoader(SettingsSuggestionsLoader.LOADER_ID_SUGGESTIONS);
    }

    @Test
    public void onServiceConnected_restartsLoader() {
        mController.onServiceConnected();

        verify(mLoaderManager).restartLoader(
                SettingsSuggestionsLoader.LOADER_ID_SUGGESTIONS, /* args= */ null, mController);
    }

    @Test
    public void onServiceDisconnected_destroysLoader() {
        mController.onServiceDisconnected();

        verify(mLoaderManager).destroyLoader(SettingsSuggestionsLoader.LOADER_ID_SUGGESTIONS);
    }

    @Test
    public void onCreateLoader_returnsSettingsSuggestionsLoader() {
        assertThat(mController.onCreateLoader(
                SettingsSuggestionsLoader.LOADER_ID_SUGGESTIONS, /* args= */ null)).isInstanceOf(
                SettingsSuggestionsLoader.class);
    }

    @Test
    public void onCreateLoader_unsupportedId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> mController.onCreateLoader(
                SettingsSuggestionsLoader.LOADER_ID_SUGGESTIONS + 1000, /* args= */ null));
    }

    @Test
    public void onLoadFinished_groupContainsSuggestionPreference() {
        mController.displayPreference(mScreen);

        mController.onLoadFinished(mLoader, Collections.singletonList(SUGGESTION_1));

        assertThat(mGroup.getPreferenceCount()).isEqualTo(1);
        Preference addedPref = mGroup.getPreference(0);
        assertThat(addedPref).isInstanceOf(SuggestionPreference.class);
    }

    @Test
    public void onLoadFinished_newSuggestion_addsToGroup() {
        mController.displayPreference(mScreen);
        mController.onLoadFinished(mLoader, Collections.singletonList(SUGGESTION_1));
        assertThat(mGroup.getPreferenceCount()).isEqualTo(1);

        mController.onLoadFinished(mLoader, Arrays.asList(SUGGESTION_1, SUGGESTION_2));

        assertThat(mGroup.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    public void onLoadFinished_removedSuggestion_removesFromGroup() {
        mController.displayPreference(mScreen);
        mController.onLoadFinished(mLoader, Arrays.asList(SUGGESTION_1, SUGGESTION_2));
        assertThat(mGroup.getPreferenceCount()).isEqualTo(2);

        mController.onLoadFinished(mLoader, Collections.singletonList(SUGGESTION_2));

        assertThat(mGroup.getPreferenceCount()).isEqualTo(1);
        assertThat(((SuggestionPreference) mGroup.getPreference(0)).getSuggestion()).isEqualTo(
                SUGGESTION_2);
    }

    @Test
    public void onLoadFinished_noSuggestions_hidesGroup() {
        mController.displayPreference(mScreen);
        mController.onLoadFinished(mLoader, Collections.singletonList(SUGGESTION_1));
        assertThat(mScreen.findPreference(PREFERENCE_KEY).isVisible()).isTrue();

        mController.onLoadFinished(mLoader, Collections.emptyList());

        assertThat(mGroup.isVisible()).isFalse();
    }

    @Test
    public void launchSuggestion_sendsPendingIntent() throws PendingIntent.CanceledException {
        PendingIntent pendingIntent = mock(PendingIntent.class);
        Suggestion suggestion = new Suggestion.Builder("1").setPendingIntent(pendingIntent).build();
        SuggestionPreference preference = new SuggestionPreference(mContext,
                suggestion, /* callback= */ null);

        mController.launchSuggestion(preference);

        verify(pendingIntent).send();
    }

    @Test
    public void launchSuggestion_callsSuggestionControllerLaunch() {
        PendingIntent pendingIntent = mock(PendingIntent.class);
        Suggestion suggestion = new Suggestion.Builder("1").setPendingIntent(pendingIntent).build();
        SuggestionPreference preference = new SuggestionPreference(mContext,
                suggestion, /* callback= */ null);

        mController.launchSuggestion(preference);

        verify(mSuggestionController).launchSuggestion(suggestion);
    }

    @Test
    public void dismissSuggestion_removesSuggestion() {
        mController.displayPreference(mScreen);
        mController.onLoadFinished(mLoader, Arrays.asList(SUGGESTION_1, SUGGESTION_2));
        assertThat(mGroup.getPreferenceCount()).isEqualTo(2);
        SuggestionPreference pref = (SuggestionPreference) mGroup.getPreference(0);

        mController.dismissSuggestion(pref);

        assertThat(mGroup.getPreferenceCount()).isEqualTo(1);
        assertThat(((SuggestionPreference) mGroup.getPreference(0)).getSuggestion()).isEqualTo(
                SUGGESTION_2);
    }

    @Test
    public void dismissSuggestion_lastSuggestion_hidesGroup() {
        mController.displayPreference(mScreen);
        mController.onLoadFinished(mLoader, Collections.singletonList(SUGGESTION_1));
        SuggestionPreference pref = (SuggestionPreference) mGroup.getPreference(0);

        mController.dismissSuggestion(pref);

        assertThat(mScreen.findPreference(PREFERENCE_KEY).isVisible()).isFalse();
    }

    @Test
    public void dismissSuggestion_callsSuggestionControllerDismiss() {
        mController.displayPreference(mScreen);
        mController.onLoadFinished(mLoader, Collections.singletonList(SUGGESTION_1));
        SuggestionPreference pref = (SuggestionPreference) mGroup.getPreference(0);

        mController.dismissSuggestion(pref);

        verify(mSuggestionController).dismissSuggestions(pref.getSuggestion());
    }
}
