package com.bakoconsigne.bako_collector_match.ui.step3;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.bakoconsigne.bako_collector_match.R;
import com.bakoconsigne.bako_collector_match.exceptions.BadRequestException;
import com.bakoconsigne.bako_collector_match.exceptions.BoxTypeSettingsException;
import com.bakoconsigne.bako_collector_match.exceptions.InternalServerException;
import com.bakoconsigne.bako_collector_match.exceptions.UnauthorizedException;
import com.bakoconsigne.bako_collector_match.listeners.CustomArduinoListener;
import com.bakoconsigne.bako_collector_match.services.ArduinoService;
import com.bakoconsigne.bako_collector_match.services.CollectorService;
import com.bakoconsigne.bako_collector_match.utils.Utils;
import com.google.zxing.client.android.BeepManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.bakoconsigne.bako_collector_match.MainActivity.ERROR_BAD_REQUEST;
import static com.bakoconsigne.bako_collector_match.MainActivity.ERROR_SERVER;
import static com.bakoconsigne.bako_collector_match.MainActivity.ERROR_UNAUTHORIZED;
import static com.bakoconsigne.bako_collector_match.MainActivity.LOGGER_TAG;
import static com.bakoconsigne.bako_collector_match.MainActivity.TIMER_DELAY_LONG;
import static com.bakoconsigne.bako_collector_match.MainActivity.TIMER_DELAY_MEDIUM;

public class CloseDrawerFragment extends Fragment implements CustomArduinoListener {

    private static final String ARDUINO_CLOSE = "# close #";

    private static final String ARDUINO_OPEN = "# open #";

    private static final String ARDUINO_WEIGHT = "kg";

    private static final double PERCENT_WEIGHT_ERROR = 0.3;

    private static final int MAX_ERROR = 3;

    private final CollectorService collectorService = CollectorService.getInstance();

    private final ArduinoService arduinoService = ArduinoService.getInstance();

    private boolean isDrawerOpen;

    private boolean isDrawerJustClose;

    private BeepManager beepManager;

    private CountDownTimer countDownTimer;

    private CountDownTimer countDownTimerError;

    private MediaPlayer mediaPlayer;

    private ImageView imageDeposit;

    private ImageView imageError;

    private ImageView imageErrorTechnical;

    private TextView timerTextView;

    private int nbError = 0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_main_closedrawer, container, false);

        this.imageDeposit = root.findViewById(R.id.imageView_main_step3_deposit);
        this.imageDeposit.setVisibility(View.VISIBLE);

        this.imageError = root.findViewById(R.id.imageView_main_step3_error);
        this.imageError.setVisibility(View.GONE);

        this.imageErrorTechnical = root.findViewById(R.id.imageView_main_step3_error_technical);
        this.imageErrorTechnical.setVisibility(View.GONE);

        this.isDrawerOpen = true;

        this.arduinoService.setCustomArduinoListener(this);

        this.beepManager = new BeepManager(requireActivity());

        this.mediaPlayer = MediaPlayer.create(requireContext(), R.raw.voice_alert_door);
        this.mediaPlayer.setLooping(true);
        this.timerTextView = root.findViewById(R.id.main_closedrawer_timer);
        this.countDownTimer = new CountDownTimer(TIMER_DELAY_MEDIUM, 1000) {
            @SuppressLint("SimpleDateFormat")
            public void onTick(long millisUntilFinished) {
                timerTextView.setText(new SimpleDateFormat("mm:ss").format(new Date(millisUntilFinished)));
            }

            public void onFinish() {
                mediaPlayer.start();
            }
        }.start();

        return root;
    }

    @Override
    public void onMessageReceived(final String message) {
        Log.i(LOGGER_TAG, "onMessageReceived " + message);
        if (getActivity() != null) {
            this.askWeight(message);
            this.checkWeight(message);
            this.openDrawer(message);
        }
    }

    private void openDrawer(final String message) {
        if (message != null && message.contains(ARDUINO_OPEN)) {

            if (this.countDownTimer != null) {

                this.mediaPlayer = MediaPlayer.create(requireContext(), R.raw.voice_alert_door);
                this.mediaPlayer.setLooping(true);

                this.countDownTimer.start();
            }
            if (this.countDownTimerError != null) {
                this.countDownTimerError.cancel();
            }
            this.isDrawerOpen = true;
            this.isDrawerJustClose = false;

            requireActivity().runOnUiThread(() -> {
                this.imageError.setVisibility(View.GONE);
                this.imageDeposit.setVisibility(View.VISIBLE);
            });
        }
    }

    private void askWeight(final String message) {
        Log.i(LOGGER_TAG, "askWeight step 1");
        if (this.isDrawerOpen) {
            Log.i(LOGGER_TAG, "askWeight step 2");
            if (message != null && message.contains(ARDUINO_CLOSE)) {
                Log.i(LOGGER_TAG, "askWeight step 3");
                if (this.mediaPlayer != null) {
                    this.mediaPlayer.stop();
                }
                this.isDrawerOpen = false;
                this.isDrawerJustClose = true;
                this.arduinoService.getWeightBox(this.collectorService.getNumDrawer());
            }
        }
    }

    private void checkWeight(final String message) {

        Log.i(LOGGER_TAG, "checkWeight step 1");
        if (this.isDrawerJustClose) {

            Log.i(LOGGER_TAG, "checkWeight step 2");
            if (message != null && message.contains(ARDUINO_WEIGHT)) {

                this.isDrawerJustClose = false;

                Log.i(LOGGER_TAG, "checkWeight step 3");
                final String weightInGram = message.replace("#", "").replace(ARDUINO_WEIGHT, "").replace(".", "").replace("=", "").trim();
                final int    weightOfBoxInGram  = Integer.parseInt(weightInGram);
                boolean      weightIsOk   = false;

                if (!this.collectorService.isDisableCheckWeight()) {

                    if (this.collectorService.isCheckWeightGreaterOnly()) {
                        weightIsOk = weightOfBoxInGram > 10;
                    } else {
                        Integer weightExpected;
                        try {
                            weightExpected = this.collectorService.getTotalWeight();
                        } catch (BoxTypeSettingsException e) {
                            requireActivity().runOnUiThread(() -> Utils.alertError(requireActivity(),
                                                                                   "Erreur de configuration: merci de définir le poids de la box dans l'administration."));

                            new Thread(() -> {
                                try {
                                    this.collectorService.monitoring(-1, "Erreur de configuration pour une de ces references (BoxType)) "
                                        + this.collectorService.getListBoxRef(), true);
                                } catch (IOException | UnauthorizedException | BadRequestException | InternalServerException exception) {
                                    Log.e(LOGGER_TAG, exception.getMessage());
                                }
                            }).start();

                            return;
                        }

                        double percentOfTolerance = PERCENT_WEIGHT_ERROR;
                        try {
                            if (this.collectorService.getStockCollector().getWeightTolerancePercent() != null) {
                                percentOfTolerance = this.collectorService.getStockCollector().getWeightTolerancePercent().doubleValue() / 100;
                            }
                        } catch (UnauthorizedException e) {
                            requireActivity().runOnUiThread(() -> Utils.alertError(requireActivity(), ERROR_UNAUTHORIZED));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } catch (InternalServerException e) {
                            requireActivity().runOnUiThread(() -> Utils.alertError(requireActivity(), ERROR_SERVER));
                        } catch (BadRequestException e) {
                            requireActivity().runOnUiThread(() -> Utils.alertError(requireActivity(), ERROR_BAD_REQUEST));
                        }
                        int minWeight = Double.valueOf(weightExpected - (weightExpected * percentOfTolerance)).intValue();
                        int maxWeight = Double.valueOf(weightExpected + (weightExpected * percentOfTolerance)).intValue();

                        weightIsOk = (minWeight <= weightOfBoxInGram && weightOfBoxInGram <= maxWeight);
                    }
                }

                if (collectorService.isDisableCheckWeight() || weightIsOk) {
                    Log.i(LOGGER_TAG, "close step weightInGram=" + weightInGram + " - weightOfBoxInGram=" + weightOfBoxInGram);
                    requireActivity().runOnUiThread(this::goToStep4);
                } else {

                    Log.i(LOGGER_TAG, "close step 3");
                    nbError++;
                    requireActivity().runOnUiThread(() -> {

                        int   duration = Toast.LENGTH_SHORT;
                        Toast toast    = Toast.makeText(requireActivity(), "Poids détecté de " + weightInGram + "g", duration);
                        toast.show();

                        imageDeposit.setVisibility(View.GONE);
                        if (nbError >= MAX_ERROR) {
                            imageError.setVisibility(View.GONE);
                            imageErrorTechnical.setVisibility(View.VISIBLE);
                        } else {
                            imageError.setVisibility(View.VISIBLE);
                            imageErrorTechnical.setVisibility(View.GONE);
                        }
                    });

                    if (nbError < MAX_ERROR) {
                        this.arduinoService.openBox(this.collectorService.getNumDrawer());
                        new Thread(() -> {
                            try {
                                this.collectorService.monitoring(-1, "Poids de " + weightOfBoxInGram + " non autorisé pour le dépôt de BoxType "
                                    + this.collectorService.getListBoxRef(), false);
                            } catch (IOException | UnauthorizedException | BadRequestException | InternalServerException e) {
                                Log.e(LOGGER_TAG, e.getMessage());
                            }
                        }).start();
                    }

                    requireActivity().runOnUiThread(() -> {

                        countDownTimer.cancel();
                        countDownTimerError = new CountDownTimer(TIMER_DELAY_LONG, 1000) {
                            @SuppressLint("SimpleDateFormat")
                            public void onTick(long millisUntilFinished) {
                                timerTextView.setText(new SimpleDateFormat("mm:ss").format(new Date(millisUntilFinished)));
                            }

                            public void onFinish() {
                                goToHome();
                            }
                        }.start();
                    });
                }
            }
        }
    }

    private void goToStep4() {

        if (this.countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (this.countDownTimerError != null) {
            this.countDownTimerError.cancel();
        }

        NavHostFragment.findNavController(this).navigate(R.id.action_navigation_closedrawer_to_voucher);
    }

    private void goToHome() {

        if (this.countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (this.countDownTimerError != null) {
            this.countDownTimerError.cancel();
        }

        NavHostFragment.findNavController(this).navigate(R.id.action_navigation_closedrawer_to_home);
    }
}
