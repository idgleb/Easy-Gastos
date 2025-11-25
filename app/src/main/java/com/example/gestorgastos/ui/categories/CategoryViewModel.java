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
                errorMessage.postValue("Error al crear categoría: " + error.getMessage());
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
                errorMessage.postValue("Error al actualizar categoría: " + error.getMessage());
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
                errorMessage.postValue("Error al eliminar categoría: " + error.getMessage());
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
}
