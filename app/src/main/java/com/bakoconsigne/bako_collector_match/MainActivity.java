package com.bakoconsigne.bako_collector_match;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
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
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.usb.UsbConnection;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;
import me.aflak.arduino.Arduino;
import me.aflak.arduino.ArduinoListener;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity implements ArduinoListener {

    public static final String LOGGER_TAG = "BAKO_COLLECTOR_MATCH";

    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

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

    private String status;

    private boolean nextMessageForMonitoring = false;

    private Handler mHandlerMonitoring;

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

        this.mHandlerMonitoring = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.updateSharedInformation();
        arduino.setArduinoListener(this);
        this.monitoring();
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
        int duration = Toast.LENGTH_SHORT;
        runOnUiThread(() -> {
            Toast toast = Toast.makeText(MainActivity.this, message, duration);
            toast.show();
        });

        if (nextMessageForMonitoring) {
            this.status += message;
            nextMessageForMonitoring = false;
        }

        if (message.contains("status")) {
            this.status = message;
            nextMessageForMonitoring = true;
        }

        this.arduinoService.onMessageReceived(message);
    }

    @Override
    public void onArduinoOpened() {
        String str = "arduino opened...";
        display(str);
        int   duration = Toast.LENGTH_SHORT;
        Toast toast    = Toast.makeText(MainActivity.this, str, duration);
        toast.show();
        arduino.send(str.getBytes());
    }

    @Override
    public void onUsbPermissionDenied() {
        int   duration = Toast.LENGTH_SHORT;
        Toast toast    = Toast.makeText(MainActivity.this, "Permission denied. Attempting again in 3 sec...", duration);
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

    private void monitoring() {

        final IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

        Runnable runnableCode = new Runnable() {
            @Override
            public void run() {
                MainActivity.this.arduinoService.status();

                final Intent batteryStatus = MainActivity.this.registerReceiver(null, intentFilter);
                final int    level         = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                final int    scale         = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                final float batteryPct = level * 100 / (float) scale;

                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                new Thread(() -> {
                    try {

                        Log.d(LOGGER_TAG, "Send status for monitoring battery=" + batteryPct + " - status=" + MainActivity.this.status);
                        CollectorService collectorService = CollectorService.getInstance();
                        collectorService.monitoring(batteryPct, MainActivity.this.status);
                    } catch (UnauthorizedException e) {
                        runOnUiThread(() -> Utils.alertError(MainActivity.this, ERROR_UNAUTHORIZED));
                    } catch (IOException ignored) {
                    }
                }).start();

                // Repeat this the same runnable code block again another 2 seconds
                // 'this' is referencing the Runnable object
                mHandlerMonitoring.postDelayed(this, 300000); // 5 min
            }
        };
        // Start the initial runnable task by posting through the handler
        mHandlerMonitoring.post(runnableCode);

    }


    /*==============================================================================================
    ===========================================USB PART=============================================
    ==============================================================================================*/

//    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
//        public void onReceive(Context context, Intent intent) {
//            Log.i(LOGGER_TAG, "coucou usbReceiver");
//            String action = intent.getAction();
//            if (MainActivity.ACTION_USB_PERMISSION.equals(action)) {
//                synchronized (this) {
//                    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
//                    UsbDevice  usbDevice  = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
//                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
//                        if (usbManager != null && usbDevice != null) {
////                            try {
////                                printTicket(usbManager, usbDevice);
////                            } catch (EscPosConnectionException | EscPosEncodingException | EscPosBarcodeException | EscPosParserException e) {
////                                throw new RuntimeException(e);
////                            }
//                        }
//                    }
//                }
//            }
//        }
//    };
//
//    public void printUsb() {
//
//        Log.i(LOGGER_TAG, "coucou printUsb");
//        //        UsbConnection usbConnection = UsbPrintersConnections.selectFirstConnected(this);
//        UsbManager    usbManager    = (UsbManager) this.getSystemService(Context.USB_SERVICE);
//        UsbConnection usbConnection = null;
//        if (usbManager.getDeviceList().size() > 0) {
//            usbConnection = new UsbConnection(usbManager, usbManager.getDeviceList().get(usbManager.getDeviceList().keySet().stream().findFirst().get()));
//        }
//
//        AtomicReference<String> listUsb = new AtomicReference<>("");
//        usbManager.getDeviceList()
//                  .forEach((s, usbDevice) -> {
//                      listUsb.set(listUsb + " s=" + s + " usbDevice=" + usbDevice.getDeviceName() + " - deviceClass=" + usbDevice.getDeviceClass()
//                                      + " - InterfaceCount=" + usbDevice.getInterfaceCount()
//                                      + " - Interface(0)=" + usbDevice.getInterface(0));
//                  });
//
//        new AlertDialog.Builder(this)
//            .setTitle("USB Connection")
//            .setMessage("usbConnection=" + usbConnection +
//                            " - usbManager=" + usbManager +
//                            " - listUsbSize=" + usbManager.getDeviceList().size() +
//                            " - listUsb=" + listUsb)
//            .show();
//
//        if (usbConnection == null || usbManager == null) {
//            new AlertDialog.Builder(this)
//                .setTitle("USB Connection")
//                .setMessage("No USB printer found.")
//                .show();
//            return;
//        }
//
//        PendingIntent permissionIntent = PendingIntent.getBroadcast(
//            this,
//            0,
//            new Intent(MainActivity.ACTION_USB_PERMISSION),
//            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0
//        );
//        IntentFilter filter = new IntentFilter(MainActivity.ACTION_USB_PERMISSION);
//        this.registerReceiver(this.usbReceiver, filter);
//        usbManager.requestPermission(usbConnection.getDevice(), permissionIntent);
//    }

//    private void printTicket(final UsbManager usbManager, final UsbDevice usbDevice)
//        throws EscPosConnectionException, EscPosEncodingException, EscPosBarcodeException, EscPosParserException {
//        EscPosPrinter printer = new EscPosPrinter(new UsbConnection(usbManager, usbDevice), 203, 48f, 32);
//        printer
//            .printFormattedText(
//                "[C]<img>"
//                    + PrinterTextParserImg.bitmapToHexadecimalString(
//                    printer, this.getResources().getDrawableForDensity(R.drawable.ticket_header, DisplayMetrics.DENSITY_MEDIUM))
//                    + "</img>\n" +
//                    //                    "[C]<img>" +
//                    //                    PrinterTextParserImg.bitmapToHexadecimalString(printer,
//                    //                                                                   this.getResources().getDrawableForDensity(R.drawable.code_barre_match,
//                    //                                                                                                                                   DisplayMetrics.DENSITY_MEDIUM))
//                    //                    + "</img>\n" +
//                    "[L]\n" +
//                    "[C]<font size='big'>Bon d'achat</font>\n" +
//                    "[C]<font size='big'>de</font> <font size='big'>3</font><font size='big'>€</font>\n" +
//                    "[L]\n" +
//                    //must have alignment at the start and \n at end without space
//                    //                                        "[C]<barcode>2019992026644</barcode>\n" +
//                    "[C]<img>" +
//                    PrinterTextParserImg.bitmapToHexadecimalString(
//                        printer,
//                        this.getResources().getDrawableForDensity(R.drawable.code_barre_match,
//                                                                  DisplayMetrics.DENSITY_DEFAULT))
//                    + "</img>\n" +
//                    "[L]\n" +
//                    "[C]Sur présentation de ce bon, \n" +
//                    "[C]bénéficiez d’une remise de 3€\n" +
//                    "[C]sur votre prochain achat\n" +
//                    "[C]d’un montant minimum de 4.50€\n" +
//                    "[C](remises déduites, hors presse,\n" +
//                    "[C]livres, gaz, carburant et\n consignes).\n" +
//                    "[C]A utiliser dans les 2 semaines\n" +
//                    "[C]suivant l’émission de ce bon,\n" +
//                    "[C]uniquement dans le Supermarché\n" +
//                    "[C]Villeneuve d’Ascq Haute Borne\n" +
//                    "[C](non valable en drive).\n\n" +
//                    "[L]\n" +
//
//                    "[C]<barcode type='ean13' height='10'>831254784551</barcode>\n" +
//                    "[C]<img>"
//                    + PrinterTextParserImg.bitmapToHexadecimalString(
//                    printer, this.getResources().getDrawableForDensity(R.drawable.ticket_footer, DisplayMetrics.DENSITY_MEDIUM))
//                    + "</img>\n"
//            );
//    }

}
