package com.example.gestorgastos.ui.admin;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.gestorgastos.data.local.entity.UserEntity;
import com.example.gestorgastos.data.repository.AdminRepository;
import com.example.gestorgastos.data.repository.AdminRepositoryImpl;
import android.util.Log;
import java.util.List;

public class AdminViewModel extends AndroidViewModel {
    private final AdminRepository adminRepository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<String> generatedPassword = new MutableLiveData<>();
    
    public AdminViewModel(@NonNull Application application) {
        super(application);
        this.adminRepository = new AdminRepositoryImpl(application);
    }
    
    // LiveData para los usuarios
    public LiveData<List<UserEntity>> getAllUsers() {
        return adminRepository.getAllUsers();
    }
    
    // Estados de la UI
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }
    
    public LiveData<String> getGeneratedPassword() {
        return generatedPassword;
    }
    
    // Operaciones CRUD
    public void createUser(UserEntity user, String password) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);
        
        String finalPassword;
        boolean generatedPasswordFlag = false;
        if (password != null && !password.trim().isEmpty()) {
            if (password.trim().length() < 6) {
                isLoading.setValue(false);
                errorMessage.setValue("La contraseña debe tener al menos 6 caracteres");
                return;
            }
            finalPassword = password.trim();
        } else {
            finalPassword = generateTemporaryPassword();
            generatedPasswordFlag = true;
        }
        
        final String passwordToShow = finalPassword;
        final boolean shouldShowPassword = generatedPasswordFlag;
        
        adminRepository.createUser(user, finalPassword, new AdminRepository.RepositoryCallback<UserEntity>() {
            @Override
            public void onSuccess(UserEntity result) {
                isLoading.postValue(false);
                successMessage.postValue("Usuario creado correctamente.");
                if (shouldShowPassword) {
                    generatedPassword.postValue(passwordToShow);
                } else {
                    generatedPassword.postValue(null);
                }
            }
            
            @Override
            public void onError(Exception error) {
                isLoading.postValue(false);
                String errorMsg = error.getMessage();
                if (errorMsg != null && errorMsg.contains("email-already-in-use")) {
                    errorMessage.postValue("Error: El email ya está registrado");
                } else if (errorMsg != null && errorMsg.contains("invalid-email")) {
                    errorMessage.postValue("Error: Email inválido");
                } else {
                    errorMessage.postValue("Error al crear usuario: " + errorMsg);
                }
            }
        });
    }
    
    public void clearGeneratedPassword() {
        generatedPassword.setValue(null);
    }
    
    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder password = new StringBuilder();
        
        password.append(chars.charAt(random.nextInt(26)));
        password.append(chars.charAt(26 + random.nextInt(26)));
        password.append(chars.charAt(52 + random.nextInt(10)));
        password.append(chars.charAt(62 + random.nextInt(7)));
        
        for (int i = 4; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        
        return new String(passwordArray);
    }
    
    public void updateUser(UserEntity user) {
        Log.d("AdminViewModel", "updateUser - UID: " + user.uid + ", Nombre: " + user.name);
        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);
        
        adminRepository.updateUser(user, new AdminRepository.RepositoryCallback<UserEntity>() {
            @Override
            public void onSuccess(UserEntity result) {
                Log.d("AdminViewModel", "Usuario actualizado exitosamente");
                isLoading.postValue(false);
                successMessage.postValue("Usuario actualizado exitosamente");
            }
            
            @Override
            public void onError(Exception error) {
                Log.e("AdminViewModel", "Error al actualizar usuario", error);
                isLoading.postValue(false);
                errorMessage.postValue("Error al actualizar usuario: " + error.getMessage());
            }
        });
    }
    
    public void deleteUser(String uid) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);
        
        adminRepository.deleteUser(uid, new AdminRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                isLoading.postValue(false);
                successMessage.postValue("Usuario eliminado exitosamente");
            }
            
            @Override
            public void onError(Exception error) {
                isLoading.postValue(false);
                errorMessage.postValue("Error al eliminar usuario: " + error.getMessage());
            }
        });
    }
    
    // Métodos de utilidad
    public void clearMessages() {
        errorMessage.setValue(null);
        successMessage.setValue(null);
    }
    
    public void refreshUsers() {
        // Recargar usuarios desde Firestore
        if (adminRepository instanceof AdminRepositoryImpl) {
            ((AdminRepositoryImpl) adminRepository).loadUsersFromFirestore();
        }
    }
}

