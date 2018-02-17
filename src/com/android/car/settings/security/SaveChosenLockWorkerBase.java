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
 * limitations under the License
 */

package com.android.car.settings.security;

import android.annotation.WorkerThread;
import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.android.internal.widget.LockPatternUtils;

/**
 * An invisible retained worker fragment to track the AsyncWork that saves
 * the chosen lock credential (pattern/pin/password).
 */
abstract class SaveChosenLockWorkerBase extends Fragment {

    public static final String EXTRA_KEY_PATTERN = "pattern";
    public static final String EXTRA_KEY_PASSWORD = "password";

    private Listener mListener;
    private boolean mFinished;
    private Intent mResultData;

    protected LockPatternUtils mUtils;
    protected int mUserId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    /**
     * Set the listener to get callback when finished saving the chosen lock.
     */
    public void setListener(Listener listener) {
        if (mListener == listener) {
            return;
        }

        mListener = listener;
        if (mFinished && mListener != null) {
            mListener.onChosenLockSaveFinished(mResultData);
        }
    }

    /**
     * Set the initial state for the async task.
     */
    protected void prepare(LockPatternUtils utils, int userId) {
        mUtils = utils;
        mUserId = userId;

        mFinished = false;
        mResultData = null;
    }

    /**
     * Start executing the async task.
     */
    protected void start() {
        new Task().execute();
    }

    /**
     * Executes the save and verify work in background.
     */
    @WorkerThread
    protected abstract Intent saveAndVerifyInBackground();

    /**
     * Send result data via the listener when task finishes.
     */
    protected void finish(Intent resultData) {
        mFinished = true;
        mResultData = resultData;
        if (mListener != null) {
            mListener.onChosenLockSaveFinished(mResultData);
        }
    }

    // Save chosen lock task.
    private class Task extends AsyncTask<Void, Void, Intent> {
        @Override
        protected Intent doInBackground(Void... params){
            return saveAndVerifyInBackground();
        }

        @Override
        protected void onPostExecute(Intent resultData) {
            finish(resultData);
        }
    }

    /**
     * Call back when finishing save the chosen lock.
     */
    interface Listener {
        public void onChosenLockSaveFinished(Intent resultData);
    }
}