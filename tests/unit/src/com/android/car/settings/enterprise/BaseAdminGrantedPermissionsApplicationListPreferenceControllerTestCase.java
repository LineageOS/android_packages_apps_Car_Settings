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

import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.annotation.Nullable;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.util.ArrayMap;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.test.annotation.UiThreadTest;

import com.android.car.settings.enterprise.BaseEnterprisePreferenceControllerTestCase.DummyPreferenceGroup;
import com.android.car.settings.testutils.TextDrawable;
import com.android.car.settingslib.applications.ApplicationFeatureProvider;
import com.android.car.settingslib.applications.ApplicationFeatureProvider.ListOfAppsCallback;
import com.android.car.settingslib.applications.UserAppInfo;
import com.android.internal.util.Preconditions;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

abstract class BaseAdminGrantedPermissionsApplicationListPreferenceControllerTestCase
        <C extends BaseAdminGrantedPermissionsApplicationListPreferenceController>
        extends BaseEnterprisePrivacyPreferenceControllerTestCase {

    private static final String TAG =
            BaseAdminGrantedPermissionsApplicationListPreferenceControllerTestCase.class
                    .getSimpleName();

    private static final UserInfo DEFAULT_USER_INFO = new UserInfo(42, "Groot", /* flags= */ 0);

    private static final Map<CharSequence, Drawable> ICONS_BY_LABEL = new ArrayMap<>();

    // Must be a spy to verify refreshUi() is called
    private C mSpiedController;

    protected final String[] mPermissions;

    private DummyPreferenceGroup mPreferenceGroup;

    BaseAdminGrantedPermissionsApplicationListPreferenceControllerTestCase(String... permissions) {
        mPermissions = permissions;
    }

    @Before
    @UiThreadTest // Needed to instantiate DummyPreferenceGroup
    public void setExtraFixtures() {
        mSpiedController = spy(newController(mApplicationFeatureProvider));
        mPreferenceGroup = new DummyPreferenceGroup(mSpiedContext);
    }

    protected abstract C newController(ApplicationFeatureProvider provider);

    @Test
    public void testGetPreferenceType() {
        assertWithMessage("preference type").that(mSpiedController.getPreferenceType())
                .isEqualTo(PreferenceGroup.class);
    }

    @Test
    public void testGetAvailabilityStatus_noPermissionsGranted() {
        CallbackHolder callbackHolder = mockListAppsWithAdminGrantedPermissions();

        // Assert initial state
        assertAvailability(mSpiedController.getAvailabilityStatus(), CONDITIONALLY_UNAVAILABLE);

        // Unblock async call
        callbackHolder.release();

        // Assert post-callback result
        assertAvailability(mSpiedController.getAvailabilityStatus(), DISABLED_FOR_PROFILE);
        assertUiNotRefreshed();
    }

    @Test
    public void testGetAvailabilityStatus_permissionsGranted() {
        expectUiRefreshed();
        CallbackHolder callbackHolder = mockListAppsWithAdminGrantedPermissions();

        // Assert initial state
        assertAvailability(mSpiedController.getAvailabilityStatus(), CONDITIONALLY_UNAVAILABLE);

        // Unblock async call
        callbackHolder.release(newUserAppInfo("foo"), newUserAppInfo("bar"));

        // Assert post-callback result
        assertAvailability(mSpiedController.getAvailabilityStatus(), AVAILABLE);
        assertUiRefreshed();
    }

    @Test
    public void testUpdateState() {
        expectUiRefreshed();
        CallbackHolder callbackHolder = mockListAppsWithAdminGrantedPermissions();
        mSpiedController.getAvailabilityStatus();
        callbackHolder.release(newUserAppInfo("foo"), newUserAppInfo("bar"));
        assertUiRefreshed();

        mSpiedController.updateState(mPreferenceGroup);

        assertPreferenceStateSet("foo", "bar");
    }

    private CallbackHolder mockListAppsWithAdminGrantedPermissions() {
        CallbackHolder callbackHolder = new CallbackHolder();

        doAnswer((inv) -> {
            Log.d(TAG, "answering to " + inv);
            ListOfAppsCallback callback = (ListOfAppsCallback) inv.getArguments()[1];
            callbackHolder.setCallback(callback);
            return null;
        }).when(mApplicationFeatureProvider)
            .listAppsWithAdminGrantedPermissions(eq(mPermissions), any());
        return callbackHolder;
    }

    private void assertPreferenceStateSet(CharSequence...appLabels) {
        List<Preference> prefs = mPreferenceGroup.getPreferences();
        assertWithMessage("preferences").that(prefs).hasSize(appLabels.length);

        for (int i = 0; i < appLabels.length; i++) {
            Preference pref = prefs.get(i);
            CharSequence label = appLabels[i];
            assertWithMessage("title at index %s", i).that(pref.getTitle()).isEqualTo(label);
            Drawable icon = getIcon(label);
            assertWithMessage("icon at index %s", i).that(pref.getIcon()).isEqualTo(icon);
            assertWithMessage("order at index %s", i).that(pref.getOrder()).isEqualTo(i);
            assertWithMessage("selectable at index %s", i).that(pref.isSelectable()).isFalse();
        }
    }

    private void assertPreferenceStateNotSet() {
        List<Preference> prefs = mPreferenceGroup.getPreferences();
        assertWithMessage("preferences").that(prefs).isEmpty();
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

    private UserAppInfo newUserAppInfo(CharSequence label) {
        ApplicationInfo appInfo = new ApplicationInfo() {
            @Override
            public CharSequence loadLabel(PackageManager pm) {
                return label;
            }

            @Override
            public Drawable loadIcon(PackageManager pm) {
                return getIcon(label);
            };

        };
        return new UserAppInfo(DEFAULT_USER_INFO, appInfo);
    }

    private Drawable getIcon(CharSequence label) {
        Drawable icon = ICONS_BY_LABEL.get(label);
        if (icon != null) {
            Log.d(TAG, "getIcon(" + label + "): returning existing icon " + icon);
            return icon;
        }
        icon = new TextDrawable(label);
        ICONS_BY_LABEL.put(label, icon);
        Log.d(TAG, "getIcon(" + label + "): returning new icon " + icon);
        return icon;
    }

    private final class CallbackHolder {
        @Nullable
        private ListOfAppsCallback mCallback;

        void release(UserAppInfo... result) {
            Preconditions.checkState(mCallback != null, "release() called before setCallback()");
            Log.d(TAG, "setting result to " + Arrays.toString(result) + " and releasing latch on"
                    + Thread.currentThread());
            mCallback.onListOfAppsResult(Arrays.asList(result));
        }

        void setCallback(ListOfAppsCallback callback) {
            Log.d(TAG, "setting callback to "  + callback);
            mCallback = Objects.requireNonNull(callback, "callback cannot be null");
        }
    }
}
