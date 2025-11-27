package com.example.gestorgastos.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Gestor de temas de la aplicación.
 * Permite cambiar entre modo claro, oscuro y seguir el sistema.
 */
public class ThemeManager {
    
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";
    
    // Modos de tema
    public static final int MODE_LIGHT = 0;
    public static final int MODE_DARK = 1;
    public static final int MODE_SYSTEM = 2;
    
    /**
     * Aplica el tema guardado en las preferencias.
     * Debe llamarse en onCreate() de cada Activity.
     */
    public static void applyTheme(Context context) {
        int themeMode = getThemeMode(context);
        applyThemeMode(themeMode);
    }
    
    /**
     * Guarda el modo de tema seleccionado y lo aplica inmediatamente.
     */
    public static void setThemeMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        applyThemeMode(mode);
    }
    
    /**
     * Obtiene el modo de tema guardado.
     * Por defecto retorna MODE_SYSTEM.
     */
    public static int getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM);
    }
    
    /**
     * Aplica el modo de tema usando AppCompatDelegate.
     */
    private static void applyThemeMode(int mode) {
        switch (mode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case MODE_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}

