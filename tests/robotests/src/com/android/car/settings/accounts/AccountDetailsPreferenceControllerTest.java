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

import static org.mockito.Mockito.mock;
import static org.robolectric.RuntimeEnvironment.application;
import static org.testng.Assert.assertThrows;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.ApplicationPackageManager;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import androidx.preference.Preference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.testutils.ShadowAccountManager;
import com.android.car.settings.testutils.ShadowContentResolver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

/** Unit test for {@link AccountDetailsPreferenceController}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowAccountManager.class, ShadowContentResolver.class,
        AccountDetailsPreferenceControllerTest.ShadowApplicationPackageManager.class})
public class AccountDetailsPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "preference_key";
    private static final String ACCOUNT_NAME = "Name";
    private final Account mAccount = new Account(ACCOUNT_NAME, "com.acct");
    private final UserHandle mUserHandle = new UserHandle(0);

    private AccountDetailsPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        mController = new AccountDetailsPreferenceController(application, PREFERENCE_KEY,
                mock(FragmentController.class));
        mController.setAccount(mAccount);
        mController.setUserHandle(mUserHandle);

        mPreference = new Preference(application);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void checkInitialized_accountSetAndUserHandleSet_doesNothing() {
        mController = new AccountDetailsPreferenceController(application, PREFERENCE_KEY,
                mock(FragmentController.class));
        mController.setAccount(mAccount);
        mController.setUserHandle(mUserHandle);

        mController.checkInitialized();
    }

    @Test
    public void checkInitialized_nullAccount_throwsIllegalStateException() {
        mController = new AccountDetailsPreferenceController(application, PREFERENCE_KEY,
                mock(FragmentController.class));
        mController.setUserHandle(mUserHandle);

        assertThrows(IllegalStateException.class, () -> mController.checkInitialized());
    }

    @Test
    public void checkInitialized_nullUserHandle_throwsIllegalStateException() {
        mController = new AccountDetailsPreferenceController(application, PREFERENCE_KEY,
                mock(FragmentController.class));
        mController.setAccount(mAccount);

        assertThrows(IllegalStateException.class, () -> mController.checkInitialized());
    }

    @Test
    public void updateState_shouldSetTitle() {
        mController.updateState(mPreference);

        assertThat(mPreference.getTitle().toString()).isEqualTo(ACCOUNT_NAME);
    }

    @Test
    public void updateState_shouldSetIcon() {
        // Add authenticator description with icon resource
        addAuthenticator(/* type= */ "com.acct", /* labelRes= */
                R.string.account_type1_label, /* iconId= */ R.drawable.ic_add);

        mController.updateState(mPreference);

        assertThat(mPreference.getIcon()).isNotNull();
        assertThat(Shadows.shadowOf(mPreference.getIcon()).getCreatedFromResId()).isEqualTo(
                R.drawable.ic_add);
    }

    private void addAuthenticator(String type, int labelRes, int iconId) {
        getShadowAccountManager().addAuthenticator(
                new AuthenticatorDescription(type, "com.android.car.settings",
                        labelRes, iconId, /* smallIconId= */ 0, /* prefId= */ 0,
                        /* customTokens= */ false));
    }

    private ShadowAccountManager getShadowAccountManager() {
        return Shadow.extract(AccountManager.get(application));
    }

    /** Shadow of ApplicationPackageManager that returns the icon passed to getUserBadgedIcon. */
    @Implements(value = ApplicationPackageManager.class, isInAndroidSdk = false,
            looseSignatures = true)
    public static class ShadowApplicationPackageManager extends
            org.robolectric.shadows.ShadowApplicationPackageManager {
        @Implementation
        public Drawable getUserBadgedIcon(Drawable icon, UserHandle user) {
            return icon;
        }
    }
}
