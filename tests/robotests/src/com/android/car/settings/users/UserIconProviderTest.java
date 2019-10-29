/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.content.Context;
import android.content.pm.UserInfo;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;

import com.android.car.settings.testutils.ShadowUserManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class})
public class UserIconProviderTest {

    private Context mContext;
    private UserIconProvider mUserIconProvider;
    private UserInfo mUserInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mUserIconProvider = new UserIconProvider();
        mUserInfo = new UserInfo(/* id= */ 10, "USER_NAME", /* flags= */ 0);
    }

    @After
    public void tearDown() {
        ShadowUserManager.reset();
    }

    @Test
    public void getRoundedUserIcon_AssignsIconIfNotPresent() {
        ShadowUserManager.setUserIcon(mUserInfo.id, null);

        RoundedBitmapDrawable returnedIcon =
                mUserIconProvider.getRoundedUserIcon(mUserInfo, mContext);

        assertThat(returnedIcon).isNotNull();
        assertThat(getShadowUserManager().getUserIcon(mUserInfo.id)).isNotNull();
    }

    private ShadowUserManager getShadowUserManager() {
        return Shadow.extract(mContext.getSystemService(Context.USER_SERVICE));
    }
}
