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

package com.android.car.settings.users;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.content.pm.UserInfo;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.testutils.ShadowCarUserManagerHelper;
import com.android.car.settings.testutils.ShadowUserIconProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowCarUserManagerHelper.class, ShadowUserIconProvider.class})
public class UsersListPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "users_list";
    private static final UserInfo TEST_CURRENT_USER = new UserInfo(/* id= */ 10,
            "TEST_USER_NAME", /* flags= */ 0);
    private static final UserInfo TEST_OTHER_USER = new UserInfo(/* id= */ 11,
            "TEST_OTHER_NAME", /* flags= */ 0);
    private UsersListPreferenceController mController;
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private CarUserManagerHelper mCarUserManagerHelper;
    @Mock
    private FragmentController mFragmentController;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.application;
        MockitoAnnotations.initMocks(this);
        ShadowCarUserManagerHelper.setMockInstance(mCarUserManagerHelper);
        mController = new UsersListPreferenceController(context, PREFERENCE_KEY,
                mFragmentController);
        mPreferenceScreen = new PreferenceManager(context).createPreferenceScreen(context);
        mPreferenceScreen.setKey(PREFERENCE_KEY);

        when(mCarUserManagerHelper.getCurrentProcessUserInfo()).thenReturn(TEST_CURRENT_USER);
        when(mCarUserManagerHelper.isCurrentProcessUser(TEST_CURRENT_USER)).thenReturn(true);
        when(mCarUserManagerHelper.getAllSwitchableUsers()).thenReturn(
                Collections.singletonList(TEST_OTHER_USER));
    }

    @After
    public void tearDown() {
        ShadowCarUserManagerHelper.reset();
    }

    @Test
    public void testPreferencePerformClick_currentUser_openNewFragment() {
        mController.displayPreference(mPreferenceScreen);

        mPreferenceScreen.getPreference(0).performClick();

        verify(mFragmentController).launchFragment(any(UserDetailsFragment.class));
    }

    @Test
    public void testPreferencePerformClick_otherUser_openNewFragment() {
        mController.displayPreference(mPreferenceScreen);

        mPreferenceScreen.getPreference(1).performClick();

        verify(mFragmentController).launchFragment(any(UserDetailsFragment.class));
    }

    @Test
    public void testPreferencePerformClick_guestUser_noAction() {
        mController.displayPreference(mPreferenceScreen);

        mPreferenceScreen.getPreference(2).performClick();

        verify(mFragmentController, never()).launchFragment(any());
    }
}
