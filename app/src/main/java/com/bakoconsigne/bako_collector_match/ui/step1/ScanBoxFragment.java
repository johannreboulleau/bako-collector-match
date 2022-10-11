package com.bakoconsigne.bako_collector_match.ui.step1;

import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.bakoconsigne.bako_collector_match.R;
import com.bakoconsigne.bako_collector_match.services.CollectorService;
import com.bakoconsigne.bako_collector_match.utils.Utils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.journeyapps.barcodescanner.camera.CameraSettings;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static com.bakoconsigne.bako_collector_match.MainActivity.ERROR_CONNECTION;
import static com.bakoconsigne.bako_collector_match.MainActivity.LOGGER_TAG;

public class ScanBoxFragment extends Fragment {

    private static final String DIES = "#";

    private final CollectorService collectorService = CollectorService.getInstance();

    private DecoratedBarcodeView barcodeView;

    private BeepManager beepManager;

    private String lastText;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_main_scanbox, container, false);

        barcodeView = root.findViewById(R.id.barcode_view_box);

        CameraSettings settings = barcodeView.getBarcodeView().getCameraSettings();
        if (settings.getRequestedCameraId() != Camera.CameraInfo.CAMERA_FACING_FRONT) {
            settings.setRequestedCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }
        barcodeView.getBarcodeView().setCameraSettings(settings);

        Collection<BarcodeFormat> formats = Arrays.asList(BarcodeFormat.QR_CODE, BarcodeFormat.EAN_13);
        barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
        barcodeView.decodeSingle(callback);

        beepManager = new BeepManager(requireActivity());

        Button buttonRetry = root.findViewById(R.id.button_nav_retry);
        buttonRetry.setOnClickListener(v -> {

            TextView textView = requireView().findViewById(R.id.fragmentScan_textView_error);
            textView.setVisibility(GONE);
            buttonRetry.setVisibility(GONE);

            lastText = null;
            barcodeView.resume();
            barcodeView.setVisibility(View.VISIBLE);
            barcodeView.decodeSingle(callback);

            ImageView imageView = requireView().findViewById(R.id.imageView_step1);
            imageView.setVisibility(View.VISIBLE);
        });
        //TODO remove
        barcodeView.setOnClickListener(v -> goToStep2());

        TextView homeImageMenuLabel = requireActivity().findViewById(R.id.bottomAppBar_home_label);
        homeImageMenuLabel.setVisibility(this.collectorService.getTotalBoxes() < 1 ? View.VISIBLE : View.GONE);
        homeImageMenuLabel.setOnClickListener(item -> goToHome());

        ImageView homeImageMenu = requireActivity().findViewById(R.id.bottomAppBar_home);
        homeImageMenu.setVisibility(this.collectorService.getTotalBoxes() < 1 ? View.VISIBLE : View.GONE);
        homeImageMenu.setOnClickListener(item -> goToHome());

        TextView prevImageMenuLabel = requireActivity().findViewById(R.id.bottomAppBar_prev_label);
        if (prevImageMenuLabel != null) {
            prevImageMenuLabel.setVisibility(this.collectorService.getTotalBoxes() > 0 ? View.VISIBLE : View.GONE);
            prevImageMenuLabel.setOnClickListener(v -> gotToStep4Choice());
        }

        ImageView prevImageMenu = requireActivity().findViewById(R.id.bottomAppBar_prev);
        if (prevImageMenu != null) {
            prevImageMenu.setVisibility(View.GONE);
        }

        return root;
    }

    @Override
    public void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            Log.d(LOGGER_TAG, result.getText());
            if (result.getText() == null || result.getText().equals(lastText)) {
                // Prevent duplicate scans
                return;
            }

            lastText = result.getText();
            barcodeView.setStatusText(result.getText());

            beepManager.playBeepSound();

            barcodeView.pauseAndWait();
            barcodeView.setVisibility(INVISIBLE);

            String boxTypeId;
            if (lastText.contains(DIES)) {
                boxTypeId = lastText.split(DIES)[1];
            } else {
                boxTypeId = lastText;
            }

            new Thread(() -> {
                try {
                    if (collectorService.isBoxReferenceExist(boxTypeId)) {
                        collectorService.addBoxInMemory(boxTypeId);
                        requireActivity().runOnUiThread(() -> goToStep2());
                    } else {
                        requireActivity().runOnUiThread(() -> {
                            TextView textView = requireView().findViewById(R.id.fragmentScan_textView_error);
                            textView.setVisibility(View.VISIBLE);
                            Button buttonRetry = requireView().findViewById(R.id.button_nav_retry);
                            buttonRetry.setVisibility(View.VISIBLE);

                            ImageView imageView = requireView().findViewById(R.id.imageView_step1);
                            imageView.setVisibility(GONE);
                        });
                    }
                } catch (IOException e) {
                    requireActivity().runOnUiThread(() -> Utils.alertError(getActivity(), ERROR_CONNECTION));
                }
            }).start();
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NotNull String[] permissions, @NotNull int[] grantResults) {
        if (requestCode == 1) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Log.i(LOGGER_TAG, "Camera Permission granted");
                // permission was granted, yay! Do the
                // contacts-related task you need to do.
            } else {

                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(getActivity(), "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void goToStep2() {
        NavHostFragment.findNavController(this).navigate(R.id.action_navigation_scan_to_opendrawer);
    }

    private void goToHome() {
        NavHostFragment.findNavController(this).navigate(R.id.action_navigation_scan_to_navigation_home);
    }

    private void gotToStep4Choice() {
        NavHostFragment.findNavController(this).navigate(R.id.action_navigation_scan_to_choice);
    }
}
