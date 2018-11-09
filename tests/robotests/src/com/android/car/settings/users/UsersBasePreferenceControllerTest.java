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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
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

import java.util.Arrays;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowCarUserManagerHelper.class, ShadowUserIconProvider.class})
public class UsersBasePreferenceControllerTest {

    private static class TestUsersBasePreferenceController extends UsersBasePreferenceController {

        TestUsersBasePreferenceController(Context context, String preferenceKey,
                FragmentController fragmentController) {
            super(context, preferenceKey, fragmentController);
        }

        @Override
        protected void userClicked(UserInfo userInfo) {
            return;
        }
    }

    private static final String PREFERENCE_KEY = "test_preference";
    private static final UserInfo TEST_CURRENT_USER = new UserInfo(/* id= */ 10,
            "TEST_USER_NAME", /* flags= */ 0);
    private static final UserInfo TEST_OTHER_USER = new UserInfo(/* id= */ 11,
            "TEST_OTHER_NAME", /* flags= */ 0);
    private TestUsersBasePreferenceController mController;
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
        mController = new TestUsersBasePreferenceController(context, PREFERENCE_KEY,
                mFragmentController);
        mPreferenceScreen = new PreferenceManager(context).createPreferenceScreen(context);
        mPreferenceScreen.setKey(PREFERENCE_KEY);
    }

    @After
    public void tearDown() {
        ShadowCarUserManagerHelper.reset();
    }

    @Test
    public void testOnCreate_registerOnUsersUpdateListener() {
        mController.onCreate();
        verify(mCarUserManagerHelper).registerOnUsersUpdateListener(
                any(CarUserManagerHelper.OnUsersUpdateListener.class));
    }

    @Test
    public void testOnCreate_unregisterOnUsersUpdateListener() {
        mController.onDestroy();
        verify(mCarUserManagerHelper).unregisterOnUsersUpdateListener(
                any(CarUserManagerHelper.OnUsersUpdateListener.class));
    }

    @Test
    public void testDisplayPreference_populateScreen() {
        when(mCarUserManagerHelper.getCurrentProcessUserInfo()).thenReturn(TEST_CURRENT_USER);
        when(mCarUserManagerHelper.isCurrentProcessUser(TEST_CURRENT_USER)).thenReturn(true);
        when(mCarUserManagerHelper.getAllSwitchableUsers()).thenReturn(
                Arrays.asList(TEST_OTHER_USER));

        mController.displayPreference(mPreferenceScreen);

        // Three users. Current user, other user, guest user.
        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(3);
    }
}
