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

import static com.android.car.settings.common.PreferenceXmlParser.METADATA_CONTROLLER;
import static com.android.car.settings.common.PreferenceXmlParser.METADATA_KEY;

import android.annotation.NonNull;
import android.annotation.XmlRes;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper to load {@link BasePreferenceController} instances from XML. Based on com.android
 * .settings.core.PreferenceControllerListHelper.
 *
 * @deprecated Use {@link PreferenceControllerListHelper2}.
 */
@Deprecated
class PreferenceControllerListHelper {

    private static final Logger LOG = new Logger(PreferenceControllerListHelper.class);

    /**
     * Creates a list of {@link BasePreferenceController} instances from the XML defined by
     * {@param xmlResId}.
     */
    @NonNull
    static List<BasePreferenceController> getPreferenceControllersFromXml(Context context,
            @XmlRes int xmlResId, FragmentController fragmentController) {
        List<BasePreferenceController> controllers = new ArrayList<>();
        List<Bundle> preferenceMetadata;
        try {
            preferenceMetadata = PreferenceXmlParser.extractMetadata(context, xmlResId,
                    PreferenceXmlParser.MetadataFlag.FLAG_NEED_KEY
                            | PreferenceXmlParser.MetadataFlag.FLAG_NEED_PREF_CONTROLLER);
        } catch (IOException | XmlPullParserException e) {
            LOG.e("Failed to parse preference XML for getting controllers", e);
            return controllers;
        }

        for (Bundle metadata : preferenceMetadata) {
            String controllerName = metadata.getString(METADATA_CONTROLLER);
            if (TextUtils.isEmpty(controllerName)) {
                continue;
            }
            String key = metadata.getString(METADATA_KEY);
            if (TextUtils.isEmpty(key)) {
                LOG.w("Controller requires key but it's not defined in XML: " + controllerName);
                continue;
            }
            BasePreferenceController controller;
            try {
                controller = BasePreferenceController.createInstance(context, controllerName, key,
                        fragmentController);
            } catch (IllegalStateException e2) {
                LOG.e("Cannot instantiate controller from reflection: " + controllerName, e2);
                continue;
            }
            controllers.add(controller);
        }

        return controllers;
    }
}
