package com.example.gestorgastos.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.gestorgastos.data.local.entity.UserEntity;
import com.example.gestorgastos.util.Result;

public interface AuthRepository {
    LiveData<UserEntity> getCurrentUser();
    
    String getCurrentUserUid();
    
    void signIn(String email, String password, AuthCallback callback);
    
    void signUp(String email, String password, String name, AuthCallback callback);
    
    void signInWithGoogle(String idToken, AuthCallback callback);
    
    void signOut();
    
    void resetPassword(String email, AuthCallback callback);
    
    void deleteAccount(AuthCallback callback);

    /**
     * Verifica si los datos del usuario necesitan sincronización y, de ser así, la ejecuta en segundo plano.
     * Se usa para sincronizar cuando la app vuelve al primer plano.
     */
    void syncUserDataIfNeeded();
    
    interface AuthCallback {
        void onSuccess(UserEntity user);
        void onError(Exception error);
    }
}


