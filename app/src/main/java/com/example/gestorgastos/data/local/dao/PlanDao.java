package com.example.gestorgastos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.gestorgastos.data.local.entity.PlanEntity;
import java.util.List;

@Dao
public interface PlanDao {
    @Query("SELECT * FROM plans WHERE remoteId = :remoteId LIMIT 1")
    LiveData<PlanEntity> getPlanByRemoteId(String remoteId);
    
    @Query("SELECT * FROM plans WHERE remoteId = :remoteId LIMIT 1")
    PlanEntity getPlanByRemoteIdSync(String remoteId);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPlan(PlanEntity plan);
    
    @Update
    void updatePlan(PlanEntity plan);
    
    @Query("SELECT * FROM plans WHERE isActive = 1")
    LiveData<List<PlanEntity>> getActivePlans();
    
    @Query("SELECT * FROM plans")
    LiveData<List<PlanEntity>> getAllPlans();
}
