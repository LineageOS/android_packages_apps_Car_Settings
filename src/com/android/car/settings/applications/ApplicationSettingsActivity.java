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
package com.android.car.settings.applications;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import com.android.car.settings.common.ListSettingsActivity;
import com.android.car.settings.common.TypedPagedListAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists all installed applications and their summary.
 */
public class ApplicationSettingsActivity extends ListSettingsActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public ArrayList<TypedPagedListAdapter.LineItem> getLineItems() {
        PackageManager pm = getPackageManager();
        Intent intent= new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent,
                PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
                        | PackageManager.MATCH_DISABLED_COMPONENTS);
        ArrayList<TypedPagedListAdapter.LineItem> items = new ArrayList<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            items.add(new ApplicationLineItem(this, pm, resolveInfo));
        }
        return items;
    }
}
