package com.example.gestorgastos.ui.auth;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gestorgastos.data.local.entity.UserEntity;
import com.example.gestorgastos.data.repository.AuthRepositoryImpl;
import com.example.gestorgastos.domain.repository.AuthRepository;

public class AuthViewModel extends AndroidViewModel {
    private final AuthRepository authRepository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<UserEntity> currentUser = new MutableLiveData<>();
    
    public AuthViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepositoryImpl(application);
        
        // Inicializar usuario actual si ya está autenticado
        LiveData<UserEntity> authUser = authRepository.getCurrentUser();
        authUser.observeForever(user -> {
            if (user != null) {
                currentUser.setValue(user);
            }
        });
    }
    
    public LiveData<UserEntity> getCurrentUser() {
        return currentUser;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public void signIn(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            errorMessage.setValue("El email es requerido");
            return;
        }
        
        if (password == null || password.trim().isEmpty()) {
            errorMessage.setValue("La contraseña es requerida");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        authRepository.signIn(email.trim(), password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(UserEntity user) {
                Log.d("AuthViewModel", "SignIn success: " + user.email);
                isLoading.postValue(false);
                currentUser.postValue(user);
            }
            
            @Override
            public void onError(Exception error) {
                Log.e("AuthViewModel", "SignIn error: " + error.getMessage());
                isLoading.postValue(false);
                errorMessage.postValue("Error al iniciar sesión: " + error.getMessage());
            }
        });
    }
    
    public void signUp(String email, String password, String name) {
        if (email == null || email.trim().isEmpty()) {
            errorMessage.setValue("El email es requerido");
            return;
        }
        
        if (password == null || password.trim().isEmpty()) {
            errorMessage.setValue("La contraseña es requerida");
            return;
        }
        
        if (password.length() < 6) {
            errorMessage.setValue("La contraseña debe tener al menos 6 caracteres");
            return;
        }
        
        if (name == null || name.trim().isEmpty()) {
            errorMessage.setValue("El nombre es requerido");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        authRepository.signUp(email.trim(), password, name.trim(), new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(UserEntity user) {
                Log.d("AuthViewModel", "SignUp success: " + user.email);
                isLoading.postValue(false);
                currentUser.postValue(user);
            }
            
            @Override
            public void onError(Exception error) {
                isLoading.postValue(false);
                errorMessage.postValue("Error al crear cuenta: " + error.getMessage());
            }
        });
    }
    
    public void signOut() {
        authRepository.signOut();
    }
    
    public void deleteAccount() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        authRepository.deleteAccount(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(UserEntity user) {
                isLoading.postValue(false);
            }
            
            @Override
            public void onError(Exception error) {
                isLoading.postValue(false);
                errorMessage.postValue("Error al eliminar cuenta: " + error.getMessage());
            }
        });
    }
    
    public void clearError() {
        errorMessage.setValue(null);
    }
}


