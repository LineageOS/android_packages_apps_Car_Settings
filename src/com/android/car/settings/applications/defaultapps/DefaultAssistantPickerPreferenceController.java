/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.settings.applications.defaultapps;

import android.car.drivingstate.CarUxRestrictions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.service.voice.VoiceInteractionService;
import android.service.voice.VoiceInteractionServiceInfo;
import android.speech.RecognitionService;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.internal.app.AssistUtils;
import com.android.settingslib.applications.DefaultAppInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Business logic for displaying and choosing the default assistant. */
public class DefaultAssistantPickerPreferenceController extends
        DefaultAppsPickerBasePreferenceController {

    private static final Logger LOG = new Logger(DefaultAssistantPickerPreferenceController.class);
    @VisibleForTesting
    static final Intent ASSIST_SERVICE_PROBE = new Intent(
            VoiceInteractionService.SERVICE_INTERFACE);
    @VisibleForTesting
    static final Intent ASSIST_ACTIVITY_PROBE = new Intent(Intent.ACTION_ASSIST);

    private final AssistUtils mAssistUtils;
    private final Map<String, Info> mAvailableAssistants = new LinkedHashMap<>();

    public DefaultAssistantPickerPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mAssistUtils = new AssistUtils(context);
    }

    @Override
    protected List<DefaultAppInfo> getCandidates() {
        mAvailableAssistants.clear();

        List<Info> assistants = new ArrayList<>();
        PackageManager pm = getContext().getPackageManager();
        addAssistServices(pm, assistants);
        addAssistActivities(pm, assistants);

        // Return things to show.
        Set<String> packages = new HashSet<>();
        List<DefaultAppInfo> candidates = new ArrayList<>();
        for (Info info : assistants) {
            if (packages.contains(info.getComponentName().getPackageName())) {
                continue;
            }
            packages.add(info.getComponentName().getPackageName());
            candidates.add(new DefaultAppInfo(getContext(), getContext().getPackageManager(),
                    getCurrentProcessUserId(), info.getComponentName()));
            mAvailableAssistants.put(info.getComponentName().getPackageName(), info);
        }

        return candidates;
    }

    @Override
    protected CharSequence getConfirmationMessage(DefaultAppInfo info) {
        if (info == null) {
            return null;
        }
        return getContext().getString(R.string.assistant_security_warning);
    }

    @Override
    protected String getCurrentDefaultKey() {
        ComponentName cn = getCurrentAssistant();
        if (cn != null) {
            return new DefaultAppInfo(getContext(), getContext().getPackageManager(),
                    getCurrentProcessUserId(), cn).getKey();
        }
        return DefaultAppsPickerBasePreferenceController.NONE_PREFERENCE_KEY;
    }

    @Override
    protected void setCurrentDefault(String key) {
        if (TextUtils.isEmpty(key)) {
            setAssistNone();
            return;
        }

        ComponentName cn = ComponentName.unflattenFromString(key);
        Info info = mAvailableAssistants.getOrDefault(cn.getPackageName(), null);
        if (info == null) {
            setAssistNone();
            return;
        }

        if (info.isVoiceInteractionService()) {
            setAssistService(info);
        } else {
            setAssistActivity(info);
        }
    }

    private ComponentName getCurrentAssistant() {
        return mAssistUtils.getAssistComponentForUser(getCurrentProcessUserId());
    }

    private void addAssistServices(PackageManager pm, List<Info> availableAssistants) {
        List<ResolveInfo> services = pm.queryIntentServices(ASSIST_SERVICE_PROBE,
                PackageManager.GET_META_DATA);
        for (ResolveInfo resolveInfo : services) {
            VoiceInteractionServiceInfo voiceInteractionServiceInfo =
                    new VoiceInteractionServiceInfo(pm, resolveInfo.serviceInfo);
            if (!voiceInteractionServiceInfo.getSupportsAssist()) {
                continue;
            }

            Info info = new Info(new ComponentName(resolveInfo.serviceInfo.packageName,
                    resolveInfo.serviceInfo.name), voiceInteractionServiceInfo);
            availableAssistants.add(info);
        }
    }

    private void addAssistActivities(PackageManager pm, List<Info> availableAssistants) {
        List<ResolveInfo> activities = pm.queryIntentActivities(ASSIST_ACTIVITY_PROBE,
                PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : activities) {
            Info info = new Info(new ComponentName(resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name));
            availableAssistants.add(info);
        }
    }

    private void setAssistNone() {
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.ASSISTANT, "");
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.VOICE_INTERACTION_SERVICE, "");
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.VOICE_RECOGNITION_SERVICE, getDefaultRecognizer());
    }

    private void setAssistService(Info serviceInfo) {
        String serviceComponentName = serviceInfo.getComponentName().flattenToShortString();
        String serviceRecognizerName = new ComponentName(
                serviceInfo.getComponentName().getPackageName(),
                serviceInfo.getVoiceInteractionServiceInfo().getRecognitionService())
                .flattenToShortString();

        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.ASSISTANT, serviceComponentName);
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.VOICE_INTERACTION_SERVICE, serviceComponentName);
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.VOICE_RECOGNITION_SERVICE, serviceRecognizerName);
    }

    private void setAssistActivity(Info activityInfo) {
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.ASSISTANT, activityInfo.getComponentName().flattenToShortString());
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.VOICE_INTERACTION_SERVICE, "");
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.VOICE_RECOGNITION_SERVICE, getDefaultRecognizer());
    }

    private String getDefaultRecognizer() {
        ResolveInfo resolveInfo = getContext().getPackageManager().resolveService(
                new Intent(RecognitionService.SERVICE_INTERFACE),
                PackageManager.GET_META_DATA);
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            LOG.w("Unable to resolve default voice recognition service.");
            return "";
        }

        return new ComponentName(resolveInfo.serviceInfo.packageName,
                resolveInfo.serviceInfo.name).flattenToShortString();
    }

    private static class Info {
        private final ComponentName mComponentName;
        private final VoiceInteractionServiceInfo mVoiceInteractionServiceInfo;

        Info(ComponentName component) {
            mComponentName = component;
            mVoiceInteractionServiceInfo = null;
        }

        Info(ComponentName component, VoiceInteractionServiceInfo voiceInteractionServiceInfo) {
            mComponentName = component;
            mVoiceInteractionServiceInfo = voiceInteractionServiceInfo;
        }

        /** Returns {@code true} if this object represents a voice interaction service. */
        public boolean isVoiceInteractionService() {
            return mVoiceInteractionServiceInfo != null;
        }

        /** Returns the component name. */
        public ComponentName getComponentName() {
            return mComponentName;
        }

        /** Returns the voice interaction service info. */
        public VoiceInteractionServiceInfo getVoiceInteractionServiceInfo() {
            return mVoiceInteractionServiceInfo;
        }
    }
}
