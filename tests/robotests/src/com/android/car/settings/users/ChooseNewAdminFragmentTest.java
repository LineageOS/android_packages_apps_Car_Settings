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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.car.userlib.CarUserManagerHelper;
import android.content.pm.UserInfo;
import android.widget.Button;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.testutils.BaseTestActivity;
import com.android.car.settings.testutils.ShadowCarUserManagerHelper;
import com.android.car.settings.testutils.ShadowTextListItem;
import com.android.car.settings.testutils.ShadowUserIconProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

/**
 * Tests for ChooseNewAdminFragment.
 */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = { ShadowCarUserManagerHelper.class, ShadowUserIconProvider.class,
        ShadowTextListItem.class })
public class ChooseNewAdminFragmentTest {
    private static final String CONFIRM_GRANT_ADMIN_DIALOG_TAG = "ConfirmGrantAdminDialog";
    private BaseTestActivity mTestActivity;
    private ChooseNewAdminFragment mFragment;

    @Mock
    private CarUserManagerHelper mCarUserManagerHelper;
    private Button mActionButton;

    @Before
    public void setUpTestActivity() {
        MockitoAnnotations.initMocks(this);
        ShadowCarUserManagerHelper.setMockInstance(mCarUserManagerHelper);

        mTestActivity = Robolectric.buildActivity(BaseTestActivity.class)
                .setup()
                .get();
    }

    @Test
    public void testAssignNewAdminAndRemoveOldAdmin() {
        UserInfo oldAdmin = new UserInfo(/* id= */ 10, "oldAdmin", UserInfo.FLAG_ADMIN);
        UserInfo nonAdmin = new UserInfo(/* id= */ 11, "nonAdmin", /* flags= */ 0);
        createChooseNewAdminFragment(oldAdmin);
        mFragment.assignNewAdminAndRemoveOldAdmin(nonAdmin);

        // Assigns new admin.
        verify(mCarUserManagerHelper).grantAdminPermissions(nonAdmin);

        // Removes old admin.
        verify(mCarUserManagerHelper).removeUser(eq(oldAdmin), anyString());
    }

    @Test
    public void testUserClick_showsConfirmGrantAdminDialog() {
        createChooseNewAdminFragment();
        mFragment.onUserClicked(new UserInfo());

        assertThat(isDialogShown(CONFIRM_GRANT_ADMIN_DIALOG_TAG)).isTrue();
    }

    /* Test that upon creation, fragment is registered for listening on user updates. */
    @Test
    public void testRegisterOnUsersUpdateListener() {
        createChooseNewAdminFragment();
        verify(mCarUserManagerHelper).registerOnUsersUpdateListener(mFragment);
    }

    /* Test that onDestroy unregisters fragment for listening on user updates. */
    @Test
    public void testUnregisterOnUsersUpdateListener() {
        createChooseNewAdminFragment();
        mFragment.onDestroy();
        verify(mCarUserManagerHelper).unregisterOnUsersUpdateListener(mFragment);
    }

    @Test
    public void testBackButtonPressed_WhenRemoveCancelled() {
        createChooseNewAdminFragment();

        mActionButton.callOnClick();
        assertThat(mTestActivity.getOnBackPressedFlag()).isTrue();
    }

    private void createChooseNewAdminFragment(UserInfo userInfo) {
        mFragment = ChooseNewAdminFragment.newInstance(userInfo);
        mTestActivity.launchFragment(mFragment);

        mActionButton = (Button) mTestActivity.findViewById(R.id.action_button1);
    }

    private void createChooseNewAdminFragment() {
        createChooseNewAdminFragment(null);
    }

    private boolean isDialogShown(String tag) {
        return mTestActivity.getSupportFragmentManager().findFragmentByTag(tag) != null;
    }
}
