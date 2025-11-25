package com.example.gestorgastos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.gestorgastos.data.local.entity.UserEntity;
import java.util.List;

@Dao
public interface UserDao {
    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    LiveData<UserEntity> getUserByUid(String uid);
    
    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    UserEntity getUserByUidSync(String uid);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertUser(UserEntity user);
    
    @Update
    void updateUser(UserEntity user);
    
    @Query("DELETE FROM users WHERE uid = :uid")
    void deleteUserByUid(String uid);
    
    @Query("SELECT * FROM users")
    LiveData<List<UserEntity>> getAllUsers();
    
    // Verificar y actualizar planes expirados
    @Query("UPDATE users SET planId = 'free', planExpiresAt = NULL, updatedAt = :updatedAt WHERE planExpiresAt IS NOT NULL AND planExpiresAt < :currentTime AND planId != 'free'")
    void expirePlans(long currentTime, long updatedAt);
}
