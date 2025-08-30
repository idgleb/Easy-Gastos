package com.example.gestorgastos.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.example.gestorgastos.data.local.AppDatabase;
import com.example.gestorgastos.data.local.dao.ExpenseDao;
import com.example.gestorgastos.data.local.entity.ExpenseEntity;
import com.example.gestorgastos.util.DateTimeUtil;
import java.util.List;
import java.util.concurrent.ExecutorService;
import android.util.Log;
import java.util.concurrent.Executors;

public class ExpenseRepositoryImpl implements ExpenseRepository {
    private final ExpenseDao expenseDao;
    private final ExecutorService executor;
    
    public ExpenseRepositoryImpl(Context context) {
        AppDatabase database = AppDatabase.getDatabase(context);
        this.expenseDao = database.expenseDao();
        this.executor = Executors.newFixedThreadPool(4);
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
                
                // TODO: Sincronizar con Firestore más tarde
                
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
                
                // TODO: Sincronizar con Firestore más tarde
                
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
                
                // TODO: Sincronizar con Firestore más tarde
                
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    @Override
    public List<ExpenseEntity> getPendingExpenses() {
        return expenseDao.getPendingExpenses();
    }
    
    @Override
    public void updateSyncState(long idLocal, String syncState) {
        executor.execute(() -> {
            expenseDao.updateSyncState(idLocal, syncState);
        });
    }
}
