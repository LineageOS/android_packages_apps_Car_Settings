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

package com.android.car.settings.bluetooth;

import android.annotation.NonNull;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserManager;
import android.text.TextUtils;

import com.android.car.settings.R;
import com.android.car.settings.common.Logger;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/**
 * Code drop from {@link com.android.settings.bluetooth.RequestPermissionActivity}.
 *
 * This {@link Activity} handles requests to toggle Bluetooth by collecting user
 * consent and waiting until the state change is completed.
 */
public class BluetoothRequestPermissionActivity extends Activity implements
        DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
    private static final Logger LOG = new Logger(BluetoothRequestPermissionActivity.class);
    private static final int REQUEST_UNKNOWN = 0;
    private static final int REQUEST_ENABLE = 1;
    private static final int REQUEST_DISABLE = 2;
    private int mRequest;
    @NonNull
    private CharSequence mAppLabel;
    private LocalBluetoothAdapter mLocalBluetoothAdapter;
    private LocalBluetoothManager mLocalBluetoothManager;
    private AlertDialog mDialog;
    private StateChangeReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(Activity.RESULT_CANCELED);

        mRequest = parseIntent();
        if (mRequest == REQUEST_UNKNOWN) {
            finish();
            return;
        }

        mReceiver = new StateChangeReceiver();

        int btState = mLocalBluetoothAdapter.getState();
        switch (mRequest) {
            case REQUEST_DISABLE:
                switch (btState) {
                    case BluetoothAdapter.STATE_OFF:
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        proceedAndFinish();
                        break;

                    case BluetoothAdapter.STATE_ON:
                    case BluetoothAdapter.STATE_TURNING_ON:
                        createDecisionDialog();
                        break;

                    default:
                        LOG.e("Unknown adapter state: " + btState);
                        finish();
                        break;
                }
                break;
            case REQUEST_ENABLE:
                switch (btState) {
                    case BluetoothAdapter.STATE_OFF:
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        createDecisionDialog();
                        break;

                    case BluetoothAdapter.STATE_ON:
                        proceedAndFinish();
                        break;
                    default:
                        LOG.e("Unknown adapter state: " + btState);
                        finish();
                        break;
                }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mReceiver.register();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mReceiver.unregister();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                proceedAndFinish();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                onCancel(/* dialog = */ null);
                break;
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    private void createInterimDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(/* context = */ this);

        switch (mRequest) {
            case REQUEST_ENABLE:
                builder.setMessage(getString(R.string.bluetooth_turning_on));
                break;
            default:
                builder.setMessage(getString(R.string.bluetooth_turning_off));
                break;
        }
        builder.setCancelable(false).setOnCancelListener(/* listener = */ this);

        mDialog = builder.create();
        mDialog.show();
    }

    private void createDecisionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(/* context= */ this);
        switch (mRequest) {
            case REQUEST_ENABLE:
                builder.setMessage(
                        mAppLabel != null ? getString(R.string.bluetooth_ask_enablement,
                                mAppLabel)
                                : getString(R.string.bluetooth_ask_enablement_no_name));
                break;

            case REQUEST_DISABLE: {
                builder.setMessage(
                        mAppLabel != null ? getString(R.string.bluetooth_ask_disablement, mAppLabel)
                                : getString(R.string.bluetooth_ask_disablement_no_name));
                break;
            }
        }

        builder.setPositiveButton(R.string.allow, this::decisionDialogPositiveButtonListener)
                .setNegativeButton(R.string.deny, (dialog, which) -> onCancel(/* dialog = */ null))
                .setOnCancelListener(/* listener = */ this);

        mDialog = builder.create();
        mDialog.show();
    }

    private void decisionDialogPositiveButtonListener(DialogInterface dialog, int which) {
        dialog.dismiss();

        if (!hasUserRestriction()) {
            switch (mRequest) {
                case REQUEST_ENABLE:
                    if (mLocalBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                        proceedAndFinish();
                    } else {
                        // If BT is not up yet, show "Turning on Bluetooth..."
                        createInterimDialog();
                    }
                    break;

                case REQUEST_DISABLE:
                    if (mLocalBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                        proceedAndFinish();
                    } else {
                        // If BT is not up yet, show "Turning off Bluetooth..."
                        createInterimDialog();
                    }
                    break;

                default:
                    finish();
                    break;
            }
        }
    }

    private boolean hasUserRestriction() {
        switch (mRequest) {
            case REQUEST_ENABLE:
                UserManager userManager = getSystemService(UserManager.class);
                if (userManager.hasUserRestriction(UserManager.DISALLOW_BLUETOOTH)) {
                    // If Bluetooth is disallowed, don't try to enable it, show policy
                    // transparency
                    // message instead.
                    DevicePolicyManager dpm = getSystemService(DevicePolicyManager.class);
                    Intent intent = dpm.createAdminSupportIntent(
                            UserManager.DISALLOW_BLUETOOTH);
                    if (intent != null) {
                        startActivity(intent);
                    }

                    return true;
                } else {
                    mLocalBluetoothAdapter.enable();
                }
                break;

            case REQUEST_DISABLE: {
                mLocalBluetoothAdapter.disable();
            }
            break;
        }

        return false;
    }

    private void proceedAndFinish() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
        finish();
    }

    private int parseIntent() {
        int request = REQUEST_UNKNOWN;
        Intent intent = getIntent();
        if (intent == null) {
            return REQUEST_UNKNOWN;
        }

        switch (intent.getAction()) {
            case BluetoothAdapter.ACTION_REQUEST_ENABLE:
                request = REQUEST_ENABLE;
                break;
            case BluetoothAdapter.ACTION_REQUEST_DISABLE:
                request = REQUEST_DISABLE;
                break;
            default:
                LOG.e("Error: this activity may be started only with intent "
                        + BluetoothAdapter.ACTION_REQUEST_ENABLE);
                return REQUEST_UNKNOWN;
        }

        String packageName = getCallingPackage();
        if (TextUtils.isEmpty(packageName)) {
            packageName = getIntent().getStringExtra(Intent.EXTRA_PACKAGE_NAME);
        }
        if (!TextUtils.isEmpty(packageName)) {
            try {
                ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(
                        packageName, 0);
                mAppLabel = applicationInfo.loadLabel(getPackageManager());
            } catch (PackageManager.NameNotFoundException e) {
                LOG.e("Couldn't find app with package name " + packageName);
                return REQUEST_UNKNOWN;
            }
        }

        mLocalBluetoothManager = LocalBluetoothManager.getInstance(
                getApplicationContext(), /* onInitCallback= */ null);
        if (mLocalBluetoothManager == null) {
            LOG.e("Bluetooth is not supported on this device");
            return REQUEST_UNKNOWN;
        }

        mLocalBluetoothAdapter = mLocalBluetoothManager.getBluetoothAdapter();
        if (mLocalBluetoothAdapter == null) {
            LOG.e("Error: there's a problem starting Bluetooth");
            return REQUEST_UNKNOWN;
        }

        return request;
    }

    private final class StateChangeReceiver extends BroadcastReceiver {
        private static final long TOGGLE_TIMEOUT_MILLIS = 10000; // 10 sec

        StateChangeReceiver() {
            getWindow().getDecorView().postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    onCancel(null);
                }
            }, TOGGLE_TIMEOUT_MILLIS);
        }

        public void register() {
            registerReceiver(/* receiver= */ this,
                    new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        }

        public void unregister() {
            unregisterReceiver(/* receiver= */ this);
        }

        public void onReceive(Context context, Intent intent) {
            Activity activity = BluetoothRequestPermissionActivity.this;
            if (intent == null) {
                return;
            }
            int currentState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothDevice.ERROR);
            switch (mRequest) {
                case REQUEST_ENABLE:
                    if (currentState == BluetoothAdapter.STATE_ON) {
                        activity.setResult(Activity.RESULT_OK);
                        proceedAndFinish();
                    }
                    break;

                case REQUEST_DISABLE:
                    if (currentState == BluetoothAdapter.STATE_OFF) {
                        activity.setResult(Activity.RESULT_OK);
                        proceedAndFinish();
                    }
                    break;
            }
        }
    }
}
