/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.car.settings.privacy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.telephony.SubscriptionManager;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.dx.mockito.inline.extended.ExtendedMockito;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;

public class AppFunctionAccessPreferenceControllerTest {
    private MockitoSession mSession;

    private final Context mContext = Mockito.spy(ApplicationProvider.getApplicationContext());
    private AppFunctionAccessPreferenceController mPreferenceController;
    @Mock
    private FragmentController mFragmentController;
    @Mock
    private Preference mMockPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mSession = ExtendedMockito.mockitoSession()
                .mockStatic(SubscriptionManager.class, withSettings().lenient())
                .startMocking();
        CarUxRestrictions carUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();
        mPreferenceController = new AppFunctionAccessPreferenceController(mContext,
                "preferenceKey", mFragmentController, carUxRestrictions);

        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mMockPreference);
        // do not actually launch into the target activity
        doNothing().when(mContext).startActivity(any());
    }

    @After
    public void tearDown() {
        if (mSession != null) {
            mSession.finishMocking();
        }
    }

    @Test
    public void handlePreferenceClicked_startsActivity() {
        mPreferenceController.handlePreferenceClicked(mMockPreference);
        verify(mContext).startActivity(any());
    }
}
