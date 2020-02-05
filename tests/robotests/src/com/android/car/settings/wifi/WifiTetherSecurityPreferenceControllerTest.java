/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.SoftApConfiguration;

import androidx.lifecycle.Lifecycle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.ListPreference;

import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.car.settings.testutils.ShadowCarWifiManager;
import com.android.car.settings.testutils.ShadowLocalBroadcastManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowCarWifiManager.class, ShadowLocalBroadcastManager.class})
public class WifiTetherSecurityPreferenceControllerTest {

    private static final String TEST_PASSWORD = "TEST_PASSWORD";

    private Context mContext;
    private ListPreference mPreference;
    private PreferenceControllerTestHelper<WifiTetherSecurityPreferenceController>
            mControllerHelper;
    private CarWifiManager mCarWifiManager;
    private LocalBroadcastManager mLocalBroadcastManager;
    private WifiTetherSecurityPreferenceController mController;

    @Before
    public void setup() {
        mContext = RuntimeEnvironment.application;
        mCarWifiManager = new CarWifiManager(mContext);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        mPreference = new ListPreference(mContext);
        mControllerHelper =
                new PreferenceControllerTestHelper<WifiTetherSecurityPreferenceController>(mContext,
                        WifiTetherSecurityPreferenceController.class, mPreference);
        mController = mControllerHelper.getController();
    }

    @After
    public void tearDown() {
        ShadowCarWifiManager.reset();
        ShadowLocalBroadcastManager.reset();
        SharedPreferences sp = mContext.getSharedPreferences(
                WifiTetherPasswordPreferenceController.SHARED_PREFERENCE_PATH,
                Context.MODE_PRIVATE);
        sp.edit().remove(WifiTetherPasswordPreferenceController.KEY_SAVED_PASSWORD).commit();
    }

    @Test
    public void onStart_securityTypeSetToNone_setsValueToNone() {
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setPassphrase(null, SoftApConfiguration.SECURITY_TYPE_OPEN)
                .build();
        mCarWifiManager.setSoftApConfig(config);

        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);

        assertThat(Integer.parseInt(mPreference.getValue()))
                .isEqualTo(SoftApConfiguration.SECURITY_TYPE_OPEN);
    }

    @Test
    public void onStart_securityTypeSetToWPA2PSK_setsValueToWPA2PSK() {
        String testPassword = "TEST_PASSWORD";
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setPassphrase(testPassword, SoftApConfiguration.SECURITY_TYPE_WPA2_PSK)
                .build();
        mCarWifiManager.setSoftApConfig(config);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);

        assertThat(Integer.parseInt(mPreference.getValue()))
                .isEqualTo(SoftApConfiguration.SECURITY_TYPE_WPA2_PSK);
    }

    @Test
    public void onPreferenceChangedToNone_updatesSecurityTypeToNone() {
        String testPassword = "TEST_PASSWORD";
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setPassphrase(testPassword, SoftApConfiguration.SECURITY_TYPE_WPA2_PSK)
                .build();
        mCarWifiManager.setSoftApConfig(config);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);

        mController.handlePreferenceChanged(mPreference,
                Integer.toString(SoftApConfiguration.SECURITY_TYPE_OPEN));

        assertThat(mCarWifiManager.getSoftApConfig().getSecurityType())
                .isEqualTo(SoftApConfiguration.SECURITY_TYPE_OPEN);
    }

    @Test
    public void onPreferenceChangedToWPA2PSK_updatesSecurityTypeToWPA2PSK() {
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setPassphrase(null, SoftApConfiguration.SECURITY_TYPE_OPEN)
                .build();
        mCarWifiManager.setSoftApConfig(config);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);

        // assume that the saved password was updated before the security type
        String newPassword = "NEW_PASSWORD";
        SharedPreferences sp = mContext.getSharedPreferences(
                WifiTetherPasswordPreferenceController.SHARED_PREFERENCE_PATH,
                Context.MODE_PRIVATE);
        sp.edit().putString(WifiTetherPasswordPreferenceController.KEY_SAVED_PASSWORD, newPassword)
                .commit();

        mController.handlePreferenceChanged(mPreference,
                Integer.toString(SoftApConfiguration.SECURITY_TYPE_WPA2_PSK));

        assertThat(mCarWifiManager.getSoftApConfig().getSecurityType())
                .isEqualTo(SoftApConfiguration.SECURITY_TYPE_WPA2_PSK);
    }

    @Test
    public void onPreferenceSwitchFromNoneToWPA2PSK_retrievesSavedPassword() {
        String savedPassword = "SAVED_PASSWORD";
        SharedPreferences sp = mContext.getSharedPreferences(
                WifiTetherPasswordPreferenceController.SHARED_PREFERENCE_PATH,
                Context.MODE_PRIVATE);
        sp.edit().putString(WifiTetherPasswordPreferenceController.KEY_SAVED_PASSWORD,
                savedPassword).commit();

        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setPassphrase(null, SoftApConfiguration.SECURITY_TYPE_OPEN)
                .build();
        mCarWifiManager.setSoftApConfig(config);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);

        mController.handlePreferenceChanged(mPreference,
                Integer.toString(SoftApConfiguration.SECURITY_TYPE_WPA2_PSK));

        assertThat(mCarWifiManager.getSoftApConfig().getPassphrase()).isEqualTo(savedPassword);
    }

    @Test
    public void onPreferenceChanged_broadcastsExactlyTwoIntents() {
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setPassphrase(null, SoftApConfiguration.SECURITY_TYPE_OPEN)
                .build();
        mCarWifiManager.setSoftApConfig(config);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);

        // assume that the saved password was updated before the security type
        String newPassword = "NEW_PASSWORD";
        SharedPreferences sp = mContext.getSharedPreferences(
                WifiTetherPasswordPreferenceController.SHARED_PREFERENCE_PATH,
                Context.MODE_PRIVATE);
        sp.edit().putString(WifiTetherPasswordPreferenceController.KEY_SAVED_PASSWORD, newPassword)
                .commit();

        int newSecurityType = SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;
        mController.handlePreferenceChanged(mPreference, newSecurityType);

        assertThat(ShadowLocalBroadcastManager.getSentBroadcastIntents().size()).isEqualTo(2);
    }

    @Test
    public void onPreferenceChanged_broadcastsSecurityTypeChangedFirst() {
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setPassphrase(null, SoftApConfiguration.SECURITY_TYPE_OPEN)
                .build();
        mCarWifiManager.setSoftApConfig(config);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);

        // assume that the saved password was updated before the security type
        String newPassword = "NEW_PASSWORD";
        SharedPreferences sp = mContext.getSharedPreferences(
                WifiTetherPasswordPreferenceController.SHARED_PREFERENCE_PATH,
                Context.MODE_PRIVATE);
        sp.edit().putString(WifiTetherPasswordPreferenceController.KEY_SAVED_PASSWORD, newPassword)
                .commit();

        int newSecurityType = SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;
        mController.handlePreferenceChanged(mPreference, newSecurityType);

        assertThat(ShadowLocalBroadcastManager.getSentBroadcastIntents().get(0).getAction())
                .isEqualTo(WifiTetherSecurityPreferenceController.ACTION_SECURITY_TYPE_CHANGED);
    }

    @Test
    public void onPreferenceChanged_broadcastsRequestTetheringRestartSecond() {
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setPassphrase(null, SoftApConfiguration.SECURITY_TYPE_OPEN)
                .build();
        mCarWifiManager.setSoftApConfig(config);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);

        // assume that the saved password was updated before the security type
        String newPassword = "NEW_PASSWORD";
        SharedPreferences sp = mContext.getSharedPreferences(
                WifiTetherPasswordPreferenceController.SHARED_PREFERENCE_PATH,
                Context.MODE_PRIVATE);
        sp.edit().putString(WifiTetherPasswordPreferenceController.KEY_SAVED_PASSWORD, newPassword)
                .commit();

        int newSecurityType = SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;
        mController.handlePreferenceChanged(mPreference, newSecurityType);

        assertThat(ShadowLocalBroadcastManager.getSentBroadcastIntents().get(1).getAction())
                .isEqualTo(WifiTetherSecurityPreferenceController.ACTION_RESTART_WIFI_TETHERING);
    }

    @Test
    public void onPreferenceChangedToWPA2PSK_broadcastsSecurityTypeWPA2PSK() {
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setPassphrase(null, SoftApConfiguration.SECURITY_TYPE_OPEN)
                .build();
        mCarWifiManager.setSoftApConfig(config);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);

        // assume that the saved password was updated before the security type
        String newPassword = "NEW_PASSWORD";
        SharedPreferences sp = mContext.getSharedPreferences(
                WifiTetherPasswordPreferenceController.SHARED_PREFERENCE_PATH,
                Context.MODE_PRIVATE);
        sp.edit().putString(WifiTetherPasswordPreferenceController.KEY_SAVED_PASSWORD, newPassword)
                .commit();

        int newSecurityType = SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;
        mController.handlePreferenceChanged(mPreference, newSecurityType);

        Intent expectedIntent = new Intent(
                WifiTetherSecurityPreferenceController.ACTION_SECURITY_TYPE_CHANGED);
        expectedIntent.putExtra(WifiTetherSecurityPreferenceController.KEY_SECURITY_TYPE,
                newSecurityType);

        assertThat(
                ShadowLocalBroadcastManager.getSentBroadcastIntents().get(0).toString())
                .isEqualTo(expectedIntent.toString());
    }

    @Test
    public void onPreferenceChangedToNone_broadcastsSecurityTypeNone() {
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setPassphrase(TEST_PASSWORD, SoftApConfiguration.SECURITY_TYPE_WPA2_PSK)
                .build();
        mCarWifiManager.setSoftApConfig(config);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);

        int newSecurityType = SoftApConfiguration.SECURITY_TYPE_OPEN;

        mController.handlePreferenceChanged(mPreference, newSecurityType);

        Intent expectedIntent = new Intent(
                WifiTetherSecurityPreferenceController.ACTION_SECURITY_TYPE_CHANGED);
        expectedIntent.putExtra(WifiTetherSecurityPreferenceController.KEY_SECURITY_TYPE,
                newSecurityType);

        assertThat(
                ShadowLocalBroadcastManager.getSentBroadcastIntents().get(0).toString())
                .isEqualTo(expectedIntent.toString());
    }

    @Test
    public void onPreferenceChanged_securityTypeChangedBeforePassword() {
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setPassphrase(null, SoftApConfiguration.SECURITY_TYPE_OPEN)
                .build();
        mCarWifiManager.setSoftApConfig(config);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);

        int newSecurityType = SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;
        mController.handlePreferenceChanged(mPreference, newSecurityType);

        Intent expectedIntent = new Intent(
                WifiTetherSecurityPreferenceController.ACTION_SECURITY_TYPE_CHANGED);
        expectedIntent.putExtra(WifiTetherSecurityPreferenceController.KEY_SECURITY_TYPE,
                newSecurityType);

        assertThat(
                ShadowLocalBroadcastManager.getSentBroadcastIntents().get(0).toString())
                .isEqualTo(expectedIntent.toString());
    }
}
