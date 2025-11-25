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
                errorMessage.postValue(translateFirebaseError(error, "login"));
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
        
        // El nombre se genera automáticamente desde el email si está vacío
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        // Si el nombre está vacío, el repositorio usará la parte del email antes del @
        String userName = (name != null && !name.trim().isEmpty()) ? name.trim() : "";
        authRepository.signUp(email.trim(), password, userName, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(UserEntity user) {
                Log.d("AuthViewModel", "SignUp success: " + user.email);
                isLoading.postValue(false);
                currentUser.postValue(user);
            }
            
            @Override
            public void onError(Exception error) {
                Log.e("AuthViewModel", "SignUp error: " + error.getMessage());
                isLoading.postValue(false);
                errorMessage.postValue(translateFirebaseError(error, "signup"));
            }
        });
    }
    
    public void signInWithGoogle(String idToken) {
        if (idToken == null || idToken.isEmpty()) {
            errorMessage.setValue("¡Ups! No pudimos obtener tu información de Google 😅");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        authRepository.signInWithGoogle(idToken, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(UserEntity user) {
                Log.d("AuthViewModel", "SignIn with Google success: " + user.email);
                isLoading.postValue(false);
                currentUser.postValue(user);
            }
            
            @Override
            public void onError(Exception error) {
                Log.e("AuthViewModel", "SignIn with Google error: " + error.getMessage());
                isLoading.postValue(false);
                errorMessage.postValue(translateFirebaseError(error, "google"));
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
                errorMessage.postValue(translateFirebaseError(error, "reset"));
            }
        });
    }
    
    /**
     * Traduce los errores de Firebase a mensajes amigables en español
     */
    private String translateFirebaseError(Exception error, String context) {
        String errorMsg = error.getMessage();
        if (errorMsg == null) errorMsg = "";
        
        // Errores de formato de email
        if (errorMsg.contains("badly formatted") || errorMsg.contains("invalid email")) {
            return "¡Hmmm! 🤔 Ese email no parece válido. Por favor revísalo:\n\n" +
                   "• Debe tener un @ en el medio\n" +
                   "• Ejemplo: nombre@dominio.com\n" +
                   "• Verifica que no tenga espacios";
        }
        
        // Errores de cuenta existente
        if (errorMsg.contains("already in use") || errorMsg.contains("email-already-in-use")) {
            return "¡Ey! 👋 Ya existe una cuenta con ese email.\n\n" +
                   "¿Tal vez ya te registraste antes? Intenta iniciar sesión o recupera tu contraseña.";
        }
        
        // Errores de usuario no encontrado
        if (errorMsg.contains("no user record") || errorMsg.contains("user not found")) {
            return "¡Ups! 🔍 No encontramos una cuenta con ese email.\n\n" +
                   "¿Quizás escribiste mal el email? O si es tu primera vez, ¡regístrate para crear tu cuenta!";
        }
        
        // Errores de contraseña incorrecta o credenciales inválidas
        if (errorMsg.contains("wrong password") || 
            errorMsg.contains("invalid-credential") ||
            errorMsg.contains("credential is incorrect") ||
            errorMsg.contains("malformed or has expired")) {
            return "¡Oops! 🔐 El email o la contraseña no son correctos.\n\n" +
                   "Revisa tus datos con cuidado o usa '¿Olvidaste tu contraseña?' para recuperarla.";
        }
        
        // Errores de contraseña débil
        if (errorMsg.contains("weak password") || errorMsg.contains("at least 6 characters")) {
            return "¡Por seguridad! 🛡️ Tu contraseña debe tener al menos 6 caracteres.\n\n" +
                   "Te recomendamos usar letras, números y símbolos para mayor protección.";
        }
        
        // Errores de demasiados intentos
        if (errorMsg.contains("too many requests") || errorMsg.contains("too-many-requests")) {
            return "¡Tranqui! 🚦 Has intentado muchas veces.\n\n" +
                   "Por seguridad, espera unos minutos antes de intentar de nuevo.";
        }
        
        // Errores de red
        if (errorMsg.contains("network error") || errorMsg.contains("network")) {
            return "¡Ups! 📡 Parece que no hay conexión a internet.\n\n" +
                   "Verifica tu WiFi o datos móviles e intenta de nuevo.";
        }
        
        // Error genérico según contexto
        if ("signup".equals(context)) {
            return "¡Ups! 😅 No pudimos crear tu cuenta en este momento.\n\n" +
                   "Por favor intenta de nuevo en unos segundos.";
        } else if ("reset".equals(context)) {
            return "¡Ups! 😅 No pudimos enviar el enlace de recuperación.\n\n" +
                   "Verifica que el email sea correcto e intenta de nuevo.";
        } else {
            return "¡Ups! 😅 No pudimos iniciar sesión.\n\n" +
                   "Verifica tus datos e intenta de nuevo.";
        }
    }
}


