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

import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.View;
import android.widget.Button;

import com.android.car.settings.R;
import com.android.car.settings.testutils.BaseTestActivity;
import com.android.car.settings.testutils.ShadowAccountManager;
import com.android.car.settings.testutils.ShadowContentResolver;
import com.android.car.settings.testutils.ShadowUserHelper;
import com.android.car.settings.users.UserHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowUserManager;

/**
 * Tests for AccountSettingsFragment class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAccountManager.class, ShadowContentResolver.class, ShadowUserHelper.class})
public class AccountSettingsFragmentTest {
    private final int mUserId = UserHandle.myUserId();

    private BaseTestActivity mActivity;
    private AccountSettingsFragment mFragment;

    @Mock
    private UserHelper mMockUserHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowUserHelper.setInstance(mMockUserHelper);
        // Set up user info
        when(mMockUserHelper.getCurrentProcessUserInfo())
                .thenReturn(new UserInfo(mUserId, "USER", /* flags= */ 0));

        mActivity = Robolectric.setupActivity(BaseTestActivity.class);
    }

    @After
    public void tearDown() {
        ShadowUserHelper.reset();
    }

    @Test
    public void cannotModifyUsers_addAccountButtonShouldNotBeVisible() {
        when(mMockUserHelper.canCurrentProcessModifyAccounts()).thenReturn(false);
        initFragment();

        Button addAccountButton = mFragment.requireActivity().findViewById(R.id.action_button1);
        assertThat(addAccountButton.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void canModifyUsers_addAccountButtonShouldBeVisible() {
        when(mMockUserHelper.canCurrentProcessModifyAccounts()).thenReturn(true);
        initFragment();

        Button addAccountButton = mFragment.requireActivity().findViewById(R.id.action_button1);
        assertThat(addAccountButton.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void clickAddAccountButton_shouldOpenChooseAccountFragment() {
        when(mMockUserHelper.canCurrentProcessModifyAccounts()).thenReturn(true);
        initFragment();

        Button addAccountButton = mFragment.requireActivity().findViewById(R.id.action_button1);
        addAccountButton.performClick();

        assertThat(mFragment.getFragmentManager().findFragmentById(
                R.id.fragment_container)).isInstanceOf(ChooseAccountFragment.class);
    }

    @Test
    public void clickAddAccountButton_shouldNotOpenChooseAccountFragmentWhenOneType() {
        when(mMockUserHelper.canCurrentProcessModifyAccounts()).thenReturn(true);
        getShadowUserManager().addProfile(mUserId, mUserId,
                String.valueOf(mUserId), /* profileFlags= */ 0);
        addAccountAndDescription(mUserId, "accountName", R.string.account_type1_label);
        initFragment();

        Button addAccountButton = mFragment.requireActivity().findViewById(R.id.action_button1);
        addAccountButton.performClick();

        assertThat(mFragment.getFragmentManager().findFragmentById(
                R.id.fragment_container)).isNotInstanceOf(ChooseAccountFragment.class);
    }

    @Test
    public void clickAddAccountButton_shouldOpenChooseAccountFragmentWhenTwoTypes() {
        when(mMockUserHelper.canCurrentProcessModifyAccounts()).thenReturn(true);
        getShadowUserManager().addProfile(mUserId, mUserId,
                String.valueOf(mUserId), /* profileFlags= */ 0);
        addAccountAndDescription(mUserId, "accountName1", R.string.account_type1_label);
        addAccountAndDescription(mUserId, "accountName2", R.string.account_type2_label);
        initFragment();

        Button addAccountButton = mFragment.requireActivity().findViewById(R.id.action_button1);
        addAccountButton.performClick();

        assertThat(mFragment.getFragmentManager().findFragmentById(
                R.id.fragment_container)).isInstanceOf(ChooseAccountFragment.class);
    }

    private void initFragment() {
        mFragment = new AccountSettingsFragment();
        mActivity.launchFragment(mFragment);
    }

    private void addAccountAndDescription(int profileId, String accountName, int labelId) {
        String type = accountName + "_type";
        getShadowAccountManager().addAccountAsUser(profileId, new Account(accountName, type));
        getShadowAccountManager().addAuthenticatorAsUser(profileId,
                new AuthenticatorDescription(type, "com.android.car.settings",
                        labelId, /* iconId= */ R.drawable.ic_add, /* smallIconId= */
                        0, /* prefId= */ 0));
    }

    private ShadowUserManager getShadowUserManager() {
        return Shadows.shadowOf(UserManager.get(application));
    }

    private ShadowAccountManager getShadowAccountManager() {
        return Shadow.extract(AccountManager.get(application));
    }
}
