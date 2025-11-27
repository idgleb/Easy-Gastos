package com.example.gestorgastos.ui.settings;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.gestorgastos.R;
import com.example.gestorgastos.databinding.FragmentSettingsBinding;
import com.example.gestorgastos.util.ThemeManager;

/**
 * Fragment de configuración de la aplicación.
 * Permite al usuario cambiar el tema (claro, oscuro, o seguir el sistema).
 */
public class SettingsFragment extends Fragment {
    
    private FragmentSettingsBinding binding;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViews();
        loadCurrentTheme();
    }
    
    private void setupViews() {
        // Configurar botón de retroceso
        binding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
        
        // Configurar versión de la app
        try {
            PackageInfo pInfo = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            String version = pInfo.versionName;
            binding.tvAppVersion.setText("Versión " + version);
        } catch (PackageManager.NameNotFoundException e) {
            binding.tvAppVersion.setText("Versión 1.0.0");
        }
        
        // Configurar listener para cambios de tema
        binding.rgThemeMode.setOnCheckedChangeListener((group, checkedId) -> {
            int themeMode;
            
            if (checkedId == R.id.rbLightMode) {
                themeMode = ThemeManager.MODE_LIGHT;
            } else if (checkedId == R.id.rbDarkMode) {
                themeMode = ThemeManager.MODE_DARK;
            } else {
                themeMode = ThemeManager.MODE_SYSTEM;
            }
            
            // Guardar y aplicar el tema
            ThemeManager.setThemeMode(requireContext(), themeMode);
            
            // Recrear la actividad para aplicar el nuevo tema
            if (getActivity() != null) {
                getActivity().recreate();
            }
        });
    }
    
    /**
     * Carga el tema actual y marca el RadioButton correspondiente.
     */
    private void loadCurrentTheme() {
        int currentTheme = ThemeManager.getThemeMode(requireContext());
        
        // Desactivar temporalmente el listener para evitar recrear la actividad
        binding.rgThemeMode.setOnCheckedChangeListener(null);
        
        switch (currentTheme) {
            case ThemeManager.MODE_LIGHT:
                binding.rbLightMode.setChecked(true);
                break;
            case ThemeManager.MODE_DARK:
                binding.rbDarkMode.setChecked(true);
                break;
            case ThemeManager.MODE_SYSTEM:
            default:
                binding.rbSystemMode.setChecked(true);
                break;
        }
        
        // Reactivar el listener
        binding.rgThemeMode.setOnCheckedChangeListener((group, checkedId) -> {
            int themeMode;
            
            if (checkedId == R.id.rbLightMode) {
                themeMode = ThemeManager.MODE_LIGHT;
            } else if (checkedId == R.id.rbDarkMode) {
                themeMode = ThemeManager.MODE_DARK;
            } else {
                themeMode = ThemeManager.MODE_SYSTEM;
            }
            
            // Guardar y aplicar el tema
            ThemeManager.setThemeMode(requireContext(), themeMode);
            
            // Recrear la actividad para aplicar el nuevo tema
            if (getActivity() != null) {
                getActivity().recreate();
            }
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

