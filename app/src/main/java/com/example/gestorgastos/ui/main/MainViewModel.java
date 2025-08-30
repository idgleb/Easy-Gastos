package com.example.gestorgastos.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.gestorgastos.data.local.entity.UserEntity;
import com.example.gestorgastos.data.repository.AuthRepositoryImpl;
import com.example.gestorgastos.domain.repository.AuthRepository;

public class MainViewModel extends AndroidViewModel {
    private final AuthRepository authRepository;
    
    public MainViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepositoryImpl(application);
    }
    
    public LiveData<UserEntity> getCurrentUser() {
        return authRepository.getCurrentUser();
    }
    
    public String getCurrentUserUid() {
        return authRepository.getCurrentUserUid();
    }
    
    public void signOut() {
        authRepository.signOut();
    }
}


