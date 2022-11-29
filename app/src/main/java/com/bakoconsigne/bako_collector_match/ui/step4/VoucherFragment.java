package com.bakoconsigne.bako_collector_match.ui.step4;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.bakoconsigne.bako_collector_match.R;
import com.bakoconsigne.bako_collector_match.services.CollectorService;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;

import java.io.IOException;

import static com.bakoconsigne.bako_collector_match.MainActivity.ERROR_CONNECTION;
import static com.bakoconsigne.bako_collector_match.MainActivity.LOGGER_TAG;
import static com.bakoconsigne.bako_collector_match.MainActivity.TIMER_DELAY_SHORT;

/**
 * Fragment Voucher when the ticket is printed.
 */
public class VoucherFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_main_voucher, container, false);

        new Thread(() -> {
            try {
                if (!CollectorService.getInstance().postDepositBoxes()) {
                    requireActivity().runOnUiThread(() -> {
                        int   duration = Toast.LENGTH_SHORT;
                        Toast toast    = Toast.makeText(requireActivity(), ERROR_CONNECTION, duration);
                        toast.show();
                    });
                }
            } catch (IOException e) {
                requireActivity().runOnUiThread(() -> {
                    int   duration = Toast.LENGTH_SHORT;
                    Toast toast    = Toast.makeText(requireActivity(), ERROR_CONNECTION, duration);
                    toast.show();
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Log.e(LOGGER_TAG, "Error to POST deposit boxes", e);
                    int   duration = Toast.LENGTH_SHORT;
                    Toast toast    = Toast.makeText(requireActivity(), ERROR_CONNECTION, duration);
                    toast.show();
                });
            }
        }).start();

        try {
            this.printTicket();
        } catch (EscPosConnectionException | EscPosParserException | EscPosBarcodeException | EscPosEncodingException e) {
            int   duration = Toast.LENGTH_SHORT;
            Toast toast    = Toast.makeText(requireActivity(), e.getMessage(), duration);
            toast.show();
        }

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

    // see https://github.com/DantSu/ESCPOS-ThermalPrinter-Android

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
    //                                requireActivity().runOnUiThread(() -> Utils.alertError(requireActivity(), e.getMessage()));
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
    //        if (usbManager != null && usbManager.getDeviceList().size() > 0) {
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

    private void printTicket()
        throws EscPosConnectionException, EscPosEncodingException, EscPosBarcodeException, EscPosParserException {

        //        EscPosPrinter printer = new EscPosPrinter(new UsbConnection(usbManager, usbDevice), 203, 48f, 32);
        EscPosPrinter printer = new EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 203, 48f, 32);

        printer
            .printFormattedText(
                    "[C]<img>"
                    + PrinterTextParserImg.bitmapToHexadecimalString(
                    printer, this.getResources().getDrawableForDensity(R.drawable.ticket_header, DisplayMetrics.DENSITY_MEDIUM))
                    + "</img>\n" +
                    "[L]\n" +
                    "[C]<font size='big'>Bon d'achat</font>\n" +
                    "[C]<font size='big'>de <b>3,60€</b></font>\n" +
                    "[L]\n" +
                    //must have alignment at the start and \n at end without space
                    // but don't work
                    // "[C]<barcode>2019992026644</barcode>\n" +
                    "[C]<img>" +
                    PrinterTextParserImg.bitmapToHexadecimalString(
                        printer,
                        this.getResources().getDrawableForDensity(R.drawable.barcode,
                                                                  DisplayMetrics.DENSITY_DEFAULT))
                    + "</img>\n" +
                    "[L]\n" +
                    "[C]Sur présentation de ce bon, \n" +
                    "[C]bénéficiez d’une remise de 3,60€\n" +
                    "[C]sur votre prochain achat\n" +
                    "[C]d’un montant minimum de 4.50€\n" +
                    "[C](remises déduites hors consignes\n" +
                    "[C]presse, livres, gaz, carburant).\n" +
                    "[C]A utiliser sous 1 mois suivant\n" +
                    "[C]l’émission de ce bon uniquement\n" +
                    "[C]sur les rayons traditionnels\n" +
                    "[C]Boucherie, Boulangerie, Fromage,\n" +
                    "[C]Charcuterie, Poisson, Fruits et\n" +
                    "[C]légumes au Supermarché Match\n" +
                    "[C]Villeneuve d’Ascq Haute Borne\n" +
                    "[C](non valable en drive).\n\n" +
                    "[L]\n" +
                    "[C]<img>"
                    + PrinterTextParserImg.bitmapToHexadecimalString(
                    printer, this.getResources().getDrawableForDensity(R.drawable.ticket_footer, DisplayMetrics.DENSITY_MEDIUM))
                    + "</img>\n" +
                    "[L]\n"
            );
    }
}
