#!/bin/bash

# Copyright (C) 2026 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Script to launch any Car Settings fragment in dual-pane mode via DeepLinkHomepageActivity.

FRAGMENT_CLASS=$1
HIGHLIGHT_KEY=$2

if [ -z "$FRAGMENT_CLASS" ]; then
    echo "Error: No fragment class provided."
    echo "Usage: $0 <FRAGMENT_CLASS_NAME> [OPTIONAL_HIGHLIGHT_MENU_KEY]"
    exit 1
fi

# Target Intent URI for SubSettingsActivity.
# ACTION_MAIN is required to trigger the dual-pane logic in BaseCarSettingsActivity.
TARGET_INTENT_URI="intent:#Intent;action=android.intent.action.MAIN;component=com.android.car.settings/com.android.car.settings.common.SubSettingsActivity;S.key_sub_settings_fragment=$FRAGMENT_CLASS;end"

echo "Launching fragment: $FRAGMENT_CLASS"

# Standard extra key from android.provider.Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_INTENT_URI
# NOTE: It is 'EMBEDDED', not 'EMBED'.
INTENT_URI_KEY="android.provider.extra.SETTINGS_EMBEDDED_DEEP_LINK_INTENT_URI"
HIGHLIGHT_KEY_EXTRA="android.provider.extra.SETTINGS_EMBEDDED_DEEP_LINK_HIGHLIGHT_MENU_KEY"

# We pass 'key_sub_settings_fragment' as a direct extra to ensure it is preserved
# if DeepLinkHomepageActivity uses replaceExtras().
EXTRA_ARGS="--es $INTENT_URI_KEY '$TARGET_INTENT_URI' \
            --es key_sub_settings_fragment '$FRAGMENT_CLASS'"

if [ -n "$HIGHLIGHT_KEY" ]; then
    echo "Highlighting menu key: $HIGHLIGHT_KEY"
    EXTRA_ARGS="$EXTRA_ARGS --es $HIGHLIGHT_KEY_EXTRA '$HIGHLIGHT_KEY'"
fi

# Start DeepLinkHomepageActivity with trampoline intent.
adb shell am start -a android.settings.SETTINGS_EMBED_DEEP_LINK_ACTIVITY \
    $EXTRA_ARGS
