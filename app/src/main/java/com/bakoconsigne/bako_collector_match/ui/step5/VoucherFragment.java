package com.bakoconsigne.bako_collector_match.ui.step5;

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
import com.bakoconsigne.bako_collector_match.services.ArduinoService;

import static com.bakoconsigne.bako_collector_match.MainActivity.TIMER_DELAY_SHORT;

public class VoucherFragment extends Fragment {

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
}
