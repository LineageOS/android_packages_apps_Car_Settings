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

import android.app.admin.DevicePolicyManager;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.ArrayList;
import java.util.List;

/**
 * Shadow for LockPatternUtils.
 */
@Implements(LockPatternUtils.class)
public class ShadowLockPatternUtils {

    private static LockPatternUtils sInstance;
    private static int sPasswordQuality = DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
    private static byte[] sSavedPassword;
    private static List<LockPatternView.Cell> sSavedPattern;

    public static void setInstance(LockPatternUtils lockPatternUtils) {
        sInstance = lockPatternUtils;
    }

    @Resetter
    public static void reset() {
        sInstance = null;
        sPasswordQuality = DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
        sSavedPassword = null;
        sSavedPattern = null;
    }

    /**
     * Sets the current password quality that is returned by
     * {@link LockPatternUtils#getKeyguardStoredPasswordQuality}.
     */
    public static void setPasswordQuality(int passwordQuality) {
        sPasswordQuality = passwordQuality;
    }

    /**
     * Returns the password saved by a call to {@link LockPatternUtils#saveLockPassword}.
     */
    public static byte[] getSavedPassword() {
        return sSavedPassword;
    }

    /**
     * Returns the pattern saved by a call to {@link LockPatternUtils#saveLockPattern}.
     */
    public static List<LockPatternView.Cell> getSavedPattern() {
        return sSavedPattern;
    }

    @Implementation
    protected void clearLock(byte[] savedCredential, int userHandle) {
        sInstance.clearLock(savedCredential, userHandle);
    }

    @Implementation
    public int getKeyguardStoredPasswordQuality(int userHandle) {
        return sPasswordQuality;
    }

    @Implementation
    public void saveLockPassword(byte[] password, byte[] savedPassword, int requestedQuality,
            int userHandler) {
        sSavedPassword = password;
    }

    @Implementation
    public void saveLockPattern(List<LockPatternView.Cell> pattern, int userId) {
        sSavedPattern = new ArrayList<>(pattern);
    }
}
