/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.car.settings.search;

import static com.android.car.settings.common.PreferenceXmlParser.METADATA_KEY;
import static com.android.car.settings.common.PreferenceXmlParser.MetadataFlag.FLAG_NEED_KEY;

import android.content.Context;
import android.provider.SearchIndexableResource;

import androidx.annotation.NonNull;
import androidx.annotation.XmlRes;

import com.android.car.settings.common.Logger;
import com.android.car.settings.common.PreferenceXmlParser;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexableRaw;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A basic SearchIndexProvider.
 */
public class CarBaseSearchIndexProvider implements Indexable.SearchIndexProvider {
    private static final Logger LOG = new Logger(CarBaseSearchIndexProvider.class);

    private final int mXmlRes;
    private final String mIntentAction;
    private final String mIntentClass;

    public CarBaseSearchIndexProvider(@XmlRes int xmlRes, String intentAction) {
        mXmlRes = xmlRes;
        mIntentAction = intentAction;
        mIntentClass = null;
    }

    public CarBaseSearchIndexProvider(@XmlRes int xmlRes, @NonNull Class intentClass) {
        mXmlRes = xmlRes;
        mIntentAction = null;
        mIntentClass = intentClass.getName();
    }

    @Override
    public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
        SearchIndexableResource sir = new SearchIndexableResource(context);
        sir.xmlResId = mXmlRes;
        sir.intentAction = mIntentAction;
        sir.intentTargetPackage = context.getPackageName();
        sir.intentTargetClass = mIntentClass;
        return Collections.singletonList(sir);
    }

    @Override
    public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
        return null;
    }

    @Override
    public List<SearchIndexableRaw> getDynamicRawDataToIndex(Context context, boolean enabled) {
        return null;
    }

    @Override
    public List<String> getNonIndexableKeys(Context context) {
        if (!isPageSearchEnabled(context)) {
            try {
                return PreferenceXmlParser.extractMetadata(context, mXmlRes, FLAG_NEED_KEY)
                        .stream()
                        .map(bundle -> bundle.getString(METADATA_KEY))
                        .collect(Collectors.toList());
            } catch (IOException | XmlPullParserException e) {
                LOG.w("Error parsing non-indexable XML - " + mXmlRes);
            }
        }

        return null;
    }

    /**
     * Returns true if the page should be considered in search query. If return false, entire page
     * will be suppressed during search query.
     */
    protected boolean isPageSearchEnabled(Context context) {
        return true;
    }
}
