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
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.os.Bundle;
import android.service.settings.suggestions.Suggestion;

import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.testutils.FragmentController;
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
    private PreferenceScreen mScreen;
    private Context mContext;
    private SuggestionsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        FragmentController<TestPreferenceFragment> fragmentController = FragmentController.of(
                new TestPreferenceFragment());
        mScreen = fragmentController.setup().getPreferenceScreen();

        mContext = RuntimeEnvironment.application;
        mController = new SuggestionsPreferenceController(mContext, PREFERENCE_KEY);
        mController.setLoaderManager(mLoaderManager);
        mController.mSuggestionController = mSuggestionController;
    }

    @Test
    public void displayPreference_removesPlaceholder() {
        Preference placeholder = new Preference(mContext);
        placeholder.setKey(PREFERENCE_KEY);
        mScreen.addPreference(placeholder);

        mController.displayPreference(mScreen);

        assertThat(mScreen.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void displayPreference_injectsSuggestionsAtPlaceholder() {
        // Add suggestion so that group is not removed for having 0 suggestions.
        mController.onLoadFinished(mLoader, Collections.singletonList(SUGGESTION_1));
        Preference placeholder = new Preference(mContext);
        placeholder.setKey(PREFERENCE_KEY);
        placeholder.setOrder(-1000);
        mScreen.addPreference(placeholder);

        mController.displayPreference(mScreen);

        assertThat(mScreen.getPreferenceCount()).isEqualTo(1);
        Preference injectedPref = mScreen.getPreference(0);
        assertThat(injectedPref).isInstanceOf(PreferenceGroup.class);
        assertThat(injectedPref.getOrder()).isEqualTo(placeholder.getOrder());
    }

    @Test
    public void displayPreference_availabilityChange_addsAndRemovesGroup() {
        CarUxRestrictions restrictionInfo = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP, /* timestamp= */ 0).build();
        // Add suggestion so that group is not removed for having 0 suggestions.
        mController.onLoadFinished(mLoader, Collections.singletonList(SUGGESTION_1));
        addPlaceholderAndDisplayPreference();

        // Group is added to screen.
        assertThat(mScreen.getPreferenceCount()).isEqualTo(1);
        Preference injectedPref = mScreen.getPreference(0);
        assertThat(injectedPref).isInstanceOf(PreferenceGroup.class);

        // Controller becomes unavailable.
        mController.onUxRestrictionsChanged(restrictionInfo);
        mController.displayPreference(mScreen);

        // Group is removed from screen.
        assertThat(mScreen.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void checkInitialized_loaderManagerSet_doesNothing() {
        mController = new SuggestionsPreferenceController(mContext, PREFERENCE_KEY);
        mController.setLoaderManager(mLoaderManager);

        mController.checkInitialized();
    }

    @Test
    public void checkInitialized_nullLoaderManager_throwsIllegalStateException() {
        mController = new SuggestionsPreferenceController(mContext, PREFERENCE_KEY);

        assertThrows(IllegalStateException.class, () -> mController.checkInitialized());
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
    public void onLoadFinished_firstSuggestion_addGroup() {
        addPlaceholderAndDisplayPreference();

        mController.onLoadFinished(mLoader, Collections.singletonList(SUGGESTION_1));

        assertThat(mScreen.getPreferenceCount()).isEqualTo(1);
        Preference group = mScreen.getPreference(0);
        assertThat(group).isInstanceOf(PreferenceGroup.class);
    }

    @Test
    public void onLoadFinished_groupContainsSuggestionPreference() {
        addPlaceholderAndDisplayPreference();

        mController.onLoadFinished(mLoader, Collections.singletonList(SUGGESTION_1));

        PreferenceGroup group = ((PreferenceGroup) mScreen.getPreference(0));
        assertThat(group.getPreferenceCount()).isEqualTo(1);
        Preference addedPref = group.getPreference(0);
        assertThat(addedPref).isInstanceOf(SuggestionPreference.class);
    }

    @Test
    public void onLoadFinished_newSuggestion_addToGroup() {
        addPlaceholderAndDisplayPreference();
        mController.onLoadFinished(mLoader, Collections.singletonList(SUGGESTION_1));
        PreferenceGroup group = ((PreferenceGroup) mScreen.getPreference(0));
        assertThat(group.getPreferenceCount()).isEqualTo(1);

        mController.onLoadFinished(mLoader, Arrays.asList(SUGGESTION_1, SUGGESTION_2));

        assertThat(group.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    public void onLoadFinished_removedSuggestion_removeFromGroup() {
        addPlaceholderAndDisplayPreference();
        mController.onLoadFinished(mLoader, Arrays.asList(SUGGESTION_1, SUGGESTION_2));
        PreferenceGroup group = ((PreferenceGroup) mScreen.getPreference(0));
        assertThat(group.getPreferenceCount()).isEqualTo(2);

        mController.onLoadFinished(mLoader, Collections.singletonList(SUGGESTION_2));

        assertThat(group.getPreferenceCount()).isEqualTo(1);
        assertThat(((SuggestionPreference) group.getPreference(0)).getSuggestion()).isEqualTo(
                SUGGESTION_2);
    }

    @Test
    public void onLoadFinished_noSuggestions_removesGroup() {
        addPlaceholderAndDisplayPreference();
        mController.onLoadFinished(mLoader, Collections.singletonList(SUGGESTION_1));
        assertThat(mScreen.getPreferenceCount()).isEqualTo(1);

        mController.onLoadFinished(mLoader, Collections.emptyList());

        assertThat(mScreen.getPreferenceCount()).isEqualTo(0);
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
        addPlaceholderAndDisplayPreference();
        mController.onLoadFinished(mLoader, Arrays.asList(SUGGESTION_1, SUGGESTION_2));
        PreferenceGroup group = ((PreferenceGroup) mScreen.getPreference(0));
        assertThat(group.getPreferenceCount()).isEqualTo(2);
        SuggestionPreference pref = (SuggestionPreference) group.getPreference(0);

        mController.dismissSuggestion(pref);

        assertThat(group.getPreferenceCount()).isEqualTo(1);
        assertThat(((SuggestionPreference) group.getPreference(0)).getSuggestion()).isEqualTo(
                SUGGESTION_2);
    }

    @Test
    public void dismissSuggestion_lastSuggestion_removesGroup() {
        addPlaceholderAndDisplayPreference();
        mController.onLoadFinished(mLoader, Collections.singletonList(SUGGESTION_1));
        PreferenceGroup group = ((PreferenceGroup) mScreen.getPreference(0));
        SuggestionPreference pref = (SuggestionPreference) group.getPreference(0);

        mController.dismissSuggestion(pref);

        assertThat(mScreen.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void dismissSuggestion_callsSuggestionControllerDismiss() {
        addPlaceholderAndDisplayPreference();
        mController.onLoadFinished(mLoader, Collections.singletonList(SUGGESTION_1));
        PreferenceGroup group = ((PreferenceGroup) mScreen.getPreference(0));
        SuggestionPreference pref = (SuggestionPreference) group.getPreference(0);

        mController.dismissSuggestion(pref);

        verify(mSuggestionController).dismissSuggestions(pref.getSuggestion());
    }

    private void addPlaceholderAndDisplayPreference() {
        Preference placeholder = new Preference(mContext);
        placeholder.setKey(PREFERENCE_KEY);
        mScreen.addPreference(placeholder);
        mController.displayPreference(mScreen);
    }

    /**
     * Preference fragment which is initialized with an empty {@link PreferenceScreen} when created.
     */
    public static class TestPreferenceFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getContext()));
        }
    }

}
