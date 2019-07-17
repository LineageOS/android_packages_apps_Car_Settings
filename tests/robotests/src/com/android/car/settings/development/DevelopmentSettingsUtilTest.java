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

package com.android.car.settings.development;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import com.android.car.settings.testutils.ShadowCarUserManagerHelper;
import com.android.car.settings.testutils.ShadowLocalBroadcastManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowUserManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowCarUserManagerHelper.class, ShadowLocalBroadcastManager.class})
public class DevelopmentSettingsUtilTest {

    private Context mContext;
    private UserManager mUserManager;

    @Mock
    private CarUserManagerHelper mCarUserManagerHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowCarUserManagerHelper.setMockInstance(mCarUserManagerHelper);
        mContext = RuntimeEnvironment.application;
        mUserManager = UserManager.get(mContext);
    }

    @After
    public void tearDown() {
        ShadowCarUserManagerHelper.reset();
        ShadowLocalBroadcastManager.reset();
    }

    @Test
    public void isEnabled_settingsOff_isAdmin_notDemo_shouldReturnFalse() {
        setCurrentUserWithFlags(UserInfo.FLAG_ADMIN);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);

        assertThat(DevelopmentSettingsUtil.isDevelopmentSettingsEnabled(mContext,
                mCarUserManagerHelper, mUserManager)).isFalse();
    }

    @Test
    public void isEnabled_settingsOn_isAdmin_notDemo_shouldReturnTrue() {
        setCurrentUserWithFlags(UserInfo.FLAG_ADMIN);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);

        assertThat(DevelopmentSettingsUtil.isDevelopmentSettingsEnabled(mContext,
                mCarUserManagerHelper, mUserManager)).isTrue();
    }

    @Test
    public void isEnabled_settingsOn_notAdmin_notDemo_shouldReturnFalse() {
        setCurrentUserWithFlags(/* flags= */ 0);

        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);

        assertThat(DevelopmentSettingsUtil.isDevelopmentSettingsEnabled(mContext,
                mCarUserManagerHelper, mUserManager)).isFalse();
    }

    @Test
    public void isEnabled_settingsOn_notAdmin_isDemo_shouldReturnTrue() {
        setCurrentUserWithFlags(UserInfo.FLAG_DEMO);

        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);

        assertThat(DevelopmentSettingsUtil.isDevelopmentSettingsEnabled(mContext,
                mCarUserManagerHelper, mUserManager)).isTrue();
    }

    @Test
    public void isEnabled_settingsOff_notAdmin_isDemo_shouldReturnFalse() {
        setCurrentUserWithFlags(UserInfo.FLAG_DEMO);

        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);

        assertThat(DevelopmentSettingsUtil.isDevelopmentSettingsEnabled(mContext,
                mCarUserManagerHelper, mUserManager)).isFalse();
    }

    @Test
    public void isEnabled_hasDisallowDebuggingRestriction_shouldReturnFalse() {
        setCurrentUserWithFlags(UserInfo.FLAG_ADMIN | UserInfo.FLAG_DEMO);

        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);

        getShadowUserManager().setUserRestriction(
                UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_DEBUGGING_FEATURES,
                true);

        assertThat(DevelopmentSettingsUtil.isDevelopmentSettingsEnabled(mContext,
                mCarUserManagerHelper, mUserManager)).isFalse();
    }

    @Test
    public void isEnabled_doesNotHaveDisallowDebuggingRestriction_shouldReturnTrue() {
        setCurrentUserWithFlags(UserInfo.FLAG_ADMIN | UserInfo.FLAG_DEMO);

        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);

        getShadowUserManager().setUserRestriction(
                UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_DEBUGGING_FEATURES,
                false);

        assertThat(DevelopmentSettingsUtil.isDevelopmentSettingsEnabled(mContext,
                mCarUserManagerHelper, mUserManager)).isTrue();
    }

    @Test
    public void setDevelopmentSettingsEnabled_setTrue() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);

        DevelopmentSettingsUtil.setDevelopmentSettingsEnabled(mContext, true);

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0)).isEqualTo(1);
    }

    @Test
    public void setDevelopmentSettingsEnabled_setFalse() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);

        DevelopmentSettingsUtil.setDevelopmentSettingsEnabled(mContext, false);

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1)).isEqualTo(0);
    }

    @Test
    public void isDeviceProvisioned_true() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.DEVICE_PROVISIONED,
                1);
        assertThat(DevelopmentSettingsUtil.isDeviceProvisioned(mContext)).isTrue();
    }

    @Test
    public void isDeviceProvisioned_false() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.DEVICE_PROVISIONED,
                0);
        assertThat(DevelopmentSettingsUtil.isDeviceProvisioned(mContext)).isFalse();
    }

    private void setCurrentUserWithFlags(int flags) {
        UserInfo userInfo = new UserInfo(UserHandle.myUserId(), null, flags);
        when(mCarUserManagerHelper.isCurrentProcessAdminUser())
                .thenReturn(UserInfo.FLAG_ADMIN == (flags & UserInfo.FLAG_ADMIN));
        when(mCarUserManagerHelper.getCurrentProcessUserInfo()).thenReturn(userInfo);
        getShadowUserManager().addUser(userInfo.id, userInfo.name, userInfo.flags);
    }

    private ShadowUserManager getShadowUserManager() {
        return Shadows.shadowOf(UserManager.get(mContext));
    }
}
