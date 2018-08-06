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

package com.android.car.settings.bluetooth;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.android.car.settings.common.Logger;

/**
 * Shared {@link ViewModel} which exposes Bluetooth information to Bluetooth settings Fragments.
 */
public class BluetoothViewModel extends AndroidViewModel {

    private static final Logger LOG = new Logger(BluetoothViewModel.class);

    private BluetoothStateLiveData mState;
    private BluetoothDiscoveryStateLiveData mDiscoveryState;

    public BluetoothViewModel(@NonNull Application application) {
        super(application);
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            LOG.w("Bluetooth is not supported on this device");
            return;
        }
        mState = new BluetoothStateLiveData(application);
        mDiscoveryState = new BluetoothDiscoveryStateLiveData(application);
    }

    /**
     * Returns a {@link LiveData} which updates with the current {@link BluetoothAdapter} state.
     * Returns {@code null} if Bluetooth is not supported.
     *
     * @see BluetoothAdapter#getState()
     */
    @Nullable
    public LiveData<Integer> getBluetoothState() {
        return mState;
    }

    /**
     * Returns a {@link LiveData} which is {@code true} when the {@link BluetoothAdapter} has
     * started the remote device discovery process and {@code false} when the process is finished.
     * Returns {@code null} if Bluetooth is not supported.
     *
     * @see BluetoothAdapter#ACTION_DISCOVERY_STARTED
     * @see BluetoothAdapter#ACTION_DISCOVERY_FINISHED
     */
    @Nullable
    public LiveData<Boolean> getBluetoothDiscoveryState() {
        return mDiscoveryState;
    }
}
