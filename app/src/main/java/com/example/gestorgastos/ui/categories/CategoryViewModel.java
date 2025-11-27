package com.example.gestorgastos.ui.categories;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import com.example.gestorgastos.data.local.entity.UserEntity;
import com.example.gestorgastos.data.repository.CategoryRepository;
import com.example.gestorgastos.data.repository.CategoryRepositoryImpl;
import com.example.gestorgastos.domain.repository.AuthRepository;
import com.example.gestorgastos.data.repository.AuthRepositoryImpl;
import android.util.Log;
import java.util.List;

public class CategoryViewModel extends AndroidViewModel {
    private static final String TAG = "CategoryViewModel";
    
    private final CategoryRepository categoryRepository;
    private final AuthRepository authRepository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    
    // Mantener referencia actualizada del usuario para verificar el plan
    private UserEntity currentUser;
    private final Observer<UserEntity> userObserver = user -> {
        if (user != null) {
            Log.d(TAG, "Usuario actualizado en CategoryViewModel - planId: " + user.planId);
            currentUser = user;
        } else {
            currentUser = null;
        }
    };
    
    public CategoryViewModel(@NonNull Application application) {
        super(application);
        this.categoryRepository = new CategoryRepositoryImpl(application);
        this.authRepository = new AuthRepositoryImpl(application);
        
        // Inicializar con el valor actual del usuario
        UserEntity initialUser = authRepository.getCurrentUser().getValue();
        if (initialUser != null) {
            currentUser = initialUser;
            Log.d(TAG, "Usuario inicial cargado - planId: " + initialUser.planId);
        }
        
        // Observar cambios en el usuario para mantener el plan actualizado
        authRepository.getCurrentUser().observeForever(userObserver);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Remover observer cuando el ViewModel se destruye
        authRepository.getCurrentUser().removeObserver(userObserver);
    }
    
    /**
     * Verifica si el usuario tiene plan premium
     * Usa la referencia actualizada del usuario en lugar de getValue()
     */
    private boolean isPremiumUser() {
        if (currentUser != null && currentUser.planId != null) {
            boolean isPremium = !"free".equalsIgnoreCase(currentUser.planId);
            Log.d(TAG, "Verificando plan premium - planId: " + currentUser.planId + ", isPremium: " + isPremium);
            return isPremium;
        }
        Log.d(TAG, "Usuario no disponible o planId es null");
        return false;
    }
    
    // LiveData para las categorías
    public LiveData<List<CategoryEntity>> getCategoriesByUser(String userUid) {
        return categoryRepository.getCategoriesByUser(userUid);
    }
    
    public LiveData<List<CategoryEntity>> getAllCategoriesByUser(String userUid) {
        return categoryRepository.getAllCategoriesByUser(userUid);
    }
    
    public LiveData<List<CategoryEntity>> getActiveCategoriesByUser(String userUid) {
        return categoryRepository.getActiveCategoriesByUser(userUid);
    }
    
    public CategoryEntity getCategoryByIdIncludingInactive(long idLocal) {
        return categoryRepository.getCategoryByIdIncludingInactive(idLocal);
    }
    
    public CategoryRepository getCategoryRepository() {
        return categoryRepository;
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
    
    // Operaciones CRUD
    public void insertCategory(CategoryEntity category) {
        if (!isPremiumUser()) {
            errorMessage.setValue("Solo usuarios con plan premium pueden crear categorías. Actualiza tu plan para acceder a esta función.");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);
        
        categoryRepository.insertCategory(category, new CategoryRepository.RepositoryCallback<CategoryEntity>() {
            @Override
            public void onSuccess(CategoryEntity result) {
                isLoading.postValue(false);
                successMessage.postValue("Categoría creada exitosamente");
            }
            
            @Override
            public void onError(Exception error) {
                isLoading.postValue(false);
                errorMessage.postValue(translateError(error, "create"));
            }
        });
    }
    
    public void updateCategory(CategoryEntity category) {
        if (!isPremiumUser()) {
            errorMessage.setValue("Solo usuarios con plan premium pueden editar categorías. Actualiza tu plan para acceder a esta función.");
            return;
        }
        
        Log.d("CategoryViewModel", "updateCategory - ID: " + category.idLocal + ", Nombre: " + category.name);
        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);
        
        categoryRepository.updateCategory(category, new CategoryRepository.RepositoryCallback<CategoryEntity>() {
            @Override
            public void onSuccess(CategoryEntity result) {
                Log.d("CategoryViewModel", "Categoría actualizada exitosamente");
                isLoading.postValue(false);
                successMessage.postValue("Categoría actualizada exitosamente");
            }
            
            @Override
            public void onError(Exception error) {
                Log.e("CategoryViewModel", "Error al actualizar categoría", error);
                isLoading.postValue(false);
                errorMessage.postValue(translateError(error, "update"));
            }
        });
    }
    
    public void deleteCategory(long idLocal) {
        if (!isPremiumUser()) {
            errorMessage.setValue("Solo usuarios con plan premium pueden eliminar categorías. Actualiza tu plan para acceder a esta función.");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);
        
        categoryRepository.deleteCategory(idLocal, new CategoryRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                isLoading.postValue(false);
                successMessage.postValue("Categoría eliminada exitosamente");
            }
            
            @Override
            public void onError(Exception error) {
                isLoading.postValue(false);
                errorMessage.postValue(translateError(error, "delete"));
            }
        });
    }
    
    /**
     * Verifica si el usuario actual tiene plan premium
     */
    public boolean hasPremiumPlan() {
        return isPremiumUser();
    }
    
    // Métodos de utilidad
    public void clearMessages() {
        errorMessage.setValue(null);
        successMessage.setValue(null);
    }
    
    public CategoryEntity createDefaultCategory(String userUid, String name, String icono) {
        CategoryEntity category = new CategoryEntity();
        category.userUid = userUid;
        category.name = name;
        category.icono = icono;
        category.isActive = true;
        return category;
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
        
        // Errores de permisos
        if (lowerError.contains("permission denied") || lowerError.contains("unauthorized")) {
            return "🔒 Sin permisos\n\n" +
                   "No tienes permisos para realizar esta acción.\n\n" +
                   "Por favor, verifica tu sesión e intenta de nuevo.";
        }
        
        // Mensajes según el contexto
        String baseMessage;
        if ("create".equals(context)) {
            baseMessage = "❌ Error al crear categoría\n\n";
        } else if ("update".equals(context)) {
            baseMessage = "❌ Error al actualizar categoría\n\n";
        } else if ("delete".equals(context)) {
            baseMessage = "❌ Error al eliminar categoría\n\n";
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
