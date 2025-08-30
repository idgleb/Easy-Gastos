package com.example.gestorgastos;

import android.app.Application;

import com.google.firebase.FirebaseApp;

public class GestorGastosApp extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Forzar modo claro globalmente
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        
        // Inicializar Firebase
        FirebaseApp.initializeApp(this);
        
        // Configurar WorkManager para sincronización periódica
        setupWorkManager();
    }
    
    private void setupWorkManager() {
        // La configuración de WorkManager se hará en los repositorios
        // cuando se necesite programar trabajos de sincronización
    }
}


