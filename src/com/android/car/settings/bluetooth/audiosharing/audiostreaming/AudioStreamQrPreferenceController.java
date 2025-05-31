/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.car.settings.bluetooth.audiosharing.audiostreaming;

import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;

import com.android.car.settings.R;
import com.android.car.settings.bluetooth.audiosharing.BaseAudioSharingPreferenceController;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.settingslib.bluetooth.BluetoothLeBroadcastMetadataExt;
import com.android.settingslib.qrcode.QrCodeGenerator;

/**
 * Controller for updating the Qr code for {@link AudioStreamQrPreference}.
 */
public class AudioStreamQrPreferenceController extends
        BaseAudioSharingPreferenceController<AudioStreamQrPreference> {
    private static final Logger LOG = new Logger(AudioStreamQrPreferenceController.class);

    public AudioStreamQrPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected void updateState(AudioStreamQrPreference preference) {
        super.updateState(preference);
        updateBroadcastQrCodeInfo();
    }

    private void updateBroadcastQrCodeInfo() {
        BluetoothLeBroadcastMetadata metadata = getCurrentBroadcast();
        if (metadata != null) {
            try {
                String uri = BluetoothLeBroadcastMetadataExt.INSTANCE.toQrCodeString(metadata);
                int size = getContext().getResources().getDimensionPixelSize(
                        R.dimen.bluetooth_audio_streaming_qr_code_size);
                int margin = getContext().getResources().getDimensionPixelSize(
                        R.dimen.qr_code_margin);
                getPreference().setPreferenceInfo(QrCodeGenerator.encodeQrCode(uri, size, margin));
            } catch (Exception e) {
                LOG.e("Couldn't generate audio sharing QR code", e);
            }
        }
    }

    @Override
    protected Class<AudioStreamQrPreference> getPreferenceType() {
        return AudioStreamQrPreference.class;
    }
}
