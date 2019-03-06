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

package com.android.car.settings.datausage;

import static android.net.NetworkPolicy.WARNING_DISABLED;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.net.NetworkPolicyManager;
import android.net.NetworkTemplate;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.TwoStatePreference;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.net.DataUsageController;

/** Controls setting the data warning threshold. */
public class DataWarningPreferenceController extends
        PreferenceController<PreferenceGroup> implements Preference.OnPreferenceChangeListener {

    private final NetworkPolicyEditor mPolicyEditor;
    private final TelephonyManager mTelephonyManager;
    private final DataUsageController mDataUsageController;
    private final SubscriptionManager mSubscriptionManager;

    private TwoStatePreference mEnableDataWarningPreference;
    private Preference mSetDataWarningPreference;
    private NetworkTemplate mNetworkTemplate;

    public DataWarningPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mPolicyEditor = new NetworkPolicyEditor(NetworkPolicyManager.from(context));
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mDataUsageController = new DataUsageController(getContext());
    }

    @Override
    protected Class<PreferenceGroup> getPreferenceType() {
        return PreferenceGroup.class;
    }

    @Override
    protected void onCreateInternal() {
        mEnableDataWarningPreference = (TwoStatePreference) getPreference().findPreference(
                getContext().getString(R.string.pk_data_set_warning));
        mEnableDataWarningPreference.setOnPreferenceChangeListener(this);
        mSetDataWarningPreference = getPreference().findPreference(
                getContext().getString(R.string.pk_data_warning));

        mNetworkTemplate = DataUsageUtils.getMobileNetworkTemplate(mTelephonyManager,
                DataUsageUtils.getDefaultSubscriptionId(mSubscriptionManager));

        // Loads the current policies to the policy editor cache.
        mPolicyEditor.read();
    }

    @Override
    protected void updateState(PreferenceGroup preference) {
        long warningBytes = mPolicyEditor.getPolicyWarningBytes(mNetworkTemplate);
        if (warningBytes == WARNING_DISABLED) {
            mSetDataWarningPreference.setSummary(null);
            mEnableDataWarningPreference.setChecked(false);
        } else {
            mSetDataWarningPreference.setSummary(
                    DataUsageUtils.bytesToIecUnits(getContext(), warningBytes));
            mEnableDataWarningPreference.setChecked(true);
        }

        mSetDataWarningPreference.setEnabled(mEnableDataWarningPreference.isChecked());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (Boolean) newValue;
        mPolicyEditor.setPolicyWarningBytes(mNetworkTemplate,
                enabled ? mDataUsageController.getDefaultWarningLevel() : WARNING_DISABLED);
        refreshUi();
        return true;
    }
}
