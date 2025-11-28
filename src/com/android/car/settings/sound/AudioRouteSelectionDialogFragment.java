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

import androidx.window.embedding.ActivityEmbeddingController;

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
    private CarUiRadioButtonListItemAdapter mAdapter;
    private final List<CarUiRadioButtonListItem> mItemList = new ArrayList<>();

    public AudioRouteSelectionDialogFragment(Context context) {
        mContext = context;
        mUsage = context.getResources().getInteger(R.integer.audio_route_selector_usage);
        mAudioRoutesManager = new AudioRoutesManager(context, mUsage);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAudioRoutesManager != null) {
            mAudioRoutesManager.tearDown();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mAudioRoutesManager.setAudioRoutesUpdateListener(this::onRoutesUpdated);
        mAdapter = new CarUiRadioButtonListItemAdapter(mItemList);

        AlertDialogBuilder builder = new AlertDialogBuilder(requireActivity())
                .setTitle(mContext.getString(R.string.audio_route_selector_title))
                .setSingleChoiceItems(mAdapter)
                .setNeutralButton(R.string.audio_route_dialog_neutral_button_text,
                        /* listener */ null);
        mAlertDialog = builder.create();

        return mAlertDialog;
    }

    private void onRoutesUpdated(List<AudioRouteItem> routes) {
        mItemList.clear();
        for (AudioRouteItem route : routes) {
            String address = route.getAddress();
            CarUiRadioButtonListItem item = new CarUiRadioButtonListItem();
            item.setTitle(mAudioRoutesManager.getDeviceName(address));
            item.setOnItemClickedListener(l -> mAudioRoutesManager.setUnicast(address));
            if (address.equals(mAudioRoutesManager.getOutputAddress())) {
                item.setChecked(true);
            }
            mItemList.add(item);
        }
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        boolean isEmbeddedDialog = ActivityEmbeddingController.getInstance(getActivity())
                .isActivityEmbedded(getActivity());
        if (!isEmbeddedDialog) {
            // finishing an embedded activity will finish all Settings activities, including the
            // homepage activity on the left pane.
            getActivity().finish();
        }
    }
}
