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

import static org.mockito.Mockito.mock;

import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.settings.testutils.ShadowCarUserManagerHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Test for the preference controller which populates the various permissions preferences.
 * Note that the switch preference represents the opposite of the restriction it is controlling.
 * i.e. DISALLOW_ADD_USER may be the restriction, but the switch represents "create new users".
 */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowCarUserManagerHelper.class})
public class PermissionsPreferenceControllerTest {

    private static final String TEST_RESTRICTION = UserManager.DISALLOW_ADD_USER;
    private static final String PREFERENCE_KEY = "permissions_preferences";
    private static final UserInfo TEST_USER = new UserInfo(/* id= */ 10,
            "TEST_USER_NAME", /* flags= */ 0);
    private PermissionsPreferenceController mController;
    private PreferenceScreen mPreferenceScreen;
    private LogicalPreferenceGroup mPreferenceGroup;
    private CarUserManagerHelper mCarUserManagerHelper;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.application;
        mController = new PermissionsPreferenceController(context, PREFERENCE_KEY,
                mock(FragmentController.class));
        mController.setUserInfo(TEST_USER);

        mPreferenceScreen = new PreferenceManager(context).createPreferenceScreen(context);
        mPreferenceGroup = new LogicalPreferenceGroup(context);
        mPreferenceGroup.setKey(PREFERENCE_KEY);
        mPreferenceScreen.addPreference(mPreferenceGroup);
        mCarUserManagerHelper = new CarUserManagerHelper(context);
    }

    @After
    public void tearDown() {
        ShadowCarUserManagerHelper.reset();
    }

    @Test
    public void testDisplayPreference_populatesGroup() {
        mController.displayPreference(mPreferenceScreen);
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(5);
    }

    @Test
    public void testDisplayPreference_callingTwice_noDuplicates() {
        mController.displayPreference(mPreferenceScreen);
        mController.displayPreference(mPreferenceScreen);
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(5);
    }

    @Test
    public void testUpdateState_setToFalse() {
        mController.displayPreference(mPreferenceScreen);

        SwitchPreference preference = getPreferenceForRestriction(mPreferenceGroup,
                TEST_RESTRICTION);
        preference.setChecked(true);
        mCarUserManagerHelper.setUserRestriction(TEST_USER, TEST_RESTRICTION, true);
        mController.updateState(mPreferenceGroup);
        assertThat(preference.isChecked()).isFalse();
    }

    @Test
    public void testUpdateState_setToTrue() {
        mController.displayPreference(mPreferenceScreen);

        SwitchPreference preference = getPreferenceForRestriction(mPreferenceGroup,
                TEST_RESTRICTION);
        preference.setChecked(false);
        mCarUserManagerHelper.setUserRestriction(TEST_USER, TEST_RESTRICTION, false);
        mController.updateState(preference);
        assertThat(preference.isChecked()).isTrue();
    }

    @Test
    public void testOnPreferenceChange_changeToFalse() {
        mController.displayPreference(mPreferenceScreen);

        SwitchPreference preference = getPreferenceForRestriction(mPreferenceGroup,
                TEST_RESTRICTION);
        mCarUserManagerHelper.setUserRestriction(TEST_USER, TEST_RESTRICTION, true);
        preference.callChangeListener(true);
        assertThat(mCarUserManagerHelper.hasUserRestriction(TEST_RESTRICTION, TEST_USER)).isFalse();
    }

    @Test
    public void testOnPreferenceChange_changeToTrue() {
        mController.displayPreference(mPreferenceScreen);

        SwitchPreference preference = getPreferenceForRestriction(mPreferenceGroup,
                TEST_RESTRICTION);
        mCarUserManagerHelper.setUserRestriction(TEST_USER, TEST_RESTRICTION, false);
        preference.callChangeListener(false);
        assertThat(mCarUserManagerHelper.hasUserRestriction(TEST_RESTRICTION, TEST_USER)).isTrue();
    }

    private SwitchPreference getPreferenceForRestriction(
            LogicalPreferenceGroup preferenceGroup, String restriction) {
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            SwitchPreference preference = (SwitchPreference) preferenceGroup.getPreference(i);
            if (restriction.equals(preference.getExtras().getString(
                    PermissionsPreferenceController.PERMISSION_TYPE_KEY))) {
                return preference;
            }
        }
        return null;
    }
}
