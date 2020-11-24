/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.android.car.settings.users.AddUserPreferenceController.CONFIRM_CREATE_NEW_USER_DIALOG_TAG;
import static com.android.car.settings.users.AddUserPreferenceController.CONFIRM_EXIT_RETAIL_MODE_DIALOG_TAG;
import static com.android.car.settings.users.AddUserPreferenceController.MAX_USERS_LIMIT_REACHED_DIALOG_TAG;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.car.user.CarUserManager;
import android.car.user.UserCreationResult;
import android.car.util.concurrent.AndroidAsyncFuture;
import android.content.Context;
import android.os.UserManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.ConfirmationDialogFragment;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.testutils.ResourceTestUtils;
import com.android.car.settings.testutils.TestLifecycleOwner;
import com.android.internal.infra.AndroidFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class AddUserPreferenceControllerTest {
    private static final int ADD_USER_TASK_TIMEOUT = 10; // in seconds

    private Context mContext = ApplicationProvider.getApplicationContext();
    private LifecycleOwner mLifecycleOwner;
    private Preference mPreference;
    private AddUserPreferenceController mPreferenceController;
    private CarUxRestrictions mCarUxRestrictions;

    @Mock
    private FragmentController mFragmentController;
    @Mock
    private UserManager mUserManager;
    @Mock
    private CarUserManager mCarUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = new TestLifecycleOwner();

        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();

        mPreference = new Preference(mContext);
        mPreferenceController = new AddUserPreferenceController(mContext,
                /* preferenceKey= */ "key", mFragmentController, mCarUxRestrictions);
        mPreferenceController.setUserManager(mUserManager);
        mPreferenceController.setCarUserManager(mCarUserManager);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
    }

    @Test
    public void onCreate_userInDemoMode_showsExitRetailModeButton() {
        when(mUserManager.isDemoUser()).thenReturn(true);

        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getTitle()).isEqualTo(
                ResourceTestUtils.getString(mContext, "exit_retail_button_text"));
    }

    @Test
    public void onCreate_userCanAddNewUser_showsAddUserButton() {
        when(mUserManager.isDemoUser()).thenReturn(false);
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_ADD_USER)).thenReturn(false);

        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getTitle()).isEqualTo(
                ResourceTestUtils.getString(mContext, "user_add_user_menu"));
    }

    @Test
    public void onCreate_userRestrictedFromAddingNewUserAndNotInDemo_doesNotShowActionButton() {
        when(mUserManager.isDemoUser()).thenReturn(false);
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_ADD_USER)).thenReturn(true);

        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.isVisible()).isFalse();
    }

    /* Test that onCreateNewUserConfirmed invokes a creation of a new non-admin. */
    @Test
    public void newUserConfirmed_invokesCreateNewUser()
            throws ExecutionException, InterruptedException, TimeoutException {
        when(mUserManager.isDemoUser()).thenReturn(false);
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_ADD_USER)).thenReturn(true);
        AndroidFuture<UserCreationResult> future = new AndroidFuture<>();
        future.complete(new UserCreationResult(UserCreationResult.STATUS_SUCCESSFUL,
                /* user= */ null, /* errorMessage= */ null));
        when(mCarUserManager.createUser(anyString(), anyInt()))
                .thenReturn(new AndroidAsyncFuture<>(future));

        mPreferenceController.onCreate(mLifecycleOwner);

        mPreferenceController.mConfirmCreateNewUserListener.onConfirm(/* arguments= */ null);
        // wait for async task
        mPreferenceController.mAddNewUserTask.get(ADD_USER_TASK_TIMEOUT, TimeUnit.SECONDS);
        verify(mCarUserManager).createUser(
                ResourceTestUtils.getString(mContext, "user_new_user_name"), /* flags= */ 0);
    }

    /* Test that if we're in demo user, click on the button starts exit out of the retail mode. */
    @Test
    public void testCallOnClick_demoUser_exitRetailMode() {
        when(mUserManager.isDemoUser()).thenReturn(true);
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_ADD_USER)).thenReturn(false);

        mPreferenceController.onCreate(mLifecycleOwner);
        mPreference.performClick();

        verify(mFragmentController).showDialog(any(ConfirmationDialogFragment.class),
                eq(CONFIRM_EXIT_RETAIL_MODE_DIALOG_TAG));
    }

    /* Test that if the max num of users is reached, click on the button informs user of that. */
    @Test
    public void testCallOnClick_userLimitReached_showErrorDialog() {
        when(mUserManager.isDemoUser()).thenReturn(false);
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_ADD_USER)).thenReturn(false);
        when(mUserManager.canAddMoreUsers()).thenReturn(false);

        mPreferenceController.onCreate(mLifecycleOwner);
        mPreference.performClick();

        verify(mFragmentController).showDialog(any(ConfirmationDialogFragment.class),
                eq(MAX_USERS_LIMIT_REACHED_DIALOG_TAG));
    }

    /* Test that if user can add other users, click on the button creates a dialog to confirm. */
    @Test
    public void testCallOnClick_showAddUserDialog() {
        when(mUserManager.isDemoUser()).thenReturn(false);
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_ADD_USER)).thenReturn(false);
        when(mUserManager.canAddMoreUsers()).thenReturn(true);

        mPreferenceController.onCreate(mLifecycleOwner);
        mPreference.performClick();

        verify(mFragmentController).showDialog(any(ConfirmationDialogFragment.class),
                eq(CONFIRM_CREATE_NEW_USER_DIALOG_TAG));
    }
}
