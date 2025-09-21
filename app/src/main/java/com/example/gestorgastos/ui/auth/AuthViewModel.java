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
            errorMessage.setValue("¡Ups! Necesitamos tu email para iniciar sesión 📧");
            return;
        }
        
        if (password == null || password.trim().isEmpty()) {
            errorMessage.setValue("¡Oye! Tu contraseña es importante para tu seguridad 🔐");
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
                errorMessage.postValue("¡Oops! No pudimos iniciar sesión. Verifica tus datos y intenta de nuevo 😊");
            }
        });
    }
    
    public void signUp(String email, String password, String name) {
        if (email == null || email.trim().isEmpty()) {
            errorMessage.setValue("¡Hola! Necesitamos tu email para crear tu cuenta 📧");
            return;
        }
        
        if (password == null || password.trim().isEmpty()) {
            errorMessage.setValue("¡Casi listo! Tu contraseña nos ayudará a proteger tu cuenta 🔐");
            return;
        }
        
        if (password.length() < 6) {
            errorMessage.setValue("¡Por seguridad! Tu contraseña debe tener al menos 6 caracteres 🛡️");
            return;
        }
        
        if (name == null || name.trim().isEmpty()) {
            errorMessage.setValue("¡Genial! ¿Cómo te gustaría que te llamemos? 👋");
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
                errorMessage.postValue("¡Ups! No pudimos crear tu cuenta. Intenta de nuevo en un momento 😊");
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
                errorMessage.postValue("¡Ups! No pudimos eliminar tu cuenta. Intenta de nuevo más tarde 😊");
            }
        });
    }
    
    public void clearError() {
        errorMessage.setValue(null);
    }
    
    public void resetPassword(String email) {
        if (email == null || email.trim().isEmpty()) {
            errorMessage.setValue("¡Ay! Necesitamos tu email para ayudarte a recuperar tu contraseña 📧");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        authRepository.resetPassword(email.trim(), new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(UserEntity user) {
                isLoading.setValue(false);
                errorMessage.setValue("¡Perfecto! Te enviamos un enlace mágico a tu email para recuperar tu contraseña ✨");
            }
            
            @Override
            public void onError(Exception error) {
                isLoading.setValue(false);
                errorMessage.postValue("¡Oops! No pudimos enviar el enlace. Verifica tu email y intenta de nuevo 😊");
            }
        });
    }
}


