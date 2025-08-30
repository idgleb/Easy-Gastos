package com.example.gestorgastos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.gestorgastos.data.local.entity.ExpenseEntity;
import com.example.gestorgastos.data.local.model.CategorySum;
import java.util.List;

@Dao
public interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE userUid = :userUid AND deletedAt IS NULL ORDER BY fechaEpochMillis DESC")
    LiveData<List<ExpenseEntity>> getExpensesByUser(String userUid);
    
    @Query("SELECT * FROM expenses WHERE userUid = :userUid AND deletedAt IS NULL ORDER BY fechaEpochMillis DESC")
    List<ExpenseEntity> getExpensesByUserSync(String userUid);
    
    @Query("SELECT * FROM expenses WHERE idLocal = :idLocal LIMIT 1")
    ExpenseEntity getExpenseById(long idLocal);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertExpense(ExpenseEntity expense);
    
    @Update
    void updateExpense(ExpenseEntity expense);
    
    @Query("UPDATE expenses SET deletedAt = :deletedAt, syncState = 'PENDING', updatedAt = :updatedAt WHERE idLocal = :idLocal")
    void softDeleteExpense(long idLocal, long deletedAt, long updatedAt);
    
    @Query("SELECT * FROM expenses WHERE syncState = 'PENDING'")
    List<ExpenseEntity> getPendingExpenses();
    
    @Query("UPDATE expenses SET syncState = :syncState WHERE idLocal = :idLocal")
    void updateSyncState(long idLocal, String syncState);
    
    // Consultas para dashboard - temporalmente comentadas
    // @Query("SELECT categoryRemoteId, SUM(monto) AS total FROM expenses WHERE userUid = :userUid AND fechaEpochMillis >= :monthStart AND fechaEpochMillis <= :monthEnd AND deletedAt IS NULL GROUP BY categoryRemoteId ORDER BY total DESC")
    // LiveData<List<CategorySum>> sumByCategoryForMonth(String userUid, long monthStart, long monthEnd);
    
    // @Query("SELECT SUM(monto) FROM expenses WHERE userUid = :userUid AND fechaEpochMillis >= :monthStart AND fechaEpochMillis <= :monthEnd AND deletedAt IS NULL")
    // LiveData<Double> getTotalForMonth(String userUid, long monthStart, long monthEnd);
}
