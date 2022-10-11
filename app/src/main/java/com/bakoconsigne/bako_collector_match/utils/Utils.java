package com.bakoconsigne.bako_collector_match.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

/**
 * Utils class
 */
public class Utils {

    /**
     * Open Alert popin
     *
     * @param activity The current {@link Activity}
     * @param message The message
     */
    public static void alertError(Activity activity, final String message) {
        alertError(activity, message, null);
    }

    /**
     * Open Alert popin
     *
     * @param activity The current {@link Activity}
     * @param message The message
     * @param listener Click Listener
     */
    public static void alertError(Activity activity, final String message, final DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(activity)
            .setTitle("Erreur")
            .setMessage(message)

            //            // Specifying a listener allows you to take an action before dismissing the dialog.
            //            // The dialog is automatically dismissed when a dialog button is clicked.
            //            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            //                public void onClick(DialogInterface dialog, int which) {
            //                    // Continue with delete operation
            //                }
            //            })

            // A null listener allows the button to dismiss the dialog and take no further action.
            .setNegativeButton(android.R.string.cancel, listener)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }
}
