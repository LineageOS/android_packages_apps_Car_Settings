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

package com.android.car.settings.security;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.userlib.CarUserManagerHelper;
import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.testutils.ShadowCarUserManagerHelper;
import com.android.car.settings.testutils.ShadowLockPatternUtils;
import com.android.internal.widget.LockPatternUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowCarUserManagerHelper.class, ShadowLockPatternUtils.class})
public class NoLockPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "no_lock";
    private static final String TEST_CURRENT_PASSWORD = "test_password";
    private static final int TEST_USER = 10;

    private Context mContext;
    private NoLockPreferenceController mController;
    @Mock
    private FragmentController mFragmentController;
    @Mock
    private CarUserManagerHelper mCarUserManagerHelper;
    @Mock
    private LockPatternUtils mLockPatternUtils;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowCarUserManagerHelper.setMockInstance(mCarUserManagerHelper);
        ShadowLockPatternUtils.setInstance(mLockPatternUtils);
        mContext = RuntimeEnvironment.application;
        mController = new NoLockPreferenceController(mContext, PREFERENCE_KEY,
                mFragmentController);
    }

    @After
    public void tearDown() {
        ShadowCarUserManagerHelper.reset();
        ShadowLockPatternUtils.reset();
    }

    @Test
    public void testHandlePreferenceTreeClick_wrongPreference() {
        // Do setup, so controller has a reference to compare the incoming preference.
        PreferenceScreen preferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(
                mContext);
        Preference preference = new Preference(mContext);
        preference.setKey(PREFERENCE_KEY);
        preferenceScreen.addPreference(preference);
        mController.displayPreference(preferenceScreen);

        assertThat(mController.handlePreferenceTreeClick(new Preference(mContext))).isFalse();
    }

    @Test
    public void testHandlePreferenceTreeClick_nextFragment() {
        PreferenceScreen preferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(
                mContext);
        Preference preference = new Preference(mContext);
        preference.setKey(PREFERENCE_KEY);
        preferenceScreen.addPreference(preference);
        mController.displayPreference(preferenceScreen);

        assertThat(mController.handlePreferenceTreeClick(preference)).isTrue();
        verify(mFragmentController).showDialog(any(ConfirmRemoveScreenLockDialog.class),
                anyString());
    }

    @Test
    public void testConfirmRemoveScreenLockListener_removeLock() {
        when(mCarUserManagerHelper.getCurrentProcessUserId()).thenReturn(TEST_USER);
        mController.setCurrentPassword(TEST_CURRENT_PASSWORD);
        mController.mRemoveLockListener.onConfirmRemoveScreenLock();
        verify(mLockPatternUtils).clearLock(TEST_CURRENT_PASSWORD, TEST_USER);
    }

    @Test
    public void testConfirmRemoveScreenLockListener_goBack() {
        when(mCarUserManagerHelper.getCurrentProcessUserId()).thenReturn(TEST_USER);
        mController.setCurrentPassword(TEST_CURRENT_PASSWORD);
        mController.mRemoveLockListener.onConfirmRemoveScreenLock();
        verify(mFragmentController).goBack();
    }
}
