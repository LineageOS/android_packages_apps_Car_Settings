/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.android.car.settings.common.TopLevelMenuFragment.FRAGMENT_MENU_PREFERENCE_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.os.UserManager;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class UsersEntryPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "key";

    private Context mContext = ApplicationProvider.getApplicationContext();
    private Preference mPreference;
    private UsersEntryPreferenceController mPreferenceController;
    private CarUxRestrictions mCarUxRestrictions;

    @Mock
    private FragmentController mFragmentController;
    @Mock
    private UserManager mUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();

        mPreference = new Preference(mContext);
        mPreferenceController = new UsersEntryPreferenceController(mContext,
                PREFERENCE_KEY, mFragmentController, mCarUxRestrictions);
        mPreferenceController.setUserManager(mUserManager);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
    }

    @Test
    @UiThreadTest
    public void handlePreferenceClicked_returnsTrue() {
        assertThat(mPreferenceController.handlePreferenceClicked(mPreference)).isTrue();
    }

    @Test
    @UiThreadTest
    public void handlePreferenceClicked_isAdmin_launchesUsersListFragment() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        mPreferenceController.handlePreferenceClicked(mPreference);
        verify(mFragmentController).launchFragment(any(UsersListFragment.class));
    }

    @Test
    @UiThreadTest
    public void handlePreferenceClicked_isNotAdmin_launchesUserDetailsFragment() {
        when(mUserManager.isAdminUser()).thenReturn(false);
        mPreferenceController.handlePreferenceClicked(mPreference);
        verify(mFragmentController).launchFragment(any(UserDetailsFragment.class));
    }

    @Test
    @UiThreadTest
    public void handlePreferenceClicked_fragmentHasKeyArgument() {
        mPreferenceController.handlePreferenceClicked(mPreference);
        ArgumentCaptor<Fragment> fragmentArgumentCaptor = ArgumentCaptor.forClass(Fragment.class);
        verify(mFragmentController).launchFragment(fragmentArgumentCaptor.capture());

        Fragment fragment = fragmentArgumentCaptor.getValue();
        assertThat(fragment.getArguments().getString(FRAGMENT_MENU_PREFERENCE_KEY)).isEqualTo(
                PREFERENCE_KEY);
    }
}
