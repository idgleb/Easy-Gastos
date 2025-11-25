package com.example.gestorgastos;

import android.app.Application;

import com.google.firebase.FirebaseApp;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.gestorgastos.work.SyncWorker;

import java.util.concurrent.TimeUnit;

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
        // Sincronizar solo cuando haya red disponible
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Trabajo periódico cada 15 minutos (mínimo permitido por WorkManager)
        PeriodicWorkRequest syncRequest =
                new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "firestore_sync_work",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
    }
}


