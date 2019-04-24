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

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.text.InputType;
import android.text.TextUtils;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.ValidatedEditTextPreference;

/**
 * Controls Wifi Hotspot password configuration.
 *
 * <p>Note: This controller uses {@link ValidatedEditTextPreference} as opposed to
 * PasswordEditTextPreference because the input is not obscured by default, and the user is setting
 * their own password, as opposed to entering password for authentication.
 */
public class WifiTetherPasswordPreferenceController extends
        WifiTetherBasePreferenceController<ValidatedEditTextPreference> {

    private static final int HOTSPOT_PASSWORD_MIN_LENGTH = 8;
    private static final int HOTSPOT_PASSWORD_MAX_LENGTH = 63;
    private static final ValidatedEditTextPreference.Validator PASSWORD_VALIDATOR =
            value -> value.length() >= HOTSPOT_PASSWORD_MIN_LENGTH
                    && value.length() <= HOTSPOT_PASSWORD_MAX_LENGTH;

    private String mPassword;

    public WifiTetherPasswordPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<ValidatedEditTextPreference> getPreferenceType() {
        return ValidatedEditTextPreference.class;
    }

    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();
        getPreference().setValidator(PASSWORD_VALIDATOR);
        mPassword = getCarWifiApConfig().preSharedKey;
    }

    @Override
    protected boolean handlePreferenceChanged(ValidatedEditTextPreference preference,
            Object newValue) {
        mPassword = newValue.toString();
        updatePassword(mPassword);
        refreshUi();
        return true;
    }

    @Override
    protected void updateState(ValidatedEditTextPreference preference) {
        super.updateState(preference);
        preference.setText(mPassword);
        if (TextUtils.isEmpty(mPassword)) {
            preference.setSummaryInputType(InputType.TYPE_CLASS_TEXT);
        } else {
            preference.setSummaryInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }

    @Override
    protected String getSummary() {
        return mPassword;
    }

    @Override
    protected String getDefaultSummary() {
        return getContext().getString(R.string.default_password_summary);
    }

    private void updatePassword(String password) {
        WifiConfiguration config = getCarWifiApConfig();
        config.preSharedKey = password;
        setCarWifiApConfig(config);
    }
}
