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

import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

/**
 * Controls launching {@link Fragment} instances and back navigation.
 */
public interface FragmentController {

    /**
     * Launches a Fragment in the main container of the current Activity. This cannot be used to
     * show dialog fragments and will throw an IllegalArgumentException if attempted. The method
     * {@link #showDialog} should be used instead.
     */
    void launchFragment(Fragment fragment);

    /**
     * Pops the top off the Fragment stack.
     */
    void goBack();

    /**
     * Shows a message to inform the user that the current feature is not available when driving.
     */
    void showBlockingMessage();

    /**
     * Shows dialog with given tag.
     */
    void showDialog(DialogFragment dialogFragment, @Nullable String tag);

    /**
     * Finds dialog by tag. This is primarily used to reattach listeners to dialogs after
     * configuration change. This method will return null if the tag references a fragment that
     * isn't a dialog fragment or no dialog with the given tag exists.
     */
    @Nullable
    DialogFragment findDialogByTag(String tag);

    /**
     * Starts an activity for a result. When the result is received, the {@link
     * ActivityResultCallback} is passed the result. Note that the implementer of this interface
     * must ensure that the callback is valid throughout the lifecycle of the new activity that is
     * created.
     *
     * @param intent      The intent used to start an activity.
     * @param requestCode User defined code which is passed to the callback when the activity exits.
     *                    Values must use the first 8 bits of the int (i.e. 0-255).
     * @param callback    Defines how the result from the started activity should be handled.
     */
    void startActivityForResult(Intent intent, int requestCode, ActivityResultCallback callback);
}
