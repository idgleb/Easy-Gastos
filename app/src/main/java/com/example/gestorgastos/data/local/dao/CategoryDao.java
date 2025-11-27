package com.example.gestorgastos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import java.util.List;

@Dao
public interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userUid = :userUid AND deletedAt IS NULL ORDER BY name ASC")
    LiveData<List<CategoryEntity>> getCategoriesByUser(String userUid);
    
    @Query("SELECT * FROM categories WHERE userUid = :userUid ORDER BY isActive DESC, name ASC")
    LiveData<List<CategoryEntity>> getAllCategoriesByUser(String userUid);
    
    @Query("SELECT * FROM categories WHERE userUid = :userUid ORDER BY isActive DESC, name ASC")
    List<CategoryEntity> getAllCategoriesByUserSync(String userUid);
    
    @Query("SELECT * FROM categories WHERE userUid = :userUid AND isActive = 1 AND deletedAt IS NULL ORDER BY name ASC")
    LiveData<List<CategoryEntity>> getActiveCategoriesByUser(String userUid);
    
    @Query("SELECT * FROM categories WHERE userUid = :userUid AND isActive = 1 AND deletedAt IS NULL ORDER BY name ASC")
    List<CategoryEntity> getActiveCategoriesByUserSync(String userUid);
    
    @Query("SELECT * FROM categories WHERE remoteId = :remoteId LIMIT 1")
    CategoryEntity getCategoryByRemoteId(String remoteId);
    
    @Query("SELECT * FROM categories WHERE idLocal = :idLocal LIMIT 1")
    CategoryEntity getCategoryById(long idLocal);
    
    @Query("SELECT * FROM categories WHERE idLocal = :idLocal AND deletedAt IS NULL LIMIT 1")
    CategoryEntity getCategoryByIdIncludingInactive(long idLocal);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertCategory(CategoryEntity category);
    
    @Update
    void updateCategory(CategoryEntity category);
    
    @Query("UPDATE categories SET isActive = 0, deletedAt = :deletedAt, syncState = 'PENDING', updatedAt = :updatedAt WHERE idLocal = :idLocal")
    void softDeleteCategory(long idLocal, long deletedAt, long updatedAt);
    
    @Query("SELECT * FROM categories WHERE syncState = 'PENDING'")
    List<CategoryEntity> getPendingCategories();
    
    @Query("UPDATE categories SET syncState = :syncState WHERE idLocal = :idLocal")
    void updateSyncState(long idLocal, String syncState);
    
    @Query("SELECT * FROM categories WHERE userUid = :userUid AND name = :name AND icono = :icono AND deletedAt IS NULL LIMIT 1")
    CategoryEntity findCategoryByAttributes(String userUid, String name, String icono);
    
    @Query("SELECT * FROM categories WHERE userUid = :userUid ORDER BY idLocal ASC")
    List<CategoryEntity> getAllCategoriesByUserDebug(String userUid);
}
