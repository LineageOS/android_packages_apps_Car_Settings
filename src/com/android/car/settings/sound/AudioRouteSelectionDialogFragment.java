/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.car.settings.sound;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import com.android.car.settings.R;
import com.android.car.ui.AlertDialogBuilder;
import com.android.car.ui.preference.CarUiDialogFragment;
import com.android.car.ui.recyclerview.CarUiRadioButtonListItem;
import com.android.car.ui.recyclerview.CarUiRadioButtonListItemAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog fragment for selecting the media audio routes.
 */
public class AudioRouteSelectionDialogFragment extends CarUiDialogFragment {
    private Context mContext;
    private AlertDialog mAlertDialog;
    private AudioRoutesManager mAudioRoutesManager;
    private int mUsage;

    public AudioRouteSelectionDialogFragment(Context context) {
        mContext = context;
        mUsage = context.getResources().getInteger(R.integer.audio_route_selector_usage);
        mAudioRoutesManager = new AudioRoutesManager(context, mUsage);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        List<String> addressList = mAudioRoutesManager.getAudioRouteList();
        List<CarUiRadioButtonListItem> itemList = new ArrayList<>();
        for (String address : addressList) {
            CarUiRadioButtonListItem item = new CarUiRadioButtonListItem();
            item.setTitle(mAudioRoutesManager.getDeviceNameForAddress(address));
            item.setOnItemClickedListener(l -> mAudioRoutesManager.updateAudioRoute(address));
            itemList.add(item);
            if (address.equals(mAudioRoutesManager.getActiveDeviceAddress())) {
                item.setChecked(true);
            }
        }
        CarUiRadioButtonListItemAdapter adapter = new CarUiRadioButtonListItemAdapter(itemList);

        AlertDialogBuilder builder = new AlertDialogBuilder(requireActivity())
                .setTitle(mContext.getString(R.string.audio_route_selector_title))
                .setSingleChoiceItems(adapter)
                .setNeutralButton(R.string.audio_route_dialog_neutral_button_text,
                        /* listener */ null);
        mAlertDialog = builder.create();

        return mAlertDialog;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        getActivity().finish();
    }
}
