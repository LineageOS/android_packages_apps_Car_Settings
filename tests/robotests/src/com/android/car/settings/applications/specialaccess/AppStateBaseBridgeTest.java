/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.car.settings.applications.specialaccess;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Looper;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

/** Unit test for {@link AppStateBaseBridge}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
public class AppStateBaseBridgeTest {

    @Mock
    private ApplicationsState mApplicationsState;
    @Mock
    private ApplicationsState.Session mSession;
    @Mock
    private AppStateBaseBridge.Callback mCallback;
    @Captor
    private ArgumentCaptor<ApplicationsState.Callbacks> mSessionCallbacksCaptor;

    private TestAppStateBaseBridge mBridge;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mApplicationsState.newSession(mSessionCallbacksCaptor.capture())).thenReturn(mSession);
        when(mApplicationsState.getBackgroundLooper()).thenReturn(Looper.getMainLooper());

        mBridge = new TestAppStateBaseBridge(mApplicationsState, mCallback);
    }

    @Test
    public void start_resumesSession() {
        mBridge.start();

        verify(mSession).onResume();
    }

    @Test
    public void start_beginsLoadingExtraInfo() {
        mBridge.start();

        assertThat(mBridge.getLoadExtraInfoCalledCount()).isEqualTo(1);
    }

    @Test
    public void onPackageListChanged_beginsLoadingExtraInfo() {
        mSessionCallbacksCaptor.getValue().onPackageListChanged();

        assertThat(mBridge.getLoadExtraInfoCalledCount()).isEqualTo(1);
    }

    @Test
    public void onLoadEntriesCompleted_beginsLoadingExtraInfo() {
        mSessionCallbacksCaptor.getValue().onLoadEntriesCompleted();

        assertThat(mBridge.getLoadExtraInfoCalledCount()).isEqualTo(1);
    }

    @Test
    public void stop_pausesSession() {
        mBridge.stop();

        verify(mSession).onPause();
    }

    @Test
    public void destroy_destroysSession() {
        mBridge.destroy();

        verify(mSession).onDestroy();
    }

    @Test
    public void forceUpdate_updatesEntryExtraInfo() {
        ApplicationsState.AppEntry entry = mock(ApplicationsState.AppEntry.class);
        mBridge.forceUpdate(entry);

        assertThat(mBridge.getArgsForLoadExtraInfo(/* forNthCall= */ 0)).containsExactly(entry);
    }

    @Test
    public void extraInfoLoaded_callbackNotified() {
        // Start loading.
        mBridge.start();

        // Everything is on the same looper in the test env, so loading will finish immediately.
        verify(mCallback).onExtraInfoUpdated();
    }

    /** Concrete impl of base class for testing. */
    private static class TestAppStateBaseBridge extends AppStateBaseBridge {

        private int mLoadExtraInfoCalledCount;
        private List<List<ApplicationsState.AppEntry>> mLoadExtraInfoArgs = new ArrayList<>();

        TestAppStateBaseBridge(ApplicationsState applicationsState, Callback callback) {
            super(applicationsState, callback);
        }

        @Override
        protected void loadExtraInfo(List<ApplicationsState.AppEntry> entries) {
            mLoadExtraInfoCalledCount++;
            mLoadExtraInfoArgs.add(entries);
        }

        int getLoadExtraInfoCalledCount() {
            return mLoadExtraInfoCalledCount;
        }

        List<ApplicationsState.AppEntry> getArgsForLoadExtraInfo(int forNthCall) {
            return mLoadExtraInfoArgs.get(forNthCall);
        }
    }
}
