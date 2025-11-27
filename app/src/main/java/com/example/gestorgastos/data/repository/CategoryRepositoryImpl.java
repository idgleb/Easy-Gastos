package com.example.gestorgastos.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.gestorgastos.data.local.AppDatabase;
import com.example.gestorgastos.data.local.dao.CategoryDao;
import com.example.gestorgastos.data.local.dao.ExpenseDao;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import com.example.gestorgastos.data.remote.FirestoreDataSource;
import com.example.gestorgastos.util.DateTimeUtil;
import com.example.gestorgastos.util.ConnectionErrorNotifier;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryRepositoryImpl implements CategoryRepository {
    private final CategoryDao categoryDao;
    private final ExpenseDao expenseDao;
    private final ExecutorService executor;
    private final FirestoreDataSource firestoreDataSource;
    
    public CategoryRepositoryImpl(Context context) {
        AppDatabase database = AppDatabase.getDatabase(context);
        this.categoryDao = database.categoryDao();
        this.expenseDao = database.expenseDao();
        this.executor = Executors.newFixedThreadPool(4);
        this.firestoreDataSource = new FirestoreDataSource();
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
                
                // Lanzar sincronización con Firestore de forma asíncrona (offline-first)
                syncCategoryWithFirestore(category);
                
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
                
                // Lanzar sincronización con Firestore de forma asíncrona
                syncCategoryWithFirestore(category);
                
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
                
                // Obtener la categoría actualizada y sincronizar soft delete con Firestore
                CategoryEntity deletedCategory = categoryDao.getCategoryById(idLocal);
                if (deletedCategory != null) {
                    deletedCategory.deletedAt = deletedAt;
                    deletedCategory.updatedAt = updatedAt;
                    deletedCategory.isActive = false;
                    deletedCategory.syncState = "PENDING";
                    syncCategoryWithFirestore(deletedCategory);
                }
                
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

    /**
     * Sincroniza todas las categorías pendientes con Firestore.
     * Se usa desde WorkManager para reintentos en segundo plano.
     */
    public void syncPendingCategoriesWithFirestore() {
        executor.execute(() -> {
            List<CategoryEntity> pending = categoryDao.getPendingCategories();
            Log.d("CategoryRepositoryImpl", "Sincronizando " + pending.size() + " categorías PENDING con Firestore");
            for (CategoryEntity category : pending) {
                syncCategoryWithFirestore(category);
            }
        });
    }

    /**
     * Sincroniza categorías desde Firestore hacia Room.
     * Si lastSyncMillis es 0, realiza sincronización completa.
     * Si lastSyncMillis > 0, realiza sincronización incremental (solo cambios desde ese timestamp).
     */
    @Override
    public void syncFromFirestore(String userUid, long lastSyncMillis, RepositoryCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                Task<QuerySnapshot> task;
                
                if (lastSyncMillis == 0) {
                    // Sincronización completa: obtener todas las categorías
                    Log.d("CategoryRepositoryImpl", "Sincronización completa de categorías desde Firestore para usuario: " + userUid);
                    task = firestoreDataSource.getCategories(userUid);
                } else {
                    // Sincronización incremental: solo cambios desde lastSyncMillis
                    Timestamp lastSync = new Timestamp(
                            lastSyncMillis / 1000,
                            (int) ((lastSyncMillis % 1000) * 1_000_000)
                    );
                    Log.d("CategoryRepositoryImpl", "Sincronización incremental de categorías desde Firestore (desde: " + lastSyncMillis + ") para usuario: " + userUid);
                    task = firestoreDataSource.getCategoriesAfter(userUid, lastSync);
                }
                
                task.addOnSuccessListener(querySnapshot -> {
                    executor.execute(() -> {
                        try {
                            int syncedCount = 0;
                            
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    try {
                                        String remoteId = doc.getId();
                                        String name = doc.getString("name");
                                        String icon = doc.getString("icon");
                                        Boolean isActive = doc.getBoolean("is_active");
                                        Timestamp updatedAt = doc.getTimestamp("updated_at");
                                        Timestamp deletedAt = doc.getTimestamp("deleted_at");
                                        
                                        // Buscar si ya existe en Room por remoteId
                                        CategoryEntity existing = categoryDao.getCategoryByRemoteId(remoteId);
                                        
                                        // Si no se encuentra por remoteId, buscar por atributos (para categorías creadas en offline)
                                        if (existing == null && name != null && icon != null) {
                                            existing = categoryDao.findCategoryByAttributes(userUid, name, icon);
                                            if (existing != null) {
                                                Log.d("CategoryRepositoryImpl", "Categoría encontrada por atributos (creada en offline): " + existing.name);
                                            }
                                        }
                                        
                                        if (existing != null) {
                                            // Actualizar categoría existente
                                            existing.name = name != null ? name : existing.name;
                                            existing.icono = icon != null ? icon : existing.icono;
                                            existing.isActive = isActive != null ? isActive : true;
                                            if (updatedAt != null) {
                                                existing.updatedAt = updatedAt.toDate().getTime();
                                            }
                                            if (deletedAt != null) {
                                                existing.deletedAt = deletedAt.toDate().getTime();
                                                existing.isActive = false;
                                            } else {
                                                existing.deletedAt = null;
                                            }
                                            existing.syncState = "SYNCED";
                                            categoryDao.updateCategory(existing);
                                            syncedCount++;
                                        } else {
                                            // Insertar nueva categoría
                                            CategoryEntity category = new CategoryEntity();
                                            category.remoteId = remoteId;
                                            category.userUid = userUid;
                                            category.name = name != null ? name : "Sin nombre";
                                            category.icono = icon != null ? icon : "⭐";
                                            category.isActive = isActive != null ? isActive : true;
                                            if (updatedAt != null) {
                                                category.updatedAt = updatedAt.toDate().getTime();
                                            } else {
                                                category.updatedAt = System.currentTimeMillis();
                                            }
                                            if (deletedAt != null) {
                                                category.deletedAt = deletedAt.toDate().getTime();
                                                category.isActive = false;
                                            }
                                            category.syncState = "SYNCED";
                                            
                                            long idLocal = categoryDao.insertCategory(category);
                                            category.idLocal = idLocal;
                                            syncedCount++;
                                        }
                                    } catch (Exception e) {
                                        Log.e("CategoryRepositoryImpl", "Error al procesar categoría desde Firestore: " + doc.getId(), e);
                                    }
                                }
                            }
                            
                            Log.d("CategoryRepositoryImpl", "Sincronización completada: " + syncedCount + " categorías procesadas");
                            if (callback != null) {
                                callback.onSuccess(syncedCount);
                            }
                        } catch (Exception e) {
                            Log.e("CategoryRepositoryImpl", "Error al procesar categorías desde Firestore", e);
                            if (callback != null) {
                                callback.onError(e);
                            }
                        }
                    });
                }).addOnFailureListener(e -> {
                    Log.e("CategoryRepositoryImpl", "Error al obtener categorías desde Firestore", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
            } catch (Exception e) {
                Log.e("CategoryRepositoryImpl", "Error al iniciar sincronización desde Firestore", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * Sincroniza una categoría con Firestore de forma asíncrona.
     * Estrategia offline-first: la operación local ya se realizó;
     * aquí solo intentamos reflejarla en la nube y actualizar syncState/remoteId.
     */
    public void syncCategoryWithFirestore(CategoryEntity category) {
        // Si no tenemos userUid no podemos sincronizar
        if (category.userUid == null || category.userUid.trim().isEmpty()) {
            Log.w("CategoryRepositoryImpl", "No se puede sincronizar categoría sin userUid");
            return;
        }

        // Construir payload para Firestore
        Map<String, Object> data = new HashMap<>();
        data.put("name", category.name);
        data.put("icon", category.icono);
        data.put("is_active", category.isActive);

        // updated_at
        Timestamp updatedAtTs = new Timestamp(
                category.updatedAt / 1000,
                (int) ((category.updatedAt % 1000) * 1_000_000)
        );
        data.put("updated_at", updatedAtTs);

        // deleted_at (soft delete)
        if (category.deletedAt != null) {
            Timestamp deletedAtTs = new Timestamp(
                    category.deletedAt / 1000,
                    (int) ((category.deletedAt % 1000) * 1_000_000)
            );
            data.put("deleted_at", deletedAtTs);
        } else {
            data.put("deleted_at", null);
        }

        // Si la categoría está eliminada, solo actualizamos flags en Firestore
        boolean isSoftDeleted = category.deletedAt != null || !category.isActive;

        if (category.remoteId == null || category.remoteId.trim().isEmpty()) {
            if (isSoftDeleted) {
                // No tiene sentido crear una categoría nueva en remoto ya eliminada
                Log.d("CategoryRepositoryImpl", "Categoría soft-deleted sin remoteId, no se crea en Firestore");
                return;
            }

            // Crear nueva categoría en Firestore
            firestoreDataSource.createCategory(category.userUid, data)
                    .addOnSuccessListener(docRef -> {
                        String remoteId = docRef.getId();
                        Log.d("CategoryRepositoryImpl", "Categoría sincronizada en Firestore. remoteId=" + remoteId);

                        executor.execute(() -> {
                            category.remoteId = remoteId;
                            category.syncState = "SYNCED";
                            categoryDao.updateCategory(category);

                            // Actualizar todos los gastos que usaban el fallback local_X para esta categoría
                            String oldLocalRef = "local_" + category.idLocal;
                            expenseDao.migrateCategoryRemoteId(oldLocalRef, remoteId);
                            Log.d("CategoryRepositoryImpl", "Migrados gastos con categoryRemoteId=" + oldLocalRef + " a remoteId=" + remoteId);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("CategoryRepositoryImpl", "Error al crear categoría en Firestore", e);
                        // Notificar error de conexión si aplica
                        ConnectionErrorNotifier.getInstance().notifyIfConnectionError(e);
                        executor.execute(() ->
                                categoryDao.updateSyncState(category.idLocal, "ERROR")
                        );
                    });
        } else {
            // Actualizar categoría existente (incluye soft delete)
            firestoreDataSource.updateCategory(category.userUid, category.remoteId, data)
                    .addOnSuccessListener(unused -> {
                        Log.d("CategoryRepositoryImpl", "Categoría actualizada en Firestore. remoteId=" + category.remoteId);
                        executor.execute(() ->
                                categoryDao.updateSyncState(category.idLocal, "SYNCED")
                        );
                    })
                    .addOnFailureListener(e -> {
                        Log.e("CategoryRepositoryImpl", "Error al actualizar categoría en Firestore", e);
                        // Notificar error de conexión si aplica
                        ConnectionErrorNotifier.getInstance().notifyIfConnectionError(e);
                        executor.execute(() ->
                                categoryDao.updateSyncState(category.idLocal, "ERROR")
                        );
                    });
        }
    }
}
