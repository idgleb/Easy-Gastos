package com.example.gestorgastos.data.repository;

import androidx.lifecycle.LiveData;
import com.example.gestorgastos.data.local.entity.UserEntity;
import java.util.List;

public interface AdminRepository {
    LiveData<List<UserEntity>> getAllUsers();
    
    void createUser(UserEntity user, String password, RepositoryCallback<UserEntity> callback);
    
    void updateUser(UserEntity user, RepositoryCallback<UserEntity> callback);
    
    void deleteUser(String uid, RepositoryCallback<Void> callback);
    
    interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }
}

