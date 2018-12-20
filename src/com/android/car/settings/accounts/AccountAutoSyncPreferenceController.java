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
package com.android.car.settings.accounts;

import android.car.drivingstate.CarUxRestrictions;
import android.car.userlib.CarUserManagerHelper;
import android.content.ContentResolver;
import android.content.Context;
import android.os.UserHandle;

import androidx.preference.TwoStatePreference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;

/**
 * Controller for the preference that allows the user to toggle automatic syncing of accounts.
 *
 * <p>Copied from {@link com.android.settings.users.AutoSyncDataPreferenceController}
 */
public class AccountAutoSyncPreferenceController extends
        PreferenceController<TwoStatePreference> implements
        ConfirmAutoSyncChangeDialogFragment.OnConfirmListener {
    private static final String TAG_CONFIRM_AUTO_SYNC_CHANGE = "confirmAutoSyncChange";

    private final UserHandle mUserHandle;

    public AccountAutoSyncPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        CarUserManagerHelper carUserManagerHelper = new CarUserManagerHelper(context);
        mUserHandle = carUserManagerHelper.getCurrentProcessUserInfo().getUserHandle();
    }

    @Override
    protected Class<TwoStatePreference> getPreferenceType() {
        return TwoStatePreference.class;
    }

    @Override
    protected void updateState(TwoStatePreference preference) {
        preference.setChecked(ContentResolver.getMasterSyncAutomaticallyAsUser(
                mUserHandle.getIdentifier()));
    }

    @Override
    protected void onCreateInternal() {
        // If the dialog is still up, reattach the preference
        ConfirmAutoSyncChangeDialogFragment dialogFragment =
                (ConfirmAutoSyncChangeDialogFragment) getFragmentController().findDialogByTag(
                        TAG_CONFIRM_AUTO_SYNC_CHANGE);
        if (dialogFragment != null) {
            dialogFragment.setOnConfirmListener(this);
        }
    }

    @Override
    protected boolean handlePreferenceChanged(TwoStatePreference preference, Object checked) {
        getFragmentController().showDialog(
                ConfirmAutoSyncChangeDialogFragment.newInstance((Boolean) checked, mUserHandle,
                        this), TAG_CONFIRM_AUTO_SYNC_CHANGE);
        // The dialog will change the state of the preference if the user confirms, so don't handle
        // it here
        return false;
    }

    @Override
    public void onConfirm(boolean enabling) {
        getPreference().setChecked(enabling);
    }
}
