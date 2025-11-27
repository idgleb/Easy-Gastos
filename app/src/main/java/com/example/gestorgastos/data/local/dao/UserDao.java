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
    
    @Query("SELECT * FROM users WHERE uid = :uid AND (deletedAt IS NULL OR deletedAt = 0) LIMIT 1")
    UserEntity getUserByUidSync(String uid);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertUser(UserEntity user);
    
    @Update
    void updateUser(UserEntity user);
    
    @Query("UPDATE users SET name = :name, email = :email, role = :role, planId = :planId, planExpiresAt = :planExpiresAt, zonaHoraria = :zonaHoraria, isActive = :isActive, syncState = :syncState, updatedAt = :updatedAt WHERE uid = :uid")
    void updateUserFields(String uid, String name, String email, String role, String planId, Long planExpiresAt, String zonaHoraria, boolean isActive, String syncState, long updatedAt);
    
    @Query("DELETE FROM users WHERE uid = :uid")
    void deleteUserByUid(String uid);
    
    @Query("UPDATE users SET deletedAt = :deletedAt, syncState = 'PENDING', updatedAt = :updatedAt WHERE uid = :uid")
    void softDeleteUser(String uid, long deletedAt, long updatedAt);
    
    @Query("SELECT * FROM users WHERE (deletedAt IS NULL OR deletedAt = 0)")
    LiveData<List<UserEntity>> getAllUsers();
    
    @Query("SELECT * FROM users WHERE (deletedAt IS NULL OR deletedAt = 0)")
    List<UserEntity> getAllUsersSync();
    
    @Query("SELECT * FROM users WHERE syncState = 'PENDING' AND (deletedAt IS NULL OR deletedAt = 0)")
    List<UserEntity> getPendingUsers();
    
    @Query("SELECT * FROM users WHERE syncState = 'PENDING' AND deletedAt IS NOT NULL AND deletedAt > 0")
    List<UserEntity> getPendingDeletions();
    
    @Query("SELECT * FROM users WHERE email = :email AND (deletedAt IS NULL OR deletedAt = 0) LIMIT 1")
    UserEntity findUserByEmail(String email);
    
    // Verificar y actualizar planes expirados
    @Query("UPDATE users SET planId = 'free', planExpiresAt = NULL, updatedAt = :updatedAt WHERE planExpiresAt IS NOT NULL AND planExpiresAt < :currentTime AND planId != 'free'")
    void expirePlans(long currentTime, long updatedAt);
}
