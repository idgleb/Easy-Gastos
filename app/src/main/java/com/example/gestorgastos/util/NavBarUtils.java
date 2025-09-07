package com.example.gestorgastos.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import androidx.annotation.ColorInt;

/**
 * Utilidad para manejar la configuración de la NavigationBar y StatusBar
 * en BottomSheets y otros componentes de la UI.
 */
public class NavBarUtils {
    
    /**
     * Configura la NavigationBar y StatusBar para mantener colores consistentes
     * cuando se muestran BottomSheets.
     * 
     * @param dialog El diálogo del BottomSheet
     * @param context El contexto de la aplicación
     */
    public static void setConsistentNavBarColors(Dialog dialog, Context context) {
        if (dialog != null && dialog.getWindow() != null) {
            // Mantener NavigationBar con fondo negro
            dialog.getWindow().setNavigationBarColor(Color.BLACK);
        }
    }
    
    /**
     * Configura la NavigationBar con un color específico.
     * 
     * @param dialog El diálogo del BottomSheet
     * @param color El color a aplicar (usar Color.BLACK, Color.WHITE, etc.)
     */
    public static void setNavigationBarColor(Dialog dialog, @ColorInt int color) {
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setNavigationBarColor(color);
        }
    }
    
    /**
     * Configura la StatusBar con un color específico.
     * 
     * @param dialog El diálogo del BottomSheet
     * @param color El color a aplicar (usar Color.BLACK, Color.WHITE, etc.)
     */
    public static void setStatusBarColor(Dialog dialog, @ColorInt int color) {
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setStatusBarColor(color);
        }
    }
    
    /**
     * Configura tanto NavigationBar como StatusBar con colores específicos.
     * 
     * @param dialog El diálogo del BottomSheet
     * @param navBarColor Color para la NavigationBar
     * @param statusBarColor Color para la StatusBar
     */
    public static void setBothBarColors(Dialog dialog, @ColorInt int navBarColor, @ColorInt int statusBarColor) {
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setNavigationBarColor(navBarColor);
            dialog.getWindow().setStatusBarColor(statusBarColor);
        }
    }
    
    /**
     * Configura la NavigationBar para que sea transparente.
     * 
     * @param dialog El diálogo del BottomSheet
     */
    public static void setTransparentNavigationBar(Dialog dialog) {
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }
    }
    
    /**
     * Configura la StatusBar para que sea transparente.
     * 
     * @param dialog El diálogo del BottomSheet
     */
    public static void setTransparentStatusBar(Dialog dialog) {
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }
}