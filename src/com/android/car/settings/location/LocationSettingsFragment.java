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

package com.android.car.settings.location;

import android.content.Context;
import android.os.Bundle;
import android.widget.Switch;

import androidx.annotation.LayoutRes;
import androidx.annotation.XmlRes;

import com.android.car.settings.R;
import com.android.car.settings.common.BasePreferenceFragment;

/**
 * Main page that hosts Location related preferences.
 */
public class LocationSettingsFragment extends BasePreferenceFragment {
    private LocationController mLocationController;
    private Switch mLocationSwitch;

    @Override
    @XmlRes
    protected int getPreferenceScreenResId() {
        return R.xml.location_settings_fragment;
    }

    @Override
    @LayoutRes
    protected int getActionBarLayoutId() {
        return R.layout.action_bar_with_toggle;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mLocationController = new LocationController(context, enabled ->
                mLocationSwitch.setChecked(enabled));
        getLifecycle().addObserver(mLocationController);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mLocationSwitch = requireActivity().findViewById(R.id.toggle_switch);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateLocationSwitch();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getLifecycle().removeObserver(mLocationController);
    }

    // Update the location master switch's state upon starting the fragment.
    private void updateLocationSwitch() {
        mLocationSwitch.setChecked(mLocationController.isEnabled());
        mLocationSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                mLocationController.setLocationEnabled(isChecked));
    }
}
