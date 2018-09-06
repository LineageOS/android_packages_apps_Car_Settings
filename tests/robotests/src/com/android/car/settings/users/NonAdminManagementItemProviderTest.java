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

import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import android.car.user.CarUserManagerHelper;
import android.content.Context;
import android.os.UserManager;
import android.view.View;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.testutils.ShadowActionListItem;
import com.android.car.settings.testutils.ShadowUserIconProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;

/**
 * Tests for UsersItemProviderTest.
 */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = { ShadowActionListItem.class, ShadowUserIconProvider.class })
public class NonAdminManagementItemProviderTest {
    @Mock
    private CarUserManagerHelper mCarUserManagerHelper;
    @Mock
    private NonAdminManagementItemProvider.AssignAdminListener mListener;
    @Mock
    private UserManager mUserManager;

    @Before
    public void setUpMocks() {
        MockitoAnnotations.initMocks(this);

        ShadowApplication shadowApp = ShadowApplication.getInstance();
        shadowApp.setSystemService(Context.USER_SERVICE, mUserManager);
    }

    @Test
    public void testSingleItem() {
        int userId = 10;
        NonAdminManagementItemProvider provider = createProvider(userId);
        assertThat(provider.size()).isEqualTo(1);
    }

    @Test
    public void testItemAssignsAdminPrivileges() {
        int userId = 10;
        NonAdminManagementItemProvider provider = createProvider(userId);

        ShadowActionListItem actionListItem = Shadow.extract(provider.get(0));
        assertThat(actionListItem.getTitle())
                .isEqualTo(application.getString(R.string.grant_admin_privileges));
    }

    @Test
    public void testClickOnAssignAdminInvokesOnAssignAdminClicked() {
        int userId = 10;
        NonAdminManagementItemProvider provider = createProvider(userId);

        ShadowActionListItem actionListItem = Shadow.extract(provider.get(0));
        actionListItem.getPrimaryActionOnClickListener().onClick(
                new View(application.getApplicationContext()));
        verify(mListener).onAssignAdminClicked();
    }

    private NonAdminManagementItemProvider createProvider(int userId) {
        return new NonAdminManagementItemProvider(userId,
                RuntimeEnvironment.application.getApplicationContext(), mListener,
                mCarUserManagerHelper);
    }
}
