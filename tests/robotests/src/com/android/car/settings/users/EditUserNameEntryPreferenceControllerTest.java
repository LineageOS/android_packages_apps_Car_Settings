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

import static android.content.pm.UserInfo.FLAG_ADMIN;
import static android.content.pm.UserInfo.FLAG_INITIALIZED;

import static com.google.common.truth.Truth.assertThat;

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
import com.android.car.settings.R;
import com.android.car.settings.common.ButtonPreference;
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

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowCarUserManagerHelper.class, ShadowUserIconProvider.class})
public class EditUserNameEntryPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "edit_user_name_entry";
    private static final String TEST_USERNAME = "Test Username";

    private Context mContext;
    private EditUserNameEntryPreferenceController mController;
    private PreferenceScreen mPreferenceScreen;
    private ButtonPreference mButtonPreference;
    @Mock
    private CarUserManagerHelper mCarUserManagerHelper;
    @Mock
    private FragmentController mFragmentController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        MockitoAnnotations.initMocks(this);
        ShadowCarUserManagerHelper.setMockInstance(mCarUserManagerHelper);
        mController = new EditUserNameEntryPreferenceController(mContext, PREFERENCE_KEY,
                mFragmentController);
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mButtonPreference = new ButtonPreference(mContext);
        mButtonPreference.setSelectable(false);
        mButtonPreference.setKey(PREFERENCE_KEY);
        mPreferenceScreen.addPreference(mButtonPreference);
    }

    @After
    public void tearDown() {
        ShadowCarUserManagerHelper.reset();
    }

    @Test
    public void testOnCreate_registerListener() {
        mController.onCreate();
        verify(mCarUserManagerHelper).registerOnUsersUpdateListener(any(CarUserManagerHelper
                .OnUsersUpdateListener.class));
    }

    @Test
    public void testOnDestroy_unregisterListener() {
        mController.onDestroy();
        verify(mCarUserManagerHelper).unregisterOnUsersUpdateListener(any(CarUserManagerHelper
                .OnUsersUpdateListener.class));
    }

    @Test
    public void testDisplayPreference_hasOneElement() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, /* flags= */ 0);
        mController.setUserInfo(userInfo);
        mController.displayPreference(mPreferenceScreen);
        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void testDisplayPreference_elementHasTitle() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, /* flags= */ 0);
        when(mCarUserManagerHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        mController.setUserInfo(userInfo);
        mController.displayPreference(mPreferenceScreen);
        assertThat(mButtonPreference.getTitle()).isEqualTo(TEST_USERNAME);
    }

    @Test
    public void testOnButtonClick_isCurrentUser_launchesEditUsernameFragment() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, /* flags= */ 0);
        when(mCarUserManagerHelper.isCurrentProcessUser(userInfo)).thenReturn(true);
        mController.setUserInfo(userInfo);
        mController.displayPreference(mPreferenceScreen);
        mButtonPreference.performButtonClick();
        verify(mFragmentController).launchFragment(any(EditUsernameFragment.class));
    }

    @Test
    public void testOnButtonClick_isNotCurrentUser_doesNothing() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, /* flags= */ 0);
        when(mCarUserManagerHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        mController.setUserInfo(userInfo);
        mController.displayPreference(mPreferenceScreen);
        mButtonPreference.performButtonClick();
        verify(mFragmentController, never()).launchFragment(any(EditUsernameFragment.class));
    }

    @Test
    public void testGetSummary_isNotInitialized() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, /* flags= */ 0);
        mController.setUserInfo(userInfo);
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.user_summary_not_set_up));
    }

    @Test
    public void testGetSummary_isInitialized() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        mController.setUserInfo(userInfo);
        assertThat(mController.getSummary()).isNull();
    }

    @Test
    public void testGetSummary_isAdmin_notCurrentUser() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME,
                FLAG_INITIALIZED | FLAG_ADMIN);
        when(mCarUserManagerHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        mController.setUserInfo(userInfo);
        assertThat(mController.getSummary()).isEqualTo(mContext.getString(R.string.user_admin));
    }

    @Test
    public void testGetSummary_isAdmin_currentUser() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME,
                FLAG_INITIALIZED | FLAG_ADMIN);
        when(mCarUserManagerHelper.isCurrentProcessUser(userInfo)).thenReturn(true);
        mController.setUserInfo(userInfo);
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.signed_in_admin_user));
    }
}
