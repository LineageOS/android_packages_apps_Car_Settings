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

package com.android.car.settings.testutils;

import android.car.userlib.CarUserManagerHelper;
import android.car.userlib.CarUserManagerHelper.OnUsersUpdateListener;
import android.content.pm.UserInfo;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.List;

/**
 * Shadow for {@link CarUserManagerHelper}
 */
@Implements(CarUserManagerHelper.class)
public class ShadowCarUserManagerHelper {
    private static CarUserManagerHelper sMockInstance;

    public static void setMockInstance(CarUserManagerHelper userManagerHelper) {
        sMockInstance = userManagerHelper;
    }

    @Resetter
    public static void reset() {
        sMockInstance = null;
    }

    @Implementation
    public void setUserName(UserInfo user, String name) {
        sMockInstance.setUserName(user, name);
    }

    @Implementation
    public UserInfo getCurrentProcessUserInfo() {
        return sMockInstance.getCurrentProcessUserInfo();
    }

    @Implementation
    public int getCurrentProcessUserId() {
        return sMockInstance.getCurrentProcessUserId();
    }

    @Implementation
    public boolean isCurrentProcessUser(UserInfo userInfo) {
        return sMockInstance.isCurrentProcessUser(userInfo);
    }

    @Implementation
    public List<UserInfo> getAllSwitchableUsers() {
        return sMockInstance.getAllSwitchableUsers();
    }

    @Implementation
    public UserInfo createNewNonAdminUser(String userName) {
        return sMockInstance.createNewNonAdminUser(userName);
    }

    @Implementation
    public void registerOnUsersUpdateListener(OnUsersUpdateListener listener) {
        sMockInstance.registerOnUsersUpdateListener(listener);
    }

    @Implementation
    public void unregisterOnUsersUpdateListener(OnUsersUpdateListener listener) {
        sMockInstance.unregisterOnUsersUpdateListener(listener);
    }

    @Implementation
    public boolean isUserLimitReached() {
        return sMockInstance.isUserLimitReached();
    }

    @Implementation
    public boolean canCurrentProcessModifyAccounts() {
        return sMockInstance.canCurrentProcessModifyAccounts();
    }

    @Implementation
    public boolean canCurrentProcessAddUsers() {
        return sMockInstance.canCurrentProcessAddUsers();
    }

    @Implementation
    public int getMaxSupportedRealUsers() {
        return sMockInstance.getMaxSupportedRealUsers();
    }

    @Implementation
    public boolean canCurrentProcessRemoveUsers() {
        return sMockInstance.canCurrentProcessRemoveUsers();
    }

    @Implementation
    public boolean canUserBeRemoved(UserInfo userInfo) {
        return sMockInstance.canUserBeRemoved(userInfo);
    }

    @Implementation
    public void grantAdminPermissions(UserInfo user) {
        sMockInstance.grantAdminPermissions(user);
    }

    @Implementation
    public boolean isCurrentProcessDemoUser() {
        return sMockInstance.isCurrentProcessDemoUser();
    }

    @Implementation
    public boolean isCurrentProcessAdminUser() {
        return sMockInstance.isCurrentProcessAdminUser();
    }

    @Implementation
    public boolean isCurrentProcessGuestUser() {
        return sMockInstance.isCurrentProcessGuestUser();
    }

    @Implementation
    public boolean removeUser(UserInfo userInfo, String guestUserName) {
        return sMockInstance.removeUser(userInfo, guestUserName);
    }

    @Implementation
    public void setUserRestriction(UserInfo userInfo, String restriction, boolean enable) {
        sMockInstance.setUserRestriction(userInfo, restriction, enable);
    }

    @Implementation
    public boolean hasUserRestriction(String restriction, UserInfo userInfo) {
        return sMockInstance.hasUserRestriction(restriction, userInfo);
    }
}
