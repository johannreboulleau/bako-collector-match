package com.bakoconsigne.bako_collector_match.ui.step3;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.bakoconsigne.bako_collector_match.R;
import com.bakoconsigne.bako_collector_match.exceptions.BoxTypeSettingsException;
import com.bakoconsigne.bako_collector_match.listeners.CustomArduinoListener;
import com.bakoconsigne.bako_collector_match.services.ArduinoService;
import com.bakoconsigne.bako_collector_match.services.CollectorService;
import com.bakoconsigne.bako_collector_match.utils.Utils;
import com.google.zxing.client.android.BeepManager;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.bakoconsigne.bako_collector_match.MainActivity.TIMER_DELAY_LONG;

public class CloseDrawerFragment extends Fragment implements CustomArduinoListener {

    private static final String ARDUINO_CLOSE = "# close #";

    private static final String ARDUINO_WEIGHT = " kg";

    private static final double PERCENT_WEIGHT_ERROR = 0.3;

    private final CollectorService collectorService = CollectorService.getInstance();

    private final ArduinoService arduinoService = ArduinoService.getInstance();

    private TextView textViewArduinoMessage;

    private boolean isDrawerOpen;

    private boolean isDrawerJustClose;

    private Integer numDrawer;

    private BeepManager beepManager;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_main_closedrawer, container, false);

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

        this.isDrawerOpen = true;

        beepManager = new BeepManager(requireActivity());

        TextView timer = root.findViewById(R.id.main_closedrawer_timer);
        CountDownTimer countDownTimer = new CountDownTimer(TIMER_DELAY_LONG, 1000) {
            @SuppressLint("SimpleDateFormat")
            public void onTick(long millisUntilFinished) {
                timer.setText(new SimpleDateFormat("mm:ss").format(new Date(millisUntilFinished)));
            }
            public void onFinish() {
                while (isDrawerOpen) {
                    long millis = System.currentTimeMillis();

                    beepManager.playBeepSound();

                    try {
                        Thread.sleep(2000 - millis % 2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }.start();

        //TODO remove
        ImageView image = root.findViewById(R.id.imageView_main_step3);
        image.setOnClickListener(v -> {
            countDownTimer.cancel();
            this.collectorService.addBoxInMemory("60b759336722f64ed675e14a");
            goToStep4();
        });

        return root;
    }

    @Override
    public void onMessageReceived(final String message) {
        this.textViewArduinoMessage.setText(message);
        this.askWeight(message);
        this.checkWeight(message);
    }

    private void askWeight(final String message) {
        if (this.isDrawerOpen) {
            if (message != null && message.contains(ARDUINO_CLOSE)) {
                this.isDrawerOpen = false;
                this.isDrawerJustClose = true;
                this.arduinoService.getWeightBox(this.numDrawer);
            }
        }
    }

    private void checkWeight(final String message) {
        if (this.isDrawerJustClose) {
            if (message != null && message.contains(ARDUINO_WEIGHT)) {
                String  weightInGram = message.replace("#", "").replace(ARDUINO_WEIGHT, "").replace(".", "").replace("=", "").trim();
                Integer weightExpected;

                try {
                    weightExpected = this.collectorService.getTotalWeight();
                } catch (BoxTypeSettingsException e) {
                    requireActivity().runOnUiThread(() -> Utils.alertError(requireActivity(),
                                                                           "Erreur de configuration: merci de définir le poids de la box dans l'administration."));
                    return;
                }

                int weightOfBox = Integer.parseInt(weightInGram);
                int minWeight   = Double.valueOf(weightExpected - (weightExpected * PERCENT_WEIGHT_ERROR)).intValue();
                int maxWeight   = Double.valueOf(weightExpected + (weightExpected * PERCENT_WEIGHT_ERROR)).intValue();
                if (minWeight <= weightOfBox && weightOfBox <= maxWeight) {
                    requireActivity().runOnUiThread(this::goToStep4);
                } else {
                    //TODO logger les erreurs pour les tests
                    requireActivity().runOnUiThread(() -> Utils.alertError(requireActivity(),
                                                                           "Erreur détectée: le poids n'est pas correcte."));
                }
            }
        }
    }

    private void goToStep4() {
        NavHostFragment.findNavController(this).navigate(R.id.action_navigation_closedrawer_to_choice);
    }
}
