package com.bakoconsigne.bako_collector_match.ui.home;

import android.content.Intent;
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
import com.bakoconsigne.bako_collector_match.SettingsActivity;
import com.bakoconsigne.bako_collector_match.services.CollectorService;

public class HomeFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_main_home, container, false);

        Button buttonResume = root.findViewById(R.id.button_nav_to_scan_box);
        buttonResume.setOnClickListener(this::goToScanFragment);

        ImageView homeImageMenu = requireActivity().findViewById(R.id.bottomAppBar_home);
        if (homeImageMenu != null) {
            homeImageMenu.setVisibility(View.GONE);
        }

        TextView homeImageMenuLabel = requireActivity().findViewById(R.id.bottomAppBar_home_label);
        if (homeImageMenuLabel != null) {
            homeImageMenuLabel.setVisibility(View.GONE);
        }

        CollectorService.getInstance().clear();

        ImageView imageSettings = root.findViewById(R.id.main_image_settings);
        imageSettings.setOnClickListener(this::goToSettings);

        return root;
    }

    public void goToScanFragment(View view) {
        NavHostFragment.findNavController(this).navigate(R.id.action_navigation_home_to_navigation_scan);
    }

    public void goToSettings(View view) {
        Intent myIntent = new Intent(requireActivity(), SettingsActivity.class);
        requireActivity().startActivity(myIntent);
    }

}
