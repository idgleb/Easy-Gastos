package com.example.gestorgastos.ui.categories;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import com.example.gestorgastos.data.repository.CategoryRepository;
import com.example.gestorgastos.data.repository.CategoryRepositoryImpl;
import android.util.Log;
import java.util.List;

public class CategoryViewModel extends AndroidViewModel {
    private final CategoryRepository categoryRepository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    
    public CategoryViewModel(@NonNull Application application) {
        super(application);
        this.categoryRepository = new CategoryRepositoryImpl(application);
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
