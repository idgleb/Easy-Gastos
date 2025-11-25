package com.example.gestorgastos.data.repository;

import androidx.lifecycle.LiveData;
import com.example.gestorgastos.data.local.entity.ExpenseEntity;
import java.util.List;

public interface ExpenseRepository {
    LiveData<List<ExpenseEntity>> getExpensesByUser(String userUid);
    
    List<ExpenseEntity> getExpensesByUserSync(String userUid);
    
    ExpenseEntity getExpenseById(long idLocal);
    
    void insertExpense(ExpenseEntity expense, RepositoryCallback<ExpenseEntity> callback);
    
    void updateExpense(ExpenseEntity expense, RepositoryCallback<ExpenseEntity> callback);
    
    void deleteExpense(long idLocal, RepositoryCallback<Void> callback);
    
    void getPendingExpenses(RepositoryCallback<List<ExpenseEntity>> callback);
    
    void updateSyncState(long idLocal, String syncState);
    
    /**
     * Sincroniza gastos desde Firestore hacia Room.
     * Si lastSyncMillis es 0, realiza sincronización completa.
     * Si lastSyncMillis > 0, realiza sincronización incremental (solo cambios desde ese timestamp).
     */
    void syncFromFirestore(String userUid, long lastSyncMillis, RepositoryCallback<Integer> callback);
    
    interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }
}



