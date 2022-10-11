package com.bakoconsigne.bako_collector_match.ui.step4;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.bakoconsigne.bako_collector_match.R;

public class ChoiceFragment extends Fragment  {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_main_choice, container, false);

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

        Button buttonDeposit = root.findViewById(R.id.button_main_choice_deposit);
        buttonDeposit.setOnClickListener(v -> goToStep1ScanBox());
        Button buttonFinish = root.findViewById(R.id.button_main_choice_finish);
        buttonFinish.setOnClickListener(v -> goToStep5Voucher());

        return root;
    }

    private void goToStep1ScanBox() {
        NavHostFragment.findNavController(this).navigate(R.id.action_navigation_choice_to_scanbox);
    }

    private void goToStep5Voucher() {
        NavHostFragment.findNavController(this).navigate(R.id.action_navigation_choice_to_voucher);
    }
}
