/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.car.settings.common;

import static com.android.car.settings.common.ExtraSettingsPreferenceController.META_DATA_DISTRACTION_OPTIMIZED;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.testutils.ResourceTestUtils;
import com.android.car.ui.preference.CarUiPreference;
import com.android.car.ui.preference.DisabledPreferenceCallback;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class ExtraSettingsPreferenceControllerTest {
    private static final Intent FAKE_INTENT = new Intent();
    private static final CarUxRestrictions NO_SETUP_UX_RESTRICTIONS =
            new CarUxRestrictions.Builder(/* reqOpt= */ true,
                    CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP, /* timestamp= */ 0).build();

    private static final CarUxRestrictions BASELINE_UX_RESTRICTIONS =
            new CarUxRestrictions.Builder(/* reqOpt= */ true,
                    CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();

    private static final CarUxRestrictions NO_UX_RESTRICTIONS =
            new CarUxRestrictions.Builder(/* reqOpt= */ false,
                    CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();

    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    private Context mContext = ApplicationProvider.getApplicationContext();
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mScreen;
    private FakeExtraSettingsPreferenceController mPreferenceController;
    private CarUiPreference mPreference;
    private Map<Preference, Bundle> mPreferenceBundleMap = new HashMap<>();

    @Mock
    private FragmentController mFragmentController;
    @Mock
    private ExtraSettingsLoader mExtraSettingsLoaderMock;

    @Before
    @UiThreadTest
    public void setUp() {
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);

        MockitoAnnotations.initMocks(this);

        mPreferenceController = new FakeExtraSettingsPreferenceController(mContext,
                /* preferenceKey= */ "key", mFragmentController,
                BASELINE_UX_RESTRICTIONS);

        mPreferenceManager = new PreferenceManager(mContext);
        mScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mScreen.setIntent(FAKE_INTENT);
        mPreferenceController.setPreference(mScreen);
        mPreference = spy(new CarUiPreference(mContext));

        Bundle bundle = new Bundle();
        bundle.putBoolean(META_DATA_DISTRACTION_OPTIMIZED, false);
        mPreferenceBundleMap = new HashMap<>();
        mPreferenceBundleMap.put(mPreference, bundle);
        when(mExtraSettingsLoaderMock.loadPreferences(FAKE_INTENT)).thenReturn(
                mPreferenceBundleMap);
        mPreferenceController.setExtraSettingsLoader(mExtraSettingsLoaderMock);
    }

    @Test
    public void onUxRestrictionsChanged_restricted_restrictedMessageSet() {
        mPreferenceController.onCreate(mLifecycleOwner);

        Mockito.reset(mPreference);
        mPreferenceController.onUxRestrictionsChanged(NO_SETUP_UX_RESTRICTIONS);

        verify((DisabledPreferenceCallback) mPreference)
                .setMessageToShowWhenDisabledPreferenceClicked(
                        ResourceTestUtils.getString(mContext, "restricted_while_driving"));
    }

    @Test
    public void onUxRestrictionsChanged_unrestricted_restrictedMessageUnset() {
        mPreferenceController.onCreate(mLifecycleOwner);

        Mockito.reset(mPreference);
        mPreferenceController.onUxRestrictionsChanged(NO_UX_RESTRICTIONS);

        verify((DisabledPreferenceCallback) mPreference)
                .setMessageToShowWhenDisabledPreferenceClicked("");
    }

    @Test
    public void onUxRestrictionsChanged_restricted_viewOnly_restrictedMessageUnset() {
        mPreferenceController.setAvailabilityStatus(PreferenceController.AVAILABLE_FOR_VIEWING);
        mPreferenceController.onCreate(mLifecycleOwner);

        Mockito.reset(mPreference);
        mPreferenceController.onUxRestrictionsChanged(NO_SETUP_UX_RESTRICTIONS);

        verify((DisabledPreferenceCallback) mPreference)
                .setMessageToShowWhenDisabledPreferenceClicked("");
    }

    private static class FakeExtraSettingsPreferenceController extends
            ExtraSettingsPreferenceController {

        private int mAvailabilityStatus;

        FakeExtraSettingsPreferenceController(Context context, String preferenceKey,
                FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
            super(context, preferenceKey, fragmentController, uxRestrictions);
            mAvailabilityStatus = AVAILABLE;
        }

        @Override
        protected int getAvailabilityStatus() {
            return mAvailabilityStatus;
        }

        public void setAvailabilityStatus(int availabilityStatus) {
            mAvailabilityStatus = availabilityStatus;
        }
    }
}
