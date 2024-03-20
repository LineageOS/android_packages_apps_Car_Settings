/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.car.settings.display;

import static android.car.settings.CarSettings.Global.FORCED_DAY_NIGHT_MODE;

import android.car.drivingstate.CarUxRestrictions;
import android.car.feature.Flags;
import android.car.settings.CarSettings;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;

import com.android.car.settings.R;
import com.android.car.settings.common.BaseActionItem;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.MultiActionPreference;
import com.android.car.settings.common.PreferenceController;
import com.android.car.settings.common.ToggleButtonActionItem;

/**
 * PreferenceController for the day/night theme mode.
 */
public class ThemeTogglePreferenceController extends PreferenceController<MultiActionPreference> {
    static final int FORCED_SENSOR_MODE = 0;
    static final int FORCED_DAY_MODE = 1;
    static final int FORCED_NIGHT_MODE = 2;

    private static final Uri THEME_MODE_URI = Settings.Global.getUriFor(
            CarSettings.Global.FORCED_DAY_NIGHT_MODE);
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ContentObserver mThemeToggleObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            refreshUi();
        }
    };

    public ThemeTogglePreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController,
            CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();
        getPreference().setSelectable(false);
        if (getAutoButton() != null) {
            getAutoButton().setDrawable(getThemeModeIcon(FORCED_SENSOR_MODE));
            getAutoButton().setOnClickListener(isChecked -> {
                if (isChecked) {
                    setForcedDayNightModeSetting(FORCED_SENSOR_MODE);
                }
                refreshUi();
            });
        }
        if (getDayButton() != null) {
            getDayButton().setDrawable(getThemeModeIcon(FORCED_DAY_MODE));
            getDayButton().setOnClickListener(isChecked -> {
                if (isChecked) {
                    setForcedDayNightModeSetting(FORCED_DAY_MODE);
                }
                refreshUi();
            });
        }
        if (getNightButton() != null) {
            getNightButton().setDrawable(getThemeModeIcon(FORCED_NIGHT_MODE));
            getNightButton().setOnClickListener(isChecked -> {
                if (isChecked) {
                    setForcedDayNightModeSetting(FORCED_NIGHT_MODE);
                }
                refreshUi();
            });
        }
    }

    @Override
    protected void onStartInternal() {
        super.onStartInternal();
        getContext().getContentResolver().registerContentObserver(THEME_MODE_URI,
                /* notifyForDescendants= */ false, mThemeToggleObserver);
    }

    @Override
    protected void onStopInternal() {
        super.onStopInternal();
        getContext().getContentResolver().unregisterContentObserver(mThemeToggleObserver);
    }

    @Override
    protected void updateState(MultiActionPreference preference) {
        if (getAutoButton() != null) {
            boolean checked = getForcedDayNightModeSetting() == FORCED_SENSOR_MODE;
            getAutoButton().setChecked(checked);
            getAutoButton().setEnabled(!checked);
        }
        if (getDayButton() != null) {
            boolean checked = getForcedDayNightModeSetting() == FORCED_DAY_MODE;
            getDayButton().setChecked(checked);
            getDayButton().setEnabled(!checked);
        }
        if (getNightButton() != null) {
            boolean checked = getForcedDayNightModeSetting() == FORCED_NIGHT_MODE;
            getNightButton().setChecked(checked);
            getNightButton().setEnabled(!checked);
        }
    }

    @Override
    protected Class<MultiActionPreference> getPreferenceType() {
        return MultiActionPreference.class;
    }

    @Override
    public int getDefaultAvailabilityStatus() {
        if (!Flags.carNightGlobalSetting()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @VisibleForTesting
    ToggleButtonActionItem getAutoButton() {
        BaseActionItem item = getPreference().getActionItem(
                MultiActionPreference.ActionItem.ACTION_ITEM1);
        if (item instanceof ToggleButtonActionItem) {
            return (ToggleButtonActionItem) item;
        }
        return null;
    }

    @VisibleForTesting
    ToggleButtonActionItem getDayButton() {
        BaseActionItem item = getPreference().getActionItem(
                MultiActionPreference.ActionItem.ACTION_ITEM2);
        if (item instanceof ToggleButtonActionItem) {
            return (ToggleButtonActionItem) item;
        }
        return null;
    }

    @VisibleForTesting
    ToggleButtonActionItem getNightButton() {
        BaseActionItem item = getPreference().getActionItem(
                MultiActionPreference.ActionItem.ACTION_ITEM3);
        if (item instanceof ToggleButtonActionItem) {
            return (ToggleButtonActionItem) item;
        }
        return null;
    }

    private int getForcedDayNightModeSetting() {
        return Settings.Global.getInt(getContext().getContentResolver(),
                FORCED_DAY_NIGHT_MODE, FORCED_SENSOR_MODE);
    }

    private void setForcedDayNightModeSetting(int mode) {
        Settings.Global.putInt(getContext().getContentResolver(), FORCED_DAY_NIGHT_MODE, mode);
    }

    private Drawable getThemeModeIcon(int mode) {
        return switch (mode) {
            case FORCED_SENSOR_MODE -> getContext().getDrawable(
                    R.drawable.ic_theme_toggle_auto_button);
            case FORCED_DAY_MODE -> getContext().getDrawable(R.drawable.ic_theme_toggle_day_button);
            case FORCED_NIGHT_MODE -> getContext().getDrawable(
                    R.drawable.ic_theme_toggle_night_button);
            default -> null;
        };
    }
}
