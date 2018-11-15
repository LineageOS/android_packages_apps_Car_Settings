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

package com.android.car.settings.language;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.settings.testutils.ShadowLocalePicker;
import com.android.car.settings.testutils.ShadowLocaleStore;
import com.android.internal.app.LocaleStore;
import com.android.internal.app.SuggestedLocaleAdapter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Locale;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowLocalePicker.class, ShadowLocaleStore.class})
public class LanguageBasePreferenceControllerTest {

    private static final String PREFERENCE_KEY = "test_preference_key";
    private static final LocaleStore.LocaleInfo TEST_LOCALE_INFO = LocaleStore.getLocaleInfo(
            Locale.FRENCH);
    private static final Locale HAS_MULTIPLE_CHILD_LOCALE = Locale.ENGLISH;
    private static final Locale HAS_CHILD_LOCALE = Locale.KOREAN;
    private static final Locale NO_CHILD_LOCALE = Locale.FRANCE;

    private static class TestLanguageBasePreferenceController extends
            LanguageBasePreferenceController {

        private final SuggestedLocaleAdapter mAdapter;

        TestLanguageBasePreferenceController(Context context,
                String preferenceKey, FragmentController fragmentController,
                SuggestedLocaleAdapter adapter) {
            super(context, preferenceKey, fragmentController);
            mAdapter = adapter;
        }

        @Override
        protected LocalePreferenceProvider defineLocaleProvider() {
            return new LocalePreferenceProvider(mContext, mAdapter);
        }
    }

    private TestLanguageBasePreferenceController mController;
    private Context mContext;
    @Mock
    private FragmentController mFragmentController;
    @Mock
    private SuggestedLocaleAdapter mSuggestedLocaleAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new TestLanguageBasePreferenceController(mContext, PREFERENCE_KEY,
                mFragmentController, mSuggestedLocaleAdapter);

        // Note that ENGLISH has 2 child locales.
        ShadowLocaleStore.addLocaleRelationship(Locale.ENGLISH, Locale.CANADA);
        ShadowLocaleStore.addLocaleRelationship(Locale.ENGLISH, Locale.US);

        // Note that KOREAN has 1 child locale.
        ShadowLocaleStore.addLocaleRelationship(Locale.KOREAN, Locale.KOREA);
    }

    @After
    public void tearDown() {
        ShadowLocaleStore.reset();
        ShadowLocalePicker.reset();
    }

    @Test
    public void testDisplayPreference_screenConstructed() {
        when(mSuggestedLocaleAdapter.getCount()).thenReturn(1);
        when(mSuggestedLocaleAdapter.getItemViewType(0)).thenReturn(
                LocalePreferenceProvider.TYPE_LOCALE);
        when(mSuggestedLocaleAdapter.getItem(0)).thenReturn(TEST_LOCALE_INFO);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        LogicalPreferenceGroup group = new LogicalPreferenceGroup(mContext);
        group.setKey(PREFERENCE_KEY);
        screen.addPreference(group);

        mController.displayPreference(screen);
        assertThat(group.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void testDisplayPreference_screenConstructed_onlyOnce() {
        when(mSuggestedLocaleAdapter.getCount()).thenReturn(1);
        when(mSuggestedLocaleAdapter.getItemViewType(0)).thenReturn(
                LocalePreferenceProvider.TYPE_LOCALE);
        when(mSuggestedLocaleAdapter.getItem(0)).thenReturn(TEST_LOCALE_INFO);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        LogicalPreferenceGroup group = new LogicalPreferenceGroup(mContext);
        group.setKey(PREFERENCE_KEY);
        screen.addPreference(group);
        mController.displayPreference(screen);
        assertThat(group.getPreferenceCount()).isEqualTo(1);

        // Calling displayPreference again shouldn't reconstruct the screen.
        mController.displayPreference(screen);
        assertThat(group.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void testHandlePreferenceClick_wrongPreference_returnsFalse() {
        assertThat(mController.handlePreferenceTreeClick(new Preference(mContext))).isFalse();
    }

    @Test
    public void testHandlePreferenceClick_hasMultipleChildLocales_returnsTrue() {
        LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(HAS_MULTIPLE_CHILD_LOCALE);
        Preference preference = new Preference(mContext);
        LocaleUtil.setLocaleArgument(preference, localeInfo);
        assertThat(mController.handlePreferenceTreeClick(preference)).isTrue();
    }

    @Test
    public void testHandlePreferenceClick_hasMultipleChildLocales_localeNotUpdated() {
        LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(HAS_MULTIPLE_CHILD_LOCALE);
        Preference preference = new Preference(mContext);
        LocaleUtil.setLocaleArgument(preference, localeInfo);
        mController.handlePreferenceTreeClick(preference);
        assertThat(ShadowLocalePicker.localeWasUpdated()).isFalse();
    }

    @Test
    public void testHandlePreferenceClick_hasMultipleChildLocales_neverCallsGoBack() {
        LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(HAS_MULTIPLE_CHILD_LOCALE);
        Preference preference = new Preference(mContext);
        LocaleUtil.setLocaleArgument(preference, localeInfo);
        mController.handlePreferenceTreeClick(preference);
        verify(mFragmentController, never()).goBack();
    }

    @Test
    public void testHandlePreferenceClick_hasSingleChildLocale_returnsTrue() {
        LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(HAS_CHILD_LOCALE);
        Preference preference = new Preference(mContext);
        LocaleUtil.setLocaleArgument(preference, localeInfo);
        assertThat(mController.handlePreferenceTreeClick(preference)).isTrue();
    }

    @Test
    public void testHandlePreferenceClick_hasSingleChildLocale_localeUpdated() {
        LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(HAS_CHILD_LOCALE);
        Preference preference = new Preference(mContext);
        LocaleUtil.setLocaleArgument(preference, localeInfo);
        mController.handlePreferenceTreeClick(preference);
        assertThat(ShadowLocalePicker.localeWasUpdated()).isTrue();
    }

    @Test
    public void testHandlePreferenceClick_hasSingleChildLocale_callsGoBack() {
        LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(HAS_CHILD_LOCALE);
        Preference preference = new Preference(mContext);
        LocaleUtil.setLocaleArgument(preference, localeInfo);
        mController.handlePreferenceTreeClick(preference);
        verify(mFragmentController).goBack();
    }

    @Test
    public void testHandlePreferenceClick_noChildLocale_returnsTrue() {
        LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(NO_CHILD_LOCALE);
        Preference preference = new Preference(mContext);
        LocaleUtil.setLocaleArgument(preference, localeInfo);
        assertThat(mController.handlePreferenceTreeClick(preference)).isTrue();
    }

    @Test
    public void testHandlePreferenceClick_noChildLocale_localeUpdated() {
        LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(NO_CHILD_LOCALE);
        Preference preference = new Preference(mContext);
        LocaleUtil.setLocaleArgument(preference, localeInfo);
        mController.handlePreferenceTreeClick(preference);
        assertThat(ShadowLocalePicker.localeWasUpdated()).isTrue();
    }

    @Test
    public void testHandlePreferenceClick_noChildLocale_callsGoBack() {
        LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(NO_CHILD_LOCALE);
        Preference preference = new Preference(mContext);
        LocaleUtil.setLocaleArgument(preference, localeInfo);
        mController.handlePreferenceTreeClick(preference);
        verify(mFragmentController).goBack();
    }
}
