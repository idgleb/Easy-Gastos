package com.example.gestorgastos.ui.admin;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.gestorgastos.data.local.entity.UserEntity;
import com.example.gestorgastos.data.repository.AdminRepository;
import com.example.gestorgastos.data.repository.AdminRepositoryImpl;
import android.util.Log;
import java.util.List;

public class AdminViewModel extends AndroidViewModel {
    private final AdminRepository adminRepository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<String> generatedPassword = new MutableLiveData<>();
    
    public AdminViewModel(@NonNull Application application) {
        super(application);
        this.adminRepository = new AdminRepositoryImpl(application);
    }
    
    // LiveData para los usuarios
    public LiveData<List<UserEntity>> getAllUsers() {
        return adminRepository.getAllUsers();
    }
    
    // Estados de la UI
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }
    
    public LiveData<String> getGeneratedPassword() {
        return generatedPassword;
    }
    
    // Operaciones CRUD
    public void createUser(UserEntity user, String password) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);
        
        String finalPassword;
        boolean generatedPasswordFlag = false;
        if (password != null && !password.trim().isEmpty()) {
            if (password.trim().length() < 6) {
                isLoading.setValue(false);
                errorMessage.setValue("La contraseña debe tener al menos 6 caracteres");
                return;
            }
            finalPassword = password.trim();
        } else {
            finalPassword = generateTemporaryPassword();
            generatedPasswordFlag = true;
        }
        
        final String passwordToShow = finalPassword;
        final boolean shouldShowPassword = generatedPasswordFlag;
        
        adminRepository.createUser(user, finalPassword, new AdminRepository.RepositoryCallback<UserEntity>() {
            @Override
            public void onSuccess(UserEntity result) {
                isLoading.postValue(false);
                successMessage.postValue("Usuario creado correctamente.");
                if (shouldShowPassword) {
                    generatedPassword.postValue(passwordToShow);
                } else {
                    generatedPassword.postValue(null);
                }
            }
            
            @Override
            public void onError(Exception error) {
                isLoading.postValue(false);
                String friendlyMessage = translateError(error, "create");
                errorMessage.postValue(friendlyMessage);
            }
        });
    }
    
    public void clearGeneratedPassword() {
        generatedPassword.setValue(null);
    }
    
    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder password = new StringBuilder();
        
        password.append(chars.charAt(random.nextInt(26)));
        password.append(chars.charAt(26 + random.nextInt(26)));
        password.append(chars.charAt(52 + random.nextInt(10)));
        password.append(chars.charAt(62 + random.nextInt(7)));
        
        for (int i = 4; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        
        return new String(passwordArray);
    }
    
    public void updateUser(UserEntity user) {
        Log.d("AdminViewModel", "updateUser - UID: " + user.uid + ", Nombre: " + user.name);
        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);
        
        adminRepository.updateUser(user, new AdminRepository.RepositoryCallback<UserEntity>() {
            @Override
            public void onSuccess(UserEntity result) {
                Log.d("AdminViewModel", "Usuario actualizado exitosamente");
                isLoading.postValue(false);
                // Mensaje que indica que los cambios están guardados localmente
                // La sincronización con Firestore ocurrirá automáticamente en background
                successMessage.postValue("Usuario actualizado. Los cambios se sincronizarán automáticamente cuando haya conexión.");
            }
            
            @Override
            public void onError(Exception error) {
                Log.e("AdminViewModel", "Error al actualizar usuario", error);
                isLoading.postValue(false);
                String friendlyMessage = translateError(error, "update");
                errorMessage.postValue(friendlyMessage);
            }
        });
    }
    
    public void deleteUser(String uid) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);
        
        adminRepository.deleteUser(uid, new AdminRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                isLoading.postValue(false);
                successMessage.postValue("Usuario eliminado exitosamente");
            }
            
            @Override
            public void onError(Exception error) {
                isLoading.postValue(false);
                String friendlyMessage = translateError(error, "delete");
                errorMessage.postValue(friendlyMessage);
            }
        });
    }
    
    // Métodos de utilidad
    public void clearMessages() {
        errorMessage.setValue(null);
        successMessage.setValue(null);
    }
    
    public void refreshUsers() {
        // Recargar usuarios desde Firestore
        if (adminRepository instanceof AdminRepositoryImpl) {
            ((AdminRepositoryImpl) adminRepository).loadUsersFromFirestore();
        }
    }
    
    /**
     * Traduce los errores técnicos a mensajes amigables para el usuario
     */
    private String translateError(Exception error, String context) {
        if (error == null) {
            return "Ocurrió un error inesperado. Por favor, intenta de nuevo.";
        }
        
        // Verificar si es un error de Firestore UNAVAILABLE
        if (error instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
            com.google.firebase.firestore.FirebaseFirestoreException firestoreError = 
                (com.google.firebase.firestore.FirebaseFirestoreException) error;
            if (firestoreError.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE) {
                return "📡 Sin conexión a internet\n\n" +
                       "No se pudo conectar con los servidores de Firebase.\n\n" +
                       "Por favor:\n\n" +
                       "• Verifica que tengas conexión a internet activa\n" +
                       "• Asegúrate de tener WiFi o datos móviles habilitados\n" +
                       "• Revisa que no estés en modo avión\n" +
                       "• Intenta de nuevo cuando tengas conexión estable";
            }
        }
        
        // Verificar si la causa es UnknownHostException
        Throwable cause = error.getCause();
        while (cause != null) {
            if (cause instanceof java.net.UnknownHostException) {
                return "📡 Sin conexión a internet\n\n" +
                       "No se pudo conectar con los servidores de Firebase.\n\n" +
                       "Por favor:\n\n" +
                       "• Verifica que tengas conexión a internet activa\n" +
                       "• Asegúrate de tener WiFi o datos móviles habilitados\n" +
                       "• Revisa que no estés en modo avión\n" +
                       "• Intenta de nuevo cuando tengas conexión estable";
            }
            cause = cause.getCause();
        }
        
        String errorMsg = error.getMessage();
        if (errorMsg == null || errorMsg.isEmpty()) {
            errorMsg = error.getClass().getSimpleName();
        }
        
        String lowerError = errorMsg.toLowerCase();
        
        // Errores de Firestore UNAVAILABLE y resolución de hostname
        if (lowerError.contains("unavailable") || 
            lowerError.contains("unable to resolve host") ||
            lowerError.contains("unknownhostexception") ||
            lowerError.contains("no address associated with hostname") ||
            lowerError.contains("firestore.googleapis.com") ||
            lowerError.contains("eai_nodata")) {
            return "📡 Sin conexión a internet\n\n" +
                   "No se pudo conectar con los servidores de Firebase.\n\n" +
                   "Por favor:\n\n" +
                   "• Verifica que tengas conexión a internet activa\n" +
                   "• Asegúrate de tener WiFi o datos móviles habilitados\n" +
                   "• Revisa que no estés en modo avión\n" +
                   "• Intenta de nuevo cuando tengas conexión estable";
        }
        
        // Errores de red generales
        if (lowerError.contains("network") || lowerError.contains("timeout") || 
            lowerError.contains("connection") || lowerError.contains("unreachable") ||
            lowerError.contains("failed to connect") || lowerError.contains("socket") ||
            lowerError.contains("connection refused") || lowerError.contains("connection reset")) {
            return "📡 Error de conexión\n\n" +
                   "No se pudo conectar con el servidor. Por favor:\n\n" +
                   "• Verifica tu conexión a internet\n" +
                   "• Asegúrate de tener WiFi o datos móviles activos\n" +
                   "• Intenta de nuevo en unos momentos";
        }
        
        // Errores de email duplicado
        if (lowerError.contains("email-already-in-use") || lowerError.contains("already in use")) {
            return "📧 Email ya registrado\n\n" +
                   "Este correo electrónico ya está siendo usado por otro usuario.\n\n" +
                   "Por favor, utiliza un email diferente.";
        }
        
        // Errores de email inválido
        if (lowerError.contains("invalid-email") || lowerError.contains("invalid email") ||
            lowerError.contains("badly formatted")) {
            return "📧 Email inválido\n\n" +
                   "El formato del correo electrónico no es válido.\n\n" +
                   "Por favor, verifica que el email tenga el formato correcto:\n" +
                   "ejemplo@dominio.com";
        }
        
        // Errores de contraseña débil
        if (lowerError.contains("weak password") || lowerError.contains("at least 6")) {
            return "🔐 Contraseña débil\n\n" +
                   "La contraseña debe tener al menos 6 caracteres.\n\n" +
                   "Te recomendamos usar una combinación de:\n" +
                   "• Letras mayúsculas y minúsculas\n" +
                   "• Números\n" +
                   "• Símbolos especiales";
        }
        
        // Errores de autenticación/autorización
        if (lowerError.contains("permission denied") || lowerError.contains("unauthorized") ||
            lowerError.contains("not authorized") || lowerError.contains("403")) {
            return "🔒 Sin permisos\n\n" +
                   "No tienes permisos para realizar esta acción.\n\n" +
                   "Por favor, verifica que tengas el rol de administrador.";
        }
        
        // Errores de servidor
        if (lowerError.contains("internal error") || lowerError.contains("500") ||
            lowerError.contains("server error")) {
            return "⚠️ Error del servidor\n\n" +
                   "Ocurrió un error en el servidor. Por favor:\n\n" +
                   "• Intenta de nuevo en unos momentos\n" +
                   "• Si el problema persiste, contacta al soporte";
        }
        
        // Errores de token/autenticación
        if (lowerError.contains("token") || lowerError.contains("authentication") ||
            lowerError.contains("401")) {
            return "🔑 Error de autenticación\n\n" +
                   "Tu sesión ha expirado o no tienes permisos.\n\n" +
                   "Por favor, cierra sesión e inicia sesión nuevamente.";
        }
        
        // Mensajes según el contexto
        String baseMessage;
        if ("create".equals(context)) {
            baseMessage = "❌ Error al crear usuario\n\n";
        } else if ("update".equals(context)) {
            baseMessage = "❌ Error al actualizar usuario\n\n";
        } else if ("delete".equals(context)) {
            baseMessage = "❌ Error al eliminar usuario\n\n";
        } else {
            baseMessage = "❌ Error\n\n";
        }
        
        // Si el mensaje es muy técnico, mostrar uno genérico
        if (errorMsg.length() > 100 || lowerError.contains("exception") || 
            lowerError.contains("stacktrace") || lowerError.contains("at ")) {
            return baseMessage + 
                   "Ocurrió un error inesperado al procesar tu solicitud.\n\n" +
                   "Por favor, intenta de nuevo. Si el problema persiste, contacta al soporte técnico.";
        }
        
        // Mostrar el mensaje original pero formateado
        return baseMessage + errorMsg;
    }
}

