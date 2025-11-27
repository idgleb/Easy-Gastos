package com.example.gestorgastos.ui.settings;

import android.os.Bundle;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;
import com.example.gestorgastos.R;
import com.example.gestorgastos.databinding.ActivitySettingsBinding;
import com.example.gestorgastos.util.ThemeManager;

/**
 * Activity para configuración de la aplicación.
 * Permite al usuario cambiar el tema (claro, oscuro, sistema).
 */
public class SettingsActivity extends AppCompatActivity {
    
    private ActivitySettingsBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Aplicar el tema antes de setContentView
        ThemeManager.applyTheme(this);
        
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupToolbar();
        setupThemeOptions();
    }
    
    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupThemeOptions() {
        // Obtener el tema actual
        int currentTheme = ThemeManager.getThemeMode(this);
        
        // Seleccionar el RadioButton correspondiente
        switch (currentTheme) {
            case ThemeManager.MODE_LIGHT:
                binding.radioLight.setChecked(true);
                break;
            case ThemeManager.MODE_DARK:
                binding.radioDark.setChecked(true);
                break;
            case ThemeManager.MODE_SYSTEM:
                binding.radioSystem.setChecked(true);
                break;
        }
        
        // Listener para cambios en la selección
        binding.radioGroupTheme.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int newThemeMode;
                
                if (checkedId == R.id.radioLight) {
                    newThemeMode = ThemeManager.MODE_LIGHT;
                } else if (checkedId == R.id.radioDark) {
                    newThemeMode = ThemeManager.MODE_DARK;
                } else {
                    newThemeMode = ThemeManager.MODE_SYSTEM;
                }
                
                // Guardar y aplicar el nuevo tema
                ThemeManager.setThemeMode(SettingsActivity.this, newThemeMode);
                
                // Recrear la activity para aplicar el tema inmediatamente
                recreate();
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

