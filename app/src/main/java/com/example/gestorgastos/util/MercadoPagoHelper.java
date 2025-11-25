package com.example.gestorgastos.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Helper para manejar la integración con Mercado Pago
 * Crea preferencias de pago y abre el checkout en el navegador
 */
public class MercadoPagoHelper {
    private static final String TAG = "MercadoPagoHelper";
    
    // IMPORTANTE: 
    // - Access Token: para crear preferencias (idealmente debería estar en backend)
    // Por ahora usamos el Access Token aquí, pero idealmente deberías crear un endpoint
    // en Cloud Functions que cree la preferencia usando el Access Token del servidor
    private static final String MP_ACCESS_TOKEN = "APP_USR-7199170803595673-111614-8f9ed71fa4dcf41a6f29b8e78237e33d-2474536291";
    
    // URL de la API de Mercado Pago para crear preferencias
    private static final String MP_API_URL = "https://api.mercadopago.com/checkout/preferences";
    
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient = new OkHttpClient();
    
    public interface OnPreferenceCreatedListener {
        void onSuccess(String initPoint);
        void onError(String error);
    }
    
    /**
     * Crea una preferencia de pago en Mercado Pago
     * @param context Contexto de la aplicación
     * @param planId ID del plan que se está comprando (ej: "premium")
     * @param planName Nombre del plan para mostrar
     * @param price Precio del plan
     * @param userUid UID de Firebase del usuario
     * @param listener Callback para manejar el resultado
     */
    public void createPreference(
            Context context,
            String planId,
            String planName,
            double price,
            String userUid,
            OnPreferenceCreatedListener listener) {
        
        try {
            // Construir el JSON de la preferencia
            JSONObject preferenceJson = new JSONObject();
            
            // Items (el producto que se está comprando)
            JSONObject item = new JSONObject();
            item.put("title", planName);
            item.put("quantity", 1);
            // Asegurar que el precio tenga máximo 2 decimales
            double roundedPrice = Math.round(price * 100.0) / 100.0;
            item.put("unit_price", roundedPrice);
            item.put("currency_id", "ARS"); // Moneda: Pesos Argentinos
            preferenceJson.put("items", new org.json.JSONArray().put(item));
            
            // Metadata (importante: aquí va el uid y planId para el webhook)
            JSONObject metadata = new JSONObject();
            metadata.put("uid", userUid);
            metadata.put("planId", planId);
            preferenceJson.put("metadata", metadata);
            
            // Configuraciones adicionales
            preferenceJson.put("back_urls", new JSONObject()
                    .put("success", "gestorgastos://payment/success")
                    .put("failure", "gestorgastos://payment/failure")
                    .put("pending", "gestorgastos://payment/pending"));
            
            preferenceJson.put("auto_return", "approved");
            
            // URL del webhook para recibir notificaciones de pago
            // IMPORTANTE: Esta URL debe estar configurada también en el panel de Mercado Pago
            String webhookUrl = "https://mercadopagowebhook-oyajdtkgga-uc.a.run.app";
            preferenceJson.put("notification_url", webhookUrl);
            
            // Configuraciones adicionales para evitar errores
            preferenceJson.put("statement_descriptor", "GESTOR GASTOS"); // Descripción en el resumen de tarjeta
            preferenceJson.put("external_reference", "plan_" + planId + "_" + userUid); // Referencia externa única
            
            // Simplificado: remover configuraciones que pueden causar problemas
            // binary_mode y expiration pueden causar conflictos en algunos casos
            
            // Log del JSON para debugging
            Log.d(TAG, "JSON de preferencia: " + preferenceJson.toString());
            
            // Crear la request
            RequestBody body = RequestBody.create(preferenceJson.toString(), JSON);
            Request request = new Request.Builder()
                    .url(MP_API_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + MP_ACCESS_TOKEN)
                    .post(body)
                    .build();
            
            // Ejecutar la request
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Error al crear preferencia", e);
                    if (listener != null) {
                        listener.onError("Error de conexión: " + e.getMessage());
                    }
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String initPoint = jsonResponse.getString("init_point");
                            
                            Log.d(TAG, "Preferencia creada, init point: " + initPoint);
                            
                            if (listener != null) {
                                listener.onSuccess(initPoint);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error al parsear respuesta", e);
                            if (listener != null) {
                                listener.onError("Error al procesar respuesta: " + e.getMessage());
                            }
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "Error HTTP: " + response.code() + " - " + errorBody);
                        
                        // Intentar parsear el error de Mercado Pago para mostrar un mensaje más útil
                        String errorMessage = "Error al procesar el pago";
                        try {
                            JSONObject errorJson = new JSONObject(errorBody);
                            if (errorJson.has("message")) {
                                errorMessage = errorJson.getString("message");
                            } else if (errorJson.has("error")) {
                                errorMessage = errorJson.getString("error");
                            }
                            if (errorJson.has("cause")) {
                                JSONArray causes = errorJson.getJSONArray("cause");
                                if (causes.length() > 0) {
                                    JSONObject firstCause = causes.getJSONObject(0);
                                    if (firstCause.has("description")) {
                                        errorMessage += ": " + firstCause.getString("description");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error al parsear mensaje de error", e);
                            errorMessage = "Error del servidor (código: " + response.code() + ")";
                        }
                        
                        if (listener != null) {
                            listener.onError(errorMessage);
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error al crear preferencia", e);
            if (listener != null) {
                listener.onError("Error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Abre el checkout de Mercado Pago en el navegador
     * @param context Contexto de la actividad
     * @param initPoint URL del checkout de Mercado Pago
     */
    public void openCheckout(Context context, String initPoint) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(initPoint));
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error al abrir checkout", e);
        }
    }
}
