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

# Script to incrementally build and install CarSettings APK.

MODULE_NAME="CarSettings"

# Find the path to the built module.
APK_PATH=$(outmod $MODULE_NAME)

if [[ "$APK_PATH" == */CarSettings.apk ]]; then
    echo "Found APK path: $APK_PATH"
    echo "Building $MODULE_NAME..."
    m $MODULE_NAME && \
    echo "Installing $APK_PATH..." && \
    adb install -r "$APK_PATH"
else
    echo "Error: Could not find a valid APK path for $MODULE_NAME."
    echo "outmod result: $APK_PATH"
    exit 1
fi
