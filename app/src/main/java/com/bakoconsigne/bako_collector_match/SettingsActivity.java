package com.bakoconsigne.bako_collector_match;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import com.bakoconsigne.bako_collector_match.exceptions.BadRequestException;
import com.bakoconsigne.bako_collector_match.exceptions.UnauthorizedException;
import com.bakoconsigne.bako_collector_match.services.CollectorService;
import com.bakoconsigne.bako_collector_match.utils.Utils;

import java.io.IOException;
import java.net.UnknownHostException;

import static com.bakoconsigne.bako_collector_match.MainActivity.ERROR_CONNECTION;
import static com.bakoconsigne.bako_collector_match.MainActivity.LOGGER_TAG;

public class SettingsActivity extends AppCompatActivity {

    private static final String ERROR_LOGIN = "Erreur : le login ou mot de passe est incorrect";

    private static final String ERROR_PERMISSION = "Erreur : vous n'avez pas les droits suffisants";

    final Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        alertLogin();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.fragment_settings_preferences, rootKey);
        }
    }

    /**
     * Open Alert Login
     */
    public void alertLogin() {
        LayoutInflater li          = LayoutInflater.from(context);
        View           promptsView = li.inflate(R.layout.prompt, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        final EditText userInput     = promptsView.findViewById(R.id.editText_login);
        final EditText passwordInput = promptsView.findViewById(R.id.editText_password);

        alertDialogBuilder.setTitle("Authentification")
                          .setMessage("Merci de vous authentifier")

                          // Specifying a listener allows you to take an action before dismissing the dialog.
                          // The dialog is automatically dismissed when a dialog button is clicked.
                          .setPositiveButton(android.R.string.ok, (dialog, which) -> new Thread(() -> {
                              try {
                                  if (!CollectorService.getInstance().loginAndIsAdmin(userInput.getText().toString(), passwordInput.getText().toString())) {
                                      runOnUiThread(() -> Utils.alertError(SettingsActivity.this, ERROR_PERMISSION, (dialog1, which1) -> goToMainActivity(null)));
                                  }
                                  runOnUiThread(() -> getWindow().setBackgroundDrawableResource(android.R.color.background_light));
                              } catch (UnknownHostException e) {
                                  runOnUiThread(() -> Utils.alertError(SettingsActivity.this, ERROR_CONNECTION, (dialog1, which1) -> goToMainActivity(null)));
                              } catch (IOException e) {
                                  Log.e(LOGGER_TAG, "Error", e);
                                  runOnUiThread(() -> Utils.alertError(SettingsActivity.this, e.getMessage(), (dialog1, which1) -> goToMainActivity(null)));
                              } catch (UnauthorizedException | BadRequestException e) {
                                  runOnUiThread(() -> Utils.alertError(SettingsActivity.this, ERROR_LOGIN, (dialog1, which1) -> goToMainActivity(null)));
                              }
                          }).start())

                          // A null listener allows the button to dismiss the dialog and take no further action.
                          .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                              // Continue with delete operation
                              goToMainActivity(null);
                          })
                          .setOnCancelListener(dialog -> goToMainActivity(null))
                          .setIcon(android.R.drawable.ic_dialog_alert)
                          .show();
    }

    public void goToMainActivity(View view) {
        Intent myIntent = new Intent(SettingsActivity.this, MainActivity.class);
        SettingsActivity.this.startActivity(myIntent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
