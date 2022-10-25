package com.bakoconsigne.bako_collector_match.ui.step4;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.bakoconsigne.bako_collector_match.MainActivity;
import com.bakoconsigne.bako_collector_match.R;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.usb.UsbConnection;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;

import java.text.SimpleDateFormat;
import java.util.Locale;

import static com.bakoconsigne.bako_collector_match.MainActivity.LOGGER_TAG;
import static com.bakoconsigne.bako_collector_match.MainActivity.TIMER_DELAY_SHORT;

/**
 * Fragment Voucher when the ticket is printed.
 */
public class VoucherFragment extends Fragment {

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyy HH:mm:ss", Locale.FRANCE);

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_main_voucher, container, false);

        TextView homeImageMenuLabel = requireActivity().findViewById(R.id.bottomAppBar_home_label);
        homeImageMenuLabel.setVisibility(View.GONE);

        ImageView homeImageMenu = requireActivity().findViewById(R.id.bottomAppBar_home);
        homeImageMenu.setVisibility(View.GONE);

        TextView prevImageMenuLabel = requireActivity().findViewById(R.id.bottomAppBar_prev_label);
        if (prevImageMenuLabel != null) {
            prevImageMenuLabel.setVisibility(View.GONE);
        }

        ImageView prevImageMenu = requireActivity().findViewById(R.id.bottomAppBar_prev);
        if (prevImageMenu != null) {
            prevImageMenu.setVisibility(View.GONE);
        }

//        this.printUsb();

        new CountDownTimer(TIMER_DELAY_SHORT, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                goToCongratulations();
            }
        }.start();

        return root;
    }

    private void goToCongratulations() {
        NavHostFragment.findNavController(this).navigate(R.id.action_navigation_voucher_to_congratulations);
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
//                    UsbManager usbManager = (UsbManager) requireActivity().getSystemService(Context.USB_SERVICE);
//                    UsbDevice  usbDevice  = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
//                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
//                        if (usbManager != null && usbDevice != null) {
//                            try {
//                                printTicket(usbManager, usbDevice);
//                            } catch (EscPosConnectionException | EscPosEncodingException | EscPosBarcodeException | EscPosParserException e) {
//                                throw new RuntimeException(e);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    };
//
//    public void printUsb() {
//
//        //        UsbConnection usbConnection = UsbPrintersConnections.selectFirstConnected(this);
//        UsbManager    usbManager    = (UsbManager) requireActivity().getSystemService(Context.USB_SERVICE);
//        UsbConnection usbConnection = null;
//        if (usbManager.getDeviceList().size() > 0) {
//            usbConnection = new UsbConnection(usbManager, usbManager.getDeviceList().get(usbManager.getDeviceList().keySet().stream().findFirst().get()));
//        }
//
//        if (usbConnection == null || usbManager == null) {
//            new AlertDialog.Builder(requireActivity())
//                .setTitle("USB Connection")
//                .setMessage("No USB printer found.")
//                .show();
//            return;
//        }
//
//        PendingIntent permissionIntent = PendingIntent.getBroadcast(
//            requireActivity(),
//            0,
//            new Intent(MainActivity.ACTION_USB_PERMISSION),
//            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0
//        );
//        IntentFilter filter = new IntentFilter(MainActivity.ACTION_USB_PERMISSION);
//        requireActivity().registerReceiver(this.usbReceiver, filter);
//        usbManager.requestPermission(usbConnection.getDevice(), permissionIntent);
//    }
//
//    private void printTicket(final UsbManager usbManager, final UsbDevice usbDevice)
//        throws EscPosConnectionException, EscPosEncodingException, EscPosBarcodeException, EscPosParserException {
//        EscPosPrinter printer = new EscPosPrinter(new UsbConnection(usbManager, usbDevice), 203, 48f, 32);
//        printer
//            .printFormattedText(
//                "[C]<img>"
//                    + PrinterTextParserImg.bitmapToHexadecimalString(
//                    printer, this.getResources().getDrawableForDensity(R.drawable.ticket_header, DisplayMetrics.DENSITY_MEDIUM))
//                    + "</img>\n" +
//                    "[L]\n" +
//                    "[C]<font size='big'>Bon d'achat</font>\n" +
//                    "[C]<font size='big'>de</font> <font size='big'>3</font><font size='big'>€</font>\n" +
//                    "[L]\n" +
//                    //must have alignment at the start and \n at end without space
//                    // but don't work
//                    // "[C]<barcode>2019992026644</barcode>\n" +
//                    "[C]<img>" +
//                    PrinterTextParserImg.bitmapToHexadecimalString(
//                        printer,
//                        this.getResources().getDrawableForDensity(R.drawable.barcode,
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
//                    "[C]<img>"
//                    + PrinterTextParserImg.bitmapToHexadecimalString(
//                    printer, this.getResources().getDrawableForDensity(R.drawable.ticket_footer, DisplayMetrics.DENSITY_MEDIUM))
//                    + "</img>\n"
//            );
//    }
}
