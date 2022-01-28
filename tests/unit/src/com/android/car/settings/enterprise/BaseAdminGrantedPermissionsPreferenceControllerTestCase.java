/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.car.settings.enterprise;

import static com.android.car.settings.common.PreferenceController.AVAILABLE;
import static com.android.car.settings.common.PreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.car.settings.common.PreferenceController.DISABLED_FOR_PROFILE;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.annotation.Nullable;
import android.util.Log;

import androidx.preference.Preference;

import com.android.car.settings.R;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settingslib.applications.ApplicationFeatureProvider;
import com.android.car.settingslib.applications.ApplicationFeatureProvider.NumberOfAppsCallback;
import com.android.internal.util.Preconditions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Objects;

abstract class BaseAdminGrantedPermissionsPreferenceControllerTestCase
        <C extends BaseAdminGrantedPermissionsPreferenceController> extends
        BaseEnterprisePrivacyPreferenceControllerTestCase {

    private static final String TAG = BaseAdminGrantedPermissionsPreferenceControllerTestCase.class
            .getSimpleName();

    // Must be a spy to verify refreshUi() is called
    private C mSpiedController;

    protected final String[] mPermissions;

    @Mock
    private Preference mPreference;

    BaseAdminGrantedPermissionsPreferenceControllerTestCase(String... permissions) {
        mPermissions = permissions;
    }

    @Before
    public void setController() {
        mSpiedController = spy(newController(mApplicationFeatureProvider));

        PreferenceControllerTestUtil.assignPreference(mSpiedController, mPreference);
    }

    protected abstract C newController(ApplicationFeatureProvider provider);

    @Test
    public void testGetAvailabilityStatus_noPermissionsGranted() {
        CallbackHolder callbackHolder = mockCalculateNumberOfAppsWithAdminGrantedPermissions();

        // Assert initial state
        assertAvailability(mSpiedController.getAvailabilityStatus(), CONDITIONALLY_UNAVAILABLE);

        // Unblock async call
        callbackHolder.release(0);

        // Assert post-callback result
        assertAvailability(mSpiedController.getAvailabilityStatus(), DISABLED_FOR_PROFILE);
        assertUiNotRefreshed();
    }

    @Test
    public void testGetAvailabilityStatus_permissionsGranted() {
        expectUiRefreshed();
        CallbackHolder callbackHolder = mockCalculateNumberOfAppsWithAdminGrantedPermissions();

        // Assert initial state
        assertAvailability(mSpiedController.getAvailabilityStatus(), CONDITIONALLY_UNAVAILABLE);

        // Unblock async call
        callbackHolder.release(42);

        // Assert post-callback result
        assertAvailability(mSpiedController.getAvailabilityStatus(), AVAILABLE);
        assertUiRefreshed();
    }

    @Test
    public void testUpdateState() {
        expectUiRefreshed();
        CallbackHolder callbackHolder = mockCalculateNumberOfAppsWithAdminGrantedPermissions();
        mSpiedController.getAvailabilityStatus();
        callbackHolder.release(42);
        assertUiRefreshed();

        mSpiedController.updateState(mPreference);

        assertPreferenceStateSet(42);
    }

    private CallbackHolder mockCalculateNumberOfAppsWithAdminGrantedPermissions() {
        CallbackHolder callbackHolder = new CallbackHolder();

        doAnswer((inv) -> {
            Log.d(TAG, "answering to " + inv);
            NumberOfAppsCallback callback = (NumberOfAppsCallback) inv.getArguments()[2];
            callbackHolder.setCallback(callback);
            return null;
        }).when(mApplicationFeatureProvider)
                .calculateNumberOfAppsWithAdminGrantedPermissions(eq(mPermissions),
                        /* sync= */ eq(true), any());
        return callbackHolder;
    }

    private void assertPreferenceStateSet(int count) {
        String expectedSummary = mRealContext.getResources().getQuantityString(
                R.plurals.enterprise_privacy_number_packages_lower_bound, count, count);
        verifyPreferenceTitleNeverSet(mPreference);
        verifyPreferenceSummarySet(mPreference, expectedSummary);
        verifyPreferenceIconNeverSet(mPreference);
    }

    private void expectUiRefreshed() {
        doNothing().when(mSpiedController).refreshUi();
    }

    private void assertUiRefreshed() {
        verify(mSpiedController).refreshUi();
    }

    private void assertUiNotRefreshed() {
        verify(mSpiedController, never()).refreshUi();
    }

    private final class CallbackHolder {
        @Nullable
        private NumberOfAppsCallback mCallback;

        void release(int result) {
            Preconditions.checkState(mCallback != null, "release() called before setCallback()");
            Log.d(TAG, "setting result to " + result + " and releasing latch on"
                    + Thread.currentThread());
            mCallback.onNumberOfAppsResult(result);
        }

        void setCallback(NumberOfAppsCallback callback) {
            Log.d(TAG, "setting callback to "  + callback);
            mCallback = Objects.requireNonNull(callback, "callback cannot be null");
        }
    }
}
