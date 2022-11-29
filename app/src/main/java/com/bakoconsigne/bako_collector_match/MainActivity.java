package com.bakoconsigne.bako_collector_match;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;
import com.bakoconsigne.bako_collector_match.exceptions.BadRequestException;
import com.bakoconsigne.bako_collector_match.exceptions.InternalServerException;
import com.bakoconsigne.bako_collector_match.exceptions.UnauthorizedException;
import com.bakoconsigne.bako_collector_match.services.ArduinoService;
import com.bakoconsigne.bako_collector_match.services.CollectorService;
import com.bakoconsigne.bako_collector_match.utils.Utils;
import com.dantsu.escposprinter.connection.usb.UsbConnection;
import me.aflak.arduino.Arduino;
import me.aflak.arduino.ArduinoListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity implements ArduinoListener {

    public static final String LOGGER_TAG = "BAKO_COLLECTOR_MATCH";

    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    public static final String ACTION_USB_DEVICE_ATTACHED = "com.android.example.USB_PERMISSION";

    public static final String ERROR = "Une erreur est survenue.";

    public static final String ERROR_CONNECTION = "Problème de connexion internet : merci de vérifier le Wifi ou l'internet mobile";

    public static final String ERROR_BAD_REQUEST = "Bad request : merci de ré-essayer. Si le souci persiste, contacter le support Bako";

    public static final String ERROR_SERVER = "Erreur serveur : merci de ré-essayer. Si le souci persiste, contacter le support Bako.";

    public static final String ERROR_UNAUTHORIZED = "Non autorisé : merci de contacter le support Bako";

    public static final String ERROR_SETTINGS = "Erreur : merci de configurer l'application";

    public static final String SETTING_TOKEN_NAME = "bako_token";

    public static final String SETTING_SITE_NAME = "bako_siteId";

    public static final String SETTING_ARDUINO_DEBUG = "bako_arduino_debug";

    public static final String SETTING_DISABLE_CHECK_WEIGHT = "bako_disable_check_weight";

    public static final String SETTING_CHECK_WEIGHT_GREATER_ONLY = "bako_check_weight_greater_only";

    public static final int TIMER_DELAY_LONG = 30000;

    public static final int TIMER_DELAY_MEDIUM = 20000;

    public static final int TIMER_DELAY_SHORT = 6000;

    private static final int MAX_CHARACTER_IN_POST = 1048576;

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
        //        ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.BLUETOOTH_CONNECT }, 2);

        ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.BLUETOOTH }, 2);

        //        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
        //            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        //                ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.BLUETOOTH_CONNECT }, 2);
        //            }
        //        }

        // Use this check to determine whether Bluetooth classic is supported on the device.
        // Then you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, "bluetooth_not_supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "ble_not_supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        //        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
        //            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH}, 2);
        //        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
        //            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 3);
        //        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        //            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        //        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
        //            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 5);
        //        }

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
            } catch (InternalServerException e) {
                runOnUiThread(() -> Utils.alertError(MainActivity.this, ERROR_SERVER));
            } catch (BadRequestException e) {
                runOnUiThread(() -> Utils.alertError(MainActivity.this, ERROR_BAD_REQUEST));
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

        Button buttonSendLogcat = findViewById(R.id.button_send_logcat);
        buttonSendLogcat.setOnClickListener(v -> {
            new Thread(() -> {
                try {
                    if (CollectorService.getInstance().monitoring(-1, getLogcat(), true)) {
                        runOnUiThread(() -> displayTextView.append("Log envoyée."));
                    } else {
                        runOnUiThread(() -> displayTextView.append("Log non envoyée - error"));
                    }
                } catch (IOException | UnauthorizedException | BadRequestException | InternalServerException e) {
                    runOnUiThread(() -> displayTextView.append("error : " + e.getMessage()));
                }
            }).start();
        });

        Thread.setDefaultUncaughtExceptionHandler((paramThread, paramThrowable) -> {
            if (!(paramThrowable instanceof UnauthorizedException)) {
                new Thread(() -> {
                    try {
                        CollectorService.getInstance().monitoring(-1, Log.getStackTraceString(paramThrowable), true);
                    } catch (IOException e) {
                        Log.e(LOGGER_TAG, e.getMessage());
                    }
                }).start();
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                Log.e(LOGGER_TAG, e.getMessage());
            }
            //Catch your exception
            // Without System.exit() this will not work.
            System.exit(2);
        });
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
        SharedPreferences sharedPreferences      = PreferenceManager.getDefaultSharedPreferences(this);
        String            token                  = sharedPreferences.getString(SETTING_TOKEN_NAME, null);
        String            siteId                 = sharedPreferences.getString(SETTING_SITE_NAME, null);
        boolean           disableCheckWight      = sharedPreferences.getBoolean(SETTING_DISABLE_CHECK_WEIGHT, false);
        boolean           checkWeightGreaterOnly = sharedPreferences.getBoolean(SETTING_CHECK_WEIGHT_GREATER_ONLY, false);

        if (token == null || siteId == null || "".equals(token.trim()) || "".equals(siteId.trim())) {
            Utils.alertError(MainActivity.this, ERROR_SETTINGS);
        }

        CollectorService.getInstance().setSiteId(siteId);
        CollectorService.getInstance().setToken(token);
        CollectorService.getInstance().setDisableCheckWeight(disableCheckWight);
        CollectorService.getInstance().setCheckWeightGreaterOnly(checkWeightGreaterOnly);

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

        // for tests
        //        printUsb();
    }

    @Override
    public void onArduinoAttached(UsbDevice device) {
        CharSequence text = "Arduino attached...";
        display(text.toString());
        int   duration = Toast.LENGTH_SHORT;
        Toast toast    = Toast.makeText(MainActivity.this, text, duration);
        toast.show();
        Log.i(LOGGER_TAG, "coucou onArduinoAttached " + device.getDeviceName() + " - " + device.getDeviceClass());
        if ("micro printer".equals(device.getProductName())) {
            return;
        }
        arduino.open(device);
    }

    @Override
    public void onArduinoDetached() {
        CharSequence text = "Arduino detached...";
        display(text.toString());
        int   duration = Toast.LENGTH_SHORT;
        Toast toast    = Toast.makeText(MainActivity.this, text, duration);
        toast.show();
    }

    @Override
    public void onArduinoMessage(byte[] bytes) {
        String message = new String(bytes);
        display(message);

        this.arduinoService.onMessageReceived(message);

        this.monitoring(message);
    }

    @Override
    public void onArduinoOpened() {
        String str = "arduino opened...";
        display(str);
    }

    @Override
    public void onUsbPermissionDenied() {
        runOnUiThread(() -> {
            int   duration = Toast.LENGTH_SHORT;
            Toast toast    = Toast.makeText(MainActivity.this, "Permission denied. Attempting again in 3 sec...", duration);
            toast.show();
        });
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

    private void monitoring(final String messageArduino) {

        final IntentFilter intentFilter  = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        final Intent       batteryStatus = MainActivity.this.registerReceiver(null, intentFilter);
        final int          level         = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        final int          scale         = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        final float        batteryPct    = level * 100 / (float) scale;

        new Thread(() -> {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            new Thread(() -> {
                try {
                    Log.d(LOGGER_TAG, "Send status for monitoring battery=" + batteryPct + " - messageFromArduino=" + messageArduino);
                    CollectorService collectorService = CollectorService.getInstance();
                    collectorService.monitoring(batteryPct, messageArduino, false);
                } catch (UnauthorizedException e) {
                    runOnUiThread(() -> Utils.alertError(MainActivity.this, ERROR_UNAUTHORIZED));
                } catch (IOException | BadRequestException | InternalServerException ignored) {
                }
            }).start();
        }).start();
    }


    /*==============================================================================================
    ===========================================USB PART=============================================
    ==============================================================================================*/

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(LOGGER_TAG, "coucou usbReceiver - action=" + action);
            display("usbReceiver - action=" + action);
            if (MainActivity.ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                    UsbDevice  usbDevice  = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    String usbDeviceStr =
                        "coucou2 usbDevice=" + usbDevice.getDeviceName() + " deviceId=" + usbDevice.getDeviceId() + " vendorId=" + usbDevice.getVendorId();
                    display(usbDeviceStr);
                    Log.i(LOGGER_TAG, usbDeviceStr);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (usbManager != null && usbDevice != null) {

                        }
                    }
                }
            }
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                synchronized (this) {
                    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                    UsbDevice  usbDevice  = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    String usbDeviceStr =
                        "ACTION_USB_DEVICE_ATTACHED usbDevice=" + usbDevice.getDeviceName() + " deviceId=" + usbDevice.getDeviceId() + " vendorId="
                            + usbDevice.getVendorId();
                    display(usbDeviceStr);
                    Log.i(LOGGER_TAG, usbDeviceStr);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (usbManager != null && usbDevice != null) {

                        }
                    }
                }
            }
        }
    };

    public void printUsb() {

        Log.i(LOGGER_TAG, "coucou printUsb");
        //        UsbConnection usbConnection = UsbPrintersConnections.selectFirstConnected(this);
        UsbManager    usbManager    = (UsbManager) this.getSystemService(Context.USB_SERVICE);
        UsbConnection usbConnection = null;
        if (usbManager.getDeviceList().size() > 0) {
            usbConnection = new UsbConnection(usbManager, usbManager.getDeviceList().get(usbManager.getDeviceList().keySet().stream().findFirst().get()));
        }

        AtomicReference<String> listUsb = new AtomicReference<>("");
        usbManager.getDeviceList()
                  .forEach((s, usbDevice) -> {
                      listUsb.set(listUsb + " s=" + s + " usbDevice=" + usbDevice.getDeviceName() + " - deviceClass=" + usbDevice.getDeviceClass()
                                      + " - InterfaceCount=" + usbDevice.getInterfaceCount()
                                      + " - Interface(0)=" + usbDevice.getInterface(0));
                  });

        new AlertDialog.Builder(this)
            .setTitle("USB Connection")
            .setMessage("usbConnection=" + usbConnection +
                            " - usbManager=" + usbManager +
                            " - listUsbSize=" + usbManager.getDeviceList().size() +
                            " - listUsb=" + listUsb)
            .show();

        if (usbConnection == null || usbManager == null) {
            new AlertDialog.Builder(this)
                .setTitle("USB Connection")
                .setMessage("No USB found.")
                .show();
            return;
        }

        PendingIntent permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            new Intent(MainActivity.ACTION_USB_PERMISSION),
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0
        );
        IntentFilter filter = new IntentFilter(MainActivity.ACTION_USB_PERMISSION);
        this.registerReceiver(this.usbReceiver, filter);
        usbManager.requestPermission(usbConnection.getDevice(), permissionIntent);

        IntentFilter filterUsbAttached = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(usbReceiver, filterUsbAttached);
    }

    private String getLogcat() {
        try {
            final Process        process        = Runtime.getRuntime().exec("logcat -d");
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            final StringBuilder log  = new StringBuilder();
            String              line = "";
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line);
            }

            if (log.length() > MAX_CHARACTER_IN_POST) {
                return log.substring(log.length() - MAX_CHARACTER_IN_POST, log.length() - 1);
            } else {
                return log.toString();
            }

        } catch (IOException e) {
            Log.e(LOGGER_TAG, e.getMessage());
            return null;
        }
    }

}
