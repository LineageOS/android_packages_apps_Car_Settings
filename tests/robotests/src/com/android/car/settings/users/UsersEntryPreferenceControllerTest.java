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

import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;

import com.android.car.settings.common.PreferenceControllerTestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowUserManager;

/** Unit test for {@link UsersEntryPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class UsersEntryPreferenceControllerTest {
    private Context mContext;
    private Preference mPreference;
    private PreferenceControllerTestHelper<UsersEntryPreferenceController> mControllerHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPreference = new Preference(mContext);
        mControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                UsersEntryPreferenceController.class, mPreference);
        mControllerHelper.markState(Lifecycle.State.STARTED);
    }

    @Test
    public void preferenceClicked_adminUser_handled() {
        setCurrentUserWithFlags(UserInfo.FLAG_ADMIN);

        assertThat(
                mPreference.getOnPreferenceClickListener().onPreferenceClick(mPreference)).isTrue();
    }

    @Test
    public void preferenceClicked_adminUser_launchesUsersListFragment() {
        setCurrentUserWithFlags(UserInfo.FLAG_ADMIN);

        mPreference.performClick();

        verify(mControllerHelper.getMockFragmentController()).launchFragment(
                any(UsersListFragment.class));
    }

    @Test
    public void preferenceClicked_nonAdminUser_handled() {
        setCurrentUserWithFlags(/* flags= */ 0);

        assertThat(
                mPreference.getOnPreferenceClickListener().onPreferenceClick(mPreference)).isTrue();
    }

    @Test
    public void preferenceClicked_nonAdminUser_launchesUserDetailsFragment() {
        setCurrentUserWithFlags(/* flags= */ 0);

        mPreference.performClick();

        ArgumentCaptor<UserDetailsFragment> fragmentCaptor = ArgumentCaptor.forClass(
                UserDetailsFragment.class);
        verify(mControllerHelper.getMockFragmentController()).launchFragment(
                fragmentCaptor.capture());
        UserDetailsFragment launchedFragment = fragmentCaptor.getValue();
        assertThat(launchedFragment.getArguments()).isNotNull();
        assertThat(launchedFragment.getArguments().getInt(Intent.EXTRA_USER_ID))
                .isEqualTo(UserHandle.myUserId());
    }

    private void setCurrentUserWithFlags(int flags) {
        getShadowUserManager().addUser(UserHandle.myUserId(), "test name", flags);
    }

    private ShadowUserManager getShadowUserManager() {
        return Shadows.shadowOf(UserManager.get(mContext));
    }
}
