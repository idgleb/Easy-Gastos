package com.example.gestorgastos.data.repository;

import androidx.lifecycle.LiveData;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import java.util.List;

public interface CategoryRepository {
    LiveData<List<CategoryEntity>> getCategoriesByUser(String userUid);
    
    LiveData<List<CategoryEntity>> getAllCategoriesByUser(String userUid);
    
    LiveData<List<CategoryEntity>> getActiveCategoriesByUser(String userUid);
    
    List<CategoryEntity> getActiveCategoriesByUserSync(String userUid);
    
    CategoryEntity getCategoryByRemoteId(String remoteId);
    
    CategoryEntity getCategoryById(long idLocal);
    
    CategoryEntity getCategoryByIdIncludingInactive(long idLocal);
    
    void insertCategory(CategoryEntity category, RepositoryCallback<CategoryEntity> callback);
    
    void updateCategory(CategoryEntity category, RepositoryCallback<CategoryEntity> callback);
    
    void deleteCategory(long idLocal, RepositoryCallback<Void> callback);
    
    List<CategoryEntity> getPendingCategories();
    
    void updateSyncState(long idLocal, String syncState);
    
    interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }
}
