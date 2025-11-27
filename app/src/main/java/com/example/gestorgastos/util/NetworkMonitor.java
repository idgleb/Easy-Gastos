package com.example.gestorgastos.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.gestorgastos.work.SyncWorker;

/**
 * Monitor de estado de red para detectar cuando no hay conexión a internet
 * y notificar al usuario a través de ConnectionErrorNotifier
 */
public class NetworkMonitor {
    private static final String TAG = "NetworkMonitor";
    private static NetworkMonitor instance;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isMonitoring = false;
    private boolean lastKnownState = true; // Asumir conectado inicialmente
    private Context appContext; // Guardar contexto para WorkManager
    
    private NetworkMonitor() {
        // Constructor privado para singleton
    }
    
    public static synchronized NetworkMonitor getInstance() {
        if (instance == null) {
            instance = new NetworkMonitor();
        }
        return instance;
    }
    
    /**
     * Inicia el monitoreo del estado de red
     */
    public void startMonitoring(Context context) {
        if (isMonitoring) {
            Log.d(TAG, "El monitoreo de red ya está activo");
            return;
        }
        
        // Guardar contexto de aplicación para usar con WorkManager
        this.appContext = context.getApplicationContext();
        
        connectivityManager = (ConnectivityManager) appContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            Log.e(TAG, "No se pudo obtener ConnectivityManager");
            return;
        }
        
        // Verificar estado inicial
        checkInitialNetworkState();
        
        // Crear NetworkRequest para monitorear cambios
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build();
        
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Log.d(TAG, "✅ Red disponible - verificando validación de internet");
                // No limpiar el error inmediatamente, esperar a que se valide la conexión
                // El error se limpiará en onCapabilitiesChanged cuando se valide
            }
            
            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                Log.w(TAG, "❌ Red perdida - notificando falta de conexión");
                lastKnownState = false;
                // Usar post para asegurar que se ejecute en el hilo principal
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    notifyNoConnection();
                });
            }
            
            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                boolean isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                
                Log.d(TAG, "onCapabilitiesChanged - hasInternet: " + hasInternet + ", isValidated: " + isValidated);
                
                if (hasInternet && isValidated) {
                    Log.d(TAG, "✅ Red con internet validado - limpiando error de conexión");
                    boolean wasOffline = !lastKnownState;
                    lastKnownState = true;
                    // Usar post para asegurar que se ejecute en el hilo principal
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        ConnectionErrorNotifier.getInstance().clearError();
                        
                        // Si estábamos offline y ahora recuperamos conexión, sincronizar datos pendientes
                        if (wasOffline) {
                            Log.d(TAG, "🔄 Conexión recuperada - disparando sincronización automática de datos pendientes");
                            triggerAutoSync();
                        }
                    });
                } else {
                    Log.w(TAG, "⚠️ Red sin internet validado - notificando falta de conexión");
                    lastKnownState = false;
                    // Usar post para asegurar que se ejecute en el hilo principal
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        notifyNoConnection();
                    });
                }
            }
        };
        
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
            isMonitoring = true;
            Log.d(TAG, "✅ Monitoreo de red iniciado");
        } catch (Exception e) {
            Log.e(TAG, "Error al registrar NetworkCallback", e);
        }
    }
    
    /**
     * Detiene el monitoreo del estado de red
     */
    public void stopMonitoring() {
        if (!isMonitoring || connectivityManager == null || networkCallback == null) {
            return;
        }
        
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            isMonitoring = false;
            Log.d(TAG, "Monitoreo de red detenido");
        } catch (Exception e) {
            Log.e(TAG, "Error al desregistrar NetworkCallback", e);
        }
    }
    
    /**
     * Verifica el estado inicial de la red
     */
    private void checkInitialNetworkState() {
        if (connectivityManager == null) {
            Log.e(TAG, "ConnectivityManager es null, no se puede verificar estado de red");
            return;
        }
        
        try {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) {
                Log.w(TAG, "⚠️ No hay red activa al iniciar - notificando falta de conexión");
                lastKnownState = false;
                // Usar postDelayed para asegurar que se notifique después de que MainActivity esté lista
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    notifyNoConnection();
                }, 1000);
                return;
            }
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            if (capabilities == null) {
                Log.w(TAG, "⚠️ No se pudieron obtener capacidades de red - notificando falta de conexión");
                lastKnownState = false;
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    notifyNoConnection();
                }, 1000);
                return;
            }
            
            boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            boolean isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            
            Log.d(TAG, "Estado inicial de red - hasInternet: " + hasInternet + ", isValidated: " + isValidated);
            
            if (!hasInternet || !isValidated) {
                Log.w(TAG, "⚠️ Red sin internet validado al iniciar - notificando falta de conexión");
                lastKnownState = false;
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    notifyNoConnection();
                }, 1000);
            } else {
                Log.d(TAG, "✅ Red con internet validado al iniciar - verificando si hay error pendiente");
                lastKnownState = true;
                // Verificar si hay un error pendiente y limpiarlo
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    ConnectionErrorNotifier.getInstance().clearError();
                }, 500);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al verificar estado inicial de red", e);
            // En caso de error, asumir que no hay conexión
            lastKnownState = false;
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                notifyNoConnection();
            }, 1000);
        }
    }
    
    /**
     * Notifica que no hay conexión
     */
    private void notifyNoConnection() {
        String errorMessage = "📡 Sin conexión a internet\n\n" +
                "No se pudo conectar con los servidores de Firebase.\n\n" +
                "Por favor:\n\n" +
                "• Verifica que tengas conexión a internet activa\n" +
                "• Asegúrate de tener WiFi o datos móviles habilitados\n" +
                "• Revisa que no estés en modo avión\n" +
                "• Intenta de nuevo cuando tengas conexión estable";
        
        // Notificar directamente al ConnectionErrorNotifier
        Log.w(TAG, "⚠️ Notificando falta de conexión a internet - publicando mensaje en LiveData");
        ConnectionErrorNotifier.getInstance().getConnectionError().postValue(errorMessage);
        Log.d(TAG, "✅ Mensaje de error de conexión publicado en LiveData");
    }
    
    /**
     * Verifica si hay conexión a internet en este momento
     */
    public boolean isConnected() {
        return lastKnownState;
    }
    
    /**
     * Verifica el estado actual de la red y actualiza el error si es necesario
     * Útil para verificar después de cambios de fragmento o cuando se sospecha que el estado cambió
     */
    public void checkCurrentNetworkState() {
        if (connectivityManager == null) {
            return;
        }
        
        try {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) {
                Log.w(TAG, "⚠️ Verificación: No hay red activa");
                if (lastKnownState) {
                    lastKnownState = false;
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        notifyNoConnection();
                    });
                }
                return;
            }
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            if (capabilities == null) {
                Log.w(TAG, "⚠️ Verificación: No se pudieron obtener capacidades");
                if (lastKnownState) {
                    lastKnownState = false;
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        notifyNoConnection();
                    });
                }
                return;
            }
            
            boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            boolean isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            
            if (hasInternet && isValidated) {
                if (!lastKnownState) {
                    Log.d(TAG, "✅ Verificación: Red con internet validado - limpiando error");
                    lastKnownState = true;
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        ConnectionErrorNotifier.getInstance().clearError();
                        
                        // Conexión recuperada, sincronizar datos pendientes
                        Log.d(TAG, "🔄 Verificación: Conexión recuperada - disparando sincronización automática");
                        triggerAutoSync();
                    });
                }
            } else {
                if (lastKnownState || !ConnectionErrorNotifier.getInstance().hasConnectionError()) {
                    Log.w(TAG, "⚠️ Verificación: Red sin internet validado - notificando error");
                    lastKnownState = false;
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        notifyNoConnection();
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al verificar estado actual de red", e);
        }
    }
    
    /**
     * Dispara la sincronización automática de datos pendientes
     * cuando se recupera la conexión a internet
     */
    private void triggerAutoSync() {
        if (appContext == null) {
            Log.w(TAG, "⚠️ No se puede disparar sincronización: contexto no disponible");
            return;
        }
        
        try {
            // Crear una solicitud de trabajo única para sincronizar
            OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                    .addTag("auto_sync_on_reconnect")
                    .build();
            
            // Encolar el trabajo
            WorkManager.getInstance(appContext).enqueue(syncRequest);
            
            Log.d(TAG, "✅ Sincronización automática encolada - SyncWorker se ejecutará pronto");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error al encolar sincronización automática", e);
        }
    }
}

