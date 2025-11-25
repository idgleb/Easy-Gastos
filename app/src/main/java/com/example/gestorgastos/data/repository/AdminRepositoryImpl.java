package com.example.gestorgastos.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.gestorgastos.data.local.AppDatabase;
import com.example.gestorgastos.data.local.dao.UserDao;
import com.example.gestorgastos.data.local.entity.UserEntity;
import com.example.gestorgastos.data.remote.FirestoreDataSource;
import com.example.gestorgastos.util.DateTimeUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminRepositoryImpl implements AdminRepository {
    private static final String TAG = "AdminRepositoryImpl";
    
    private final UserDao userDao;
    private final FirestoreDataSource firestoreDataSource;
    private final ExecutorService executor;
    private final MutableLiveData<List<UserEntity>> allUsersLiveData;
    private final OkHttpClient httpClient = new OkHttpClient();
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String CREATE_USER_ENDPOINT =
            "https://us-central1-gestor-gastos-app-6e1d9.cloudfunctions.net/createUserByAdmin";
    private static final String DELETE_USER_ENDPOINT =
            "https://us-central1-gestor-gastos-app-6e1d9.cloudfunctions.net/deleteUserByAdmin";
    
    public AdminRepositoryImpl(Context context) {
        AppDatabase database = AppDatabase.getDatabase(context);
        this.userDao = database.userDao();
        this.firestoreDataSource = new FirestoreDataSource();
        this.executor = Executors.newSingleThreadExecutor();
        this.allUsersLiveData = new MutableLiveData<>();
        
        // Cargar usuarios desde Firestore
        loadUsersFromFirestore();
    }
    
    @Override
    public LiveData<List<UserEntity>> getAllUsers() {
        return allUsersLiveData;
    }
    
    @Override
    public void createUser(UserEntity user, String password, RepositoryCallback<UserEntity> callback) {
        final String finalPassword = password;
        if (finalPassword == null || finalPassword.length() < 6) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Password inválida para creación de usuario"));
            }
            return;
        }
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (callback != null) {
                callback.onError(new IllegalStateException("Debe iniciar sesión como admin para crear usuarios."));
            }
            return;
        }

        currentUser.getIdToken(true)
                .addOnSuccessListener(result -> executor.execute(() ->
                        callCreateUserFunction(result.getToken(), user, finalPassword, callback)))
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Error obteniendo ID token para función admin", error);
                    if (callback != null) {
                        callback.onError(error);
                    }
                });
    }

    private void callCreateUserFunction(String idToken, UserEntity user, String password,
                                        RepositoryCallback<UserEntity> callback) {
        try {
            if (user.planId == null || user.planId.trim().isEmpty()) {
                user.planId = "free";
            }
            if (user.role == null || user.role.trim().isEmpty()) {
                user.role = "user";
            }
            if (user.zonaHoraria == null || user.zonaHoraria.trim().isEmpty()) {
                user.zonaHoraria = DateTimeUtil.getCurrentZoneId();
            }

            JSONObject payload = new JSONObject();
            payload.put("email", user.email);
            payload.put("password", password);
            payload.put("name", user.name);
            payload.put("role", user.role);
            payload.put("planId", user.planId);
            if (user.planExpiresAt != null) {
                payload.put("planExpiresAt", user.planExpiresAt);
            }
            payload.put("zonaHoraria", user.zonaHoraria);
            payload.put("isActive", user.isActive);

            RequestBody body = RequestBody.create(payload.toString(), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                    .url(CREATE_USER_ENDPOINT)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + idToken)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    throw new IOException("Cloud Function error: " + response.code() + " - " + responseBody);
                }
                JSONObject json = responseBody.isEmpty() ? new JSONObject() : new JSONObject(responseBody);
                user.uid = json.optString("uid", null);
                user.updatedAt = DateTimeUtil.getCurrentEpochMillis();

                loadUsersFromFirestore();
                if (callback != null) {
                    callback.onSuccess(user);
                }
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error al invocar Cloud Function createUserByAdmin", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
    
    public void loadUsersFromFirestore() {
        executor.execute(() -> {
            try {
                firestoreDataSource.getAllUsers()
                    .addOnSuccessListener(querySnapshot -> {
                        executor.execute(() -> {
                            List<UserEntity> users = new ArrayList<>();
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    try {
                                        UserEntity user = mapDocumentToUser(doc);
                                        if (user != null) {
                                            // Guardar en Room también
                                            UserEntity existingUser = userDao.getUserByUidSync(user.uid);
                                            if (existingUser != null) {
                                                // Actualizar si existe
                                                user.idLocal = existingUser.idLocal;
                                                userDao.updateUser(user);
                                            } else {
                                                // Insertar si no existe
                                                user.idLocal = userDao.insertUser(user);
                                            }
                                            users.add(user);
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error al mapear usuario: " + doc.getId(), e);
                                    }
                                }
                            }
                            allUsersLiveData.postValue(users);
                            Log.d(TAG, "Usuarios cargados desde Firestore: " + users.size());
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al cargar usuarios desde Firestore", e);
                        // Mostrar lista vacía si falla la carga
                        allUsersLiveData.postValue(new ArrayList<>());
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error al iniciar carga de usuarios", e);
            }
        });
    }
    
    private UserEntity mapDocumentToUser(DocumentSnapshot doc) {
        try {
            UserEntity user = new UserEntity();
            user.uid = doc.getId();
            user.name = doc.getString("name");
            user.email = doc.getString("email");
            user.role = doc.getString("role") != null ? doc.getString("role") : "user";
            user.planId = doc.getString("plan_id") != null ? doc.getString("plan_id") : "free";
            Long planExpiresAt = doc.getLong("plan_expires_at");
            user.planExpiresAt = planExpiresAt;
            user.zonaHoraria = doc.getString("zona_horaria") != null ? doc.getString("zona_horaria") : DateTimeUtil.getCurrentZoneId();
            Boolean isActive = doc.getBoolean("is_active");
            user.isActive = isActive != null ? isActive : true;
            
            // Obtener timestamp
            if (doc.getTimestamp("updated_at") != null) {
                user.updatedAt = doc.getTimestamp("updated_at").toDate().getTime();
            } else {
                user.updatedAt = DateTimeUtil.getCurrentEpochMillis();
            }
            
            return user;
        } catch (Exception e) {
            Log.e(TAG, "Error al mapear documento a UserEntity", e);
            return null;
        }
    }
    
    @Override
    public void updateUser(UserEntity user, RepositoryCallback<UserEntity> callback) {
        executor.execute(() -> {
            try {
                // Actualizar en Room
                user.updatedAt = DateTimeUtil.getCurrentEpochMillis();
                userDao.updateUser(user);
                
                // Actualizar en Firestore
                Map<String, Object> updates = new HashMap<>();
                updates.put("name", user.name);
                updates.put("email", user.email);
                updates.put("role", user.role);
                updates.put("plan_id", user.planId);
                if (user.planExpiresAt != null) {
                    updates.put("plan_expires_at", user.planExpiresAt);
                } else {
                    updates.put("plan_expires_at", null);
                }
                updates.put("zona_horaria", user.zonaHoraria);
                updates.put("is_active", user.isActive);
                updates.put("updated_at", com.google.firebase.firestore.FieldValue.serverTimestamp());
                
                firestoreDataSource.updateUser(user.uid, updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Usuario actualizado en Firestore: " + user.uid);
                        // Recargar lista
                        loadUsersFromFirestore();
                        if (callback != null) {
                            callback.onSuccess(user);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al actualizar usuario en Firestore", e);
                        if (callback != null) {
                            callback.onError(e);
                        }
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error al actualizar usuario", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    @Override
    public void deleteUser(String uid, RepositoryCallback<Void> callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (callback != null) {
                callback.onError(new IllegalStateException("Debe iniciar sesión como admin para eliminar usuarios."));
            }
            return;
        }

        currentUser.getIdToken(true)
                .addOnSuccessListener(result -> executor.execute(() ->
                        callDeleteUserFunction(result.getToken(), uid, callback)))
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Error obteniendo ID token para eliminar usuario", error);
                    if (callback != null) {
                        callback.onError(error);
                    }
                });
    }

    private void callDeleteUserFunction(String idToken, String uid,
                                        RepositoryCallback<Void> callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("uid", uid);

            RequestBody body = RequestBody.create(payload.toString(), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                    .url(DELETE_USER_ENDPOINT)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + idToken)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    throw new IOException("Cloud Function error: " + response.code() + " - " + responseBody);
                }

                userDao.deleteUserByUid(uid);
                loadUsersFromFirestore();

                if (callback != null) {
                    callback.onSuccess(null);
                }
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error al invocar Cloud Function deleteUserByAdmin", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
}

