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

import static com.google.common.truth.Truth.assertThat;

import static java.util.stream.Collectors.toList;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public final class DeviceAdminAppsPreferenceControllerTest
        extends BasePreferenceControllerTestCase {

    private DeviceAdminAppsPreferenceController mController;
    private DummyPreferenceGroup mPreferenceGroup;

    @Before
    @UiThreadTest
    public void setUp() throws Exception {
        mController = new DeviceAdminAppsPreferenceController(mSpiedContext, mPreferenceKey,
                mFragmentController, mUxRestrictions);
        mPreferenceGroup = new DummyPreferenceGroup(mSpiedContext);
    }

    @Test
    public void testUpdateState_noBroadcastReceivers() {
        mockQueryBroadcastReceivers();

        mController.updateState(mPreferenceGroup);

        verifyPreferenceTitles(mRealContext.getString(R.string.device_admin_apps_list_empty));
    }

    @Test
    public void testUpdateState_singleActiveAdminApp() {
        mockQueryBroadcastReceivers(mDefaultResolveInfo);

        mController.updateState(mPreferenceGroup);

        verifyPreferenceTitles(mDefaultDeviceAdminInfo.loadLabel(mRealPm));
    }

    @Test
    public void testUpdateState_multipleActiveAdminApps() {
        mockQueryBroadcastReceivers(mDefaultResolveInfo, mFancyResolveInfo);

        mController.updateState(mPreferenceGroup);

        verifyPreferenceTitles(mDefaultDeviceAdminInfo.loadLabel(mRealPm),
                mFancyDeviceAdminInfo.loadLabel(mRealPm));
    }

    private void verifyPreferenceTitles(CharSequence... titles) {
        assertThat(mPreferenceGroup.getPreferences().stream()
                .map(p -> p.getTitle()).collect(toList())).containsExactly(titles);
    }

    private static final class DummyPreferenceGroup extends PreferenceGroup {

        private final List<Preference> mList = new ArrayList<>();

        DummyPreferenceGroup(Context context) {
            super(context, null);
        }

        @Override public void removeAll() {
            mList.clear();
        }

        @Override public boolean addPreference(Preference preference) {
            mList.add(preference);
            return true;
        }

        @Override public int getPreferenceCount() {
            return mList.size();
        }

        public List<Preference> getPreferences() {
            return mList;
        }
    }
}
