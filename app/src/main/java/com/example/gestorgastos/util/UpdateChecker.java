package com.example.gestorgastos.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Utilidad para verificar actualizaciones de la aplicación desde Firestore.
 * Compara la versión actual con la última versión disponible.
 */
public class UpdateChecker {
    private static final String TAG = "UpdateChecker";
    private static final String FIRESTORE_PATH = "app_info/version";
    
    public interface UpdateCheckListener {
        void onUpdateAvailable(int currentVersion, int latestVersion, String versionName, String message);
        void onNoUpdateAvailable();
        void onError(String error);
    }
    
    /**
     * Verifica si hay una actualización disponible comparando la versión actual
     * con la información almacenada en Firestore.
     * 
     * @param context Contexto de la aplicación
     * @param listener Listener para recibir los resultados de la verificación
     */
    public static void checkForUpdate(Context context, UpdateCheckListener listener) {
        try {
            // Obtener versión actual de la app
            PackageInfo pInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            int currentVersionCode = pInfo.versionCode;
            String currentVersionName = pInfo.versionName;
            
            Log.d(TAG, "Versión actual: " + currentVersionName + " (" + currentVersionCode + ")");
            
            // Obtener información de versión desde Firestore
            FirebaseFirestore.getInstance()
                    .document(FIRESTORE_PATH)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Long minVersionCode = documentSnapshot.getLong("minVersionCode");
                            Long latestVersionCode = documentSnapshot.getLong("latestVersionCode");
                            String latestVersionName = documentSnapshot.getString("latestVersionName");
                            String updateMessage = documentSnapshot.getString("updateMessage");
                            
                            if (minVersionCode == null) minVersionCode = 0L;
                            if (latestVersionCode == null) latestVersionCode = 0L;
                            if (latestVersionName == null) latestVersionName = "Nueva versión";
                            if (updateMessage == null) updateMessage = "Hay una nueva versión disponible con mejoras y correcciones.";
                            
                            // Verificar si hay actualización disponible
                            if (latestVersionCode > currentVersionCode) {
                                Log.d(TAG, "Actualización disponible: " + latestVersionName + " (" + latestVersionCode + ")");
                                listener.onUpdateAvailable(
                                    currentVersionCode,
                                    latestVersionCode.intValue(),
                                    latestVersionName,
                                    updateMessage
                                );
                            } else {
                                Log.d(TAG, "No hay actualizaciones disponibles");
                                listener.onNoUpdateAvailable();
                            }
                        } else {
                            Log.w(TAG, "Documento de versión no encontrado en Firestore");
                            listener.onNoUpdateAvailable();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al verificar actualización", e);
                        listener.onError("No se pudo verificar actualizaciones: " + e.getMessage());
                    });
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error al obtener información del paquete", e);
            listener.onError("Error al obtener información de la app");
        }
    }
    
    /**
     * Abre Google Play Store para actualizar la aplicación.
     * Si Google Play no está disponible, intenta abrir en el navegador.
     * 
     * @param context Contexto de la aplicación
     */
    public static void openPlayStore(Context context) {
        try {
            String packageName = context.getPackageName();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + packageName));
            context.startActivity(intent);
        } catch (Exception e) {
            // Si no está instalado Google Play, abrir en navegador
            try {
                String packageName = context.getPackageName();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                context.startActivity(intent);
            } catch (Exception ex) {
                Log.e(TAG, "Error al abrir Play Store", ex);
            }
        }
    }
}

