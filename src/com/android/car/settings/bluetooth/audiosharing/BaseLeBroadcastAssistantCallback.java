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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;

import androidx.annotation.NonNull;

/**
 * Base class that implements all methods of the callbacks API. This allows the creation of listener
 * classes that only implement some of the methods, and leave the rest as no-op.
 */
public class BaseLeBroadcastAssistantCallback implements BluetoothLeBroadcastAssistant.Callback {
    @Override
    public void onSearchStarted(int reason) {}

    @Override
    public void onSearchStartFailed(int reason) {}

    @Override
    public void onSearchStopped(int reason) {}

    @Override
    public void onSearchStopFailed(int reason) {}

    @Override
    public void onSourceFound(@NonNull BluetoothLeBroadcastMetadata source) {}

    @Override
    public void onSourceAdded(@NonNull BluetoothDevice sink, int sourceId, int reason) {}

    @Override
    public void onSourceAddFailed(@NonNull BluetoothDevice sink,
            @NonNull BluetoothLeBroadcastMetadata source, int reason) {}

    @Override
    public void onSourceModified(@NonNull BluetoothDevice sink, int sourceId, int reason) {}

    @Override
    public void onSourceModifyFailed(@NonNull BluetoothDevice sink, int sourceId, int reason) {}

    @Override
    public void onSourceRemoved(@NonNull BluetoothDevice sink, int sourceId, int reason) {}

    @Override
    public void onSourceRemoveFailed(@NonNull BluetoothDevice sink, int sourceId, int reason) {}

    @Override
    public void onReceiveStateChanged(@NonNull BluetoothDevice sink, int sourceId,
            @NonNull BluetoothLeBroadcastReceiveState state) {}
}
