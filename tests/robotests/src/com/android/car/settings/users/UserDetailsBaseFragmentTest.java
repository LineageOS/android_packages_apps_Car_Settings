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

import static org.mockito.Mockito.when;

import android.car.userlib.CarUserManagerHelper;
import android.content.pm.UserInfo;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.View;
import android.widget.Button;

import com.android.car.settings.R;
import com.android.car.settings.common.ConfirmationDialogFragment;
import com.android.car.settings.testutils.BaseTestActivity;
import com.android.car.settings.testutils.ShadowCarUserManagerHelper;
import com.android.car.settings.testutils.ShadowUserIconProvider;
import com.android.car.settings.testutils.ShadowUserManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class, ShadowCarUserManagerHelper.class,
        ShadowUserIconProvider.class})
public class UserDetailsBaseFragmentTest {

    /*
     * This class needs to be public and static in order for it to be recreated from instance
     * state if necessary.
     */
    public static class TestUserDetailsBaseFragment extends UserDetailsBaseFragment {

        @Override
        protected String getTitleText() {
            return "test_title";
        }

        @Override
        protected int getPreferenceScreenResId() {
            return R.xml.test_user_details_base_fragment;
        }
    }

    private BaseTestActivity mTestActivity;
    private UserDetailsBaseFragment mUserDetailsBaseFragment;
    @Mock
    private CarUserManagerHelper mCarUserManagerHelper;
    @Mock
    private UserManager mUserManager;

    private Button mRemoveUserButton;

    @Before
    public void setUpTestActivity() {
        MockitoAnnotations.initMocks(this);
        ShadowCarUserManagerHelper.setMockInstance(mCarUserManagerHelper);
        ShadowUserManager.setInstance(mUserManager);

        mTestActivity = Robolectric.setupActivity(BaseTestActivity.class);
    }

    @After
    public void tearDown() {
        ShadowCarUserManagerHelper.reset();
        ShadowUserManager.reset();
    }

    @Test
    public void testRemoveUserButtonVisible_whenAllowedToRemoveUsers() {
        getShadowUserManager().setUserRestriction(
                Process.myUserHandle(), UserManager.DISALLOW_REMOVE_USER, false);
        when(mCarUserManagerHelper.isCurrentProcessDemoUser()).thenReturn(false);
        createUserDetailsBaseFragment(/*userId=*/1);

        assertThat(mRemoveUserButton.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void testRemoveUserButtonHidden_whenNotAllowedToRemoveUsers() {
        getShadowUserManager().setUserRestriction(
                Process.myUserHandle(), UserManager.DISALLOW_REMOVE_USER, true);
        when(mCarUserManagerHelper.isCurrentProcessDemoUser()).thenReturn(false);
        createUserDetailsBaseFragment(/*userId=*/1);

        assertThat(mRemoveUserButton.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testRemoveUserButtonHidden_whenUserIsSystemUser() {
        getShadowUserManager().setUserRestriction(
                Process.myUserHandle(), UserManager.DISALLOW_REMOVE_USER, false);
        when(mCarUserManagerHelper.isCurrentProcessDemoUser()).thenReturn(false);
        createUserDetailsBaseFragment(UserHandle.USER_SYSTEM);

        assertThat(mRemoveUserButton.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testRemoveUserButtonHidden_demoUser() {
        getShadowUserManager().setUserRestriction(
                Process.myUserHandle(), UserManager.DISALLOW_REMOVE_USER, false);
        when(mCarUserManagerHelper.isCurrentProcessDemoUser()).thenReturn(true);
        createUserDetailsBaseFragment(/*userId=*/1);

        assertThat(mRemoveUserButton.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testRemoveUserButtonClick_createsRemovalDialog() {
        getShadowUserManager().setUserRestriction(
                Process.myUserHandle(), UserManager.DISALLOW_REMOVE_USER, false);
        when(mCarUserManagerHelper.isCurrentProcessDemoUser()).thenReturn(false);
        when(mCarUserManagerHelper.getAllPersistentUsers()).thenReturn(
                Arrays.asList(new UserInfo()));
        createUserDetailsBaseFragment(/*userId=*/1);
        mRemoveUserButton.performClick();

        assertThat(mUserDetailsBaseFragment.findDialogByTag(
                ConfirmationDialogFragment.TAG)).isNotNull();
    }

    private void createUserDetailsBaseFragment(int userId) {
        UserInfo testUser = new UserInfo();
        testUser.id = userId;
        // Use UserDetailsFragment, since we cannot test an abstract class.
        mUserDetailsBaseFragment = UserDetailsBaseFragment.addUserIdToFragmentArguments(
                new TestUserDetailsBaseFragment(), testUser.id);
        when(mUserManager.getUserInfo(testUser.id)).thenReturn(testUser);
        mTestActivity.launchFragment(mUserDetailsBaseFragment);
        mRemoveUserButton = (Button) mTestActivity.findViewById(R.id.action_button1);
    }

    private ShadowUserManager getShadowUserManager() {
        return Shadow.extract(RuntimeEnvironment.application.getSystemService(UserManager.class));
    }
}
