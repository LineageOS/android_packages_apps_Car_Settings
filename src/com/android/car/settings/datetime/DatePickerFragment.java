/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.car.settings.datetime;

import android.app.timedetector.ManualTimeSuggestion;
import android.app.timedetector.TimeDetector;
import android.content.Intent;
import android.os.Bundle;
import android.widget.DatePicker;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;

import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;

import java.util.Calendar;

/**
 * Sets the system date.
 */
public class DatePickerFragment extends BaseFragment {
    private static final int MILLIS_IN_SECOND = 1000;

    private DatePicker mDatePicker;

    @Override
    public void onStop() {
        super.onStop();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, mDatePicker.getYear());
        c.set(Calendar.MONTH, mDatePicker.getMonth());
        c.set(Calendar.DAY_OF_MONTH, mDatePicker.getDayOfMonth());
        long when = Math.max(c.getTimeInMillis(), DatetimeSettingsFragment.MIN_DATE);
        if (when / MILLIS_IN_SECOND < Integer.MAX_VALUE) {
            TimeDetector timeDetector =
                    getContext().getSystemService(TimeDetector.class);
            ManualTimeSuggestion manualTimeSuggestion =
                    TimeDetector.createManualTimeSuggestion(when, "Settings: Set date");
            timeDetector.suggestManualTime(manualTimeSuggestion);
            getContext().sendBroadcast(new Intent(Intent.ACTION_TIME_CHANGED));
        }
    }

    @Override
    @LayoutRes
    protected int getLayoutId() {
        return R.layout.date_picker;
    }

    @Override
    @StringRes
    protected int getTitleId() {
        return R.string.date_picker_title;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mDatePicker = getView().findViewById(R.id.date_picker);
    }
}
