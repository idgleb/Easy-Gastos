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
    
    List<ExpenseEntity> getPendingExpenses();
    
    void updateSyncState(long idLocal, String syncState);
    
    interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }
}


