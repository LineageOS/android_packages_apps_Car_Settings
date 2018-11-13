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

package com.android.car.settings.accounts;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.car.userlib.CarUserManagerHelper;
import android.content.pm.UserInfo;
import android.os.UserHandle;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.testutils.ShadowAccountManager;
import com.android.car.settings.testutils.ShadowCarUserManagerHelper;
import com.android.car.settings.testutils.ShadowContentResolver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

/** Unit tests for {@link AddAccountPreferenceController}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowCarUserManagerHelper.class, ShadowContentResolver.class,
        ShadowAccountManager.class})
public class AddAccountPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "test_key";
    private static final int USER_ID = 0;
    private static final int NOT_THIS_USER_ID = 1;

    private PreferenceScreen mPreferenceScreen;
    private AddAccountPreferenceController mController;
    @Mock
    private FragmentController mFragmentController;
    @Mock
    private CarUserManagerHelper mMockCarUserManagerHelper;

    private AccountManager mAccountManager = AccountManager.get(application);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // Set up user info
        ShadowCarUserManagerHelper.setMockInstance(mMockCarUserManagerHelper);
        doReturn(new UserInfo(USER_ID, "name", 0)).when(
                mMockCarUserManagerHelper).getCurrentProcessUserInfo();

        // Add authenticated account types
        addAuthenticator(/* type= */ "com.acct1", /* label= */ R.string.account_type1_label);
        addAuthenticator(/* type= */ "com.acct2", /* label= */ R.string.account_type2_label);

        mPreferenceScreen = new PreferenceManager(application).createPreferenceScreen(application);
        mController = new AddAccountPreferenceController(application, PREFERENCE_KEY,
                mFragmentController);
    }

    @Test
    public void displayPreference_authenticatorPreferencesShouldBeSet() {
        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(2);

        Preference acct1Pref = mPreferenceScreen.getPreference(0);
        assertThat(acct1Pref.getTitle()).isEqualTo("Type 1");

        Preference acct2Pref = mPreferenceScreen.getPreference(1);
        assertThat(acct2Pref.getTitle()).isEqualTo("Type 2");
    }

    @Test
    public void onAccountsUpdate_currentUserUpdated_shouldForceUpdate() {
        mController.displayPreference(mPreferenceScreen);
        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(2);

        addAuthenticator(/* type= */ "com.acct3", /* label= */ R.string.account_type3_label);

        mController.onAccountsUpdate(new UserHandle(USER_ID));

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(3);
        Preference acct3Pref = mPreferenceScreen.getPreference(2);
        assertThat(acct3Pref.getTitle()).isEqualTo("Type 3");
    }

    @Test
    public void onAccountsUpdate_currentUserNotUpdated_shouldNotForceUpdate() {
        mController.displayPreference(mPreferenceScreen);
        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(2);

        addAuthenticator(/* type= */ "com.acct3", /* label= */ R.string.account_type3_label);

        mController.onAccountsUpdate(new UserHandle(NOT_THIS_USER_ID));

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    public void onPreferenceClick_shouldCallListenerOnAddAccount() {
        AddAccountPreferenceController.AddAccountListener mockListener = mock(
                AddAccountPreferenceController.AddAccountListener.class);
        mController.setListener(mockListener);
        mController.displayPreference(mPreferenceScreen);

        Preference acct1Pref = mPreferenceScreen.getPreference(0);
        mController.handlePreferenceTreeClick(acct1Pref);

        verify(mockListener).addAccount("com.acct1");
    }

    private void addAuthenticator(String type, int labelRes) {
        getShadowAccountManager().addAuthenticator(
                new AuthenticatorDescription(type, "com.android.car.settings",
                        labelRes, 0, 0, 0, false));
    }

    private ShadowAccountManager getShadowAccountManager() {
        return Shadow.extract(mAccountManager);
    }
}
