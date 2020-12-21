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

package com.android.car.settings.security;

import static com.android.car.settings.security.ChooseLockTypePreferenceController.LOCK_CHECK;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.testutils.TestLifecycleOwner;
import com.android.car.ui.preference.CarUiTwoActionTextPreference;
import com.android.internal.widget.LockscreenCredential;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class ChooseLockTypePreferenceControllerTest {

    private static final LockscreenCredential TEST_LOCKSCREEN_CREDENTIAL =
            LockscreenCredential.createPin("1234");
    private static final int TEST_PASSWORD_QUALITY = DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;

    private Context mContext = ApplicationProvider.getApplicationContext();
    private LifecycleOwner mLifecycleOwner;
    private Preference mPreference;
    private ChooseLockTypePreferenceController mPreferenceController;
    private CarUxRestrictions mCarUxRestrictions;

    @Mock
    private FragmentController mFragmentController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = new TestLifecycleOwner();

        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();

        mPreference = new CarUiTwoActionTextPreference(mContext);
        mPreferenceController = new ChooseLockTypePreferenceController(mContext,
                "key", mFragmentController, mCarUxRestrictions);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
    }

    @Test
    public void preferenceClicked_screenLockActivityLaunched() {
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreference.performClick();

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragmentController).startActivityForResult(intentArgumentCaptor.capture(),
                eq(LOCK_CHECK), any());

        Intent intent = intentArgumentCaptor.getValue();
        assertThat(intent.getComponent().getClassName()).isEqualTo(
                VerifyLockChangeActivity.class.getName());
    }

    @Test
    public void activityResult_resultNotOk_fragmentNotLaunched() {
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.processActivityResult(LOCK_CHECK,
                Activity.RESULT_CANCELED, /* data= */ null);

        verify(mFragmentController, never()).launchFragment(any());
    }

    @Test
    @UiThreadTest
    public void activityResult_resultOk_fragmentLaunched() {
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.processActivityResult(LOCK_CHECK,
                Activity.RESULT_OK, /* data= */ null);

        verify(mFragmentController).launchFragment(any(ChooseLockTypeFragment.class));
    }

    @Test
    @UiThreadTest
    public void activityResult_resultOk_currentScreenLockSet() {
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.processActivityResult(LOCK_CHECK,
                Activity.RESULT_OK, createDataBundle());

        ArgumentCaptor<Fragment> fragmentArgumentCaptor = ArgumentCaptor.forClass(Fragment.class);
        verify(mFragmentController).launchFragment(fragmentArgumentCaptor.capture());

        Fragment fragment = fragmentArgumentCaptor.getValue();
        assertThat((LockscreenCredential) fragment.getArguments().getParcelable(
                PasswordHelper.EXTRA_CURRENT_SCREEN_LOCK)).isEqualTo(TEST_LOCKSCREEN_CREDENTIAL);
    }

    @Test
    @UiThreadTest
    public void activityResult_resultOk_passwordQualitySet() {
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.processActivityResult(LOCK_CHECK,
                Activity.RESULT_OK, createDataBundle());

        ArgumentCaptor<Fragment> fragmentArgumentCaptor = ArgumentCaptor.forClass(Fragment.class);
        verify(mFragmentController).launchFragment(fragmentArgumentCaptor.capture());

        Fragment fragment = fragmentArgumentCaptor.getValue();
        assertThat(fragment.getArguments().getInt(
                PasswordHelper.EXTRA_CURRENT_PASSWORD_QUALITY)).isEqualTo(TEST_PASSWORD_QUALITY);
    }

    private Intent createDataBundle() {
        Intent intent = new Intent();
        intent.putExtra(PasswordHelper.EXTRA_CURRENT_SCREEN_LOCK, TEST_LOCKSCREEN_CREDENTIAL);
        intent.putExtra(PasswordHelper.EXTRA_CURRENT_PASSWORD_QUALITY, TEST_PASSWORD_QUALITY);
        return intent;
    }
}
