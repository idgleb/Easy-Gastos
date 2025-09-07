package com.example.gestorgastos.util;

/**
 * Constantes para animaciones en la aplicación.
 * Centraliza las duraciones y interpoladores para facilitar el mantenimiento.
 */
public class AnimationConstants {
    
    // ===== DURACIONES =====
    
    /**
     * Duración rápida para animaciones de botones y elementos pequeños
     */
    public static final int DURATION_FAST = 450;
    
    /**
     * Duración estándar para animaciones de formularios
     */
    public static final int DURATION_STANDARD = 500;
    
    /**
     * Duración lenta para animaciones de entrada de pantalla
     */
    public static final int DURATION_SLOW = 900;
    
    /**
     * Duración para animaciones de transición entre pantallas
     */
    public static final int DURATION_TRANSITION = 700;
    
    /**
     * Duración para animaciones de fade in/out
     */
    public static final int DURATION_FADE = 400;
    
    /**
     * Duración para animaciones de slide up/down
     */
    public static final int DURATION_SLIDE = 350;
    
    // ===== DELAYS =====
    
    /**
     * Delay pequeño para evitar superposición de animaciones
     */
    public static final int DELAY_SMALL = 50;
    
    /**
     * Delay estándar para secuencias de animación
     */
    public static final int DELAY_STANDARD = 100;
    
    /**
     * Delay para animaciones de entrada de pantalla
     */
    public static final int DELAY_ENTRY = 200;
    
    // ===== CONFIGURACIONES ESPECÍFICAS =====
    
    /**
     * Configuración para animaciones de campos de formulario
     */
    public static class FormField {
        public static final int DURATION = DURATION_STANDARD;
        public static final int DELAY = DELAY_SMALL;
    }
    
    /**
     * Configuración para animaciones de botones
     */
    public static class Button {
        public static final int DURATION_FAST = 200;
        public static final int DURATION_SLOW = 450;
        public static final int DELAY = DELAY_SMALL;
    }
    
    /**
     * Configuración específica para botones de AuthActivity
     */
    public static class AuthButton {
        public static final int DURATION = 300;
        public static final int DELAY = DELAY_SMALL;
        public static final float SCALE_FACTOR = 0.8f;
        public static final float ALPHA_DISABLED = 0.6f;
    }
    
    /**
     * Configuración para animaciones de entrada de pantalla
     */
    public static class ScreenEntry {
        public static final int DURATION_LOGO = 5000;
        public static final int DURATION_TITLE = DURATION_SLOW;
        public static final int DURATION_SUBTITLE = DURATION_SLOW;
        public static final int DURATION_FORM = 2000;
        public static final int DURATION_MODE_TEXT = 400;
        
        public static final int DELAY_TITLE = 200;
        public static final int DELAY_SUBTITLE = 400;
        public static final int DELAY_FORM = 600;
        public static final int DELAY_MODE_TEXT = 800;
    }
    
    /**
     * Configuración para animaciones de transición entre pantallas
     */
    public static class ScreenTransition {
        public static final int DURATION = DURATION_TRANSITION;
        public static final float SCALE_FACTOR = 1.3f;
        public static final float ALPHA_FACTOR = 0.8f;
        public static final int DELAY = DELAY_STANDARD;
    }
    
    /**
     * Configuración para animaciones de loading
     */
    public static class Loading {
        public static final int DURATION = 200;
        public static final float ALPHA_DISABLED = 0.6f;
    }
}
