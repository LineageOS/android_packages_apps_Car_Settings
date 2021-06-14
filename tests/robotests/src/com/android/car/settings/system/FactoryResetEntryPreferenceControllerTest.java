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

package com.android.car.settings.system;

import static android.os.UserManager.DISALLOW_FACTORY_RESET;

import static com.android.car.settings.common.PreferenceController.AVAILABLE;
import static com.android.car.settings.common.PreferenceController.AVAILABLE_FOR_VIEWING;
import static com.android.car.settings.enterprise.ActionDisabledByAdminDialogFragment.DISABLED_BY_ADMIN_CONFIRM_DIALOG_TAG;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.lifecycle.Lifecycle;

import com.android.car.settings.R;
import com.android.car.settings.common.ClickableWhileDisabledPreference;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.car.settings.enterprise.ActionDisabledByAdminDialogFragment;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

/** Unit test for {@link FactoryResetEntryPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class})
public class FactoryResetEntryPreferenceControllerTest {
    private static final int SECONDARY_USER_ID = 10;

    private Context mContext;
    private FactoryResetEntryPreferenceController mController;
    private FragmentController mMockFragmentController;
    private ClickableWhileDisabledPreference mPref;
    private PreferenceControllerTestHelper<FactoryResetEntryPreferenceController> mTestHelper;
    private ShadowUserManager mShadowUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPref = new ClickableWhileDisabledPreference(mContext);

        mTestHelper =
                new PreferenceControllerTestHelper<>(mContext,
                FactoryResetEntryPreferenceController.class, mPref);
        mController = mTestHelper.getController();
        mMockFragmentController = mTestHelper.getMockFragmentController();
        mShadowUserManager = ShadowUserManager.getShadow();
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_DEMO_MODE, 0);
        ShadowUserManager.reset();
    }

    @Test
    public void getAvailabilityStatus_nonAdminUser_disabledForUser() {
        createAndSwitchToSecondaryUserWithFlags(/* flags= */ 0);
        mTestHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_FOR_VIEWING);
        assertThat(mPref.isEnabled()).isFalse();
    }

    @Test
    public void getAvailabilityStatus_adminUser_unrestricted_available() {
        createAndSwitchToSecondaryUserWithFlags(UserInfo.FLAG_ADMIN);
        mTestHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mPref.isEnabled()).isTrue();
    }

    @Test
    public void getAvailabilityStatus_adminUser_baseRestricted_notDpmRestricted_disabledForUser() {
        createAndSwitchToSecondaryUserWithFlags(UserInfo.FLAG_ADMIN);
        mShadowUserManager.addBaseUserRestriction(DISALLOW_FACTORY_RESET);
        mTestHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_FOR_VIEWING);
        assertThat(mPref.isEnabled()).isFalse();
    }

    @Test
    public void getAvailabilityStatus_adminUser_baseRestricted_dpmRestricted_available() {
        createAndSwitchToSecondaryUserWithFlags(UserInfo.FLAG_ADMIN);
        mShadowUserManager.addBaseUserRestriction(DISALLOW_FACTORY_RESET);
        mShadowUserManager.setUserRestriction(
                UserHandle.of(SECONDARY_USER_ID), DISALLOW_FACTORY_RESET, true);
        mTestHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_FOR_VIEWING);
        assertThat(mPref.isEnabled()).isFalse();
    }

    @Test
    public void getAvailabilityStatus_adminUser_notBaseRestricted_dpmRestricted_available() {
        createAndSwitchToSecondaryUserWithFlags(UserInfo.FLAG_ADMIN);
        mShadowUserManager.setUserRestriction(
                UserHandle.of(SECONDARY_USER_ID), DISALLOW_FACTORY_RESET, true);
        mTestHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mPref.isEnabled()).isTrue();
    }

    @Test
    public void getAvailabilityStatus_demoMode_demoUser_available() {
        createAndSwitchToSecondaryUserWithFlags(UserInfo.FLAG_DEMO);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.DEVICE_DEMO_MODE, 1);
        mTestHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mPref.isEnabled()).isTrue();
    }

    @Test
    public void getAvailabilityStatus_demoMode_demoUser_baseRestricted_disabledForUser() {
        createAndSwitchToSecondaryUserWithFlags(UserInfo.FLAG_DEMO);
        mShadowUserManager.addBaseUserRestriction(DISALLOW_FACTORY_RESET);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.DEVICE_DEMO_MODE, 1);
        mTestHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_FOR_VIEWING);
        assertThat(mPref.isEnabled()).isFalse();
    }

    @Test
    public void getAvailabilityStatus_demoMode_demoUser_dpmRestricted_available() {
        createAndSwitchToSecondaryUserWithFlags(UserInfo.FLAG_DEMO);
        mShadowUserManager.setUserRestriction(
                UserHandle.of(SECONDARY_USER_ID), DISALLOW_FACTORY_RESET, true);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.DEVICE_DEMO_MODE, 1);
        mTestHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mPref.isEnabled()).isTrue();
    }

    @Test
    public void performClick_showsBlockedToast_nonAdminUser() {
        createAndSwitchToSecondaryUserWithFlags(/* flags= */ 0);
        mTestHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        mPref.performClick();

        assertShowingBlockedToast();
    }

    @Test
    public void performClick_triggersAction_adminUser_unrestricted() {
        createAndSwitchToSecondaryUserWithFlags(UserInfo.FLAG_ADMIN);
        mTestHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        mPref.performClick();

        assertNoToastAndNoDialog();
    }

    @Test
    public void performClick_showsBlockedToast_adminUser_baseRestricted_notDpmRestricted() {
        createAndSwitchToSecondaryUserWithFlags(UserInfo.FLAG_ADMIN);
        mShadowUserManager.addBaseUserRestriction(DISALLOW_FACTORY_RESET);
        mTestHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        mPref.performClick();

        assertShowingBlockedToast();
    }

    @Test
    public void performClick_showsBlockedToast_adminUser_baseRestricted_dpmRestricted() {
        createAndSwitchToSecondaryUserWithFlags(UserInfo.FLAG_ADMIN);
        mShadowUserManager.addBaseUserRestriction(DISALLOW_FACTORY_RESET);
        mShadowUserManager.setUserRestriction(
                UserHandle.of(SECONDARY_USER_ID), DISALLOW_FACTORY_RESET, true);
        mTestHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        mPref.performClick();

        assertShowingBlockedToast();
    }

    @Test
    public void performClick_showsBlockedDialog_adminUser_notBaseRestricted_dpmRestricted() {
        createAndSwitchToSecondaryUserWithFlags(UserInfo.FLAG_ADMIN);
        mShadowUserManager.setUserRestriction(
                UserHandle.of(SECONDARY_USER_ID), DISALLOW_FACTORY_RESET, true);
        mTestHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        mPref.performClick();

        assertShowingDisabledByAdminDialog();
    }

    @Test
    public void performClick_triggersAction_demoUser_unrestricted() {
        createAndSwitchToSecondaryUserWithFlags(UserInfo.FLAG_DEMO);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.DEVICE_DEMO_MODE, 1);
        mTestHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        mPref.performClick();

        assertNoToastAndNoDialog();
    }

    @Test
    public void performClick_showsBlockedDialog_demoUser_baseRestricted() {
        createAndSwitchToSecondaryUserWithFlags(UserInfo.FLAG_DEMO);
        mShadowUserManager.addBaseUserRestriction(DISALLOW_FACTORY_RESET);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.DEVICE_DEMO_MODE, 1);
        mTestHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        mPref.performClick();

        assertShowingBlockedToast();
    }

    @Test
    public void performClick_showsBlockedDialog_demoUser_dpmRestricted() {
        createAndSwitchToSecondaryUserWithFlags(UserInfo.FLAG_DEMO);
        mShadowUserManager.setUserRestriction(
                UserHandle.of(SECONDARY_USER_ID), DISALLOW_FACTORY_RESET, true);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.DEVICE_DEMO_MODE, 1);
        mTestHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        mPref.performClick();

        assertShowingDisabledByAdminDialog();
    }

    private void createAndSwitchToSecondaryUserWithFlags(int flags) {
        mShadowUserManager.addUser(SECONDARY_USER_ID, "test name", flags);
        mShadowUserManager.switchUser(SECONDARY_USER_ID);
    }

    private void assertShowingBlockedToast() {
        assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(mContext.getResources().getString(R.string.action_unavailable));
    }

    private void assertShowingDisabledByAdminDialog() {
        verify(mMockFragmentController).showDialog(any(ActionDisabledByAdminDialogFragment.class),
                eq(DISABLED_BY_ADMIN_CONFIRM_DIALOG_TAG));
    }

    private void assertNoToastAndNoDialog() {
        assertThat(ShadowToast.shownToastCount()).isEqualTo(0);
        verify(mMockFragmentController, never())
                .showDialog(any(ActionDisabledByAdminDialogFragment.class), anyString());
    }
}
