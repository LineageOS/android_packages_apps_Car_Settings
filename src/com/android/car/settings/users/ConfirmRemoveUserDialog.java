package com.android.car.settings.users;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import com.android.car.settings.R;

/**
 * Dialog to confirm user removal.
 */
public class ConfirmRemoveUserDialog extends DialogFragment implements
        DialogInterface.OnClickListener {
    private static final String DIALOG_TAG = "ConfirmRemoveUserDialog";
    private ConfirmRemoveUserListener mListener;

    /**
     * Interface for listeners that want to receive a callback when user confirms user removal in a
     * dialog.
     */
    public interface ConfirmRemoveUserListener {
        void onRemoveUserConfirmed();
    }

    /**
     * Shows the dialog.
     *
     * @param parent Fragment associated with the dialog.
     */
    public void show(Fragment parent) {
        setTargetFragment(parent, 0);
        show(parent.getFragmentManager(), DIALOG_TAG);
    }

    /**
     * Registers a listener for OnRemoveUserConfirmed that will get called if user confirms
     * the dialog.
     *
     * @param listener Instance of {@link ConfirmRemoveUserListener} to call when confirmed.
     */
    public void registerConfirmRemoveUserListener(ConfirmRemoveUserListener listener) {
        mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.really_remove_user_title)
                .setMessage(R.string.really_remove_user_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete_button, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mListener != null) {
            mListener.onRemoveUserConfirmed();
        }
    }
}