package com.bakoconsigne.bako_collector_match.ui.step2;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import com.bakoconsigne.bako_collector_match.dto.StockCollectorDTO;
import com.bakoconsigne.bako_collector_match.dto.StockDTO;
import com.bakoconsigne.bako_collector_match.dto.StockDrawerDTO;
import com.bakoconsigne.bako_collector_match.exceptions.UnauthorizedException;
import com.bakoconsigne.bako_collector_match.listeners.CustomArduinoListener;
import com.bakoconsigne.bako_collector_match.services.ArduinoService;
import com.bakoconsigne.bako_collector_match.services.CollectorService;
import com.bakoconsigne.bako_collector_match.utils.Utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

import static com.bakoconsigne.bako_collector_match.MainActivity.ERROR_CONNECTION;
import static com.bakoconsigne.bako_collector_match.MainActivity.ERROR_UNAUTHORIZED;
import static com.bakoconsigne.bako_collector_match.MainActivity.TIMER_DELAY_LONG;

public class OpenDrawerFragment extends Fragment implements CustomArduinoListener {
    private static final String ARDUINO_OPEN = "# open #";

    private final ArduinoService arduinoService = ArduinoService.getInstance();

    private final CollectorService collectorService = CollectorService.getInstance();

    private CountDownTimer countDownTimer;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_main_opendrawer, container, false);

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

        TextView timer = root.findViewById(R.id.main_opendrawer_timer);
        countDownTimer = new CountDownTimer(TIMER_DELAY_LONG, 1000) {

            @SuppressLint("SimpleDateFormat")
            public void onTick(long millisUntilFinished) {
                timer.setText(new SimpleDateFormat("mm:ss").format(new Date(millisUntilFinished)));
            }

            public void onFinish() {
                arduinoService.close();
                goToHome();
            }
        }.start();

        ImageView imageView = root.findViewById(R.id.imageView_main_step2);
        //TODO remove
        imageView.setOnClickListener(v -> goToStep3());

        this.arduinoService.setCustomArduinoListener(this);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        new Thread(() -> {
            try {
                StockCollectorDTO stockCollector = collectorService.getStockCollector();

                // search the drawer to open
                StockDrawerDTO stockDrawerAvailable = stockCollector.getDrawers().stream().sorted(Comparator.comparingInt(StockDrawerDTO::getNumDrawer))
                                                                    .filter(stockDrawerDTO -> {
                                                                        Integer totalBox = stockDrawerDTO.getStockList().stream()
                                                                                                         .map(StockDTO::getQuantity)
                                                                                                         .reduce(Integer::sum)
                                                                                                         .orElse(0);
                                                                        return !((totalBox + collectorService.getTotalBoxes()) > stockCollector.getMaxPerDrawer());
                                                                    })
                                                                    .findFirst()
                                                                    .orElse(null);

                if (stockDrawerAvailable == null) {
                    requireActivity().runOnUiThread(() -> Utils.alertError(requireActivity(), "Tous les tiroirs sont pleins, le dépot est impossible. Veuillez nous "
                        + "excuser pour la gêne occasionnée. " + stockCollector.getDrawers().size()));
                } else {
                    // then open
                    this.collectorService.setNumDrawer(stockDrawerAvailable.getNumDrawer());
                    this.arduinoService.openBox(stockDrawerAvailable.getNumDrawer());
                }
            } catch (UnauthorizedException e) {
                requireActivity().runOnUiThread(() -> Utils.alertError(requireActivity(), ERROR_UNAUTHORIZED));
            } catch (IOException e) {
                requireActivity().runOnUiThread(() -> Utils.alertError(requireActivity(), ERROR_CONNECTION));
            }
        }).start();
    }

    @Override
    public void onMessageReceived(final String message) {
        if (message != null && message.contains(ARDUINO_OPEN)) {
            requireActivity().runOnUiThread(this::goToStep3);
        }
    }

    private void goToStep3() {
        countDownTimer.cancel();
        NavHostFragment.findNavController(this).navigate(R.id.action_navigation_opendrawer_to_closedrawer);
    }

    private void goToHome() {
        NavHostFragment.findNavController(this).navigate(R.id.action_navigation_opendrawer_to_home);
    }
}
