/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.car.settings.bluetooth.audiosharing;

import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastMetadata;

import androidx.annotation.NonNull;

/**
 * Base class that implements all methods of the callbacks API. This allows the creation of listener
 * classes that only implement some of the methods, and leave the rest as no-op.
 */
public abstract class BaseLeBroadcastCallback implements BluetoothLeBroadcast.Callback {
    @Override
    public void onBroadcastStarted(int reason, int broadcastId) {}

    @Override
    public void onBroadcastStartFailed(int reason) {}

    @Override
    public void onBroadcastStopped(int reason, int broadcastId) {}

    @Override
    public void onBroadcastStopFailed(int reason) {}

    @Override
    public void onPlaybackStarted(int reason, int broadcastId) {}

    @Override
    public void onPlaybackStopped(int reason, int broadcastId) {}

    @Override
    public void onBroadcastUpdated(int reason, int broadcastId) {}

    @Override
    public void onBroadcastUpdateFailed(int reason, int broadcastId) {}

    @Override
    public void onBroadcastMetadataChanged(int broadcastId,
            @NonNull BluetoothLeBroadcastMetadata metadata) {}
}
