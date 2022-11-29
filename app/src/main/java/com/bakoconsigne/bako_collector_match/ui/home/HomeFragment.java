package com.bakoconsigne.bako_collector_match.ui.home;

import android.Manifest;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.bakoconsigne.bako_collector_match.R;
import com.bakoconsigne.bako_collector_match.ResetCollectActivity;
import com.bakoconsigne.bako_collector_match.SettingsActivity;
import com.bakoconsigne.bako_collector_match.services.ArduinoService;
import com.bakoconsigne.bako_collector_match.services.CollectorService;
import com.google.android.material.bottomappbar.BottomAppBar;

public class HomeFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_main_home, container, false);

        requireBluetoothPermission();

        Button buttonResume = root.findViewById(R.id.button_nav_to_scan_box);
        buttonResume.setOnClickListener(this::goToScanFragment);

        CollectorService.getInstance().clear();

        ImageView imageSettings = root.findViewById(R.id.main_image_settings);
        imageSettings.setOnClickListener(v -> {
            AlertDialog alertDialog = new AlertDialog.Builder(requireContext()).create(); //Read Update
            alertDialog.setTitle("Bonjour");
            alertDialog.setMessage("Que voulez-vous faire ?");

            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Collecte", (dialog, which) -> {
                goToResetCollect(v);
            });

            alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Settings", (dialog, which) -> {
                goToSettings(v);
            });

            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Annuler", (dialog, which) -> {
                alertDialog.cancel();
            });

            alertDialog.show();  //<-- See This!
        });

        return root;
    }

    public void goToScanFragment(View view) {
        NavHostFragment.findNavController(this).navigate(R.id.action_navigation_home_to_navigation_scan);
    }

    public void goToSettings(View view) {
        Intent myIntent = new Intent(requireActivity(), SettingsActivity.class);
        requireActivity().startActivity(myIntent);
    }

    public void goToResetCollect(View view) {
        Intent myIntent = new Intent(requireActivity(), ResetCollectActivity.class);
        requireActivity().startActivity(myIntent);
    }

    // see https://github.com/DantSu/ESCPOS-ThermalPrinter-Android

    private void requireBluetoothPermission() {

//        EscPosPrinter printer = new EscPosPrinter(new UsbConnection(usbManager, usbDevice), 203, 48f, 32);
        BluetoothManager bluetoothManager = (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[] { Manifest.permission.BLUETOOTH_CONNECT }, 1);
        }
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[] { Manifest.permission.BLUETOOTH_SCAN }, 1);
        }
        bluetoothManager.getAdapter().getBondedDevices();

        //TODO check if printer bluetooth is present

//        EscPosPrinter printer = new EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 203, 48f, 32);
//
//        printer
//            .printFormattedText(
//                "[C]<img>"
//                    + PrinterTextParserImg.bitmapToHexadecimalString(
//                    printer, this.getResources().getDrawableForDensity(R.drawable.ticket_header, DisplayMetrics.DENSITY_MEDIUM))
//                    + "</img>\n" +
//                    "[L]\n" +
//                    "[C]<font size='big'>Bon d'achat</font>\n" +
//                    "[C]<font size='big'>de</font> <font size='big'>3,60</font><font size='big'>€</font>\n" +
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
//                    "[C]bénéficiez d’une remise de 3,60€\n" +
//                    "[C]sur votre prochain achat\n" +
//                    "[C]d’un montant minimum de 4.50€\n" +
//                    "[C](remises déduites hors consignes\n" +
//                    "[C]presse, livres, gaz, carburant).\n" +
//                    "[C]A utiliser sous 1 mois suivant\n" +
//                    "[C]l’émission de ce bon uniquement\n" +
//                    "[C]sur les rayons traditionnels\n" +
//                    "[C]Boucherie, Boulangerie, Fromage,\n" +
//                    "[C]Charcuterie, Poisson, Fruits et\n" +
//                    "[C]légumes au Supermarché Match\n" +
//                    "[C]Villeneuve d’Ascq Haute Borne\n" +
//                    "[C](non valable en drive).\n\n" +
//                    "[L]\n" +
//                    "[C]<img>"
//                    + PrinterTextParserImg.bitmapToHexadecimalString(
//                    printer, this.getResources().getDrawableForDensity(R.drawable.ticket_footer, DisplayMetrics.DENSITY_MEDIUM))
//                    + "</img>\n"
//            );
    }

}
