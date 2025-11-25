package com.example.gestorgastos.ui.main;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gestorgastos.data.local.entity.UserEntity;
import com.example.gestorgastos.data.repository.AuthRepositoryImpl;
import com.example.gestorgastos.domain.repository.AuthRepository;
import com.example.gestorgastos.util.MercadoPagoHelper;

public class MainViewModel extends AndroidViewModel {
    private final AuthRepository authRepository;
    private final MercadoPagoHelper mercadoPagoHelper;
    
    // LiveData para el estado del proceso de pago
    private final MutableLiveData<String> paymentError = new MutableLiveData<>();
    private final MutableLiveData<String> paymentInitPoint = new MutableLiveData<>();
    
    public MainViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepositoryImpl(application);
        this.mercadoPagoHelper = new MercadoPagoHelper();
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

    public void syncUserDataIfNeeded() {
        authRepository.syncUserDataIfNeeded();
    }
    
    /**
     * Inicia el proceso de actualización de plan a premium.
     * La lógica de negocio está aquí, no en la Activity (Clean Architecture).
     * 
     * @param context Contexto necesario para abrir el checkout
     * @param userUid UID del usuario que está actualizando su plan
     */
    public void upgradePlan(Context context, String userUid) {
        android.util.Log.d("MainViewModel", "upgradePlan - Iniciando creación de preferencia para userUid: " + userUid);
        // Configuración del plan premium
        String planId = "premium";
        String planName = "Plan Premium Para Siempre";
        double price = 100.0; // Precio en pesos argentinos (aumentado para evitar error CPT01)
        
        mercadoPagoHelper.createPreference(
            context,
            planId,
            planName,
            price,
            userUid,
            new MercadoPagoHelper.OnPreferenceCreatedListener() {
                @Override
                public void onSuccess(String initPoint) {
                    android.util.Log.d("MainViewModel", "Preferencia creada exitosamente, initPoint: " + initPoint);
                    paymentInitPoint.postValue(initPoint);
                }
                
                @Override
                public void onError(String error) {
                    android.util.Log.e("MainViewModel", "Error al crear preferencia: " + error);
                    paymentError.postValue("Error al crear el pago: " + error);
                }
            }
        );
    }
    
    /**
     * LiveData para observar cuando el checkout está listo para abrirse
     */
    public LiveData<String> getPaymentInitPoint() {
        return paymentInitPoint;
    }
    
    /**
     * LiveData para observar errores en el proceso de pago
     */
    public LiveData<String> getPaymentError() {
        return paymentError;
    }
    
    /**
     * Limpia los valores de LiveData después de usarlos
     */
    public void clearPaymentState() {
        paymentInitPoint.setValue(null);
        paymentError.setValue(null);
    }
}


