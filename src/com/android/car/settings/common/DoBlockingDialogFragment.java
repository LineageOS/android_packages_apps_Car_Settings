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
package com.android.car.settings.common;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import androidx.car.app.CarAlertDialog;

import com.android.car.settings.R;

/**
 * A dialog to block non-distrction optimized view when restriction is applied.
 */
public class DoBlockingDialogFragment extends DialogFragment {
    public static final String DIALOG_TAG = "block_dialog_tag";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getContext();
        return new CarAlertDialog.Builder(context)
                .setBody(context.getString(R.string.restricted_while_driving))
                .setPositiveButton(context.getString(R.string.okay),
                        /* listener= */ null)
                .create();
    }
}
