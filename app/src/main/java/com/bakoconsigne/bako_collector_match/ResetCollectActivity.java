package com.bakoconsigne.bako_collector_match;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.bakoconsigne.bako_collector_match.exceptions.BadRequestException;
import com.bakoconsigne.bako_collector_match.exceptions.InternalServerException;
import com.bakoconsigne.bako_collector_match.exceptions.UnauthorizedException;
import com.bakoconsigne.bako_collector_match.services.CollectorService;
import com.bakoconsigne.bako_collector_match.utils.Utils;

import java.io.IOException;
import java.net.UnknownHostException;

import static com.bakoconsigne.bako_collector_match.MainActivity.ERROR_BAD_REQUEST;
import static com.bakoconsigne.bako_collector_match.MainActivity.ERROR_CONNECTION;
import static com.bakoconsigne.bako_collector_match.MainActivity.ERROR_SERVER;
import static com.bakoconsigne.bako_collector_match.MainActivity.ERROR_UNAUTHORIZED;
import static com.bakoconsigne.bako_collector_match.MainActivity.LOGGER_TAG;

public class ResetCollectActivity extends AppCompatActivity {

    private static final String ERROR_LOGIN = "Erreur : le login ou mot de passe est incorrect";

    private static final String ERROR_PERMISSION = "Erreur : vous n'avez pas les droits suffisants";

    final Context context = this;

    Button buttonReset;

    TextView textViewSuccess;

    CheckBox checkBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_reset_collect);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        alertLogin();

        this.buttonReset = findViewById(R.id.activity_resetCollect_button);
        this.buttonReset.setOnClickListener(v -> resetCollector());

        this.checkBox = findViewById(R.id.activity_resetCollect_checkBox);

        this.textViewSuccess = findViewById(R.id.activity_resetCollect_textView_success);
        this.textViewSuccess.setVisibility(View.GONE);
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
                                  if (!CollectorService.getInstance().loginAndIsAdminOrCollector(userInput.getText().toString(), passwordInput.getText().toString())) {
                                      runOnUiThread(() -> Utils.alertError(ResetCollectActivity.this, ERROR_PERMISSION, (dialog1, which1) -> goToMainActivity(null)));
                                  }
                                  runOnUiThread(() -> getWindow().setBackgroundDrawableResource(android.R.color.background_light));
                              } catch (UnknownHostException e) {
                                  runOnUiThread(() -> Utils.alertError(ResetCollectActivity.this, ERROR_CONNECTION, (dialog1, which1) -> goToMainActivity(null)));
                              } catch (IOException | InternalServerException e) {
                                  Log.e(LOGGER_TAG, "Error", e);
                                  runOnUiThread(() -> Utils.alertError(ResetCollectActivity.this, e.getMessage(), (dialog1, which1) -> goToMainActivity(null)));
                              } catch (UnauthorizedException | BadRequestException e) {
                                  runOnUiThread(() -> Utils.alertError(ResetCollectActivity.this, ERROR_LOGIN, (dialog1, which1) -> goToMainActivity(null)));
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
        Intent myIntent = new Intent(ResetCollectActivity.this, MainActivity.class);
        ResetCollectActivity.this.startActivity(myIntent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    public void resetCollector() {

        androidx.appcompat.app.AlertDialog alertDialog = new androidx.appcompat.app.AlertDialog.Builder(context).create(); //Read Update
        alertDialog.setTitle("Confirmation");
        alertDialog.setMessage("Etes-vous sûr de remettre à zéro le collecteur ?");

        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Confirmer", (dialog, which) -> {
            CheckBox changeTicket = findViewById(R.id.activity_resetCollect_checkBox);

            new Thread(() -> {
                try {
                    if (!CollectorService.getInstance().resetStock(changeTicket.isChecked())) {
                        runOnUiThread(() -> {
                            int   duration = Toast.LENGTH_SHORT;
                            Toast toast    = Toast.makeText(this, ERROR_CONNECTION, duration);
                            toast.show();
                        });
                    } else {
                        runOnUiThread(() -> {
                            this.buttonReset.setVisibility(View.GONE);
                            this.checkBox.setVisibility(View.GONE);
                            textViewSuccess.setVisibility(View.VISIBLE);
                        });
                    }
                } catch (UnauthorizedException e) {
                    runOnUiThread(() -> Utils.alertError(this, ERROR_UNAUTHORIZED));
                } catch (IOException e) {
                    runOnUiThread(() -> Utils.alertError(this, ERROR_CONNECTION));
                } catch (BadRequestException e) {
                    runOnUiThread(() -> Utils.alertError(this, ERROR_BAD_REQUEST));
                } catch (InternalServerException e) {
                    runOnUiThread(() -> Utils.alertError(this, ERROR_SERVER));
                }
            }).start();
        });

        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Annuler", (dialog, which) -> {
            alertDialog.cancel();
        });

        alertDialog.show();  //<-- See This!
    }
}
