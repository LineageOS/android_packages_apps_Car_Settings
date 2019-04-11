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

package com.android.car.settings.wifi;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceGroup;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PasswordEditTextPreference;
import com.android.settingslib.wifi.AccessPoint;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Business logic relating to the security type and associated password. */
public class NetworkSecurityGroupPreferenceController extends
        AddNetworkBasePreferenceController<PreferenceGroup> {

    private static final Map<Integer, Integer> SECURITY_TYPE_TO_DESC_RES = new HashMap<>();

    static {
        SECURITY_TYPE_TO_DESC_RES.put(AccessPoint.SECURITY_NONE, R.string.wifi_security_none);
        SECURITY_TYPE_TO_DESC_RES.put(AccessPoint.SECURITY_WEP, R.string.wifi_security_wep);
        SECURITY_TYPE_TO_DESC_RES.put(AccessPoint.SECURITY_PSK, R.string.wifi_security_psk_generic);
        SECURITY_TYPE_TO_DESC_RES.put(AccessPoint.SECURITY_EAP, R.string.wifi_security_eap);
    }

    private static final List<Integer> SECURITY_TYPES = Arrays.asList(
            AccessPoint.SECURITY_NONE,
            AccessPoint.SECURITY_WEP,
            AccessPoint.SECURITY_PSK,
            AccessPoint.SECURITY_EAP);

    private final CharSequence[] mSecurityTypeNames;
    private final CharSequence[] mSecurityTypeIds;
    private ListPreference mSecurityTypePreference;
    private PasswordEditTextPreference mPasswordTextPreference;
    private int mSelectedSecurityType;

    public NetworkSecurityGroupPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);

        // Security type setup.
        mSecurityTypeNames = new CharSequence[SECURITY_TYPES.size()];
        mSecurityTypeIds = new CharSequence[SECURITY_TYPES.size()];
        mSelectedSecurityType = AccessPoint.SECURITY_NONE;

        for (int i = 0; i < SECURITY_TYPES.size(); i++) {
            int type = SECURITY_TYPES.get(i);
            mSecurityTypeNames[i] = getContext().getString(SECURITY_TYPE_TO_DESC_RES.get(type));
            mSecurityTypeIds[i] = Integer.toString(type);
        }
    }

    /** Returns the current password text. */
    public String getPasswordText() {
        return mPasswordTextPreference.getText();
    }

    /** Returns the currently selected security type. */
    public int getSelectedSecurityType() {
        return mSelectedSecurityType;
    }

    @Override
    protected Class<PreferenceGroup> getPreferenceType() {
        return PreferenceGroup.class;
    }

    @Override
    protected void onCreateInternal() {
        mSecurityTypePreference = createSecurityTypePreference();
        mPasswordTextPreference = createPasswordTextPreference();
        getPreference().addPreference(mSecurityTypePreference);
        getPreference().addPreference(mPasswordTextPreference);
    }

    @Override
    protected void updateState(PreferenceGroup preferenceGroup) {
        if (hasAccessPoint()) {
            mSecurityTypePreference.setVisible(false);
        } else {
            mSecurityTypePreference.setSummary(
                    SECURITY_TYPE_TO_DESC_RES.get(mSelectedSecurityType));
            mPasswordTextPreference.setVisible(mSelectedSecurityType != AccessPoint.SECURITY_NONE);
        }
    }

    private ListPreference createSecurityTypePreference() {
        ListPreference preference = new ListPreference(getContext());
        preference.setKey(getContext().getString(R.string.pk_add_wifi_security));
        preference.setTitle(R.string.wifi_security);
        preference.setDialogTitle(R.string.wifi_security);
        preference.setEntries(mSecurityTypeNames);
        preference.setEntryValues(mSecurityTypeIds);
        preference.setDefaultValue(Integer.toString(AccessPoint.SECURITY_NONE));
        preference.setPersistent(false);

        preference.setOnPreferenceChangeListener((pref, newValue) -> {
            mSelectedSecurityType = Integer.parseInt(newValue.toString());
            refreshUi();
            return true;
        });

        return preference;
    }

    private PasswordEditTextPreference createPasswordTextPreference() {
        PasswordEditTextPreference preference = new PasswordEditTextPreference(getContext());
        preference.setKey(getContext().getString(R.string.pk_add_wifi_password));
        preference.setTitle(R.string.wifi_password);
        preference.setDialogTitle(R.string.wifi_password);

        return preference;
    }
}
