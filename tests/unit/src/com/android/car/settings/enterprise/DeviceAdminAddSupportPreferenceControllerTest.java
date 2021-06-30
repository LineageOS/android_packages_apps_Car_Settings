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

import org.junit.Before;
import org.junit.Test;

public final class DeviceAdminAddSupportPreferenceControllerTest extends
        BaseDeviceAdminAddPreferenceControllerTestCase
                <DeviceAdminAddSupportPreferenceController> {

    private DeviceAdminAddSupportPreferenceController mController;

    @Before
    public void setController() {
        mController = new DeviceAdminAddSupportPreferenceController(mSpiedContext,
                mPreferenceKey, mFragmentController, mUxRestrictions);
        mController.setDeviceAdmin(mDefaultDeviceAdminInfo);
    }

    @Test
    public void testUpdateState_nullMessage() {
        mockGetLongSupportMessageForUser(null);

        mController.updateState(mPreference);

        verifyPreferenceTitleNeverSet();
    }

    @Test
    public void testUpdateState_emptyMessage() {
        mockGetLongSupportMessageForUser("");

        mController.updateState(mPreference);

        verifyPreferenceTitleNeverSet();
    }

    @Test
    public void testUpdate_validMessage() {
        mockGetLongSupportMessageForUser("WHAZZZZUP");

        mController.updateState(mPreference);

        verifyPreferenceTitleSet("WHAZZZZUP");
    }
}
