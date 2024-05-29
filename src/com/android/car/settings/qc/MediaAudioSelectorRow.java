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

package com.android.car.settings.qc;

import static com.android.car.settings.qc.SettingsQCRegistry.MEDIA_AUDIO_SELECTOR_URI;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;

import com.android.car.qc.QCItem;
import com.android.car.qc.QCList;
import com.android.car.qc.QCRow;
import com.android.car.settings.R;
import com.android.car.settings.sound.AudioRouteSelectionActivity;
import com.android.car.settings.sound.AudioRoutesManager;

/**
 * Quick control for showing a media audio route selector.
 */
public class MediaAudioSelectorRow extends SettingsQCItem {

    private AudioRoutesManager mAudioRoutesManager;

    public MediaAudioSelectorRow(Context context) {
        super(context);
        mAudioRoutesManager = new AudioRoutesManager(context, context.getResources().getInteger(
                R.integer.audio_route_selector_usage));
    }

    @Override
    QCItem getQCItem() {
        if (isHiddenForZone()) {
            return null;
        }
        if (mAudioRoutesManager.getAudioRouteList().size() < 2) {
            return null;
        }

        QCList.Builder listBuilder = new QCList.Builder();
        listBuilder.addRow(new QCRow.Builder()
                .setTitle(getContext().getString(R.string.audio_route_selector_title))
                .setSubtitle(mAudioRoutesManager.getDeviceNameForAddress(
                        mAudioRoutesManager.getActiveDeviceAddress()))
                .setIcon(Icon.createWithResource(getContext(), R.drawable.ic_qc_speaker_group))
                .setPrimaryAction(getPrimaryAction())
                .build()
        );
        return listBuilder.build();
    }

    PendingIntent getPrimaryAction() {
        Intent intent = new Intent();
        intent.setAction(AudioRouteSelectionActivity.INTENT_ACTION);
        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), /* requestCode= */ 0,
                intent, PendingIntent.FLAG_IMMUTABLE, null);
        return pendingIntent;
    }

    @Override
    Uri getUri() {
        return MEDIA_AUDIO_SELECTOR_URI;
    }

    @Override
    Class getBackgroundWorkerClass() {
        return MediaAudioSelectorRowWorker.class;
    }
}
