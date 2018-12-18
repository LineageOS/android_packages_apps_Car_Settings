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
import android.text.TextUtils;

import androidx.preference.EditTextPreference;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;

/** Business logic for adding/displaying the network name. */
public class NetworkNamePreferenceController extends
        AddNetworkBasePreferenceController<EditTextPreference> {

    private NetworkNameChangeListener mListener;

    public NetworkNamePreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<EditTextPreference> getPreferenceType() {
        return EditTextPreference.class;
    }

    /** Returns the currently inputted network name. */
    public String getNetworkName() {
        return getPreference().getText();
    }

    /** Sets the listener that is triggered on network name change. */
    public void setTextChangeListener(NetworkNameChangeListener listener) {
        mListener = listener;
    }

    @Override
    protected void onStartInternal() {
        if (hasAccessPoint()) {
            getPreference().setText(getAccessPoint().getSsid().toString());
            getPreference().setSelectable(false);
        } else {
            getPreference().setText(null);
        }
    }

    @Override
    protected void updateState(EditTextPreference preference) {
        preference.setSummary(TextUtils.isEmpty(preference.getText()) ? getContext().getString(
                R.string.default_network_name_summary) : preference.getText());
    }

    @Override
    protected boolean handlePreferenceChanged(EditTextPreference preference, Object newValue) {
        preference.setText(newValue.toString());
        if (mListener != null) {
            mListener.onNetworkNameChanged(newValue.toString());
        }
        refreshUi();
        return true;
    }

    /** A listener associated with the network name. */
    public interface NetworkNameChangeListener {
        /**
         * This method is called when the network name has changed. The new name is provided as an
         * argument.
         */
        void onNetworkNameChanged(String newName);
    }
}
