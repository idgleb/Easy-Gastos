package com.example.gestorgastos.ui.about;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.gestorgastos.R;
import com.example.gestorgastos.databinding.FragmentAboutBinding;

/**
 * Fragment que muestra información sobre la aplicación.
 * Incluye: logo, descripción, desarrollador, enlaces y opción para compartir la app.
 */
public class AboutFragment extends Fragment {
    
    private FragmentAboutBinding binding;
    
    // URL del repositorio de GitHub
    private static final String GITHUB_URL = "https://github.com/idgleb/Easy-Gastos";
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViews();
        loadAppVersion();
    }
    
    private void setupViews() {
        // Configurar botón de retroceso
        binding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
        
        // Configurar enlace a GitHub
        binding.cardGitHub.setOnClickListener(v -> openUrl(GITHUB_URL));
        
        // Configurar botón de compartir app
        binding.btnShareApp.setOnClickListener(v -> shareApp());
    }
    
    private void loadAppVersion() {
        try {
            PackageInfo pInfo = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            String version = pInfo.versionName;
            binding.tvAppVersion.setText(getString(R.string.about_version_format, version));
        } catch (PackageManager.NameNotFoundException e) {
            binding.tvAppVersion.setText(getString(R.string.about_version_format, "1.0.0"));
        }
    }
    
    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), 
                getString(R.string.about_error_open_url), 
                Toast.LENGTH_SHORT).show();
        }
    }
    
    private void shareApp() {
        try {
            String appName = getString(R.string.app_name);
            String shareText = getString(R.string.about_share_text, appName, GITHUB_URL);
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, appName);
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            
            startActivity(Intent.createChooser(shareIntent, getString(R.string.about_share_title)));
        } catch (Exception e) {
            Toast.makeText(requireContext(), 
                getString(R.string.about_error_share), 
                Toast.LENGTH_SHORT).show();
        }
    }
}

