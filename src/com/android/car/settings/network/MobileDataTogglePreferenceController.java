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

package com.android.car.settings.network;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.telephony.TelephonyManager;

import androidx.preference.TwoStatePreference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;

/**
 * Business logic to control the toggle that enables/disables usage of mobile data. Does not have
 * support for multi-sim.
 */
public class MobileDataTogglePreferenceController extends
        PreferenceController<TwoStatePreference> implements
        ConfirmMobileDataDisableDialog.ConfirmMobileDataDisableListener {

    private TelephonyManager mTelephonyManager;

    public MobileDataTogglePreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    protected Class<TwoStatePreference> getPreferenceType() {
        return TwoStatePreference.class;
    }

    @Override
    protected void onCreateInternal() {
        ConfirmMobileDataDisableDialog dialog =
                (ConfirmMobileDataDisableDialog) getFragmentController().findDialogByTag(
                        ConfirmMobileDataDisableDialog.TAG);
        if (dialog != null) {
            dialog.setListener(this);
        }
    }

    @Override
    protected void updateState(TwoStatePreference preference) {
        preference.setChecked(mTelephonyManager.isDataEnabled());
    }

    @Override
    protected boolean handlePreferenceChanged(TwoStatePreference preference, Object newValue) {
        boolean newToggleValue = (Boolean) newValue;
        if (!newToggleValue) {
            ConfirmMobileDataDisableDialog dialog = new ConfirmMobileDataDisableDialog();
            dialog.setListener(this);
            getFragmentController().showDialog(dialog, ConfirmMobileDataDisableDialog.TAG);
        } else {
            setMobileDataEnabled(true);
        }
        return false;
    }

    @Override
    public void onMobileDataDisableConfirmed() {
        setMobileDataEnabled(false);
    }

    @Override
    public void onMobileDataDisableRejected() {
        refreshUi();
    }

    private void setMobileDataEnabled(boolean enabled) {
        mTelephonyManager.setDataEnabled(enabled);
        refreshUi();
    }
}
