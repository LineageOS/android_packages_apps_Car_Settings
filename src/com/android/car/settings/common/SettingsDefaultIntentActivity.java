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

package com.android.car.settings.common;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.android.car.settings.R;

/**
 * Activity class that shows a toast. This is mainly for hosting all the intents that do not invoke
 * corresponding functions on AAOS.
 */
public class SettingsDefaultIntentActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null) {
            showToastAndFinishActivity();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showToastAndFinishActivity();
    }

    private void showToastAndFinishActivity() {
        Toast toast = Toast.makeText(getApplicationContext(),
                R.string.unsupported_intents_toast_text, Toast.LENGTH_LONG);
        toast.show();

        finish();
    }
}
