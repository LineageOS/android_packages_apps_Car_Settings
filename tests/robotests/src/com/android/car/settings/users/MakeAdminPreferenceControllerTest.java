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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.content.pm.UserInfo;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.ButtonPreference;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.testutils.ShadowCarUserManagerHelper;
import com.android.car.settings.testutils.ShadowUserIconProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowCarUserManagerHelper.class, ShadowUserIconProvider.class})
public class MakeAdminPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "permissions_make_admin";
    private static final UserInfo TEST_USER = new UserInfo(/* id= */ 10,
            "Test Username", /* flags= */0);

    private Context mContext;
    private MakeAdminPreferenceController mController;
    @Mock
    private FragmentController mFragmentController;
    @Mock
    private ConfirmGrantAdminPermissionsDialog mDialog;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        MockitoAnnotations.initMocks(this);
        ShadowCarUserManagerHelper.setMockInstance(mock(CarUserManagerHelper.class));
        mController = new MakeAdminPreferenceController(mContext, PREFERENCE_KEY,
                mFragmentController);
    }

    @After
    public void tearDown() {
        ShadowCarUserManagerHelper.reset();
    }

    @Test
    public void testOnCreate_noPreviousDialog_dialogListenerNotSet() {
        when(mFragmentController.findDialogByTag(
                ConfirmGrantAdminPermissionsDialog.TAG)).thenReturn(null);
        mController.onCreate();
        verify(mDialog, never()).setConfirmGrantAdminListener(
                any(ConfirmGrantAdminPermissionsDialog.ConfirmGrantAdminListener.class));
    }

    @Test
    public void testOnCreate_hasPreviousDialog_dialogListenerSet() {
        when(mFragmentController.findDialogByTag(
                ConfirmGrantAdminPermissionsDialog.TAG)).thenReturn(mDialog);
        mController.onCreate();
        verify(mDialog).setConfirmGrantAdminListener(
                any(ConfirmGrantAdminPermissionsDialog.ConfirmGrantAdminListener.class));
    }

    @Test
    public void testOnButtonClick_showsDialog() {
        PreferenceScreen preferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(
                mContext);
        ButtonPreference buttonPreference = new ButtonPreference(mContext);
        buttonPreference.setSelectable(false);
        buttonPreference.setKey(PREFERENCE_KEY);
        preferenceScreen.addPreference(buttonPreference);
        mController.setUserInfo(TEST_USER);
        mController.displayPreference(preferenceScreen);
        buttonPreference.performButtonClick();
        verify(mFragmentController).showDialog(any(ConfirmGrantAdminPermissionsDialog.class),
                matches(ConfirmGrantAdminPermissionsDialog.TAG));
    }

}
