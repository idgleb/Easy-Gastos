package com.example.gestorgastos.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.gestorgastos.data.local.entity.UserEntity;
import com.example.gestorgastos.util.Result;

public interface AuthRepository {
    LiveData<UserEntity> getCurrentUser();
    
    String getCurrentUserUid();
    
    void signIn(String email, String password, AuthCallback callback);
    
    void signUp(String email, String password, String name, AuthCallback callback);
    
    void signOut();
    
    void resetPassword(String email, AuthCallback callback);
    
    void deleteAccount(AuthCallback callback);
    
    interface AuthCallback {
        void onSuccess(UserEntity user);
        void onError(Exception error);
    }
}


