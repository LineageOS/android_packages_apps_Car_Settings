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

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.preference.PreferenceViewHolder;

import com.android.car.settings.R;
import com.android.car.ui.preference.CarUiPreference;

/**
 * A Preference used to show the QR code for joining the broadcast stream.
 */
public class AudioStreamQrPreference extends CarUiPreference {

    private Bitmap mImage;

    public AudioStreamQrPreference(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public AudioStreamQrPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AudioStreamQrPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AudioStreamQrPreference(Context context) {
        this(context, null);
    }

    private void init() {
        setLayoutResource(R.layout.audio_stream_qr_preference);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        ImageView qrCode = (ImageView) holder.findViewById(R.id.audio_sharing_qr_code);
        if (qrCode != null && mImage != null) {
            qrCode.setImageBitmap(mImage);
        }
    }

    /**
     * Sets information for this preference.
     */
    public void setPreferenceInfo(Bitmap bmp) {
        mImage = bmp;
        notifyChanged();
    }
}
