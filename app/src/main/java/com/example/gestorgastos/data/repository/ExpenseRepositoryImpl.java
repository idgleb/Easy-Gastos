package com.example.gestorgastos.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.gestorgastos.data.local.AppDatabase;
import com.example.gestorgastos.data.local.dao.ExpenseDao;
import com.example.gestorgastos.data.local.dao.CategoryDao;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import com.example.gestorgastos.data.local.entity.ExpenseEntity;
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

public class ExpenseRepositoryImpl implements ExpenseRepository {
    private final ExpenseDao expenseDao;
    private final CategoryDao categoryDao;
    private final ExecutorService executor;
    private final FirestoreDataSource firestoreDataSource;
    
    public ExpenseRepositoryImpl(Context context) {
        AppDatabase database = AppDatabase.getDatabase(context);
        this.expenseDao = database.expenseDao();
        this.categoryDao = database.categoryDao();
        this.executor = Executors.newFixedThreadPool(4);
        this.firestoreDataSource = new FirestoreDataSource();
    }
    
    @Override
    public LiveData<List<ExpenseEntity>> getExpensesByUser(String userUid) {
        return expenseDao.getExpensesByUser(userUid);
    }
    
    @Override
    public List<ExpenseEntity> getExpensesByUserSync(String userUid) {
        return expenseDao.getExpensesByUserSync(userUid);
    }
    
    @Override
    public ExpenseEntity getExpenseById(long idLocal) {
        return expenseDao.getExpenseById(idLocal);
    }
    
    @Override
    public void insertExpense(ExpenseEntity expense, RepositoryCallback<ExpenseEntity> callback) {
        Log.d("ExpenseRepositoryImpl", "insertExpense - UserUid: " + expense.userUid + 
              ", CategoryRemoteId: " + expense.categoryRemoteId + 
              ", Monto: " + expense.monto);
        
        executor.execute(() -> {
            try {
                // Establecer valores por defecto
                expense.updatedAt = DateTimeUtil.getCurrentEpochMillis();
                expense.syncState = "PENDING";
                
                Log.d("ExpenseRepositoryImpl", "Insertando en Room - Expense: " + expense.toString());
                
                // Insertar en Room
                long expenseId = expenseDao.insertExpense(expense);
                expense.idLocal = expenseId;
                
                Log.d("ExpenseRepositoryImpl", "Gasto insertado exitosamente - ID: " + expenseId);
                
                // Sincronizar con Firestore en segundo plano (offline-first)
                syncExpenseWithFirestore(expense);
                
                callback.onSuccess(expense);
            } catch (Exception e) {
                Log.e("ExpenseRepositoryImpl", "Error al insertar gasto", e);
                callback.onError(e);
            }
        });
    }
    
    @Override
    public void updateExpense(ExpenseEntity expense, RepositoryCallback<ExpenseEntity> callback) {
        executor.execute(() -> {
            try {
                // Actualizar timestamp y estado de sincronización
                expense.updatedAt = DateTimeUtil.getCurrentEpochMillis();
                expense.syncState = "PENDING";
                
                // Actualizar en Room
                expenseDao.updateExpense(expense);
                
                // Sincronizar con Firestore en segundo plano
                syncExpenseWithFirestore(expense);
                
                callback.onSuccess(expense);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    @Override
    public void deleteExpense(long idLocal, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                long deletedAt = DateTimeUtil.getCurrentEpochMillis();
                long updatedAt = DateTimeUtil.getCurrentEpochMillis();
                
                // Soft delete en Room
                expenseDao.softDeleteExpense(idLocal, deletedAt, updatedAt);
                
                // Obtener el gasto actualizado para sincronizar soft delete
                ExpenseEntity deletedExpense = expenseDao.getExpenseById(idLocal);
                if (deletedExpense != null) {
                    deletedExpense.deletedAt = deletedAt;
                    deletedExpense.updatedAt = updatedAt;
                    deletedExpense.syncState = "PENDING";
                    syncExpenseWithFirestore(deletedExpense);
                }
                
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    @Override
    public void getPendingExpenses(RepositoryCallback<List<ExpenseEntity>> callback) {
        executor.execute(() -> {
            try {
                List<ExpenseEntity> pending = expenseDao.getPendingExpenses();
                callback.onSuccess(pending);
            } catch (Exception e) {
                Log.e("ExpenseRepositoryImpl", "Error al obtener gastos pendientes", e);
                callback.onError(e);
            }
        });
    }
    
    @Override
    public void updateSyncState(long idLocal, String syncState) {
        executor.execute(() -> {
            expenseDao.updateSyncState(idLocal, syncState);
        });
    }

    /**
     * Sincroniza todos los gastos pendientes con Firestore.
     * Se usa desde WorkManager para reintentos en segundo plano.
     */
    public void syncPendingExpensesWithFirestore() {
        getPendingExpenses(new RepositoryCallback<List<ExpenseEntity>>() {
            @Override
            public void onSuccess(List<ExpenseEntity> pending) {
                Log.d("ExpenseRepositoryImpl", "Sincronizando " + pending.size() + " gastos PENDING con Firestore");
                for (ExpenseEntity expense : pending) {
                    syncExpenseWithFirestore(expense);
                }
            }
            
            @Override
            public void onError(Exception error) {
                Log.e("ExpenseRepositoryImpl", "Error al obtener gastos pendientes para sincronizar", error);
            }
        });
    }

    /**
     * Sincroniza gastos desde Firestore hacia Room.
     * Si lastSyncMillis es 0, realiza sincronización completa.
     * Si lastSyncMillis > 0, realiza sincronización incremental (solo cambios desde ese timestamp).
     */
    @Override
    public void syncFromFirestore(String userUid, long lastSyncMillis, RepositoryCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                Task<QuerySnapshot> task;
                
                if (lastSyncMillis == 0) {
                    // Sincronización completa: obtener todos los gastos
                    Log.d("ExpenseRepositoryImpl", "Sincronización completa de gastos desde Firestore para usuario: " + userUid);
                    task = firestoreDataSource.getExpenses(userUid);
                } else {
                    // Sincronización incremental: solo cambios desde lastSyncMillis
                    Timestamp lastSync = new Timestamp(
                            lastSyncMillis / 1000,
                            (int) ((lastSyncMillis % 1000) * 1_000_000)
                    );
                    Log.d("ExpenseRepositoryImpl", "Sincronización incremental de gastos desde Firestore (desde: " + lastSyncMillis + ") para usuario: " + userUid);
                    task = firestoreDataSource.getExpensesAfter(userUid, lastSync);
                }
                
                task.addOnSuccessListener(querySnapshot -> {
                    executor.execute(() -> {
                        try {
                            int syncedCount = 0;
                            
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    try {
                                        String remoteId = doc.getId();
                                        String categoryRemoteId = doc.getString("category_remote_id");
                                        Double amount = doc.getDouble("amount");
                                        Timestamp timestamp = doc.getTimestamp("timestamp");
                                        Timestamp updatedAt = doc.getTimestamp("updated_at");
                                        Timestamp deletedAt = doc.getTimestamp("deleted_at");
                                        
                                        // Buscar si ya existe en Room por remoteId
                                        ExpenseEntity existing = expenseDao.getExpenseByRemoteId(remoteId);
                                        
                                        // Si no se encuentra por remoteId, buscar por atributos (para gastos creados en offline)
                                        if (existing == null && categoryRemoteId != null && amount != null && timestamp != null) {
                                            existing = expenseDao.findExpenseByAttributes(
                                                userUid,
                                                categoryRemoteId,
                                                amount,
                                                timestamp.toDate().getTime()
                                            );
                                            if (existing != null) {
                                                Log.d("ExpenseRepositoryImpl", "Gasto encontrado por atributos (creado en offline): idLocal=" + existing.idLocal);
                                            }
                                        }
                                        
                                        if (existing != null) {
                                            // Actualizar gasto existente
                                            if (categoryRemoteId != null) {
                                                existing.categoryRemoteId = categoryRemoteId;
                                            }
                                            if (amount != null) {
                                                existing.monto = amount;
                                            }
                                            if (timestamp != null) {
                                                existing.fechaEpochMillis = timestamp.toDate().getTime();
                                            }
                                            if (updatedAt != null) {
                                                existing.updatedAt = updatedAt.toDate().getTime();
                                            }
                                            if (deletedAt != null) {
                                                existing.deletedAt = deletedAt.toDate().getTime();
                                            } else {
                                                existing.deletedAt = null;
                                            }
                                            existing.syncState = "SYNCED";
                                            expenseDao.updateExpense(existing);
                                            syncedCount++;
                                        } else {
                                            // Insertar nuevo gasto
                                            ExpenseEntity expense = new ExpenseEntity();
                                            expense.remoteId = remoteId;
                                            expense.userUid = userUid;
                                            expense.categoryRemoteId = categoryRemoteId != null ? categoryRemoteId : "default";
                                            expense.monto = amount != null ? amount : 0.0;
                                            if (timestamp != null) {
                                                expense.fechaEpochMillis = timestamp.toDate().getTime();
                                            } else {
                                                expense.fechaEpochMillis = System.currentTimeMillis();
                                            }
                                            if (updatedAt != null) {
                                                expense.updatedAt = updatedAt.toDate().getTime();
                                            } else {
                                                expense.updatedAt = System.currentTimeMillis();
                                            }
                                            if (deletedAt != null) {
                                                expense.deletedAt = deletedAt.toDate().getTime();
                                            }
                                            expense.syncState = "SYNCED";
                                            
                                            long idLocal = expenseDao.insertExpense(expense);
                                            expense.idLocal = idLocal;
                                            syncedCount++;
                                        }
                                    } catch (Exception e) {
                                        Log.e("ExpenseRepositoryImpl", "Error al procesar gasto desde Firestore: " + doc.getId(), e);
                                    }
                                }
                            }
                            
                            Log.d("ExpenseRepositoryImpl", "Sincronización completada: " + syncedCount + " gastos procesados");
                            if (callback != null) {
                                callback.onSuccess(syncedCount);
                            }
                        } catch (Exception e) {
                            Log.e("ExpenseRepositoryImpl", "Error al procesar gastos desde Firestore", e);
                            if (callback != null) {
                                callback.onError(e);
                            }
                        }
                    });
                }).addOnFailureListener(e -> {
                    Log.e("ExpenseRepositoryImpl", "Error al obtener gastos desde Firestore", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
            } catch (Exception e) {
                Log.e("ExpenseRepositoryImpl", "Error al iniciar sincronización desde Firestore", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * Sincroniza un gasto con Firestore (offline-first).
     */
    private void syncExpenseWithFirestore(ExpenseEntity expense) {
        if (expense.userUid == null || expense.userUid.trim().isEmpty()) {
            Log.w("ExpenseRepositoryImpl", "No se puede sincronizar gasto sin userUid");
            return;
        }

        // Resolver remoteId real de la categoría si aún estamos usando el fallback "local_X"
        String resolvedCategoryRemoteId = expense.categoryRemoteId;
        if (resolvedCategoryRemoteId != null && resolvedCategoryRemoteId.startsWith("local_")) {
            try {
                long localId = Long.parseLong(resolvedCategoryRemoteId.replace("local_", ""));
                CategoryEntity category = categoryDao.getCategoryByIdIncludingInactive(localId);
                if (category != null && category.remoteId != null && !category.remoteId.trim().isEmpty()) {
                    resolvedCategoryRemoteId = category.remoteId;
                    Log.d("ExpenseRepositoryImpl", "Resuelto category_remote_id desde local_" + localId + " a remoteId=" + resolvedCategoryRemoteId);
                }
            } catch (NumberFormatException e) {
                Log.w("ExpenseRepositoryImpl", "No se pudo parsear id local de categoría desde: " + resolvedCategoryRemoteId, e);
            }
        }

        // Construir payload
        Map<String, Object> data = new HashMap<>();
        data.put("category_remote_id", resolvedCategoryRemoteId);
        data.put("amount", expense.monto);
        data.put("timestamp", new Timestamp(
                expense.fechaEpochMillis / 1000,
                (int) ((expense.fechaEpochMillis % 1000) * 1_000_000)
        ));

        Timestamp updatedAtTs = new Timestamp(
                expense.updatedAt / 1000,
                (int) ((expense.updatedAt % 1000) * 1_000_000)
        );
        data.put("updated_at", updatedAtTs);

        if (expense.deletedAt != null) {
            Timestamp deletedAtTs = new Timestamp(
                    expense.deletedAt / 1000,
                    (int) ((expense.deletedAt % 1000) * 1_000_000)
            );
            data.put("deleted_at", deletedAtTs);
        } else {
            data.put("deleted_at", null);
        }

        boolean isSoftDeleted = expense.deletedAt != null;

        if (expense.remoteId == null || expense.remoteId.trim().isEmpty()) {
            if (isSoftDeleted) {
                // No crear en remoto algo ya eliminado
                Log.d("ExpenseRepositoryImpl", "Gasto soft-deleted sin remoteId, no se crea en Firestore");
                return;
            }

            firestoreDataSource.createExpense(expense.userUid, data)
                    .addOnSuccessListener(docRef -> {
                        String remoteId = docRef.getId();
                        Log.d("ExpenseRepositoryImpl", "Gasto sincronizado en Firestore. remoteId=" + remoteId);
                        executor.execute(() -> {
                            expense.remoteId = remoteId;
                            expense.syncState = "SYNCED";
                            expenseDao.updateExpense(expense);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ExpenseRepositoryImpl", "Error al crear gasto en Firestore", e);
                        // Notificar error de conexión si aplica
                        ConnectionErrorNotifier.getInstance().notifyIfConnectionError(e);
                        executor.execute(() ->
                                expenseDao.updateSyncState(expense.idLocal, "ERROR")
                        );
                    });
        } else {
            firestoreDataSource.updateExpense(expense.userUid, expense.remoteId, data)
                    .addOnSuccessListener(unused -> {
                        Log.d("ExpenseRepositoryImpl", "Gasto actualizado en Firestore. remoteId=" + expense.remoteId);
                        executor.execute(() ->
                                expenseDao.updateSyncState(expense.idLocal, "SYNCED")
                        );
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ExpenseRepositoryImpl", "Error al actualizar gasto en Firestore", e);
                        // Notificar error de conexión si aplica
                        ConnectionErrorNotifier.getInstance().notifyIfConnectionError(e);
                        executor.execute(() ->
                                expenseDao.updateSyncState(expense.idLocal, "ERROR")
                        );
                    });
        }
    }
}
