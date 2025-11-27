package com.example.gestorgastos.util;

import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.FirebaseFirestoreException;
import java.net.UnknownHostException;

/**
 * Utilidad singleton para notificar errores de conexión de Firestore
 * desde cualquier parte de la aplicación (repositorios, listeners, etc.)
 */
public class ConnectionErrorNotifier {
    private static final String TAG = "ConnectionErrorNotifier";
    private static ConnectionErrorNotifier instance;
    private final MutableLiveData<String> connectionErrorLiveData = new MutableLiveData<>();
    
    private ConnectionErrorNotifier() {
        // Constructor privado para singleton
    }
    
    public static synchronized ConnectionErrorNotifier getInstance() {
        if (instance == null) {
            instance = new ConnectionErrorNotifier();
        }
        return instance;
    }
    
    /**
     * Obtiene el LiveData para observar errores de conexión
     */
    public MutableLiveData<String> getConnectionError() {
        return connectionErrorLiveData;
    }
    
    /**
     * Notifica un error de conexión si es UNAVAILABLE o UnknownHostException
     */
    public void notifyIfConnectionError(Exception error) {
        if (error == null) {
            return;
        }
        
        // Verificar si es un error de Firestore UNAVAILABLE
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreError = (FirebaseFirestoreException) error;
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
                String errorMessage = "📡 Sin conexión a internet\n\n" +
                        "No se pudo conectar con los servidores de Firebase.\n\n" +
                        "Por favor:\n\n" +
                        "• Verifica que tengas conexión a internet activa\n" +
                        "• Asegúrate de tener WiFi o datos móviles habilitados\n" +
                        "• Revisa que no estés en modo avión\n" +
                        "• Intenta de nuevo cuando tengas conexión estable";
                connectionErrorLiveData.postValue(errorMessage);
                Log.w(TAG, "⚠️ Error de conexión UNAVAILABLE detectado");
                return;
            }
        }
        
        // Verificar si la causa es UnknownHostException
        Throwable cause = error.getCause();
        while (cause != null) {
            if (cause instanceof UnknownHostException) {
                String errorMessage = "📡 Sin conexión a internet\n\n" +
                        "No se pudo conectar con los servidores de Firebase.\n\n" +
                        "Por favor:\n\n" +
                        "• Verifica que tengas conexión a internet activa\n" +
                        "• Asegúrate de tener WiFi o datos móviles habilitados\n" +
                        "• Revisa que no estés en modo avión\n" +
                        "• Intenta de nuevo cuando tengas conexión estable";
                connectionErrorLiveData.postValue(errorMessage);
                Log.w(TAG, "⚠️ Error UnknownHostException detectado");
                return;
            }
            cause = cause.getCause();
        }
        
        // Verificar si el mensaje contiene indicadores de error de conexión
        String errorMsg = error.getMessage();
        if (errorMsg != null) {
            String lowerError = errorMsg.toLowerCase();
            if (lowerError.contains("unavailable") ||
                lowerError.contains("unable to resolve host") ||
                lowerError.contains("unknownhostexception") ||
                lowerError.contains("no address associated with hostname") ||
                lowerError.contains("firestore.googleapis.com") ||
                lowerError.contains("eai_nodata")) {
                String errorMessage = "📡 Sin conexión a internet\n\n" +
                        "No se pudo conectar con los servidores de Firebase.\n\n" +
                        "Por favor:\n\n" +
                        "• Verifica que tengas conexión a internet activa\n" +
                        "• Asegúrate de tener WiFi o datos móviles habilitados\n" +
                        "• Revisa que no estés en modo avión\n" +
                        "• Intenta de nuevo cuando tengas conexión estable";
                connectionErrorLiveData.postValue(errorMessage);
                Log.w(TAG, "⚠️ Error de conexión detectado en mensaje");
            }
        }
    }
    
    /**
     * Limpia el error de conexión (cuando la conexión se restablece)
     */
    public void clearError() {
        String currentValue = connectionErrorLiveData.getValue();
        if (currentValue != null && !currentValue.isEmpty()) {
            Log.d(TAG, "✅ Limpiando error de conexión - había: " + currentValue.substring(0, Math.min(30, currentValue.length())));
            connectionErrorLiveData.postValue(null);
        } else {
            Log.d(TAG, "ℹ️ No hay error de conexión para limpiar");
        }
    }
    
    /**
     * Verifica si hay un error de conexión activo
     */
    public boolean hasConnectionError() {
        String currentValue = connectionErrorLiveData.getValue();
        return currentValue != null && !currentValue.isEmpty();
    }
}

