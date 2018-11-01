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

package com.android.car.settings.system;

import static com.android.car.settings.common.BasePreferenceController.AVAILABLE;
import static com.android.car.settings.common.BasePreferenceController.DISABLED_FOR_USER;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;

import androidx.preference.Preference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.testutils.ShadowCarUserManagerHelper;
import com.android.car.settings.testutils.ShadowCarrierConfigManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;

import java.util.List;

/** Unit test for {@link SystemUpdatePreferenceController}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowCarUserManagerHelper.class, ShadowCarrierConfigManager.class})
public class SystemUpdatePreferenceControllerTest {

    private static final String PREFERENCE_KEY = "preference_key";

    @Mock
    private CarUserManagerHelper mCarUserManagerHelper;
    private Context mContext;
    private SystemUpdatePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowCarUserManagerHelper.setMockInstance(mCarUserManagerHelper);

        mContext = RuntimeEnvironment.application;
        mController = new SystemUpdatePreferenceController(mContext, PREFERENCE_KEY,
                mock(FragmentController.class));
    }

    @Test
    public void getAvailabilityStatus_adminUser_available() {
        when(mCarUserManagerHelper.isCurrentProcessAdminUser()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_nonAdminUser_disabledForUser() {
        when(mCarUserManagerHelper.isCurrentProcessAdminUser()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_FOR_USER);
    }

    @Test
    public void handlePreferenceTreeClick_triggersClientInitiatedAction() {
        // Arrange
        String action = "action";
        String key = "key";
        String value = "value";

        PersistableBundle config = new PersistableBundle();
        config.putBoolean(CarrierConfigManager.KEY_CI_ACTION_ON_SYS_UPDATE_BOOL, true);
        config.putString(CarrierConfigManager.KEY_CI_ACTION_ON_SYS_UPDATE_INTENT_STRING, action);
        config.putString(CarrierConfigManager.KEY_CI_ACTION_ON_SYS_UPDATE_EXTRA_STRING, key);
        config.putString(CarrierConfigManager.KEY_CI_ACTION_ON_SYS_UPDATE_EXTRA_VAL_STRING, value);

        ShadowCarrierConfigManager shadowCarrierConfigManager = Shadow.extract(
                mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE));
        shadowCarrierConfigManager.setConfigForSubId(SubscriptionManager.getDefaultSubscriptionId(),
                config);

        Preference preference = new Preference(mContext);
        preference.setKey(PREFERENCE_KEY);

        // Act
        mController.handlePreferenceTreeClick(preference);

        // Assert
        List<Intent> broadcasts = ShadowApplication.getInstance().getBroadcastIntents();
        assertThat(broadcasts).hasSize(1);
        Intent broadcast = broadcasts.get(0);
        assertThat(broadcast.getAction()).isEqualTo(action);
        assertThat(broadcast.getStringExtra(key)).isEqualTo(value);
    }
}
