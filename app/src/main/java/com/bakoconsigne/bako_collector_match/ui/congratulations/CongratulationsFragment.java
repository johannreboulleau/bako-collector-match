package com.bakoconsigne.bako_collector_match.ui.congratulations;

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

import static com.bakoconsigne.bako_collector_match.MainActivity.TIMER_DELAY_SHORT;

public class CongratulationsFragment extends Fragment {

    CountDownTimer countDownTimer;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_main_congratulations, container, false);

        TextView homeImageMenuLabel = root.findViewById(R.id.congrats_bottomAppBar_home_label);
        homeImageMenuLabel.setVisibility(View.VISIBLE);
        homeImageMenuLabel.setOnClickListener(v -> goToHome());

        ImageView homeImageMenu = root.findViewById(R.id.congrats_bottomAppBar_home);
        homeImageMenu.setVisibility(View.VISIBLE);
        homeImageMenu.setOnClickListener(v -> goToHome());

        this.countDownTimer = new CountDownTimer(TIMER_DELAY_SHORT, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                goToHome();
            }
        }.start();

        return root;
    }

    private void goToHome() {
        if (this.countDownTimer != null) {
            countDownTimer.cancel();
        }
        NavHostFragment.findNavController(this).navigate(R.id.action_navigation_congratulations_to_home);
    }
}
