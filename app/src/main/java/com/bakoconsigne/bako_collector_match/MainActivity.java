package com.bakoconsigne.bako_collector_match;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.usb.UsbDevice;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;
import com.bakoconsigne.bako_collector_match.exceptions.UnauthorizedException;
import com.bakoconsigne.bako_collector_match.services.ArduinoService;
import com.bakoconsigne.bako_collector_match.services.CollectorService;
import com.bakoconsigne.bako_collector_match.utils.Utils;
import me.aflak.arduino.Arduino;
import me.aflak.arduino.ArduinoListener;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements ArduinoListener {

    public static final String LOGGER_TAG = "BAKO_COLLECTOR_MATCH";

    public static final String ERROR_CONNECTION = "Problème de connexion internet : merci de vérifier le Wifi ou l'internet mobile";

    public static final String ERROR_UNAUTHORIZED = "Non autorisé : merci de contacter le support Bako";

    public static final String ERROR_SETTINGS = "Erreur : merci de configurer l'application";

    public static final String SETTING_TOKEN_NAME = "bako_token";

    public static final String SETTING_SITE_NAME = "bako_siteId";

    public static final String SETTING_ARDUINO_DEBUG = "bako_arduino_debug";

    public static final int TIMER_DELAY_LONG = 60000;
    public static final int TIMER_DELAY_SHORT = 15000;

    /////////////
    // arduino //
    /////////////

    private final ArduinoService arduinoService = ArduinoService.getInstance();

    private Arduino arduino;

    TextView displayTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        ActivityCompat.requestPermissions(MainActivity.this,
                                          new String[] { Manifest.permission.CAMERA },
                                          1);

        if (!isNetworkAvailable()) {
            Utils.alertError(MainActivity.this, ERROR_CONNECTION);
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String            token             = sharedPreferences.getString(SETTING_TOKEN_NAME, null);
        String            siteId            = sharedPreferences.getString(SETTING_SITE_NAME, null);

        if (token == null || siteId == null || "".equals(token.trim()) || "".equals(siteId.trim())) {
            Utils.alertError(MainActivity.this, ERROR_SETTINGS);
        }

        new Thread(() -> {
            try {
                CollectorService collectorService = CollectorService.getInstance(token, siteId);
                collectorService.loadListBoxType();
            } catch (UnauthorizedException e) {
                runOnUiThread(() -> Utils.alertError(MainActivity.this, ERROR_UNAUTHORIZED));
            } catch (IOException e) {
                runOnUiThread(() -> Utils.alertError(MainActivity.this, ERROR_CONNECTION));
            }
        }).start();

        arduino = new Arduino(this);
        arduinoService.setArduino(arduino);

        displayTextView = findViewById(R.id.textView_arduino_debug_message);
        displayTextView.setMovementMethod(new ScrollingMovementMethod());

        Button buttonOpen = findViewById(R.id.button_openArduino);
        buttonOpen.setOnClickListener(v -> arduinoService.open());

        Button buttonClose = findViewById(R.id.button_closeArduino);
        buttonClose.setOnClickListener(v -> arduinoService.close());

        Button buttonStatus = findViewById(R.id.button_statusArduino);
        buttonStatus.setOnClickListener(v -> arduinoService.status());

        Button buttonCheck = findViewById(R.id.button_checkArduino);
        buttonCheck.setOnClickListener(v -> arduinoService.check());

        EditText editTextSendArduino = findViewById(R.id.editText_sendArduino);
        Button   buttonSendArduino   = findViewById(R.id.button_sendArduino);
        buttonSendArduino.setOnClickListener(v -> this.arduinoService.send(editTextSendArduino.getText().toString()));
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.updateSharedInformation();
        arduino.setArduinoListener(this);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network             nw                  = connectivityManager.getActiveNetwork();
        if (nw == null) {
            return false;
        }
        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
        return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
    }

    private void updateSharedInformation() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String            token             = sharedPreferences.getString(SETTING_TOKEN_NAME, null);
        String            siteId            = sharedPreferences.getString(SETTING_SITE_NAME, null);
        if (token == null || siteId == null || "".equals(token.trim()) || "".equals(siteId.trim())) {
            Utils.alertError(MainActivity.this, ERROR_SETTINGS);
        }

        CollectorService.getInstance().setSiteId(siteId);
        CollectorService.getInstance().setToken(token);

        boolean     arduinoDebug        = sharedPreferences.getBoolean(SETTING_ARDUINO_DEBUG, false);
        EditText    editTextSendArduino = findViewById(R.id.editText_sendArduino);
        Button      buttonSendArduino   = findViewById(R.id.button_sendArduino);
        Button      buttonOpen          = findViewById(R.id.button_openArduino);
        Button      buttonClose         = findViewById(R.id.button_closeArduino);
        Button      buttonStatus        = findViewById(R.id.button_statusArduino);
        Button      buttonCheck         = findViewById(R.id.button_checkArduino);
        FrameLayout layoutDebug         = findViewById(R.id.frameLayout_debugMode);
        if (arduinoDebug) {
            layoutDebug.setVisibility(View.VISIBLE);
            layoutDebug.bringToFront();
            displayTextView.setVisibility(View.VISIBLE);
            editTextSendArduino.setVisibility(View.VISIBLE);
            buttonSendArduino.setVisibility(View.VISIBLE);
            buttonOpen.setVisibility(View.VISIBLE);
            buttonClose.setVisibility(View.VISIBLE);
            buttonStatus.setVisibility(View.VISIBLE);
            buttonCheck.setVisibility(View.VISIBLE);
        } else {
            layoutDebug.setVisibility(View.GONE);
            displayTextView.setVisibility(View.GONE);
            editTextSendArduino.setVisibility(View.GONE);
            buttonSendArduino.setVisibility(View.GONE);
            buttonOpen.setVisibility(View.GONE);
            buttonClose.setVisibility(View.GONE);
            buttonStatus.setVisibility(View.GONE);
            buttonCheck.setVisibility(View.GONE);
        }
    }

    @Override
    public void onArduinoAttached(UsbDevice device) {
        CharSequence text = "Arduino attached...";
        display(text.toString());
        int   duration = Toast.LENGTH_SHORT;
        Toast toast    = Toast.makeText(getApplicationContext(), text, duration);
        toast.show();
        arduino.open(device);
    }

    @Override
    public void onArduinoDetached() {
        CharSequence text = "Arduino detached...";
        display(text.toString());
        int   duration = Toast.LENGTH_SHORT;
        Toast toast    = Toast.makeText(getApplicationContext(), text, duration);
        toast.show();
    }

    @Override
    public void onArduinoMessage(byte[] bytes) {
        String message = new String(bytes);
        display(message);
        int duration = Toast.LENGTH_SHORT;
        runOnUiThread(() -> {
            Toast toast = Toast.makeText(getApplicationContext(), message, duration);
            toast.show();
        });

        this.arduinoService.onMessageReceived(message);
    }

    @Override
    public void onArduinoOpened() {
        String str = "arduino opened...";
        display(str);
        int   duration = Toast.LENGTH_SHORT;
        Toast toast    = Toast.makeText(getApplicationContext(), str, duration);
        toast.show();
        arduino.send(str.getBytes());
    }

    @Override
    public void onUsbPermissionDenied() {
        int   duration = Toast.LENGTH_SHORT;
        Toast toast    = Toast.makeText(getApplicationContext(), "Permission denied. Attempting again in 3 sec...", duration);
        toast.show();
        new Handler().postDelayed(() -> arduino.reopen(), 3000);
    }

    private void display(final String message) {
        runOnUiThread(() -> displayTextView.append(message + "\n"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        arduino.unsetArduinoListener();
        arduino.close();
    }
}
