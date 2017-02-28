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
 * limitations under the License.
 */
package com.android.car.settings;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

import com.android.settingslib.drawer.ProfileSelectDialog;
import com.android.settingslib.drawer.SettingsDrawerActivity;
import com.android.settingslib.drawer.Tile;

/**
 * Base activity class for car settings
 */
public class CarSettingActivity extends SettingsDrawerActivity {
    private static final String TAG = "CarSettingActivity";
    private static final String CAR_PACKAGE = "android.car.settings.SETTINGS";
    private static final String SETTING_PKG = "com.android.car.settings";

    @Override
    public String getSettingAction() {
        return CAR_PACKAGE;
    }

    @Override
    public String getSettingPkg() {
        return SETTING_PKG;
    }

    public boolean openTile(Tile tile) {
        if (tile == null) {
            Intent intent = new Intent(getSettingAction()).addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        }
        try {
            ProfileSelectDialog.updateUserHandlesIfNeeded(this /* context */, tile);
            int numUserHandles = tile.userHandle.size();
            if (numUserHandles > 1) {
                ProfileSelectDialog.show(getFragmentManager(), tile);
                return false;
            } else if (numUserHandles == 1) {
                // Show menu on top level items.
                tile.intent.putExtra(EXTRA_SHOW_MENU, true);
                tile.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivityAsUser(tile.intent, tile.userHandle.get(0));
            } else {
                // Show menu on top level items.
                tile.intent.putExtra(EXTRA_SHOW_MENU, true);
                tile.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(tile.intent);
            }
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Couldn't find tile " + tile.intent, e);
        }
        return true;
    }
}
