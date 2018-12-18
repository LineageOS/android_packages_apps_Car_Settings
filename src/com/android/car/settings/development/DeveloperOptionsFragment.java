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

package com.android.car.settings.development;

import android.car.userlib.CarUserManagerHelper;
import android.os.Bundle;
import android.widget.Switch;

import androidx.annotation.LayoutRes;
import androidx.annotation.XmlRes;

import com.android.car.settings.R;
import com.android.car.settings.common.SettingsFragment;

/**
 * Fragment which displays all of the developer options. It also has a switch to disable developer
 * options, if desired.
 */
public class DeveloperOptionsFragment extends SettingsFragment {

    private Switch mOnOffSwitch;

    private final EnableDeveloperSettingsWarningDialog.DeveloperSettingsToggleListener mListener =
            new EnableDeveloperSettingsWarningDialog.DeveloperSettingsToggleListener() {
                @Override
                public void onEnableDeveloperSettingsConfirmed() {
                    DevelopmentSettingsUtil.setDevelopmentSettingsEnabled(getContext(), true);
                }

                @Override
                public void onEnableDeveloperSettingsRejected() {
                    mOnOffSwitch.setChecked(false);
                }
            };

    @Override
    @XmlRes
    protected int getPreferenceScreenResId() {
        return R.xml.developer_options_fragment;
    }

    @Override
    @LayoutRes
    protected int getActionBarLayoutId() {
        return R.layout.action_bar_with_toggle;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EnableDeveloperSettingsWarningDialog dialog =
                (EnableDeveloperSettingsWarningDialog) findDialogByTag(
                        EnableDeveloperSettingsWarningDialog.TAG);
        if (dialog != null) {
            dialog.setEnableDeveloperSettingsWarningListener(mListener);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mOnOffSwitch = requireActivity().findViewById(R.id.toggle_switch);

        CarUserManagerHelper carUserManagerHelper = new CarUserManagerHelper(getContext());
        mOnOffSwitch.setChecked(DevelopmentSettingsUtil.isDevelopmentSettingsEnabled(getContext(),
                carUserManagerHelper));
        mOnOffSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (isChecked) {
                        EnableDeveloperSettingsWarningDialog dialog =
                                new EnableDeveloperSettingsWarningDialog();
                        dialog.setEnableDeveloperSettingsWarningListener(mListener);
                        DeveloperOptionsFragment.this.showDialog(dialog,
                                EnableDeveloperSettingsWarningDialog.TAG);
                    } else {
                        DevelopmentSettingsUtil.setDevelopmentSettingsEnabled(getContext(), false);
                    }
                });
    }
}
