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

package com.android.car.settings.displaycompat;

 import static com.android.car.oem.tokens.Token.applyOemTokenStyle;
 import static com.android.systemui.car.Flags.displayCompatibilityV2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.car.settings.R;

/**
 * Activity to show a dialog for setting the Aspect Ratio.
 */
public class CarAspectRatioDialogActivity extends Activity {
    private static final String TAG = "CarAspectRatioADA";
    private static final String ACTION_SHOW_DIALOG =
            "com.android.car.settings.aspectRatio.action.SHOW_DIALOG";
    private static final String EXTRA_KEY_COMPONENT_NAME =
            "com.android.car.settings.aspectRatio.extra.COMPONENT_NAME";
    private static final String EXTRA_KEY_USER_ID =
            "com.android.car.settings.aspectRatio.extra.USER_ID";
    private CarAspectRatioDialogHelper mHelper;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!displayCompatibilityV2()) {
            finish();
            return;
        }

        if (!ACTION_SHOW_DIALOG.equals(getIntent().getAction())) {
            Log.e(TAG, "Incorrect action sent to the receiver: " + getIntent().getAction());
            finish();
            return;
        }
        ComponentName componentName =
                getIntent().getParcelableExtra(EXTRA_KEY_COMPONENT_NAME, ComponentName.class);
        if (componentName == null) {
            Log.e(TAG, "No component sent in extra with key: " + EXTRA_KEY_COMPONENT_NAME);
            finish();
            return;
        }
        int userId = getIntent().getIntExtra(EXTRA_KEY_USER_ID, this.getUserId());

        applyOemTokenStyle(this);

        setContentView(R.layout.aspect_ratio_dialog_activity);
        if (mHelper == null) {
            mHelper = new CarAspectRatioDialogHelper(this);
        }
        View dialogView = findViewById(R.id.aspect_ratio_dialog);
        if (dialogView == null) {
            throw new IllegalStateException(
                    "Aspect Ratio Dialog requires the dialog container with id "
                            + "\"aspect_ratio_dialog\".");
        }
        mHelper.setupAspectRatioDialog(dialogView, componentName, userId, () -> finish());
    }
}
