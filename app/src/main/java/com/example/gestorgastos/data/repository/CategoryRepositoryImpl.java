package com.example.gestorgastos.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import com.example.gestorgastos.data.local.AppDatabase;
import com.example.gestorgastos.data.local.dao.CategoryDao;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import com.example.gestorgastos.util.DateTimeUtil;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryRepositoryImpl implements CategoryRepository {
    private final CategoryDao categoryDao;
    private final ExecutorService executor;
    
    public CategoryRepositoryImpl(Context context) {
        AppDatabase database = AppDatabase.getDatabase(context);
        this.categoryDao = database.categoryDao();
        this.executor = Executors.newFixedThreadPool(4);
    }
    
    @Override
    public LiveData<List<CategoryEntity>> getCategoriesByUser(String userUid) {
        return categoryDao.getCategoriesByUser(userUid);
    }
    
    @Override
    public LiveData<List<CategoryEntity>> getAllCategoriesByUser(String userUid) {
        Log.d("CategoryRepositoryImpl", "getAllCategoriesByUser llamado para usuario: " + userUid);
        return categoryDao.getAllCategoriesByUser(userUid);
    }
    
    @Override
    public LiveData<List<CategoryEntity>> getActiveCategoriesByUser(String userUid) {
        return categoryDao.getActiveCategoriesByUser(userUid);
    }
    
    @Override
    public List<CategoryEntity> getActiveCategoriesByUserSync(String userUid) {
        return categoryDao.getActiveCategoriesByUserSync(userUid);
    }
    
    @Override
    public CategoryEntity getCategoryByRemoteId(String remoteId) {
        return categoryDao.getCategoryByRemoteId(remoteId);
    }
    
    @Override
    public CategoryEntity getCategoryById(long idLocal) {
        return categoryDao.getCategoryById(idLocal);
    }
    
    @Override
    public CategoryEntity getCategoryByIdIncludingInactive(long idLocal) {
        return categoryDao.getCategoryByIdIncludingInactive(idLocal);
    }
    
    @Override
    public void insertCategory(CategoryEntity category, RepositoryCallback<CategoryEntity> callback) {
        executor.execute(() -> {
            try {
                // Establecer valores por defecto
                category.updatedAt = DateTimeUtil.getCurrentEpochMillis();
                category.syncState = "PENDING";
                
                // Insertar en Room
                long categoryId = categoryDao.insertCategory(category);
                category.idLocal = categoryId;
                
                // TODO: Sincronizar con Firestore más tarde
                
                callback.onSuccess(category);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    @Override
    public void updateCategory(CategoryEntity category, RepositoryCallback<CategoryEntity> callback) {
        executor.execute(() -> {
            try {
                // Actualizar timestamp y estado de sincronización
                category.updatedAt = DateTimeUtil.getCurrentEpochMillis();
                category.syncState = "PENDING";
                
                // Actualizar en Room
                categoryDao.updateCategory(category);
                
                // TODO: Sincronizar con Firestore más tarde
                
                callback.onSuccess(category);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    @Override
    public void deleteCategory(long idLocal, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                long deletedAt = DateTimeUtil.getCurrentEpochMillis();
                long updatedAt = DateTimeUtil.getCurrentEpochMillis();
                
                // Soft delete en Room
                categoryDao.softDeleteCategory(idLocal, deletedAt, updatedAt);
                
                // TODO: Sincronizar con Firestore más tarde
                
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    @Override
    public List<CategoryEntity> getPendingCategories() {
        return categoryDao.getPendingCategories();
    }
    
    @Override
    public void updateSyncState(long idLocal, String syncState) {
        executor.execute(() -> {
            categoryDao.updateSyncState(idLocal, syncState);
        });
    }
    
    public void debugCategories(String userUid) {
        executor.execute(() -> {
            List<CategoryEntity> allCategories = categoryDao.getAllCategoriesByUserDebug(userUid);
            Log.d("CategoryRepositoryImpl", "=== DEBUG CATEGORÍAS ===");
            Log.d("CategoryRepositoryImpl", "Usuario: " + userUid);
            Log.d("CategoryRepositoryImpl", "Total categorías en BD: " + allCategories.size());
            for (CategoryEntity cat : allCategories) {
                Log.d("CategoryRepositoryImpl", "ID: " + cat.idLocal + 
                      ", Nombre: " + cat.name + 
                      ", Activa: " + cat.isActive + 
                      ", DeletedAt: " + cat.deletedAt);
            }
            Log.d("CategoryRepositoryImpl", "=== FIN DEBUG ===");
        });
    }
}
